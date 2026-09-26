package com.haoze.nexus.ui.compose

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haoze.nexus.R
import com.haoze.nexus.bluetooth.HidProfile
import com.haoze.nexus.macro.Macro
import com.haoze.nexus.ui.component.animation.bouncyCardClickable

// Material Design 3 Expressive 统一圆角规范
private val M3ShapeHero = RoundedCornerShape(28.dp)
private val M3ShapeCard = RoundedCornerShape(24.dp)
private val M3ShapeSubCard = RoundedCornerShape(20.dp)
private val M3ShapeKeycap = RoundedCornerShape(14.dp)
private val M3ShapeIconBox = RoundedCornerShape(14.dp)
private val M3ShapeTag = RoundedCornerShape(8.dp)

/**
 * Nexus 全新整合型首页：
 * 遵循 Material Design 3 设计规范与 Expressive 设计语言，
 * 全面优化结构分区、排布节奏、外边距/内边距与呼吸留白，
 * 并提供完善的多屏幕与大屏居中自适应能力。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    isConnected: Boolean,
    connectedDeviceName: String?,
    connectingDeviceAddress: String?,
    inputProfile: HidProfile,
    onInputProfileChanged: (HidProfile) -> Unit,
    onShowDeviceList: () -> Unit,
    onDisconnectDevice: () -> Unit,
    onOpenKeyboard: () -> Unit,
    onOpenTouchpad: () -> Unit,
    onOpenGamepad: () -> Unit,
    onOpenTvRemote: () -> Unit,
    onOpenAudioReceiver: () -> Unit,
    onOpenAgent: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBottomBarCustomization: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSponsor: () -> Unit,
    onOpenSponsorList: () -> Unit,
    onCoreCommand: (CoreCommand) -> Unit,
    macros: List<Macro>,
    onMacroClick: (Macro) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { settingsPrefs(context) }
    var keepScreenOn by rememberBooleanSetting(prefs, "keep_screen_on", true)
    var hapticFeedback by rememberBooleanSetting(prefs, "haptic_feedback", true)
    var keySound by rememberBooleanSetting(prefs, "key_sound_enabled", true)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp
        val horizontalMargin = if (isWideScreen) 24.dp else 18.dp

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(
                    start = horizontalMargin,
                    end = horizontalMargin,
                    top = 10.dp,
                    bottom = 124.dp
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // 1. 顶部 Header (品牌名与快速操作)
                item {
                    HomeHeader(
                        isConnected = isConnected,
                        onShowDeviceList = onShowDeviceList,
                        onOpenSettings = onOpenSettings
                    )
                }

                // 2. Hero 连接状态卡片 (核心连接中枢)
                item {
                    HeroConnectionCard(
                        isConnected = isConnected,
                        connectedDeviceName = connectedDeviceName,
                        isConnecting = connectingDeviceAddress != null,
                        inputProfile = inputProfile,
                        onInputProfileChanged = onInputProfileChanged,
                        onShowDeviceList = onShowDeviceList,
                        onDisconnect = onDisconnectDevice
                    )
                }

                // 3. 核心外设控制矩阵 (Primary Peripheral Deck)
                item {
                    HomeSectionHeader(
                        title = stringResource(R.string.home_peripherals_title),
                        icon = Icons.Default.Tune
                    )
                }

                item {
                    PeripheralDeck(
                        onOpenKeyboard = onOpenKeyboard,
                        onOpenTouchpad = onOpenTouchpad,
                        onOpenGamepad = onOpenGamepad,
                        onOpenTvRemote = onOpenTvRemote,
                        onOpenAudioReceiver = onOpenAudioReceiver,
                        isWideScreen = isWideScreen
                    )
                }

                // 4. 终端与 Agent 快捷指令 (Quick Commands)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HomeSectionHeader(
                            title = stringResource(R.string.home_quick_commands_title),
                            icon = Icons.Default.Terminal
                        )
                        TextButton(
                            onClick = onOpenAgent,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_all_macros),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    QuickCommandsCard(
                        isConnected = isConnected,
                        onCoreCommand = onCoreCommand,
                        macros = macros,
                        onMacroClick = onMacroClick,
                        onOpenAgent = onOpenAgent
                    )
                }

                // 5. 快捷工具与系统偏好 (Quick Tools & System)
                item {
                    HomeSectionHeader(
                        title = stringResource(R.string.home_quick_preferences_title),
                        icon = Icons.Default.Settings
                    )
                }

                item {
                    QuickToolsCard(
                        keepScreenOn = keepScreenOn,
                        onKeepScreenOnChanged = {
                            keepScreenOn = it
                            prefs.edit().putBoolean("keep_screen_on", it).apply()
                        },
                        hapticFeedback = hapticFeedback,
                        onHapticFeedbackChanged = {
                            hapticFeedback = it
                            prefs.edit().putBoolean("haptic_feedback", it).apply()
                        },
                        keySound = keySound,
                        onKeySoundChanged = {
                            keySound = it
                            prefs.edit().putBoolean("key_sound_enabled", it).apply()
                        },
                        onOpenBottomBarCustomization = onOpenBottomBarCustomization,
                        onOpenAbout = onOpenAbout,
                        onOpenSponsor = onOpenSponsor
                    )
                }
            }
        }
    }
}

/**
 * 首页顶部导航与状态栏区：
 * 采用轻量通透的大字号品牌排版与精致的 Tonal 圆形快捷入口。
 */
