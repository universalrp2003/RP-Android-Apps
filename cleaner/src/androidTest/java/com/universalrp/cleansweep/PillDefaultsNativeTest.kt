package com.universalrp.cleansweep

import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.data.NewsPreferences
import com.universalrp.cleansweep.notify.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator synthetic values only; no real dual-SIM/OEM reception claim. */
@RunWith(AndroidJUnit4::class)
class PillDefaultsNativeTest : ExactCandidateNativeTest() {
    private val i get() = InstrumentationRegistry.getInstrumentation()
    private val c: Context get() = i.targetContext
    @Test fun missingNewsChoiceDefaultsToTranslationWithoutResettingExplicitChoices() {
        val prefs=c.getSharedPreferences("news_topics_283",0)
        val saved=prefs.getString("language",null)
        try {
            prefs.edit().remove("language").commit()
            assertEquals(NewsPreferences.Language.TAMIL_TRANSLATED,NewsPreferences.language(c))
            assertTrue(NewsPreferences.tamil(c));assertTrue(NewsPreferences.translate(c))
            NewsPreferences.Language.values().forEach {
                prefs.edit().putString("language",it.name).commit()
                assertEquals(it,NewsPreferences.language(c))
            }
        } finally { prefs.edit().putString("language",saved).commit() }
    }
    @Test fun firstSimIsDefaultAndSavedBothOrOffRemainSelected() {
        val prefs=c.getSharedPreferences(StatusPill.PREFS,0)
        val saved=prefs.getString(StatusPill.KEY_SIGNAL_MODE,null)
        try {
            prefs.edit().remove(StatusPill.KEY_SIGNAL_MODE).commit()
            assertEquals(PillSignalPolicy.Mode.FIRST,StatusPill.signalMode(c))
            listOf(PillSignalPolicy.Mode.BOTH,PillSignalPolicy.Mode.OFF).forEach {
                StatusPill.setSignalMode(c,it);assertEquals(it,StatusPill.signalMode(c))
            }
        } finally { prefs.edit().putString(StatusPill.KEY_SIGNAL_MODE,saved).commit() }
    }
    @Test fun actualOverlayRendersIndependentColouredSimValuesAndRetainsDataAndMeter() {
        assertTrue("CI must grant existing overlay permission",StatusPill.canDraw(c))
        val prefs=c.getSharedPreferences(StatusPill.PREFS,0)
        val saved=prefs.getString(StatusPill.KEY_SIGNAL_MODE,null)
        try {
            StatusPill.setSignalMode(c,PillSignalPolicy.Mode.BOTH)
            StatusPill.update(c,"12 MB",simSignals=listOf(
                PillSignalPolicy.Sample(1,-95,"LTE_RSRP"),PillSignalPolicy.Sample(2,-70,"LTE_RSRP")))
            i.waitForIdleSync()
            i.runOnMainSync {
                val field=StatusPill::class.java.getDeclaredField("rootContainer").apply { isAccessible=true }
                val root=field.get(StatusPill) as LinearLayout
                val text=(root.getChildAt(0) as TextView).text as Spanned
                assertTrue(text.toString().contains("12 MB"))
                // 2.95: compact pill — coloured percentages only, no slot labels and no ≈.
                assertTrue(text.toString().contains("47%"))
                assertTrue(text.toString().contains("73%"))
                assertFalse(text.toString().contains("≈"))
                assertFalse(text.toString().contains("S1"))
                assertFalse(text.toString().contains("S2"))
                // 3.00: the owner asked for "no warning red color on pill just show count only".
                // The two SIM readings must still be rendered as independent numbers, but they
                // are no longer amber/green verdicts: every span uses the one steady colour.
                // This still fails if the values are dropped or merged, which is the point.
                val colors=text.getSpans(0,text.length,ForegroundColorSpan::class.java).map { it.foregroundColor }
                assertFalse("the pill must not colour a SIM reading amber",colors.contains(Color.parseColor("#FFD600")))
                assertFalse("the pill must not colour a SIM reading green",colors.contains(Color.parseColor("#00E676")))
                assertFalse("the pill must not colour a SIM reading red",colors.contains(Color.parseColor("#FF5252")))
                assertTrue("both readings must use the steady count colour",
                    colors.isNotEmpty() && colors.all { it==Color.parseColor("#E8FBFF") })
                assertEquals(android.view.View.VISIBLE,root.getChildAt(1).visibility)
            }
            StatusPill.setSignalMode(c,PillSignalPolicy.Mode.OFF)
            i.waitForIdleSync()
            i.runOnMainSync {
                val field=StatusPill::class.java.getDeclaredField("dataTextView").apply { isAccessible=true }
                assertEquals("12 MB",(field.get(StatusPill) as TextView).text.toString())
            }
        } finally {
            prefs.edit().putString(StatusPill.KEY_SIGNAL_MODE,saved).commit()
            StatusPill.remove()
            i.waitForIdleSync()
        }
    }
    @Test fun actualOverlayOmitsUnavailableSignalsWithoutLosingDataOrMeter() {
        assertTrue(StatusPill.canDraw(c))
        val prefs=c.getSharedPreferences(StatusPill.PREFS,0)
        val saved=prefs.getString(StatusPill.KEY_SIGNAL_MODE,null)
        try {
            StatusPill.setSignalMode(c,PillSignalPolicy.Mode.BOTH)
            listOf(listOf(PillSignalPolicy.Sample(1,null,null),PillSignalPolicy.Sample(2,-95,"LTE_RSRP")),
                emptyList()).forEach { samples ->
                StatusPill.update(c,"12 MB",simSignals=samples)
                i.waitForIdleSync()
                i.runOnMainSync {
                    val field=StatusPill::class.java.getDeclaredField("rootContainer").apply { isAccessible=true }
                    val root=field.get(StatusPill) as LinearLayout
                    val text=(root.getChildAt(0) as TextView).text.toString()
                    assertTrue(text.contains("12 MB"))
                    assertFalse(text.contains("S1"));assertFalse(text.contains("—"))
                    if (samples.isEmpty()) assertEquals("12 MB",text) else {
                        assertTrue(text.contains("47%"));assertFalse(text.contains("≈"));assertFalse(text.contains("S2"))
                    }
                    assertEquals(android.view.View.VISIBLE,root.getChildAt(1).visibility)
                }
            }
        } finally {
            prefs.edit().putString(StatusPill.KEY_SIGNAL_MODE,saved).commit()
            StatusPill.remove();i.waitForIdleSync()
        }
    }

}
