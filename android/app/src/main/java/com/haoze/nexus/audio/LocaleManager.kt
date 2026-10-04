package com.haoze.nexus.audio

import android.content.Context
import com.haoze.nexus.ui.AppLanguageManager
import com.haoze.nexus.ui.AppLanguageMode

/** 应用内语言选项：跟随系统，或固定为中文/英文。保留此枚举以兼容现有音频模块调用。 */
enum class AppLanguage(val storageValue: String, val tag: String?) {
    SYSTEM(AppLanguageMode.SYSTEM.storageValue, null),
    ZH(AppLanguageMode.CHINESE.storageValue, "zh"),
    EN(AppLanguageMode.ENGLISH.storageValue, "en");

    companion object {
        fun fromStorage(value: String?): AppLanguage = entries.firstOrNull { it.storageValue == value } ?: SYSTEM

        fun fromMode(mode: AppLanguageMode): AppLanguage = when (mode) {
            AppLanguageMode.SYSTEM -> SYSTEM
            AppLanguageMode.CHINESE -> ZH
            AppLanguageMode.ENGLISH -> EN
        }
    }

    fun toMode(): AppLanguageMode = when (this) {
        SYSTEM -> AppLanguageMode.SYSTEM
        ZH -> AppLanguageMode.CHINESE
        EN -> AppLanguageMode.ENGLISH
    }
}

/**
 * 语言管理桥接单例：已对齐至全局 AppLanguageManager，
 * 保证音频后台服务、通知栏及多端校时与全应用语言偏好完全同步。
 */
object LocaleManager {
    fun current(context: Context): AppLanguage =
        AppLanguage.fromMode(AppLanguageManager.getMode(context))

    fun set(context: Context, language: AppLanguage) {
        AppLanguageManager.setMode(context, language.toMode())
    }

    /** 返回本地化 Context，跟随全局 AppLanguageManager 规则 */
    fun wrap(context: Context): Context = AppLanguageManager.wrap(context)
}
