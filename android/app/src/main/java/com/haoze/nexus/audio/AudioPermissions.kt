package com.haoze.nexus.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Manages required runtime permissions for audio receiver and local network discovery.
 */
object AudioPermissions {
    const val PERMISSION_ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

    fun getMissingAudioPermissions(context: Context): Array<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= 37 &&
            ContextCompat.checkSelfPermission(context, PERMISSION_ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(PERMISSION_ACCESS_LOCAL_NETWORK)
        }
        return permissions.toTypedArray()
    }

    fun hasRequiredAudioPermissions(context: Context): Boolean =
        getMissingAudioPermissions(context).isEmpty()
}
