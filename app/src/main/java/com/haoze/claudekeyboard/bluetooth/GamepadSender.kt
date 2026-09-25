package com.haoze.claudekeyboard.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.util.Log

/**
 * Sends HID gamepad reports via Bluetooth.
 * Shares the same hidDevice connection as KeyboardSender.
 * Report layout matches the gamepad descriptor (Report ID 4):
 * 16 buttons + 1-byte Hat switch + 4 unsigned 16-bit axes (X, Y, Z, Rz) + 2 triggers (Rx, Ry).
 */
class GamepadSender(
    val hidDevice: BluetoothHidDevice,
    val host: BluetoothDevice
) {

    /** Called when a HID report fails to send. Parameter is a human-readable message. */
    var onSendError: ((String) -> Unit)? = null

    /**
     * Send a full gamepad state report (thread-safe: builds a local report instance).
     * @param buttonMask Bitmask of pressed buttons (bit index 0..15)
     * @param hatSwitch 4-bit Hat Switch direction (0 = released, 1 = Up, 2 = Up-Right, ..., 8 = Up-Left)
     * @param leftXFloat Left stick X, normalized -1..1
     * @param leftYFloat Left stick Y, normalized -1..1
     * @param rightXFloat Right stick X, normalized -1..1
     * @param rightYFloat Right stick Y, normalized -1..1
     */
    fun sendGamepadReport(
        buttonMask: Int,
        hatSwitch: Int,
        leftXFloat: Float,
        leftYFloat: Float,
        rightXFloat: Float,
        rightYFloat: Float
    ) {
        val report = GamepadReport()
        report.buttonMask = buttonMask
        report.hatSwitch = hatSwitch

        // Convert normalized -1..1 to unsigned 16-bit 0..65535 (32768 center)
        report.leftX = axisToUnsigned(leftXFloat)
        report.leftY = axisToUnsigned(leftYFloat)
        report.rightX = axisToUnsigned(rightXFloat)
        report.rightY = axisToUnsigned(rightYFloat)

        // UI 上 LT / RT 是数字按钮（bit 8 / 9），但描述符同时暴露了 Rx / Ry 模拟扳机轴。
        // 把按钮态同步到扳机轴，DirectInput / SDL / 浏览器 Gamepad API
        // 才能看到完整的 6 轴 + POV，否则会被当成"轴不全"的残缺手柄而拒绝识别。
        report.leftTrigger = if (isPressed(buttonMask, GamepadReport.BUTTON_LT)) GamepadReport.TRIGGER_MAX else GamepadReport.TRIGGER_RELEASED
        report.rightTrigger = if (isPressed(buttonMask, GamepadReport.BUTTON_RT)) GamepadReport.TRIGGER_MAX else GamepadReport.TRIGGER_RELEASED

        // Android 的 sendReport 会自行前置 Report ID，这里必须把第 0 字节剥掉
        if (!hidDevice.sendReport(host, GamepadReport.ID, report.bytes.copyOfRange(1, report.bytes.size))) {
            Log.e(TAG, "Gamepad report wasn't sent")
            onSendError?.invoke("Gamepad report send failed")
        }
    }

    private fun axisToUnsigned(value: Float): Int =
        ((value.coerceIn(-1f, 1f) + 1f) * 32767.5f).toInt().coerceIn(0, GamepadReport.AXIS_MAX)

    private fun isPressed(buttonMask: Int, bitIndex: Int): Boolean =
        (buttonMask and (1 shl bitIndex)) != 0

    companion object {
        private const val TAG = "GamepadSender"
    }
}
