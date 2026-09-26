package com.haoze.nexus.ui.gamepad

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R

@Composable
internal fun GamepadCenterButton(
    button: ButtonDef,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 0.6.dp else 0.dp,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "centerBtnPress"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "centerBtnScale"
    )

    // Well size 32.dp, cap size 29.dp (1.5.dp gap)
    Box(
        modifier = Modifier
            .size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Button well
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF1B1B1C))
                .border(0.8.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
        )

        // Button Cap (Flat Disk)
        Box(
            modifier = Modifier
                .size(29.dp)
                .offset(y = pressOffsetY)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(CircleShape)
                .gamepadButtonTouch(
                    onPress = { onPress(button.mappingId) },
                    onRelease = { onRelease(button.mappingId) },
                    onPressedStateChange = { isPressed = it }
                ),
            contentAlignment = Alignment.Center
        ) {
            val baseColor = Color(0xFF333336)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.width / 2f

                val maxOffset = 0.6.dp.toPx()
                val edgeOffset = if (isPressed) 0.3f.dp.toPx() else 1.5f.dp.toPx()
                val faceOffset = if (isPressed) 0f else maxOffset

                // Calculate radii to fit perfectly within the clip boundaries
                val edgeR = r - edgeOffset / 2f - 0.5.dp.toPx()
                val faceR = r - maxOffset - 0.5.dp.toPx()

                // 3D side edge
                drawCircle(
                    color = Color(0xFF1F1F21),
                    radius = edgeR,
                    center = Offset(r, r + edgeOffset / 2f)
                )

                // Top face
                val faceColor = if (isPressed) Color(0xFF232325) else baseColor
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(faceColor.gpLighter(0.08f), faceColor.gpDarker(0.12f))
                    ),
                    radius = faceR,
                    center = Offset(r, r - faceOffset)
                )

                // Bevel highlight (vertical gradient brush for better shading)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                        } else {
                            listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                        }
                    ),
                    radius = faceR - 0.5.dp.toPx(),
                    center = Offset(r, r - faceOffset),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }

            // Offset the icon content to match the 3D displacement
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = if (isPressed) 0.dp else (-0.6).dp),
                contentAlignment = Alignment.Center
            ) {
                if (button.label == "VIEW" || button.label == "CREATE") {
                    Canvas(modifier = Modifier.size(11.dp)) {
                        val w = size.width
                        val h = size.height
                        val strokeW = 1.5.dp.toPx()

                        // Back window
                        drawRect(
                            color = Color.White.copy(alpha = 0.75f),
                            topLeft = Offset(w * 0.25f, 0f),
                            size = Size(w * 0.75f, h * 0.75f),
                            style = Stroke(strokeW)
                        )
                        // Front window
                        drawRect(
                            color = Color.White.copy(alpha = 0.75f),
                            topLeft = Offset(0f, h * 0.25f),
                            size = Size(w * 0.75f, h * 0.75f),
                            style = Stroke(strokeW)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.size(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.6.dp)
                                    .background(Color.White.copy(alpha = 0.75f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun XboxLogoGuideButton(
    button: ButtonDef,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 0.6.dp else 0.dp,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "xboxLogoPress"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "xboxLogoScale"
    )

    // Well size 50.dp, cap size 47.dp (1.5.dp gap)
    Box(
        modifier = Modifier
            .size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Button well
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF1B1B1C))
                .border(0.8.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
        )

        // 2. Button Cap (Flat Disk)
        Box(
            modifier = Modifier
                .size(47.dp)
                .offset(y = pressOffsetY)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(CircleShape)
                .gamepadButtonTouch(
                    onPress = { onPress(button.mappingId) },
                    onRelease = { onRelease(button.mappingId) },
                    onPressedStateChange = { isPressed = it }
                )
        ) {
            val baseColor = Color(0xFF333336)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.width / 2f

                val maxOffset = 0.6.dp.toPx()
                val edgeOffset = if (isPressed) 0.3f.dp.toPx() else 1.8f.dp.toPx()
                val faceOffset = if (isPressed) 0f else maxOffset

                // Calculate radii to fit perfectly within the clip boundaries
                val edgeR = r - edgeOffset / 2f - 0.5.dp.toPx()
                val faceR = r - maxOffset - 0.5.dp.toPx()

                // 3D side edge
                drawCircle(
                    color = Color(0xFF1F1F21),
                    radius = edgeR,
                    center = Offset(r, r + edgeOffset / 2f)
                )

                // Top face
                val faceColor = if (isPressed) Color(0xFF232325) else baseColor
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(faceColor.gpLighter(0.08f), faceColor.gpDarker(0.12f))
                    ),
                    radius = faceR,
                    center = Offset(r, r - faceOffset)
                )

                // Bevel highlight border (vertical gradient brush for better shading)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                        } else {
                            listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                        }
                    ),
                    radius = faceR - 0.5.dp.toPx(),
                    center = Offset(r, r - faceOffset),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
            
            // Developer logo overlay centered on top face
            val faceOffset = if (isPressed) 0.dp else 0.6.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = -faceOffset),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
internal fun PlayStationLogoButton(
    button: ButtonDef,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 0.6.dp else 0.dp,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "psLogoPress"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "psLogoScale"
    )

    // Well size 50.dp, cap size 47.dp (1.5.dp gap)
    Box(
        modifier = Modifier
            .size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        // Button well
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF1B1B1C))
                .border(0.8.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
        )

        // Button Cap (Flat Disk)
        Box(
            modifier = Modifier
                .size(47.dp)
                .offset(y = pressOffsetY)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(CircleShape)
                .gamepadButtonTouch(
                    onPress = { onPress(button.mappingId) },
                    onRelease = { onRelease(button.mappingId) },
                    onPressedStateChange = { isPressed = it }
                )
        ) {
            val baseColor = Color(0xFF333336)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.width / 2f

                val maxOffset = 0.6.dp.toPx()
                val edgeOffset = if (isPressed) 0.3f.dp.toPx() else 1.8f.dp.toPx()
                val faceOffset = if (isPressed) 0f else maxOffset

                // Calculate radii to fit perfectly within the clip boundaries
                val edgeR = r - edgeOffset / 2f - 0.5.dp.toPx()
                val faceR = r - maxOffset - 0.5.dp.toPx()

                // 3D side edge
                drawCircle(
                    color = Color(0xFF1F1F21),
                    radius = edgeR,
                    center = Offset(r, r + edgeOffset / 2f)
                )

                // Top face
                val faceColor = if (isPressed) Color(0xFF232325) else baseColor
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(faceColor.gpLighter(0.08f), faceColor.gpDarker(0.12f))
                    ),
                    radius = faceR,
                    center = Offset(r, r - faceOffset)
                )
                
                // Bevel highlight (vertical gradient brush for better shading)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                        } else {
                            listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                        }
                    ),
                    radius = faceR - 0.5.dp.toPx(),
                    center = Offset(r, r - faceOffset),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
            
            // Developer logo overlay centered on top face
            val faceOffset = if (isPressed) 0.dp else 0.6.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = -faceOffset),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
