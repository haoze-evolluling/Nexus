package com.haoze.nexus.ui.gamepad

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
internal fun GamepadDpad(
    @Suppress("UNUSED_PARAMETER") isXboxStyle: Boolean,
    onDpadChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    var activeDirection by remember { mutableIntStateOf(0) }
    
    // Physical tilt based on pressed direction (pure pivot rotation)
    var targetRotX = 0f
    var targetRotY = 0f
    
    if (activeDirection and 1 != 0) targetRotX = 12f  // UP: depresses top, raises bottom
    if (activeDirection and 2 != 0) targetRotX = -12f // DOWN: depresses bottom, raises top
    if (activeDirection and 4 != 0) targetRotY = -12f // LEFT: depresses left, raises right
    if (activeDirection and 8 != 0) targetRotY = 12f  // RIGHT: depresses right, raises left
    
    val rotX by animateFloatAsState(targetValue = targetRotX, animationSpec = spring(stiffness = Spring.StiffnessHigh))
    val rotY by animateFloatAsState(targetValue = targetRotY, animationSpec = spring(stiffness = Spring.StiffnessHigh))

    Box(
        modifier = modifier
            .size(114.dp) // Large D-pad size
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var currentBit = determineDpadBit(down.position.x, down.position.y, size.width.toFloat())
                    activeDirection = currentBit
                    onDpadChange(currentBit)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            activeDirection = 0
                            onDpadChange(0)
                            break
                        }
                        change.consume()
                        val newBit = determineDpadBit(change.position.x, change.position.y, size.width.toFloat())
                        if (newBit != currentBit) {
                            currentBit = newBit
                            activeDirection = currentBit
                            onDpadChange(currentBit)
                        }
                    }
                }
            }
    ) {
        // 1. Stationary Background Well (Casing hole remains static)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val barWWell = w * 0.36f
            val startOffWell = (w - barWWell) / 2f
            
            val path1 = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = w * 0.02f,
                        top = startOffWell,
                        right = w * 0.98f,
                        bottom = startOffWell + barWWell,
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                )
            }
            val path2 = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = startOffWell,
                        top = w * 0.02f,
                        right = startOffWell + barWWell,
                        bottom = w * 0.98f,
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                )
            }
            val wellPath = Path.combine(
                PathOperation.Union,
                path1,
                path2
            )
            
            // Fill plus-shaped well
            drawPath(
                path = wellPath,
                color = Color(0xFF1B1B1C)
            )
            // Well border
            drawPath(
                path = wellPath,
                color = Color.Black.copy(alpha = 0.5f),
                style = Stroke(width = 1.2.dp.toPx())
            )
            // Inner shadow for depth
            drawPath(
                path = wellPath,
                color = Color.Black.copy(alpha = 0.2f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // 2. Stationary Drop Shadow Layer (does NOT rotate in 3D, shifts dynamically opposite to tilt)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val barW = w * 0.28f
            val startOff = (w - barW) / 2f
            
            val cross1 = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = w * 0.06f,
                        top = startOff,
                        right = w * 0.94f,
                        bottom = startOff + barW,
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                )
            }
            val cross2 = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = startOff,
                        top = w * 0.06f,
                        right = startOff + barW,
                        bottom = w * 0.94f,
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                )
            }
            val crossPath = Path.combine(
                PathOperation.Union,
                cross1,
                cross2
            )
            
            // Dynamic D-pad drop shadow offset (shifts in opposite direction of tilt)
            var shadowOffsetX = 0f
            var shadowOffsetY = 1.5f.dp.toPx()
            
            if (activeDirection and 1 != 0) shadowOffsetY += 2f.dp.toPx()  // UP: shifts shadow down
            if (activeDirection and 2 != 0) shadowOffsetY -= 2f.dp.toPx()  // DOWN: shifts shadow up
            if (activeDirection and 4 != 0) shadowOffsetX += 2f.dp.toPx()  // LEFT: shifts shadow right
            if (activeDirection and 8 != 0) shadowOffsetX -= 2f.dp.toPx()  // RIGHT: shifts shadow left
            
            withTransform({
                translate(left = shadowOffsetX, top = shadowOffsetY)
            }) {
                drawPath(
                    path = crossPath,
                    color = Color.Black.copy(alpha = 0.4f)
                )
            }
        }

        // 3. Animated D-pad Cross Body (Applying rotation to inner cross only)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = rotX
                    rotationY = rotY
                    cameraDistance = 8f * density
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val cx = w / 2f
                val cy = w / 2f
                
                val barW = w * 0.28f
                val startOff = (w - barW) / 2f
                
                val cross1 = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = w * 0.06f,
                            top = startOff,
                            right = w * 0.94f,
                            bottom = startOff + barW,
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    )
                }
                val cross2 = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = startOff,
                            top = w * 0.06f,
                            right = startOff + barW,
                            bottom = w * 0.94f,
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    )
                }
                val crossPath = Path.combine(
                    PathOperation.Union,
                    cross1,
                    cross2
                )

                // Draw cross body (matte vertical gradient remains static/unpressed color on tap)
                val bodyBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF38383B), Color(0xFF2D2D30))
                )
                drawPath(
                    path = crossPath,
                    brush = bodyBrush
                )

                // Beveled highlight stroke remains static/unpressed on tap
                val highlightBrush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                )
                drawPath(
                    path = crossPath,
                    brush = highlightBrush,
                    style = Stroke(width = 0.8.dp.toPx())
                )
                // Outer dark border stroke remains static on tap
                drawPath(
                    path = crossPath,
                    color = Color.Black.copy(alpha = 0.35f),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Central depressed dish (concave look)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1B1B1D), Color(0xFF2C2C30)),
                        center = Offset(cx, cy),
                        radius = barW / 1.5f
                    ),
                    radius = barW / 1.8f
                )
                // Soft outline for dish
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = barW / 1.8f,
                    style = Stroke(width = 0.8.dp.toPx())
                )

                // Elegant, thin directional markers/arrows (static subtle white/grey)
                val arrowColor = { directionBit: Int ->
                    if (activeDirection and directionBit != 0) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f)
                }
                val arrowDist = w * 0.34f
                val arrowSize = w * 0.04f

                // Up Arrow
                val pathU = Path().apply {
                    moveTo(cx, cy - arrowDist)
                    lineTo(cx - arrowSize, cy - arrowDist + arrowSize * 1.1f)
                    lineTo(cx + arrowSize, cy - arrowDist + arrowSize * 1.1f)
                    close()
                }
                drawPath(pathU, arrowColor(1))

                // Down Arrow
                val pathD = Path().apply {
                    moveTo(cx, cy + arrowDist)
                    lineTo(cx - arrowSize, cy + arrowDist - arrowSize * 1.1f)
                    lineTo(cx + arrowSize, cy + arrowDist - arrowSize * 1.1f)
                    close()
                }
                drawPath(pathD, arrowColor(2))

                // Left Arrow
                val pathL = Path().apply {
                    moveTo(cx - arrowDist, cy)
                    lineTo(cx - arrowDist + arrowSize * 1.1f, cy - arrowSize)
                    lineTo(cx - arrowDist + arrowSize * 1.1f, cy + arrowSize)
                    close()
                }
                drawPath(pathL, arrowColor(4))

                // Right Arrow
                val pathR = Path().apply {
                    moveTo(cx + arrowDist, cy)
                    lineTo(cx + arrowDist - arrowSize * 1.1f, cy - arrowSize)
                    lineTo(cx + arrowDist - arrowSize * 1.1f, cy + arrowSize)
                    close()
                }
                drawPath(pathR, arrowColor(8))
            }
        }
    }
}

internal fun determineDpadBit(x: Float, y: Float, totalSize: Float): Int {
    val center = totalSize / 2f
    val dx = x - center
    val dy = y - center
    val distance = sqrt(dx * dx + dy * dy)
    if (distance < totalSize * 0.08f) return 0

    var mask = 0
    // Threshold distance for diagonal combined directions
    val sectorThreshold = distance * 0.38f

    if (dy < -sectorThreshold) mask = mask or 1 // UP
    if (dy > sectorThreshold) mask = mask or 2  // DOWN
    if (dx < -sectorThreshold) mask = mask or 4 // LEFT
    if (dx > sectorThreshold) mask = mask or 8  // RIGHT

    if (mask == 0) {
        mask = if (abs(dx) > abs(dy)) {
            if (dx > 0) 8 else 4
        } else {
            if (dy > 0) 2 else 1
        }
    }
    return mask
}
