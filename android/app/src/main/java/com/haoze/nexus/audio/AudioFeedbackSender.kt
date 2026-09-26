package com.haoze.nexus.audio

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Handles encoding and transmission of [ReceiverFeedback] UDP packets.
 */
object AudioFeedbackSender {

    fun sendFeedback(
        socket: DatagramSocket?,
        address: InetAddress?,
        port: Int,
        session: Long,
        highest: Long,
        received: Long,
        lost: Long,
        queue: Int,
        bitrate: Int,
        syncState: Int = SyncState.UNKNOWN,
        offsetMs: Int = 0,
        rttMs: Int = 0
    ) {
        if (address == null || port == 0 || socket == null) return
        try {
            val bytes = ReceiverFeedback(
                session = session,
                highestSeq = highest,
                received = received,
                lost = lost,
                queue = queue,
                bitrate = bitrate,
                syncState = syncState,
                offsetMs = offsetMs,
                rttMs = rttMs
            ).encode()
            socket.send(DatagramPacket(bytes, bytes.size, address, port))
        } catch (_: Exception) {}
    }
}
