package com.haoze.nexus.ui.compose

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// ==========================================
// 设置持久化与状态工具
// ==========================================

@Composable
internal fun settingsPrefs(): SharedPreferences =
    LocalContext.current.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

internal fun settingsPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

@Composable
internal fun rememberBooleanSetting(
    prefs: SharedPreferences,
    key: String,
    defaultValue: Boolean
) = remember(key) { mutableStateOf(prefs.getBoolean(key, defaultValue)) }

@Composable
internal fun rememberIntSetting(
    prefs: SharedPreferences,
    key: String,
    defaultValue: Int
) = remember(key) { mutableIntStateOf(prefs.getInt(key, defaultValue)) }

internal fun saveBoolean(
    prefs: SharedPreferences,
    key: String,
    value: Boolean,
    onBooleanSettingChanged: (String, Boolean) -> Unit
) {
    prefs.edit().putBoolean(key, value).apply()
    onBooleanSettingChanged(key, value)
}

fun getThemeModeIndex(context: Context): Int = ThemeController.nightModeIndex

fun setThemeModeIndex(context: Context, index: Int) {
    ThemeController.setNightModeIndex(context, index)
}

fun getThemeColorStyle(context: Context): ThemeColorStyle =
    ThemeColorStyle.fromStorageValue(
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE).getString("theme_color_style", null)
    )

fun setThemeColorStyle(context: Context, style: ThemeColorStyle) {
    context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        .edit().putString("theme_color_style", style.storageValue).apply()
}
