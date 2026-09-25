package com.haoze.claudekeyboard.ui.compose

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object ThemeController {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_THEME_MODE_INDEX = "theme_mode_index"

    var nightModeIndex by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        nightModeIndex = prefs.getInt(KEY_THEME_MODE_INDEX, 0)
    }

    fun setNightModeIndex(context: Context, index: Int) {
        nightModeIndex = index
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE_INDEX, index)
            .apply()
    }
}
