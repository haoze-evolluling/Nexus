package com.haoze.claudekeyboard.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class SteamVoicePacket(
    val codec: Int,
    val sampleRate: Int,
    val channels: Int,
    val bitrate: Int,
    val frameMilliseconds: Int,
    val session: Long,
    val sequence: Long,
    val opus: ByteArray,
    val flags: Int = 0,
    /** 帧首样本在发送端流时钟下的捕获时间（纳秒），用于多设备同步播放。 */
    val timestampNs: Long = 0L,
)

/** 校准阶段（随反馈上报，驱动两端同步校准 UI）。 */
object SyncState {
    const val UNKNOWN = 0
    const val CALIBRATING = 1
    const val ALIGNED = 2
    const val PLAYING = 3
}

data class ReceiverFeedback(
    val session: Long,
    val highestSeq: Long,
    val received: Long,
    val lost: Long,
    val queue: Int,
    val bitrate: Int,
    val syncState: Int = SyncState.UNKNOWN,
    val offsetMs: Int = 0,
    val rttMs: Int = 0,
) {
    fun encode(): ByteArray {
        val b = ByteBuffer.allocate(SIZE).order(ByteOrder.BIG_ENDIAN)
        b.put("SVCT".toByteArray()).put(SteamVoiceProtocol.version.toByte()).put(1.toByte()).putShort(0)
        b.putInt(session.toInt()).putInt(highestSeq.toInt()).putInt(received.toInt()).putInt(lost.toInt()).putShort(queue.toShort()).putInt(bitrate)
        b.put(syncState.toByte()).putShort(offsetMs.toShort()).putShort(rttMs.toShort())
        return b.array()
    }

    companion object {
        const val SIZE = 35
    }
}

data class SettingsControl(val bitrateKbps: Int, val frameMs: Int, val updatedAtMs: Long, val deviceId: String) {
    fun encode(): ByteArray {
        val b = ByteBuffer.allocate(40).order(ByteOrder.BIG_ENDIAN)
        b.put("SVCS".toByteArray()).put(SteamVoiceProtocol.version.toByte()).put(1.toByte()).putShort(0)
        b.putInt(bitrateKbps * 1000).putShort(frameMs.toShort()).putShort(0).putLong(updatedAtMs)
        val id = deviceId.toByteArray().copyOf(16)
        b.put(id)
        return b.array()
    }
    companion object {
        fun decode(data: ByteArray, length: Int): SettingsControl? {
            if (length != 40 || data.copyOfRange(0, 4).decodeToString() != "SVCS" || data[4].toInt() != SteamVoiceProtocol.version || data[5].toInt() != 1) return null
            val b = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)
            b.position(8)
            val bitrate = b.int / 1000
            val frame = b.short.toInt() and 0xffff
            b.short
            val updated = b.long
            val idBytes = ByteArray(16); b.get(idBytes)
            val id = idBytes.decodeToString().trimEnd('\u0000')
            return SettingsControl(bitrate, frame, updated, id)
        }
    }
}

/** NTP 风格的时钟同步报文（SVTS）。t4 由请求方本地记录，不经网络传输。 */
data class TimeSyncControl(val kind: Int, val t1: Long, val t2: Long, val t3: Long) {
    fun encode(): ByteArray {
        val b = ByteBuffer.allocate(SIZE).order(ByteOrder.BIG_ENDIAN)
        b.put(MAGIC.toByteArray()).put(SteamVoiceProtocol.version.toByte()).put(kind.toByte()).putShort(0)
        b.putLong(t1).putLong(t2).putLong(t3).putLong(0)
        return b.array()
    }

    companion object {
        const val KIND_REQUEST = 1
        const val KIND_RESPONSE = 2
        const val SIZE = 40
        private const val MAGIC = "SVTS"

        fun decode(data: ByteArray, length: Int): TimeSyncControl? {
            if (length != SIZE || data.copyOfRange(0, 4).decodeToString() != MAGIC) return null
            if (data[4].toInt() != SteamVoiceProtocol.version) return null
            val kind = data[5].toInt() and 0xff
            if (kind != KIND_REQUEST && kind != KIND_RESPONSE) return null
            val b = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)
            return TimeSyncControl(kind, b.getLong(8), b.getLong(16), b.getLong(24))
        }
    }
}

/** Android-to-Android playback calibration control. It deliberately has its
 * own magic so desktop senders never need to understand or relay it. */
