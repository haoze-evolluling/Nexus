package com.haoze.nexus.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.ui.tvremote.CircularDpad
import com.haoze.nexus.ui.tvremote.DpadDirection
import com.haoze.nexus.util.performKeyClick

@Composable
internal fun TvRemoteTopRow(
    colors: TvRemoteColors,
    ledActive: Boolean,
    enabled: Boolean,
    onPower: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.controlSurface.copy(alpha = 0.7f))
                .border(BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f)), CircleShape)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (ledActive) colors.primary else colors.outlineVariant.copy(alpha = 0.6f))
            )
            Text(
                text = stringResource(R.string.tvremote_led_label),
                modifier = Modifier.padding(start = 8.dp),
                color = if (ledActive) colors.primary else colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
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
            iconTint = colors.onErrorContainer,
            rippleColor = colors.error,
            size = 46.dp,
            iconSize = 22.dp,
            hasBorder = false
        )
    }
}

@Composable
internal fun TvRemoteDpad(
    colors: TvRemoteColors,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 196.dp,
    onAction: (TvRemoteAction) -> Unit
) {
    val outerRadius = size / 2
    val innerRadius = (size * 0.245f).coerceAtLeast(46.dp)
    CircularDpad(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.38f),
        enabled = enabled,
        outerRadius = outerRadius,
        innerRadius = innerRadius,
        repeatDelay = 200L,
        ringColor = colors.controlSurface,
        ringBorderColor = colors.dpadBorder,
        centerColor = colors.controlSurfaceHigh,
        centerBorderColor = colors.dpadBorder,
        dividerColor = colors.dpadBorder,
        highlightColor = colors.pressedStateLayer,
        iconColor = colors.onSurfaceVariant,
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
internal fun VolumeStack(
    colors: TvRemoteColors,
    enabled: Boolean,
    onAction: (TvRemoteAction) -> Unit
) {
    val containerShape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .width(130.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(containerShape)
            .background(colors.controlSurface)
            .border(BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)), containerShape)
    ) {
        VolumeRow(
            label = stringResource(R.string.tvremote_volume_up),
            iconRes = R.drawable.ic_volume_up,
            colors = colors,
            enabled = enabled,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            onClick = { onAction(TvRemoteAction.VOLUME_UP) }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outlineVariant.copy(alpha = 0.4f))
        )
        VolumeRow(
            label = stringResource(R.string.tvremote_volume_down),
            iconRes = R.drawable.ic_volume_down,
            colors = colors,
            enabled = enabled,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
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
    shape: Shape,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(53.5.dp)
            .clip(shape)
            .clickable(
                interactionSource = null,
                indication = ripple(color = colors.primary),
                enabled = enabled
            ) {
                view.performKeyClick()
                onClick()
            }
            .semantics { contentDescription = label }
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteIconBadge(
            iconRes = iconRes,
            background = colors.secondaryContainer,
            tint = colors.onSecondaryContainer,
            badgeSize = 32.dp,
            iconSize = 18.dp
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            color = colors.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun MediaControlRow(
    colors: TvRemoteColors,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onAction: (TvRemoteAction) -> Unit
) {
    Row(
        modifier = modifier.width(288.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MediaIconButton(
            iconRes = R.drawable.ic_skip_previous,
            contentDescription = stringResource(R.string.tvremote_previous),
            colors = colors,
            enabled = enabled,
            isPrimary = false,
            onClick = { onAction(TvRemoteAction.PREVIOUS) }
        )
        MediaIconButton(
            iconRes = R.drawable.ic_play_arrow,
            contentDescription = stringResource(R.string.tvremote_play_pause),
            colors = colors,
            enabled = enabled,
            isPrimary = true,
            onClick = { onAction(TvRemoteAction.PLAY_PAUSE) }
        )
        MediaIconButton(
            iconRes = R.drawable.ic_skip_next,
            contentDescription = stringResource(R.string.tvremote_next),
            colors = colors,
            enabled = enabled,
            isPrimary = false,
            onClick = { onAction(TvRemoteAction.NEXT) }
        )
        MediaIconButton(
            iconRes = R.drawable.ic_stop,
            contentDescription = stringResource(R.string.tvremote_stop),
            colors = colors,
            enabled = enabled,
            isPrimary = false,
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
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val buttonShape = RoundedCornerShape(16.dp)
    val containerColor = if (isPrimary) colors.primaryContainer else colors.controlSurface
    val contentColor = if (isPrimary) colors.onPrimaryContainer else colors.onSurfaceVariant
    val borderColor = if (isPrimary) {
        colors.primary.copy(alpha = 0.2f)
    } else {
        colors.outlineVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(buttonShape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), buttonShape)
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
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(if (isPrimary) 24.dp else 22.dp)
        )
    }
}
