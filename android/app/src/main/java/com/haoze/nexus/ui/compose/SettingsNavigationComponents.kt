package com.haoze.nexus.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ==========================================
// 兼容性保留与辅助组件 (导航组、操作按钮、加载与说明文本)
// ==========================================

data class SettingsNavigationItemData(
    val title: String,
    val subtitle: String? = null,
    val leadingIcon: ImageVector? = null,
    val value: String? = null,
    val valueMaxScreenFraction: Float? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val contentPadding: PaddingValues? = null
)

@Composable
fun SettingsGroupTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = 24.dp, top = 20.dp, bottom = 8.dp, end = 24.dp)
            .fillMaxWidth()
    )
}

@Composable
fun SettingsSurfaceGroup(
    content: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    groupContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    if (content.isEmpty()) return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(groupContentPadding),
        shape = SettingsCardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content.forEachIndexed { index, itemContent ->
                itemContent()
                if (index < content.size - 1) {
                    SettingsItemDivider()
                }
            }
        }
    }
}

@Composable
fun SettingsSurfaceItem(
    index: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content
    )
}

@Composable
fun SettingsNavigationGroup(
    items: List<SettingsNavigationItemData>,
    modifier: Modifier = Modifier
) {
    SettingsSurfaceGroup(
        modifier = modifier,
        content = items.map { item ->
            {
                SettingsNavigationItem(
                    title = item.title,
                    subtitle = item.subtitle,
                    leadingIcon = item.leadingIcon,
                    value = item.value,
                    valueMaxScreenFraction = item.valueMaxScreenFraction,
                    enabled = item.enabled,
                    onClick = item.onClick,
                    contentPadding = item.contentPadding ?: DefaultSettingsItemContentPadding
                )
            }
        }
    )
}

@Composable
fun SettingsNavigationItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    value: String? = null,
    valueMaxScreenFraction: Float? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    contentPadding: PaddingValues = DefaultSettingsItemContentPadding
) {
    val configuration = LocalConfiguration.current
    val valueMaxWidth = valueMaxScreenFraction
        ?.coerceIn(0.1f, 1f)
        ?.let { (configuration.screenWidthDp.dp * it) }

    SettingsItem(
        title = title,
        subtitle = subtitle,
        leadingIcon = leadingIcon,
        modifier = modifier,
        contentPadding = contentPadding,
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.38f
                    ),
                    textAlign = TextAlign.End,
                    modifier = valueMaxWidth?.let { maxWidth ->
                        Modifier.widthIn(max = maxWidth)
                    } ?: Modifier
                )
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = SettingsCornerShape,
        content = content
    )
}

@Composable
fun SettingsOutlinedActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = SettingsCornerShape,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun SettingsInfoText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 24.dp, top = 4.dp, bottom = 8.dp, end = 24.dp)
            .fillMaxWidth()
    )
}

@Composable
fun SettingsLoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
