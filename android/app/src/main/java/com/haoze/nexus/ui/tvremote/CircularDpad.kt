package com.haoze.nexus.ui.tvremote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haoze.nexus.R
import com.haoze.nexus.util.performKeyClick
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

enum class DpadDirection { UP, DOWN, LEFT, RIGHT }

@Composable
fun CircularDpad(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outerRadius: Dp = 106.dp,
    innerRadius: Dp = 51.dp,
    repeatDelay: Long = 200L,
    ringColor: Color,
    ringBorderColor: Color,
    centerColor: Color,
    centerBorderColor: Color,
    dividerColor: Color,
    highlightColor: Color,
    iconColor: Color,
    textColor: Color,
    onDirection: (DpadDirection) -> Unit,
    onConfirm: () -> Unit
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val centerText = stringResource(R.string.tvremote_ok)

    val iconUp = painterResource(R.drawable.ic_nav_up)
    val iconDown = painterResource(R.drawable.ic_nav_down)
    val iconLeft = painterResource(R.drawable.ic_nav_left)
    val iconRight = painterResource(R.drawable.ic_nav_right)

    var pressedDirection by remember { mutableStateOf<DpadDirection?>(null) }
    var isCenterPressed by remember { mutableStateOf(false) }

    val currentOnDirection by rememberUpdatedState(onDirection)
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 1.dp.toPx() }
    val iconSizePx = with(density) { 24.dp.toPx() }
    val iconSize = Size(iconSizePx, iconSizePx)

    val textStyle = remember(textColor) {
        TextStyle(
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }

    Box(
        modifier = modifier
            .size(outerRadius * 2)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val size = this.size
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val innerRadiusPx = innerRadius.toPx()
                    val outerRadiusPx = outerRadius.toPx()

                    val dx = down.position.x - centerX
                    val dy = down.position.y - centerY
                    val dist = sqrt(dx * dx + dy * dy)

                    var repeatJob: Job? = null

                    if (dist < innerRadiusPx) {
                        isCenterPressed = true
                        view.performKeyClick()
                        currentOnConfirm()
                    } else if (dist <= outerRadiusPx) {
                        val dir = calculateDirection(dx, dy)
                        pressedDirection = dir
                        view.performKeyClick()
                        currentOnDirection(dir)

                        repeatJob = coroutineScope.launch {
                            delay(500L)
                            while (isActive) {
                                currentOnDirection(dir)
                                delay(repeatDelay)
                            }
                        }
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id }
                            if (pointer == null || !pointer.pressed) {
                                break
                            }
                            val moveDx = pointer.position.x - centerX
                            val moveDy = pointer.position.y - centerY
                            val moveDist = sqrt(moveDx * moveDx + moveDy * moveDy)

                            if (moveDist > outerRadiusPx) {
                                repeatJob?.cancel()
                                repeatJob = null
                                pressedDirection = null
                                isCenterPressed = false
                            }
                        }
                    } finally {
                        repeatJob?.cancel()
                        pressedDirection = null
                        isCenterPressed = false
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            val strokeInset = strokeWidthPx / 2f
            val actualOuterRadius = min(w, h) / 2f - strokeInset
            val innerRadiusPx = innerRadius.toPx()

            val ringRect = Rect(
                centerX - actualOuterRadius,
                centerY - actualOuterRadius,
                centerX + actualOuterRadius,
                centerY + actualOuterRadius
            )
            val centerRect = Rect(
                centerX - innerRadiusPx,
                centerY - innerRadiusPx,
                centerX + innerRadiusPx,
                centerY + innerRadiusPx
            )

            // 1. 外环背景与边框
            drawCircle(
                color = ringColor,
                radius = actualOuterRadius,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = ringBorderColor,
                radius = actualOuterRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeWidthPx)
            )

            // 2. 按压象限高亮
            pressedDirection?.let { dir ->
                val startAngle = when (dir) {
                    DpadDirection.RIGHT -> 315f
                    DpadDirection.DOWN -> 45f
                    DpadDirection.LEFT -> 135f
                    DpadDirection.UP -> 225f
                }
                val path = Path().apply {
                    arcTo(rect = ringRect, startAngleDegrees = startAngle, sweepAngleDegrees = 90f, forceMoveTo = false)
                    arcTo(rect = centerRect, startAngleDegrees = startAngle + 90f, sweepAngleDegrees = -90f, forceMoveTo = false)
                    close()
                }
                drawPath(path, color = highlightColor)
            }

            // 3. 四象限分割线
            rotate(degrees = 45f, pivot = Offset(centerX, centerY)) {
                drawLine(
                    color = dividerColor,
                    start = Offset(centerX, centerY - actualOuterRadius),
                    end = Offset(centerX, centerY + actualOuterRadius),
                    strokeWidth = strokeWidthPx
                )
            }
            rotate(degrees = 135f, pivot = Offset(centerX, centerY)) {
                drawLine(
                    color = dividerColor,
                    start = Offset(centerX, centerY - actualOuterRadius),
                    end = Offset(centerX, centerY + actualOuterRadius),
                    strokeWidth = strokeWidthPx
                )
            }

            // 4. 中心圆背景、高亮及边框
            drawCircle(
                color = centerColor,
                radius = innerRadiusPx,
                center = Offset(centerX, centerY)
            )
            if (isCenterPressed) {
                drawCircle(
                    color = highlightColor,
                    radius = innerRadiusPx,
                    center = Offset(centerX, centerY)
                )
            }
            drawCircle(
                color = centerBorderColor,
                radius = innerRadiusPx,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeWidthPx)
            )

            // 5. 中心 "OK" 文字
            val textLayoutResult = textMeasurer.measure(
                text = centerText,
                style = textStyle
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    centerX - textLayoutResult.size.width / 2f,
                    centerY - textLayoutResult.size.height / 2f
                )
            )

            // 6. 方向图标绘制
            val midRadius = (actualOuterRadius + innerRadiusPx) / 2f
            val halfIcon = iconSizePx / 2f
            val colorFilter = ColorFilter.tint(iconColor)

            // UP (0, -midRadius)
            translate(centerX - halfIcon, centerY - midRadius - halfIcon) {
                with(iconUp) {
                    draw(size = iconSize, colorFilter = colorFilter)
                }
            }
            // DOWN (0, +midRadius)
            translate(centerX - halfIcon, centerY + midRadius - halfIcon) {
                with(iconDown) {
                    draw(size = iconSize, colorFilter = colorFilter)
                }
            }
            // LEFT (-midRadius, 0)
            translate(centerX - midRadius - halfIcon, centerY - halfIcon) {
                with(iconLeft) {
                    draw(size = iconSize, colorFilter = colorFilter)
                }
            }
            // RIGHT (+midRadius, 0)
            translate(centerX + midRadius - halfIcon, centerY - halfIcon) {
                with(iconRight) {
                    draw(size = iconSize, colorFilter = colorFilter)
                }
            }
        }
    }
}

private fun calculateDirection(x: Float, y: Float): DpadDirection {
    var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble()))
    if (angle < 0) angle += 360.0

    return when {
        angle > 315 || angle <= 45 -> DpadDirection.RIGHT
        angle > 45 && angle <= 135 -> DpadDirection.DOWN
        angle > 135 && angle <= 225 -> DpadDirection.LEFT
        else -> DpadDirection.UP
    }
}
