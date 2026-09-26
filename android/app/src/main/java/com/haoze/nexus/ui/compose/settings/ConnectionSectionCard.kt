package com.haoze.nexus.ui.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.bluetooth.HidProfile
import com.haoze.nexus.ui.compose.SettingsCard
import com.haoze.nexus.ui.compose.SettingsItemDivider
import com.haoze.nexus.ui.compose.SettingsSwitchItem

/**
 * 设置主页 - 设备与连接配置卡片 (模拟设备类型、启动自连、断连重连、屏幕常亮、连接通知)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsCard(
    inputProfile: HidProfile,
    onProfileSelected: (HidProfile) -> Unit,
    autoConnect: Boolean,
    onAutoConnectChanged: (Boolean) -> Unit,
    autoReconnect: Boolean,
    onAutoReconnectChanged: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    connectionNotifications: Boolean,
    onConnectionNotificationsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(modifier = modifier) {
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
                    onClick = { onProfileSelected(profile) },
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
            onCheckedChange = onAutoConnectChanged
        )

        SettingsItemDivider()

        SettingsSwitchItem(
            title = stringResource(R.string.settings_auto_reconnect),
            subtitle = stringResource(R.string.settings_auto_reconnect_subtitle),
            checked = autoReconnect,
            onCheckedChange = onAutoReconnectChanged
        )

        SettingsItemDivider()

        SettingsSwitchItem(
            title = stringResource(R.string.settings_keep_screen_on),
            subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
            checked = keepScreenOn,
            onCheckedChange = onKeepScreenOnChanged
        )

        SettingsItemDivider()

        SettingsSwitchItem(
            title = stringResource(R.string.settings_connection_notifications),
            subtitle = stringResource(R.string.settings_connection_notifications_subtitle),
            checked = connectionNotifications,
            onCheckedChange = onConnectionNotificationsChanged
        )
    }
}
