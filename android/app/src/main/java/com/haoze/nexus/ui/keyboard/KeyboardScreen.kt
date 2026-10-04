package com.haoze.nexus.ui.keyboard

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.haoze.nexus.sound.KeyboardSoundSynthesizer
import com.haoze.nexus.sound.SwitchType
import com.haoze.nexus.ui.component.animation.rememberScreenEntranceTransition
import com.haoze.nexus.ui.component.animation.screenEntranceMain
import com.haoze.nexus.ui.component.animation.screenEntranceTopBar
import com.haoze.nexus.ui.compose.ThemeController

/**
 * 机械键盘页面（Bluke 的 Keyboard 界面完整移植）：
 * 金属外壳画布 + 磨砂工具栏 + 多点触控键帽阵列，
 * 按键经 [sendKey] 以按下/释放语义发送 HID 报告。
 */
@Composable
fun KeyboardScreen(
    isConnected: Boolean,
    connectedDeviceName: String?,
    canReconnect: Boolean,
    soundSynthesizer: KeyboardSoundSynthesizer,
    onExit: () -> Unit,
    onOpenTouchpad: () -> Unit,
    onReconnect: () -> Unit,
    sendKey: (Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sharedPrefs = remember { context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE) }
    val entranceState = rememberScreenEntranceTransition()

    val isDarkTheme = when (ThemeController.nightModeIndex) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val activePressedKeys = remember { mutableStateListOf<Int>() }

    var selectedLayoutType by rememberSaveable {
        mutableStateOf(if (isDarkTheme) KeyboardLayoutType.OBLIVION_75 else KeyboardLayoutType.MODEL_M_VINTAGE)
    }
    var selectedCaseColor by rememberSaveable {
        mutableStateOf(if (isDarkTheme) CaseColor.BLACK else CaseColor.WHITE)
    }
    var currentSwitch by rememberSaveable { mutableStateOf(soundSynthesizer.getCurrentSwitch()) }
    var isMuted by rememberSaveable { mutableStateOf(!sharedPrefs.getBoolean("key_sound_enabled", true)) }
    var isHapticsEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("haptic_feedback", true)) }
    var keySensitivity by remember { mutableStateOf(sharedPrefs.getFloat("key_sensitivity", 6f)) }

    var isCapsLockActive by rememberSaveable { mutableStateOf(false) }
    var isNumLockActive by rememberSaveable { mutableStateOf(true) }
    var isScrollLockActive by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isDarkTheme) {
        if (isDarkTheme) {
            if (selectedCaseColor == CaseColor.WHITE) {
                selectedCaseColor = CaseColor.BLACK
            }
            if (selectedLayoutType == KeyboardLayoutType.MODEL_M_VINTAGE) {
                selectedLayoutType = KeyboardLayoutType.OBLIVION_75
            }
        } else {
            if (selectedCaseColor == CaseColor.BLACK) {
                selectedCaseColor = CaseColor.WHITE
            }
            if (selectedLayoutType == KeyboardLayoutType.OBLIVION_75) {
                selectedLayoutType = KeyboardLayoutType.MODEL_M_VINTAGE
            }
        }
    }

    LaunchedEffect(Unit) {
        soundSynthesizer.setMute(isMuted)
    }

    fun handleLocalKeyPress(keyCode: Int, isPress: Boolean) {
        if (isPress) {
            if (isHapticsEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
            }
            if (!activePressedKeys.contains(keyCode)) {
                activePressedKeys.add(keyCode)
                soundSynthesizer.playPress(keyCode)
                sendKey(keyCode, true)

                when (keyCode) {
                    0x39 -> isCapsLockActive = !isCapsLockActive // KEY_CAPSLOCK
                    0x47 -> isScrollLockActive = !isScrollLockActive // KEY_SCROLL_LOCK
                    0x53 -> isNumLockActive = !isNumLockActive // KEY_NUM_LOCK
                }
            }
        } else {
            activePressedKeys.remove(keyCode)
            soundSynthesizer.playRelease(keyCode)
            sendKey(keyCode, false)
        }
    }

    val caseColorVal = selectedCaseColor.getActualColor(sharedPrefs)
    val caseMetallic = selectedCaseColor.getActualMetallic(sharedPrefs)
    val caseBrush = if (caseMetallic) {
        Brush.linearGradient(
            colors = listOf(
                caseColorVal,
                caseColorVal.copy(alpha = 0.85f),
                caseColorVal.copy(alpha = 0.7f),
                caseColorVal,
                caseColorVal.copy(alpha = 0.9f)
            ),
            start = Offset(0f, 0f),
            end = Offset(500f, 500f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                caseColorVal,
                caseColorVal.copy(alpha = 0.92f)
            )
        )
    }

    // Seamless full-screen canvas acting as the aluminum keyboard plate and chassis
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(caseBrush)
            .padding(bottom = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Sleek Frosted Glass Top Settings Bar (KBSim Web Style Toggles Toolbar)
        val topBarBg = if (isDarkTheme) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        val pillBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
        val pillBorderColor = if (isDarkTheme) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        val topBarContentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
        val topBarSubTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
        val statusLedColor = if (isConnected) {
            if (isDarkTheme) Color(0xFF39FF14) else Color(0xFF2E7D32)
        } else {
            if (isDarkTheme) Color(0xFFFF9800) else Color(0xFFE65100)
        }
        val lockActiveLed = if (isDarkTheme) Color(0xFF39FF14) else Color(0xFF2E7D32)
        val lockInactiveLed = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        val caseDotBorder = if (isDarkTheme) Color.White else Color.Black.copy(alpha = 0.25f)

        val pillModifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(pillBg)
            .then(
                if (!isDarkTheme) Modifier.border(0.5.dp, pillBorderColor, RoundedCornerShape(6.dp))
                else Modifier
            )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .screenEntranceTopBar(entranceState)
                .background(topBarBg)
                .then(
                    if (!isDarkTheme) Modifier.border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    else Modifier
                )
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Section: Exit, input-mode switch, Connection status and Reconnect Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Exit Button (Pill style to match other buttons)
                Row(
                    modifier = pillModifier
                        .clickable { onExit() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit",
                        tint = topBarContentColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Close",
                        color = topBarContentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Input Mode Switcher (keyboard <-> touchpad)
                Row(
                    modifier = pillModifier
                        .clickable {
                            if (isHapticsEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            onOpenTouchpad()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Switch Mode",
                        tint = topBarContentColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Keyboard Mode",
                        color = topBarContentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status LED and connection details
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusLedColor)
                )

                Text(
                    text = connectedDeviceName ?: "No Host",
                    color = topBarContentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )

                Text(
                    text = if (isConnected) "[connected]" else "[offline]",
                    color = topBarSubTextColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif
                )

                // Reconnect Button
                if (!isConnected && canReconnect) {
                    Row(
                        modifier = pillModifier
                            .clickable { onReconnect() }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reconnect",
                            tint = topBarContentColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Reconnect",
                            color = topBarContentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right Section: lock LEDs indicators, configuration pills, and mute button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Keyboard Status Lock LEDs Panel
                Row(
                    modifier = pillModifier
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // CAPS
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                sendKey(0x39, true)
                                sendKey(0x39, false)
                                isCapsLockActive = !isCapsLockActive
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isCapsLockActive) lockActiveLed else lockInactiveLed)
                        )
                        Text(
                            text = "CAPS",
                            color = if (isCapsLockActive) topBarContentColor else topBarSubTextColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }

                    // NUM
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                sendKey(0x53, true)
                                sendKey(0x53, false)
                                isNumLockActive = !isNumLockActive
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isNumLockActive) lockActiveLed else lockInactiveLed)
                        )
                        Text(
                            text = "NUM",
                            color = if (isNumLockActive) topBarContentColor else topBarSubTextColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }

                    // SCR
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                sendKey(0x47, true)
                                sendKey(0x47, false)
                                isScrollLockActive = !isScrollLockActive
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isScrollLockActive) lockActiveLed else lockInactiveLed)
                        )
                        Text(
                            text = "SCR",
                            color = if (isScrollLockActive) topBarContentColor else topBarSubTextColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }

                // 2. Layout Selector Pill
                Row(
                    modifier = pillModifier
                        .clickable {
                            val enabledLayouts = KeyboardLayoutType.entries.toList()
                            val currentIndexInEnabled = enabledLayouts.indexOf(selectedLayoutType)
                            val nextIndex = if (currentIndexInEnabled < 0) 0 else (currentIndexInEnabled + 1) % enabledLayouts.size
                            selectedLayoutType = enabledLayouts[nextIndex]
                            soundSynthesizer.playPress()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Layout",
                        tint = topBarContentColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedLayoutType.displayName,
                        color = topBarContentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3. Switch Selector Pill
                Row(
                    modifier = pillModifier
                        .clickable {
                            val enabledSwitches = SwitchType.entries.toList()
                            val currentIndexInEnabled = enabledSwitches.indexOf(currentSwitch)
                            val nextIndex = (currentIndexInEnabled + 1) % enabledSwitches.size
                            val nextSwitch = enabledSwitches[nextIndex]
                            soundSynthesizer.changeSwitchType(nextSwitch)
                            currentSwitch = nextSwitch
                            soundSynthesizer.playPress()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Switch",
                        tint = topBarContentColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentSwitch.displayName,
                        color = topBarContentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 4. Case Color Selector Pill
                Row(
                    modifier = pillModifier
                        .clickable {
                            val enabledColors = CaseColor.entries.toList()
                            val currentIndexInEnabled = enabledColors.indexOf(selectedCaseColor)
                            val nextIndex = (currentIndexInEnabled + 1) % enabledColors.size
                            selectedCaseColor = enabledColors[nextIndex]
                            soundSynthesizer.playRelease()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(selectedCaseColor.getActualColor(sharedPrefs))
                            .border(0.5.dp, caseDotBorder, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCaseColor.displayName,
                        color = topBarContentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 5. Mute Speaker Button (Squarish, height 28dp, radius 6dp)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(pillBg)
                        .then(
                            if (!isDarkTheme) Modifier.border(0.5.dp, pillBorderColor, RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .clickable {
                            val muted = !isMuted
                            isMuted = muted
                            soundSynthesizer.setMute(muted)
                            sharedPrefs.edit { putBoolean("key_sound_enabled", !muted) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute",
                        tint = topBarContentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 2. Main Mechanical Keyboard Chassis (centered with custom margins)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .screenEntranceMain(entranceState)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            KeyboardView(
                layoutType = selectedLayoutType,
                caseColor = selectedCaseColor,
                activePressedKeys = activePressedKeys,
                isConnected = isConnected,
                isCapsLockActive = isCapsLockActive,
                isNumLockActive = isNumLockActive,
                isScrollLockActive = isScrollLockActive,
                isDarkTheme = isDarkTheme,
                keySensitivity = keySensitivity,
                onKeyPressChange = { code, press -> handleLocalKeyPress(code, press) }
            )
        }
    }
}
