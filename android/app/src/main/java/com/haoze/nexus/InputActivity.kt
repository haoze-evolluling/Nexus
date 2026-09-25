package com.haoze.nexus

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.haoze.nexus.bluetooth.BluetoothViewModel

/**
 * 输入类页面（键盘/触控板/手柄）宿主基类：横屏、全出血绘制，
 * 内容由子类通过 setContent 构建的 Compose 界面填充。
 */
abstract class InputActivity : ComponentActivity() {

    protected val bluetoothViewModel: BluetoothViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!bluetoothViewModel.hasBluetoothSupport()) {
            Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bluetoothViewModel.startAndBindService()
    }

    fun openKeyboard() = openInput(KeyboardActivity::class.java)

    fun openTouchpad() = openInput(TouchpadActivity::class.java)

    private fun openInput(target: Class<out InputActivity>) {
        if (javaClass == target) return
        startActivity(Intent(this, target))
        finish()
    }

    protected fun hideSystemBars() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                )
                window.insetsController?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
            }
        } catch (_: Exception) {
        }
    }
}
