package com.haoze.claudekeyboard

import android.app.Application
import android.content.Context
import com.haoze.claudekeyboard.bluetooth.HidProfile
import com.haoze.claudekeyboard.ui.compose.ThemeController

class SyncTouchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        migrateDeviceTypeDefaultIfNeeded()
        ThemeController.init(this)
    }

    private fun migrateDeviceTypeDefaultIfNeeded() {
        val prefs = getSharedPreferences(PREFS_SETTINGS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_PROFILE_MIGRATED_TO_KEYBOARD_MOUSE, false)) {
            val currentKey = prefs.getString(KEY_HID_PROFILE, null)
            val editor = prefs.edit().putBoolean(KEY_PROFILE_MIGRATED_TO_KEYBOARD_MOUSE, true)
            // 若原来是手柄或未显式配置（旧版本默认即为手柄），升级后自动调整为键鼠组合
            if (currentKey == null || currentKey == HidProfile.GAMEPAD.storageKey) {
                editor.putString(KEY_HID_PROFILE, HidProfile.KEYBOARD_MOUSE.storageKey)
            }
            editor.apply()
        }
    }

    companion object {
        private const val PREFS_SETTINGS_NAME = "settings_prefs"
        private const val KEY_HID_PROFILE = "hid_profile"
        private const val KEY_PROFILE_MIGRATED_TO_KEYBOARD_MOUSE = "device_type_migrated_to_combo_v1"
    }
}
