package com.haoze.nexus.ui.compose

import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import com.haoze.nexus.ui.component.animation.bouncyCardClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.haoze.nexus.R
import com.haoze.nexus.macro.Macro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppPage {
    HOME,
    AGENT,
    TV_REMOTE,
    SETTINGS
}

enum class CoreCommand {
    YES,
    YES_TO_ALL,
    NO,
    CTRL_C,
    BACKSPACE,
    ENTER
}

enum class TvRemoteAction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    CONFIRM,
    BACK,
    ASSISTANT,
    HOME,
    MUTE,
    VOLUME_UP,
    VOLUME_DOWN,
    POWER,
    PLAY_PAUSE,
    NEXT,
    PREVIOUS,
    STOP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp(
    isConnected: Boolean,
    connectedDeviceName: String?,
    onNavigate: (AppPage) -> Unit,
    onNavigateRoute: (String) -> Unit,
    onOpenKeyboard: () -> Unit,
    onOpenTouchpad: () -> Unit,
    onOpenGamepad: () -> Unit,
    onShowDeviceList: () -> Unit,
    showDeviceList: Boolean,
    pairedDevices: List<BluetoothDevice>,
    deviceListPermissionDenied: Boolean,
    connectingDeviceAddress: String?,
    connectedDeviceAddress: String?,
    lastConnectedDeviceAddress: String?,
    onDismissDeviceList: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onConnectionTimeout: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()
    var showConnectionTimeout by remember { mutableStateOf(false) }

    BackHandler(enabled = pagerState.currentPage == 1) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = 0,
                animationSpec = tween(durationMillis = 280)
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                title = {
                    Text(
                        if (pagerState.currentPage == 0) stringResource(R.string.tab_claude)
                        else stringResource(R.string.home_feature_hub)
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    HomeConnectionPage(
                        isConnected = isConnected,
                        connectedDeviceName = connectedDeviceName,
                        onShowDeviceList = onShowDeviceList
                    )
                } else {
                    val context = LocalContext.current
                    FeatureHubScreen(
                        onOpenKeyboard = onOpenKeyboard,
                        onOpenTouchpad = onOpenTouchpad,
                        onOpenGamepad = onOpenGamepad,
                        onOpenAudioReceiver = { context.startActivity(Intent(context, com.haoze.nexus.ui.audio.AudioReceiverActivity::class.java)) },
                        onNavigateAgent = { onNavigate(AppPage.AGENT) },
                        onNavigateTvRemote = { onNavigate(AppPage.TV_REMOTE) },
                        onNavigateSettings = { onNavigate(AppPage.SETTINGS) },
                        onNavigateAbout = { onNavigateRoute(com.haoze.nexus.ui.Routes.ABOUT) },
                        onNavigateSponsor = { onNavigateRoute(com.haoze.nexus.ui.Routes.SPONSOR) },
                        onNavigateSponsorList = { onNavigateRoute(com.haoze.nexus.ui.Routes.SPONSOR_LIST) }
                    )
                }
            }

            FloatingNavigationBar(
                currentPage = pagerState.currentPage,
                onPageSelected = { targetPage ->
                    if (pagerState.currentPage != targetPage) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = targetPage,
                                animationSpec = tween(durationMillis = 280)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                pagerProgress = { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            )
        }
    }

    if (showDeviceList) {
        DeviceListAlertDialog(
            devices = pairedDevices,
            permissionDenied = deviceListPermissionDenied,
            connectingAddress = connectingDeviceAddress,
            connectedAddress = connectedDeviceAddress,
            lastConnectedAddress = lastConnectedDeviceAddress,
            onDismiss = onDismissDeviceList,
            onDeviceSelected = onConnectDevice,
            onDisconnect = onDisconnectDevice,
            onConnectionTimeout = {
                onConnectionTimeout()
                onDismissDeviceList()
                showConnectionTimeout = true
            }
        )
    }

    if (showConnectionTimeout) {
        ConnectionTimeoutAlertDialog(onDismiss = { showConnectionTimeout = false })
    }
}

@Composable
fun NexusConfirmationDialog(title: String, message: String, confirmLabel: String, destructive: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppAlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) }, confirmButton = {
        TextButton(onClick = onConfirm) { Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } })
}

@Composable
private fun DeviceListAlertDialog(
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
private fun ConnectionTimeoutAlertDialog(onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_connect_timeout_title)) },
        text = { Text(stringResource(R.string.dialog_connect_timeout_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_ok)) }
        }
    )
}

