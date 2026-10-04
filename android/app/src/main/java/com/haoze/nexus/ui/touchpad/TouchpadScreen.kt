package com.haoze.nexus.ui.touchpad

import android.content.Context
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haoze.nexus.R
import com.haoze.nexus.bluetooth.MouseReport
import com.haoze.nexus.bluetooth.MouseSender
import com.haoze.nexus.ui.component.animation.rememberScreenEntranceTransition
import com.haoze.nexus.ui.component.animation.screenEntranceBottomBar
import com.haoze.nexus.ui.component.animation.screenEntranceCenter
import com.haoze.nexus.ui.component.animation.screenEntranceMain
import com.haoze.nexus.util.performHapticLongPress
import com.haoze.nexus.util.performKeyClick
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ---- 手势参数（与原 TouchpadFragment 保持一致） ----

private const val PREFS_NAME = "settings_prefs"
private const val KEY_SENSITIVITY = "touchpad_sensitivity"
private const val KEY_CURSOR_SPEED = "cursor_speed"
private const val KEY_SCROLL_DIRECTION_NATURAL = "scroll_direction_natural"

/** 鼠标报文发送节流间隔，约 120Hz */
private const val THROTTLE_INTERVAL_MS = 8L

/** 单指/三指轻点判定的位移与时长上限 */
private const val TAP_MAX_DISTANCE = 20f
private const val TAP_MAX_DURATION_MS = 200L

/** 双指轻点判定的时长与位移上限 */
private const val TWO_FINGER_TAP_TIMEOUT_MS = 300L
private const val TWO_FINGER_TAP_MAX_DISTANCE = 50f

/** 长按 300ms 进入拖拽；长按触发前位移超过 30px 则取消长按 */
private const val LONG_PRESS_DELAY_MS = 300L
private const val DRAG_MAX_DISTANCE = 30f

/** 滚动结束后忽略单指事件的冷却时间 */
private const val SCROLL_COOLDOWN_MS = 100L

/**
 * 触控板页面：单指移动/轻点、双指滚动/轻点、三指轻点与长按拖拽，
 * 手势行为与原 TouchpadFragment 完全一致。未连接时整体半透明并禁用交互。
 */
