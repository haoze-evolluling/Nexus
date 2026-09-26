package com.haoze.nexus.ui.compose

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.bluetooth.HidProfile
import com.haoze.nexus.ui.Routes
import com.haoze.nexus.ui.compose.settings.AboutSettingsCard
import com.haoze.nexus.ui.compose.settings.AppearanceSettingsCard
import com.haoze.nexus.ui.compose.settings.ConnectionSettingsCard
import com.haoze.nexus.ui.compose.settings.DataSettingsCard
import com.haoze.nexus.ui.compose.settings.FeedbackSettingsCard
import com.haoze.nexus.ui.compose.settings.InputSettingsCard
import com.haoze.nexus.ui.compose.settings.ResetMacrosConfirmDialog
import com.haoze.nexus.ui.compose.settings.SettingsVersionFooter

/**
 * 设置页一级界面：
 * 将软件所有设置项拆分并重组平铺在一级界面中，无需二级/三级路由跳转。
 * 整体 UI 严格遵循 Material 3 (MD3) 设计体系。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToRoute: ((String) -> Unit)? = null,
    onThemeColorStyleChanged: (ThemeColorStyle) -> Unit = {},
    onBooleanSettingChanged: (String, Boolean) -> Unit = { _, _ -> },
    inputProfile: HidProfile = HidProfile.KEYBOARD_MOUSE,
    onInputProfileChanged: (HidProfile) -> Boolean = { true },
    onResetMacros: () -> Unit = {},
    showBackIcon: Boolean = true,
    contentBottomPadding: Dp = 0.dp
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
        onBack = onBack,
        showBackIcon = showBackIcon
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp + contentBottomPadding),
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
                AppearanceSettingsCard(
                    currentThemeColorStyle = currentThemeColorStyle,
                    onThemeColorStyleSelected = { style ->
                        currentThemeColorStyle = style
                        setThemeColorStyle(context, style)
                        onThemeColorStyleChanged(style)
                    },
                    onNavigateToBottomBarCustomization = onNavigateToRoute?.let { navigate ->
                        { navigate(Routes.BOTTOM_BAR_CUSTOMIZATION) }
                    }
                )
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
                InputSettingsCard(
                    touchpadSensitivity = touchpadSensitivity,
                    onSensitivityChanged = { value ->
                        touchpadSensitivity = value
                        prefs.edit().putInt("touchpad_sensitivity", touchpadSensitivity).apply()
                    },
                    cursorSpeed = cursorSpeed,
                    onCursorSpeedChanged = { value ->
                        cursorSpeed = value
                        prefs.edit().putInt("cursor_speed", cursorSpeed).apply()
                    },
                    naturalScroll = naturalScroll,
                    onNaturalScrollChanged = { checked ->
                        naturalScroll = checked
                        prefs.edit().putBoolean("scroll_direction_natural", checked).apply()
                    }
                )
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
                FeedbackSettingsCard(
                    hapticFeedback = hapticFeedback,
                    onHapticFeedbackChanged = { checked ->
                        hapticFeedback = checked
                        prefs.edit().putBoolean("haptic_feedback", checked).apply()
                    },
                    keySound = keySound,
                    onKeySoundChanged = { checked ->
                        keySound = checked
                        prefs.edit().putBoolean("key_sound_enabled", checked).apply()
                    },
                    gamepadVibration = gamepadVibration,
                    onGamepadVibrationChanged = { checked ->
                        gamepadVibration = checked
                        prefs.edit().putBoolean("gamepad_vibration_enabled", checked).apply()
                    }
                )
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
                ConnectionSettingsCard(
                    inputProfile = inputProfile,
                    onProfileSelected = switchProfile,
                    autoConnect = autoConnect,
                    onAutoConnectChanged = { checked ->
                        autoConnect = checked
                        saveBoolean(prefs, "auto_connect_on_launch", checked, onBooleanSettingChanged)
                    },
                    autoReconnect = autoReconnect,
                    onAutoReconnectChanged = { checked ->
                        autoReconnect = checked
                        saveBoolean(prefs, "auto_reconnect_on_disconnect", checked, onBooleanSettingChanged)
                    },
                    keepScreenOn = keepScreenOn,
                    onKeepScreenOnChanged = { checked ->
                        keepScreenOn = checked
                        saveBoolean(prefs, "keep_screen_on", checked, onBooleanSettingChanged)
                    },
                    connectionNotifications = connectionNotifications,
                    onConnectionNotificationsChanged = { checked ->
                        connectionNotifications = checked
                        saveBoolean(prefs, "connection_notifications", checked, onBooleanSettingChanged)
                    }
                )
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
                DataSettingsCard(
                    onResetMacrosClick = { showResetMacrosConfirm = true }
                )
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
                AboutSettingsCard(
                    versionName = versionName,
                    onNavigateToAbout = onNavigateToRoute?.let { navigate -> { navigate(Routes.ABOUT) } },
                    onNavigateToSponsor = onNavigateToRoute?.let { navigate -> { navigate(Routes.SPONSOR) } },
                    onNavigateToSponsorList = onNavigateToRoute?.let { navigate -> { navigate(Routes.SPONSOR_LIST) } }
                )
            }

            // 底部版本标语
            item {
                SettingsVersionFooter(versionName = versionName)
            }
        }
    }

    if (showResetMacrosConfirm) {
        ResetMacrosConfirmDialog(
            onDismissRequest = { showResetMacrosConfirm = false },
            onResetConfirmed = onResetMacros
        )
    }
}
