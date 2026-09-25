package com.haoze.claudekeyboard.audio

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NtpClient {
    const val DEFAULT_SERVER = "ntp.aliyun.com"
    private const val NTP_EPOCH_SECONDS = 2_208_988_800L

    /** Returns local wall-clock minus NTP server time, or null on failure. */
    fun queryOffsetMs(server: String = DEFAULT_SERVER, timeoutMs: Int = 1_000): Long? = runCatching {
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            val payload = ByteArray(48); payload[0] = 0x1b
            val address = InetAddress.getByName(server)
            val sentAt = System.currentTimeMillis()
            socket.send(DatagramPacket(payload, payload.size, address, 123))
            socket.receive(DatagramPacket(payload, payload.size))
            val receivedAt = System.currentTimeMillis()
            val b = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
            val seconds = b.getInt(40).toLong() and 0xffffffffL
            if (seconds == 0L) error("invalid NTP response")
            val fraction = b.getInt(44).toLong() and 0xffffffffL
            val serverMs = (seconds - NTP_EPOCH_SECONDS) * 1_000L + fraction * 1_000L / (1L shl 32)
            ((sentAt + receivedAt) / 2L) - serverMs
        }
    }.getOrNull()
}

