package com.haoze.nexus.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal data class TvRemoteColors(
    val surface: Color,
    val controlSurface: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val dpadBorder: Color
) {
    val pressedStateLayer: Color
        get() = primary.copy(alpha = 0.12f)
}

@Composable
internal fun rememberTvRemoteColors(): TvRemoteColors {
    val isDark = when (ThemeController.nightModeIndex) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    return if (isDark) DarkTvRemoteColors else LightTvRemoteColors
}

internal val LightTvRemoteColors = TvRemoteColors(
    surface = Color(0xFFF8F9FA),
    controlSurface = Color.White,
    outlineVariant = Color(0xFFC4C7C5),
    primary = Color(0xFF0B57D0),
    onSurface = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFF444746),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondaryContainer = Color(0xFFE8EAED),
    onSecondaryContainer = Color(0xFF202124),
    tertiaryContainer = Color(0xFFC9E6FF),
    onTertiaryContainer = Color(0xFF003355),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    dpadBorder = Color(0xFF747775)
)

internal val DarkTvRemoteColors = TvRemoteColors(
    surface = Color(0xFF111315),
    controlSurface = Color(0xFF24292E),
    outlineVariant = Color(0xFF444746),
    primary = Color(0xFFA8C7FA),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC4C7C5),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondaryContainer = Color(0xFF2B3035),
    onSecondaryContainer = Color(0xFFE2E2E6),
    tertiaryContainer = Color(0xFF004A77),
    onTertiaryContainer = Color(0xFFC9E6FF),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    dpadBorder = Color(0xFF8E918F)
)
