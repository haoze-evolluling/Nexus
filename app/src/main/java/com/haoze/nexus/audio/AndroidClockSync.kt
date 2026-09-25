package com.haoze.claudekeyboard.audio

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class AndroidClockSyncResult(val offsetMs: Long, val rttMs: Long)

/** Performs a short NTP-style probe against another SteamVoice Android receiver. */
object AndroidClockSync {
    private const val PROBES = 4
    private const val TIMEOUT_MS = 900

    fun query(device: AndroidDevice): AndroidClockSyncResult? {
        val estimator = ClockSyncEstimator()
        val address = InetAddress.getByName(device.host)
        DatagramSocket().use { socket ->
            socket.soTimeout = TIMEOUT_MS
            repeat(PROBES) {
                val t1 = System.nanoTime()
                val request = TimeSyncControl(TimeSyncControl.KIND_REQUEST, t1, 0, 0).encode()
                socket.send(DatagramPacket(request, request.size, address, device.port))
                val bytes = ByteArray(TimeSyncControl.SIZE)
                val response = DatagramPacket(bytes, bytes.size)
                try {
                    socket.receive(response)
                    val control = TimeSyncControl.decode(response.data, response.length)
                    if (control?.kind == TimeSyncControl.KIND_RESPONSE && control.t1 == t1) {
                        estimator.onExchange(t1, control.t2, control.t3, System.nanoTime())
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    // A lost probe is expected on Wi-Fi; remaining probes still form an estimate.
                }
            }
        }
        // Unlike the PC playback telemetry, peer calibration needs the actual
        // mapping between two boot-relative clocks to schedule one instant.
        val offset = estimator.medianOffsetMs() ?: return null
        val rtt = estimator.lastRttMs() ?: return null
        return AndroidClockSyncResult(offset, rtt)
    }
}

