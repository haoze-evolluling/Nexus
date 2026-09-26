package com.haoze.nexus

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.haoze.nexus.ui.AppLanguageManager
import com.haoze.nexus.ui.AppLanguageMode

abstract class AppLocalizedActivity : ComponentActivity() {
    private var languageModeAtCreate = AppLanguageMode.SYSTEM

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        languageModeAtCreate = AppLanguageManager.getMode(this)
    }

    override fun onResume() {
        super.onResume()
        val currentLanguageMode = AppLanguageManager.getMode(this)
        if (currentLanguageMode != languageModeAtCreate) {
            languageModeAtCreate = currentLanguageMode
            recreate()
        }
    }
}
