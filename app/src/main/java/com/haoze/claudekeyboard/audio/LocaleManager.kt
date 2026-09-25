package com.haoze.claudekeyboard.audio

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/** 应用内语言选项：跟随系统，或固定为中文/英文。 */
enum class AppLanguage(val storageValue: String, val tag: String?) {
    SYSTEM("system", null),
    ZH("zh", "zh"),
    EN("en", "en");

    companion object {
        fun fromStorage(value: String?): AppLanguage = entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

/**
 * 应用内语言切换。偏好存于普通 SharedPreferences：attachBaseContext 需要同步读取，
 * 不适合走 DataStore 的挂起接口。Activity 在 attachBaseContext 里用 wrap() 包一层
 * 配置化 Context，接收服务在构建通知时同样用 wrap() 取本地化文案。
 */
object LocaleManager {
    private const val PREFS_NAME = "steamvoice_language"
    private const val KEY_LANGUAGE = "app_language"

    fun current(context: Context): AppLanguage =
        AppLanguage.fromStorage(prefs(context).getString(KEY_LANGUAGE, null))

    fun set(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(KEY_LANGUAGE, language.storageValue).apply()
    }

    /** 按语言偏好返回本地化 Context；跟随系统时原样返回。 */
    fun wrap(context: Context): Context {
        val tag = current(context).tag ?: return context
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

