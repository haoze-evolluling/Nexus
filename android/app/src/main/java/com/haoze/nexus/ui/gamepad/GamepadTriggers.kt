package com.haoze.nexus.ui.gamepad

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun GamepadBumperButton(
    button: ButtonDef,
    isLeft: Boolean,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) (-0.6).dp else 0.dp,
        animationSpec = tween(durationMillis = 70, easing = LinearEasing),
        label = "bumperPress"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = tween(durationMillis = 70, easing = LinearEasing),
        label = "bumperScale"
    )

    val outerShape = if (isLeft) {
        RoundedCornerShape(topStart = 14.dp, topEnd = 6.dp, bottomStart = 6.dp, bottomEnd = 10.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, topEnd = 14.dp, bottomStart = 10.dp, bottomEnd = 6.dp)
    }
    
    val innerShape = if (isLeft) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 8.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 8.dp, bottomEnd = 4.dp)
    }

    // Outer well container (widened to 120.dp)
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(34.dp)
            .background(Color(0xFF1B1B1C), outerShape)
            .border(1.2.dp, Color.Black.copy(alpha = 0.5f), outerShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner button cap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp) // Well padding
                .offset(y = pressOffsetY)
                .graphicsLayer {
                    scaleY = pressScale
                }
                .clip(innerShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(Color(0xFF202022), Color(0xFF19191B))
                        } else {
                            listOf(Color(0xFF38383B), Color(0xFF2D2D30))
                        }
                    )
                )
                .border(
                    0.8.dp,
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    innerShape
                )
                .gamepadButtonTouch(
                    onPress = { onPress(button.mappingId) },
                    onRelease = { onRelease(button.mappingId) },
                    onPressedStateChange = { isPressed = it }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = button.label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
internal fun GamepadTriggerButton(
    button: ButtonDef,
    isLeft: Boolean,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 0.6.dp else 0.dp,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "triggerPress"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "triggerScale"
    )

    val outerShape = if (isLeft) {
        RoundedCornerShape(topStart = 10.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 8.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, topEnd = 10.dp, bottomStart = 8.dp, bottomEnd = 14.dp)
    }

    val innerShape = if (isLeft) {
        RoundedCornerShape(topStart = 8.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 6.dp, bottomEnd = 12.dp)
    }

    // Outer well container (widened to 120.dp)
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(48.dp)
            .background(Color(0xFF1B1B1C), outerShape)
            .border(1.2.dp, Color.Black.copy(alpha = 0.5f), outerShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner button cap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp) // Well padding
                .offset(y = pressOffsetY)
                .graphicsLayer {
                    scaleY = pressScale
                }
                .clip(innerShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(Color(0xFF202022), Color(0xFF19191B))
                        } else {
                            listOf(Color(0xFF38383B), Color(0xFF2D2D30))
                        }
                    )
                )
                .border(
                    0.8.dp,
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    innerShape
                )
                .gamepadButtonTouch(
                    onPress = { onPress(button.mappingId) },
                    onRelease = { onRelease(button.mappingId) },
                    onPressedStateChange = { isPressed = it }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = button.label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
