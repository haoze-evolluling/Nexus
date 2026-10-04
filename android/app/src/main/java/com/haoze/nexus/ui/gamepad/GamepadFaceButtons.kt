package com.haoze.nexus.ui.gamepad

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

@Composable
internal fun GamepadFaceButton(
    button: ButtonDef,
    isXboxStyle: Boolean,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    externalIsPressed: Boolean = false
) {
    var internalIsPressed by remember { mutableStateOf(false) }
    val isPressed = internalIsPressed || externalIsPressed

    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 0.6.dp else 0.dp,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "pressOffset"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "pressScale"
    )

    // Base color of the disk itself (dark gray, themed to the background #28282A)
    val baseColor = Color(0xFF333336)
    
    // Label/Symbol color (A, B, X, Y or △, ◯, ✕, ☐)
    val labelColor = button.color

    // Gap clearance reduced to 1.5.dp (well size 48.dp, cap size 45.dp)
    Box(
        modifier = modifier
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Button Well (physical hole in casing with shadow)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF1B1B1C))
                .border(0.8.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
        )

        // 2. Button Cap (presses down into the well)
        Box(
            modifier = Modifier
                .size(45.dp)
                .offset(y = pressOffsetY)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(CircleShape)
                .gamepadButtonTouch(
                    onPress = { onPress(button.mappingId) },
                    onRelease = { onRelease(button.mappingId) },
                    onPressedStateChange = { internalIsPressed = it }
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.width / 2f

                val maxOffset = 0.6.dp.toPx()
                val edgeOffset = if (isPressed) 0.3f.dp.toPx() else 1.8f.dp.toPx()
                val faceOffset = if (isPressed) 0f else maxOffset

                // Calculate radii to fit perfectly within the clip boundaries
                val edgeR = r - edgeOffset / 2f - 0.5.dp.toPx()
                val faceR = r - maxOffset - 0.5.dp.toPx()

                // Draw 3D side edge of the disk (darker than the face)
                drawCircle(
                    color = Color(0xFF1F1F21),
                    radius = edgeR,
                    center = Offset(r, r + edgeOffset / 2f)
                )

                // Top face of the disk (themed to the background, soft vertical gradient)
                val faceColor = if (isPressed) Color(0xFF232325) else baseColor
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(faceColor.gpLighter(0.08f), faceColor.gpDarker(0.12f))
                    ),
                    radius = faceR,
                    center = Offset(r, r - faceOffset)
                )

                // Flat face bevel edge highlight
                drawCircle(
                    color = Color.White.copy(alpha = if (isPressed) 0.05f else 0.15f),
                    radius = faceR - 0.5.dp.toPx(),
                    center = Offset(r, r - faceOffset),
                    style = Stroke(width = 0.8.dp.toPx())
                )

                // Bezel shadow
                drawCircle(
                    color = Color.Black.copy(alpha = if (isPressed) 0.10f else 0.25f),
                    radius = faceR,
                    center = Offset(r, r - faceOffset),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }

            // Offset the content slightly to match the unpressed/pressed 3D displacement
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = if (isPressed) 0.dp else (-0.6).dp),
                contentAlignment = Alignment.Center
            ) {
                if (isXboxStyle) {
                    Text(
                        text = button.label,
                        color = labelColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                } else {
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val strokeW = 2.8.dp.toPx()
                        val radius = 6.5.dp.toPx()
                        
                        when (button.label) {
                            "△" -> {
                                val path = Path().apply {
                                    moveTo(cx, cy - radius)
                                    lineTo(cx + radius, cy + radius * 0.5f)
                                    lineTo(cx - radius, cy + radius * 0.5f)
                                    close()
                                }
                                drawPath(path, labelColor, style = Stroke(width = strokeW))
                            }
                            "◯" -> {
                                drawCircle(labelColor, radius = radius, center = Offset(cx, cy), style = Stroke(width = strokeW))
                            }
                            "✕" -> {
                                drawLine(labelColor, start = Offset(cx - radius, cy - radius), end = Offset(cx + radius, cy + radius), strokeWidth = strokeW)
                                drawLine(labelColor, start = Offset(cx + radius, cy - radius), end = Offset(cx - radius, cy + radius), strokeWidth = strokeW)
                            }
                            "☐" -> {
                                val side = radius * 1.5f
                                drawRect(
                                    labelColor,
                                    topLeft = Offset(cx - side/2f, cy - side/2f),
                                    size = Size(side, side),
                                    style = Stroke(width = strokeW)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FaceButtonsDiamond(
    config: ConsoleConfig,
    @Suppress("UNUSED_PARAMETER") isXboxStyle: Boolean,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = 40.dp
    val density = LocalDensity.current.density

    // Track active pressed button mapping IDs via proximity multi-touch
    val activePressedButtons = remember { mutableStateListOf<Int>() }

    val updateProximityPresses = { pointerPositions: List<Offset>, containerSizePx: Float ->
        val centerPx = containerSizePx / 2f
        val spacingPx = 40f * density
        val touchRadiusPx = 35f * density // Proximity radius covering multi-button thumb presses

        val topCenter = Offset(centerPx, centerPx - spacingPx)
        val rightCenter = Offset(centerPx + spacingPx, centerPx)
        val bottomCenter = Offset(centerPx, centerPx + spacingPx)
        val leftCenter = Offset(centerPx - spacingPx, centerPx)

        val buttonCenters = listOf(
            config.faceTop.mappingId to topCenter,
            config.faceRight.mappingId to rightCenter,
            config.faceBottom.mappingId to bottomCenter,
            config.faceLeft.mappingId to leftCenter
        )

        val newlyActive = mutableSetOf<Int>()
        for (pos in pointerPositions) {
            for ((mappingId, btnCenter) in buttonCenters) {
                val dx = pos.x - btnCenter.x
                val dy = pos.y - btnCenter.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= touchRadiusPx) {
                    newlyActive.add(mappingId)
                }
            }
        }

        // Press newly touched buttons
        for (mappingId in newlyActive) {
            if (!activePressedButtons.contains(mappingId)) {
                activePressedButtons.add(mappingId)
                onPress(mappingId)
            }
        }

        // Release buttons no longer under any active finger
        val toRemove = mutableListOf<Int>()
        for (mappingId in activePressedButtons) {
            if (!newlyActive.contains(mappingId)) {
                toRemove.add(mappingId)
                onRelease(mappingId)
            }
        }
        activePressedButtons.removeAll(toRemove.toSet())
    }

    Box(
        modifier = modifier
            .size(134.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val activePositions = mutableMapOf<PointerId, Offset>()
                    activePositions[down.id] = down.position
                    updateProximityPresses(activePositions.values.toList(), size.width.toFloat())

                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            if (change.pressed) {
                                activePositions[change.id] = change.position
                            } else {
                                activePositions.remove(change.id)
                            }
                        }
                        if (activePositions.isEmpty()) {
                            for (id in activePressedButtons.toList()) {
                                onRelease(id)
                            }
                            activePressedButtons.clear()
                            break
                        }
                        updateProximityPresses(activePositions.values.toList(), size.width.toFloat())
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        GamepadFaceButton(config.faceTop, isXboxStyle, onPress, onRelease, Modifier.offset(y = -spacing), externalIsPressed = activePressedButtons.contains(config.faceTop.mappingId))
        GamepadFaceButton(config.faceRight, isXboxStyle, onPress, onRelease, Modifier.offset(x = spacing), externalIsPressed = activePressedButtons.contains(config.faceRight.mappingId))
        GamepadFaceButton(config.faceBottom, isXboxStyle, onPress, onRelease, Modifier.offset(y = spacing), externalIsPressed = activePressedButtons.contains(config.faceBottom.mappingId))
        GamepadFaceButton(config.faceLeft, isXboxStyle, onPress, onRelease, Modifier.offset(x = -spacing), externalIsPressed = activePressedButtons.contains(config.faceLeft.mappingId))
    }
}
