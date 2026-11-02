package com.universalrp.cleansweep

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalrp.cleansweep.data.*
import com.universalrp.cleansweep.notify.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.Instant

/** Synthetic fixtures only. No live weather, real alert, speech or phone accuracy claim. */
@RunWith(AndroidJUnit4::class)
class OfficialStatusNativeTest {
    private val c: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun waitFor(condition: () -> Boolean) {
        val until=SystemClock.elapsedRealtime()+15_000
        while(SystemClock.elapsedRealtime()<until) { if(condition())return;SystemClock.sleep(100) }
        assertTrue("Timed out waiting for foreground ownership",condition())
    }
    @Test fun isolatedPlatformPdfExtractionAndParsing() = runBlocking {
        val pdf = PdfDocument()
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(1200,1200,1).create())
        val paint=Paint().apply { textSize=13f }
        val lines=listOf("India Meteorological Department NATIONAL BULLETIN NO. 1",
            "TIME OF ISSUE: 1730 HOURS IST DATED:02.11.2026", "Cyclonic Storm \"TESTSTORM\" over sea.",
            "Forecast location and intensity are given in following table:",
            "02.11.26/1430 14.3/80.9 70-80 gusting to 90 Cyclonic Storm")
        lines.forEachIndexed { n,line -> page.canvas.drawText(line,25f,35f+n*25,paint) }
        pdf.finishPage(page)
        val out=ByteArrayOutputStream();pdf.writeTo(out);pdf.close()
        val text=OfficialCycloneClient.pdfText(c,out.toByteArray())
        assertNotNull("Isolated Android35 PDF text extraction",text)
        val bulletin=OfficialCyclonePolicy.imd(text!!,"https://rsmcnewdelhi.imd.gov.in/uploads/archive/47/47_synthetic.pdf",Instant.parse("2026-11-02T13:00:00Z").toEpochMilli())
        assertNotNull("Platform text order must satisfy actual bulletin parser",bulletin)
        assertEquals(80,bulletin!!.windHigh)
    }
    @Test fun invalidPdfCannotReachParser() = runBlocking { assertNull(OfficialCycloneClient.pdfText(c,"not a PDF".toByteArray())) }
    @Test fun foregroundServicesShareCardAndIndependentStop() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val activity=instrumentation.startActivitySync(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val monitor=Intent(c,ChargeMonitorService::class.java)
        val radio=Intent(c,RadioWatchService::class.java)
        c.startForegroundService(monitor);c.startForegroundService(radio)
        waitFor { RoutineStatus.activeOwners().containsAll(setOf(RoutineStatus.MONITOR,RoutineStatus.RADIO)) }
        val nm=c.getSystemService(NotificationManager::class.java)
        waitFor { nm.activeNotifications.any { it.id==RoutineStatus.ID } }
        assertEquals(1,nm.activeNotifications.count { it.id in setOf(RoutineStatus.ID,4211,280,281) })
        c.stopService(radio)
        waitFor { RoutineStatus.RADIO !in RoutineStatus.activeOwners() }
        assertTrue(RoutineStatus.MONITOR in RoutineStatus.activeOwners())
        assertTrue(nm.activeNotifications.any { it.id==RoutineStatus.ID })
        c.stopService(monitor)
        waitFor { RoutineStatus.MONITOR !in RoutineStatus.activeOwners() }
        waitFor { nm.activeNotifications.none { it.id==RoutineStatus.ID } }
        instrumentation.runOnMainSync { activity.finish() }
    }
}
