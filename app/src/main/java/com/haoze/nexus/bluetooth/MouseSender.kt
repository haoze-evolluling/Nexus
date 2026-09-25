package com.haoze.claudekeyboard.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.util.Log

/**
 * Sends HID mouse reports via Bluetooth.
 * Shares the same hidDevice connection as KeyboardSender.
 *
 * 报文节奏对齐 BtRemote 的成功经验：一次手势事件只发一个报文。
 * 相对位移报文由主机按 delta 累加，移动后无需再发清零报文——
 * 多余的清零报文会让中断通道队列饱和，表现为光标滞后卡顿。
 */
class MouseSender(
    val hidDevice: BluetoothHidDevice,
    val host: BluetoothDevice
) {

    /** Called when a HID report fails to send. Parameter is a human-readable message. */
    var onSendError: ((String) -> Unit)? = null

    /**
     * Send a mouse report using a local report instance (thread-safe).
     */
    private fun sendReportLocal(report: MouseReport) {
        if (!hidDevice.sendReport(host, MouseReport.ID, report.bytes.copyOfRange(1, report.bytes.size))) {
            Log.e(TAG, "Mouse report wasn't sent")
            onSendError?.invoke("Mouse report send failed")
        }
    }

    /**
     * Send a relative mouse movement (no buttons held).
     * Single report per event: the host accumulates deltas, no clear report needed.
     * @param dx X-axis movement (-127 to 127)
     * @param dy Y-axis movement (-127 to 127)
     */
    @Synchronized
    fun sendMouseMove(dx: Int, dy: Int) {
        val report = MouseReport()
        report.deltaX = dx.coerceIn(-127, 127).toByte()
        report.deltaY = dy.coerceIn(-127, 127).toByte()
        sendReportLocal(report)
    }

    /**
     * Send a relative mouse movement with explicit button state.
     * Buttons and delta travel in the same single report; the button state
     * persists on the host until a later report clears it, so no follow-up
     * hold/clear report is sent.
     * Used during drag operations where buttons must stay held.
     * @param dx X-axis movement (-127 to 127)
     * @param dy Y-axis movement (-127 to 127)
     * @param leftButton Whether left button is held
     * @param rightButton Whether right button is held
     * @param middleButton Whether middle button is held
     */
    @Synchronized
    fun sendMouseMoveWithButtons(dx: Int, dy: Int, leftButton: Boolean = false, rightButton: Boolean = false, middleButton: Boolean = false) {
        val report = MouseReport()
        report.leftButton = leftButton
        report.rightButton = rightButton
        report.middleButton = middleButton
        report.deltaX = dx.coerceIn(-127, 127).toByte()
        report.deltaY = dy.coerceIn(-127, 127).toByte()
        sendReportLocal(report)
    }

    /**
     * Send a mouse button click (press + release).
     * @param button MouseReport.BUTTON_LEFT, BUTTON_RIGHT, or BUTTON_MIDDLE
     */
    @Synchronized
    fun sendMouseClick(button: Int) {
        val press = MouseReport()
        when (button) {
            MouseReport.BUTTON_LEFT -> press.leftButton = true
            MouseReport.BUTTON_RIGHT -> press.rightButton = true
            MouseReport.BUTTON_MIDDLE -> press.middleButton = true
        }
        sendReportLocal(press)
        // Release
        val release = MouseReport()
        sendReportLocal(release)
    }

    /**
     * Send vertical and horizontal scroll in one report (both wheels share a
     * single report), then one clear report to stop the scroll.
     * @param vAmount Vertical scroll amount (positive = up, negative = down)
     * @param hAmount Horizontal scroll amount (-127 to 127)
     */
    @Synchronized
    fun sendMouseScroll(vAmount: Int, hAmount: Int = 0) {
        if (vAmount == 0 && hAmount == 0) return
        val report = MouseReport()
        report.wheel = vAmount.coerceIn(-127, 127).toByte()
        report.horizontalWheel = hAmount.coerceIn(-127, 127).toByte()
        sendReportLocal(report)
        // Clear
        val clear = MouseReport()
        sendReportLocal(clear)
    }

    companion object {
        private const val TAG = "MouseSender"
    }
}
