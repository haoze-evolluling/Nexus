package com.haoze.nexus

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import com.haoze.nexus.bluetooth.HidProfile
import com.haoze.nexus.crash.CrashBreadcrumbs
import com.haoze.nexus.crash.CrashCollector
import com.haoze.nexus.crash.CrashHandler
import com.haoze.nexus.crash.CrashLogManager
import com.haoze.nexus.ui.compose.ThemeController

class NexusApplication : Application() {

    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        instance = this

        CrashCollector.appStartElapsedRealtime = SystemClock.elapsedRealtime()

        // Install the global uncaught-exception crash handler.
        CrashHandler.install(this)
        CrashBreadcrumbs.record("APP", "Application.onCreate() initialized")

        // Asynchronously check for and extract any uncaught native crash from previous runs (Android 11+).
        Thread({
            CrashLogManager.checkAndCollectNativeCrashes(this)
        }, "NativeCrashCollector").start()

        migrateDeviceTypeDefaultIfNeeded()
        ThemeController.init(this)

        // Track activity lifecycles to maintain foreground status and record crash breadcrumbs.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                CrashBreadcrumbs.record("LIFECYCLE", "${activity.javaClass.simpleName} created")
            }

            override fun onActivityStarted(activity: Activity) {
                startedActivityCount++
                if (startedActivityCount == 1) {
                    CrashHandler.isAppForeground = true
                    CrashBreadcrumbs.record("LIFECYCLE", "App entered foreground")
                }
            }

            override fun onActivityResumed(activity: Activity) {
                CrashHandler.currentActivityName = activity.javaClass.simpleName
                CrashBreadcrumbs.record("LIFECYCLE", "${activity.javaClass.simpleName} resumed")
            }

            override fun onActivityPaused(activity: Activity) {
                CrashBreadcrumbs.record("LIFECYCLE", "${activity.javaClass.simpleName} paused")
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount = maxOf(0, startedActivityCount - 1)
                if (startedActivityCount == 0) {
                    CrashHandler.isAppForeground = false
                    CrashBreadcrumbs.record("LIFECYCLE", "App entered background")
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (CrashHandler.currentActivityName == activity.javaClass.simpleName) {
                    CrashHandler.currentActivityName = null
                }
                CrashBreadcrumbs.record("LIFECYCLE", "${activity.javaClass.simpleName} destroyed")
            }
        })
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
        @Volatile
        var instance: NexusApplication? = null
            private set

        private const val PREFS_SETTINGS_NAME = "settings_prefs"
        private const val KEY_HID_PROFILE = "hid_profile"
        private const val KEY_PROFILE_MIGRATED_TO_KEYBOARD_MOUSE = "device_type_migrated_to_combo_v1"
    }
}

