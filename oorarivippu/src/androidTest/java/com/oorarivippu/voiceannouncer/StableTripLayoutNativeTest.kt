package com.oorarivippu.voiceannouncer

import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap

/** Synthetic values only. Checks real layout geometry, not GPS or audible accuracy. */
@RunWith(AndroidJUnit4::class)
class StableTripLayoutNativeTest {
    @Test fun changingValuesKeepFieldPositionsInBothLanguagesAtNormalAndDoubleFont() {
        val i = InstrumentationRegistry.getInstrumentation()
        for (ta in listOf(false, true)) for (scale in listOf(1f, 2f)) {
            val activity = i.startActivitySync(Intent(i.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            val boxes = ConcurrentHashMap<Int, Rect>()
            val sample = mutableStateOf(0)
            val values = listOf(
                listOf("54.8 km/h", "CI town", "Unavailable", "121.91 km"),
                listOf("Speed not measured", "A much longer synthetic locality and district name", "2 seconds", "Unavailable"),
                listOf("0.0 km/h", "செயற்கை நீண்ட ஊர் மற்றும் மாவட்டத்தின் பெயர்", "கிடைக்கவில்லை", "12345.67 km"))
            try {
                i.runOnMainSync {
                    activity.setContent {
                        val density = LocalDensity.current.density
                        CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                            MaterialTheme {
                                key(ta, scale) {
                                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
                                        repeat(4) { index ->
                                            Box(Modifier.fillMaxWidth().onGloballyPositioned { boxes[index] = it.boundsInRoot() }) {
                                                StableTripField(if (ta) "நிலையான தலைப்பு $index" else "Fixed title $index",
                                                    values[sample.value][index], ta)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                SystemClock.sleep(800); i.waitForIdleSync()
                assertEquals(4, boxes.size)
                val original = boxes.toMap()
                for (next in 1..2) {
                    i.runOnMainSync { sample.value = next }
                    SystemClock.sleep(500); i.waitForIdleSync()
                    original.forEach { (index, bounds) ->
                        val current = boxes[index]!!
                        assertEquals("Field $index top moved, ta=$ta scale=$scale", bounds.top, current.top, 1f)
                        assertEquals("Field $index height changed, ta=$ta scale=$scale", bounds.height, current.height, 1f)
                        assertEquals(bounds.left, current.left, 1f)
                        assertEquals(bounds.width, current.width, 1f)
                    }
                }
            } finally { i.runOnMainSync { activity.finish() }; i.waitForIdleSync() }
        }
    }
}
