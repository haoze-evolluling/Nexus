package com.haoze.nexus.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AppLanguageManagerTest {

    @Test
    fun fromStorageValue_parsesCorrectly() {
        assertEquals(AppLanguageMode.SYSTEM, AppLanguageMode.fromStorageValue("system"))
        assertEquals(AppLanguageMode.CHINESE, AppLanguageMode.fromStorageValue("zh"))
        assertEquals(AppLanguageMode.ENGLISH, AppLanguageMode.fromStorageValue("en"))
    }

    @Test
    fun fromStorageValue_defaultsToSystemOnUnknownOrNull() {
        assertEquals(AppLanguageMode.SYSTEM, AppLanguageMode.fromStorageValue(null))
        assertEquals(AppLanguageMode.SYSTEM, AppLanguageMode.fromStorageValue(""))
        assertEquals(AppLanguageMode.SYSTEM, AppLanguageMode.fromStorageValue("invalid_code"))
    }

    @Test
    fun appLanguageMode_storageValuesMatchExpectedStrings() {
        assertEquals("system", AppLanguageMode.SYSTEM.storageValue)
        assertEquals("zh", AppLanguageMode.CHINESE.storageValue)
        assertEquals("en", AppLanguageMode.ENGLISH.storageValue)
    }

    @Test
    fun appLanguageMode_localesMatchExpected() {
        assertNull(AppLanguageMode.SYSTEM.locale)
        assertEquals(Locale.SIMPLIFIED_CHINESE, AppLanguageMode.CHINESE.locale)
        assertEquals(Locale.ENGLISH, AppLanguageMode.ENGLISH.locale)
    }

    @Test
    fun sharedPreferencesConstants_maintainCompatibility() {
        assertEquals("nexus_language", AppLanguageManager.PREFS_NAME)
        assertEquals("app_language", AppLanguageManager.KEY_LANGUAGE_MODE)
    }
}
