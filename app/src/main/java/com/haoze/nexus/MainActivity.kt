package com.haoze.nexus

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.haoze.nexus.bluetooth.BluetoothViewModel
import com.haoze.nexus.ui.Routes
import com.haoze.nexus.ui.compose.AppPage
import com.haoze.nexus.ui.compose.NexusApp
import com.haoze.nexus.ui.compose.NexusTheme
import com.haoze.nexus.ui.compose.ThemeController
import com.haoze.nexus.ui.compose.getThemeColorStyle

class MainActivity : ComponentActivity() {

    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    private var isConnectedState by mutableStateOf(false)
    private var connectedDeviceNameState by mutableStateOf<String?>(null)
    private var showDeviceListDialog by mutableStateOf(false)
    private var pairedDevicesState by mutableStateOf<List<BluetoothDevice>>(emptyList())
    private var deviceListPermissionDenied by mutableStateOf(false)
    private var connectingDeviceAddress by mutableStateOf<String?>(null)
    private var colorStyleState by mutableStateOf(com.haoze.nexus.ui.compose.ThemeColorStyle.SYSTEM)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { !it }) {
            Toast.makeText(this, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
        } else {
            bluetoothViewModel.startAndBindService()
        }
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 设置页可能修改了主题色，返回后刷新
        colorStyleState = getThemeColorStyle(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeController.init(this)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!bluetoothViewModel.hasBluetoothSupport()) {
            Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val missing = getMissingPermissions()
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing)
        } else {
            bluetoothViewModel.startAndBindService()
        }

        colorStyleState = getThemeColorStyle(this)
        observeViewModel()
        setupComposeContent()

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun getMissingPermissions(): Array<String> {
        val required = mutableListOf<String>()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    required.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                    required.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    required.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            else -> {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    required.add(Manifest.permission.BLUETOOTH)
                }
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                    required.add(Manifest.permission.BLUETOOTH_ADMIN)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return required.toTypedArray()
    }

    private fun setupComposeContent() {
        setContent {
            NexusTheme(colorStyle = colorStyleState) {
                NexusApp(
                    isConnected = isConnectedState,
                    connectedDeviceName = connectedDeviceNameState,
                    onNavigate = ::openPage,
                    onNavigateRoute = ::openRoute,
                    onOpenKeyboard = {
                        startActivity(Intent(this@MainActivity, KeyboardActivity::class.java))
                    },
                    onOpenTouchpad = {
                        startActivity(Intent(this@MainActivity, TouchpadActivity::class.java))
                    },
                    onOpenGamepad = {
                        startActivity(Intent(this@MainActivity, GamepadActivity::class.java))
                    },
                    onShowDeviceList = ::showDeviceListDialog,
                    showDeviceList = showDeviceListDialog,
                    pairedDevices = pairedDevicesState,
                    deviceListPermissionDenied = deviceListPermissionDenied,
                    connectingDeviceAddress = connectingDeviceAddress,
                    connectedDeviceAddress = bluetoothViewModel.getConnectedDeviceAddress(),
                    lastConnectedDeviceAddress = bluetoothViewModel.getLastConnectedDeviceAddress(),
                    onDismissDeviceList = { dismissDeviceList(cancelConnection = true) },
                    onConnectDevice = ::connectToDevice,
                    onDisconnectDevice = ::disconnectDevice,
                    onConnectionTimeout = ::onDeviceConnectionTimeout
                )
            }
        }
    }

    private fun observeViewModel() {
        bluetoothViewModel.connectionState.observe(this) { isConnected ->
            val deviceName = bluetoothViewModel.connectedDeviceName.value
            isConnectedState = isConnected
            connectedDeviceNameState = deviceName
            updateKeepScreenOn(isConnected)
            if (isConnected) {
                connectingDeviceAddress = null
                showDeviceListDialog = false
            }
        }

        bluetoothViewModel.connectedDeviceName.observe(this) {
            connectedDeviceNameState = it
        }

        bluetoothViewModel.registrationState.observe(this) { isRegistered ->
            if (!isRegistered) {
                Toast.makeText(this, R.string.toast_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            }
        }

        bluetoothViewModel.sendError.observe(this) { message ->
            Toast.makeText(this, getString(R.string.toast_send_error, message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPage(page: AppPage) {
        when (page) {
            AppPage.AGENT -> startActivity(Intent(this, AgentActivity::class.java))
            AppPage.TV_REMOTE -> startActivity(Intent(this, TvRemoteActivity::class.java))
            AppPage.SETTINGS -> settingsLauncher.launch(SettingsActivity.createIntent(this, Routes.SETTINGS))
            else -> return
        }
    }

    private fun openRoute(route: String) {
        settingsLauncher.launch(SettingsActivity.createIntent(this, route))
    }

    private fun updateKeepScreenOn(isConnected: Boolean) {
        val prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        val keepScreenOn = prefs.getBoolean("keep_screen_on", true)
        if (isConnected && keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun showDeviceListDialog() {
        deviceListPermissionDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        pairedDevicesState = if (deviceListPermissionDenied) {
            emptyList()
        } else {
            try {
                getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices?.toList().orEmpty()
            } catch (_: SecurityException) {
                deviceListPermissionDenied = true
                emptyList()
            }
        }
        showDeviceListDialog = true
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (connectingDeviceAddress != null || device.address == bluetoothViewModel.getConnectedDeviceAddress()) return
        connectingDeviceAddress = device.address
        Thread {
            if (!bluetoothViewModel.connectToDevice(device.address)) {
                runOnUiThread {
                    if (connectingDeviceAddress == device.address) {
                        connectingDeviceAddress = null
                        Toast.makeText(this, R.string.dialog_connect_timeout_message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun dismissDeviceList(cancelConnection: Boolean) {
        if (cancelConnection && connectingDeviceAddress != null) {
            bluetoothViewModel.disconnect()
        }
        connectingDeviceAddress = null
        showDeviceListDialog = false
    }

    private fun disconnectDevice() {
        bluetoothViewModel.disconnect()
        connectingDeviceAddress = null
        showDeviceListDialog = false
    }

    private fun onDeviceConnectionTimeout() {
        bluetoothViewModel.disconnect()
        connectingDeviceAddress = null
    }

}
