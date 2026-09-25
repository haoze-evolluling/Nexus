package com.haoze.nexus.audio

import android.content.Context
import com.haoze.nexus.R

object DeviceIdentity {
    fun friendlyName(context: Context): String {
        val manufacturer = android.os.Build.MANUFACTURER.trim()
        val model = android.os.Build.MODEL.trim()
        return listOf(manufacturer, model).filter { it.isNotEmpty() }.distinct().joinToString(" ")
            .ifEmpty { context.getString(R.string.android_device) }
    }
}
