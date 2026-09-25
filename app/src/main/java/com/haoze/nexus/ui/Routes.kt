package com.haoze.nexus.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val SETTINGS = "settings"
    const val APPEARANCE_SETTINGS = "appearance_settings"
    const val DAY_NIGHT_MODE = "day_night_mode"
    const val THEME_COLOR_SETTINGS = "theme_color_settings"
    const val INPUT_SETTINGS = "input_settings"
    const val FEEDBACK_SETTINGS = "feedback_settings"
    const val CONNECTION_SETTINGS = "connection_settings"
    const val DATA_SETTINGS = "data_settings"
    const val ABOUT = "about"
    const val SPONSOR = "sponsor"
    const val SPONSOR_LIST = "sponsor_list"
}

enum class SettingsSection(val title: String, val order: Int) {
    APPEARANCE("外观", 0), INPUT("输入", 1),
    FEEDBACK("反馈", 2), CONNECTION("连接", 3), DATA("数据管理", 4)
}

data class SettingsDestination(
    val route: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val parentRoute: String? = null,
    val mainSection: SettingsSection? = null
)

object ScreenDestinations {
    private fun main(route: String, title: String, description: String, icon: ImageVector, section: SettingsSection) =
        SettingsDestination(route, title, description, icon, mainSection = section)
    private fun child(route: String, title: String, description: String, icon: ImageVector, parent: SettingsDestination) =
        SettingsDestination(route, title, description, icon, parentRoute = parent.route)

    val appearanceSettings = main(Routes.APPEARANCE_SETTINGS, "外观设置", "选择应用的显示模式与主题色", Icons.Filled.Palette, SettingsSection.APPEARANCE)
    val inputSettings = main(Routes.INPUT_SETTINGS, "输入设置", "调整移动灵敏度、光标速度和滚动方向", Icons.Filled.Mouse, SettingsSection.INPUT)
    val feedbackSettings = main(Routes.FEEDBACK_SETTINGS, "触感反馈", "控制操作时的触感反馈", Icons.Filled.Vibration, SettingsSection.FEEDBACK)
    val connectionSettings = main(Routes.CONNECTION_SETTINGS, "连接与设备", "管理自动连接、断线重连、屏幕常亮和连接通知", Icons.Filled.Bluetooth, SettingsSection.CONNECTION)
    val dataSettings = main(Routes.DATA_SETTINGS, "数据管理", "恢复内置快捷命令，并清除自定义命令", Icons.Filled.DeleteSweep, SettingsSection.DATA)
    val dayNightMode = child(Routes.DAY_NIGHT_MODE, "应用主题", "选择浅色、深色或跟随系统显示", Icons.Filled.Contrast, appearanceSettings)
    val themeColorSettings = child(Routes.THEME_COLOR_SETTINGS, "主题色配置", "选择应用界面的强调色", Icons.Filled.Palette, appearanceSettings)
    val all = listOf(appearanceSettings, inputSettings, feedbackSettings,
        connectionSettings, dataSettings, dayNightMode, themeColorSettings)
    val mainEntries = all.filter { it.mainSection != null }
        .sortedWith(compareBy({ it.mainSection!!.order }, { all.indexOf(it) }))
    private val byRoute = all.associateBy { it.route }

    init {
        require(byRoute.size == all.size) { "设置路由不得重复" }
        all.forEach { destination ->
            require((destination.mainSection != null) xor (destination.parentRoute != null)) { "设置页面必须是一级入口或具有父路由: ${destination.route}" }
            require(destination.parentRoute == null || byRoute.containsKey(destination.parentRoute)) { "无效父路由: ${destination.parentRoute}" }
        }
    }
}
