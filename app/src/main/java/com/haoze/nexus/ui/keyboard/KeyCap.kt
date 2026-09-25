package com.haoze.claudekeyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeyCap(
    legend: String,
    shiftedLegend: String,
    width: Dp,
    height: Dp,
    isPressed: Boolean,
    keyBgColor: Color,
    legendColor: Color,
    baseUnitWidth: Dp,
    modifier: Modifier = Modifier,
    glowColor: Color? = null,
    isDarkTheme: Boolean = true
) {
    val keyBorderColor = keyBgColor.darker()

    // Proportional radii mapped directly from KBSim's 5px/3px on a 54px keysize
    val outerRadius = baseUnitWidth * 0.092f
    val innerRadius = baseUnitWidth * 0.055f

    // Inset padding mapped directly from KBSim's keysize/9 and keysize/18
    val insetX = baseUnitWidth / 9f
    val insetTop = baseUnitWidth / 18f
    val insetBottom = baseUnitWidth / 6f // keysize * 3 / 18

    val outerBorderColor = if (isDarkTheme) Color.Black.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.22f)
    val innerBorderColor = if (isDarkTheme) Color.Black.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .testTag("key_$legend")
            .padding(1.dp)
    ) {
        // Underglow if active
        if (glowColor != null && glowColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .background(glowColor.copy(alpha = glowColor.alpha * 0.45f), RoundedCornerShape(outerRadius))
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(outerRadius), spotColor = glowColor)
            )
        }

        // 1. Key Border Base Bezel (KBSim .keyborder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(keyBorderColor, RoundedCornerShape(outerRadius))
                .border(1.dp, outerBorderColor, RoundedCornerShape(outerRadius))
        )

        // 2. Key Top Bevel (KBSim .keytop, inset and styled)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = insetX,
                    end = insetX,
                    top = insetTop,
                    bottom = insetBottom
                )
                .background(
                    color = if (isPressed) keyBorderColor else keyBgColor,
                    shape = RoundedCornerShape(innerRadius)
                )
                .border(1.dp, innerBorderColor, RoundedCornerShape(innerRadius)),
            contentAlignment = Alignment.Center
        ) {
            val mainFontSize = (baseUnitWidth.value * 0.24f).coerceIn(7f, 13f).sp
            val shiftFontSize = (baseUnitWidth.value * 0.17f).coerceIn(5f, 9f).sp

            if (shiftedLegend.isNotEmpty()) {
                // Stack legends inside a column so the secondary text sits exactly slightly above the primary with perfect alignment
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = 3.dp)
                        .padding(
                            start = (baseUnitWidth.value * 0.08f).dp,
                            end = (baseUnitWidth.value * 0.08f).dp,
                            top = 0.dp,
                            bottom = 0.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = shiftedLegend,
                        fontSize = shiftFontSize,
                        color = legendColor.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = legend,
                        fontSize = mainFontSize,
                        color = legendColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            } else {
                // Alphabetical or single legend, centered
                Text(
                    text = legend,
                    fontSize = if (legend.length > 3) (mainFontSize.value * 0.82f).sp else mainFontSize,
                    color = legendColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

// Extension function to darken mechanical key highlights on pressing
fun Color.darker(): Color {
    return Color(
        red = (this.red * 0.82f).coerceIn(0f, 1f),
        green = (this.green * 0.82f).coerceIn(0f, 1f),
        blue = (this.blue * 0.82f).coerceIn(0f, 1f),
        alpha = this.alpha
    )
}
