package com.haoze.nexus.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 连接控制报文（SVCR），与桌面端 internal/protocol/conn.go 保持一致。
 * 请求方携带稳定设备 ID 与名称；响应方返回允许/拒绝；断开用于即时通知对端。
 */
data class ConnControl(val kind: Int, val deviceId: String, val name: String = "", val allow: Boolean = false, val nonce: Long = 0L) {

    fun encode(): ByteArray {
        val id = deviceId.toByteArray(Charsets.UTF_8)
        require(id.isNotEmpty() && id.size <= MAX_DEVICE_ID_LEN) { "invalid device id length" }
        val nameBytes = truncateUtf8(name, MAX_NAME_LEN)
        val prefix = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(nonce).array()
        val body: ByteArray = when (kind) {
            KIND_REQUEST -> prefix + id + byteArrayOf(0) + nameBytes
            KIND_RESPONSE -> prefix + id + byteArrayOf(0, if (allow) ALLOW else DENY)
            KIND_BYE -> prefix + id
            else -> throw IllegalArgumentException("unknown conn control kind $kind")
        }
        return ByteBuffer.allocate(HEADER_SIZE + body.size).order(ByteOrder.BIG_ENDIAN).apply {
            put(MAGIC.toByteArray(Charsets.UTF_8))
            put(NexusProtocol.version.toByte())
            put(kind.toByte())
            putShort(0)
            put(body)
        }.array()
    }

    companion object {
        const val KIND_REQUEST = 1
        const val KIND_RESPONSE = 2
        const val KIND_BYE = 3
        const val MAX_DEVICE_ID_LEN = 64
        const val MAX_NAME_LEN = 64
        private const val HEADER_SIZE = 8
        private const val MAGIC = "NXCR"
        private const val ALLOW: Byte = 1
        private const val DENY: Byte = 2

        fun decode(data: ByteArray, length: Int): ConnControl? {
            if (length < HEADER_SIZE) return null
            val magic = String(data, 0, 4, Charsets.UTF_8)
            if (magic != MAGIC && magic != "SVCR") return null
            if (data[4].toInt() != NexusProtocol.version) return null
            val kind = data[5].toInt() and 0xff
            if (length < HEADER_SIZE + 8) return null
            val nonce = ByteBuffer.wrap(data, HEADER_SIZE, 8).order(ByteOrder.BIG_ENDIAN).long
            val bodyStart = HEADER_SIZE + 8
            val nul = findNul(data, bodyStart, length)
            fun field(start: Int, end: Int) = String(data, start, end - start, Charsets.UTF_8)
            return when (kind) {
                KIND_REQUEST -> {
                    if (nul <= bodyStart) return null
                    val id = field(bodyStart, nul)
                    if (!validId(id)) return null
                    ConnControl(kind, id, field(nul + 1, length), nonce = nonce)
                }
                KIND_RESPONSE -> {
                    if (nul <= bodyStart || length != nul + 2) return null
                    val id = field(bodyStart, nul)
                    if (!validId(id)) return null
                    when (data[length - 1]) {
                        ALLOW -> ConnControl(kind, id, allow = true, nonce = nonce)
                        DENY -> ConnControl(kind, id, allow = false, nonce = nonce)
                        else -> null
                    }
                }
                KIND_BYE -> {
                    val id = field(bodyStart, length)
                    if (!validId(id) || nul >= 0) return null
                    ConnControl(kind, id, nonce = nonce)
                }
                else -> null
            }
        }

        private fun validId(id: String): Boolean {
            val bytes = id.toByteArray(Charsets.UTF_8)
            return bytes.isNotEmpty() && bytes.size <= MAX_DEVICE_ID_LEN
        }

        /** 按字符边界截断 UTF-8 文本，避免切出无效字节序列。 */
        private fun truncateUtf8(text: String, maxBytes: Int): ByteArray {
            val bytes = text.toByteArray(Charsets.UTF_8)
            if (bytes.size <= maxBytes) return bytes
            var end = maxBytes
            while (end > 0 && (bytes[end].toInt() and 0xc0) == 0x80) end--
            return bytes.copyOf(end)
        }

        private fun findNul(data: ByteArray, from: Int, to: Int): Int {
            for (i in from until to) {
                if (data[i].toInt() == 0) return i
            }
            return -1
        }
    }
}

