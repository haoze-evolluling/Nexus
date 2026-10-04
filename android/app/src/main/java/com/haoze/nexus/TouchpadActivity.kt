package com.haoze.nexus

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.haoze.nexus.ui.compose.NexusTheme
import com.haoze.nexus.ui.compose.getThemeColorStyle
import com.haoze.nexus.ui.touchpad.TouchpadScreen

/**
 * 触控板页宿主：Compose 实现的 TouchpadScreen，
 * 连接状态变化时同步启用手势与按钮，鼠标报文经 MouseSender 直发 HID。
 */
class TouchpadActivity : InputActivity() {

    private var isConnectedState by mutableStateOf(false)
    private var connectedDeviceNameState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothViewModel.connectionState.observe(this) { isConnectedState = it }
        bluetoothViewModel.connectedDeviceName.observe(this) { connectedDeviceNameState = it }

        setContent {
            NexusTheme(colorStyle = getThemeColorStyle(this)) {
                TouchpadScreen(
                    isConnected = isConnectedState,
                    onExit = ::finish,
                    onOpenKeyboard = ::openKeyboard,
                    getMouseSender = { bluetoothViewModel.mouseSender.value }
                )
            }
        }
    }
}
