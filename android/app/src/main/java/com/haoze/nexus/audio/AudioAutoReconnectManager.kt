package com.haoze.nexus.audio

import android.content.Context
import com.haoze.nexus.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetAddress

/**
 * Manages exponential backoff auto-reconnection attempts to a disconnected PC.
 */
class AudioAutoReconnectManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val getSelfId: () -> String,
    private val isStopRequested: () -> Boolean
) {
    private var reconnectJob: Job? = null

    fun startAutoReconnect(pc: PcDevice) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delays = TransportTiming.RECONNECT_BACKOFF_DELAYS_MS
            val maxAttempts = delays.size
            val connector = PcConnector()
            val selfId = getSelfId()
            val selfName = DeviceIdentity.friendlyName(context)

            for (attempt in 1..maxAttempts) {
                if (!isActive || isStopRequested()) break
                ConnectionBus.setReconnectProgress(pc.deviceId, attempt, maxAttempts)
                ConnectionBus.notify(R.string.msg_reconnecting, pc.name)
                delay(delays[attempt - 1])
                if (!isActive || isStopRequested()) break

                val currentState = ConnectionBus.stateOf(pc.deviceId).value
                if (currentState != ConnectionState.RECONNECTING) break

                val result = connector.request(pc, selfId, selfName, timeoutMs = 4_000L, manageState = false)
                if (result is PcConnector.ConnectResult.Accepted) {
                    val actualHost = result.verifiedHost.ifEmpty { pc.host }
                    ConnectionBus.queuedSender = ActivePc(
                        pc.deviceId,
                        pc.name,
                        InetAddress.getByName(actualHost),
                        nonce = result.nonce
                    )
                    ConnectionBus.clearReconnectProgress(pc.deviceId)
                    ConnectionBus.transition(pc.deviceId, ConnectionEvent.AUTHORIZED)
                    ConnectionBus.notify(R.string.msg_reconnected, pc.name)
                    return@launch
                } else if (result is PcConnector.ConnectResult.Denied) {
                    ConnectionBus.clearReconnectProgress(pc.deviceId)
                    ConnectionBus.transition(pc.deviceId, ConnectionEvent.DENIED)
                    return@launch
                }
            }
            ConnectionBus.clearReconnectProgress(pc.deviceId)
            ConnectionBus.transition(pc.deviceId, ConnectionEvent.HANDSHAKE_TIMEOUT)
            ConnectionBus.notify(R.string.msg_reconnect_failed, pc.name)
        }
    }

    fun cancelAutoReconnect(deviceId: String) {
        reconnectJob?.cancel()
        reconnectJob = null
        ConnectionBus.clearReconnectProgress(deviceId)
    }

    fun cancelAll() {
        reconnectJob?.cancel()
        reconnectJob = null
    }
}
