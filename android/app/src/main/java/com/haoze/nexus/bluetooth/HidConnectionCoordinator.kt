package com.haoze.nexus.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.util.Log
import com.haoze.nexus.R
import java.util.concurrent.Executors

/**
 * Coordinates Bluetooth HID state machine, app registration, device connection,
 * auto-reconnect retries, and callback handling.
 */
class HidConnectionCoordinator(
    private val context: Context,
    private val mainHandler: Handler,
    private val notificationManager: HidNotificationManager,
    private val hidPreferences: HidPreferences,
    private val reconnectManager: HidReconnectManager,
    private val eventManager: HidEventManager,
    private val senderHolder: HidSenderHolder,
    private val bluetoothAdapterProvider: () -> BluetoothAdapter?,
    private val isShuttingDownProvider: () -> Boolean,
) {
    companion object {
        private const val TAG = "BluetoothHidService"
        private const val REREGISTER_DELAY_MS = 600L
    }

    @Volatile
    var hidDevice: BluetoothHidDevice? = null

    @Volatile
    var state: HidState = HidState.Unregistered
        set(value) {
            Log.d(TAG, "State: ${field::class.simpleName} → ${value::class.simpleName}")
            field = value
        }

    @Volatile
    var userInitiatedDisconnect: Boolean = false

    @Volatile
    var activeProfile: HidProfile = HidProfile.DEFAULT

    fun isConnected(): Boolean = state is HidState.Connected

    fun isRegistered(): Boolean = state is HidState.Registered || state is HidState.Connected

    fun getConnectedDevice(): BluetoothDevice? = (state as? HidState.Connected)?.device

    fun getConnectedDeviceName(): String? = (state as? HidState.Connected)?.deviceName

    fun getConnectedDeviceAddress(): String? = (state as? HidState.Connected)?.device?.address

    fun getLastDeviceAddress(): String? = hidPreferences.getLastDeviceAddress()

    fun hasLastConnectedDevice(): Boolean = getLastDeviceAddress() != null

    fun setDiscoverable() {
        HidDevicePolicyHelper.setDiscoverable(bluetoothAdapterProvider())
    }

    fun setInputProfile(profile: HidProfile): Boolean {
        if (profile == activeProfile) return false
        val hd = hidDevice
        if (hd == null) {
            activeProfile = profile
            hidPreferences.saveHidProfile(profile)
            eventManager.notifyProfileChanged(profile)
            Log.d(TAG, "HID profile saved (proxy not ready): $profile")
            return true
        }

        Log.d(TAG, "Switching HID profile: $activeProfile → $profile")
        activeProfile = profile
        hidPreferences.saveHidProfile(profile)
        eventManager.notifyProfileChanged(profile)

        if (state is HidState.Connected) {
            userInitiatedDisconnect = true
            (state as? HidState.Connected)?.let { hd.disconnect(it.device) }
        }

        mainHandler.postDelayed({
            if (isShuttingDownProvider()) return@postDelayed
            forceUnregisterApp()
            reconnectManager.resetRegistration()
            registerHidDevice()
        }, REREGISTER_DELAY_MS)
        return true
    }

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
        val adapter = bluetoothAdapterProvider() ?: return false
        if (state !is HidState.Registered && state !is HidState.Connected) {
            Log.w(TAG, "Cannot connect: HID not registered")
            return false
        }

        if (state is HidState.Connected) {
            userInitiatedDisconnect = true
            (state as? HidState.Connected)?.let { hd.disconnect(it.device) }
        }

        val device = adapter.bondedDevices?.find { it.address == address } ?: run {
            Log.w(TAG, "Device ($address) not found in bonded devices")
            return false
        }

        hidPreferences.saveLastConnectedDevice(address, device.name)
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

    fun registerHidDevice() {
        val hd = hidDevice ?: return

        if (state is HidState.Registered || state is HidState.Connected) {
            Log.d(TAG, "HID device already registered, skipping")
            return
        }
        if (state is HidState.Registering) {
            Log.d(TAG, "HID device registration already in progress, skipping")
            return
        }

        try {
            hd.unregisterApp()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister stale app: ${e.message}")
        }

        val appName = context.getString(R.string.app_name)
        val sdpSettings = activeProfile.buildSdpSettings(appName)
        Log.d(TAG, "Registering HID app as ${activeProfile.name} (descriptor ${sdpSettings.descriptors.size} bytes)")

        val executor = Executors.newSingleThreadExecutor()
        val callback = createHidCallback()

        state = HidState.Registering
        val registerStarted = hd.registerApp(sdpSettings, null, null, executor, callback)
        Log.d(TAG, "HID app registration started: $registerStarted")
        if (!registerStarted) {
            state = HidState.Unregistered
            scheduleRegistrationRetry()
        }
    }

    private fun createHidCallback(): BluetoothHidDevice.Callback {
        return object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                super.onAppStatusChanged(pluggedDevice, registered)
                Log.d(TAG, "HID app registration status changed: $registered")

                if (registered) {
                    reconnectManager.resetRegistration()
                    state = HidState.Registered(
                        lastDeviceAddress = hidPreferences.getLastDeviceAddress(),
                        lastDeviceName = hidPreferences.getLastDeviceName()
                    )
                    notificationManager.updateForegroundNotification(context.getString(R.string.notification_waiting))
                    eventManager.notifyRegistrationStateChanged(true)
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
                        val connectedDev = device ?: run {
                            Log.e(TAG, "STATE_CONNECTED with null device — ignoring")
                            return
                        }
                        val name = connectedDev.name
                        state = HidState.Connected(connectedDev, name)
                        userInitiatedDisconnect = false
                        reconnectManager.resetReconnect()

                        hidPreferences.saveLastConnectedDevice(connectedDev.address, name)

                        Log.d(TAG, "Connected to: $name")
                        notificationManager.showConnectionNotification(
                            context.getString(R.string.notification_connected, name ?: ""),
                            hidPreferences.isConnectionNotificationsEnabled()
                        )

                        val hd = hidDevice
                        if (hd != null) {
                            senderHolder.attachSenders(hd, connectedDev) { errorMsg ->
                                eventManager.notifySendError(errorMsg)
                            }
                            HidDevicePolicyHelper.setConnectionPolicy(hd, connectedDev)
                        }

                        notificationManager.updateForegroundNotification(context.getString(R.string.notification_connected, name ?: ""))
                        eventManager.notifyConnectionStateChanged(true, name)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        state = HidState.Registered(
                            lastDeviceAddress = hidPreferences.getLastDeviceAddress(),
                            lastDeviceName = hidPreferences.getLastDeviceName()
                        )
                        senderHolder.clear()
                        Log.d(TAG, "Disconnected")
                        notificationManager.showConnectionNotification(
                            context.getString(R.string.notification_disconnected),
                            hidPreferences.isConnectionNotificationsEnabled()
                        )
                        notificationManager.updateForegroundNotification(context.getString(R.string.notification_waiting))
                        eventManager.notifyConnectionStateChanged(false, null)
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
                hidDevice?.let { hd ->
                    HidDevicePolicyHelper.sendReportReply(hd, device, type, id)
                }
            }
        }
    }

    fun scheduleRegistrationRetry() {
        val adapter = bluetoothAdapterProvider() ?: return
        if (isShuttingDownProvider() || !adapter.isEnabled || hidDevice == null) return
        if (state is HidState.Registered || state is HidState.Connected) return

        reconnectManager.scheduleRegistrationRetry(
            handler = mainHandler,
            onRetry = { registerHidDevice() },
            onExhausted = { eventManager.notifyRegistrationStateChanged(false) }
        )
    }

    fun scheduleReconnect() {
        val address = getLastDeviceAddress() ?: return
        reconnectManager.scheduleReconnect(mainHandler, address) {
            if (hidDevice == null || state is HidState.Connected || userInitiatedDisconnect) return@scheduleReconnect
            tryConnectToLastDevice()
        }
    }

    fun tryConnectToLastDevice(): Boolean {
        val address = getLastDeviceAddress() ?: return false
        val hd = hidDevice ?: return false
        val adapter = bluetoothAdapterProvider() ?: return false
        if (state is HidState.Connected) return true
        if (state !is HidState.Registered) {
            Log.w(TAG, "Cannot connect: HID not registered yet")
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
            scheduleReconnect()
        }
        return result
    }

    fun unregisterHidDevice() {
        hidDevice?.let { device ->
            if (state is HidState.Registered || state is HidState.Connected) {
                device.unregisterApp()
                state = HidState.Unregistered
                Log.d(TAG, "HID device unregistered")
            }
        }
    }

    fun forceUnregisterApp() {
        val hd = hidDevice ?: return
        try {
            hd.unregisterApp()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to force unregister app: ${e.message}")
        }
        state = HidState.Unregistered
        senderHolder.clear()
        Log.d(TAG, "HID app force-unregistered")
    }

    fun resetToUnregistered() {
        state = HidState.Unregistered
        reconnectManager.resetRegistration()
        senderHolder.clear()
        mainHandler.removeCallbacksAndMessages(null)
        hidDevice?.let {
            try {
                bluetoothAdapterProvider()?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
            } catch (_: Exception) {}
        }
        hidDevice = null
    }
}
