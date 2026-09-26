package com.haoze.nexus.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

/**
 * Factory for creating [AudioTrack] instances configured for low-latency PCM playback.
 */
object AudioTrackFactory {
    private const val TAG = "AudioTrackFactory"

    fun createTrack(): AudioTrack {
        val min = AudioTrack.getMinBufferSize(
            NexusProtocol.sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        check(min > 0) { "AudioTrack buffer size unavailable: $min" }
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(NexusProtocol.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(min * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build().also {
                it.play()
                Log.i(TAG, "AudioTrack started buffer=$min configured=${min * 2} state=${it.state}")
            }
    }
}
