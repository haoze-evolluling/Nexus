package com.haoze.claudekeyboard.ui.compose

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haoze.claudekeyboard.R
import kotlin.math.sqrt

private const val PROJECT_REPOSITORY_URL = "https://github.com/haoze-evolluling/SyncTouch"

private data class AboutCapability(val title: String, val description: String)

private val aboutCapabilities = listOf(
    AboutCapability("蓝牙键盘", "通过蓝牙 HID 将手机输入转换为键盘按键。"),
    AboutCapability("触控板", "提供光标移动、点击和双指滚动等鼠标控制。"),
    AboutCapability("电视遥控", "发送方向、媒体、音量和电源等遥控按键。"),
    AboutCapability("Agent 快捷命令", "用可编辑的快捷按钮快速发送常用命令。"),
    AboutCapability("设备连接", "管理已配对设备，并支持自动连接与断线重连。"),
    AboutCapability("快捷设置", "通过系统快捷设置磁贴查看并进入连接状态。")
)

private val aboutBoundaries = listOf(
    "本机蓝牙直连" to "SyncTouch 通过 Android 蓝牙 HID 与已配对设备直接通信，不经过远程服务器。",
    "设备兼容性" to "接收设备需要支持相应的蓝牙 HID 键盘、鼠标或遥控输入能力；实际表现取决于设备系统和蓝牙环境。",
    "本地数据存储" to "已连接设备信息、应用偏好和自定义快捷命令保存在本机，用于恢复你的使用习惯。"
)

@Composable
fun AboutSettingsScreen(onBack: () -> Unit, versionName: String, isConnected: Boolean) {
    val context = LocalContext.current
    val openRepository = {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_REPOSITORY_URL))) }
            .onFailure { Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show() }
        Unit
    }

    SettingsScaffold(
        title = "应用信息",
        onBack = onBack,
        titleTrailing = {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(8.dp)
                    .background(
                        if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        CircleShape
                    )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 0.dp,
                end = 16.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(SettingsSectionSpacing)
        ) {
            item { AboutHero(versionName) }
            item { AboutSectionHeading("核心能力", "01 / CAPABILITIES"); CapabilityGrid() }
            item { AboutSectionHeading("运行边界", "02 / ARCHITECTURE"); BoundaryGrid() }
            item { ProjectCard(openRepository) }
            item {
                Text(
                    text = "SYNCTOUCH / CONTROL AT YOUR FINGERTIPS",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AboutHero(versionName: String) {
    Card(
        shape = SettingsCornerShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        BoxWithConstraints(modifier = Modifier.padding(22.dp)) {
            if (maxWidth < 680.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { HeroCopy(versionName); HidPathDiagram() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeroCopy(versionName, Modifier.weight(1f))
                    HidPathDiagram(Modifier.widthIn(min = 300.dp, max = 360.dp).weight(0.65f))
                }
            }
        }
    }
}

@Composable
private fun HeroCopy(versionName: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("// BLUETOOTH HID CONTROL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("SyncTouch 蓝牙输入控制", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("将手机变为蓝牙键盘、触控板和遥控器，让常用输入与设备控制触手可及。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutBadge("版本 ${versionName.ifBlank { "--" }}")
            AboutBadge("Bluetooth HID")
            AboutBadge("本地直连")
        }
    }
}

@Composable
private fun AboutBadge(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun HidPathDiagram(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Card(modifier = modifier.fillMaxWidth().height(205.dp), shape = SettingsCornerShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)), border = BorderStroke(1.dp, outline.copy(alpha = 0.35f))) {
        Canvas(Modifier.fillMaxWidth().height(205.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
            val radius = 20.dp.toPx()
            val phone = androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.60f)
            val hid = androidx.compose.ui.geometry.Offset(size.width * 0.50f, size.height * 0.26f)
            val device = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.67f)
            drawNodeConnection(phone, hid, radius, outline)
            drawNodeConnection(hid, device, radius, secondary)
            listOf(phone to primary, hid to tertiary, device to secondary).forEach { (center, color) ->
                drawCircle(color, radius, center, style = Stroke(2.dp.toPx()))
                drawCircle(color, 5.dp.toPx(), center)
            }
            val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = labelColor.toArgb(); textSize = 10.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.apply {
                drawText("PHONE", phone.x, phone.y + radius + 18.dp.toPx(), labelPaint)
                drawText("BLUETOOTH HID", hid.x, hid.y + radius + 18.dp.toPx(), labelPaint)
                drawText("DEVICE", device.x, device.y + radius + 18.dp.toPx(), labelPaint)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNodeConnection(start: androidx.compose.ui.geometry.Offset, end: androidx.compose.ui.geometry.Offset, nodeRadius: Float, color: Color) {
    val deltaX = end.x - start.x
    val deltaY = end.y - start.y
    val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
    if (distance <= nodeRadius * 2) return
    val directionX = deltaX / distance
    val directionY = deltaY / distance
    drawLine(color, androidx.compose.ui.geometry.Offset(start.x + directionX * nodeRadius, start.y + directionY * nodeRadius), androidx.compose.ui.geometry.Offset(end.x - directionX * nodeRadius, end.y - directionY * nodeRadius), 3.dp.toPx())
}

@Composable
private fun AboutSectionHeading(title: String, index: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(index, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CapabilityGrid() {
    BoxWithConstraints {
        val columns = if (maxWidth >= 680.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            aboutCapabilities.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { index, capability -> CapabilityCard(capability, rowIndex * columns + index + 1, Modifier.weight(1f)) }
                    repeat(columns - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CapabilityCard(capability: AboutCapability, index: Int, modifier: Modifier) {
    Card(modifier = modifier.fillMaxHeight(), shape = SettingsCornerShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("[%02d]".format(index), color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(capability.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(capability.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BoundaryGrid() {
    BoxWithConstraints {
        if (maxWidth >= 680.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { aboutBoundaries.forEachIndexed { index, boundary -> BoundaryCard(boundary.first, boundary.second, index, Modifier.weight(1f)) } }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { aboutBoundaries.forEachIndexed { index, boundary -> BoundaryCard(boundary.first, boundary.second, index, Modifier.fillMaxWidth()) } }
        }
    }
}

@Composable
private fun BoundaryCard(title: String, description: String, index: Int, modifier: Modifier) {
    val accents = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
    Column(modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(thickness = 2.dp, color = accents[index])
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ProjectCard(onOpenRepository: () -> Unit) {
    Card(shape = SettingsCornerShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("MAINTAINED BY", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("haoze-evolluling", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("SyncTouch 开源项目", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenRepository), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("开源项目仓库", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(PROJECT_REPOSITORY_URL.removePrefix("https://"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "打开项目仓库", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
