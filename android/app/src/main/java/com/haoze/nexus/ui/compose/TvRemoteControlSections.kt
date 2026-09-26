package com.haoze.nexus.ui.compose

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
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
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
internal fun TvRemoteDpad(
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
internal fun VolumeStack(
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
internal fun MediaControlRow(
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
