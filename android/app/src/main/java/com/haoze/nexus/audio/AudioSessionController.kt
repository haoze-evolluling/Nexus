package com.haoze.nexus.audio

import android.content.Context
import android.util.Log
import com.haoze.nexus.R
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID

/**
 * Manages active PC session state, incoming connection authorization prompts, and handshakes.
 */
class AudioSessionController(
    private val context: Context,
    private val notificationManager: AudioReceiverNotificationManager
) {

    companion object {
        private const val TAG = "AudioSessionController"
        private const val PROMPT_EXPIRY_MS = 35_000L
    }

    class PromptRecord(
        val prompt: PcAuthPrompt,
        val address: InetAddress,
        val port: Int,
        val nonce: Long
    )

    private val pendingPrompts = HashMap<String, PromptRecord>()

    @Volatile var activePc: ActivePc? = null
    @Volatile var lastConnectedPc: PcDevice? = null

    fun fromActivePc(address: InetAddress, port: Int): Boolean {
        val pc = activePc ?: return false
        if (pc.address != address) return false
        if (pc.port == 0) return true
        return pc.port == port
    }

    fun handleConnControl(
        conn: ConnControl,
        datagram: DatagramPacket,
        socket: DatagramSocket?,
        cachedTrustedPcs: Set<String>,
        selfId: String,
        onResetAudioSession: () -> Unit,
        onCancelReconnect: (String) -> Unit
    ) {
        when (conn.kind) {
            ConnControl.KIND_REQUEST -> {
                val current = activePc
                if (current != null && current.deviceId != conn.deviceId) {
                    // A second PC cannot replace a live session implicitly.
                    respondConn(datagram.address, datagram.port, allow = false, nonce = conn.nonce, socket, selfId)
                    ConnectionBus.transition(conn.deviceId, ConnectionEvent.DENIED)
                    return
                }
                val duplicate = pendingPrompts.values.firstOrNull {
                    it.prompt.deviceId == conn.deviceId && it.nonce == conn.nonce &&
                        it.address == datagram.address && it.port == datagram.port
                }
                val name = conn.name.ifBlank { conn.deviceId.take(8) }
                val trusted = cachedTrustedPcs.contains(conn.deviceId)
                if (activePc?.deviceId == conn.deviceId || trusted) {
                    if (duplicate != null) {
                        pendingPrompts.remove(duplicate.prompt.requestId)
                        if (ConnectionBus.authPrompt.value?.requestId == duplicate.prompt.requestId) {
                            ConnectionBus.authPrompt.value = null
                            notificationManager.dismissAuthNotification()
                        }
                    }
                    respondConn(datagram.address, datagram.port, allow = true, nonce = conn.nonce, socket, selfId)
                    adoptPc(ActivePc(conn.deviceId, name, datagram.address, datagram.port, conn.nonce), socket, selfId, onCancelReconnect)
                    return
                }
                if (duplicate != null) return
                val requestId = UUID.randomUUID().toString().replace("-", "").take(16)
                val prompt = PcAuthPrompt(requestId, conn.deviceId, name, datagram.address.hostAddress ?: "", System.currentTimeMillis())
                pendingPrompts[requestId] = PromptRecord(prompt, datagram.address, datagram.port, conn.nonce)
                ConnectionBus.authPrompt.value = prompt
                notificationManager.postAuthNotification(prompt)
            }
            ConnControl.KIND_BYE -> {
                if (activePc?.deviceId == conn.deviceId && activePc?.nonce == conn.nonce) {
                    val gone = activePc
                    onCancelReconnect(conn.deviceId)
                    onResetAudioSession()
                    ConnectionBus.peerCalibration.value = emptyMap()
                    ConnectionBus.notify(
                        R.string.msg_disconnected,
                        gone?.name ?: LocaleManager.wrap(context).getString(R.string.generic_pc)
                    )
                    ConnectionBus.transition(conn.deviceId, ConnectionEvent.REMOTE_BYE)
                }
            }
            ConnControl.KIND_RESPONSE -> {} // 响应发给发起连接的临时 socket，不会到这里
        }
    }

    fun applyDecision(
        decision: Triple<String, Boolean, Boolean>,
        socket: DatagramSocket?,
        selfId: String,
        onTrustDevice: (String, String) -> Unit,
        onCancelReconnect: (String) -> Unit
    ) {
        val record = pendingPrompts.remove(decision.first) ?: return
        ConnectionBus.authPrompt.value = null
        notificationManager.dismissAuthNotification()
        respondConn(record.address, record.port, decision.second, record.nonce, socket, selfId)
        if (decision.second) {
            if (decision.third) {
                onTrustDevice(record.prompt.deviceId, record.prompt.name)
            }
            adoptPc(
                ActivePc(record.prompt.deviceId, record.prompt.name, record.address, record.port, record.nonce),
                socket,
                selfId,
                onCancelReconnect
            )
        }
    }

    fun expirePrompts() {
        if (pendingPrompts.isEmpty()) return
        val now = System.currentTimeMillis()
        val expired = pendingPrompts.values.filter { now - it.prompt.createdAtMs > PROMPT_EXPIRY_MS }
        for (record in expired) pendingPrompts.remove(record.prompt.requestId)
        if (expired.isNotEmpty()) {
            // 只清理确实过期的请求；重传产生的新请求弹窗不能被旧记录的过期连带关闭。
            val current = ConnectionBus.authPrompt.value
            if (current == null || expired.any { it.prompt.requestId == current.requestId }) {
                ConnectionBus.authPrompt.value = null
                notificationManager.dismissAuthNotification()
            }
        }
    }

    fun adoptPc(
        pc: ActivePc,
        socket: DatagramSocket?,
        selfId: String,
        onCancelReconnect: (String) -> Unit
    ) {
        onCancelReconnect(pc.deviceId)
        activePc = pc
        lastConnectedPc = PcDevice(pc.deviceId, pc.name, pc.address.hostAddress ?: "", NexusProtocol.desktopControlPort)
        ConnectionBus.activePc.value = pc
        ConnectionBus.transition(pc.deviceId, ConnectionEvent.REQUEST_RECEIVED)
        ConnectionBus.transition(pc.deviceId, ConnectionEvent.AUTHORIZED)
        notificationManager.updateForegroundNotification(pc.name)
        ConnectionBus.notify(R.string.msg_connected, pc.name)
        val pendingForPc = pendingPrompts.values.filter { it.prompt.deviceId == pc.deviceId }
        for (rec in pendingForPc) {
            pendingPrompts.remove(rec.prompt.requestId)
            respondConn(rec.address, rec.port, allow = true, nonce = rec.nonce, socket, selfId)
        }
        if (ConnectionBus.authPrompt.value?.deviceId == pc.deviceId) {
            ConnectionBus.authPrompt.value = null
            notificationManager.dismissAuthNotification()
        }
    }

    fun sendBye(pc: ActivePc, socket: DatagramSocket?, selfId: String) {
        runCatching {
            val bye = ConnControl(ConnControl.KIND_BYE, selfId, nonce = pc.nonce).encode()
            repeat(3) {
                socket?.send(DatagramPacket(bye, bye.size, pc.address, NexusProtocol.desktopControlPort))
            }
        }
    }

    private fun respondConn(
        address: InetAddress,
        port: Int,
        allow: Boolean,
        nonce: Long,
        socket: DatagramSocket?,
        selfId: String
    ) {
        try {
            val response = ConnControl(ConnControl.KIND_RESPONSE, selfId, allow = allow, nonce = nonce).encode()
            socket?.send(DatagramPacket(response, response.size, address, port))
        } catch (e: Exception) {
            Log.w(TAG, "respond conn failed: ${e.message}")
        }
    }

    fun clear() {
        pendingPrompts.clear()
        activePc = null
    }
}
