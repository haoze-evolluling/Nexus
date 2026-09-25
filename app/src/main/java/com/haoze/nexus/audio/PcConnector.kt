package com.haoze.nexus.audio

import android.util.Log
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.security.SecureRandom

/**
 * 与电脑端控制端口之间的连接协商。请求通过临时 UDP socket 发送，
 * 每 1.5 秒重传一次直至超时，桌面端会按设备去重。
 */
class PcConnector {

    sealed interface ConnectResult {
        /** 对端同意，responderId 为电脑的设备标识。 */
        data class Accepted(val responderId: String, val nonce: Long, val verifiedHost: String = "") : ConnectResult
        data object Denied : ConnectResult
        data object Timeout : ConnectResult
    }

    fun request(pc: PcDevice, selfId: String, selfName: String, timeoutMs: Long = TransportTiming.HANDSHAKE_TIMEOUT_MS): ConnectResult {
        ConnectionBus.transition(pc.deviceId, ConnectionEvent.CONNECT)
        val nonce = SecureRandom().nextLong().let { if (it == 0L) 1L else it }
        val payload = try {
            ConnControl(ConnControl.KIND_REQUEST, selfId, selfName, nonce = nonce).encode()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "invalid identity for connect request", e)
            ConnectionBus.transition(pc.deviceId, ConnectionEvent.HANDSHAKE_TIMEOUT)
            return ConnectResult.Timeout
        }
        java.net.DatagramSocket().use { socket ->
            socket.soTimeout = 200
            val target = InetSocketAddress(pc.host, pc.port)
            val started = System.nanoTime()
            var nextRetransmitNs = 0L
            val promptIndicationDelayNs = TransportTiming.PROMPT_INDICATION_DELAY_MS * 1_000_000L
            var promptIndicated = false
            val buf = ByteArray(128)
            while (System.nanoTime() - started < timeoutMs * 1_000_000L) {
                val elapsed = System.nanoTime() - started
                if (!promptIndicated && elapsed >= promptIndicationDelayNs) {
                    if (ConnectionBus.stateOf(pc.deviceId).value == ConnectionState.CONNECTING) {
                        ConnectionBus.transition(pc.deviceId, ConnectionEvent.REQUEST_RECEIVED)
                    }
                    promptIndicated = true
                }
                if (elapsed >= nextRetransmitNs) {
                    runCatching { socket.send(DatagramPacket(payload, payload.size, target)) }
                    nextRetransmitNs = elapsed + RETRANSMIT_INTERVAL_MS * 1_000_000L
                }
                val datagram = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(datagram)
                } catch (_: SocketTimeoutException) {
                    continue
                }
                val msg = ConnControl.decode(buf, datagram.length) ?: continue
                if (msg.kind == ConnControl.KIND_RESPONSE) {
                    if (msg.nonce != nonce && msg.nonce != 0L) continue
                    if (msg.deviceId != pc.deviceId) continue
                    val verifiedHost = datagram.address?.hostAddress ?: pc.host
                    return if (msg.allow) {
                        ConnectionBus.transition(pc.deviceId, ConnectionEvent.AUTHORIZED)
                        ConnectResult.Accepted(msg.deviceId, nonce, verifiedHost)
                    } else {
                        ConnectionBus.transition(pc.deviceId, ConnectionEvent.DENIED)
                        ConnectResult.Denied
                    }
                }
            }
            ConnectionBus.transition(pc.deviceId, ConnectionEvent.HANDSHAKE_TIMEOUT)
            return ConnectResult.Timeout
        }
    }

    /** 通知电脑断开连接；UDP 不可靠，连发三次提高送达率。 */
    fun bye(pc: PcDevice, selfId: String, nonce: Long = 0L) {
        val payload = try {
            ConnControl(ConnControl.KIND_BYE, selfId, nonce = nonce).encode()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "invalid identity for bye", e)
            return
        }
        java.net.DatagramSocket().use { socket ->
            val target = InetSocketAddress(pc.host, pc.port)
            val packet = DatagramPacket(payload, payload.size, target)
            repeat(BYE_ATTEMPTS) {
                runCatching { socket.send(packet) }
                if (it < BYE_ATTEMPTS - 1) runCatching { Thread.sleep(60) }
            }
        }
    }

    private companion object {
        const val TAG = "NexusPcConnector"
        const val RETRANSMIT_INTERVAL_MS = 1500
        const val BYE_ATTEMPTS = 3
    }
}

