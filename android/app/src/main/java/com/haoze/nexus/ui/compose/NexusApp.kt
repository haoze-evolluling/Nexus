package com.haoze.nexus.ui.compose

import android.bluetooth.BluetoothDevice
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haoze.nexus.macro.Macro
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp(
    isConnected: Boolean,
    connectedDeviceName: String?,
    bottomBarItems: List<BottomBarDestination>,
    inputProfile: com.haoze.nexus.bluetooth.HidProfile,
    onInputProfileChanged: (com.haoze.nexus.bluetooth.HidProfile) -> Unit,
    onNavigate: (AppPage) -> Unit,
    onNavigateRoute: (String) -> Unit,
    onOpenKeyboard: () -> Unit,
    onOpenTouchpad: () -> Unit,
    onOpenGamepad: () -> Unit,
    onOpenTvRemote: () -> Unit,
    onOpenAudioReceiver: () -> Unit,
    onOpenAgent: () -> Unit,
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
    onConnectionTimeout: () -> Unit,
    // Audio receiver
    audioDiscovery: com.haoze.nexus.audio.PcDiscovery,
    audioConnector: com.haoze.nexus.audio.PcConnector,
    audioRepository: com.haoze.nexus.audio.SettingsRepository,
    audioTrustRepository: com.haoze.nexus.audio.PcTrustRepository,
    audioSelfId: String,
    audioSelfName: String,
    audioReceiverRunning: Boolean,
    onAudioConnect: (com.haoze.nexus.audio.PcDevice) -> Unit,
    onAudioDisconnect: (com.haoze.nexus.audio.PcDevice) -> Unit,
    // Core command & Macros
    onCoreCommand: (CoreCommand) -> Unit,
    macros: List<Macro>,
    onMacroClick: (Macro) -> Unit,
    onMacroLongClick: (Macro) -> Unit,
    onAddMacro: () -> Unit,
    onTvRemoteAction: (TvRemoteAction) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { bottomBarItems.size }
    val coroutineScope = rememberCoroutineScope()
    var showConnectionTimeout by remember { mutableStateOf(false) }

    LaunchedEffect(bottomBarItems.size) {
        if (pagerState.currentPage >= bottomBarItems.size) {
            pagerState.scrollToPage(0)
        }
    }

    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = 0,
                animationSpec = tween(durationMillis = 280)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = (bottomBarItems.size - 1).coerceAtLeast(1)
        ) { page ->
            when (bottomBarItems.getOrNull(page)) {
                BottomBarDestination.HOME -> {
                    HomeScreen(
                        isConnected = isConnected,
                        connectedDeviceName = connectedDeviceName,
                        connectingDeviceAddress = connectingDeviceAddress,
                        inputProfile = inputProfile,
                        onInputProfileChanged = onInputProfileChanged,
                        onShowDeviceList = onShowDeviceList,
                        onDisconnectDevice = onDisconnectDevice,
                        onOpenKeyboard = onOpenKeyboard,
                        onOpenTouchpad = onOpenTouchpad,
                        onOpenGamepad = onOpenGamepad,
                        onOpenTvRemote = {
                            val tvRemoteIndex = bottomBarItems.indexOf(BottomBarDestination.TV_REMOTE)
                            if (tvRemoteIndex >= 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(tvRemoteIndex) }
                            } else {
                                onOpenTvRemote()
                            }
                        },
                        onOpenAudioReceiver = {
                            val audioIndex = bottomBarItems.indexOf(BottomBarDestination.AUDIO_RECEIVER)
                            if (audioIndex >= 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(audioIndex) }
                            } else {
                                onOpenAudioReceiver()
                            }
                        },
                        onOpenAgent = {
                            val agentIndex = bottomBarItems.indexOf(BottomBarDestination.AGENT)
                            if (agentIndex >= 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(agentIndex) }
                            } else {
                                onOpenAgent()
                            }
                        },
                        onOpenSettings = {
                            val settingsIndex = bottomBarItems.indexOf(BottomBarDestination.SETTINGS)
                            if (settingsIndex >= 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(settingsIndex) }
                            } else {
                                onNavigate(AppPage.SETTINGS)
                            }
                        },
                        onOpenBottomBarCustomization = {
                            onNavigateRoute(com.haoze.nexus.ui.Routes.BOTTOM_BAR_CUSTOMIZATION)
                        },
                        onOpenAbout = { onNavigateRoute(com.haoze.nexus.ui.Routes.ABOUT) },
                        onOpenSponsor = { onNavigateRoute(com.haoze.nexus.ui.Routes.SPONSOR) },
                        onOpenSponsorList = { onNavigateRoute(com.haoze.nexus.ui.Routes.SPONSOR_LIST) },
                        onCoreCommand = onCoreCommand,
                        macros = macros,
                        onMacroClick = onMacroClick
                    )
                }
                BottomBarDestination.AUDIO_RECEIVER -> {
                    com.haoze.nexus.ui.audio.AudioReceiverScreen(
                        discovery = audioDiscovery,
                        connector = audioConnector,
                        repository = audioRepository,
                        trustRepository = audioTrustRepository,
                        selfId = audioSelfId,
                        selfName = audioSelfName,
                        receiverRunning = audioReceiverRunning,
                        onConnect = onAudioConnect,
                        onDisconnect = onAudioDisconnect,
                        onBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        showBackIcon = false,
                        contentBottomPadding = 108.dp
                    )
                }
                BottomBarDestination.AGENT -> {
                    AgentScreen(
                        isConnected = isConnected,
                        connectedDeviceName = connectedDeviceName,
                        macros = macros,
                        onBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onCoreCommand = onCoreCommand,
                        onMacroClick = onMacroClick,
                        onMacroLongClick = onMacroLongClick,
                        onAddMacro = onAddMacro,
                        showBackIcon = false,
                        contentBottomPadding = 108.dp
                    )
                }
                BottomBarDestination.TV_REMOTE -> {
                    TvRemoteScreen(
                        enabled = isConnected,
                        onBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onAction = onTvRemoteAction,
                        showBackIcon = false,
                        contentBottomPadding = 108.dp
                    )
                }
                BottomBarDestination.SETTINGS -> {
                    SettingsScreen(
                        onBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onNavigateToRoute = onNavigateRoute,
                        inputProfile = inputProfile,
                        onInputProfileChanged = { profile ->
                            onInputProfileChanged(profile)
                            true
                        },
                        showBackIcon = false,
                        contentBottomPadding = 108.dp
                    )
                }
                null -> Unit
            }
        }

        FloatingNavigationBar(
            selectedPage = pagerState.currentPage,
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
            items = bottomBarItems,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            pagerProgress = { pagerState.currentPage + pagerState.currentPageOffsetFraction }
        )
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
