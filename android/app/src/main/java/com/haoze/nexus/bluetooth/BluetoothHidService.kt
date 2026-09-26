package com.haoze.nexus.bluetooth

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.haoze.nexus.R
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

/**
 * Foreground service that manages Bluetooth HID device registration and connection.
 * Uses an explicit [HidState] state machine instead of independent boolean flags.
 */
class BluetoothHidService : Service() {

    companion object {
        private const val TAG = "BluetoothHidService"
        private const val HID_PROXY_TIMEOUT_MS = 15_000L
        /** 切换 Profile 后等主机断连完成再重新注册的间隔 */
        private const val REREGISTER_DELAY_MS = 600L
    }

    // ---- Binder ----

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // ---- Helper components ----

    private val notificationManager by lazy { HidNotificationManager(this) }
    private val hidPreferences by lazy { HidPreferences(this) }
    private val reconnectManager = HidReconnectManager()
    private val mainHandler = Handler(Looper.getMainLooper())

    // ---- Bluetooth components ----

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null

    // ---- State machine ----

    @Volatile
    private var state: HidState = HidState.Unregistered
        set(value) {
            Log.d(TAG, "State: ${field::class.simpleName} → ${value::class.simpleName}")
            field = value
        }

    @Volatile
    private var userInitiatedDisconnect = false
    private var isShuttingDown = false

    /**
     * 当前对外呈现的 HID 身份（游戏手柄 / 键鼠组合）。
     * 决定注册时使用的 SDP 设置与报告描述符，见 [HidProfile]。
     */
    @Volatile
    private var activeProfile: HidProfile = HidProfile.DEFAULT

    // ---- Sender instances (live only in Connected state) ----

    @Volatile
    private var keyboardSender: KeyboardSender? = null
    @Volatile
    private var mouseSender: MouseSender? = null
    @Volatile
    private var tvRemoteSender: TvRemoteSender? = null
    @Volatile
    private var gamepadSender: GamepadSender? = null

    // ---- Callbacks ----

    private val connectionStateListeners = CopyOnWriteArraySet<(Boolean, String?) -> Unit>()
    private val registrationStateListeners = CopyOnWriteArraySet<(Boolean) -> Unit>()
    private val sendErrorListeners = CopyOnWriteArraySet<(String) -> Unit>()
    private val profileListeners = CopyOnWriteArraySet<(HidProfile) -> Unit>()

