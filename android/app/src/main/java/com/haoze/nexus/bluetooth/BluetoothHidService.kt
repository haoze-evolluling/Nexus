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

/**
 * Foreground service that manages Bluetooth HID device registration and connection.
 * Acts as the entry point and lifecycle manager, delegating state and communication
 * to [HidConnectionCoordinator], [HidEventManager], and [HidSenderHolder].
 */
class BluetoothHidService : Service() {

    companion object {
        private const val TAG = "BluetoothHidService"
        private const val HID_PROXY_TIMEOUT_MS = 15_000L
    }

    // ---- Binder ----

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // ---- Sub-components ----

    private val notificationManager by lazy { HidNotificationManager(this) }
    private val hidPreferences by lazy { HidPreferences(this) }
    private val reconnectManager = HidReconnectManager()
    private val eventManager = HidEventManager()
    private val senderHolder = HidSenderHolder()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isShuttingDown = false
    private var isHidSupportedState: Boolean? = null

    private val coordinator by lazy {
        HidConnectionCoordinator(
            context = this,
            mainHandler = mainHandler,
            notificationManager = notificationManager,
            hidPreferences = hidPreferences,
            reconnectManager = reconnectManager,
            eventManager = eventManager,
            senderHolder = senderHolder,
            bluetoothAdapterProvider = { bluetoothAdapter },
            isShuttingDownProvider = { isShuttingDown }
        )
    }

    // ---- Bluetooth state receiver ----

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (btState) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "Bluetooth turned on, re-initializing HID")
                        coordinator.resetToUnregistered()
                        mainHandler.postDelayed({ initializeHidDevice() }, 1000)
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG, "Bluetooth turned off")
                        coordinator.resetToUnregistered()
                        eventManager.notifyConnectionStateChanged(false, null)
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

        coordinator.activeProfile = hidPreferences.getStoredHidProfile()
        Log.d(TAG, "Active HID profile: ${coordinator.activeProfile}")

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
        coordinator.disconnect()
        coordinator.unregisterHidDevice()
        eventManager.clear()
    }

    // ---- Listeners registration ----

    fun addOnConnectionStateChangedListener(listener: (Boolean, String?) -> Unit) {
        eventManager.addOnConnectionStateChangedListener(listener)
    }

    fun removeOnConnectionStateChangedListener(listener: (Boolean, String?) -> Unit) {
        eventManager.removeOnConnectionStateChangedListener(listener)
    }

    fun addOnRegistrationStateChangedListener(listener: (Boolean) -> Unit) {
        eventManager.addOnRegistrationStateChangedListener(listener)
    }

    fun removeOnRegistrationStateChangedListener(listener: (Boolean) -> Unit) {
        eventManager.removeOnRegistrationStateChangedListener(listener)
    }

    fun addOnSendErrorListener(listener: (String) -> Unit) {
        eventManager.addOnSendErrorListener(listener)
    }

    fun removeOnSendErrorListener(listener: (String) -> Unit) {
        eventManager.removeOnSendErrorListener(listener)
    }

    fun addOnProfileChangedListener(listener: (HidProfile) -> Unit) {
        eventManager.addOnProfileChangedListener(listener)
    }

    fun removeOnProfileChangedListener(listener: (HidProfile) -> Unit) {
        eventManager.removeOnProfileChangedListener(listener)
    }

    fun addOnHidSupportedListener(listener: (Boolean) -> Unit) {
        eventManager.addOnHidSupportedListener(listener)
    }

    fun removeOnHidSupportedListener(listener: (Boolean) -> Unit) {
        eventManager.removeOnHidSupportedListener(listener)
    }

    fun addOnRegistrationFailedListener(listener: () -> Unit) {
        eventManager.addOnRegistrationFailedListener(listener)
    }

    fun removeOnRegistrationFailedListener(listener: () -> Unit) {
        eventManager.removeOnRegistrationFailedListener(listener)
    }

    // ---- Public API ----

    fun getHidDevice(): BluetoothHidDevice? = coordinator.hidDevice
    fun getConnectedDevice(): BluetoothDevice? = coordinator.getConnectedDevice()
    fun getKeyboardSender(): KeyboardSender? = senderHolder.keyboardSender
    fun getMouseSender(): MouseSender? = senderHolder.mouseSender
    fun getTvRemoteSender(): TvRemoteSender? = senderHolder.tvRemoteSender
    fun getGamepadSender(): GamepadSender? = senderHolder.gamepadSender
    fun isConnected(): Boolean = coordinator.isConnected()
    fun isRegistered(): Boolean = coordinator.isRegistered()
    fun isHidSupported(): Boolean = isHidSupportedState ?: (coordinator.hidDevice != null)
    fun hasLastConnectedDevice(): Boolean = coordinator.hasLastConnectedDevice()

    fun getConnectedDeviceName(): String? = coordinator.getConnectedDeviceName()
    fun getConnectedDeviceAddress(): String? = coordinator.getConnectedDeviceAddress()

    /** 当前对外呈现的 HID 身份。 */
    fun getInputProfile(): HidProfile = coordinator.activeProfile

    /**
     * 切换对外呈现的 HID 身份（键鼠组合 ⇄ 游戏手柄）。
     */
    fun setInputProfile(profile: HidProfile): Boolean = coordinator.setInputProfile(profile)

    fun getLastConnectedDeviceAddress(): String? = coordinator.getLastDeviceAddress()

    fun disconnect() {
        coordinator.disconnect()
    }

    fun connectToDevice(address: String): Boolean = coordinator.connectToDevice(address)

    fun dismissConnectionNotification() {
        notificationManager.dismissConnectionNotification()
    }

    // ---- HID proxy initialization ----

    private fun initializeHidDevice() {
        Log.d(TAG, "Initializing HID device")

        val proxyTimeout = Runnable {
            if (coordinator.hidDevice == null && coordinator.state is HidState.Unregistered) {
                Log.e(TAG, "getProfileProxy timed out after ${HID_PROXY_TIMEOUT_MS}ms")
                isHidSupportedState = false
                eventManager.notifyHidSupported(false)
                eventManager.notifyRegistrationStateChanged(false)
            }
        }
        mainHandler.postDelayed(proxyTimeout, HID_PROXY_TIMEOUT_MS)

        val proxyInitiated = try {
            bluetoothAdapter?.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    mainHandler.removeCallbacks(proxyTimeout)
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        coordinator.hidDevice = proxy as BluetoothHidDevice
                        Log.d(TAG, "HID device proxy obtained")
                        isHidSupportedState = true
                        eventManager.notifyHidSupported(true)
                        reconnectManager.resetRegistration()
                        coordinator.registerHidDevice()
                        coordinator.setDiscoverable()
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        coordinator.hidDevice = null
                        coordinator.state = HidState.Unregistered
                        Log.d(TAG, "HID device proxy disconnected")
                    }
                }
            }, BluetoothProfile.HID_DEVICE) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get HID profile proxy: ${e.message}", e)
            false
        }

        if (!proxyInitiated) {
            mainHandler.removeCallbacks(proxyTimeout)
            Log.e(TAG, "BluetoothProfile.HID_DEVICE is not supported on this device")
            isHidSupportedState = false
            eventManager.notifyHidSupported(false)
            eventManager.notifyRegistrationStateChanged(false)
        }
    }
}
