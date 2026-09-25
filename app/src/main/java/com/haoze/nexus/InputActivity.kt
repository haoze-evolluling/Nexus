package com.haoze.claudekeyboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.haoze.claudekeyboard.bluetooth.BluetoothViewModel

/**
 * 输入类页面（键盘/触控板/手柄）宿主基类：横屏、全出血绘制，
 * 内容由子类通过 setContent 构建的 Compose 界面填充。
 */
abstract class InputActivity : ComponentActivity() {

    protected val bluetoothViewModel: BluetoothViewModel by viewModels()
    private var isSwitchingInput = false

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
        isSwitchingInput = true
        finish()
    }
}
