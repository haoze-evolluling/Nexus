package com.haoze.nexus.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.macro.Macro

@Composable
fun MacroEditorAlertDialog(
    macro: Macro?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    var label by remember(macro) { mutableStateOf(macro?.label.orEmpty()) }
    var description by remember(macro) { mutableStateOf(macro?.description.orEmpty()) }
    var command by remember(macro) { mutableStateOf(macro?.command.orEmpty()) }
    var sendEnter by remember(macro) { mutableStateOf(macro?.sendEnter ?: false) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (macro == null) R.string.dialog_add_macro else R.string.dialog_edit_macro)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.dialog_macro_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.dialog_macro_description)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text(stringResource(R.string.dialog_macro_command)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.switch_send_enter), Modifier.weight(1f))
                    Switch(checked = sendEnter, onCheckedChange = { sendEnter = it })
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                onDelete?.let { delete ->
                    TextButton(onClick = delete) {
                        Text(stringResource(R.string.dialog_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
                TextButton(onClick = {
                    val trimmedLabel = label.trim()
                    val trimmedCommand = command.trim()
                    if (trimmedLabel.isNotEmpty() && trimmedCommand.isNotEmpty()) {
                        onSave(trimmedLabel, description.trim(), trimmedCommand, sendEnter)
                    } else {
                        onDismiss()
                    }
                }) { Text(stringResource(R.string.dialog_save)) }
            }
        }
    )
}
