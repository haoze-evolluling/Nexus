package com.haoze.nexus.ui.compose.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.ui.compose.SettingsActionItem
import com.haoze.nexus.ui.compose.SettingsCard
import com.haoze.nexus.ui.compose.SettingsItemDivider

/**
 * 设置主页 - 关于与支持配置卡片 (关于软件、赞助作者、致谢列表、GitHub 仓库)
 */
@Composable
fun AboutSettingsCard(
    versionName: String,
    onNavigateToAbout: (() -> Unit)?,
    onNavigateToSponsor: (() -> Unit)?,
    onNavigateToSponsorList: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    SettingsCard(modifier = modifier) {
        SettingsActionItem(
            title = stringResource(R.string.home_about_title),
            subtitle = if (versionName.isNotBlank()) stringResource(R.string.about_section_version_prefix, versionName) else stringResource(R.string.about_section_version_empty),
            leadingIcon = Icons.Default.Info,
            onClick = { onNavigateToAbout?.invoke() },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        SettingsItemDivider()

        SettingsActionItem(
            title = stringResource(R.string.home_sponsor_title),
            subtitle = stringResource(R.string.about_section_sponsor_subtitle),
            leadingIcon = Icons.Default.Favorite,
            onClick = { onNavigateToSponsor?.invoke() },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        SettingsItemDivider()

        SettingsActionItem(
            title = stringResource(R.string.home_sponsor_list_title),
            subtitle = stringResource(R.string.about_section_sponsor_list_subtitle),
            leadingIcon = Icons.Default.WorkspacePremium,
            onClick = { onNavigateToSponsorList?.invoke() },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        SettingsItemDivider()

        SettingsActionItem(
            title = stringResource(R.string.settings_github_repo),
            subtitle = stringResource(R.string.settings_github_url),
            leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/haoze-evolluling/Nexus"))
                    )
                }
            },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}

/**
 * 设置主页底部品牌与版本信息
 */
@Composable
fun SettingsVersionFooter(
    versionName: String,
    modifier: Modifier = Modifier
) {
    Spacer(modifier.height(12.dp))
    Text(
        text = "Nexus ${if (versionName.isNotBlank()) "v$versionName" else ""} · Control at Your Fingertips",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
