package com.haoze.nexus.audio

/** Transport state machine shared by connector, receiver service and UI. */
enum class ConnectionState { IDLE, CONNECTING, AWAITING_AUTHORIZATION, CONNECTED, DISCONNECTING, RECONNECTING, FAILED }
enum class ConnectionEvent { CONNECT, REQUEST_RECEIVED, AUTHORIZED, DENIED, HANDSHAKE_TIMEOUT, HEARTBEAT_TIMEOUT, REMOTE_BYE, LOCAL_DISCONNECT, RETRY }

fun nextConnectionState(state: ConnectionState, event: ConnectionEvent): ConnectionState = when (state) {
    ConnectionState.IDLE -> if (event == ConnectionEvent.CONNECT || event == ConnectionEvent.REQUEST_RECEIVED) ConnectionState.CONNECTING else state
    ConnectionState.CONNECTING -> when (event) { ConnectionEvent.AUTHORIZED -> ConnectionState.CONNECTED; ConnectionEvent.REQUEST_RECEIVED -> ConnectionState.AWAITING_AUTHORIZATION; ConnectionEvent.DENIED, ConnectionEvent.HANDSHAKE_TIMEOUT -> ConnectionState.FAILED; else -> state }
    ConnectionState.AWAITING_AUTHORIZATION -> when (event) { ConnectionEvent.AUTHORIZED -> ConnectionState.CONNECTED; ConnectionEvent.DENIED, ConnectionEvent.HANDSHAKE_TIMEOUT -> ConnectionState.FAILED; else -> state }
    ConnectionState.CONNECTED -> when (event) { ConnectionEvent.LOCAL_DISCONNECT, ConnectionEvent.REMOTE_BYE -> ConnectionState.DISCONNECTING; ConnectionEvent.HEARTBEAT_TIMEOUT -> ConnectionState.RECONNECTING; else -> state }
    ConnectionState.DISCONNECTING -> if (event == ConnectionEvent.RETRY) ConnectionState.IDLE else state
    ConnectionState.RECONNECTING -> when (event) { ConnectionEvent.AUTHORIZED -> ConnectionState.CONNECTED; ConnectionEvent.HANDSHAKE_TIMEOUT -> ConnectionState.FAILED; ConnectionEvent.LOCAL_DISCONNECT -> ConnectionState.DISCONNECTING; else -> state }
    ConnectionState.FAILED -> if (event == ConnectionEvent.RETRY || event == ConnectionEvent.CONNECT) ConnectionState.CONNECTING else state
}

object TransportTiming {
    const val HANDSHAKE_TIMEOUT_MS = 8_000L
    const val REQUEST_RETRY_MS = 1_500L
    const val HEARTBEAT_INTERVAL_MS = 1_000L
    const val HEARTBEAT_TIMEOUT_MS = 3_500L
    const val MAX_RECONNECT_ATTEMPTS = 5
}

