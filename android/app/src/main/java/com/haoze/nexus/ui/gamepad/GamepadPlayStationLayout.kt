package com.haoze.nexus.ui.gamepad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haoze.nexus.ui.component.animation.ScreenEntranceState
import com.haoze.nexus.ui.component.animation.screenEntranceCenter
import com.haoze.nexus.ui.component.animation.screenEntranceLeftWing
import com.haoze.nexus.ui.component.animation.screenEntranceRightWing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun GamepadPlayStationLayout(
    isEditMode: Boolean,
    config: ConsoleConfig,
    layout: GamepadLayoutState,
    buttonMask: Int,
    entranceState: ScreenEntranceState,
    scope: CoroutineScope,
    pressButton: (Int) -> Unit,
    releaseButton: (Int) -> Unit,
    onMoveLeftStick: (Float, Float) -> Unit,
    onMoveRightStick: (Float, Float) -> Unit,
    onDpadChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── PLAYSTATION-STYLE SYMMETRICAL LAYOUT ──
        // Left Column: split Dpad (top), Left Stick (bottom)
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .screenEntranceLeftWing(entranceState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(26.dp))
            EditableComponentWrapper(
                isEditMode = isEditMode,
                offsetX = layout.dpadOffsetX,
                offsetY = layout.dpadOffsetY,
                scale = layout.dpadScale,
                onOffsetChange = { x, y ->
                    layout.dpadOffsetX = x
                    layout.dpadOffsetY = y
                    layout.saveLayoutPref("${config.id}_dpad_x", x)
                    layout.saveLayoutPref("${config.id}_dpad_y", y)
                },
                onScaleChange = { s ->
                    layout.dpadScale = s
                    layout.saveLayoutPref("${config.id}_dpad_scale", s)
                }
            ) {
                GamepadDpad(
                    isXboxStyle = false,
                    onDpadChange = onDpadChange
                )
            }
            Spacer(Modifier.height(16.dp))
            EditableComponentWrapper(
                isEditMode = isEditMode,
                offsetX = layout.leftStickOffsetX,
                offsetY = layout.leftStickOffsetY,
                scale = layout.leftStickScale,
                onOffsetChange = { x, y ->
                    layout.leftStickOffsetX = x
                    layout.leftStickOffsetY = y
                    layout.saveLayoutPref("${config.id}_left_stick_x", x)
                    layout.saveLayoutPref("${config.id}_left_stick_y", y)
                },
                onScaleChange = { s ->
                    layout.leftStickScale = s
                    layout.saveLayoutPref("${config.id}_left_stick_scale", s)
                }
            ) {
                GamepadAnalogStick(
                    label = "L",
                    isClicked = (buttonMask and (1 shl GamepadButton.L3)) != 0,
                    isHeld = (buttonMask and (1 shl GamepadButton.L3)) != 0,
                    onMove = onMoveLeftStick,
                    onStickClick = {
                        scope.launch {
                            pressButton(GamepadButton.L3)
                            delay(100L.milliseconds)
                            releaseButton(GamepadButton.L3)
                        }
                    },
                    onToggleHold = { hold ->
                        if (hold) pressButton(GamepadButton.L3) else releaseButton(GamepadButton.L3)
                    }
                )
            }
        }

        // Center Column: PS Button + L1/L2 R1/R2, Right Stick/Left Stick spacing
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .screenEntranceCenter(entranceState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(16.dp))
            // Bumper/Trigger stacks
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EditableComponentWrapper(
                        isEditMode = isEditMode,
                        offsetX = layout.leftTriggerOffsetX,
                        offsetY = layout.leftTriggerOffsetY,
                        scale = layout.leftTriggerScale,
                        onOffsetChange = { x, y ->
                            layout.leftTriggerOffsetX = x
                            layout.leftTriggerOffsetY = y
                            layout.saveLayoutPref("${config.id}_left_trigger_x", x)
                            layout.saveLayoutPref("${config.id}_left_trigger_y", y)
                        },
                        onScaleChange = { s ->
                            layout.leftTriggerScale = s
                            layout.saveLayoutPref("${config.id}_left_trigger_scale", s)
                        }
                    ) {
                        GamepadTriggerButton(config.leftTrigger, true, pressButton, releaseButton)
                    }
                    Spacer(Modifier.height(4.dp))
                    EditableComponentWrapper(
                        isEditMode = isEditMode,
                        offsetX = layout.leftBumperOffsetX,
                        offsetY = layout.leftBumperOffsetY,
                        scale = layout.leftBumperScale,
                        onOffsetChange = { x, y ->
                            layout.leftBumperOffsetX = x
                            layout.leftBumperOffsetY = y
                            layout.saveLayoutPref("${config.id}_left_bumper_x", x)
                            layout.saveLayoutPref("${config.id}_left_bumper_y", y)
                        },
                        onScaleChange = { s ->
                            layout.leftBumperScale = s
                            layout.saveLayoutPref("${config.id}_left_bumper_scale", s)
                        }
                    ) {
                        GamepadBumperButton(config.leftBumper, true, pressButton, releaseButton)
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EditableComponentWrapper(
                        isEditMode = isEditMode,
                        offsetX = layout.rightTriggerOffsetX,
                        offsetY = layout.rightTriggerOffsetY,
                        scale = layout.rightTriggerScale,
                        onOffsetChange = { x, y ->
                            layout.rightTriggerOffsetX = x
                            layout.rightTriggerOffsetY = y
                            layout.saveLayoutPref("${config.id}_right_trigger_x", x)
                            layout.saveLayoutPref("${config.id}_right_trigger_y", y)
                        },
                        onScaleChange = { s ->
                            layout.rightTriggerScale = s
                            layout.saveLayoutPref("${config.id}_right_trigger_scale", s)
                        }
                    ) {
                        GamepadTriggerButton(config.rightTrigger, false, pressButton, releaseButton)
                    }
                    Spacer(Modifier.height(4.dp))
                    EditableComponentWrapper(
                        isEditMode = isEditMode,
                        offsetX = layout.rightBumperOffsetX,
                        offsetY = layout.rightBumperOffsetY,
                        scale = layout.rightBumperScale,
                        onOffsetChange = { x, y ->
                            layout.rightBumperOffsetX = x
                            layout.rightBumperOffsetY = y
                            layout.saveLayoutPref("${config.id}_right_bumper_x", x)
                            layout.saveLayoutPref("${config.id}_right_bumper_y", y)
                        },
                        onScaleChange = { s ->
                            layout.rightBumperScale = s
                            layout.saveLayoutPref("${config.id}_right_bumper_scale", s)
                        }
                    ) {
                        GamepadBumperButton(config.rightBumper, false, pressButton, releaseButton)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // PlayStation Guide button
            EditableComponentWrapper(
                isEditMode = isEditMode,
                offsetX = layout.guideOffsetX,
                offsetY = layout.guideOffsetY,
                scale = layout.guideScale,
                onOffsetChange = { x, y ->
                    layout.guideOffsetX = x
                    layout.guideOffsetY = y
                    layout.saveLayoutPref("${config.id}_guide_x", x)
                    layout.saveLayoutPref("${config.id}_guide_y", y)
                },
                onScaleChange = { s ->
                    layout.guideScale = s
                    layout.saveLayoutPref("${config.id}_guide_scale", s)
                }
            ) {
                PlayStationLogoButton(config.guideButton, pressButton, releaseButton)
            }

            Spacer(Modifier.height(12.dp))

            // Center buttons (Select, Start) flanking the area below Guide
            Row(
                modifier = Modifier.fillMaxWidth(0.75f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditableComponentWrapper(
                    isEditMode = isEditMode,
                    offsetX = layout.selectOffsetX,
                    offsetY = layout.selectOffsetY,
                    scale = layout.selectScale,
                    onOffsetChange = { x, y ->
                        layout.selectOffsetX = x
                        layout.selectOffsetY = y
                        layout.saveLayoutPref("${config.id}_select_x", x)
                        layout.saveLayoutPref("${config.id}_select_y", y)
                    },
                    onScaleChange = { s ->
                        layout.selectScale = s
                        layout.saveLayoutPref("${config.id}_select_scale", s)
                    }
                ) {
                    GamepadCenterButton(config.selectButton, pressButton, releaseButton)
                }
                EditableComponentWrapper(
                    isEditMode = isEditMode,
                    offsetX = layout.startOffsetX,
                    offsetY = layout.startOffsetY,
                    scale = layout.startScale,
                    onOffsetChange = { x, y ->
                        layout.startOffsetX = x
                        layout.startOffsetY = y
                        layout.saveLayoutPref("${config.id}_start_x", x)
                        layout.saveLayoutPref("${config.id}_start_y", y)
                    },
                    onScaleChange = { s ->
                        layout.startScale = s
                        layout.saveLayoutPref("${config.id}_start_scale", s)
                    }
                ) {
                    GamepadCenterButton(config.startButton, pressButton, releaseButton)
                }
            }
            Spacer(Modifier.weight(1f))
        }

        // Right Column: Face Buttons (top), Right Stick (bottom)
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .screenEntranceRightWing(entranceState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(16.dp))
            EditableComponentWrapper(
                isEditMode = isEditMode,
                offsetX = layout.faceButtonsOffsetX,
                offsetY = layout.faceButtonsOffsetY,
                scale = layout.faceButtonsScale,
                onOffsetChange = { x, y ->
                    layout.faceButtonsOffsetX = x
                    layout.faceButtonsOffsetY = y
                    layout.saveLayoutPref("${config.id}_face_buttons_x", x)
                    layout.saveLayoutPref("${config.id}_face_buttons_y", y)
                },
                onScaleChange = { s ->
                    layout.faceButtonsScale = s
                    layout.saveLayoutPref("${config.id}_face_buttons_scale", s)
                }
            ) {
                FaceButtonsDiamond(
                    config = config,
                    isXboxStyle = false,
                    onPress = pressButton,
                    onRelease = releaseButton
                )
            }
            Spacer(Modifier.height(6.dp))
            EditableComponentWrapper(
                isEditMode = isEditMode,
                offsetX = layout.rightStickOffsetX,
                offsetY = layout.rightStickOffsetY,
                scale = layout.rightStickScale,
                onOffsetChange = { x, y ->
                    layout.rightStickOffsetX = x
                    layout.rightStickOffsetY = y
                    layout.saveLayoutPref("${config.id}_right_stick_x", x)
                    layout.saveLayoutPref("${config.id}_right_stick_y", y)
                },
                onScaleChange = { s ->
                    layout.rightStickScale = s
                    layout.saveLayoutPref("${config.id}_right_stick_scale", s)
                }
            ) {
                GamepadAnalogStick(
                    label = "R",
                    isClicked = (buttonMask and (1 shl GamepadButton.R3)) != 0,
                    isHeld = (buttonMask and (1 shl GamepadButton.R3)) != 0,
                    onMove = onMoveRightStick,
                    onStickClick = {
                        scope.launch {
                            pressButton(GamepadButton.R3)
                            delay(100L.milliseconds)
                            releaseButton(GamepadButton.R3)
                        }
                    },
                    onToggleHold = { hold ->
                        if (hold) pressButton(GamepadButton.R3) else releaseButton(GamepadButton.R3)
                    }
                )
            }
        }
    }
}
