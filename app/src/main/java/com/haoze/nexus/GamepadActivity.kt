package com.haoze.claudekeyboard

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.haoze.claudekeyboard.ui.compose.SyncTouchTheme
import com.haoze.claudekeyboard.ui.compose.getThemeColorStyle
import com.haoze.claudekeyboard.ui.gamepad.GamepadScreen

/**
 * Bluke 手柄页宿主：横屏全出血（与键盘/触控板一致），
 * 手柄报文经 GamepadSender 直发 HID，顶栏模式按钮切回键盘。
 */
class GamepadActivity : InputActivity() {

    private var isConnectedState by mutableStateOf(false)
    private var connectedDeviceNameState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        bluetoothViewModel.connectionState.observe(this) { isConnectedState = it }
        bluetoothViewModel.connectedDeviceName.observe(this) { connectedDeviceNameState = it }

        setContent {
            SyncTouchTheme(colorStyle = getThemeColorStyle(this)) {
                GamepadScreen(
                    isConnected = isConnectedState,
                    deviceName = connectedDeviceNameState,
                    onExit = ::finish,
                    onOpenKeyboard = ::openKeyboard,
                    sendGamepadReport = { mask, hat, lx, ly, rx, ry ->
                        bluetoothViewModel.getGamepadSenderDirect()?.sendGamepadReport(mask, hat, lx, ly, rx, ry)
                    }
                )
            }
        }
    }

    private fun hideSystemBars() {
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
