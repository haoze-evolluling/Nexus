package com.haoze.nexus.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ==========================================
// Material 3 规范尺寸与圆角
// ==========================================

/**
 * Material 3 统一卡片圆角 (16dp, Large Corner Shape)
 */
val SettingsCardShape = RoundedCornerShape(16.dp)

/**
 * 历史组件兼容圆角
 */
val SettingsCornerShape = RoundedCornerShape(12.dp)

/**
 * 统一设置项内容内边距 (水平 16dp, 垂直 14dp)
 */
val DefaultSettingsItemContentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

/**
 * Material 3 8dp 网格体系间距
 */
val SettingsSectionSpacing = 16.dp
val SettingsItemSpacing = 2.dp
