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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.haoze.nexus.audio.ActivePc
import com.haoze.nexus.audio.AudioReceiverService
import com.haoze.nexus.audio.ConnectionBus
import com.haoze.nexus.audio.ConnectionEvent
import com.haoze.nexus.audio.DeviceIdentity
import com.haoze.nexus.audio.PcConnector
import com.haoze.nexus.audio.PcDevice
import com.haoze.nexus.audio.PcDiscovery
import com.haoze.nexus.audio.PcTrustRepository
import com.haoze.nexus.audio.SettingsRepository
import com.haoze.nexus.bluetooth.BluetoothViewModel
import com.haoze.nexus.bluetooth.HidProfile
import com.haoze.nexus.bluetooth.KeyboardSender
import com.haoze.nexus.macro.Macro
import com.haoze.nexus.macro.MacroRepository
import com.haoze.nexus.ui.Routes
import com.haoze.nexus.ui.audio.AudioReceiverActivity
import com.haoze.nexus.ui.compose.AppPage
import com.haoze.nexus.ui.compose.BottomBarDestination
import com.haoze.nexus.ui.compose.BottomBarPreferences
import com.haoze.nexus.ui.compose.CoreCommand
import com.haoze.nexus.ui.compose.NexusApp
import com.haoze.nexus.ui.compose.NexusTheme
import com.haoze.nexus.ui.compose.ThemeColorStyle
import com.haoze.nexus.ui.compose.ThemeController
import com.haoze.nexus.ui.compose.TvRemoteAction
import com.haoze.nexus.ui.compose.getThemeColorStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppLocalizedActivity() {

    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    private var isConnectedState by mutableStateOf(false)
    private var connectedDeviceNameState by mutableStateOf<String?>(null)
    private var showDeviceListDialog by mutableStateOf(false)
    private var pairedDevicesState by mutableStateOf<List<BluetoothDevice>>(emptyList())
    private var deviceListPermissionDenied by mutableStateOf(false)
    private var connectingDeviceAddress by mutableStateOf<String?>(null)
    private var colorStyleState by mutableStateOf(ThemeColorStyle.SYSTEM)
    private var inputProfileState by mutableStateOf(HidProfile.DEFAULT)
    private var bottomBarItemsState by mutableStateOf<List<BottomBarDestination>>(BottomBarDestination.DEFAULT_DESTINATIONS)

    // Audio receiver state
    private val audioNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        startAudioReceiver()
    }
    private val audioDiscovery by lazy { PcDiscovery(this) }
    private val audioConnector = PcConnector()
    private val audioRepository by lazy { SettingsRepository(applicationContext) }
    private val audioTrustRepository by lazy { PcTrustRepository(applicationContext) }
    private var audioSelfId by mutableStateOf("")
    private val audioSelfName: String by lazy { DeviceIdentity.friendlyName(applicationContext) }
    private var audioReceiverRunning by mutableStateOf(false)

    // Macro state
    private val macroRepository by lazy { MacroRepository(this) }
    private var macrosState by mutableStateOf<List<Macro>>(emptyList())

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
        // 设置页可能修改了主题色或底栏配置，返回后刷新
        colorStyleState = getThemeColorStyle(this)
        bottomBarItemsState = BottomBarPreferences.getBottomBarDestinations(this)
        macrosState = macroRepository.getAllMacros()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeController.init(this)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bottomBarItemsState = BottomBarPreferences.getBottomBarDestinations(this)
        macrosState = macroRepository.getAllMacros()

        lifecycleScope.launch(Dispatchers.IO) {
            audioSelfId = audioRepository.settings.first().deviceId
        }

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

        if (bottomBarItemsState.contains(BottomBarDestination.AUDIO_RECEIVER)) {
            ensureAudioReceiverRunning()
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        bottomBarItemsState = BottomBarPreferences.getBottomBarDestinations(this)
        macrosState = macroRepository.getAllMacros()
        colorStyleState = getThemeColorStyle(this)
        if (bottomBarItemsState.contains(BottomBarDestination.AUDIO_RECEIVER)) {
            audioDiscovery.start()
        }
    }

    override fun onStart() {
        super.onStart()
        if (bottomBarItemsState.contains(BottomBarDestination.AUDIO_RECEIVER)) {
            audioDiscovery.start()
        }
    }

    override fun onStop() {
        super.onStop()
        audioDiscovery.stop()
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
                    bottomBarItems = bottomBarItemsState,
                    inputProfile = inputProfileState,
                    onInputProfileChanged = { profile ->
                        bluetoothViewModel.setInputProfile(profile)
                    },
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
                    onOpenTvRemote = {
                        startActivity(Intent(this@MainActivity, TvRemoteActivity::class.java))
                    },
                    onOpenAudioReceiver = {
                        startActivity(Intent(this@MainActivity, AudioReceiverActivity::class.java))
                    },
                    onOpenAgent = {
                        startActivity(Intent(this@MainActivity, AgentActivity::class.java))
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
                    onConnectionTimeout = ::onDeviceConnectionTimeout,
                    audioDiscovery = audioDiscovery,
                    audioConnector = audioConnector,
                    audioRepository = audioRepository,
                    audioTrustRepository = audioTrustRepository,
                    audioSelfId = audioSelfId,
                    audioSelfName = audioSelfName,
                    audioReceiverRunning = audioReceiverRunning,
                    onAudioConnect = ::connectToPc,
                    onAudioDisconnect = ::disconnectFromPc,
                    onCoreCommand = ::sendCoreCommand,
                    macros = macrosState,
                    onMacroClick = ::sendMacro,
                    onMacroLongClick = {
                        startActivity(Intent(this@MainActivity, AgentActivity::class.java))
                    },
                    onAddMacro = {
                        startActivity(Intent(this@MainActivity, AgentActivity::class.java))
                    },
                    onTvRemoteAction = ::sendTvRemoteAction
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

        bluetoothViewModel.inputProfile.observe(this) { profile ->
            inputProfileState = profile
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

    // Audio receiver lifecycle & connection
    private fun ensureAudioReceiverRunning() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            audioNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startAudioReceiver()
    }

    private fun startAudioReceiver() {
        ContextCompat.startForegroundService(this, Intent(this, AudioReceiverService::class.java))
        audioReceiverRunning = true
    }

    private fun connectToPc(pc: PcDevice) {
        if (audioSelfId.isEmpty()) return
        ensureAudioReceiverRunning()
        val requestId = audioSelfId
        lifecycleScope.launch(Dispatchers.IO) {
            val result = audioConnector.request(pc, requestId, audioSelfName)
            if (result is PcConnector.ConnectResult.Accepted) {
                runCatching {
                    val actualHost = result.verifiedHost.ifEmpty { pc.host }
                    ConnectionBus.queuedSender = ActivePc(pc.deviceId, pc.name, java.net.InetAddress.getByName(actualHost), nonce = result.nonce)
                    audioTrustRepository.trust(pc.deviceId, pc.name)
                }
            } else if (result is PcConnector.ConnectResult.Denied) {
                ConnectionBus.notify(R.string.pc_denied)
            } else if (result is PcConnector.ConnectResult.Timeout) {
                ConnectionBus.notify(R.string.pc_connect_failed, pc.name)
            }
        }
    }

    private fun disconnectFromPc(pc: PcDevice) {
        if (audioSelfId.isEmpty()) return
        ConnectionBus.transition(pc.deviceId, ConnectionEvent.LOCAL_DISCONNECT)
        ConnectionBus.localDisconnects.add(pc.deviceId)
    }

    // Core Command & Macros
    private fun sendCoreCommand(command: CoreCommand) {
        bluetoothViewModel.getKeyboardSenderDirect()?.let { sender ->
            Thread {
                when (command) {
                    CoreCommand.YES -> sender.sendText("y")
                    CoreCommand.YES_TO_ALL -> sender.sendText("a")
                    CoreCommand.NO -> sender.sendText("n")
                    CoreCommand.CTRL_C -> sender.sendKeyPress(KeyboardSender.MODIFIER_CTRL_LEFT, KeyboardSender.KEY_C)
                    CoreCommand.BACKSPACE -> sender.sendKeyPress(0x00, KeyboardSender.KEY_BACKSPACE)
                    CoreCommand.ENTER -> sender.sendKeyPress(0x00, KeyboardSender.KEY_ENTER)
                }
            }.start()
        }
    }

    private fun sendMacro(macro: Macro) {
        bluetoothViewModel.getKeyboardSenderDirect()?.let { sender ->
            Thread {
                if (macro.sendEnter) sender.sendMacro(macro.command) else sender.sendText(macro.command)
            }.start()
        }
    }

    // TV Remote action
    private fun sendTvRemoteAction(action: TvRemoteAction) {
        bluetoothViewModel.getTvRemoteSenderDirect()?.let { sender ->
            when (action) {
                TvRemoteAction.UP -> sender.sendUp()
                TvRemoteAction.DOWN -> sender.sendDown()
                TvRemoteAction.LEFT -> sender.sendLeft()
                TvRemoteAction.RIGHT -> sender.sendRight()
                TvRemoteAction.CONFIRM -> sender.sendConfirm()
                TvRemoteAction.BACK -> sender.sendBack()
                TvRemoteAction.ASSISTANT -> sender.sendAssistant()
                TvRemoteAction.HOME -> sender.sendHome()
                TvRemoteAction.MUTE -> sender.sendMute()
                TvRemoteAction.VOLUME_UP -> sender.sendVolumeUp()
                TvRemoteAction.VOLUME_DOWN -> sender.sendVolumeDown()
                TvRemoteAction.POWER -> sender.sendPower()
                TvRemoteAction.PLAY_PAUSE -> sender.sendPlayPause()
                TvRemoteAction.NEXT -> sender.sendNext()
                TvRemoteAction.PREVIOUS -> sender.sendPrevious()
                TvRemoteAction.STOP -> sender.sendStop()
            }
        }
    }
}
