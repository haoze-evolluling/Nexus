package com.haoze.nexus.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TvRemoteScreen(
    enabled: Boolean,
    onBack: () -> Unit,
    onAction: (TvRemoteAction) -> Unit,
    showBackIcon: Boolean = true,
    contentBottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val colors = rememberTvRemoteColors()
    val scope = rememberCoroutineScope()
    var ledActive by remember { mutableStateOf(false) }

    fun runAction(action: TvRemoteAction) {
        if (!enabled) return
        ledActive = true
        onAction(action)
        scope.launch {
            delay(150)
            ledActive = false
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.home_tvremote_title),
        onBack = onBack,
        showBackIcon = showBackIcon
    ) { innerPadding ->
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp + contentBottomPadding),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, colors.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TvRemoteTopRow(
                    colors = colors,
                    ledActive = ledActive,
                    enabled = enabled,
                    onPower = { runAction(TvRemoteAction.POWER) }
                )
                TvRemoteDpad(
                    colors = colors,
                    enabled = enabled,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .size(212.dp),
                    onAction = ::runAction
                )
                RemoteTwoButtonRow(
                    modifier = Modifier.padding(top = 20.dp),
                    colors = colors,
                    enabled = enabled,
                    left = RemoteButtonSpec(
                        label = stringResource(R.string.tvremote_back),
                        iconRes = R.drawable.ic_arrow_back,
                        iconBackground = colors.primaryContainer,
                        iconTint = colors.onPrimaryContainer,
                        onClick = { runAction(TvRemoteAction.BACK) }
                    ),
                    right = RemoteButtonSpec(
                        label = stringResource(R.string.tvremote_assistant),
                        iconRes = R.drawable.ic_assistant,
                        iconBackground = colors.tertiaryContainer,
                        iconTint = colors.onTertiaryContainer,
                        onClick = { runAction(TvRemoteAction.ASSISTANT) }
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.width(130.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RemoteCapsuleButton(
                            spec = RemoteButtonSpec(
                                label = stringResource(R.string.tvremote_home),
                                iconRes = R.drawable.ic_home,
                                iconBackground = colors.primaryContainer,
                                iconTint = colors.onPrimaryContainer,
                                onClick = { runAction(TvRemoteAction.HOME) }
                            ),
                            colors = colors,
                            enabled = enabled
                        )
                        Spacer(Modifier.height(18.dp))
                        RemoteCapsuleButton(
                            spec = RemoteButtonSpec(
                                label = stringResource(R.string.tvremote_mute),
                                iconRes = R.drawable.ic_volume_off,
                                iconBackground = colors.secondaryContainer,
                                iconTint = colors.onSecondaryContainer,
                                onClick = { runAction(TvRemoteAction.MUTE) }
                            ),
                            colors = colors,
                            enabled = enabled
                        )
                    }
                    Spacer(Modifier.width(28.dp))
                    VolumeStack(colors = colors, enabled = enabled, onAction = ::runAction)
                }
                MediaControlRow(
                    colors = colors,
                    enabled = enabled,
                    modifier = Modifier.padding(top = 20.dp),
                    onAction = ::runAction
                )
            }
        }
    }
}
