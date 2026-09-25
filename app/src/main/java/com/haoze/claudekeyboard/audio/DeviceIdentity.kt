package com.haoze.claudekeyboard.audio

import android.content.Context
import com.haoze.claudekeyboard.R

object DeviceIdentity {
    fun friendlyName(context: Context): String {
        val manufacturer = android.os.Build.MANUFACTURER.trim()
        val model = android.os.Build.MODEL.trim()
        return listOf(manufacturer, model).filter { it.isNotEmpty() }.distinct().joinToString(" ")
            .ifEmpty { context.getString(R.string.android_device) }
    }
}

/** 电脑在设备列表中的连接状态。 */
enum class PcConnectionState { ONLINE, CONNECTING, CONNECTED }
