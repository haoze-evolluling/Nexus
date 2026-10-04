package com.haoze.nexus.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.ui.AppLanguageManager
import com.haoze.nexus.ui.AppLanguageMode

@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    onLanguageChanged: (AppLanguageMode) -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(AppLanguageManager.getMode(context)) }

    SettingsScaffold(
        title = stringResource(R.string.language_settings),
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.language_settings_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard {
                AppLanguageMode.entries.forEachIndexed { index, mode ->
                    if (index > 0) {
                        SettingsItemDivider()
                    }
                    SettingsRadioItem(
                        title = stringResource(mode.labelRes),
                        selected = selectedMode == mode,
                        onClick = {
                            if (selectedMode != mode) {
                                selectedMode = mode
                                onLanguageChanged(mode)
                            }
                        }
                    )
                }
            }
        }
    }
}
