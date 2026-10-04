package com.haoze.nexus.ui.compose

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.bluetooth.HidProfile

// ==========================================
// 设备与连接设置界面
// ==========================================

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
