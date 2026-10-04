package com.haoze.nexus.ui.compose

import android.content.Context

/**
 * 底栏自定义偏好存储管理工具类。
 */
object BottomBarPreferences {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_BOTTOM_BAR_DESTINATIONS = "bottom_bar_destinations"

    fun getBottomBarDestinations(context: Context): List<BottomBarDestination> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BOTTOM_BAR_DESTINATIONS, null)
        return BottomBarDestination.parseJsonList(json)
    }

    fun setBottomBarDestinations(context: Context, destinations: List<BottomBarDestination>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BOTTOM_BAR_DESTINATIONS, BottomBarDestination.toJsonList(destinations))
            .apply()
    }

    fun resetBottomBarDestinations(context: Context) {
        setBottomBarDestinations(context, BottomBarDestination.DEFAULT_DESTINATIONS)
    }
}
