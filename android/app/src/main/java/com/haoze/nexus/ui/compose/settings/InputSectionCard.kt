package com.haoze.nexus.ui.compose.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.haoze.nexus.R
import com.haoze.nexus.ui.compose.SettingsCard
import com.haoze.nexus.ui.compose.SettingsItemDivider
import com.haoze.nexus.ui.compose.SettingsSliderItem
import com.haoze.nexus.ui.compose.SettingsSwitchItem

/**
 * 设置主页 - 触控与指针配置卡片 (触控板灵敏度、光标速度、自然滚动)
 */
@Composable
fun InputSettingsCard(
    touchpadSensitivity: Int,
    onSensitivityChanged: (Int) -> Unit,
    cursorSpeed: Int,
    onCursorSpeedChanged: (Int) -> Unit,
    naturalScroll: Boolean,
    onNaturalScrollChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(modifier = modifier) {
        SettingsSliderItem(
            title = stringResource(R.string.settings_touchpad_sensitivity),
            subtitle = stringResource(R.string.settings_touchpad_sensitivity_subtitle),
            value = touchpadSensitivity.toFloat(),
            onValueChange = { onSensitivityChanged(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8
        )

        SettingsItemDivider()

        SettingsSliderItem(
            title = stringResource(R.string.settings_cursor_speed),
            subtitle = stringResource(R.string.settings_cursor_speed_subtitle),
            value = cursorSpeed.toFloat(),
            onValueChange = { onCursorSpeedChanged(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8
        )

        SettingsItemDivider()

        SettingsSwitchItem(
            title = stringResource(R.string.settings_scroll_direction_natural),
            subtitle = stringResource(R.string.settings_scroll_direction_natural_subtitle),
            checked = naturalScroll,
            onCheckedChange = onNaturalScrollChanged
        )
    }
}