    // ---- Bluetooth state receiver ----

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (btState) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "Bluetooth turned on, re-initializing HID")
                        resetToUnregistered()
                        mainHandler.postDelayed({ initializeHidDevice() }, 1000)
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG, "Bluetooth turned off")
                        resetToUnregistered()
                        notifyConnectionStateChanged(false, null)
                        notificationManager.updateForegroundNotification(getString(R.string.notification_waiting))
                    }
                }
            }
        }
    }

    // ---- Lifecycle ----

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported")
            stopSelf()
            return
        }

        activeProfile = hidPreferences.getStoredHidProfile()
        Log.d(TAG, "Active HID profile: $activeProfile")

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

        notificationManager.createNotificationChannels()
        startForeground(
            HidNotificationManager.NOTIFICATION_ID,
            notificationManager.createForegroundNotification(getString(R.string.notification_waiting))
        )
        initializeHidDevice()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isShuttingDown = true
        mainHandler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver already unregistered")
        }
        disconnect()
        unregisterHidDevice()
    }

    // ---- Callbacks ----

    fun addOnConnectionStateChangedListener(listener: (Boolean, String?) -> Unit) {
        connectionStateListeners.add(listener)
    }

    fun removeOnConnectionStateChangedListener(listener: (Boolean, String?) -> Unit) {
        connectionStateListeners.remove(listener)
    }

    fun addOnRegistrationStateChangedListener(listener: (Boolean) -> Unit) {
        registrationStateListeners.add(listener)
    }

    fun removeOnRegistrationStateChangedListener(listener: (Boolean) -> Unit) {
        registrationStateListeners.remove(listener)
    }

    fun addOnSendErrorListener(listener: (String) -> Unit) {
        sendErrorListeners.add(listener)
    }

    fun removeOnSendErrorListener(listener: (String) -> Unit) {
        sendErrorListeners.remove(listener)
    }

    fun addOnProfileChangedListener(listener: (HidProfile) -> Unit) {
        profileListeners.add(listener)
    }

    fun removeOnProfileChangedListener(listener: (HidProfile) -> Unit) {
        profileListeners.remove(listener)
    }

    private fun notifyConnectionStateChanged(isConnected: Boolean, deviceName: String?) {
        connectionStateListeners.forEach { it(isConnected, deviceName) }
    }

    private fun notifyRegistrationStateChanged(isRegistered: Boolean) {
        registrationStateListeners.forEach { it(isRegistered) }
    }

    private fun notifySendError(message: String) {
        sendErrorListeners.forEach { it(message) }
    }

    private fun notifyProfileChanged(profile: HidProfile) {
        profileListeners.forEach { it(profile) }
    }

    // ---- Public API ----

    fun getHidDevice(): BluetoothHidDevice? = hidDevice
    fun getConnectedDevice(): BluetoothDevice? = (state as? HidState.Connected)?.device
    fun getKeyboardSender(): KeyboardSender? = keyboardSender
    fun getMouseSender(): MouseSender? = mouseSender
    fun getTvRemoteSender(): TvRemoteSender? = tvRemoteSender
    fun getGamepadSender(): GamepadSender? = gamepadSender
    fun isConnected(): Boolean = state is HidState.Connected
    fun isRegistered(): Boolean = state is HidState.Registered || state is HidState.Connected
    fun hasLastConnectedDevice(): Boolean = getLastDeviceAddress() != null

    fun getConnectedDeviceName(): String? = (state as? HidState.Connected)?.deviceName
    fun getConnectedDeviceAddress(): String? = (state as? HidState.Connected)?.device?.address

    /** 当前对外呈现的 HID 身份。 */
    fun getInputProfile(): HidProfile = activeProfile

    /**
     * 切换对外呈现的 HID 身份（键鼠组合 ⇄ 游戏手柄）。
     *
     * HID 的 SDP 设置与报告描述符在注册时就固定了，换身份必须先断开当前主机、
     * unregisterApp 再用新的描述符重新注册。主机端一般需要重新连接一次，
     * 极少数需要删除配对重新配对（描述符被缓存时）。
     *
     * 切换不影响键盘 / 鼠标 / 消费者控制的报文结构，键鼠功能保持不变。
     *
     * @return true 表示已触发切换；false 表示 HID 尚不可用或已是目标身份。
     */
    fun setInputProfile(profile: HidProfile): Boolean {
        if (profile == activeProfile) return false
        val hd = hidDevice
        if (hd == null) {
            // 代理还没拿到：先落盘，等 registerHidDevice 时自然生效
            activeProfile = profile
            hidPreferences.saveHidProfile(profile)
            notifyProfileChanged(profile)
            Log.d(TAG, "HID profile saved (proxy not ready): $profile")
            return true
        }

        Log.d(TAG, "Switching HID profile: $activeProfile → $profile")
        activeProfile = profile
        hidPreferences.saveHidProfile(profile)
        notifyProfileChanged(profile)

        // 断开现有连接 —— 主机必须重新枚举 Report Map 才能拿到新的设备类型
        if (state is HidState.Connected) {
            userInitiatedDisconnect = true
            (state as? HidState.Connected)?.let { hd.disconnect(it.device) }
        }

        mainHandler.postDelayed({
            if (isShuttingDown) return@postDelayed
            forceUnregisterApp()
            reconnectManager.resetRegistration()
            registerHidDevice()
        }, REREGISTER_DELAY_MS)
        return true
    }

    fun getLastConnectedDeviceAddress(): String? = hidPreferences.getLastDeviceAddress()

    fun disconnect() {
        userInitiatedDisconnect = true
        val currentState = state
        if (currentState is HidState.Connected) {
            hidDevice?.disconnect(currentState.device)
        }
        setDiscoverable()
    }

    fun connectToDevice(address: String): Boolean {
        val hd = hidDevice ?: return false
        val adapter = bluetoothAdapter ?: return false
        if (state !is HidState.Registered && state !is HidState.Connected) {
            Log.w(TAG, "Cannot connect: HID not registered")
            return false
        }

        // Disconnect current device if connected
        if (state is HidState.Connected) {
            userInitiatedDisconnect = true
            (state as? HidState.Connected)?.let { hd.disconnect(it.device) }
        }

        val device = adapter.bondedDevices?.find { it.address == address } ?: run {
            Log.w(TAG, "Device ($address) not found in bonded devices")
            return false
        }

        // Update last device for reconnect
        hidPreferences.saveLastConnectedDevice(address, device.name)
        // NOTE: userInitiatedDisconnect intentionally NOT reset here.
        // It was set to true above if we were connected to a previous device.
        // After the async disconnect callback fires, it will check this flag and
        // skip scheduleReconnect(). The flag is reset in onConnectionStateChanged:
        //   - STATE_CONNECTED resets it on successful connection
        //   - STATE_DISCONNECTED resets it after checking (to prevent stale state)
        reconnectManager.resetReconnect()
        setDiscoverable()

        Log.d(TAG, "Attempting HID connect to ${device.name} ($address)")
        val result = try {
            hd.connect(device)
        } catch (e: Exception) {
            Log.w(TAG, "HID connect failed: ${e.message}")
            false
        }
        Log.d(TAG, "hd.connect returned $result")
        return result
    }

    // ---- HID initialization ----

    private fun initializeHidDevice() {
        Log.d(TAG, "Initializing HID device")

        // Timeout: if getProfileProxy never fires, clean up and report failure
        val proxyTimeout = Runnable {
            if (hidDevice == null && state is HidState.Unregistered) {
                Log.e(TAG, "getProfileProxy timed out after ${HID_PROXY_TIMEOUT_MS}ms")
                notifyRegistrationStateChanged(false)
            }
        }
        mainHandler.postDelayed(proxyTimeout, HID_PROXY_TIMEOUT_MS)

        bluetoothAdapter?.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                mainHandler.removeCallbacks(proxyTimeout)
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as BluetoothHidDevice
                    Log.d(TAG, "HID device proxy obtained")
                    registerHidDevice()
                    setDiscoverable()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = null
                    state = HidState.Unregistered
                    Log.d(TAG, "HID device proxy disconnected")
                }
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    private fun registerHidDevice() {
        val hd = hidDevice ?: return

        if (state is HidState.Registered || state is HidState.Connected) {
            Log.d(TAG, "HID device already registered, skipping")
            return
        }
        if (state is HidState.Registering) {
            Log.d(TAG, "HID device registration already in progress, skipping")
            return
        }

        // Clean up any stale registration
        try {
            hd.unregisterApp()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister stale app: ${e.message}")
        }

        // 用当前 Profile 的 SDP 设置：手柄模式用 Gamepad 描述符且不再声明为键盘子类
        val appName = getString(R.string.app_name)
        val sdpSettings = activeProfile.buildSdpSettings(appName)
        Log.d(TAG, "Registering HID app as ${activeProfile.name} (descriptor ${sdpSettings.descriptors.size} bytes)")

        val executor = Executors.newSingleThreadExecutor()

        val callback = object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                super.onAppStatusChanged(pluggedDevice, registered)
                Log.d(TAG, "HID app registration status changed: $registered")

                if (registered) {
                    reconnectManager.resetRegistration()
                    state = HidState.Registered(
                        lastDeviceAddress = hidPreferences.getLastDeviceAddress(),
                        lastDeviceName = hidPreferences.getLastDeviceName()
                    )
                    notificationManager.updateForegroundNotification(getString(R.string.notification_waiting))
                    notifyRegistrationStateChanged(true)
                    if (hidPreferences.isAutoConnectOnLaunchEnabled()) {
                        val lastAddr = hidPreferences.getLastDeviceAddress()
                        if (lastAddr != null) {
                            tryConnectToLastDevice()
                        }
                    }
                } else {
                    state = HidState.Unregistered
                    scheduleRegistrationRetry()
                }
            }

            override fun onConnectionStateChanged(device: BluetoothDevice?, connState: Int) {
                super.onConnectionStateChanged(device, connState)
                when (connState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        val device = device ?: run {
                            Log.e(TAG, "STATE_CONNECTED with null device — ignoring")
                            return
                        }
                        val name = device.name
                        state = HidState.Connected(device, name)
                        userInitiatedDisconnect = false
                        reconnectManager.resetReconnect()

                        // Persist last connected device
                        hidPreferences.saveLastConnectedDevice(device.address, name)

                        Log.d(TAG, "Connected to: $name")
                        notificationManager.showConnectionNotification(
                            getString(R.string.notification_connected, name ?: ""),
                            hidPreferences.isConnectionNotificationsEnabled()
                        )

                        // Create senders
                        keyboardSender = KeyboardSender(hidDevice!!, device).also {
                            it.onSendError = ::notifySendError
                        }
                        mouseSender = MouseSender(hidDevice!!, device).also {
                            it.onSendError = ::notifySendError
                        }
                        tvRemoteSender = TvRemoteSender(hidDevice!!, device, keyboardSender!!).also {
                            it.onSendError = ::notifySendError
                        }
                        gamepadSender = GamepadSender(hidDevice!!, device).also {
                            it.onSendError = ::notifySendError
                        }

                        // Set connection policy for auto-reconnect (API 33+)
                        HidDevicePolicyHelper.setConnectionPolicy(hidDevice!!, device)
                        notificationManager.updateForegroundNotification(getString(R.string.notification_connected, name ?: ""))
                        notifyConnectionStateChanged(true, name)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        state = HidState.Registered(
                            lastDeviceAddress = hidPreferences.getLastDeviceAddress(),
                            lastDeviceName = hidPreferences.getLastDeviceName()
                        )
                        keyboardSender = null
                        mouseSender = null
                        tvRemoteSender = null
                        gamepadSender = null
                        Log.d(TAG, "Disconnected")
                        notificationManager.showConnectionNotification(
                            getString(R.string.notification_disconnected),
                            hidPreferences.isConnectionNotificationsEnabled()
                        )
                        notificationManager.updateForegroundNotification(getString(R.string.notification_waiting))
                        notifyConnectionStateChanged(false, null)
                        setDiscoverable()

                        if (!userInitiatedDisconnect && hidPreferences.isAutoReconnectOnDisconnectEnabled()) {
                            scheduleReconnect()
                        }
                        userInitiatedDisconnect = false
                    }
                }
            }

            override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
                super.onGetReport(device, type, id, bufferSize)
                HidDevicePolicyHelper.sendReportReply(hidDevice!!, device, type, id)
            }
        }

        state = HidState.Registering
        val registerStarted = hd.registerApp(sdpSettings, null, null, executor, callback)
        Log.d(TAG, "HID app registration started: $registerStarted")
        if (!registerStarted) {
            state = HidState.Unregistered
            scheduleRegistrationRetry()
        }
    }

    // ---- Discoverability ----

    private fun setDiscoverable() {
        HidDevicePolicyHelper.setDiscoverable(bluetoothAdapter)
    }

    // ---- Retry logic ----

    private fun scheduleRegistrationRetry() {
        val adapter = bluetoothAdapter ?: return
        if (isShuttingDown || !adapter.isEnabled || hidDevice == null) return
        if (state is HidState.Registered || state is HidState.Connected) return

        reconnectManager.scheduleRegistrationRetry(
            handler = mainHandler,
            onRetry = { registerHidDevice() },
            onExhausted = { notifyRegistrationStateChanged(false) }
        )
    }

    private fun scheduleReconnect() {
        val address = getLastDeviceAddress() ?: return
        reconnectManager.scheduleReconnect(mainHandler, address) {
            val hd = hidDevice ?: return@scheduleReconnect
            if (state is HidState.Connected || userInitiatedDisconnect) return@scheduleReconnect
            tryConnectToLastDevice()
        }
    }

    private fun tryConnectToLastDevice(): Boolean {
        val address = getLastDeviceAddress() ?: return false
        val hd = hidDevice ?: return false
        val adapter = bluetoothAdapter ?: return false
        if (state is HidState.Connected) return true
        if (state !is HidState.Registered) {
            Log.w(TAG, "Cannot connect: HID not registered yet")
            // Don't count this as a retry — registration may complete later
            reconnectManager.decrementReconnectRetryCount()
            scheduleReconnect()
            return false
        }

        if (!reconnectManager.checkAndRecordReconnectAttempt()) return false

        val device = adapter.bondedDevices?.find { it.address == address } ?: run {
            Log.w(TAG, "Last device ($address) not found in bonded devices")
            return false
        }
        Log.d(TAG, "Attempting HID connect to ${device.name} ($address)")
        val result = try {
            hd.connect(device)
        } catch (e: Exception) {
            Log.w(TAG, "HID connect failed: ${e.message}")
            false
        }
        Log.d(TAG, "hd.connect returned $result")
        if (!result) {
            Log.d(TAG, "Phone-initiated connect failed — the host must discover and connect to this device")
            // Schedule next retry with backoff
            scheduleReconnect()
        }
        return result
    }

    // ---- Cleanup ----

    private fun unregisterHidDevice() {
        hidDevice?.let { device ->
            if (state is HidState.Registered || state is HidState.Connected) {
                device.unregisterApp()
                state = HidState.Unregistered
                Log.d(TAG, "HID device unregistered")
            }
        }
    }

    /**
     * 无条件注销当前 HID 应用（忽略状态机），用于切换 Profile 前清理旧注册。
     * 若不清干净，栈里会残留旧描述符，主机连上来仍然只看到键盘。
     */
    private fun forceUnregisterApp() {
        val hd = hidDevice ?: return
        try {
            hd.unregisterApp()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to force unregister app: ${e.message}")
        }
        state = HidState.Unregistered
        keyboardSender = null
        mouseSender = null
        tvRemoteSender = null
        gamepadSender = null
        Log.d(TAG, "HID app force-unregistered")
    }

    private fun resetToUnregistered() {
        state = HidState.Unregistered
        reconnectManager.resetRegistration()
        keyboardSender = null
        mouseSender = null
        tvRemoteSender = null
        gamepadSender = null
        mainHandler.removeCallbacksAndMessages(null)
        hidDevice?.let {
            try {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
            } catch (_: Exception) {}
        }
        hidDevice = null
    }

    private fun getLastDeviceAddress(): String? = hidPreferences.getLastDeviceAddress()

    fun dismissConnectionNotification() {
        notificationManager.dismissConnectionNotification()
    }
}
