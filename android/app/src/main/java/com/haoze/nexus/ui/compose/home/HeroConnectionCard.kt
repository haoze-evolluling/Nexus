package com.haoze.nexus.ui.compose.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.bluetooth.HidProfile
import com.haoze.nexus.ui.component.animation.bouncyCardClickable

/**
 * Hero 连接中枢卡片：
 * 页面视觉焦点，拥有充裕的内边距、自然的微呼吸光效、清晰的设备态与模式切换。
 */
@Composable
internal fun HeroConnectionCard(
    isConnected: Boolean,
    connectedDeviceName: String?,
    isConnecting: Boolean,
    inputProfile: HidProfile,
    onInputProfileChanged: (HidProfile) -> Unit,
    onShowDeviceList: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowColor by animateColorAsState(
        targetValue = when {
            isConnected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
            isConnecting -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        },
        animationSpec = tween(400),
        label = "heroGlow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(M3ShapeHero)
            .bouncyCardClickable(onClick = onShowDeviceList),
        shape = M3ShapeHero,
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 设备主体状态行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 呼吸光晕与主体图标
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .then(if (isConnected) Modifier.scale(pulseScale) else Modifier)
                            .background(glowColor, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = if (isConnected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 设备状态与标题
                Column(modifier = Modifier.weight(1f)) {
                    // 状态胶囊标签
                    Surface(
                        shape = M3ShapeTag,
                        color = when {
                            isConnected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            isConnecting -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = when {
                                isConnected -> "已连接"
                                isConnecting -> "正在连接..."
                                else -> "待连接"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                isConnected -> MaterialTheme.colorScheme.onPrimaryContainer
                                isConnecting -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                        )
                    }

                    Text(
                        text = when {
                            isConnected && !connectedDeviceName.isNullOrBlank() -> connectedDeviceName
                            isConnecting -> stringResource(R.string.device_connecting)
                            else -> stringResource(R.string.status_not_connected)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isConnected) stringResource(R.string.home_device_connected_desc)
                        else stringResource(R.string.home_device_disconnected_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // 模式选择与主控交互操作
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // HID 模式切换 Chip 组
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = inputProfile == HidProfile.KEYBOARD_MOUSE,
                        onClick = { onInputProfileChanged(HidProfile.KEYBOARD_MOUSE) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.settings_device_type_keyboard_mouse),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.height(36.dp)
                    )

                    FilterChip(
                        selected = inputProfile == HidProfile.GAMEPAD,
                        onClick = { onInputProfileChanged(HidProfile.GAMEPAD) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.settings_device_type_gamepad),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.height(36.dp)
                    )
                }

                // 主操作按钮
                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_disconnect_device),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = onShowDeviceList,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_connection_action_disconnected),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}
