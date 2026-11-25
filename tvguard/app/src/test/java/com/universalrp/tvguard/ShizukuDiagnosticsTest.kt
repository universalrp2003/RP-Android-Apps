package com.universalrp.tvguard

import com.universalrp.tvguard.diagnostics.ShizukuDiagnostics
import org.junit.Assert.*
import org.junit.Test

class ShizukuDiagnosticsTest {
    @Test fun parsesOnlyExplicitPidRssNameTable() {
        val rows = ShizukuDiagnostics.parsePs(" PID RSS NAME\n 123 456 com.example\n 124 100 com.example:remote\n")!!
        assertEquals(2, rows.size)
        assertEquals(456L, rows[0].rssKb)
        assertEquals("com.example:remote", rows[1].name)
        assertEquals(1, ShizukuDiagnostics.matchedPackageCount(rows, setOf("com.example")))
        assertNull(ShizukuDiagnostics.matchedPackageCount(rows, null))
        assertNull(ShizukuDiagnostics.matchedPackageCount(null, setOf("com.example")))
        assertNull(ShizukuDiagnostics.parsePs("USER PID NAME\n u0 1 foo"))
        assertNull(ShizukuDiagnostics.parsePs("PID RSS NAME\n 2 bad pkg"))
        assertNull(ShizukuDiagnostics.parsePs("PID RSS NAME\n"))
        assertNull(ShizukuDiagnostics.parsePs("PID RSS NAME\n1 8 a\n1 9 b"))
    }
}
