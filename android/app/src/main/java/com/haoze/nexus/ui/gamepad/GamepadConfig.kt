package com.haoze.nexus.ui.gamepad

import androidx.compose.ui.graphics.Color

// ── Console configuration ──

data class ButtonDef(
    val label: String,
    val mappingId: Int,
    val color: Color = Color(0xFF3A3D42)
)

data class ConsoleConfig(
    val id: String,
    val name: String,
    val faceTop: ButtonDef,
    val faceRight: ButtonDef,
    val faceBottom: ButtonDef,
    val faceLeft: ButtonDef,
    val leftBumper: ButtonDef,
    val rightBumper: ButtonDef,
    val leftTrigger: ButtonDef,
    val rightTrigger: ButtonDef,
    val selectButton: ButtonDef,
    val startButton: ButtonDef,
    val guideButton: ButtonDef,
    val leftStickAboveDpad: Boolean = true
)

val CONSOLES = listOf(
    ConsoleConfig(
        id = "xbox_series",
        name = "Xbox Series",
        faceTop = ButtonDef("Y", GamepadButton.Y, Color(0xFFFFCA28)),
        faceRight = ButtonDef("B", GamepadButton.B, Color(0xFFEF5350)),
        faceBottom = ButtonDef("A", GamepadButton.A, Color(0xFF66BB6A)),
        faceLeft = ButtonDef("X", GamepadButton.X, Color(0xFF42A5F5)),
        leftBumper = ButtonDef("LB", GamepadButton.LB),
        rightBumper = ButtonDef("RB", GamepadButton.RB),
        leftTrigger = ButtonDef("LT", GamepadButton.LT),
        rightTrigger = ButtonDef("RT", GamepadButton.RT),
        selectButton = ButtonDef("VIEW", GamepadButton.SELECT),
        startButton = ButtonDef("MENU", GamepadButton.START),
        guideButton = ButtonDef("XBOX", GamepadButton.GUIDE, Color(0xFF2E7D32)),
        leftStickAboveDpad = true
    ),
    ConsoleConfig(
        id = "playstation_5",
        name = "PlayStation 5",
        faceTop = ButtonDef("△", GamepadButton.Y, Color(0xFF4DB6AC)),
        faceRight = ButtonDef("◯", GamepadButton.B, Color(0xFFEF5350)),
        faceBottom = ButtonDef("✕", GamepadButton.A, Color(0xFF5C6BC0)),
        faceLeft = ButtonDef("☐", GamepadButton.X, Color(0xFFEC407A)),
        leftBumper = ButtonDef("L1", GamepadButton.LB),
        rightBumper = ButtonDef("R1", GamepadButton.RB),
        leftTrigger = ButtonDef("L2", GamepadButton.LT),
        rightTrigger = ButtonDef("R2", GamepadButton.RT),
        selectButton = ButtonDef("CREATE", GamepadButton.SELECT),
        startButton = ButtonDef("OPTIONS", GamepadButton.START),
        guideButton = ButtonDef("PS", GamepadButton.GUIDE, Color(0xFF1565C0)),
        leftStickAboveDpad = false
    )
)
