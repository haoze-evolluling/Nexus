package com.haoze.nexus.ui.compose.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Troubleshoot
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.haoze.nexus.R
import com.haoze.nexus.crash.CrashLogManager
import com.haoze.nexus.ui.compose.AppAlertDialog
import com.haoze.nexus.ui.compose.SettingsActionItem
import com.haoze.nexus.ui.compose.SettingsCard
import com.haoze.nexus.ui.compose.SettingsInfoText
import com.haoze.nexus.ui.compose.SettingsItemDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设置主页 / 数据设置 - 崩溃日志配置与导出卡片
 */
@Composable
fun CrashLogSettingsCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var crashLogCount by remember { mutableIntStateOf(CrashLogManager.getCrashLogCount(context)) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val noContentMessage = stringResource(R.string.crash_export_no_content)
    val openErrorMessage = stringResource(R.string.crash_export_open_error)
    val exportSuccessMessage = stringResource(R.string.crash_export_success)
    val exportFailedPrefix = stringResource(R.string.crash_export_failed)

    val crashExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val content = CrashLogManager.generateExportContent(context)
                        if (content.isEmpty()) error(noContentMessage)
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
                            ?: error(openErrorMessage)
                    }
                }
                if (result.isSuccess) {
                    CrashLogManager.markManuallyExported(context)
                    Toast.makeText(context, exportSuccessMessage, Toast.LENGTH_SHORT).show()
                } else {
                    val err = result.exceptionOrNull()?.message.orEmpty()
                    Toast.makeText(context, String.format(exportFailedPrefix, err), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SettingsCard(modifier = modifier) {
        SettingsActionItem(
            title = stringResource(R.string.crash_export_title),
            subtitle = if (crashLogCount > 0) {
                stringResource(R.string.crash_export_subtitle_count, crashLogCount)
            } else {
                stringResource(R.string.crash_export_subtitle_empty)
            },
            leadingIcon = Icons.Filled.Troubleshoot,
            enabled = crashLogCount > 0,
            onClick = {
                if (crashLogCount <= 0) {
                    Toast.makeText(context, R.string.crash_export_no_logs, Toast.LENGTH_SHORT).show()
                } else {
                    val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    crashExportLauncher.launch("Nexus-crash-$date.txt")
                }
            }
        )

        if (crashLogCount > 0) {
            SettingsItemDivider()

            SettingsActionItem(
                title = stringResource(R.string.crash_clear_title),
                subtitle = stringResource(R.string.crash_clear_subtitle),
                leadingIcon = Icons.Filled.Delete,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showClearConfirmDialog = true }
            )
        }
    }

    SettingsInfoText(stringResource(R.string.crash_section_info))

    if (showClearConfirmDialog) {
        AppAlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.crash_clear_dialog_title)) },
            text = { Text(stringResource(R.string.crash_clear_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        CrashLogManager.clearCrashLogs(context)
                        crashLogCount = 0
                        Toast.makeText(context, R.string.crash_clear_success, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}
