package com.haoze.nexus.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.ui.Routes

// ==========================================
// 外观与主题设置界面
// ==========================================

@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val context = LocalContext.current
    var colorStyle by remember { mutableStateOf(getThemeColorStyle(context)) }

    SettingsScaffold(
        title = stringResource(R.string.settings_section_appearance),
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
                SettingsSectionHeader(stringResource(R.string.settings_group_theme), icon = Icons.Default.Palette)
            }
            item {
                SettingsCard {
                    SettingsThemeModeSelector(
                        selectedMode = ThemeController.nightModeIndex,
                        onModeSelected = { ThemeController.setNightModeIndex(context, it) }
                    )
                }
            }
            item {
                SettingsSectionHeader(stringResource(R.string.settings_group_theme_color), icon = Icons.Default.Palette)
            }
            item {
                SettingsCard {
                    SettingsThemeColorPicker(
                        selectedStyle = colorStyle,
                        onStyleSelected = {
                            colorStyle = it
                            setThemeColorStyle(context, it)
                        }
                    )
                }
            }
            item {
                SettingsSectionHeader(stringResource(R.string.bottom_bar_customization), icon = Icons.Default.Palette)
            }
            item {
                SettingsCard {
                    SettingsActionItem(
                        title = stringResource(R.string.bottom_bar_customization),
                        subtitle = stringResource(R.string.bottom_bar_customization_subtitle),
                        onClick = { onNavigateToRoute(Routes.BOTTOM_BAR_CUSTOMIZATION) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayNightModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    SettingsScaffold(
        title = stringResource(R.string.settings_theme_mode),
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 16.dp)
        ) {
            SettingsCard {
                SettingsThemeModeSelector(
                    selectedMode = ThemeController.nightModeIndex,
                    onModeSelected = { ThemeController.setNightModeIndex(context, it) }
                )
            }
        }
    }
}

@Composable
fun ThemeColorSettingsScreen(
    onBack: () -> Unit,
    onThemeColorStyleChanged: (ThemeColorStyle) -> Unit
) {
    val context = LocalContext.current
    var selectedStyle by remember { mutableStateOf(getThemeColorStyle(context)) }

    SettingsScaffold(
        title = stringResource(R.string.settings_group_theme_color),
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 16.dp)
        ) {
            SettingsCard {
                SettingsThemeColorPicker(
                    selectedStyle = selectedStyle,
                    onStyleSelected = { style ->
                        selectedStyle = style
                        setThemeColorStyle(context, style)
                        onThemeColorStyleChanged(style)
                    }
                )
            }
        }
    }
}
