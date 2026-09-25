package com.haoze.nexus

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.haoze.nexus.sound.KeyboardSoundSynthesizer
import com.haoze.nexus.ui.compose.NexusTheme
import com.haoze.nexus.ui.compose.getThemeColorStyle
import com.haoze.nexus.ui.keyboard.KeyboardScreen

class KeyboardActivity : InputActivity() {

    private val soundSynthesizer: KeyboardSoundSynthesizer by lazy { KeyboardSoundSynthesizer(this) }

    private var isConnectedState by mutableStateOf(false)
    private var connectedDeviceNameState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        bluetoothViewModel.connectionState.observe(this) { isConnectedState = it }
        bluetoothViewModel.connectedDeviceName.observe(this) { connectedDeviceNameState = it }

        setContent {
            NexusTheme(colorStyle = getThemeColorStyle(this)) {
                KeyboardScreen(
                    isConnected = isConnectedState,
                    connectedDeviceName = connectedDeviceNameState,
                    canReconnect = bluetoothViewModel.hasLastConnectedDevice(),
                    soundSynthesizer = soundSynthesizer,
                    onExit = ::finish,
                    onOpenTouchpad = ::openTouchpad,
                    onReconnect = ::reconnectLastDevice,
                    sendKey = ::sendKeyToHost
                )
            }
        }
    }

    // Bluke 键盘在未连接时同样保持可交互，本地锁键与键音不受连接状态影响
    private fun sendKeyToHost(keyCode: Int, isPress: Boolean) {
        bluetoothViewModel.getKeyboardSenderDirect()?.sendKey(keyCode, isPress)
    }

    private fun reconnectLastDevice() {
        bluetoothViewModel.getLastConnectedDeviceAddress()?.let { address ->
            Thread { bluetoothViewModel.connectToDevice(address) }.start()
        }
    }

    override fun onDestroy() {
        soundSynthesizer.release()
        super.onDestroy()
    }
}
