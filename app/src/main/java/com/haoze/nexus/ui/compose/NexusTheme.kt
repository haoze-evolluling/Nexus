package com.haoze.nexus.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

private val Purple80 = Color(0xFFD0BCFF)
private val PurpleGrey80 = Color(0xFFCCC2DC)
private val Pink80 = Color(0xFFEFB8C8)

private val Purple40 = Color(0xFF6650a4)
private val PurpleGrey40 = Color(0xFF625b71)
private val Pink40 = Color(0xFF7D5260)

/**
 * 主题色风格：跟随系统动态取色，或八种固定主题色。
 * 色值与 DITING 主题体系保持一致。
 */
enum class ThemeColorStyle(
    val storageValue: String,
    val displayName: String,
    val lightPrimary: Color,
    val darkPrimary: Color,
    val lightPrimaryContainer: Color,
    val darkPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val lightSecondary: Color,
    val darkSecondary: Color,
    val lightTertiary: Color,
    val darkTertiary: Color
) {
    SYSTEM("system", "跟随系统", Purple40, Purple80, Color(0xFFEADDFF), Color(0xFF4F378B), Color(0xFF21005D), Color(0xFFEADDFF), PurpleGrey40, PurpleGrey80, Pink40, Pink80),
    INDIGO("indigo", "经典靛蓝", Color(0xFF3F51B5), Color(0xFFBAC3FF), Color(0xFFDEE0FF), Color(0xFF25389C), Color(0xFF00105C), Color(0xFFDEE0FF), Color(0xFF5B5D72), Color(0xFFC3C5DD), Color(0xFF77536D), Color(0xFFE5BAD8)),
    BLUE("blue", "睡莲蓝", Color(0xFF00639C), Color(0xFF98CBFF), Color(0xFFCFE5FF), Color(0xFF004A77), Color(0xFF001D33), Color(0xFFCFE5FF), Color(0xFF526070), Color(0xFFBAC8DB), Color(0xFF695779), Color(0xFFD5BEE5)),
    CYAN("cyan", "池水青", Color(0xFF006A64), Color(0xFF4EDAD0), Color(0xFF70F7EC), Color(0xFF00504B), Color(0xFF00201E), Color(0xFF70F7EC), Color(0xFF4A6360), Color(0xFFB0CCC8), Color(0xFF48617A), Color(0xFFAFC9E7)),
    GREEN("green", "柳叶绿", Color(0xFF3B693A), Color(0xFFA1D39B), Color(0xFFBCF0B5), Color(0xFF235124), Color(0xFF002204), Color(0xFFBCF0B5), Color(0xFF53634F), Color(0xFFBACCB3), Color(0xFF386568), Color(0xFFA0CFD1)),
    AMBER("amber", "琥珀金", Color(0xFF795900), Color(0xFFF6BE3D), Color(0xFFFFDF9E), Color(0xFF5B4300), Color(0xFF261A00), Color(0xFFFFDF9E), Color(0xFF6B5D3F), Color(0xFFD7C5A0), Color(0xFF496547), Color(0xFFB0CDAC)),
    ORANGE("orange", "落日橙", Color(0xFF944B00), Color(0xFFFFB688), Color(0xFFFFDCC5), Color(0xFF723700), Color(0xFF301400), Color(0xFFFFDCC5), Color(0xFF755846), Color(0xFFE5BEA9), Color(0xFF645F31), Color(0xFFCEC790)),
    PINK("pink", "暮霞粉", Color(0xFF934164), Color(0xFFFFAFD0), Color(0xFFFFD9E2), Color(0xFF75294C), Color(0xFF3E001F), Color(0xFFFFD9E2), Color(0xFF74565F), Color(0xFFE2BDC7), Color(0xFF7C5635), Color(0xFFEEBD93)),
    PURPLE("purple", "鸢尾紫", Color(0xFF6750A4), Color(0xFFD0BCFF), Color(0xFFEADDFF), Color(0xFF4F378B), Color(0xFF21005D), Color(0xFFEADDFF), Color(0xFF625B71), Color(0xFFCCC2DC), Color(0xFF7D5260), Color(0xFFEFB8C8)),
    SLATE("slate", "云石灰", Color(0xFF4C616C), Color(0xFFB3C9D6), Color(0xFFCFE5F3), Color(0xFF344954), Color(0xFF071E27), Color(0xFFCFE5F3), Color(0xFF536067), Color(0xFFBBC7CE), Color(0xFF605A71), Color(0xFFCAC1DC));

    companion object {
        fun fromStorageValue(value: String?): ThemeColorStyle =
            when (value) {
                "red" -> PINK // 历史红色平滑回退至暮霞粉
                else -> entries.firstOrNull { it.storageValue == value } ?: SYSTEM
            }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun NexusTheme(
    colorStyle: ThemeColorStyle = ThemeColorStyle.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (ThemeController.nightModeIndex) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val view = LocalView.current
    val colors = when {
        colorStyle == ThemeColorStyle.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        colorStyle == ThemeColorStyle.SYSTEM -> if (darkTheme) DarkColorScheme else LightColorScheme
        darkTheme -> darkColorScheme(
            primary = colorStyle.darkPrimary,
            onPrimary = Color(0xFF1B1B1F),
            primaryContainer = colorStyle.darkPrimaryContainer,
            onPrimaryContainer = colorStyle.darkOnPrimaryContainer,
            secondary = colorStyle.darkSecondary,
            tertiary = colorStyle.darkTertiary,
            surfaceTint = colorStyle.darkPrimary
        )
        else -> lightColorScheme(
            primary = colorStyle.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = colorStyle.lightPrimaryContainer,
            onPrimaryContainer = colorStyle.lightOnPrimaryContainer,
            secondary = colorStyle.lightSecondary,
            tertiary = colorStyle.lightTertiary,
            surfaceTint = colorStyle.lightPrimary
        )
    }
    val backgroundColor = colors.background.toArgb()

    SideEffect {
        context.findActivity()?.window?.let { window ->
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.statusBarColor = backgroundColor
                window.navigationBarColor = backgroundColor
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
        view.rootView.setBackgroundColor(backgroundColor)
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
