package com.haoze.nexus.ui.component.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 分层入场动效状态封装：
 * 包含顶栏、主面板、左右翼、中心控制区、底栏等分层变换参数。
 */
@Stable
class ScreenEntranceState internal constructor(
    val topBarProgress: Float,
    val mainProgress: Float,
    val wingsProgress: Float,
    val centerProgress: Float,
    val bottomProgress: Float
) {
    // 顶栏：自上而下滑入 + 渐现
    val topBarOffsetY: Dp get() = lerp(-20.dp.value, 0f, topBarProgress).dp
    val topBarAlpha: Float get() = topBarProgress.coerceIn(0f, 1f)

    // 主面板/底盘：温和上浮 + 弹性缩放 + 渐现
    val mainScale: Float get() = lerp(0.94f, 1f, mainProgress)
    val mainOffsetY: Dp get() = lerp(16.dp.value, 0f, mainProgress).dp
    val mainAlpha: Float get() = mainProgress.coerceIn(0f, 1f)

    // 左翼控制区（摇杆/十字键）：自左向右推入 + 渐现
    val leftWingOffsetX: Dp get() = lerp(-28.dp.value, 0f, wingsProgress).dp
    val leftWingAlpha: Float get() = wingsProgress.coerceIn(0f, 1f)

    // 右翼控制区（动作键/摇杆）：自右向左推入 + 渐现
    val rightWingOffsetX: Dp get() = lerp(28.dp.value, 0f, wingsProgress).dp
    val rightWingAlpha: Float get() = wingsProgress.coerceIn(0f, 1f)

    // 中央控制区（导引大键/扳机/菜单）：中心绽放 + 渐现
    val centerScale: Float get() = lerp(0.85f, 1f, centerProgress)
    val centerAlpha: Float get() = centerProgress.coerceIn(0f, 1f)

    // 底部工具栏：自下而上滑入 + 渐现
    val bottomBarOffsetY: Dp get() = lerp(20.dp.value, 0f, bottomProgress).dp
    val bottomBarAlpha: Float get() = bottomProgress.coerceIn(0f, 1f)
}

/**
 * 启动并管理全页面分层入场动画编排
 */
@Composable
fun rememberScreenEntranceTransition(): ScreenEntranceState {
    val topAnim = remember { Animatable(0f) }
    val mainAnim = remember { Animatable(0f) }
    val wingsAnim = remember { Animatable(0f) }
    val centerAnim = remember { Animatable(0f) }
    val bottomAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 并行驱动交错动效
        launch {
            topAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 380f
                )
            )
        }
        launch {
            mainAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 320f
                )
            )
        }
        launch {
            delay(30)
            wingsAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 350f
                )
            )
        }
        launch {
            delay(40)
            centerAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.80f,
                    stiffness = 360f
                )
            )
        }
        launch {
            delay(60)
            bottomAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 380f
                )
            )
        }
    }

    return remember(topAnim.value, mainAnim.value, wingsAnim.value, centerAnim.value, bottomAnim.value) {
        ScreenEntranceState(
            topBarProgress = topAnim.value,
            mainProgress = mainAnim.value,
            wingsProgress = wingsAnim.value,
            centerProgress = centerAnim.value,
            bottomProgress = bottomAnim.value
        )
    }
}

/**
 * 顶栏入场修饰符
 */
fun Modifier.screenEntranceTopBar(state: ScreenEntranceState): Modifier = this.graphicsLayer {
    translationY = state.topBarOffsetY.toPx()
    alpha = state.topBarAlpha
}

/**
 * 主面板/键盘定位板入场修饰符
 */
fun Modifier.screenEntranceMain(state: ScreenEntranceState): Modifier = this.graphicsLayer {
    scaleX = state.mainScale
    scaleY = state.mainScale
    translationY = state.mainOffsetY.toPx()
    alpha = state.mainAlpha
}

/**
 * 左翼控制区入场修饰符
 */
fun Modifier.screenEntranceLeftWing(state: ScreenEntranceState): Modifier = this.graphicsLayer {
    translationX = state.leftWingOffsetX.toPx()
    alpha = state.leftWingAlpha
}

/**
 * 右翼控制区入场修饰符
 */
fun Modifier.screenEntranceRightWing(state: ScreenEntranceState): Modifier = this.graphicsLayer {
    translationX = state.rightWingOffsetX.toPx()
    alpha = state.rightWingAlpha
}

/**
 * 中央核心控制区入场修饰符
 */
fun Modifier.screenEntranceCenter(state: ScreenEntranceState): Modifier = this.graphicsLayer {
    scaleX = state.centerScale
    scaleY = state.centerScale
    alpha = state.centerAlpha
}

/**
 * 底部操作栏入场修饰符
 */
fun Modifier.screenEntranceBottomBar(state: ScreenEntranceState): Modifier = this.graphicsLayer {
    translationY = state.bottomBarOffsetY.toPx()
    alpha = state.bottomBarAlpha
}

/**
 * 弹簧物理按压微交互修饰符（卡片点击时平滑缩放反馈）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bouncyCardClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 500f
        ),
        label = "bouncy_card_scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        )
}
