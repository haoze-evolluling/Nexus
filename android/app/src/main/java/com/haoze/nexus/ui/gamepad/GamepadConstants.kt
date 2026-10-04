package com.haoze.nexus.ui.gamepad

// ── Gamepad Button Constants ──

object GamepadButton {
    const val A = 0          // Button 1 (Usage 0x01) -> BTN_A / BTN_SOUTH (Bottom face button)
    const val B = 1          // Button 2 (Usage 0x02) -> BTN_B / BTN_EAST (Right face button)
    const val X = 3          // Button 4 (Usage 0x04) -> BTN_X / BTN_NORTH (Left face button)
    const val Y = 4          // Button 5 (Usage 0x05) -> BTN_Y / BTN_WEST (Top face button)
    const val LB = 6         // Button 7 (Usage 0x07) -> BTN_TL (Left Bumper / L1)
    const val RB = 7         // Button 8 (Usage 0x08) -> BTN_TR (Right Bumper / R1)
    const val LT = 8         // Button 9 (Usage 0x09) -> BTN_TL2 (Left Trigger / L2)
    const val RT = 9         // Button 10 (Usage 0x0A) -> BTN_TR2 (Right Trigger / R2)
    const val SELECT = 10    // Button 11 (Usage 0x0B) -> BTN_SELECT (View / Create / Back)
    const val START = 11     // Button 12 (Usage 0x0C) -> BTN_START (Menu / Options / Start)
    const val GUIDE = 12     // Button 13 (Usage 0x0D) -> BTN_MODE (Xbox / PS / Home)
    const val L3 = 13        // Button 14 (Usage 0x0E) -> BTN_THUMBL (Left Stick Click)
    const val R3 = 14        // Button 15 (Usage 0x0F) -> BTN_THUMBR (Right Stick Click)
}

/**
 * Converts 4-direction D-pad bitmask to standard 8-way HID Hat Switch value:
 * 0 = Neutral/Released, 1 = Up, 2 = Up-Right, 3 = Right, 4 = Down-Right,
 * 5 = Down, 6 = Down-Left, 7 = Left, 8 = Up-Left.
 */
internal fun dpadMaskToHatSwitch(mask: Int): Int = when (mask) {
    1 -> 1              // UP
    1 or 8 -> 2         // UP + RIGHT (9)
    8 -> 3              // RIGHT
    2 or 8 -> 4         // DOWN + RIGHT (10)
    2 -> 5              // DOWN
    2 or 4 -> 6         // DOWN + LEFT (6)
    4 -> 7              // LEFT
    1 or 4 -> 8         // UP + LEFT (5)
    else -> 0           // Center / Released
}