data class PeerCalibrationControl(
    val kind: Int,
    val operation: Long,
    val deviceId: String,
    val pcId: String,
    val targetNs: Long = 0L,
    val offsetNs: Long = 0L,
    val rttMs: Long = 0L,
) {
    fun encode(): ByteArray {
        val device = deviceId.toByteArray(Charsets.UTF_8)
        val pc = pcId.toByteArray(Charsets.UTF_8)
        require(device.size in 1..MAX_ID_BYTES && pc.size in 1..MAX_ID_BYTES)
        return ByteBuffer.allocate(HEADER_SIZE + device.size + pc.size).order(ByteOrder.BIG_ENDIAN).apply {
            put(MAGIC.toByteArray()).put(SteamVoiceProtocol.version.toByte()).put(kind.toByte()).putShort(0)
            putLong(operation).putLong(targetNs).putLong(offsetNs).putLong(rttMs)
            putShort(device.size.toShort()).putShort(pc.size.toShort()).put(device).put(pc)
        }.array()
    }

    companion object {
        const val REQUEST = 1; const val ACCEPT = 2; const val REJECT = 3
        const val COMMIT = 4; const val COMPLETE = 5; const val CANCEL = 6
        private const val MAGIC = "SVAC"
        private const val HEADER_SIZE = 44
        private const val MAX_ID_BYTES = 64

        fun decode(data: ByteArray, length: Int): PeerCalibrationControl? {
            if (length < HEADER_SIZE || data.copyOfRange(0, 4).decodeToString() != MAGIC || data[4].toInt() != SteamVoiceProtocol.version) return null
            val kind = data[5].toInt() and 0xff
            if (kind !in REQUEST..CANCEL) return null
            val b = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)
            val operation = b.getLong(8); val target = b.getLong(16); val offset = b.getLong(24); val rtt = b.getLong(32)
            val deviceLen = b.getShort(40).toInt() and 0xffff; val pcLen = b.getShort(42).toInt() and 0xffff
            if (deviceLen !in 1..MAX_ID_BYTES || pcLen !in 1..MAX_ID_BYTES || length != HEADER_SIZE + deviceLen + pcLen) return null
            val device = ByteArray(deviceLen); val pc = ByteArray(pcLen); b.position(HEADER_SIZE); b.get(device); b.get(pc)
            return PeerCalibrationControl(kind, operation, device.decodeToString(), pc.decodeToString(), target, offset, rtt)
        }
    }
}

object SteamVoiceProtocol {
    const val port = 40125
    const val version = 4
    const val codecOpus = 1
    const val sampleRate = 48000
    const val channels = 2
    const val frameMilliseconds = 10
    val supportedFrameMilliseconds = setOf(10, 20)
    const val desktopControlPort = 40126
    const val handshakeTimeoutMs = 8_000L
    const val requestRetryMs = 1_500L
    const val heartbeatIntervalMs = 1_000L
    const val heartbeatTimeoutMs = 3_500L
    private const val headerSize = 40
    fun decode(data: ByteArray, length: Int): SteamVoicePacket? {
        if (length < headerSize || data.copyOfRange(0, 4).decodeToString() != "SV01" || data[4].toInt() != version || data[5].toInt() != codecOpus) return null
        val buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)
        val rate = buffer.getInt(6)
        val channelCount = data[10].toInt() and 0xff
        val bitrate = buffer.getInt(12)
        val payloadLength = buffer.getShort(24).toInt() and 0xffff
        val frameMs = buffer.getShort(26).toInt() and 0xffff
        if (rate != sampleRate || channelCount != channels || frameMs !in supportedFrameMilliseconds) return null
        if (payloadLength + headerSize != length) return null
        val session = buffer.getInt(16).toLong() and 0xffffffffL
        val sequence = buffer.getInt(20).toLong() and 0xffffffffL
        val timestamp = buffer.getLong(32)
        return SteamVoicePacket(data[5].toInt(), rate, channelCount, bitrate, frameMs, session, sequence, data.copyOfRange(headerSize, length), data[28].toInt() and 0xff, timestamp)
    }
}

data class HeartbeatControl(val kind: Int, val session: Long, val sequence: Long, val timestampNs: Long) {
    fun encode(): ByteArray = ByteBuffer.allocate(SIZE).order(ByteOrder.BIG_ENDIAN).apply {
        put("SVHB".toByteArray()).put(SteamVoiceProtocol.version.toByte()).put(kind.toByte()).putShort(0)
        putInt(session.toInt()).putInt(sequence.toInt()).putLong(timestampNs).putLong(0)
    }.array()
    companion object {
        const val KIND_PING = 1; const val KIND_PONG = 2; const val SIZE = 32
        fun decode(data: ByteArray, length: Int): HeartbeatControl? {
            if (length != SIZE || data.copyOfRange(0, 4).decodeToString() != "SVHB" || data[4].toInt() != SteamVoiceProtocol.version) return null
            val kind = data[5].toInt() and 0xff
            if (kind != KIND_PING && kind != KIND_PONG) return null
            val b = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)
            return HeartbeatControl(kind, b.getInt(8).toLong() and 0xffffffffL, b.getInt(12).toLong() and 0xffffffffL, b.getLong(16))
        }
    }
}

