package com.haoze.nexus.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice

/**
 * Holds and manages the lifecycle of HID input senders (keyboard, mouse, TV remote, gamepad).
 * Senders are active only while connected to a host device.
 */
class HidSenderHolder {
    @Volatile
    var keyboardSender: KeyboardSender? = null
        private set

    @Volatile
    var mouseSender: MouseSender? = null
        private set

    @Volatile
    var tvRemoteSender: TvRemoteSender? = null
        private set

    @Volatile
    var gamepadSender: GamepadSender? = null
        private set

    fun attachSenders(
        hidDevice: BluetoothHidDevice,
        device: BluetoothDevice,
        onError: (String) -> Unit
    ) {
        val kb = KeyboardSender(hidDevice, device).also {
            it.onSendError = onError
        }
        val mouse = MouseSender(hidDevice, device).also {
            it.onSendError = onError
        }
        val tv = TvRemoteSender(hidDevice, device, kb).also {
            it.onSendError = onError
        }
        val pad = GamepadSender(hidDevice, device).also {
            it.onSendError = onError
        }

        keyboardSender = kb
        mouseSender = mouse
        tvRemoteSender = tv
        gamepadSender = pad
    }

    fun clear() {
        keyboardSender = null
        mouseSender = null
        tvRemoteSender = null
        gamepadSender = null
    }
}
