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
            "It lay centred at 1430 hours IST of today, the 2nd November 2026 near latitude 14.3N and longitude 80.9E.",
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
    @Test fun restoredThreeSegmentsVisibleActiveAndIdle() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val density=c.resources.displayMetrics.density
            val w=(34*density).toInt();val h=(14*density).toInt()
            val view=NetworkLedMeterView(c).apply { animStep=2;isDownloadActive=true;isUploadActive=true }
            view.layout(0,0,w,h)
            val bitmap=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888)
            fun renderAndCheck(active:Boolean) {
                view.isDownloadActive=active;view.isUploadActive=active
                bitmap.eraseColor(android.graphics.Color.BLACK)
                view.draw(android.graphics.Canvas(bitmap))
                val start=10.5f*density;val gap=1.5f*density
                val segment=(w-start-2*gap)/3
                for(row in 0..1) for(i in 0..2) {
                    val pixel=bitmap.getPixel((start+i*(segment+gap)+segment/2).toInt(),(h*(row+.5f)/2).toInt())
                    val brightness=android.graphics.Color.red(pixel)+android.graphics.Color.green(pixel)+android.graphics.Color.blue(pixel)
                    if(active) assertTrue("All six active segments visible",brightness>250)
                    else assertTrue("Idle tracks remain visible, not falsely active",brightness in 100..200)
                }
            }
            renderAndCheck(true);renderAndCheck(false);bitmap.recycle()
        }
    }
    @Test fun foregroundServicesShareCardAndIndependentStop() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        // Fresh targetSdk33+ installs may run FGS without permission, but Android
        // hides their shade cards. Grant only in this synthetic emulator test.
        instrumentation.uiAutomation.grantRuntimePermission(c.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,
            c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
        val nm=c.getSystemService(NotificationManager::class.java)
        assertTrue("Notification permission is a required test precondition", nm.areNotificationsEnabled())
        val activity=instrumentation.startActivitySync(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val monitor=Intent(c,ChargeMonitorService::class.java)
        val radio=Intent(c,RadioWatchService::class.java)
        try {
            c.startForegroundService(monitor);c.startForegroundService(radio)
            waitFor { RoutineStatus.activeOwners().containsAll(setOf(RoutineStatus.MONITOR,RoutineStatus.RADIO)) }
            waitFor { nm.activeNotifications.any { it.id==RoutineStatus.ID } }
            assertEquals(1,nm.activeNotifications.count { it.id in setOf(RoutineStatus.ID,4211,280,281) })
            c.stopService(radio)
            waitFor { RoutineStatus.RADIO !in RoutineStatus.activeOwners() }
            assertTrue(RoutineStatus.MONITOR in RoutineStatus.activeOwners())
            assertTrue(nm.activeNotifications.any { it.id==RoutineStatus.ID })
            c.stopService(monitor)
            waitFor { RoutineStatus.MONITOR !in RoutineStatus.activeOwners() }
            waitFor { nm.activeNotifications.none { it.id==RoutineStatus.ID } }
        } finally {
            c.stopService(radio);c.stopService(monitor)
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
    @Test fun weatherWidgetShowsVerifiedReportWithoutBrandingOrButtonOverlap() {
        val prefs = c.getSharedPreferences("cleansweep_widget_views", 0)
        val home = WeatherAlertStore.savedPlace(c) ?: error("Native script must supply a synthetic home")
        val news = NewsDisasterUpdates.enabled(c)
        val now = System.currentTimeMillis()
        try {
            NewsDisasterUpdates.setEnabled(c, true)
            prefs.edit().putString("profile_987654", "WEATHER").commit()
            com.universalrp.cleansweep.widget.DisasterWidgetCache.save(c,
                listOf(NewsDisasterPolicy.Event(987654,"TC","CI TEST",now,50.0,now,100.0)),
                NewsDisasterPolicy.Origin(home.latitude,home.longitude,false,now), now)
            val options = android.os.Bundle().apply { putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,250) }
            val weather = WeatherAlertStore.CachedWidgetWeather(true,home.name,"CLEAR_OR_NORMAL",null,27.0,14.0,"TOMORROW_IO_1M",now)
            val companion = com.universalrp.cleansweep.widget.CleanSweepWidget.Companion
            val method = companion.javaClass.declaredMethods.single { it.name == "build" && it.parameterTypes.size == 8 }.apply { isAccessible = true }
            val remote = method.invoke(companion,c,987654,options,null,null,null,weather,now) as android.widget.RemoteViews
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val view = remote.apply(c,android.widget.FrameLayout(c))
                val density = c.resources.displayMetrics.density
                view.measure(android.view.View.MeasureSpec.makeMeasureSpec((400*density).toInt(),android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec((250*density).toInt(),android.view.View.MeasureSpec.EXACTLY))
                view.layout(0,0,view.measuredWidth,view.measuredHeight)
                val details = view.findViewById<android.widget.TextView>(R.id.widget_data)
                val report = view.findViewById<android.widget.TextView>(R.id.widget_disaster)
                val controls = view.findViewById<android.view.View>(R.id.widget_mode).parent as android.view.View
                assertFalse(details.text.contains("Tomorrow.io"))
                assertTrue(report.text.contains("CI TEST") && report.text.contains("GDACS/JTWC"))
                assertTrue(report.text.contains("not a local impact prediction"))
                assertEquals(android.view.View.VISIBLE,report.visibility)
                assertTrue("Report must not overlap controls",(report.parent as android.view.View).top + report.bottom <= controls.top)
                assertTrue("Controls remain inside widget",controls.bottom <= view.measuredHeight)
            }
        } finally {
            prefs.edit().remove("profile_987654").commit()
            c.getSharedPreferences(com.universalrp.cleansweep.widget.DisasterWidgetCache.PREFS,0).edit().clear().commit()
            NewsDisasterUpdates.setEnabled(c,news)
        }
    }
}
