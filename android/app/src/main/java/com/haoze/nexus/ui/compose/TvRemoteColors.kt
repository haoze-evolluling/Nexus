package com.haoze.nexus.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal data class TvRemoteColors(
    val surface: Color,
    val controlSurface: Color,
    val controlSurfaceHigh: Color = controlSurface,
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
    val onErrorContainer: Color = error,
    val dpadBorder: Color
) {
    val pressedStateLayer: Color
        get() = primary.copy(alpha = 0.14f)
}

@Composable
internal fun rememberTvRemoteColors(): TvRemoteColors {
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
    return androidx.compose.runtime.remember(colorScheme) {
        TvRemoteColors(
            surface = colorScheme.surfaceContainerLow,
            controlSurface = colorScheme.surfaceContainer,
            controlSurfaceHigh = colorScheme.surfaceContainerHigh,
            outlineVariant = colorScheme.outlineVariant,
            primary = colorScheme.primary,
            onSurface = colorScheme.onSurface,
            onSurfaceVariant = colorScheme.onSurfaceVariant,
            primaryContainer = colorScheme.primaryContainer,
            onPrimaryContainer = colorScheme.onPrimaryContainer,
            secondaryContainer = colorScheme.secondaryContainer,
            onSecondaryContainer = colorScheme.onSecondaryContainer,
            tertiaryContainer = colorScheme.tertiaryContainer,
            onTertiaryContainer = colorScheme.onTertiaryContainer,
            error = colorScheme.error,
            errorContainer = colorScheme.errorContainer,
            onErrorContainer = colorScheme.onErrorContainer,
            dpadBorder = colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    }
}

internal val LightTvRemoteColors = TvRemoteColors(
    surface = Color(0xFFF8F9FA),
    controlSurface = Color.White,
    controlSurfaceHigh = Color(0xFFF1F3F4),
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
    onErrorContainer = Color(0xFF410E0B),
    dpadBorder = Color(0xFF747775)
)

internal val DarkTvRemoteColors = TvRemoteColors(
    surface = Color(0xFF111315),
    controlSurface = Color(0xFF24292E),
    controlSurfaceHigh = Color(0xFF2E343A),
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
    onErrorContainer = Color(0xFFF9DEDC),
    dpadBorder = Color(0xFF8E918F)
)
