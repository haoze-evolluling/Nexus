package com.haoze.nexus.ui.compose

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haoze.nexus.R
import com.haoze.nexus.ui.tvremote.CircularDpad
import com.haoze.nexus.ui.tvremote.DpadDirection
import com.haoze.nexus.util.performKeyClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TvRemoteScreen(
    enabled: Boolean,
    onBack: () -> Unit,
    onAction: (TvRemoteAction) -> Unit
) {
    val colors = rememberTvRemoteColors()
    val scope = rememberCoroutineScope()
    var ledActive by remember { mutableStateOf(false) }

    fun runAction(action: TvRemoteAction) {
        if (!enabled) return
        ledActive = true
        onAction(action)
        scope.launch {
            delay(150)
            ledActive = false
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.home_tvremote_title),
        onBack = onBack
    ) { innerPadding ->
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TvRemoteTopRow(
                    colors = colors,
                    ledActive = ledActive,
                    enabled = enabled,
                    onPower = { runAction(TvRemoteAction.POWER) }
                )
                TvRemoteDpad(
                    colors = colors,
                    enabled = enabled,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .size(212.dp),
                    onAction = ::runAction
                )
                RemoteTwoButtonRow(
                    modifier = Modifier.padding(top = 20.dp),
                    colors = colors,
                    enabled = enabled,
                    left = RemoteButtonSpec(
                        label = stringResource(R.string.tvremote_back),
                        iconRes = R.drawable.ic_arrow_back,
                        iconBackground = colors.primaryContainer,
                        iconTint = colors.onPrimaryContainer,
                        onClick = { runAction(TvRemoteAction.BACK) }
                    ),
                    right = RemoteButtonSpec(
                        label = stringResource(R.string.tvremote_assistant),
                        iconRes = R.drawable.ic_assistant,
                        iconBackground = colors.tertiaryContainer,
                        iconTint = colors.onTertiaryContainer,
                        onClick = { runAction(TvRemoteAction.ASSISTANT) }
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.width(130.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RemoteCapsuleButton(
                            spec = RemoteButtonSpec(
                                label = stringResource(R.string.tvremote_home),
                                iconRes = R.drawable.ic_home,
                                iconBackground = colors.primaryContainer,
                                iconTint = colors.onPrimaryContainer,
                                onClick = { runAction(TvRemoteAction.HOME) }
                            ),
                            colors = colors,
                            enabled = enabled
                        )
                        Spacer(Modifier.height(18.dp))
                        RemoteCapsuleButton(
                            spec = RemoteButtonSpec(
                                label = stringResource(R.string.tvremote_mute),
                                iconRes = R.drawable.ic_volume_off,
                                iconBackground = colors.secondaryContainer,
                                iconTint = colors.onSecondaryContainer,
                                onClick = { runAction(TvRemoteAction.MUTE) }
                            ),
                            colors = colors,
                            enabled = enabled
                        )
                    }
                    Spacer(Modifier.width(28.dp))
                    VolumeStack(colors = colors, enabled = enabled, onAction = ::runAction)
                }
                MediaControlRow(
                    colors = colors,
                    enabled = enabled,
                    modifier = Modifier.padding(top = 20.dp),
                    onAction = ::runAction
                )
            }
        }
    }
}

@Composable
private fun TvRemoteTopRow(
    colors: TvRemoteColors,
    ledActive: Boolean,
    enabled: Boolean,
    onPower: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (ledActive) colors.primary else colors.outlineVariant)
            )
            Text(
                text = stringResource(R.string.tvremote_led_label),
                modifier = Modifier.padding(start = 10.dp),
                color = colors.primary,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.weight(1f))
        IconOnlyCircleButton(
            iconRes = R.drawable.baseline_power_settings_new_24,
            contentDescription = stringResource(R.string.tvremote_power),
            colors = colors,
            enabled = enabled,
            onClick = onPower,
            background = colors.errorContainer,
            iconTint = colors.error,
            rippleColor = colors.error
        )
    }
}

@Composable
private fun TvRemoteDpad(
    colors: TvRemoteColors,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onAction: (TvRemoteAction) -> Unit
) {
    CircularDpad(
        modifier = modifier.alpha(if (enabled) 1f else 0.4f),
        enabled = enabled,
        outerRadius = 106.dp,
        innerRadius = 51.dp,
        repeatDelay = 200L,
        ringColor = colors.controlSurface,
        ringBorderColor = colors.dpadBorder,
        centerColor = colors.controlSurface,
        centerBorderColor = colors.dpadBorder,
        dividerColor = colors.dpadBorder,
        highlightColor = colors.pressedStateLayer,
        iconColor = colors.primary,
        textColor = colors.onSurface,
        onDirection = { direction ->
            onAction(
                when (direction) {
                    DpadDirection.UP -> TvRemoteAction.UP
                    DpadDirection.DOWN -> TvRemoteAction.DOWN
                    DpadDirection.LEFT -> TvRemoteAction.LEFT
                    DpadDirection.RIGHT -> TvRemoteAction.RIGHT
                }
            )
        },
        onConfirm = { onAction(TvRemoteAction.CONFIRM) }
    )
}

@Composable
private fun RemoteTwoButtonRow(
    modifier: Modifier = Modifier,
    colors: TvRemoteColors,
    enabled: Boolean,
    left: RemoteButtonSpec,
    right: RemoteButtonSpec
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        RemoteCapsuleButton(spec = left, colors = colors, enabled = enabled)
        Spacer(Modifier.width(28.dp))
        RemoteCapsuleButton(spec = right, colors = colors, enabled = enabled)
    }
}