@Composable
fun MacroEditorAlertDialog(macro: Macro?, onDismiss: () -> Unit, onSave: (String, String, String, Boolean) -> Unit, onDelete: (() -> Unit)?) {
    var label by remember(macro) { mutableStateOf(macro?.label.orEmpty()) }
    var description by remember(macro) { mutableStateOf(macro?.description.orEmpty()) }
    var command by remember(macro) { mutableStateOf(macro?.command.orEmpty()) }
    var sendEnter by remember(macro) { mutableStateOf(macro?.sendEnter ?: false) }
    AppAlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (macro == null) R.string.dialog_add_macro else R.string.dialog_edit_macro)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(label, { label = it }, label = { Text(stringResource(R.string.dialog_macro_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text(stringResource(R.string.dialog_macro_description)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(command, { command = it }, label = { Text(stringResource(R.string.dialog_macro_command)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.switch_send_enter), Modifier.weight(1f)); Switch(sendEnter, { sendEnter = it }) }
        }
    }, confirmButton = {
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
    })
}

@Composable
private fun HomeConnectionPage(
    isConnected: Boolean,
    connectedDeviceName: String?,
    onShowDeviceList: () -> Unit
) {
    val glowColor by animateColorAsState(
        targetValue = if (isConnected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        },
        animationSpec = tween(250),
        label = "BluetoothControlGlowColor"
    )
    val haloColor by animateColorAsState(
        targetValue = if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(250),
        label = "BluetoothControlHaloColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(250),
        label = "BluetoothControlContainerColor"
    )
    val contentColor = Color(0xFF5CB67B)
    val haloSize by animateDpAsState(if (isConnected) 148.dp else 124.dp, tween(250), label = "BluetoothControlHaloSize")
    val glowSize by animateDpAsState(if (isConnected) 128.dp else 108.dp, tween(250), label = "BluetoothControlGlowSize")
    val buttonSize by animateDpAsState(if (isConnected) 92.dp else 84.dp, tween(250), label = "BluetoothControlButtonSize")
    val statusText = stringResource(
        if (isConnected) R.string.status_connected_label else R.string.status_not_connected
    )
    val hintText = if (isConnected) {
        connectedDeviceName ?: stringResource(R.string.home_connection_connected_hint)
    } else {
        stringResource(R.string.home_connection_disconnected_hint)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(156.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(haloSize)
                    .background(haloColor, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(glowSize)
                    .background(glowColor, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .background(containerColor, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onShowDeviceList),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_connect),
                    contentDescription = stringResource(
                        if (isConnected) R.string.home_connection_action_connected
                        else R.string.home_connection_action_disconnected
                    ),
                    tint = contentColor,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
        Text(
            text = statusText,
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = hintText,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        HomeDeviceSelector(
            isConnected = isConnected,
            connectedDeviceName = connectedDeviceName,
            onClick = onShowDeviceList,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

@Composable
private fun HomeDeviceSelector(
    isConnected: Boolean,
    connectedDeviceName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceName = if (isConnected && !connectedDeviceName.isNullOrBlank()) {
        connectedDeviceName
    } else {
        stringResource(R.string.home_select_device)
    }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = deviceName,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.home_device_selector_label)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.home_connection_action_disconnected)
                )
            },
            shape = SettingsCornerShape,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(SettingsCornerShape)
                .clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeatureHubScreen(
    onOpenKeyboard: () -> Unit,
    onOpenTouchpad: () -> Unit,
    onOpenGamepad: () -> Unit,
    onOpenAudioReceiver: () -> Unit,
    onNavigateAgent: () -> Unit,
    onNavigateTvRemote: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigateSponsor: () -> Unit,
    onNavigateSponsorList: () -> Unit
) {
    val inputItems = listOf(
        FeatureHubItem(stringResource(R.string.home_keyboard_title), Icons.Default.Keyboard, onOpenKeyboard),
        FeatureHubItem(stringResource(R.string.home_touchpad_title), Icons.Default.Mouse, onOpenTouchpad),
        FeatureHubItem(stringResource(R.string.home_gamepad_title), Icons.Default.SportsEsports, onOpenGamepad),
        FeatureHubItem(stringResource(R.string.home_tvremote_title), Icons.Default.SettingsRemote, onNavigateTvRemote),
        FeatureHubItem(stringResource(R.string.home_audio_stream_title), Icons.Default.GraphicEq, onOpenAudioReceiver)
    )
    val systemItems = listOf(
        FeatureHubItem(stringResource(R.string.home_agent_title), Icons.Default.Terminal, onNavigateAgent),
        FeatureHubItem(stringResource(R.string.home_settings_title), Icons.Default.Settings, onNavigateSettings)
    )
    val aboutItems = listOf(
        FeatureHubItem(stringResource(R.string.home_about_title), Icons.Default.Info, onNavigateAbout),
        FeatureHubItem(stringResource(R.string.home_sponsor_title), Icons.Default.Favorite, onNavigateSponsor),
        FeatureHubItem(stringResource(R.string.home_sponsor_list_title), Icons.Filled.WorkspacePremium, onNavigateSponsorList)
    )
    val categories = listOf(
        FeatureHubCategory(stringResource(R.string.home_input_controls), inputItems),
        FeatureHubCategory(stringResource(R.string.home_shortcuts_and_system), systemItems),
        FeatureHubCategory(stringResource(R.string.home_about_support), aboutItems)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 108.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { category ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(category.items.size) { index ->
                val item = category.items[index]
                Card(
                    shape = SettingsCornerShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SettingsCornerShape)
                        .bouncyCardClickable(
                            onClick = item.onClick,
                            onLongClick = item.onLongClick
                        )
                        .heightIn(min = 80.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private data class FeatureHubItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null
)

private data class FeatureHubCategory(
    val title: String,
    val items: List<FeatureHubItem>
)

@Composable
fun AgentScreen(
    isConnected: Boolean,
    connectedDeviceName: String?,
    macros: List<Macro>,
    onBack: () -> Unit,
    onCoreCommand: (CoreCommand) -> Unit,
    onMacroClick: (Macro) -> Unit,
    onMacroLongClick: (Macro) -> Unit,
    onAddMacro: () -> Unit
) {
    SettingsScaffold(
        title = stringResource(R.string.home_agent_title),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