@Composable
fun TouchpadScreen(
    isConnected: Boolean,
    onExit: () -> Unit,
    onOpenKeyboard: () -> Unit,
    getMouseSender: () -> MouseSender?
) {
    val context = LocalContext.current
    val hapticView = LocalView.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val entranceState = rememberScreenEntranceTransition()

    // 灵敏度 1-10 可实时调节；光标速度与自然滚动方向仅在启动时读取
    var sensitivity by remember {
        mutableIntStateOf(prefs.getInt(KEY_SENSITIVITY, 5).coerceIn(1, 10))
    }
    val cursorSpeed = remember { prefs.getInt(KEY_CURSOR_SPEED, 5) }
    val naturalScrolling = remember { prefs.getBoolean(KEY_SCROLL_DIRECTION_NATURAL, false) }

    val colorScheme = MaterialTheme.colorScheme
    val panelShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .safeDrawingPadding()
    ) {
        // 顶部对齐、高 360dp 的横向整条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .padding(start = 6.dp, top = 6.dp, end = 12.dp, bottom = 36.dp)
        ) {
            // 触控区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .screenEntranceMain(entranceState)
                    .padding(start = 2.dp, end = 14.dp, bottom = 60.dp)
                    .clip(panelShape)
                    .background(
                        if (isConnected) colorScheme.surfaceContainerLow
                        else colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                    )
                    .border(
                        1.dp,
                        if (isConnected) colorScheme.outlineVariant else colorScheme.outlineVariant.copy(alpha = 0.4f),
                        panelShape
                    )
                    .touchpadGestures(
                        enabled = isConnected,
                        sensitivity = { sensitivity },
                        cursorSpeed = cursorSpeed,
                        naturalScrolling = naturalScrolling,
                        getMouseSender = getMouseSender,
                        hapticView = hapticView
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!isConnected) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .screenEntranceCenter(entranceState)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(R.string.touchpad_not_connected_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // 底部一栏
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .screenEntranceBottomBar(entranceState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TouchpadPanelButton(
                    text = stringResource(R.string.tab_claude),
                    enabled = true,
                    hapticView = hapticView,
                    onClick = onExit
                )
                TouchpadPanelButton(
                    text = stringResource(R.string.tab_keyboard),
                    enabled = true,
                    hapticView = hapticView,
                    onClick = onOpenKeyboard
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.touchpad_sensitivity),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = sensitivity.toFloat(),
                    onValueChange = { value ->
                        sensitivity = value.roundToInt().coerceIn(1, 10)
                        prefs.edit().putInt(KEY_SENSITIVITY, sensitivity).apply()
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    }
}

/**
 * 触控板手势处理，逐条对应原 TouchpadFragment.handleTouchEvent：
 * 单指移动/轻点（左键）、双指滚动/轻点（右键）、三指轻点（中键）、
 * 长按 300ms 进入拖拽（按住左键移动，抬手归零报文释放按键）。
 */
private fun Modifier.touchpadGestures(
    enabled: Boolean,
    sensitivity: () -> Int,
    cursorSpeed: Int,
    naturalScrolling: Boolean,
    getMouseSender: () -> MouseSender?,
    hapticView: View
): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput

    // 手势状态（对应原 Fragment 的字段）
    var lastX = 0f
    var lastY = 0f
    var startX = 0f
    var startY = 0f
    var startTime = 0L
    var isScrollMode = false
    var scrollEndTime = 0L
    var isDragging = false
    var lastTwoFingerX = 0f
    var lastTwoFingerY = 0f
    var isTwoFingerGesture = false
    var twoFingerStartTime = 0L
    var twoFingerStartX = floatArrayOf(0f, 0f)
    var twoFingerStartY = floatArrayOf(0f, 0f)
    var isThreeFingerGesture = false
    var threeFingerStartTime = 0L
    var threeFingerStartX = 0f
    var threeFingerStartY = 0f
    var lastSendTime = 0L
    var longPressPending = false
    var longPressDeadline = 0L

    fun cancelLongPress() {
        longPressPending = false
    }

    fun releaseDrag() {
        isDragging = false
        getMouseSender()?.let { sender ->
            sender.sendMouseMove(0, 0) // 归零报文释放所有按键
        }
    }

    fun fireLongPress() {
        cancelLongPress()
        if (!isScrollMode) {
            isDragging = true
            hapticView.performHapticLongPress()
            getMouseSender()?.let { sender ->
                sender.sendMouseMoveWithButtons(0, 0, leftButton = true)
            }
        }
    }

    // 对应 MotionEvent.ACTION_DOWN
    fun handleDown(change: PointerInputChange) {
        val now = System.currentTimeMillis()
        // 滚动冷却期内忽略本次触摸：不初始化任何状态（与原实现一致）
        if (now - scrollEndTime < SCROLL_COOLDOWN_MS) return
        lastX = change.position.x
        lastY = change.position.y
        startX = change.position.x
        startY = change.position.y
        startTime = now
        isScrollMode = false
        isDragging = false
        // 启动长按计时，300ms 后进入拖拽
        longPressPending = true
        longPressDeadline = now + LONG_PRESS_DELAY_MS
    }

    // 对应 MotionEvent.ACTION_POINTER_DOWN
    fun handlePointerDown(changes: List<PointerInputChange>) {
        when (changes.size) {
            2 -> {
                // 取消长按/拖拽，进入双指滚动
                cancelLongPress()
                if (isDragging) releaseDrag()
                isScrollMode = true
                hapticView.performKeyClick()
                val a = changes[0]
                val b = changes[1]
                lastTwoFingerX = (a.position.x + b.position.x) / 2f
                lastTwoFingerY = (a.position.y + b.position.y) / 2f
                // 记录双指手势起点（用于双指轻点判定）
                isTwoFingerGesture = true
                twoFingerStartTime = SystemClock.uptimeMillis()
                twoFingerStartX[0] = a.position.x
                twoFingerStartY[0] = a.position.y
                twoFingerStartX[1] = b.position.x
                twoFingerStartY[1] = b.position.y
            }
            3 -> {
                // 取消滚动，开始三指轻点跟踪
                cancelLongPress()
                if (isDragging) releaseDrag()
                isScrollMode = false
                isThreeFingerGesture = true
                threeFingerStartTime = SystemClock.uptimeMillis()
                threeFingerStartX = changes[0].position.x
                threeFingerStartY = changes[0].position.y
            }
            // 其余 pointerCount：原实现不做处理
        }
    }

    // 对应 MotionEvent.ACTION_MOVE
    fun handleMove(changes: List<PointerInputChange>) {
        val pointerCount = changes.size
        val now = System.currentTimeMillis()

        // 长按触发前手指移动过远则取消长按
        if (!isDragging && !isScrollMode && pointerCount == 1) {
            val dx = changes[0].position.x - startX
            val dy = changes[0].position.y - startY
            if (sqrt(dx * dx + dy * dy) > DRAG_MAX_DISTANCE) cancelLongPress()
        }

        // 发送节流
        if (now - lastSendTime < THROTTLE_INTERVAL_MS) return

        if (isScrollMode && pointerCount >= 2) {
            // 双指滚动：取前两指中点位移
            val currentX = (changes[0].position.x + changes[1].position.x) / 2f
            val currentY = (changes[0].position.y + changes[1].position.y) / 2f
            val dx = currentX - lastTwoFingerX
            val dy = currentY - lastTwoFingerY

            // 垂直滚动（自然滚动：方向取反）
            val scrollDy = if (naturalScrolling) -dy else dy
            val vScrollAmount = (scrollDy * sensitivity() / 50).toInt().coerceIn(-5, 5)
            // 水平滚动（反向：左滑右滚）
            val hScrollAmount = (-dx * sensitivity() / 50).toInt().coerceIn(-5, 5)

            if (vScrollAmount != 0 || hScrollAmount != 0) {
                getMouseSender()?.let { sender ->
                    // 垂直/水平滚轮共用一个报文，值 + 清零共两个报文
                    sender.sendMouseScroll(vScrollAmount, hScrollAmount)
                }
                lastTwoFingerX = currentX
                lastTwoFingerY = currentY
                lastSendTime = now
            }
        } else if (!isScrollMode && pointerCount == 1
            && System.currentTimeMillis() - scrollEndTime >= SCROLL_COOLDOWN_MS
        ) {
            // 单指光标移动（或拖拽移动）
            val dx = changes[0].position.x - lastX
            val dy = changes[0].position.y - lastY

            // 灵敏度与光标速度双乘数
            val sensitivityMultiplier = sensitivity() / 5f
            val speedMultiplier = cursorSpeed / 5f
            val moveX = (dx * sensitivityMultiplier * speedMultiplier).toInt().coerceIn(-127, 127)
            val moveY = (dy * sensitivityMultiplier * speedMultiplier).toInt().coerceIn(-127, 127)

            if (moveX != 0 || moveY != 0) {
                getMouseSender()?.let { sender ->
                    if (isDragging) {
                        sender.sendMouseMoveWithButtons(moveX, moveY, leftButton = true)
                    } else {
                        sender.sendMouseMove(moveX, moveY)
                    }
                }
                lastX = changes[0].position.x
                lastY = changes[0].position.y
                lastSendTime = now
            }
        }
    }

    // 对应 MotionEvent.ACTION_POINTER_UP
    fun handlePointerUp(changes: List<PointerInputChange>) {
        if (changes.size == 2) {
            // 退出滚动模式
            isScrollMode = false
            scrollEndTime = System.currentTimeMillis()

            // 双指轻点 → 右键
            var isTap = false
            if (isTwoFingerGesture) {
                val duration = SystemClock.uptimeMillis() - twoFingerStartTime
                val dx = changes[0].position.x - twoFingerStartX[0]
                val dy = changes[0].position.y - twoFingerStartY[0]
                val distance = sqrt(dx * dx + dy * dy)
                if (duration < TWO_FINGER_TAP_TIMEOUT_MS && distance < TWO_FINGER_TAP_MAX_DISTANCE) {
                    isTap = true
                    getMouseSender()?.let { sender ->
                        sender.sendMouseClick(MouseReport.BUTTON_RIGHT)
                    }
                    hapticView.performKeyClick()
                }
            }
            // 滚动抬起触感（轻点已触发时不重复）
            if (!isTap) {
                hapticView.performKeyClick()
            }
            isTwoFingerGesture = false
        }
    }

    // 对应 MotionEvent.ACTION_UP
    fun handleUp(change: PointerInputChange) {
        cancelLongPress()

        if (isThreeFingerGesture) {
            // 三指轻点 → 中键
            val duration = SystemClock.uptimeMillis() - threeFingerStartTime
            val dx = change.position.x - threeFingerStartX
            val dy = change.position.y - threeFingerStartY
            val distance = sqrt(dx * dx + dy * dy)
            if (duration < TAP_MAX_DURATION_MS && distance < TAP_MAX_DISTANCE) {
                getMouseSender()?.let { sender ->
                    sender.sendMouseClick(MouseReport.BUTTON_MIDDLE)
                }
                hapticView.performKeyClick()
            }
            isThreeFingerGesture = false
        } else if (isDragging) {
            // 抬手释放拖拽（松开左键）
            releaseDrag()
        } else if (!isScrollMode) {
            // 单指轻点 → 左键
            val dx = change.position.x - startX
            val dy = change.position.y - startY
            val distance = sqrt(dx * dx + dy * dy)
            val duration = System.currentTimeMillis() - startTime
            if (distance < TAP_MAX_DISTANCE && duration < TAP_MAX_DURATION_MS) {
                getMouseSender()?.let { sender ->
                    sender.sendMouseClick(MouseReport.BUTTON_LEFT)
                }
                hapticView.performKeyClick()
            }
        }
        isScrollMode = false
        isTwoFingerGesture = false
    }

    // 对应 MotionEvent.ACTION_CANCEL
    fun handleCancel() {
        cancelLongPress()
        if (isDragging) releaseDrag()
        isScrollMode = false
        isThreeFingerGesture = false
    }

    awaitEachGesture {
        handleDown(awaitFirstDown(requireUnconsumed = false))

        try {
            while (true) {
                // 长按计时与事件等待交替进行：等待期内事件照常分发，超时则触发拖拽
                val event = if (longPressPending) {
                    val remaining = longPressDeadline - System.currentTimeMillis()
                    if (remaining > 0) {
                        withTimeoutOrNull(remaining) { awaitPointerEvent() }
                    } else {
                        null
                    }
                } else {
                    awaitPointerEvent()
                }

                if (event == null) {
                    // 长按超时
                    fireLongPress()
                    continue
                }

                val changes = event.changes
                when {
                    // 变更已被消费 = 系统合成的取消事件
                    changes.any { it.isConsumed } -> handleCancel()
                    changes.size == 1 && changes[0].changedToUp() -> handleUp(changes[0])
                    changes.any { it.changedToUp() } -> handlePointerUp(changes)
                    changes.any { it.changedToDown() } -> handlePointerDown(changes)
                    else -> handleMove(changes)
                }
                if (changes.none { it.pressed }) break
            }
        } finally {
            // 手势处理被取消（如断开连接）时，确保松开拖拽中的左键
            if (isDragging) releaseDrag()
        }
    }
}

/**
 * 底部一栏的描边按钮，视觉对应 bg_outline_button.xml：
 * surfaceContainer 填充、outlineVariant 描边、12dp 圆角、onSurfaceVariant 文字。
 */
@Composable
private fun TouchpadPanelButton(
    text: String,
    enabled: Boolean = true,
    hapticView: View,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(200.dp)
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) {
                hapticView.performKeyClick()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}
