package com.haoze.nexus.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ConnectionProtocolTest {

    @Test
    fun testConnControlRoundTrip() {
        val original = ConnControl(
            kind = ConnControl.KIND_REQUEST,
            deviceId = "android-device-123",
            name = "Pixel Phone",
            nonce = 9876543210L
        )
        val encoded = original.encode()
        val decoded = ConnControl.decode(encoded, encoded.size)

        assertNotNull(decoded)
        assertEquals(ConnControl.KIND_REQUEST, decoded?.kind)
        assertEquals("android-device-123", decoded?.deviceId)
        assertEquals("Pixel Phone", decoded?.name)
        assertEquals(9876543210L, decoded?.nonce)
    }

    @Test
    fun testConnControlResponseRoundTrip() {
        val original = ConnControl(
            kind = ConnControl.KIND_RESPONSE,
            deviceId = "pc-uuid-42",
            allow = true,
            nonce = 123456789L
        )
        val encoded = original.encode()
        val decoded = ConnControl.decode(encoded, encoded.size)

        assertNotNull(decoded)
        assertEquals(ConnControl.KIND_RESPONSE, decoded?.kind)
        assertEquals("pc-uuid-42", decoded?.deviceId)
        assertTrue(decoded?.allow == true)
        assertEquals(123456789L, decoded?.nonce)
    }

    @Test
    fun testConnControlRejectsLegacyAndMissingNonce() {
        val nonce = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(12345L).array()
        val id = "device".toByteArray(Charsets.UTF_8)
        val name = "name".toByteArray(Charsets.UTF_8)

        // Legacy SVCR magic datagram
        val legacy = "SVCR".toByteArray(Charsets.UTF_8) + byteArrayOf(4, 1, 0, 0) + nonce + id + byteArrayOf(0) + name
        assertNull("Legacy SVCR magic must be rejected", ConnControl.decode(legacy, legacy.size))

        // Pre-nonce datagram (no 8-byte nonce)
        val noNonce = "NXCR".toByteArray(Charsets.UTF_8) + byteArrayOf(4, 1, 0, 0) + id + byteArrayOf(0) + name
        assertNull("Missing nonce datagram must be rejected", ConnControl.decode(noNonce, noNonce.size))
    }

    @Test
    fun testPacketsRejectLegacySvMagic() {
        // SettingsControl rejects SVCS
        val legacySettings = "SVCS".toByteArray() + byteArrayOf(4, 1, 0, 0) + ByteArray(32)
        assertNull("Legacy SVCS must be rejected", SettingsControl.decode(legacySettings, legacySettings.size))

        // TimeSyncControl rejects SVTS
        val legacyTimeSync = "SVTS".toByteArray() + byteArrayOf(4, 1, 0, 0) + ByteArray(32)
        assertNull("Legacy SVTS must be rejected", TimeSyncControl.decode(legacyTimeSync, legacyTimeSync.size))

        // HeartbeatControl rejects SVHB
        val legacyHeartbeat = "SVHB".toByteArray() + byteArrayOf(4, 1, 0, 0) + ByteArray(24)
        assertNull("Legacy SVHB must be rejected", HeartbeatControl.decode(legacyHeartbeat, legacyHeartbeat.size))

        // PeerCalibrationControl rejects SVAC
        val legacyCalibration = "SVAC".toByteArray() + byteArrayOf(4, 1, 0, 0) + ByteArray(40)
        assertNull("Legacy SVAC must be rejected", PeerCalibrationControl.decode(legacyCalibration, legacyCalibration.size))

        // NexusVoicePacket rejects SV01
        val legacyVoice = "SV01".toByteArray() + byteArrayOf(4, 1) + ByteArray(34)
        assertNull("Legacy SV01 must be rejected", NexusProtocol.decode(legacyVoice, legacyVoice.size))
    }

    @Test
    fun testStateTransitionsOnDisconnect() {
        // CONNECTED on LOCAL_DISCONNECT goes directly to IDLE
        assertEquals(ConnectionState.IDLE, nextConnectionState(ConnectionState.CONNECTED, ConnectionEvent.LOCAL_DISCONNECT))
        assertEquals(ConnectionState.IDLE, nextConnectionState(ConnectionState.CONNECTED, ConnectionEvent.REMOTE_BYE))

        // CONNECTED on HEARTBEAT_TIMEOUT goes to RECONNECTING
        assertEquals(ConnectionState.RECONNECTING, nextConnectionState(ConnectionState.CONNECTED, ConnectionEvent.HEARTBEAT_TIMEOUT))

        // RECONNECTING on AUTHORIZED goes to CONNECTED
        assertEquals(ConnectionState.CONNECTED, nextConnectionState(ConnectionState.RECONNECTING, ConnectionEvent.AUTHORIZED))

        // RECONNECTING on HANDSHAKE_TIMEOUT or DENIED goes to FAILED
        assertEquals(ConnectionState.FAILED, nextConnectionState(ConnectionState.RECONNECTING, ConnectionEvent.HANDSHAKE_TIMEOUT))
        assertEquals(ConnectionState.FAILED, nextConnectionState(ConnectionState.RECONNECTING, ConnectionEvent.DENIED))

        // RECONNECTING on LOCAL_DISCONNECT goes to IDLE
        assertEquals(ConnectionState.IDLE, nextConnectionState(ConnectionState.RECONNECTING, ConnectionEvent.LOCAL_DISCONNECT))
    }
}
