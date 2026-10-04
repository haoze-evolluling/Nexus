package com.haoze.nexus.ui.gamepad

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit

internal class GamepadLayoutState(
    val config: ConsoleConfig,
    private val sharedPrefs: SharedPreferences,
    private val triggerVibration: (Long) -> Unit
) {
    val defaultDpadX = if (config.id == "playstation_5") -24f else 32f
    val defaultDpadY = if (config.id == "playstation_5") 0f else 20f
    val defaultLeftX = if (config.id == "playstation_5") 45f else 0f
    val defaultLeftY = if (config.id == "playstation_5") 20f else 0f
    val defaultRightX = -45f
    val defaultRightY = 20f

    var dpadOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_dpad_x", defaultDpadX))
    var dpadOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_dpad_y", defaultDpadY))
    var dpadScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_dpad_scale", 1f))

    var leftStickOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_left_stick_x", defaultLeftX))
    var leftStickOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_left_stick_y", defaultLeftY))
    var leftStickScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_left_stick_scale", 1f))

    var rightStickOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_right_stick_x", defaultRightX))
    var rightStickOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_right_stick_y", defaultRightY))
    var rightStickScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_right_stick_scale", 1f))

    var faceButtonsOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_face_buttons_x", 0f))
    var faceButtonsOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_face_buttons_y", 0f))
    var faceButtonsScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_face_buttons_scale", 1f))

    var leftTriggerOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_left_trigger_x", 0f))
    var leftTriggerOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_left_trigger_y", 0f))
    var leftTriggerScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_left_trigger_scale", 1f))

    var leftBumperOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_left_bumper_x", 0f))
    var leftBumperOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_left_bumper_y", 0f))
    var leftBumperScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_left_bumper_scale", 1f))

    var rightTriggerOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_right_trigger_x", 0f))
    var rightTriggerOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_right_trigger_y", 0f))
    var rightTriggerScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_right_trigger_scale", 1f))

    var rightBumperOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_right_bumper_x", 0f))
    var rightBumperOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_right_bumper_y", 0f))
    var rightBumperScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_right_bumper_scale", 1f))

    var guideOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_guide_x", 0f))
    var guideOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_guide_y", 0f))
    var guideScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_guide_scale", 1f))

    var selectOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_select_x", 0f))
    var selectOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_select_y", 0f))
    var selectScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_select_scale", 1f))

    var startOffsetX by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_start_x", 0f))
    var startOffsetY by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_start_y", 0f))
    var startScale by mutableFloatStateOf(sharedPrefs.getFloat("${config.id}_start_scale", 1f))

    val isModified: Boolean
        get() = dpadOffsetX != defaultDpadX || dpadOffsetY != defaultDpadY || dpadScale != 1f ||
                leftStickOffsetX != defaultLeftX || leftStickOffsetY != defaultLeftY || leftStickScale != 1f ||
                rightStickOffsetX != defaultRightX || rightStickOffsetY != defaultRightY || rightStickScale != 1f ||
                faceButtonsOffsetX != 0f || faceButtonsOffsetY != 0f || faceButtonsScale != 1f ||
                leftTriggerOffsetX != 0f || leftTriggerOffsetY != 0f || leftTriggerScale != 1f ||
                leftBumperOffsetX != 0f || leftBumperOffsetY != 0f || leftBumperScale != 1f ||
                rightTriggerOffsetX != 0f || rightTriggerOffsetY != 0f || rightTriggerScale != 1f ||
                rightBumperOffsetX != 0f || rightBumperOffsetY != 0f || rightBumperScale != 1f ||
                guideOffsetX != 0f || guideOffsetY != 0f || guideScale != 1f ||
                selectOffsetX != 0f || selectOffsetY != 0f || selectScale != 1f ||
                startOffsetX != 0f || startOffsetY != 0f || startScale != 1f

    fun saveLayoutPref(key: String, value: Float) {
        sharedPrefs.edit { putFloat(key, value) }
    }

    fun resetDefaults() {
        dpadOffsetX = defaultDpadX
        dpadOffsetY = defaultDpadY
        dpadScale = 1f

        leftStickOffsetX = defaultLeftX
        leftStickOffsetY = defaultLeftY
        leftStickScale = 1f

        rightStickOffsetX = defaultRightX
        rightStickOffsetY = defaultRightY
        rightStickScale = 1f

        faceButtonsOffsetX = 0f
        faceButtonsOffsetY = 0f
        faceButtonsScale = 1f

        leftTriggerOffsetX = 0f
        leftTriggerOffsetY = 0f
        leftTriggerScale = 1f

        leftBumperOffsetX = 0f
        leftBumperOffsetY = 0f
        leftBumperScale = 1f

        rightTriggerOffsetX = 0f
        rightTriggerOffsetY = 0f
        rightTriggerScale = 1f

        rightBumperOffsetX = 0f
        rightBumperOffsetY = 0f
        rightBumperScale = 1f

        guideOffsetX = 0f
        guideOffsetY = 0f
        guideScale = 1f

        selectOffsetX = 0f
        selectOffsetY = 0f
        selectScale = 1f

        startOffsetX = 0f
        startOffsetY = 0f
        startScale = 1f

        sharedPrefs.edit {
            remove("${config.id}_dpad_x")
            remove("${config.id}_dpad_y")
            remove("${config.id}_dpad_scale")
            remove("${config.id}_left_stick_x")
            remove("${config.id}_left_stick_y")
            remove("${config.id}_left_stick_scale")
            remove("${config.id}_right_stick_x")
            remove("${config.id}_right_stick_y")
            remove("${config.id}_right_stick_scale")
            remove("${config.id}_face_buttons_x")
            remove("${config.id}_face_buttons_y")
            remove("${config.id}_face_buttons_scale")
            remove("${config.id}_left_trigger_x")
            remove("${config.id}_left_trigger_y")
            remove("${config.id}_left_trigger_scale")
            remove("${config.id}_left_bumper_x")
            remove("${config.id}_left_bumper_y")
            remove("${config.id}_left_bumper_scale")
            remove("${config.id}_right_trigger_x")
            remove("${config.id}_right_trigger_y")
            remove("${config.id}_right_trigger_scale")
            remove("${config.id}_right_bumper_x")
            remove("${config.id}_right_bumper_y")
            remove("${config.id}_right_bumper_scale")
            remove("${config.id}_guide_x")
            remove("${config.id}_guide_y")
            remove("${config.id}_guide_scale")
            remove("${config.id}_select_x")
            remove("${config.id}_select_y")
            remove("${config.id}_select_scale")
            remove("${config.id}_start_x")
            remove("${config.id}_start_y")
            remove("${config.id}_start_scale")
        }

        triggerVibration(50)
    }
}

@Composable
internal fun rememberGamepadLayoutState(
    config: ConsoleConfig,
    sharedPrefs: SharedPreferences,
    triggerVibration: (Long) -> Unit
): GamepadLayoutState {
    return remember(config.id) {
        GamepadLayoutState(config, sharedPrefs, triggerVibration)
    }
}