@Composable
private fun RemoteCapsuleButton(
    spec: RemoteButtonSpec,
    colors: TvRemoteColors,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .width(130.dp)
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.controlSurface)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = null,
                indication = ripple(color = colors.primary),
                enabled = enabled
            ) {
                view.performKeyClick()
                spec.onClick()
            }
            .semantics { contentDescription = spec.label }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteIconBadge(
            iconRes = spec.iconRes,
            background = spec.iconBackground,
            tint = spec.iconTint
        )
        Text(
            text = spec.label,
            modifier = Modifier.padding(start = 10.dp),
            color = colors.onSurface,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IconOnlyCircleButton(
    iconRes: Int,
    contentDescription: String,
    colors: TvRemoteColors,
    enabled: Boolean,
    onClick: () -> Unit,
    background: Color = colors.controlSurface,
    iconTint: Color = colors.onSurfaceVariant,
    rippleColor: Color = colors.primary
) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .size(60.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .clickable(
                interactionSource = null,
                indication = ripple(color = rippleColor),
                enabled = enabled
            ) {
                view.performKeyClick()
                onClick()
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun VolumeStack(
    colors: TvRemoteColors,
    enabled: Boolean,
    onAction: (TvRemoteAction) -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.controlSurface)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        VolumeRow(
            label = stringResource(R.string.tvremote_volume_up),
            iconRes = R.drawable.ic_volume_up,
            colors = colors,
            enabled = enabled,
            onClick = { onAction(TvRemoteAction.VOLUME_UP) }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colors.outlineVariant)
        )
        VolumeRow(
            label = stringResource(R.string.tvremote_volume_down),
            iconRes = R.drawable.ic_volume_down,
            colors = colors,
            enabled = enabled,
            onClick = { onAction(TvRemoteAction.VOLUME_DOWN) }
        )
    }
}

@Composable
private fun VolumeRow(
    label: String,
    iconRes: Int,
    colors: TvRemoteColors,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(
                interactionSource = null,
                indication = ripple(color = colors.primary),
                enabled = enabled
            ) {
                view.performKeyClick()
                onClick()
            }
            .semantics { contentDescription = label }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteIconBadge(
            iconRes = iconRes,
            background = colors.secondaryContainer,
            tint = colors.onSecondaryContainer
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 10.dp),
            color = colors.onSurface,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MediaControlRow(
    colors: TvRemoteColors,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onAction: (TvRemoteAction) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        MediaIconButton(
            iconRes = R.drawable.ic_skip_previous,
            contentDescription = stringResource(R.string.tvremote_previous),
            colors = colors,
            enabled = enabled,
            onClick = { onAction(TvRemoteAction.PREVIOUS) }
        )
        Spacer(Modifier.width(18.dp))
        MediaIconButton(
            iconRes = R.drawable.ic_play_arrow,
            contentDescription = stringResource(R.string.tvremote_play_pause),
            colors = colors,
            enabled = enabled,
            onClick = { onAction(TvRemoteAction.PLAY_PAUSE) }
        )
        Spacer(Modifier.width(18.dp))
        MediaIconButton(
            iconRes = R.drawable.ic_skip_next,
            contentDescription = stringResource(R.string.tvremote_next),
            colors = colors,
            enabled = enabled,
            onClick = { onAction(TvRemoteAction.NEXT) }
        )
        Spacer(Modifier.width(18.dp))
        MediaIconButton(
            iconRes = R.drawable.ic_stop,
            contentDescription = stringResource(R.string.tvremote_stop),
            colors = colors,
            enabled = enabled,
            onClick = { onAction(TvRemoteAction.STOP) }
        )
    }
}

@Composable
private fun MediaIconButton(
    iconRes: Int,
    contentDescription: String,
    colors: TvRemoteColors,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.controlSurface)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = null,
                indication = ripple(color = colors.primary),
                enabled = enabled
            ) {
                view.performKeyClick()
                onClick()
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        RemoteIconBadge(
            iconRes = iconRes,
            background = colors.secondaryContainer,
            tint = colors.onSecondaryContainer
        )
    }
}

@Composable
private fun RemoteIconBadge(
    iconRes: Int,
    background: Color,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

private data class RemoteButtonSpec(
    val label: String,
    val iconRes: Int,
    val iconBackground: Color,
    val iconTint: Color,
    val onClick: () -> Unit
)

private data class TvRemoteColors(
    val surface: Color,
    val controlSurface: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val dpadBorder: Color
) {
    val pressedStateLayer: Color
        get() = primary.copy(alpha = 0.12f)
}

@Composable
private fun rememberTvRemoteColors(): TvRemoteColors {
    val isDark = when (ThemeController.nightModeIndex) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    return if (isDark) DarkTvRemoteColors else LightTvRemoteColors
}

private val LightTvRemoteColors = TvRemoteColors(
    surface = Color(0xFFF8F9FA),
    controlSurface = Color.White,
    outlineVariant = Color(0xFFC4C7C5),
    primary = Color(0xFF0B57D0),
    onSurface = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFF444746),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondaryContainer = Color(0xFFE8EAED),
    onSecondaryContainer = Color(0xFF202124),
    tertiaryContainer = Color(0xFFC9E6FF),
    onTertiaryContainer = Color(0xFF003355),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    dpadBorder = Color(0xFF747775)
)

private val DarkTvRemoteColors = TvRemoteColors(
    surface = Color(0xFF111315),
    controlSurface = Color(0xFF24292E),
    outlineVariant = Color(0xFF444746),
    primary = Color(0xFFA8C7FA),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC4C7C5),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondaryContainer = Color(0xFF2B3035),
    onSecondaryContainer = Color(0xFFE2E2E6),
    tertiaryContainer = Color(0xFF004A77),
    onTertiaryContainer = Color(0xFFC9E6FF),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    dpadBorder = Color(0xFF8E918F)
)
