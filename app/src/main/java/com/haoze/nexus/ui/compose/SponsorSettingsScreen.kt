package com.haoze.claudekeyboard.ui.compose

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haoze.claudekeyboard.R

@Composable
fun SponsorSettingsScreen(onBack: () -> Unit) {
    SettingsScaffold(title = "赞助", onBack = onBack) { innerPadding ->
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
                Text("请作者喝杯蜜雪 🧋", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text("如果 SyncTouch 帮助到了你，欢迎请作者喝杯蜜雪。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentQrCode(R.drawable.alipay_code, "支付宝付款码", Modifier.weight(1f))
                    PaymentQrCode(R.drawable.wechatpay_code, "微信付款码", Modifier.weight(1f))
                }
                Text("付款时请备注您的网名或希望展示的名称，方便后续记录您的支持。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SettingsGroupTitle("你的每一笔支持都会用于")
            SettingsSurfaceGroup(
                content = listOf {
                    SponsorList(listOf("持续开发输入控制功能", "修复 Bug", "维护设备兼容性"))
                }
            )

            SettingsGroupTitle("即使不捐赠，也欢迎")
            SettingsSurfaceGroup(
                content = listOf {
                    SponsorList(listOf("点一个 Star⭐", "提交 Issue", "提交 PR", "分享给更多人"))
                }
            )

            Text(
                "感谢每一位支持项目的朋友！",
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
