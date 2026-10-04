package com.haoze.nexus.ui.gamepad

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

internal fun Color.gpDarker(factor: Float = 0.22f): Color {
    return Color(
        red = (this.red * (1f - factor)).coerceIn(0f, 1f),
        green = (this.green * (1f - factor)).coerceIn(0f, 1f),
        blue = (this.blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = this.alpha
    )
}

internal fun Color.gpLighter(factor: Float = 0.25f): Color {
    return Color(
        red = (this.red + (1f - this.red) * factor).coerceIn(0f, 1f),
        green = (this.green + (1f - this.green) * factor).coerceIn(0f, 1f),
        blue = (this.blue + (1f - this.blue) * factor).coerceIn(0f, 1f),
        alpha = this.alpha
    )
}

fun Modifier.gamepadButtonTouch(
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onPressedStateChange: (Boolean) -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val targetPointerId = down.id
        onPressedStateChange(true)
        onPress()
        down.consume()
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == targetPointerId } ?: break
            if (!change.pressed) {
                onPressedStateChange(false)
                onRelease()
                break
            }
            change.consume()
        }
    }
}
