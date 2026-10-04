package com.haoze.nexus.ui.gamepad

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.edit
import com.haoze.nexus.ui.component.animation.rememberScreenEntranceTransition
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// ── Main View ──

@Composable
fun GamepadScreen(
    isConnected: Boolean,
    deviceName: String?,
    onExit: () -> Unit,
    onOpenKeyboard: () -> Unit,
    sendGamepadReport: (buttonMask: Int, hatSwitch: Int, leftX: Float, leftY: Float, rightX: Float, rightY: Float) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val entranceState = rememberScreenEntranceTransition()

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val config = CONSOLES[selectedIndex]

    var isEditMode by rememberSaveable { mutableStateOf(false) }

    var isVibrationEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("gamepad_vibration_enabled", true))
    }

    val triggerVibration = { milliseconds: Long ->
        if (isVibrationEnabled) {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    val layoutState = rememberGamepadLayoutState(config, sharedPrefs, triggerVibration)

    var buttonMask by remember { mutableIntStateOf(0) }
    var hatSwitchValue by remember { mutableIntStateOf(0) }
    var lastGamepadReportTime by remember { mutableLongStateOf(0L) }
    var isGamepadDirty by remember { mutableStateOf(false) }
    
    var leftStickX by remember { mutableFloatStateOf(0f) }
    var leftStickY by remember { mutableFloatStateOf(0f) }
    var rightStickX by remember { mutableFloatStateOf(0f) }
    var rightStickY by remember { mutableFloatStateOf(0f) }

    val transmitGamepadState = { force: Boolean ->
        val now = System.currentTimeMillis()
        if (force || now - lastGamepadReportTime >= 8L) {
            sendGamepadReport(
                buttonMask,
                hatSwitchValue,
                leftStickX,
                leftStickY,
                rightStickX,
                rightStickY
            )
            lastGamepadReportTime = now
            isGamepadDirty = false
        } else {
            isGamepadDirty = true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10L.milliseconds)
            if (isGamepadDirty) {
                sendGamepadReport(
                    buttonMask,
                    hatSwitchValue,
                    leftStickX,
                    leftStickY,
                    rightStickX,
                    rightStickY
                )
                lastGamepadReportTime = System.currentTimeMillis()
                isGamepadDirty = false
            }
        }
    }

    val pressButton = { bitIndex: Int ->
        buttonMask = buttonMask or (1 shl bitIndex)
        transmitGamepadState(true)
        triggerVibration(15)
    }
    val releaseButton = { bitIndex: Int ->
        buttonMask = buttonMask and (1 shl bitIndex).inv()
        transmitGamepadState(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF28282A))
            .navigationBarsPadding()
            .testTag("gamepad_view_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Settings Bar ──
            GamepadTopBar(
                entranceState = entranceState,
                isConnected = isConnected,
                deviceName = deviceName,
                isEditMode = isEditMode,
                isModified = layoutState.isModified,
                currentConsoleName = config.name,
                isVibrationEnabled = isVibrationEnabled,
                onExit = onExit,
                onOpenKeyboard = {
                    onOpenKeyboard()
                    triggerVibration(25)
                },
                onResetDefaults = { layoutState.resetDefaults() },
                onToggleEditMode = {
                    isEditMode = !isEditMode
                    triggerVibration(30)
                },
                onCycleConsole = {
                    selectedIndex = (selectedIndex + 1) % CONSOLES.size
                    triggerVibration(15)
                },
                onToggleVibration = {
                    val active = !isVibrationEnabled
                    isVibrationEnabled = active
                    sharedPrefs.edit { putBoolean("gamepad_vibration_enabled", active) }
                    if (active) triggerVibration(50)
                }
            )

            // ── Main Controls Body ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (config.leftStickAboveDpad) {
                    GamepadXboxLayout(
                        isEditMode = isEditMode,
                        config = config,
                        layout = layoutState,
                        buttonMask = buttonMask,
                        entranceState = entranceState,
                        scope = scope,
                        pressButton = pressButton,
                        releaseButton = releaseButton,
                        onMoveLeftStick = { x, y ->
                            leftStickX = x
                            leftStickY = y
                            transmitGamepadState(false)
                        },
                        onMoveRightStick = { x, y ->
                            rightStickX = x
                            rightStickY = y
                            transmitGamepadState(false)
                        },
                        onDpadChange = { mask ->
                            hatSwitchValue = dpadMaskToHatSwitch(mask)
                            transmitGamepadState(true)
                            if (mask != 0) triggerVibration(15)
                        }
                    )
                } else {
                    GamepadPlayStationLayout(
                        isEditMode = isEditMode,
                        config = config,
                        layout = layoutState,
                        buttonMask = buttonMask,
                        entranceState = entranceState,
                        scope = scope,
                        pressButton = pressButton,
                        releaseButton = releaseButton,
                        onMoveLeftStick = { x, y ->
                            leftStickX = x
                            leftStickY = y
                            transmitGamepadState(false)
                        },
                        onMoveRightStick = { x, y ->
                            rightStickX = x
                            rightStickY = y
                            transmitGamepadState(false)
                        },
                        onDpadChange = { mask ->
                            hatSwitchValue = dpadMaskToHatSwitch(mask)
                            transmitGamepadState(true)
                            if (mask != 0) triggerVibration(15)
                        }
                    )
                }
            }
        }
    }
}
