package com.haoze.nexus.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashBreadcrumbsTest {

    @Test
    fun recordAndRetrieveBreadcrumbs() {
        CrashBreadcrumbs.record("TEST", "Test message 1", "DEBUG")
        CrashBreadcrumbs.record("TEST", "Test message 2", "INFO")

        val breadcrumbs = CrashBreadcrumbs.getBreadcrumbs()
        assertTrue(breadcrumbs.size >= 2)
        val lastTwo = breadcrumbs.takeLast(2)
        assertEquals("Test message 1", lastTwo[0].message)
        assertEquals("DEBUG", lastTwo[0].level)
        assertEquals("Test message 2", lastTwo[1].message)
        assertEquals("INFO", lastTwo[1].level)
    }

    @Test
    fun formatAllProducesFormattedOutput() {
        CrashBreadcrumbs.record("FORMAT_TEST", "Formatting check")
        val formatted = CrashBreadcrumbs.formatAll()
        assertTrue(formatted.contains("[INFO] [FORMAT_TEST] Formatting check"))
    }

    @Test
    fun ringBufferCapacityDoesNotExceedMax() {
        // Record more than MAX_BREADCRUMBS (150)
        for (i in 1..200) {
            CrashBreadcrumbs.record("BURST", "Burst message $i")
        }
        val breadcrumbs = CrashBreadcrumbs.getBreadcrumbs()
        assertEquals(150, breadcrumbs.size)
        // Last message must be message 200
        assertEquals("Burst message 200", breadcrumbs.last().message)
        // First message in buffer must be message 51
        assertEquals("Burst message 51", breadcrumbs.first().message)
    }
}
