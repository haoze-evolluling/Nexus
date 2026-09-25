package com.haoze.claudekeyboard.ui.compose

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.haoze.claudekeyboard.R
import com.haoze.claudekeyboard.bluetooth.HidProfile
import com.haoze.claudekeyboard.ui.Routes

/**
 * 设置页一级界面：
 * 将软件所有设置项拆分并重组平铺在一级界面中，无需二级/三级路由跳转。
 * 整体 UI 严格遵循 Material 3 (MD3) 设计体系。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToRoute: ((String) -> Unit)? = null,
    onThemeColorStyleChanged: (ThemeColorStyle) -> Unit = {},
    onBooleanSettingChanged: (String, Boolean) -> Unit = { _, _ -> },
    inputProfile: HidProfile = HidProfile.KEYBOARD_MOUSE,
    onInputProfileChanged: (HidProfile) -> Boolean = { true },
    onResetMacros: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { settingsPrefs(context) }

    // 主题与颜色状态
    var currentThemeColorStyle by remember { mutableStateOf(getThemeColorStyle(context)) }

    // 触控与光标设置状态
    var touchpadSensitivity by rememberIntSetting(prefs, "touchpad_sensitivity", 5)
    var cursorSpeed by rememberIntSetting(prefs, "cursor_speed", 5)
    var naturalScroll by rememberBooleanSetting(prefs, "scroll_direction_natural", false)

    // 声音与触感反馈状态
    var hapticFeedback by rememberBooleanSetting(prefs, "haptic_feedback", true)
    var keySound by rememberBooleanSetting(prefs, "key_sound_enabled", true)
    var gamepadVibration by rememberBooleanSetting(prefs, "gamepad_vibration_enabled", true)

    // 连接与行为状态
    var autoConnect by rememberBooleanSetting(prefs, "auto_connect_on_launch", true)
    var autoReconnect by rememberBooleanSetting(prefs, "auto_reconnect_on_disconnect", true)
    var keepScreenOn by rememberBooleanSetting(prefs, "keep_screen_on", true)
    var connectionNotifications by rememberBooleanSetting(prefs, "connection_notifications", true)

    // 重置宏确认对话框状态
    var showResetMacrosConfirm by remember { mutableStateOf(false) }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

    val switchProfile: (HidProfile) -> Unit = { profile ->
        if (profile != inputProfile && !onInputProfileChanged(profile)) {
            Toast.makeText(context, R.string.settings_device_type_switch_failed, Toast.LENGTH_SHORT).show()
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.home_settings_title),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ==========================================
            // 板块 1：外观与个性化 (Appearance)
            // ==========================================
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_section_appearance_title),
                    icon = Icons.Default.Palette
                )
            }
            item {
                SettingsCard {
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
                        onStyleSelected = { style ->
                            currentThemeColorStyle = style
                            setThemeColorStyle(context, style)
                            onThemeColorStyleChanged(style)
                        }
                    )
                }
            }

            // ==========================================
            // 板块 2：触控与指针 (Touchpad & Pointer)
            // ==========================================
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_section_input_title),
                    icon = Icons.Default.Mouse
                )
            }
            item {
                SettingsCard {
                    SettingsSliderItem(
                        title = stringResource(R.string.settings_touchpad_sensitivity),
                        subtitle = stringResource(R.string.settings_touchpad_sensitivity_subtitle),
                        value = touchpadSensitivity.toFloat(),
                        onValueChange = { value ->
                            touchpadSensitivity = value.toInt()
                            prefs.edit().putInt("touchpad_sensitivity", touchpadSensitivity).apply()
                        },
                        valueRange = 1f..10f,
                        steps = 8
                    )

                    SettingsItemDivider()

                    SettingsSliderItem(
                        title = stringResource(R.string.settings_cursor_speed),
                        subtitle = stringResource(R.string.settings_cursor_speed_subtitle),
                        value = cursorSpeed.toFloat(),
                        onValueChange = { value ->
                            cursorSpeed = value.toInt()
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
                        onCheckedChange = { checked ->
                            naturalScroll = checked
                            prefs.edit().putBoolean("scroll_direction_natural", checked).apply()
                        }
                    )
                }
            }

            // ==========================================
            // 板块 3：声音与触感反馈 (Audio & Feedback)
            // ==========================================
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_section_feedback_title),
                    icon = Icons.Default.Vibration
                )
            }
            item {
                SettingsCard {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_haptic_feedback),
                        subtitle = stringResource(R.string.settings_haptic_feedback_subtitle),
                        checked = hapticFeedback,
                        onCheckedChange = { checked ->
                            hapticFeedback = checked
                            prefs.edit().putBoolean("haptic_feedback", checked).apply()
                        }
                    )

                    SettingsItemDivider()

                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_key_sound),
                        subtitle = stringResource(R.string.settings_key_sound_subtitle),
                        checked = keySound,
                        onCheckedChange = { checked ->
                            keySound = checked
                            prefs.edit().putBoolean("key_sound_enabled", checked).apply()
                        }
                    )

                    SettingsItemDivider()

                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_gamepad_vibration),
                        subtitle = stringResource(R.string.settings_gamepad_vibration_subtitle),
                        checked = gamepadVibration,
                        onCheckedChange = { checked ->
                            gamepadVibration = checked
                            prefs.edit().putBoolean("gamepad_vibration_enabled", checked).apply()
                        }
                    )
                }
            }

            // ==========================================
            // 板块 4：设备与连接 (Device & Connection)
            // ==========================================
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_section_connection_title),
                    icon = Icons.Default.Bluetooth
                )
            }
            item {
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_group_device_type),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (inputProfile == HidProfile.KEYBOARD_MOUSE) {
                                stringResource(R.string.settings_device_type_keyboard_mouse_subtitle)
                            } else {
                                stringResource(R.string.settings_device_type_gamepad_subtitle)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val profiles = listOf(
                        Triple(HidProfile.KEYBOARD_MOUSE, stringResource(R.string.settings_device_type_keyboard_mouse), Icons.Default.Mouse),
                        Triple(HidProfile.GAMEPAD, stringResource(R.string.settings_device_type_gamepad), Icons.Default.SportsEsports)
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        profiles.forEachIndexed { index, (profile, label, icon) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = profiles.size),
                                onClick = { switchProfile(profile) },
                                selected = inputProfile == profile,
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = inputProfile == profile) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                        )
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.settings_device_type_switch_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )

                    SettingsItemDivider()

                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_auto_connect_launch),
                        subtitle = stringResource(R.string.settings_auto_connect_launch_subtitle),
                        checked = autoConnect,
                        onCheckedChange = { checked ->
                            autoConnect = checked
                            saveBoolean(prefs, "auto_connect_on_launch", checked, onBooleanSettingChanged)
                        }
                    )

                    SettingsItemDivider()

                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_auto_reconnect),
                        subtitle = stringResource(R.string.settings_auto_reconnect_subtitle),
                        checked = autoReconnect,
                        onCheckedChange = { checked ->
                            autoReconnect = checked
                            saveBoolean(prefs, "auto_reconnect_on_disconnect", checked, onBooleanSettingChanged)
                        }
                    )

                    SettingsItemDivider()

                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_keep_screen_on),
                        subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
                        checked = keepScreenOn,
                        onCheckedChange = { checked ->
                            keepScreenOn = checked
                            saveBoolean(prefs, "keep_screen_on", checked, onBooleanSettingChanged)
                        }
                    )

                    SettingsItemDivider()

                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_connection_notifications),
                        subtitle = stringResource(R.string.settings_connection_notifications_subtitle),
                        checked = connectionNotifications,
                        onCheckedChange = { checked ->
                            connectionNotifications = checked
                            saveBoolean(prefs, "connection_notifications", checked, onBooleanSettingChanged)
                        }
                    )
                }
            }

            // ==========================================
            // 板块 5：数据与管理 (Data Management)
            // ==========================================
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_section_data_title),
                    icon = Icons.Default.DeleteSweep
                )
            }
            item {
                SettingsCard {
                    SettingsActionItem(
                        title = stringResource(R.string.settings_reset_macros),
                        subtitle = stringResource(R.string.dialog_reset_macros_confirm),
                        titleColor = MaterialTheme.colorScheme.error,
                        leadingIcon = Icons.Default.Restore,
                        onClick = { showResetMacrosConfirm = true },
                        trailing = {
                            TextButton(onClick = { showResetMacrosConfirm = true }) {
                                Text(
                                    stringResource(R.string.dialog_reset),
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
            }

            // ==========================================
            // 板块 6：关于与支持 (About & Support)
            // ==========================================
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_section_about_title),
                    icon = Icons.Default.Info
                )
            }
            item {
                SettingsCard {
                    SettingsActionItem(
                        title = stringResource(R.string.home_about_title),
                        subtitle = if (versionName.isNotBlank()) "版本 $versionName · 查看核心能力与运行边界" else "查看核心能力与运行边界",
                        leadingIcon = Icons.Default.Info,
                        onClick = { onNavigateToRoute?.invoke(Routes.ABOUT) },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsItemDivider()

                    SettingsActionItem(
                        title = stringResource(R.string.home_sponsor_title),
                        subtitle = "请作者喝杯蜜雪，支持项目持续维护",
                        leadingIcon = Icons.Default.Favorite,
                        onClick = { onNavigateToRoute?.invoke(Routes.SPONSOR) },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsItemDivider()

                    SettingsActionItem(
                        title = stringResource(R.string.home_sponsor_list_title),
                        subtitle = "感谢所有支持 SyncTouch 的朋友",
                        leadingIcon = Icons.Default.WorkspacePremium,
                        onClick = { onNavigateToRoute?.invoke(Routes.SPONSOR_LIST) },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsItemDivider()

                    SettingsActionItem(
                        title = stringResource(R.string.settings_github_repo),
                        subtitle = stringResource(R.string.settings_github_url),
                        leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/haoze-evolluling/SyncTouch"))
                                )
                            }
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }

            // 底部版本标语
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "SyncTouch ${if (versionName.isNotBlank()) "v$versionName" else ""} · Control at Your Fingertips",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showResetMacrosConfirm) {
        AppAlertDialog(
            onDismissRequest = { showResetMacrosConfirm = false },
            title = { Text(stringResource(R.string.settings_reset_macros)) },
            text = { Text(stringResource(R.string.dialog_reset_macros_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onResetMacros()
                    showResetMacrosConfirm = false
                    Toast.makeText(context, "已恢复默认快捷命令", Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.dialog_reset), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetMacrosConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

// ==========================================
// 兼容性保留子屏幕 (防止历史路由调用处编译报错)
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

@Composable
fun FeedbackSettingsScreen(onBack: () -> Unit) {
    val prefs = settingsPrefs()
    var haptic by rememberBooleanSetting(prefs, "haptic_feedback", true)

    SettingsScaffold(
        title = stringResource(R.string.settings_section_feedback),
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 16.dp)
        ) {
            SettingsCard {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_haptic_feedback),
                    subtitle = stringResource(R.string.settings_haptic_feedback_subtitle),
                    checked = haptic,
                    onCheckedChange = {
                        haptic = it
                        prefs.edit().putBoolean("haptic_feedback", it).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun ConnectionSettingsScreen(
    onBack: () -> Unit,
    onBooleanSettingChanged: (String, Boolean) -> Unit,
    inputProfile: HidProfile,
    onInputProfileChanged: (HidProfile) -> Boolean
) {
    val context = LocalContext.current
    val prefs = settingsPrefs()
    var autoConnect by rememberBooleanSetting(prefs, "auto_connect_on_launch", true)
    var autoReconnect by rememberBooleanSetting(prefs, "auto_reconnect_on_disconnect", true)
    var keepScreenOn by rememberBooleanSetting(prefs, "keep_screen_on", true)
    var notifications by rememberBooleanSetting(prefs, "connection_notifications", true)

    val switchProfile: (HidProfile) -> Unit = { profile ->
        if (profile != inputProfile && !onInputProfileChanged(profile)) {
            Toast.makeText(context, R.string.settings_device_type_switch_failed, Toast.LENGTH_SHORT).show()
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_section_connection),
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
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_auto_connect_launch),
                        subtitle = stringResource(R.string.settings_auto_connect_launch_subtitle),
                        checked = autoConnect,
                        onCheckedChange = { autoConnect = it; saveBoolean(prefs, "auto_connect_on_launch", it, onBooleanSettingChanged) }
                    )
                    SettingsItemDivider()
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_auto_reconnect),
                        subtitle = stringResource(R.string.settings_auto_reconnect_subtitle),
                        checked = autoReconnect,
                        onCheckedChange = { autoReconnect = it; saveBoolean(prefs, "auto_reconnect_on_disconnect", it, onBooleanSettingChanged) }
                    )
                    SettingsItemDivider()
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_keep_screen_on),
                        subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
                        checked = keepScreenOn,
                        onCheckedChange = { keepScreenOn = it; saveBoolean(prefs, "keep_screen_on", it, onBooleanSettingChanged) }
                    )
                    SettingsItemDivider()
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_connection_notifications),
                        subtitle = stringResource(R.string.settings_connection_notifications_subtitle),
                        checked = notifications,
                        onCheckedChange = { notifications = it; saveBoolean(prefs, "connection_notifications", it, onBooleanSettingChanged) }
                    )
                }
            }
        }
    }
}

@Composable
fun DataSettingsScreen(
    onBack: () -> Unit,
    onResetMacros: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = stringResource(R.string.settings_section_data),
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 16.dp)
        ) {
            SettingsCard {
                SettingsActionItem(
                    title = stringResource(R.string.settings_reset_macros),
                    subtitle = stringResource(R.string.dialog_reset_macros_confirm),
                    titleColor = MaterialTheme.colorScheme.error,
                    leadingIcon = Icons.Default.DeleteSweep,
                    onClick = { showConfirm = true }
                )
            }
        }
    }

    if (showConfirm) {
        AppAlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.settings_reset_macros)) },
            text = { Text(stringResource(R.string.dialog_reset_macros_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onResetMacros()
                    showConfirm = false
                }) {
                    Text(stringResource(R.string.dialog_reset), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

// ==========================================
// 辅助持久化与状态工具
// ==========================================

@Composable
private fun settingsPrefs(): SharedPreferences =
    LocalContext.current.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

private fun settingsPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

@Composable
private fun rememberBooleanSetting(
    prefs: SharedPreferences,
    key: String,
    defaultValue: Boolean
) = remember(key) { mutableStateOf(prefs.getBoolean(key, defaultValue)) }

@Composable
private fun rememberIntSetting(
    prefs: SharedPreferences,
    key: String,
    defaultValue: Int
) = remember(key) { mutableIntStateOf(prefs.getInt(key, defaultValue)) }

private fun saveBoolean(
    prefs: SharedPreferences,
    key: String,
    value: Boolean,
    onBooleanSettingChanged: (String, Boolean) -> Unit
) {
    prefs.edit().putBoolean(key, value).apply()
    onBooleanSettingChanged(key, value)
}

fun getThemeModeIndex(context: Context): Int = ThemeController.nightModeIndex

fun setThemeModeIndex(context: Context, index: Int) {
    ThemeController.setNightModeIndex(context, index)
}

fun getThemeColorStyle(context: Context): ThemeColorStyle =
    ThemeColorStyle.fromStorageValue(
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE).getString("theme_color_style", null)
    )

fun setThemeColorStyle(context: Context, style: ThemeColorStyle) {
    context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        .edit().putString("theme_color_style", style.storageValue).apply()
}
