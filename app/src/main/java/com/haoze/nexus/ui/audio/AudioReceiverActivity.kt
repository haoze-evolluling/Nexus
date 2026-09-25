package com.haoze.nexus.ui.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.haoze.nexus.R
import com.haoze.nexus.audio.ActivePc
import com.haoze.nexus.audio.AndroidDevice
import com.haoze.nexus.audio.AudioReceiverService
import com.haoze.nexus.audio.AudioSettings
import com.haoze.nexus.audio.CalibrationPhase
import com.haoze.nexus.audio.CalibrationState
import com.haoze.nexus.audio.ConnectionBus
import com.haoze.nexus.audio.DeviceIdentity
import com.haoze.nexus.audio.LocaleManager
import com.haoze.nexus.audio.PcAuthPrompt
import com.haoze.nexus.audio.PcConnectionState
import com.haoze.nexus.audio.PcConnector
import com.haoze.nexus.audio.PcDevice
import com.haoze.nexus.audio.PcDiscovery
import com.haoze.nexus.audio.PcTrustRepository
import com.haoze.nexus.audio.PeerCalibrationPhase
import com.haoze.nexus.audio.PeerCalibrationState
import com.haoze.nexus.audio.SettingsRepository
import com.haoze.nexus.ui.compose.NexusTheme
import com.haoze.nexus.ui.compose.getThemeColorStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class AudioReceiverActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { startReceiver() }
    private val discovery by lazy { PcDiscovery(this) }
    private val connector = PcConnector()
    private val repository by lazy { SettingsRepository(applicationContext) }
    private val trustRepository by lazy { PcTrustRepository(applicationContext) }
    private var selfId by mutableStateOf("")
    private val selfName: String by lazy { DeviceIdentity.friendlyName(applicationContext) }
    private var receiverRunning by mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) { selfId = repository.settings.first().deviceId }
        setContent {
            NexusTheme(colorStyle = getThemeColorStyle(this)) {
                AudioReceiverScreen(
                    discovery = discovery,
                    connector = connector,
                    repository = repository,
                    trustRepository = trustRepository,
                    selfId = selfId,
                    selfName = selfName,
                    receiverRunning = receiverRunning,
                    onConnect = ::connectToPc,
                    onDisconnect = ::disconnectFromPc,
                    onBack = { finish() }
                )
            }
        }
        ensureReceiverRunning()
    }

    override fun onStart() {
        super.onStart()
        discovery.start()
    }

    override fun onStop() {
        super.onStop()
        discovery.stop()
    }

    private fun ensureReceiverRunning() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startReceiver()
    }

    private fun startReceiver() {
        ContextCompat.startForegroundService(this, Intent(this, AudioReceiverService::class.java))
        receiverRunning = true
    }

    private fun connectToPc(pc: PcDevice, onDone: (PcConnector.ConnectResult) -> Unit) {
        if (selfId.isEmpty()) return
        ensureReceiverRunning()
        val requestId = selfId
        Thread {
            val result = connector.request(pc, requestId, selfName)
            if (result is PcConnector.ConnectResult.Accepted) {
                runCatching {
                    ConnectionBus.queuedSender = ActivePc(pc.deviceId, pc.name, java.net.InetAddress.getByName(pc.host), nonce = result.nonce)
                }
            }
            runOnUiThread { onDone(result) }
        }.start()
    }

    private fun disconnectFromPc(pc: PcDevice) {
        if (selfId.isEmpty()) return
        ConnectionBus.localDisconnects.add(pc.deviceId)
    }
}

