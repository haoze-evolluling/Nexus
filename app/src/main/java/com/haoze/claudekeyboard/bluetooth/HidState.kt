package com.haoze.claudekeyboard.bluetooth

import android.bluetooth.BluetoothDevice

/**
 * Explicit state machine for Bluetooth HID device lifecycle.
 * Replaces the previous 6 independent boolean flags with a single
 * sealed class that enforces valid state transitions.
 */
sealed class HidState {
    /** HID profile proxy not yet obtained or Bluetooth off. */
    object Unregistered : HidState()

    /** registerApp() called, waiting for onAppStatusChanged callback. */
    object Registering : HidState()

    /** HID app registered but no host connected. Discoverable. */
    data class Registered(
        val lastDeviceAddress: String? = null,
        val lastDeviceName: String? = null
    ) : HidState()

    /** Connected to a host device. KeyboardSender/MouseSender are live. */
    data class Connected(
        val device: BluetoothDevice,
        val deviceName: String?
    ) : HidState()
}
