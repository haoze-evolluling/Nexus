package com.haoze.nexus.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haoze.nexus.util.performKeyClick

internal data class RemoteButtonSpec(
    val label: String,
    val iconRes: Int,
    val iconBackground: Color,
    val iconTint: Color,
    val onClick: () -> Unit
)

@Composable
internal fun RemoteIconBadge(
    iconRes: Int,
    background: Color,
    tint: Color,
    badgeSize: Dp = 34.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = Modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun RemoteCapsuleButton(
    spec: RemoteButtonSpec,
    colors: TvRemoteColors,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp
) {
    val view = LocalView.current
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .width(130.dp)
            .height(height)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(colors.controlSurface)
            .border(BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)), shape)
            .clickable(
                interactionSource = null,
                indication = ripple(color = colors.primary),
                enabled = enabled
            ) {
                view.performKeyClick()
                spec.onClick()
            }
            .semantics { contentDescription = spec.label }
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteIconBadge(
            iconRes = spec.iconRes,
            background = spec.iconBackground,
            tint = spec.iconTint,
            badgeSize = 32.dp,
            iconSize = 18.dp
        )
        Text(
            text = spec.label,
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
internal fun IconOnlyCircleButton(
    iconRes: Int,
    contentDescription: String,
    colors: TvRemoteColors,
    enabled: Boolean,
    onClick: () -> Unit,
    background: Color = colors.controlSurface,
    iconTint: Color = colors.onSurfaceVariant,
    rippleColor: Color = colors.primary,
    size: Dp = 46.dp,
    iconSize: Dp = 22.dp,
    hasBorder: Boolean = false
) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .background(background)
            .then(
                if (hasBorder) {
                    Modifier.border(BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)), CircleShape)
                } else {
                    Modifier
                }
            )
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
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun RemoteTwoButtonRow(
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
