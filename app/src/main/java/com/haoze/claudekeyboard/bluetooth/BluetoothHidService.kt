package com.haoze.claudekeyboard.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.haoze.claudekeyboard.MainActivity
import com.haoze.claudekeyboard.R
import java.util.concurrent.Executors

/**
 * Foreground service that manages Bluetooth HID device registration and connection.
 * Uses an explicit [HidState] state machine instead of independent boolean flags.
 */
class BluetoothHidService : Service() {

    companion object {
        private const val TAG = "BluetoothHidService"
        private const val NOTIFICATION_CHANNEL_ID = "bluetooth_hid_channel"
        private const val NOTIFICATION_ID = 1001
        private const val CONNECTION_NOTIFICATION_CHANNEL_ID = "connection_events"
        private const val CONNECTION_NOTIFICATION_ID = 1002
        private const val MAX_REGISTRATION_RETRIES = 3
        private const val MAX_RECONNECT_RETRIES = 5
        private const val RECONNECT_BASE_DELAY_MS = 2_000L
        private const val RECONNECT_MAX_DELAY_MS = 30_000L
        private const val HID_PROXY_TIMEOUT_MS = 15_000L
        private const val PREFS_NAME = "bluetooth_prefs"
        private const val KEY_LAST_DEVICE_ADDRESS = "last_device_address"
        private const val KEY_LAST_DEVICE_NAME = "last_device_name"
        private const val PREFS_SETTINGS_NAME = "settings_prefs"
        private const val KEY_AUTO_CONNECT_LAUNCH = "auto_connect_on_launch"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect_on_disconnect"
        private const val KEY_HID_PROFILE = "hid_profile"
        /** 切换 Profile 后等主机断连完成再重新注册的间隔 */
        private const val REREGISTER_DELAY_MS = 600L
    }

    // ---- Binder ----

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    override fun onBind(intent: Intent?): IBinder = binder

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
    private var registrationRetryCount = 0
    private var reconnectRetryCount = 0
    @Volatile
    private var lastReconnectAttempt: Long = 0

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

    private val connectionStateListeners = java.util.concurrent.CopyOnWriteArraySet<(Boolean, String?) -> Unit>()
    private val registrationStateListeners = java.util.concurrent.CopyOnWriteArraySet<(Boolean) -> Unit>()
    private val sendErrorListeners = java.util.concurrent.CopyOnWriteArraySet<(String) -> Unit>()
    private val profileListeners = java.util.concurrent.CopyOnWriteArraySet<(HidProfile) -> Unit>()

