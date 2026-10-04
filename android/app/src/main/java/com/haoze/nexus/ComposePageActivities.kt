package com.haoze.nexus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.haoze.nexus.bluetooth.BluetoothViewModel
import com.haoze.nexus.bluetooth.HidProfile
import com.haoze.nexus.bluetooth.KeyboardSender
import com.haoze.nexus.macro.Macro
import com.haoze.nexus.macro.MacroRepository
import com.haoze.nexus.ui.Routes
import com.haoze.nexus.ui.compose.AboutSettingsScreen
import com.haoze.nexus.ui.compose.BottomBarCustomizationScreen
import com.haoze.nexus.ui.compose.AgentScreen
import com.haoze.nexus.ui.compose.CoreCommand
import com.haoze.nexus.ui.compose.ConnectionSettingsScreen
import com.haoze.nexus.ui.compose.DataSettingsScreen
import com.haoze.nexus.ui.compose.DayNightModeScreen
import com.haoze.nexus.ui.compose.FeedbackSettingsScreen
import com.haoze.nexus.ui.compose.InputSettingsScreen
import com.haoze.nexus.ui.compose.MacroEditorAlertDialog
import com.haoze.nexus.ui.AppLanguageManager
import com.haoze.nexus.ui.compose.LanguageSettingsScreen
import com.haoze.nexus.ui.compose.AppearanceSettingsScreen
import com.haoze.nexus.ui.compose.SettingsScreen
import com.haoze.nexus.ui.compose.SponsorListScreen
import com.haoze.nexus.ui.compose.SponsorSettingsScreen
import com.haoze.nexus.ui.compose.NexusConfirmationDialog
import com.haoze.nexus.ui.compose.NexusTheme
import com.haoze.nexus.ui.compose.ThemeColorSettingsScreen
import com.haoze.nexus.ui.compose.TvRemoteAction
import com.haoze.nexus.ui.compose.TvRemoteScreen
import com.haoze.nexus.ui.compose.getThemeColorStyle

abstract class ComposePageActivity : AppLocalizedActivity() {
    protected val bluetoothViewModel: BluetoothViewModel by viewModels()
    protected var isConnected by mutableStateOf(false)
    protected var connectedDeviceName by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bluetoothViewModel.startAndBindService()
        bluetoothViewModel.connectionState.observe(this) { isConnected = it }
        bluetoothViewModel.connectedDeviceName.observe(this) { connectedDeviceName = it }
    }

    protected fun setComposeContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        setContent(content = content)
    }
}

class TvRemoteActivity : ComposePageActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent {
            NexusTheme(colorStyle = getThemeColorStyle(this@TvRemoteActivity)) {
                TvRemoteScreen(isConnected, ::finish, ::sendAction)
            }
        }
    }

    private fun sendAction(action: TvRemoteAction) {
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

class AgentActivity : ComposePageActivity() {
    private lateinit var macroRepository: MacroRepository
    private var macros by mutableStateOf<List<Macro>>(emptyList())
    private var showMacroEditor by mutableStateOf(false)
    private var editingMacro by mutableStateOf<Macro?>(null)
    private var pendingDeleteMacroId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        macroRepository = MacroRepository(this)
        loadMacros()
        setComposeContent {
            NexusTheme(colorStyle = getThemeColorStyle(this@AgentActivity)) {
                AgentScreen(
                    isConnected, connectedDeviceName, macros, ::finish, ::sendCoreCommand, ::sendMacro,
                    onMacroLongClick = { macro -> editingMacro = macro; showMacroEditor = true },
                    onAddMacro = { editingMacro = null; showMacroEditor = true }
                )
                if (showMacroEditor) {
                    MacroEditorAlertDialog(
                        macro = editingMacro,
                        onDismiss = { showMacroEditor = false },
                        onSave = { label, description, command, sendEnter ->
                            saveMacro(editingMacro?.id, label, description, command, sendEnter)
                            showMacroEditor = false
                        },
                        onDelete = editingMacro?.id?.let { id -> {
                            showMacroEditor = false
                            pendingDeleteMacroId = id
                        } }
                    )
                }
                pendingDeleteMacroId?.let { id ->
                    NexusConfirmationDialog(
                        getString(R.string.dialog_delete_macro),
                        getString(R.string.dialog_delete_macro_message),
                        getString(R.string.dialog_delete),
                        true,
                        { macroRepository.deleteCustomMacro(id); pendingDeleteMacroId = null; loadMacros() },
                        { pendingDeleteMacroId = null }
                    )
                }
            }
        }
    }

    private fun sendCoreCommand(command: CoreCommand) {
        bluetoothViewModel.getKeyboardSenderDirect()?.let { sender -> Thread {
            when (command) {
                CoreCommand.YES -> sender.sendText("y")
                CoreCommand.YES_TO_ALL -> sender.sendText("a")
                CoreCommand.NO -> sender.sendText("n")
                CoreCommand.CTRL_C -> sender.sendKeyPress(KeyboardSender.MODIFIER_CTRL_LEFT, KeyboardSender.KEY_C)
                CoreCommand.BACKSPACE -> sender.sendKeyPress(0x00, KeyboardSender.KEY_BACKSPACE)
                CoreCommand.ENTER -> sender.sendKeyPress(0x00, KeyboardSender.KEY_ENTER)
            }
        }.start() }
    }

    private fun sendMacro(macro: Macro) {
        bluetoothViewModel.getKeyboardSenderDirect()?.let { sender -> Thread {
            if (macro.sendEnter) sender.sendMacro(macro.command) else sender.sendText(macro.command)
        }.start() }
    }

    private fun loadMacros() { macros = macroRepository.getAllMacros() }

    private fun saveMacro(id: String?, label: String, description: String, command: String, sendEnter: Boolean) {
        if (id == null) {
            macroRepository.addCustomMacro(label, description, command, sendEnter)
            Toast.makeText(this, R.string.toast_macro_added, Toast.LENGTH_SHORT).show()
        } else {
            macroRepository.updateCustomMacro(id, label, description, command, sendEnter)
            Toast.makeText(this, R.string.toast_macro_updated, Toast.LENGTH_SHORT).show()
        }
        loadMacros()
    }
}

