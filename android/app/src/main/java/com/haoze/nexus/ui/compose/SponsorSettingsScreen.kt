package com.haoze.nexus.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R

@Composable
fun SponsorSettingsScreen(onBack: () -> Unit) {
    SettingsScaffold(title = stringResource(R.string.home_sponsor_title), onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.sponsor_card_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.sponsor_card_desc), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentQrCode(R.drawable.alipay_code, stringResource(R.string.sponsor_alipay), Modifier.weight(1f))
                    PaymentQrCode(R.drawable.wechatpay_code, stringResource(R.string.sponsor_wechat), Modifier.weight(1f))
                }
                Text(stringResource(R.string.sponsor_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SettingsGroupTitle(stringResource(R.string.sponsor_group_help))
            SettingsSurfaceGroup(
                content = listOf {
                    SponsorList(listOf(
                        stringResource(R.string.sponsor_help_item1),
                        stringResource(R.string.sponsor_help_item2),
                        stringResource(R.string.sponsor_help_item3)
                    ))
                }
            )

            SettingsGroupTitle(stringResource(R.string.sponsor_group_other_help))
            SettingsSurfaceGroup(
                content = listOf {
                    SponsorList(listOf(
                        stringResource(R.string.sponsor_other_item1),
                        stringResource(R.string.sponsor_other_item2),
                        stringResource(R.string.sponsor_other_item3),
                        stringResource(R.string.sponsor_other_item4)
                    ))
                }
            )

            Text(
                stringResource(R.string.sponsor_footer_thanks),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun PaymentQrCode(drawableRes: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(painterResource(drawableRes), contentDescription = label, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SponsorList(items: List<String>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Text(item, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            }
        }
    }
}
