package com.haoze.nexus.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class TombstoneParserTest {

    @Test
    fun parseEmptyBytesReturnsEmptyMessage() {
        val result = TombstoneParser.parse(ByteArray(0))
        assertEquals("(Empty tombstone trace)", result)
    }

    @Test
    fun parseStreamReturnsResult() {
        val input = "pid: 1234, tid: 5678, name: main\nSIGSEGV".toByteArray(Charsets.UTF_8)
        val stream = ByteArrayInputStream(input)
        val result = TombstoneParser.parse(stream)
        assertTrue(result.contains("SIGSEGV"))
    }

    @Test
    fun sanitizePlainTextTombstoneTruncatesMemoryDump() {
        val raw = buildString {
            append("pid: 1234, tid: 5678\n")
            append("Signal: 11 (SIGSEGV)\n")
            append("memory map:\n")
            for (i in 0 until 50) {
                append("00000000-00001000 r-xp 00000000\n")
            }
        }.toByteArray(Charsets.UTF_8)

        val result = TombstoneParser.parse(raw)
        assertTrue(result.contains("Signal: 11 (SIGSEGV)"))
        assertTrue(result.contains("(Excessive memory dumps truncated)"))
        assertFalse(result.contains("00000000-00001000"))
    }

    @Test
    fun wireReaderVarintDecoding() {
        // Encode 300 in varint: 300 = 0x12C -> 0xAC 0x02
        val bytes = byteArrayOf(0xAC.toByte(), 0x02.toByte())
        val res = TombstoneWireReader.readVarint(bytes, 0)
        assertTrue(res != null)
        assertEquals(300L, res?.value)
        assertEquals(2, res?.nextOffset)
    }

    @Test
    fun wireReaderIsLikelyProtobuf() {
        val textBytes = "pid: 1234, tid: 5678\n".toByteArray(Charsets.UTF_8)
        assertFalse(TombstoneWireReader.isLikelyProtobuf(textBytes))

        // Tag 0x08 (field 1, wire type 0) is typical protobuf header
        val protoBytes = byteArrayOf(0x08.toByte(), 0x01.toByte())
        assertTrue(TombstoneWireReader.isLikelyProtobuf(protoBytes))
    }

    @Test
    fun backtraceFrameFormatting() {
        val frame = BacktraceFrame(
            pc = 0x7fa1b2c000L,
            functionName = "opus_decode",
            functionOffset = 0x48L,
            fileName = "/system/lib64/libopus.so"
        )
        val formatted = frame.format(0)
        assertTrue(formatted.contains("#00 pc 0000007fa1b2c000"))
        assertTrue(formatted.contains("/system/lib64/libopus.so"))
        assertTrue(formatted.contains("(opus_decode+72)"))
    }

    @Test
    fun parsedTombstoneFormatting() {
        val tombstone = ParsedTombstone(
            arch = "arm64",
            pid = 1234,
            tid = 1234,
            processName = "com.haoze.nexus",
            signalNumber = 11,
            signalName = "SIGSEGV",
            signalCode = 1,
            signalCodeName = "SEGV_MAPERR",
            faultAddress = 0xdeadbeefL,
            hasFaultAddress = true,
            abortMessage = "Null pointer dereference in native code"
        )
        val thread = ThreadInfo(
            tid = 1234,
            name = "main"
        )
        thread.frames.add(
            BacktraceFrame(pc = 0x123456L, fileName = "libopus_jni.so", functionName = "nativeInit")
        )
        tombstone.threads[1234] = thread

        val report = tombstone.format()
        assertTrue(report.contains("ABI: 'arm64'"))
        assertTrue(report.contains("Process: com.haoze.nexus (PID: 1234, TID: 1234, UID: 0)"))
        assertTrue(report.contains("Signal: 11 (SIGSEGV), SEGV_MAPERR, fault addr 0xdeadbeef"))
        assertTrue(report.contains("Abort Message: 'Null pointer dereference in native code'"))
        assertTrue(report.contains("Crashing Thread: 1234 (main)"))
        assertTrue(report.contains("libopus_jni.so"))
    }
}
