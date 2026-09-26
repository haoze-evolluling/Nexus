package com.haoze.nexus.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.macro.Macro

@Composable
fun AgentScreen(
    isConnected: Boolean,
    connectedDeviceName: String?,
    macros: List<Macro>,
    onBack: () -> Unit,
    onCoreCommand: (CoreCommand) -> Unit,
    onMacroClick: (Macro) -> Unit,
    onMacroLongClick: (Macro) -> Unit,
    onAddMacro: () -> Unit,
    showBackIcon: Boolean = true,
    contentBottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    SettingsScaffold(
        title = stringResource(R.string.home_agent_title),
        onBack = onBack,
        showBackIcon = showBackIcon
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp + contentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSectionSpacing)
        ) {
            item { SettingsGroupTitle(stringResource(R.string.home_connection_status)) }
            item {
                SettingsSurfaceGroup(
                    content = listOf {
                        SettingsItem(
                            title = stringResource(R.string.home_connection_status),
                            subtitle = connectionStatusText(isConnected, connectedDeviceName),
                            leadingIcon = Icons.Default.Bluetooth
                        )
                    }
                )
            }

            item { SettingsGroupTitle(stringResource(R.string.home_agent_title)) }
            item {
                SettingsSurfaceGroup(
                    content = listOf {
                        CoreCommandGrid(onCoreCommand = onCoreCommand)
                    }
                )
            }

            item { SettingsGroupTitle(stringResource(R.string.macro_list_title)) }
            item {
                SettingsSurfaceGroup(
                    content = buildList {
                        macros.forEach { macro ->
                            add {
                                MacroSettingsItem(
                                    macro = macro,
                                    onClick = { onMacroClick(macro) },
                                    onLongClick = { if (!macro.isPreset) onMacroLongClick(macro) }
                                )
                            }
                        }
                        add {
                            SettingsTextItem(
                                title = stringResource(R.string.btn_add_macro),
                                subtitle = stringResource(R.string.macro_long_press_hint),
                                textColor = MaterialTheme.colorScheme.primary,
                                onClick = onAddMacro
                            )
                        }
                    }
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

private data class CoreCommandSpec(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val command: CoreCommand
)

@Composable
private fun CoreCommandGrid(
    onCoreCommand: (CoreCommand) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val commands = listOf(
        CoreCommandSpec(stringResource(R.string.btn_yes), Icons.Default.Check, primary, CoreCommand.YES),
        CoreCommandSpec(stringResource(R.string.btn_yes_to_all), Icons.Default.Check, primary, CoreCommand.YES_TO_ALL),
        CoreCommandSpec(stringResource(R.string.btn_no), Icons.Default.PowerSettingsNew, error, CoreCommand.NO),
        CoreCommandSpec(stringResource(R.string.btn_ctrl_c), Icons.Default.Keyboard, primary, CoreCommand.CTRL_C),
        CoreCommandSpec(stringResource(R.string.btn_backspace), Icons.Default.Keyboard, error, CoreCommand.BACKSPACE),
        CoreCommandSpec(stringResource(R.string.btn_enter), Icons.Default.Keyboard, primary, CoreCommand.ENTER)
    )

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        commands.chunked(2).forEachIndexed { rowIndex, rowCommands ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                CoreCommandTile(
                    spec = rowCommands[0],
                    modifier = Modifier.weight(1f),
                    onClick = { onCoreCommand(rowCommands[0].command) }
                )
                if (rowCommands.size == 1) {
                    Spacer(Modifier.weight(1f))
                } else {
                    CoreCommandVerticalDivider()
                    CoreCommandTile(
                        spec = rowCommands[1],
                        modifier = Modifier.weight(1f),
                        onClick = { onCoreCommand(rowCommands[1].command) }
                    )
                }
            }
            if (rowIndex < commands.lastIndex / 2) {
                CoreCommandHorizontalDivider()
            }
        }
    }
}

@Composable
private fun CoreCommandHorizontalDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun CoreCommandTile(
    spec: CoreCommandSpec,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = spec.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = spec.color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = spec.color,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun CoreCommandVerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MacroSettingsItem(
    macro: Macro,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    SettingsItem(
        title = macro.label,
        subtitle = macro.description.ifBlank { macro.command },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    )
}

@Composable
private fun connectionStatusText(isConnected: Boolean, connectedDeviceName: String?): String {
    return if (isConnected && connectedDeviceName != null) {
        stringResource(
            R.string.device_name_status,
            connectedDeviceName,
            stringResource(R.string.status_connected_label)
        )
    } else {
        stringResource(R.string.status_not_connected)
    }
}
