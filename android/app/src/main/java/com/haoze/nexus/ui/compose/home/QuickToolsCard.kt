package com.haoze.nexus.ui.compose.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R

/**
 * 快捷工具与偏好卡片：
 * 重新分类梳理为「交互偏好」与「系统支持」两大功能区，避免堆叠混杂，
 * 统一行内边距（56dp+ 行高），左侧 Tonal 容器图标，右侧开关或箭头指示器。
 */
@Composable
internal fun QuickToolsCard(
    keepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    hapticFeedback: Boolean,
    onHapticFeedbackChanged: (Boolean) -> Unit,
    keySound: Boolean,
    onKeySoundChanged: (Boolean) -> Unit,
    onOpenBottomBarCustomization: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSponsor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = M3ShapeCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // 分区 1: 交互与硬件控制偏好
            Text(
                text = "交互与控制偏好",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)
            )

            QuickToggleRow(
                title = stringResource(R.string.settings_keep_screen_on),
                subtitle = "控制过程中避免屏幕自动锁定休眠",
                icon = Icons.Default.PowerSettingsNew,
                checked = keepScreenOn,
                onCheckedChange = onKeepScreenOnChanged
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            QuickToggleRow(
                title = stringResource(R.string.settings_haptic_feedback),
                subtitle = "按键触控与操作震动轻微提示",
                icon = Icons.Default.Vibration,
                checked = hapticFeedback,
                onCheckedChange = onHapticFeedbackChanged
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            QuickToggleRow(
                title = stringResource(R.string.settings_key_sound),
                subtitle = "键盘输入打字模拟机械声效",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                checked = keySound,
                onCheckedChange = onKeySoundChanged
            )

            // 分区 2: 系统个性化与关于
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Text(
                text = "个性化与支持",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp)
            )

            QuickLinkRow(
                title = stringResource(R.string.bottom_bar_customization),
                subtitle = stringResource(R.string.bottom_bar_customization_subtitle),
                icon = Icons.Default.Tune,
                onClick = onOpenBottomBarCustomization
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            QuickLinkRow(
                title = stringResource(R.string.home_about_title),
                subtitle = "版本信息、开源声明与诊断支持",
                icon = Icons.Default.Info,
                onClick = onOpenAbout
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            QuickLinkRow(
                title = stringResource(R.string.home_sponsor_title),
                subtitle = stringResource(R.string.home_sponsor_list_title),
                icon = Icons.Default.Favorite,
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = onOpenSponsor
            )
        }
    }
}

/**
 * 设置开关行组件：
 * 包含左侧图标容器、主副标题、右侧标准 Material 3 Switch，触控区域充裕。
 */
@Composable
internal fun QuickToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(38.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 设置导航跳转行组件：
 * 包含左侧图标容器、主副标题与右侧箭头指示器。
 */
@Composable
internal fun QuickLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(38.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
