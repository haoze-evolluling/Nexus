package com.haoze.nexus.ui.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.haoze.nexus.R
import com.haoze.nexus.audio.ActivePc
import com.haoze.nexus.audio.AudioReceiverService
import com.haoze.nexus.audio.ConnectionBus
import com.haoze.nexus.audio.ConnectionEvent
import com.haoze.nexus.audio.DeviceIdentity
import com.haoze.nexus.audio.LocaleManager
import com.haoze.nexus.audio.PcConnector
import com.haoze.nexus.audio.PcDevice
import com.haoze.nexus.audio.PcDiscovery
import com.haoze.nexus.audio.PcTrustRepository
import com.haoze.nexus.audio.SettingsRepository
import com.haoze.nexus.ui.compose.NexusTheme
import com.haoze.nexus.ui.compose.getThemeColorStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AudioReceiverActivity : com.haoze.nexus.AppLocalizedActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { startReceiver() }
    private val discovery by lazy { PcDiscovery(this) }
    private val connector = PcConnector()
    private val repository by lazy { SettingsRepository(applicationContext) }
    private val trustRepository by lazy { PcTrustRepository(applicationContext) }
    private var selfId by mutableStateOf("")
    private val selfName: String by lazy { DeviceIdentity.friendlyName(applicationContext) }
    private var receiverRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch(Dispatchers.IO) { selfId = repository.settings.first().deviceId }
        setContent {
            NexusTheme(colorStyle = getThemeColorStyle(this)) {
                AudioReceiverScreen(
                    discovery = discovery,
                    connector = connector,
                    repository = repository,
                    trustRepository = trustRepository,
                    selfId = selfId,
                    selfName = selfName,
                    receiverRunning = receiverRunning,
                    onConnect = ::connectToPc,
                    onDisconnect = ::disconnectFromPc,
                    onBack = { finish() }
                )
            }
        }
        ensureReceiverRunning()
    }

    override fun onStart() {
        super.onStart()
        discovery.start()
    }

    override fun onStop() {
        super.onStop()
        discovery.stop()
    }

    private fun ensureReceiverRunning() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startReceiver()
    }

    private fun startReceiver() {
        ContextCompat.startForegroundService(this, Intent(this, AudioReceiverService::class.java))
        receiverRunning = true
    }

    private fun connectToPc(pc: PcDevice) {
        if (selfId.isEmpty()) return
        ensureReceiverRunning()
        val requestId = selfId
        lifecycleScope.launch(Dispatchers.IO) {
            val result = connector.request(pc, requestId, selfName)
            if (result is PcConnector.ConnectResult.Accepted) {
                runCatching {
                    val actualHost = result.verifiedHost.ifEmpty { pc.host }
                    ConnectionBus.queuedSender = ActivePc(pc.deviceId, pc.name, java.net.InetAddress.getByName(actualHost), nonce = result.nonce)
                    trustRepository.trust(pc.deviceId, pc.name)
                }
            } else if (result is PcConnector.ConnectResult.Denied) {
                ConnectionBus.notify(R.string.pc_denied)
            } else if (result is PcConnector.ConnectResult.Timeout) {
                ConnectionBus.notify(R.string.pc_connect_failed, pc.name)
            }
        }
    }

    private fun disconnectFromPc(pc: PcDevice) {
        if (selfId.isEmpty()) return
        ConnectionBus.transition(pc.deviceId, ConnectionEvent.LOCAL_DISCONNECT)
        ConnectionBus.localDisconnects.add(pc.deviceId)
    }
}