@Composable
private fun HomeHeader(
    isConnected: Boolean,
    onShowDeviceList: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Nexus",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.home_header_slogan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 蓝牙/设备管理快捷入口
            Surface(
                shape = CircleShape,
                color = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onShowDeviceList)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = stringResource(R.string.device_list_title),
                        tint = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    if (isConnected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
            }

            // 设置入口
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenSettings)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.home_settings_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Hero 连接中枢卡片：
 * 页面视觉焦点，拥有充裕的内边距、自然的微呼吸光效、清晰的设备态与模式切换。
 */
@Composable
private fun HeroConnectionCard(
    isConnected: Boolean,
    connectedDeviceName: String?,
    isConnecting: Boolean,
    inputProfile: HidProfile,
    onInputProfileChanged: (HidProfile) -> Unit,
    onShowDeviceList: () -> Unit,
    onDisconnect: () -> Unit
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
        modifier = Modifier
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

/**
 * 核心外设控制矩阵：
 * 采用 2x2 网格与通栏宽卡片，卡片层次分明，留白舒展，图标与标签清晰对应。
 */
@Composable
private fun PeripheralDeck(
    onOpenKeyboard: () -> Unit,
    onOpenTouchpad: () -> Unit,
    onOpenGamepad: () -> Unit,
    onOpenTvRemote: () -> Unit,
    onOpenAudioReceiver: () -> Unit,
    isWideScreen: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 第一行：键盘与触控板
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PeripheralCard(
                title = stringResource(R.string.home_keyboard_title),
                subtitle = "全尺寸虚拟键盘",
                tag = stringResource(R.string.home_tag_keyboard),
                icon = Icons.Default.Keyboard,
                onClick = onOpenKeyboard,
                modifier = Modifier.weight(1f)
            )
            PeripheralCard(
                title = stringResource(R.string.home_touchpad_title),
                subtitle = "手势与指针控制",
                tag = stringResource(R.string.home_tag_touchpad),
                icon = Icons.Default.Mouse,
                onClick = onOpenTouchpad,
                modifier = Modifier.weight(1f)
            )
        }

        // 第二行：手柄与电视遥控
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PeripheralCard(
                title = stringResource(R.string.home_gamepad_title),
                subtitle = "双摇杆与线性扳机",
                tag = stringResource(R.string.home_tag_gamepad),
                icon = Icons.Default.SportsEsports,
                onClick = onOpenGamepad,
                modifier = Modifier.weight(1f)
            )
            PeripheralCard(
                title = stringResource(R.string.home_tvremote_title),
                subtitle = "方向导航与媒体控制",
                tag = stringResource(R.string.home_tag_tvremote),
                icon = Icons.Default.SettingsRemote,
                onClick = onOpenTvRemote,
                modifier = Modifier.weight(1f)
            )
        }

        // 第三行：无线音频串流通栏宽卡片
        PeripheralWideCard(
            title = stringResource(R.string.home_audio_stream_title),
            subtitle = stringResource(R.string.home_audio_stream_desc),
            tag = stringResource(R.string.home_tag_audio),
            icon = Icons.Default.GraphicEq,
            onClick = onOpenAudioReceiver
        )
    }
}

