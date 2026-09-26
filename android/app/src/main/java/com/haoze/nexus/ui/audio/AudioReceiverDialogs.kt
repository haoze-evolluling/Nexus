package com.haoze.nexus.ui.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.audio.AudioSettings
import com.haoze.nexus.audio.PcAuthPrompt
import com.haoze.nexus.audio.PcTrustRepository
import com.haoze.nexus.audio.SettingsRepository
import kotlinx.coroutines.launch

@Composable
internal fun AudioSettingsDialog(
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
internal fun PcAuthDialog(
    prompt: PcAuthPrompt,
    onRespond: (allow: Boolean, remember: Boolean) -> Unit
) {
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
