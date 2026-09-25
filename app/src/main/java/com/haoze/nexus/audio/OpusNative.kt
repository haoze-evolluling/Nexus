package com.haoze.nexus.audio

object OpusNative {
    init { System.loadLibrary("nexus_native") }
    external fun createEncoder(sampleRate: Int, channels: Int, bitrate: Int): Long
    external fun encode(handle: Long, pcm16le: ByteArray, frameSamples: Int): ByteArray?
    external fun setEncoderBitrate(handle: Long, bitrate: Int): Boolean
    external fun destroyEncoder(handle: Long)
    external fun createDecoder(sampleRate: Int, channels: Int): Long
    external fun decode(handle: Long, opusFrame: ByteArray, decodeFec: Boolean): ByteArray?
    external fun decodePlc(handle: Long, frameSamples: Int): ByteArray?
    external fun destroyDecoder(handle: Long)
}

