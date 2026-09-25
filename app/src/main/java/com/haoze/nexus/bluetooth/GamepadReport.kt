package com.haoze.nexus.bluetooth

/**
 * Inline class wrapping a 16-byte HID gamepad report.
 *
 * 报文布局（与 [DescriptorCollection] 的 Gamepad TLC 严格对应）：
 * ```
 * [0]      Report ID = 4
 * [1..2]   16 个按键位，小端（bit 0..15）
 * [3]      Hat Switch：低 4 位有效（0..8），高 4 位填充
 * [4..5]   X  - 左摇杆 X
 * [6..7]   Y  - 左摇杆 Y
 * [8..9]   Z  - 右摇杆 X
 * [10..11] Rz - 右摇杆 Y
 * [12..13] Rx - 左扳机 LT
 * [14..15] Ry - 右扳机 RT
 * ```
 * 摇杆为无符号 16 位（0..65535，32768 = 中位），扳机为 0（松开）..65535（完全按下）。
 * Report ID = 4（1 = 键盘，2 = 鼠标，3 = 消费者控制）。
 */
@JvmInline
value class GamepadReport(
    val bytes: ByteArray = ByteArray(SIZE) { 0 }
) {

    init {
        bytes[0] = ID.toByte()
    }

    /** 16 button bits packed into 2 bytes, little-endian (bit index 0..15). */
    var buttonMask: Int
        get() = (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
        set(value) {
            bytes[1] = (value and 0xFF).toByte()
            bytes[2] = ((value shr 8) and 0xFF).toByte()
        }

    /**
     * Hat switch value (0 = centered/released, 1 = Up, 2 = Up-Right, 3 = Right,
     * 4 = Down-Right, 5 = Down, 6 = Down-Left, 7 = Left, 8 = Up-Left).
     * 0 落在描述符声明的 Logical Range 1..8 之外，即 Null State。
     */
    var hatSwitch: Int
        get() = bytes[3].toInt() and 0x0F
        set(value) {
            bytes[3] = (value and 0x0F).toByte()
        }

    /** 左摇杆 X → Usage(X)。 */
    var leftX: Int
        get() = readAxis(4)
        set(value) { writeAxis(4, value) }

    /** 左摇杆 Y → Usage(Y)。 */
    var leftY: Int
        get() = readAxis(6)
        set(value) { writeAxis(6, value) }

    /** 右摇杆 X → Usage(Z)。 */
    var rightX: Int
        get() = readAxis(8)
        set(value) { writeAxis(8, value) }

    /** 右摇杆 Y → Usage(Rz)。 */
    var rightY: Int
        get() = readAxis(10)
        set(value) { writeAxis(10, value) }

    /** 左扳机 LT → Usage(Rx)，0 = 松开。 */
    var leftTrigger: Int
        get() = readAxis(12)
        set(value) { writeAxis(12, value) }

    /** 右扳机 RT → Usage(Ry)，0 = 松开。 */
    var rightTrigger: Int
        get() = readAxis(14)
        set(value) { writeAxis(14, value) }

    private fun readAxis(offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun writeAxis(offset: Int, value: Int) {
        val clamped = value.coerceIn(0, AXIS_MAX)
        bytes[offset] = (clamped and 0xFF).toByte()
        bytes[offset + 1] = ((clamped shr 8) and 0xFF).toByte()
    }

    /**
     * Reset all fields except Report ID to center state
     * (buttons released, hat centered, sticks centered, triggers released).
     */
    fun reset() {
        buttonMask = 0
        hatSwitch = 0
        leftX = AXIS_CENTER
        leftY = AXIS_CENTER
        rightX = AXIS_CENTER
        rightY = AXIS_CENTER
        leftTrigger = TRIGGER_RELEASED
        rightTrigger = TRIGGER_RELEASED
    }

    companion object {
        /** Report ID = 4 for gamepad in both descriptors */
        const val ID = 4

        /** 完整报文长度（含 Report ID 字节）。 */
        const val SIZE = 16

        /** Axis neutral value (stick centered) */
        const val AXIS_CENTER = 32768

        /** 16 位轴上限 */
        const val AXIS_MAX = 65535

        /** 扳机完全松开 */
        const val TRIGGER_RELEASED = 0

        /** 扳机完全按下 */
        const val TRIGGER_MAX = AXIS_MAX

        // 按钮位与 UI 侧的 GamepadButton 常量一致
        const val BUTTON_LT = 8
        const val BUTTON_RT = 9
    }
}
