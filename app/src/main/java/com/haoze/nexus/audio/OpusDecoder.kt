package com.haoze.nexus.audio

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer

/** Platform Opus decoder. The desktop sends one elementary Opus frame per datagram. */
class OpusDecoder {
    private val codec: MediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)

    init {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, 48000, 2)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)
        codec.configure(format, null, null, 0)
        codec.start()
    }

    @Synchronized
    fun decode(frame: ByteArray, frameMs: Int): ByteArray? {
        val input = codec.dequeueInputBuffer(10_000)
        if (input < 0) return null
        codec.getInputBuffer(input)?.apply { clear(); put(frame) }
        codec.queueInputBuffer(input, 0, frame.size, 0L, 0)
        val info = MediaCodec.BufferInfo()
        val output = codec.dequeueOutputBuffer(info, 10_000)
        if (output < 0) return null
        val bytes = codec.getOutputBuffer(output)?.let { buffer ->
            buffer.position(info.offset); buffer.limit(info.offset + info.size)
            ByteArray(info.size).also { buffer.get(it) }
        }
        codec.releaseOutputBuffer(output, false)
        return bytes
    }

    fun release() {
        try { codec.stop() } catch (_: Exception) { }
        codec.release()
    }
}

