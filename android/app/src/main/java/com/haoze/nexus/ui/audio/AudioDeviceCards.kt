package com.haoze.nexus.ui.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.audio.AndroidDevice
import com.haoze.nexus.audio.ConnectionState
import com.haoze.nexus.audio.PcDevice
import com.haoze.nexus.audio.PeerCalibrationPhase
import com.haoze.nexus.audio.PeerCalibrationState

@Composable
internal fun EmptyDevices(modifier: Modifier = Modifier) {
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
internal fun PcCard(
    pc: PcDevice,
    state: ConnectionState,
    reconnectProgress: Pair<Int, Int>?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val connected = state == ConnectionState.CONNECTED
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
                        state.label(reconnectProgress),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (state) {
                ConnectionState.CONNECTED -> OutlinedButton(onClick = onDisconnect) { Text(stringResource(R.string.btn_disconnect)) }
                ConnectionState.CONNECTING, ConnectionState.AWAITING_AUTHORIZATION -> CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                ConnectionState.RECONNECTING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    OutlinedButton(onClick = onDisconnect) { Text(stringResource(R.string.btn_cancel_reconnect)) }
                }
                ConnectionState.FAILED -> Button(onClick = onConnect) { Text(stringResource(R.string.btn_reconnect)) }
                ConnectionState.IDLE, ConnectionState.DISCONNECTING -> Button(onClick = onConnect) { Text(stringResource(R.string.btn_connect)) }
            }
        }
    }
}

@Composable
internal fun AndroidDeviceCard(
    device: AndroidDevice,
    state: PeerCalibrationState,
    canStart: Boolean,
    onSync: () -> Unit
) {
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
internal fun StateDot(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        ConnectionState.CONNECTING, ConnectionState.AWAITING_AUTHORIZATION -> MaterialTheme.colorScheme.tertiary
        ConnectionState.RECONNECTING, ConnectionState.FAILED -> MaterialTheme.colorScheme.error
        ConnectionState.IDLE, ConnectionState.DISCONNECTING -> MaterialTheme.colorScheme.outline
    }
    Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
}

@Composable
internal fun ConnectionState.label(reconnectProgress: Pair<Int, Int>? = null): String = when (this) {
    ConnectionState.IDLE, ConnectionState.DISCONNECTING -> stringResource(R.string.pc_state_online)
    ConnectionState.CONNECTING -> stringResource(R.string.pc_state_connecting)
    ConnectionState.AWAITING_AUTHORIZATION -> stringResource(R.string.pc_state_awaiting_auth)
    ConnectionState.RECONNECTING -> {
        if (reconnectProgress != null) stringResource(R.string.pc_state_reconnecting, reconnectProgress.first, reconnectProgress.second)
        else stringResource(R.string.pc_state_connecting)
    }
    ConnectionState.FAILED -> stringResource(R.string.pc_state_failed)
    ConnectionState.CONNECTED -> stringResource(R.string.pc_state_connected)
}
