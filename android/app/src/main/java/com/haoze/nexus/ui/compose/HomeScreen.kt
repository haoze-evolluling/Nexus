package com.haoze.nexus.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.bluetooth.HidProfile
import com.haoze.nexus.macro.Macro
import com.haoze.nexus.ui.compose.home.HeroConnectionCard
import com.haoze.nexus.ui.compose.home.HomeHeader
import com.haoze.nexus.ui.compose.home.HomeSectionHeader
import com.haoze.nexus.ui.compose.home.PeripheralDeck
import com.haoze.nexus.ui.compose.home.QuickCommandsCard
import com.haoze.nexus.ui.compose.home.QuickToolsCard

/**
 * Nexus 全新整合型首页：
 * 遵循 Material Design 3 设计规范与 Expressive 设计语言，
 * 全面优化结构分区、排布节奏、外边距/内边距与呼吸留白，
 * 并提供完善的多屏幕与大屏居中自适应能力。
 */
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
