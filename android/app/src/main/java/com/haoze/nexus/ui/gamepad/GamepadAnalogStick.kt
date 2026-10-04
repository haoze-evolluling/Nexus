package com.haoze.nexus.ui.gamepad

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
internal fun GamepadStickHoldButton(
    label: String,
    isHeld: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pressOffsetY by animateDpAsState(
        targetValue = if (isHeld) 0.6.dp else 0.dp,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "stickHoldPress"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isHeld) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "stickHoldScale"
    )

    // Well size 30.dp, Cap size 26.dp (Hole / Inset shadow effect)
    Box(
        modifier = modifier
            .size(30.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Dark Button Well (Hole Inset)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF171718))
                .border(0.8.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
        )

        // 2. 3D Circular Button Cap
        Box(
            modifier = Modifier
                .size(26.dp)
                .offset(y = pressOffsetY)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(CircleShape)
                .clickable { onToggle(!isHeld) },
            contentAlignment = Alignment.Center
        ) {
            val baseColor = if (isHeld) Color(0xFF3A3A3E) else Color(0xFF28282B)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.width / 2f

                val maxOffset = 0.6.dp.toPx()
                val edgeOffset = if (isHeld) 0.2.dp.toPx() else 1.5.dp.toPx()
                val faceOffset = if (isHeld) 0f else maxOffset

                val edgeR = r - edgeOffset / 2f - 0.5.dp.toPx()
                val faceR = r - maxOffset - 0.5.dp.toPx()

                // 3D Side shadow edge
                drawCircle(
                    color = Color(0xFF161618),
                    radius = edgeR,
                    center = Offset(r, r + edgeOffset / 2f)
                )

                // Top face gradient
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(baseColor.gpLighter(0.10f), baseColor.gpDarker(0.12f))
                    ),
                    radius = faceR,
                    center = Offset(r, r - faceOffset)
                )

                // Top bevel highlight / active border
                drawCircle(
                    color = if (isHeld) Color(0xFFE5E5EA) else Color.White.copy(alpha = 0.15f),
                    radius = faceR - 0.5.dp.toPx(),
                    center = Offset(r, r - faceOffset),
                    style = Stroke(width = if (isHeld) 1.dp.toPx() else 0.8.dp.toPx())
                )
            }

            // Centered Text "L3" / "R3"
            Text(
                text = label,
                color = if (isHeld) Color(0xFFFFFFFF) else Color.White.copy(alpha = 0.70f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun GamepadAnalogStick(
    label: String,
    isClicked: Boolean,
    modifier: Modifier = Modifier,
    isHeld: Boolean = false,
    onMove: (Float, Float) -> Unit,
    onStickClick: () -> Unit,
    onToggleHold: ((Boolean) -> Unit)? = null
) {
    var stickOffsetX by remember { mutableFloatStateOf(0f) }
    var stickOffsetY by remember { mutableFloatStateOf(0f) }
    var isTouchActive by remember { mutableStateOf(false) }

    val currentIsHeld by rememberUpdatedState(isHeld)
    val currentOnStickClick by rememberUpdatedState(onStickClick)
    val currentOnMove by rememberUpdatedState(onMove)

    val stickScale by animateFloatAsState(
        targetValue = if (isClicked || isHeld) 0.90f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "stickScale"
    )

    Box(
        modifier = modifier.size(108.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .pointerInput(Unit) {
                    val centerPx = size.width / 2f
                    val maxInputRadius = 32.dp.toPx()  // Wide linear touch response radius
                    val maxVisualRadius = 22.dp.toPx() // Clean visual cap travel boundary
                    val tapSlopPx = 8f

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isTouchActive = true
                        val pointerId = down.id
                        val downTime = System.currentTimeMillis()

                        val updateStickOffset = { pos: Offset ->
                            val dx = pos.x - centerPx
                            val dy = pos.y - centerPx
                            val dist = sqrt(dx * dx + dy * dy)

                            if (dist == 0f) {
                                stickOffsetX = 0f
                                stickOffsetY = 0f
                                currentOnMove(0f, 0f)
                            } else {
                                val normDist = (dist / maxInputRadius).coerceIn(0f, 1f)
                                val normalizedX = (dx / dist) * normDist
                                val normalizedY = (dy / dist) * normDist

                                val visualDist = dist.coerceAtMost(maxVisualRadius)
                                stickOffsetX = (dx / dist) * visualDist
                                stickOffsetY = (dy / dist) * visualDist

                                currentOnMove(normalizedX, normalizedY)
                            }
                        }

                        updateStickOffset(down.position)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                            if (!change.pressed) {
                                isTouchActive = false
                                val dragDistance = sqrt((change.position.x - down.position.x) * (change.position.x - down.position.x) + 
                                                        (change.position.y - down.position.y) * (change.position.y - down.position.y))

                                if (dragDistance < tapSlopPx && System.currentTimeMillis() - downTime < 250) {
                                    if (!currentIsHeld) {
                                        currentOnStickClick()
                                    }
                                }
                                stickOffsetX = 0f
                                stickOffsetY = 0f
                                currentOnMove(0f, 0f)
                                break
                            }

                            change.consume()
                            updateStickOffset(change.position)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Analog Stick Base Well (neumorphic molding & concentric rings)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width

                // Neumorphic outer lip molding (top-left highlight, bottom-right shadow)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF3B3B3E), Color(0xFF1B1B1C)),
                        start = Offset(0f, 0f),
                        end = Offset(w, w)
                    ),
                    radius = w / 2f + 3.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFF121213),
                    radius = w / 2f + 3.dp.toPx(),
                    style = Stroke(width = 0.8.dp.toPx())
                )
                
                // Recessed well background
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF141416), Color(0xFF222224))
                    ),
                    radius = w / 2f - 1.dp.toPx()
                )
                
                // Soft inner shadow ring inside the well for depth
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = w / 2f - 2.dp.toPx(),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 2. Parallax 3D Shadow Layer (floats under the cap, shifts slightly less)
            Canvas(
                modifier = Modifier
                    .offset { IntOffset((stickOffsetX * 0.6f).roundToInt(), (stickOffsetY * 0.6f).roundToInt()) }
                    .size(82.dp)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width / 2f
                    ),
                    radius = size.width / 2f
                )
            }
                
            // 3. Analog Stick Thumb (rendered on top, NOT clipped, with 3D tilt & press)
            Box(
                modifier = Modifier
                    .offset { IntOffset(stickOffsetX.roundToInt(), stickOffsetY.roundToInt()) }
                    .size(76.dp) // Large thumb cap (narrow gap to well)
                    .clip(CircleShape)
                    .graphicsLayer {
                        // Quick press/sink scaling when clicked (no shape distortion when moving)
                        scaleX = stickScale
                        scaleY = stickScale
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val cx = w / 2f
                    val cy = w / 2f
                    val r = w / 2f
                    
                    // Outer base edge (very dark ring)
                    drawCircle(
                        color = Color(0xFF161719),
                        radius = r
                    )
                    
                    // Outer ring highlight/shadow
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = r - 0.5.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    // Shifting central elements for 3D parallax deflection and press look
                    val pressShift = if (isClicked) 1.2.dp.toPx() else 0f
                    val cupCx = cx + stickOffsetX * 0.12f
                    val cupCy = cy + stickOffsetY * 0.12f + pressShift
                    
                    // Create paths for the knurled dome slope (between outer ring and inner cup)
                    val pathOuter = Path().apply {
                        addOval(Rect(center = Offset(cx, cy), radius = r - 1.dp.toPx()))
                    }
                    val pathInner = Path().apply {
                        addOval(Rect(center = Offset(cupCx, cupCy), radius = r * 0.58f))
                    }
                    val ringPath = Path.combine(
                        PathOperation.Difference,
                        pathOuter,
                        pathInner
                    )
                    
                    // Fill the dome slope with a soft vertical gradient for a flatter look
                    drawPath(
                        path = ringPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF38383B), Color(0xFF232325))
                        )
                    )
                    
                    // Flatter recessed cup gradient (no radial concentration in center)
                    val cupRadius = r * 0.58f
                    val concaveBrush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B1C1E), Color(0xFF292A2E))
                    )
                    
                    // 3D Inner cup recessed background
                    drawCircle(
                        brush = concaveBrush,
                        radius = cupRadius,
                        center = Offset(cupCx, cupCy)
                    )
                    
                    // Bevel ring border (simple dark separator)
                    drawCircle(
                        color = Color(0xFF141517),
                        radius = cupRadius,
                        center = Offset(cupCx, cupCy),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    
                    // Bevel highlight (simple soft outline ring)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.09f),
                        radius = cupRadius - 0.6.dp.toPx(),
                        center = Offset(cupCx, cupCy),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }
            }
        }

        // 2. L3 / R3 Hold Toggle positioned at TOP-LEFT of joystick
        if (onToggleHold != null) {
            GamepadStickHoldButton(
                label = if (label == "L") "L3" else "R3",
                isHeld = isHeld,
                onToggle = onToggleHold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-10).dp, y = (-10).dp)
            )
        }
    }
}
