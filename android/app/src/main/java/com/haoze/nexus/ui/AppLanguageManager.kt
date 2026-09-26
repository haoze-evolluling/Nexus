package com.haoze.nexus.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import com.haoze.nexus.R
import java.util.Locale

enum class AppLanguageMode(
    val storageValue: String,
    @StringRes val labelRes: Int,
    val locale: Locale?
) {
    SYSTEM("system", R.string.language_follow_system, null),
    CHINESE("zh", R.string.language_chinese, Locale.SIMPLIFIED_CHINESE),
    ENGLISH("en", R.string.language_english, Locale.ENGLISH);

    companion object {
        fun fromStorageValue(value: String?): AppLanguageMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

object AppLanguageManager {
    const val PREFS_NAME = "nexus_language"
    const val KEY_LANGUAGE_MODE = "app_language"

    fun getMode(context: Context): AppLanguageMode = AppLanguageMode.fromStorageValue(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_MODE, AppLanguageMode.SYSTEM.storageValue)
    )

    fun setMode(context: Context, mode: AppLanguageMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_MODE, mode.storageValue)
            .apply()
    }

    fun wrap(context: Context): Context {
        val locale = getMode(context).locale ?: run {
            val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            if (systemLocale != null && systemLocale.language.equals(Locale.CHINESE.language, ignoreCase = true)) {
                Locale.SIMPLIFIED_CHINESE
            } else {
                Locale.ENGLISH
            }
        }
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }
}
