package com.haoze.nexus.ui.compose.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.haoze.nexus.R
import com.haoze.nexus.ui.compose.SettingsActionItem
import com.haoze.nexus.ui.compose.SettingsCard

/**
 * 设置主页 - 数据与管理配置卡片 (恢复快捷命令默认设置)
 */
@Composable
fun DataSettingsCard(
    onResetMacrosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(modifier = modifier) {
        SettingsActionItem(
            title = stringResource(R.string.settings_reset_macros),
            subtitle = stringResource(R.string.dialog_reset_macros_confirm),
            titleColor = MaterialTheme.colorScheme.error,
            leadingIcon = Icons.Default.Restore,
            onClick = onResetMacrosClick,
            trailing = {
                TextButton(onClick = onResetMacrosClick) {
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