@Composable
fun AudioReceiverScreen(
    discovery: PcDiscovery,
    connector: PcConnector,
    repository: SettingsRepository,
    trustRepository: PcTrustRepository,
    selfId: String,
    selfName: String,
    receiverRunning: Boolean,
    onConnect: (PcDevice, (PcConnector.ConnectResult) -> Unit) -> Unit,
    onDisconnect: (PcDevice) -> Unit,
    onBack: () -> Unit
) {
    val devices by discovery.devices.collectAsState()
    val androidDevices by discovery.androidDevices.collectAsState()
    val activePc by ConnectionBus.activePc.collectAsState()
    val authPrompt by ConnectionBus.authPrompt.collectAsState()
    val calibration by ConnectionBus.calibration.collectAsState()
    val pcStates = remember { mutableStateMapOf<String, PcConnectionState>() }
    val peerCalibration by ConnectionBus.peerCalibration.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ConnectionBus.messages.collect { msg ->
            scope.launch { snackbar.showSnackbar(context.getString(msg.resId, *msg.args)) }
        }
    }
    LaunchedEffect(devices) {
        val online = devices.map { it.deviceId }.toSet()
        pcStates.keys.toList().forEach { if (it !in online) pcStates.remove(it) }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(stringResource(R.string.home_audio_stream_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            when {
                                activePc != null -> stringResource(R.string.receiver_active, activePc?.name ?: "")
                                receiverRunning -> stringResource(R.string.receiver_idle)
                                else -> stringResource(R.string.receiver_starting)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                val calib = calibration
                AnimatedVisibility(
                    visible = calib != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    if (calib != null) {
                        CalibrationPanel(state = calib, done = calib.phase == CalibrationPhase.DONE)
                    }
                }
                val visibleAndroidDevices = androidDevices.filter { it.deviceId != selfId }
                if (devices.isEmpty() && visibleAndroidDevices.isEmpty()) {
                    EmptyDevices(Modifier.weight(1f))
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        if (devices.isNotEmpty()) item {
                            Row(
                                Modifier.padding(start = 20.dp, top = 4.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(R.string.nearby_pcs), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(
                                    pluralStringResource(R.plurals.device_count, devices.size, devices.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 20.dp),
                                )
                            }
                        }
                        items(devices, key = { it.deviceId }) { pc ->
                            val effective = if (activePc?.deviceId == pc.deviceId) PcConnectionState.CONNECTED else pcStates[pc.deviceId] ?: PcConnectionState.ONLINE
                            PcCard(
                                pc = pc,
                                state = effective,
                                onConnect = {
                                    pcStates[pc.deviceId] = PcConnectionState.CONNECTING
                                    onConnect(pc) { result ->
                                        when (result) {
                                            is PcConnector.ConnectResult.Accepted -> pcStates.remove(pc.deviceId)
                                            is PcConnector.ConnectResult.Denied -> {
                                                pcStates.remove(pc.deviceId)
                                                scope.launch { snackbar.showSnackbar(context.getString(R.string.pc_denied)) }
                                            }
                                            is PcConnector.ConnectResult.Timeout -> {
                                                pcStates.remove(pc.deviceId)
                                                scope.launch { snackbar.showSnackbar(context.getString(R.string.pc_connect_failed, pc.name)) }
                                            }
                                        }
                                    }
                                },
                                onDisconnect = {
                                    pcStates.remove(pc.deviceId)
                                    onDisconnect(pc)
                                },
                            )
                        }
                        if (visibleAndroidDevices.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.nearby_android_devices),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 2.dp),
                                )
                            }
                            items(visibleAndroidDevices, key = { it.deviceId }) { device ->
                                AndroidDeviceCard(device, peerCalibration[device.deviceId] ?: PeerCalibrationState(), activePc != null) {
                                    ConnectionBus.peerCalibrationRequests.add(device)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    authPrompt?.let { prompt ->
        PcAuthDialog(
            prompt = prompt,
            onRespond = { allow, remember ->
                ConnectionBus.decisions.add(Triple(prompt.requestId, allow, remember))
            },
        )
    }

    if (showSettingsDialog) {
        AudioSettingsDialog(
            repository = repository,
            trustRepository = trustRepository,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun AudioSettingsDialog(
    repository: SettingsRepository,
    trustRepository: PcTrustRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by repository.settings.collectAsState(initial = AudioSettings())
    val trustedPcs by trustRepository.trusted.collectAsState(initial = emptyMap())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bitrate
                Column {
                    Text(stringResource(R.string.settings_bitrate_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_bitrate_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    listOf(64, 96, 128, 192).forEach { bitrate ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = bitrate == settings.initialBitrateKbps,
                                onClick = { scope.launch { repository.setInitialBitrate(bitrate) } }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("$bitrate kbps" + if (bitrate == 128) " (${stringResource(R.string.bitrate_recommended)})" else "", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Frame ms
                Column {
                    Text(stringResource(R.string.settings_frame_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.settings_frame_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    listOf(10, 20).forEach { frame ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = frame == settings.frameMs,
                                onClick = { scope.launch { repository.setFrameMs(frame) } }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("$frame ms (${if (frame == 10) stringResource(R.string.frame_10_note) else stringResource(R.string.frame_20_note)})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Trusted PCs
                Column {
                    Text(stringResource(R.string.settings_trusted_title), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    if (trustedPcs.isEmpty()) {
                        Text(stringResource(R.string.settings_trusted_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        trustedPcs.forEach { (id, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name.ifBlank { id.take(12) }, style = MaterialTheme.typography.bodyMedium)
                                    Text(id.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { scope.launch { trustRepository.untrust(id) } }) {
                                    Text(stringResource(R.string.btn_remove))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_confirm))
            }
        }
    )
}

@Composable
private fun calibrationStepLabels(): List<String> = listOf(
    stringResource(R.string.calib_step_detect),
    stringResource(R.string.calib_step_calculate),
    stringResource(R.string.calib_step_sync),
    stringResource(R.string.calib_step_done),
)

@Composable
private fun CalibrationPhase.label(): String = when (this) {
    CalibrationPhase.DETECT -> stringResource(R.string.calib_phase_detect)
    CalibrationPhase.CALCULATE -> stringResource(R.string.calib_phase_calculate)
    CalibrationPhase.SYNC -> stringResource(R.string.calib_phase_sync)
    CalibrationPhase.DONE -> stringResource(R.string.calib_phase_done)
}

@Composable
private fun CalibrationPanel(state: CalibrationState, done: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalibrationWave(Modifier.size(width = 64.dp, height = 28.dp))
                Spacer(Modifier.width(14.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (done) stringResource(R.string.calib_synced) else stringResource(R.string.calibrating),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                    val stats = if (done || state.phase == CalibrationPhase.SYNC) {
                        val offset = state.offsetMs
                        val rtt = state.rttMs
                        if (offset != null && rtt != null) stringResource(R.string.calib_stats, abs(offset), rtt) else null
                    } else null
                    Text(
                        stats ?: state.phase.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            CalibrationSteps(state.phase)
        }
    }
}

@Composable
private fun CalibrationWave(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "calib-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "calib-wave-phase",
    )
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val bars = 12
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (bars - 1)) / bars
        val mid = size.height / 2f
        for (i in 0 until bars) {
            val level = abs(sin(phase + i * 0.55f)) * 0.85f + 0.15f
            val h = size.height * level * 0.9f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(i * (barWidth + gap), mid - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
private fun CalibrationSteps(current: CalibrationPhase) {
    val activeIndex = when (current) {
        CalibrationPhase.DETECT -> 0
        CalibrationPhase.CALCULATE -> 1
        CalibrationPhase.SYNC -> 2
        CalibrationPhase.DONE -> 3
    }
    val pulse = rememberInfiniteTransition(label = "step-pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "step-pulse-scale",
    )
    val connectorColor = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val nodeSpacing = size.width / 4f
            val connectorY = size.height / 2f
            for (index in 1 until 4) {
                drawLine(
                    color = connectorColor.copy(alpha = if (index <= activeIndex) 0.7f else 0.25f),
                    start = Offset(nodeSpacing * (index - 0.5f), connectorY),
                    end = Offset(nodeSpacing * (index + 0.5f), connectorY),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            calibrationStepLabels().forEachIndexed { index, label ->
                val active = index == activeIndex
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (index <= activeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(10.dp).then(if (active) Modifier.scale(scale) else Modifier),
                    ) {}
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index <= activeIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PcAuthDialog(prompt: PcAuthPrompt, onRespond: (allow: Boolean, remember: Boolean) -> Unit) {
    var remember by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = { onRespond(false, false) },
        title = { Text(stringResource(R.string.auth_title)) },
        text = {
            Column {
                Text(stringResource(R.string.auth_message, prompt.name), style = MaterialTheme.typography.bodyMedium)
                Text(prompt.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remember, onCheckedChange = { remember = it })
                    Text(stringResource(R.string.auth_remember), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onRespond(true, remember) }) { Text(stringResource(R.string.auth_allow)) } },
        dismissButton = { TextButton(onClick = { onRespond(false, false) }) { Text(stringResource(R.string.auth_deny)) } },
    )
}

@Composable
private fun EmptyDevices(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Computer, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(stringResource(R.string.searching_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.searching_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            CircularProgressIndicator(Modifier.size(20.dp).padding(top = 8.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun PcCard(pc: PcDevice, state: PcConnectionState, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    val connected = state == PcConnectionState.CONNECTED
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Computer, null, tint = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.padding(horizontal = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(pc.name, style = MaterialTheme.typography.titleMedium)
                Text(pc.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StateDot(state)
                    Text(
                        state.label(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (state) {
                PcConnectionState.CONNECTED -> OutlinedButton(onClick = onDisconnect) { Text(stringResource(R.string.btn_disconnect)) }
                PcConnectionState.CONNECTING -> CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                PcConnectionState.ONLINE -> Button(onClick = onConnect) { Text(stringResource(R.string.btn_connect)) }
            }
        }
    }
}

@Composable
private fun AndroidDeviceCard(device: AndroidDevice, state: PeerCalibrationState, canStart: Boolean, onSync: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (state.phase == PeerCalibrationPhase.COMPLETE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.padding(horizontal = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text(device.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (state.phase) {
                    PeerCalibrationPhase.IDLE -> Text(stringResource(R.string.android_sync_ready), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PeerCalibrationPhase.REQUESTING, PeerCalibrationPhase.MEASURING, PeerCalibrationPhase.WAITING_TARGET -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp); Text(stringResource(R.string.android_sync_running), style = MaterialTheme.typography.labelMedium) }
                    PeerCalibrationPhase.FAILED -> Text(stringResource(R.string.android_sync_failed), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    PeerCalibrationPhase.COMPLETE -> Text(
                        if (state.rttMs != null) stringResource(R.string.android_sync_result, state.rttMs)
                        else stringResource(R.string.android_sync_complete),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val calibrating = state.phase in setOf(
                PeerCalibrationPhase.REQUESTING,
                PeerCalibrationPhase.MEASURING,
                PeerCalibrationPhase.WAITING_TARGET,
            )
            Button(onClick = onSync, enabled = canStart && !calibrating) {
                if (calibrating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.android_sync_action))
            }
        }
    }
}

@Composable
private fun StateDot(state: PcConnectionState) {
    val color = when (state) {
        PcConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        PcConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
        PcConnectionState.ONLINE -> MaterialTheme.colorScheme.outline
    }
    Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
}

@Composable
private fun PcConnectionState.label(): String = when (this) {
    PcConnectionState.ONLINE -> stringResource(R.string.pc_state_online)
    PcConnectionState.CONNECTING -> stringResource(R.string.pc_state_connecting)
    PcConnectionState.CONNECTED -> stringResource(R.string.pc_state_connected)
}
