package com.haoze.nexus.ui.compose

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import kotlinx.coroutines.delay

@Composable
fun DeviceListAlertDialog(
    devices: List<BluetoothDevice>,
    permissionDenied: Boolean,
    connectingAddress: String?,
    connectedAddress: String?,
    lastConnectedAddress: String?,
    onDismiss: () -> Unit,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onConnectionTimeout: () -> Unit
) {
    LaunchedEffect(connectingAddress) {
        if (connectingAddress != null) {
            delay(10_000L)
            onConnectionTimeout()
        }
    }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.device_list_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.device_list_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when {
                    permissionDenied -> DeviceListMessage(stringResource(R.string.toast_permission_denied))
                    devices.isEmpty() -> DeviceListMessage(stringResource(R.string.device_no_paired))
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        itemsIndexed(devices, key = { _, device -> device.address }) { index, device ->
                            DeviceListItem(
                                device = device,
                                colorIndex = index,
                                connectingAddress = connectingAddress,
                                connectedAddress = connectedAddress,
                                lastConnectedAddress = lastConnectedAddress,
                                onClick = { onDeviceSelected(device) },
                                onDisconnect = onDisconnect
                            )
                            if (index < devices.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun DeviceListMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DeviceListItem(
    device: BluetoothDevice,
    colorIndex: Int,
    connectingAddress: String?,
    connectedAddress: String?,
    lastConnectedAddress: String?,
    onClick: () -> Unit,
    onDisconnect: () -> Unit
) {
    val isConnecting = device.address == connectingAddress
    val isConnected = device.address == connectedAddress
    val status = when {
        isConnecting -> stringResource(R.string.device_connecting)
        isConnected -> stringResource(R.string.status_connected_label)
        device.address == lastConnectedAddress -> stringResource(R.string.device_last_connected)
        else -> null
    }
    val canSelect = !isConnected && connectingAddress == null
    val interactionSource = remember { MutableInteractionSource() }
    val bluetoothColors = monetBluetoothColors(colorIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = canSelect,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(bluetoothColors.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = bluetoothColors.foreground
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name ?: stringResource(R.string.status_unknown_device),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    status?.let {
                        Text(
                            text = it,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = device.address,
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isConnected) {
                TextButton(onClick = onDisconnect) {
                    Text(stringResource(R.string.btn_disconnect), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (isConnecting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }
    }
}

private data class BluetoothIconColors(
    val background: Color,
    val foreground: Color
)

private fun monetBluetoothColors(index: Int): BluetoothIconColors {
    val palette = listOf(
        BluetoothIconColors(Color(0xFFD7E8ED), Color(0xFF245A6D)), // Water lily blue
        BluetoothIconColors(Color(0xFFE3DCEA), Color(0xFF57416D)), // Iris violet
        BluetoothIconColors(Color(0xFFDCE9DB), Color(0xFF365B42)), // Garden green
        BluetoothIconColors(Color(0xFFEEDBDD), Color(0xFF713E4B)), // Rose garden
        BluetoothIconColors(Color(0xFFF0E7C9), Color(0xFF6A5725)), // Sunlit haystack
        BluetoothIconColors(Color(0xFFDDE9E7), Color(0xFF2C5D58)), // Morning mist
        BluetoothIconColors(Color(0xFFEADBD3), Color(0xFF70483B)), // Warm reflection
        BluetoothIconColors(Color(0xFFDCE3F0), Color(0xFF354C78))  // Evening sky
    )
    return palette[index % palette.size]
}

@Composable
fun ConnectionTimeoutAlertDialog(onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_connect_timeout_title)) },
        text = { Text(stringResource(R.string.dialog_connect_timeout_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) }
        }
    )
}
