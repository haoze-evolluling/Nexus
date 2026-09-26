package com.haoze.nexus.ui.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.audio.CalibrationPhase
import com.haoze.nexus.audio.ConnectionBus
import com.haoze.nexus.audio.ConnectionState
import com.haoze.nexus.audio.PcConnector
import com.haoze.nexus.audio.PcDevice
import com.haoze.nexus.audio.PcDiscovery
import com.haoze.nexus.audio.PcTrustRepository
import com.haoze.nexus.audio.PeerCalibrationState
import com.haoze.nexus.audio.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun AudioReceiverScreen(
    discovery: PcDiscovery,
    connector: PcConnector,
    repository: SettingsRepository,
    trustRepository: PcTrustRepository,
    selfId: String,
    selfName: String,
    receiverRunning: Boolean,
    onConnect: (PcDevice) -> Unit,
    onDisconnect: (PcDevice) -> Unit,
    onBack: () -> Unit,
    showBackIcon: Boolean = true,
    contentBottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val devices by discovery.devices.collectAsState()
    val androidDevices by discovery.androidDevices.collectAsState()
    val activePc by ConnectionBus.activePc.collectAsState()
    val authPrompt by ConnectionBus.authPrompt.collectAsState()
    val calibration by ConnectionBus.calibration.collectAsState()
    val reconnectProgress by ConnectionBus.reconnectProgress.collectAsState()
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
                    if (showBackIcon) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Column(modifier = if (!showBackIcon) Modifier.padding(start = 8.dp) else Modifier) {
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
                    EmptyDevices(Modifier.weight(1f).padding(bottom = contentBottomPadding))
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp + contentBottomPadding),
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
                            val stateFlow = remember(pc.deviceId) { ConnectionBus.stateOf(pc.deviceId) }
                            val rawState by stateFlow.collectAsState()
                            val effective = if (activePc?.deviceId == pc.deviceId) ConnectionState.CONNECTED else rawState
                            val progress = reconnectProgress[pc.deviceId]
                            PcCard(
                                pc = pc,
                                state = effective,
                                reconnectProgress = progress,
                                onConnect = { onConnect(pc) },
                                onDisconnect = { onDisconnect(pc) },
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
