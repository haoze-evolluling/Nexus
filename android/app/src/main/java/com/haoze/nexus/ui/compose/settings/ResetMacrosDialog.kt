package com.haoze.nexus.ui.compose.settings

import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.haoze.nexus.R
import com.haoze.nexus.ui.compose.AppAlertDialog

/**
 * 恢复默认快捷命令确认弹窗
 */
@Composable
fun ResetMacrosConfirmDialog(
    onDismissRequest: () -> Unit,
    onResetConfirmed: () -> Unit
) {
    val context = LocalContext.current
    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.settings_reset_macros)) },
        text = { Text(stringResource(R.string.dialog_reset_macros_confirm)) },
        confirmButton = {
            TextButton(onClick = {
                onResetConfirmed()
                onDismissRequest()
                Toast.makeText(context, "已恢复默认快捷命令", Toast.LENGTH_SHORT).show()
            }) {
                Text(stringResource(R.string.dialog_reset), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
