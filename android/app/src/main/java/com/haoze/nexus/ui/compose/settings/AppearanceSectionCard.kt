package com.haoze.nexus.ui.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.ui.compose.SettingsActionItem
import com.haoze.nexus.ui.compose.SettingsCard
import com.haoze.nexus.ui.compose.SettingsItemDivider
import com.haoze.nexus.ui.compose.SettingsThemeColorPicker
import com.haoze.nexus.ui.compose.SettingsThemeModeSelector
import com.haoze.nexus.ui.compose.ThemeColorStyle
import com.haoze.nexus.ui.compose.ThemeController

/**
 * 设置主页 - 外观与个性化配置卡片 (主题模式、强调色风格、底栏自定义入口)
 */
@Composable
fun AppearanceSettingsCard(
    currentThemeColorStyle: ThemeColorStyle,
    onThemeColorStyleSelected: (ThemeColorStyle) -> Unit,
    onNavigateToBottomBarCustomization: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    SettingsCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_group_theme),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsThemeModeSelector(
            selectedMode = ThemeController.nightModeIndex,
            onModeSelected = { modeIndex ->
                ThemeController.setNightModeIndex(context, modeIndex)
            }
        )

        SettingsItemDivider()

        SettingsThemeColorPicker(
            selectedStyle = currentThemeColorStyle,
            onStyleSelected = onThemeColorStyleSelected
        )

        if (onNavigateToBottomBarCustomization != null) {
            SettingsItemDivider()
            SettingsActionItem(
                title = stringResource(R.string.bottom_bar_customization),
                subtitle = stringResource(R.string.bottom_bar_customization_subtitle),
                onClick = onNavigateToBottomBarCustomization
            )
        }
    }
}
