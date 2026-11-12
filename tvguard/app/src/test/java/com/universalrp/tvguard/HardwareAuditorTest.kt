package com.universalrp.tvguard

import com.universalrp.tvguard.hardware.HardwareAuditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareAuditorTest {

    @Test
    fun testParseMemInfoMock() {
        val parsed = HardwareAuditor.parseProcMeminfo()
        // Running on host/sandbox Linux
        // Should parse MemTotal, MemFree, etc. from real Linux /proc/meminfo
        assertTrue(parsed.containsKey("MemTotal"))
        assertTrue((parsed["MemTotal"] ?: 0L) > 0L)
    }

    @Test
    fun testParseCpuInfoMock() {
        val parsed = HardwareAuditor.parseProcCpuinfo()
        // Should parse at least processor or CPU info from Linux
        assertTrue(parsed.isNotEmpty())
    }

    @Test
    fun testProcPartitionsMock() {
        val partitions = HardwareAuditor.parseProcPartitions()
        // If /proc/partitions exists on host, it will parse partitions
        // Doesn't throw
        assertTrue(partitions.size >= 0)
    }
}
