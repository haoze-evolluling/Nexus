package com.haoze.nexus.audio

import java.util.TreeMap

class PacketJitterBuffer(private val targetPackets: Int = 4) {
    sealed class Item { data class Packet(val value: NexusVoicePacket): Item(); data object Gap: Item() }
    private val packets = TreeMap<Long, NexusVoicePacket>()
    private var session = -1L
    private var nextSequence = -1L
    fun offer(packet: NexusVoicePacket) {
        if (packet.session != session) { packets.clear(); session = packet.session; nextSequence = packet.sequence }
        if (packet.sequence >= nextSequence) packets.putIfAbsent(packet.sequence, packet)
    }
    fun takeItem(): Item? {
        if (nextSequence < 0 || packets.size < targetPackets) return null
        val packet = packets.remove(nextSequence)
        if (packet == null) { nextSequence++; return Item.Gap }
        nextSequence++
        return Item.Packet(packet)
    }
    fun queuedPackets(): Int = packets.size
    fun expectedSequence(): Long = nextSequence
    fun clear() { packets.clear(); nextSequence = -1L; session = -1L }
}

