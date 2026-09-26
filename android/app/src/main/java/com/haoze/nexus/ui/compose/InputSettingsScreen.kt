package com.haoze.nexus.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R

// ==========================================
// 触控与指针输入设置界面
// ==========================================

@Composable
fun InputSettingsScreen(onBack: () -> Unit) {
    val prefs = settingsPrefs()
    var sensitivity by rememberIntSetting(prefs, "touchpad_sensitivity", 5)
    var cursorSpeed by rememberIntSetting(prefs, "cursor_speed", 5)
    var naturalScroll by rememberBooleanSetting(prefs, "scroll_direction_natural", false)

    SettingsScaffold(
        title = stringResource(R.string.settings_section_input),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(SettingsSectionSpacing)
        ) {
            item {
                SettingsCard {
                    SettingsSliderItem(
                        title = stringResource(R.string.settings_touchpad_sensitivity),
                        subtitle = stringResource(R.string.settings_touchpad_sensitivity_subtitle),
                        value = sensitivity.toFloat(),
                        onValueChange = {
                            sensitivity = it.toInt()
                            prefs.edit().putInt("touchpad_sensitivity", sensitivity).apply()
                        },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    SettingsItemDivider()
                    SettingsSliderItem(
                        title = stringResource(R.string.settings_cursor_speed),
                        subtitle = stringResource(R.string.settings_cursor_speed_subtitle),
                        value = cursorSpeed.toFloat(),
                        onValueChange = {
                            cursorSpeed = it.toInt()
                            prefs.edit().putInt("cursor_speed", cursorSpeed).apply()
                        },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    SettingsItemDivider()
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_scroll_direction_natural),
                        subtitle = stringResource(R.string.settings_scroll_direction_natural_subtitle),
                        checked = naturalScroll,
                        onCheckedChange = {
                            naturalScroll = it
                            prefs.edit().putBoolean("scroll_direction_natural", it).apply()
                        }
                    )
                }
            }
        }
    }
}
