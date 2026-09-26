package com.haoze.nexus.ui.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.haoze.nexus.R
import com.haoze.nexus.ui.compose.SettingsCard
import com.haoze.nexus.ui.compose.SettingsItemDivider
import com.haoze.nexus.ui.compose.SettingsSwitchItem

/**
 * 设置主页 - 声音与触感反馈配置卡片 (触觉反馈、按键音效、手柄振动)
 */
@Composable
fun FeedbackSettingsCard(
    hapticFeedback: Boolean,
    onHapticFeedbackChanged: (Boolean) -> Unit,
    keySound: Boolean,
    onKeySoundChanged: (Boolean) -> Unit,
    gamepadVibration: Boolean,
    onGamepadVibrationChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(modifier = modifier) {
        SettingsSwitchItem(
            title = stringResource(R.string.settings_haptic_feedback),
            subtitle = stringResource(R.string.settings_haptic_feedback_subtitle),
            checked = hapticFeedback,
            onCheckedChange = onHapticFeedbackChanged
        )

        SettingsItemDivider()

        SettingsSwitchItem(
            title = stringResource(R.string.settings_key_sound),
            subtitle = stringResource(R.string.settings_key_sound_subtitle),
            checked = keySound,
            onCheckedChange = onKeySoundChanged
        )

        SettingsItemDivider()

        SettingsSwitchItem(
            title = stringResource(R.string.settings_gamepad_vibration),
            subtitle = stringResource(R.string.settings_gamepad_vibration_subtitle),
            checked = gamepadVibration,
            onCheckedChange = onGamepadVibrationChanged
        )
    }
}
