package com.haoze.claudekeyboard.util

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View

fun View.performKeyClick() {
    if (!isHapticEnabled()) return
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

fun View.performHapticLongPress() {
    if (!isHapticEnabled()) return
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

fun View.isHapticEnabled(): Boolean {
    val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("haptic_feedback", true)
}