/**
 * 设置路由宿主：与 DITING 的 SettingsRouteActivity 一致，
 * 每个设置页面由独立的 SettingsActivity 实例承载，子页面通过 createIntent 压栈。
 */
class SettingsActivity : ComposePageActivity() {
    private val route: String
        get() = intent.getStringExtra(EXTRA_ROUTE) ?: Routes.SETTINGS

    private lateinit var macroRepository: MacroRepository
    private var colorStyleState by mutableStateOf(com.haoze.nexus.ui.compose.ThemeColorStyle.SYSTEM)
    private var inputProfileState by mutableStateOf(HidProfile.DEFAULT)
    private var childLaunchInProgress = false

    private val childActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        childLaunchInProgress = false
        // 子页面可能修改了主题色，返回后同步到当前页面
        colorStyleState = getThemeColorStyle(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        macroRepository = MacroRepository(this)
        colorStyleState = getThemeColorStyle(this)
        bluetoothViewModel.inputProfile.observe(this) { inputProfileState = it }
        setComposeContent {
            NexusTheme(colorStyle = colorStyleState) {
                SettingsRouteContent(
                    route = route,
                    onBack = ::finish,
                    onNavigate = ::openRoute,
                    onBooleanSettingChanged = ::onBooleanSettingChanged,
                    onResetMacros = { macroRepository.resetToDefaults() },
                    onThemeColorStyleChanged = { style -> colorStyleState = style }
                )
            }
        }
    }

    private fun openRoute(nextRoute: String) {
        if (childLaunchInProgress || nextRoute == route) return
        childLaunchInProgress = true
        childActivityLauncher.launch(createIntent(this, nextRoute))
    }

    private fun onBooleanSettingChanged(key: String, value: Boolean) {
        if (key == "connection_notifications" && !value) bluetoothViewModel.dismissConnectionNotification()
    }

    @androidx.compose.runtime.Composable
    private fun SettingsRouteContent(
        route: String,
        onBack: () -> Unit,
        onNavigate: (String) -> Unit,
        onBooleanSettingChanged: (String, Boolean) -> Unit,
        onResetMacros: () -> Unit,
        onThemeColorStyleChanged: (com.haoze.nexus.ui.compose.ThemeColorStyle) -> Unit
    ) {
        when (route) {
            Routes.SETTINGS -> SettingsScreen(
                onBack = onBack,
                onNavigateToRoute = onNavigate,
                onThemeColorStyleChanged = onThemeColorStyleChanged,
                onBooleanSettingChanged = onBooleanSettingChanged,
                inputProfile = inputProfileState,
                onInputProfileChanged = { profile -> bluetoothViewModel.setInputProfile(profile) },
                onResetMacros = onResetMacros
            )
            Routes.APPEARANCE_SETTINGS -> AppearanceSettingsScreen(onBack, onNavigate)
            Routes.LANGUAGE_SETTINGS -> LanguageSettingsScreen(
                onBack = onBack,
                onLanguageChanged = { mode ->
                    AppLanguageManager.setMode(this@SettingsActivity, mode)
                    recreate()
                }
            )
            Routes.BOTTOM_BAR_CUSTOMIZATION -> BottomBarCustomizationScreen(onBack)
            Routes.DAY_NIGHT_MODE -> DayNightModeScreen(onBack)
            Routes.THEME_COLOR_SETTINGS -> ThemeColorSettingsScreen(onBack, onThemeColorStyleChanged)
            Routes.INPUT_SETTINGS -> InputSettingsScreen(onBack)
            Routes.FEEDBACK_SETTINGS -> FeedbackSettingsScreen(onBack)
            Routes.CONNECTION_SETTINGS -> ConnectionSettingsScreen(
                onBack,
                onBooleanSettingChanged,
                inputProfile = inputProfileState,
                onInputProfileChanged = { profile -> bluetoothViewModel.setInputProfile(profile) }
            )
            Routes.DATA_SETTINGS -> DataSettingsScreen(onBack, onResetMacros)
            Routes.ABOUT -> {
                val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
                AboutSettingsScreen(onBack, versionName, isConnected)
            }
            Routes.SPONSOR -> SponsorSettingsScreen(onBack)
            Routes.SPONSOR_LIST -> SponsorListScreen(onBack)
            else -> SettingsScreen(
                onBack = onBack,
                onNavigateToRoute = onNavigate,
                onThemeColorStyleChanged = onThemeColorStyleChanged,
                onBooleanSettingChanged = onBooleanSettingChanged,
                inputProfile = inputProfileState,
                onInputProfileChanged = { profile -> bluetoothViewModel.setInputProfile(profile) },
                onResetMacros = onResetMacros
            )
        }
    }

    companion object {
        const val EXTRA_ROUTE = "settings_route"

        fun createIntent(context: Context, route: String): Intent =
            Intent(context, SettingsActivity::class.java).putExtra(EXTRA_ROUTE, route)
    }
}
