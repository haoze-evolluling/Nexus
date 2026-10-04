package com.haoze.nexus.bluetooth

import android.content.Context
import android.content.SharedPreferences

/**
 * Encapsulates SharedPreferences access for Bluetooth HID state and settings.
 */
class HidPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "bluetooth_prefs"
        private const val KEY_LAST_DEVICE_ADDRESS = "last_device_address"
        private const val KEY_LAST_DEVICE_NAME = "last_device_name"

        private const val PREFS_SETTINGS_NAME = "settings_prefs"
        private const val KEY_AUTO_CONNECT_LAUNCH = "auto_connect_on_launch"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect_on_disconnect"
        private const val KEY_HID_PROFILE = "hid_profile"
        private const val KEY_CONNECTION_NOTIFICATIONS = "connection_notifications"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val settingsPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_SETTINGS_NAME, Context.MODE_PRIVATE)

    fun getLastDeviceAddress(): String? = prefs.getString(KEY_LAST_DEVICE_ADDRESS, null)

    fun getLastDeviceName(): String? = prefs.getString(KEY_LAST_DEVICE_NAME, null)

    fun saveLastConnectedDevice(address: String, name: String?) {
        prefs.edit()
            .putString(KEY_LAST_DEVICE_ADDRESS, address)
            .putString(KEY_LAST_DEVICE_NAME, name)
            .apply()
    }

    fun isAutoConnectOnLaunchEnabled(): Boolean =
        settingsPrefs.getBoolean(KEY_AUTO_CONNECT_LAUNCH, true)

    fun isAutoReconnectOnDisconnectEnabled(): Boolean =
        settingsPrefs.getBoolean(KEY_AUTO_RECONNECT, true)

    fun isConnectionNotificationsEnabled(): Boolean =
        settingsPrefs.getBoolean(KEY_CONNECTION_NOTIFICATIONS, true)

    fun getStoredHidProfile(): HidProfile =
        HidProfile.fromStorageKey(settingsPrefs.getString(KEY_HID_PROFILE, null))

    fun saveHidProfile(profile: HidProfile) {
        settingsPrefs.edit()
            .putString(KEY_HID_PROFILE, profile.storageKey)
            .apply()
    }
}
