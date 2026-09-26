package com.haoze.nexus.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector
import com.haoze.nexus.R
import org.json.JSONArray

/**
 * 可配置在底部悬浮导航栏中的目标页面定义。
 */
enum class BottomBarDestination(
    val id: String,
    val titleRes: Int,
    val tabLabelRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
) {
    HOME(
        id = "home",
        titleRes = R.string.tab_home,
        tabLabelRes = R.string.tab_home,
        descriptionRes = R.string.bottom_bar_desc_home,
        icon = Icons.Default.Home
    ),
    AUDIO_RECEIVER(
        id = "audio_receiver",
        titleRes = R.string.home_audio_stream_title,
        tabLabelRes = R.string.home_audio_stream_title,
        descriptionRes = R.string.bottom_bar_desc_audio,
        icon = Icons.Default.GraphicEq
    ),
    AGENT(
        id = "agent",
        titleRes = R.string.home_agent_title,
        tabLabelRes = R.string.home_agent_chip,
        descriptionRes = R.string.bottom_bar_desc_agent,
        icon = Icons.Default.Terminal
    ),
    TV_REMOTE(
        id = "tv_remote",
        titleRes = R.string.home_tvremote_title,
        tabLabelRes = R.string.tab_tv_remote,
        descriptionRes = R.string.bottom_bar_desc_tv_remote,
        icon = Icons.Default.SettingsRemote
    ),
    SETTINGS(
        id = "settings",
        titleRes = R.string.home_settings_title,
        tabLabelRes = R.string.home_settings_title,
        descriptionRes = R.string.bottom_bar_desc_settings,
        icon = Icons.Default.Settings
    );

    companion object {
        const val MIN_COUNT = 2
        const val MAX_COUNT = 4

        val DEFAULT_DESTINATIONS: List<BottomBarDestination> = listOf(HOME, AUDIO_RECEIVER)

        fun fromId(id: String): BottomBarDestination? = entries.firstOrNull { it.id == id }

        fun parseJsonList(jsonStr: String?): List<BottomBarDestination> {
            if (jsonStr.isNullOrBlank()) return DEFAULT_DESTINATIONS
            return try {
                val array = JSONArray(jsonStr)
                val items = mutableListOf<BottomBarDestination>()
                for (i in 0 until array.length()) {
                    val id = array.optString(i)
                    val destination = fromId(id)
                    if (destination != null && destination !in items) {
                        items.add(destination)
                    }
                }
                if (items.size < MIN_COUNT) {
                    DEFAULT_DESTINATIONS
                } else {
                    items.take(MAX_COUNT)
                }
            } catch (_: Exception) {
                DEFAULT_DESTINATIONS
            }
        }

        fun toJsonList(destinations: List<BottomBarDestination>): String {
            val validItems = destinations.distinct().take(MAX_COUNT)
            val finalItems = if (validItems.size < MIN_COUNT) DEFAULT_DESTINATIONS else validItems
            val array = JSONArray()
            finalItems.forEach { array.put(it.id) }
            return array.toString()
        }
    }
}