/**
 * 单个外设卡片：
 * 采用 18dp 舒展内边距，大圆角与柔和 Tonal 容器背景。
 */
@Composable
private fun PeripheralCard(
    title: String,
    subtitle: String,
    tag: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = M3ShapeSubCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .clip(M3ShapeSubCard)
            .bouncyCardClickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            M3ShapeIconBox
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Surface(
                    shape = M3ShapeTag,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 通栏外设卡片（无线音频接收）：
 * 赋予重点功能的视觉丰富度与横向平衡。
 */
@Composable
private fun PeripheralWideCard(
    title: String,
    subtitle: String,
    tag: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = M3ShapeSubCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(M3ShapeSubCard)
            .bouncyCardClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                        M3ShapeIconBox
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = M3ShapeTag,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 终端与 Agent 快捷指令卡片：
 * 采用拟物键帽与轻薄芯片设计，舒适的触控高度与清晰的应答按键反馈。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCommandsCard(
    isConnected: Boolean,
    onCoreCommand: (CoreCommand) -> Unit,
    macros: List<Macro>,
    onMacroClick: (Macro) -> Unit,
    onOpenAgent: () -> Unit
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 提示标签
            Text(
                text = "高频终端响应按键",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 核心控制键帽条
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickCommandPill(
                    label = "Y",
                    sub = stringResource(R.string.btn_yes),
                    onClick = { onCoreCommand(CoreCommand.YES) },
                    modifier = Modifier.weight(1f)
                )
                QuickCommandPill(
                    label = "All",
                    sub = stringResource(R.string.btn_yes_to_all),
                    onClick = { onCoreCommand(CoreCommand.YES_TO_ALL) },
                    modifier = Modifier.weight(1f)
                )
                QuickCommandPill(
                    label = "^C",
                    sub = stringResource(R.string.btn_ctrl_c),
                    onClick = { onCoreCommand(CoreCommand.CTRL_C) },
                    modifier = Modifier.weight(1f)
                )
                QuickCommandPill(
                    label = "Enter",
                    sub = stringResource(R.string.btn_enter),
                    onClick = { onCoreCommand(CoreCommand.ENTER) },
                    modifier = Modifier.weight(1f)
                )
                QuickCommandPill(
                    label = "N",
                    sub = stringResource(R.string.btn_no),
                    onClick = { onCoreCommand(CoreCommand.NO) },
                    isDestructive = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 自定义宏指令芯片区
            if (macros.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "常用宏捷径",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        macros.take(8).forEach { macro ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .bouncyCardClickable(onClick = { onMacroClick(macro) })
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = macro.label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "暂无快捷宏，可在 Agent 中配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onOpenAgent,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "添加宏",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 拟物键帽组件：
 * 拥有立体质感、明确的高亮与危险色提示，以及适度的内边距与触控面积。
 */
@Composable
private fun QuickCommandPill(
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    Surface(
        shape = M3ShapeKeycap,
        color = if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .clip(M3ShapeKeycap)
            .bouncyCardClickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 快捷工具与偏好卡片：
 * 重新分类梳理为「交互偏好」与「系统支持」两大功能区，避免堆叠混杂，
 * 统一行内边距（56dp+ 行高），左侧 Tonal 容器图标，右侧开关或箭头指示器。
 */
@Composable
private fun QuickToolsCard(
    keepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    hapticFeedback: Boolean,
    onHapticFeedbackChanged: (Boolean) -> Unit,
    keySound: Boolean,
    onKeySoundChanged: (Boolean) -> Unit,
    onOpenBottomBarCustomization: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSponsor: () -> Unit
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
        modifier = Modifier.fillMaxWidth()
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
private fun QuickToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
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
private fun QuickLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
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

/**
 * 分区标题组件：
 * 采用小巧精致的带底色图标标识与现代 Typography 层级。
 */
@Composable
private fun HomeSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(26.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
