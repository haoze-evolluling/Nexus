package com.haoze.nexus

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.haoze.nexus.ui.compose.NexusTheme
import com.haoze.nexus.ui.compose.getThemeColorStyle
import com.haoze.nexus.ui.gamepad.GamepadScreen

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
            NexusTheme(colorStyle = getThemeColorStyle(this)) {
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
}