    // ---- Persistence ----

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val settingsPrefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_SETTINGS_NAME, Context.MODE_PRIVATE)
    }

    private val mainHandler = Handler(Looper.getMainLooper())

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
                        updateNotification(getString(R.string.notification_waiting))
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

        activeProfile = HidProfile.fromStorageKey(settingsPrefs.getString(KEY_HID_PROFILE, null))
        Log.d(TAG, "Active HID profile: $activeProfile")

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

        createNotificationChannel()
        createConnectionNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.notification_waiting)))
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
            settingsPrefs.edit().putString(KEY_HID_PROFILE, profile.storageKey).apply()
            notifyProfileChanged(profile)
            Log.d(TAG, "HID profile saved (proxy not ready): $profile")
            return true
        }

        Log.d(TAG, "Switching HID profile: $activeProfile → $profile")
        activeProfile = profile
        settingsPrefs.edit().putString(KEY_HID_PROFILE, profile.storageKey).apply()
        notifyProfileChanged(profile)

        // 断开现有连接 —— 主机必须重新枚举 Report Map 才能拿到新的设备类型
        if (state is HidState.Connected) {
            userInitiatedDisconnect = true
            (state as? HidState.Connected)?.let { hd.disconnect(it.device) }
        }

        mainHandler.postDelayed({
            if (isShuttingDown) return@postDelayed
            forceUnregisterApp()
            registrationRetryCount = 0
            registerHidDevice()
        }, REREGISTER_DELAY_MS)
        return true
    }
    fun getLastConnectedDeviceAddress(): String? = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null)

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
        prefs.edit()
            .putString(KEY_LAST_DEVICE_ADDRESS, address)
            .putString(KEY_LAST_DEVICE_NAME, device.name)
            .apply()
        // NOTE: userInitiatedDisconnect intentionally NOT reset here.
        // It was set to true above if we were connected to a previous device.
        // After the async disconnect callback fires, it will check this flag and
        // skip scheduleReconnect(). The flag is reset in onConnectionStateChanged:
        //   - STATE_CONNECTED resets it on successful connection
        //   - STATE_DISCONNECTED resets it after checking (to prevent stale state)
        lastReconnectAttempt = 0
        reconnectRetryCount = 0
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
                    registrationRetryCount = 0
                    state = HidState.Registered(
                        lastDeviceAddress = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null),
                        lastDeviceName = prefs.getString(KEY_LAST_DEVICE_NAME, null)
                    )
                    updateNotification(getString(R.string.notification_waiting))
                    notifyRegistrationStateChanged(true)
                    if (settingsPrefs.getBoolean(KEY_AUTO_CONNECT_LAUNCH, true)) {
                        val lastAddr = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null)
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
                        reconnectRetryCount = 0

                        // Persist last connected device
                        prefs.edit()
                            .putString(KEY_LAST_DEVICE_ADDRESS, device.address)
                            .putString(KEY_LAST_DEVICE_NAME, name)
                            .apply()

                        Log.d(TAG, "Connected to: $name")
                        showConnectionNotification(getString(R.string.notification_connected, name ?: ""))

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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try {
                                val method = hidDevice!!.javaClass.getMethod(
                                    "setConnectionPolicy",
                                    BluetoothDevice::class.java,
                                    Int::class.javaPrimitiveType
                                )
                                method.invoke(hidDevice, device, 1) // CONNECTION_POLICY_ALLOWED
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to set connection policy: ${e.message}")
                            }
                        }
                        updateNotification(getString(R.string.notification_connected, name ?: ""))
                        notifyConnectionStateChanged(true, name)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        val wasConnected = state is HidState.Connected
                        state = HidState.Registered(
                            lastDeviceAddress = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null),
                            lastDeviceName = prefs.getString(KEY_LAST_DEVICE_NAME, null)
                        )
                        keyboardSender = null
                        mouseSender = null
                        tvRemoteSender = null
                        gamepadSender = null
                        Log.d(TAG, "Disconnected")
                        showConnectionNotification(getString(R.string.notification_disconnected))
                        updateNotification(getString(R.string.notification_waiting))
                        notifyConnectionStateChanged(false, null)
                        setDiscoverable()

                        if (!userInitiatedDisconnect && settingsPrefs.getBoolean(KEY_AUTO_RECONNECT, true)) {
                            scheduleReconnect()
                        }
                        userInitiatedDisconnect = false
                    }
                }
            }

            override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
                super.onGetReport(device, type, id, bufferSize)
                try {
                    val method = hidDevice!!.javaClass.getMethod(
                        "sendReply",
                        BluetoothDevice::class.java,
                        Byte::class.javaPrimitiveType,
                        Byte::class.javaPrimitiveType,
                        ByteArray::class.java
                    )
                    method.invoke(hidDevice, device, type, id, ByteArray(8))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send GET_REPORT reply: ${e.message}")
                }
            }

            // NOTE: BluetoothHidDevice.Callback does not have an onError method.
            // HID errors are surfaced through onConnectionStateChanged(DISCONNECTED)
            // which is already handled above with reconnect logic.
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
        try {
            val method = bluetoothAdapter?.javaClass?.getMethod(
                "setScanMode",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            val result = method?.invoke(bluetoothAdapter, 23, 300)
            Log.d(TAG, "setScanMode result: $result")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set discoverable mode: ${e.message}")
        }
    }

    // ---- Retry logic ----

    private fun scheduleRegistrationRetry() {
        val adapter = bluetoothAdapter ?: return
        if (isShuttingDown || !adapter.isEnabled || hidDevice == null) return
        if (state is HidState.Registered || state is HidState.Connected) return
        if (registrationRetryCount >= MAX_REGISTRATION_RETRIES) {
            Log.w(TAG, "HID app registration failed after retries")
            notifyRegistrationStateChanged(false)
            return
        }

        registrationRetryCount++
        Log.d(TAG, "Retrying HID app registration ($registrationRetryCount/$MAX_REGISTRATION_RETRIES)")
        mainHandler.postDelayed({
            registerHidDevice()
        }, 1000L * registrationRetryCount)
    }

    private fun scheduleReconnect() {
        val address = getLastDeviceAddress() ?: return
        if (reconnectRetryCount >= MAX_RECONNECT_RETRIES) {
            Log.w(TAG, "Reconnect retries exhausted ($MAX_RECONNECT_RETRIES) for $address")
            return
        }

        // Exponential backoff: 2s, 4s, 8s, 16s, 30s (capped)
        val delay = minOf(
            RECONNECT_BASE_DELAY_MS * (1L shl reconnectRetryCount),
            RECONNECT_MAX_DELAY_MS
        )
        reconnectRetryCount++
        Log.d(TAG, "Scheduling reconnect attempt $reconnectRetryCount/$MAX_RECONNECT_RETRIES to $address in ${delay}ms")

        mainHandler.postDelayed({
            val hd = hidDevice ?: return@postDelayed
            if (state is HidState.Connected || userInitiatedDisconnect) return@postDelayed
            tryConnectToLastDevice()
        }, delay)
    }

    private fun tryConnectToLastDevice(): Boolean {
        val address = getLastDeviceAddress() ?: return false
        val hd = hidDevice ?: return false
        val adapter = bluetoothAdapter ?: return false
        if (state is HidState.Connected) return true
        if (state !is HidState.Registered) {
            Log.w(TAG, "Cannot connect: HID not registered yet")
            // Don't count this as a retry — registration may complete later
            reconnectRetryCount = maxOf(0, reconnectRetryCount - 1)
            scheduleReconnect()
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastReconnectAttempt < 2000) return false
        lastReconnectAttempt = now

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
        registrationRetryCount = 0
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

    private fun getLastDeviceAddress(): String? = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null)

    // ---- Notifications ----

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createConnectionNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CONNECTION_NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_connection_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "设备连接和断开通知"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showConnectionNotification(contentText: String) {
        if (!settingsPrefs.getBoolean("connection_notifications", true)) return
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CONNECTION_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(CONNECTION_NOTIFICATION_ID, notification)
    }

    fun dismissConnectionNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CONNECTION_NOTIFICATION_ID)
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
