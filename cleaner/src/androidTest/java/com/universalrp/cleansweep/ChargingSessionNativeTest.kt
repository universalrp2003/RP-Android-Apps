package com.universalrp.cleansweep

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.voice.ChargingVoiceSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic session fixtures on the isolated CI emulator; no real charger or audible voice claim. */
@RunWith(AndroidJUnit4::class)
class ChargingSessionNativeTest : ExactCandidateNativeTest() {
    @Test fun baselineIsPrivateDeduplicatedAndNeverJoinedAcrossBoots() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val p = app.getSharedPreferences("liveguard_charge_voice_session", Context.MODE_PRIVATE)
        p.edit().clear().commit()
        val boot = Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT)
        ChargingVoiceSession.connected(app, "synthetic-one")
        assertTrue(p.edit().putInt("start_percent", 20).putLong("start_elapsed", SystemClock.elapsedRealtime() - 1_000)
            .putInt("start_boot", boot).commit())
        assertTrue(ChargingVoiceSession.hasActiveBaseline(app, "synthetic-one"))
        assertFalse(ChargingVoiceSession.hasActiveBaseline(app, "wrong-token"))
        val first = requireNotNull(ChargingVoiceSession.disconnected(app, "synthetic-one"))
        assertEquals(20, first.start)
        assertTrue(requireNotNull(first.elapsedMs) >= 1_000)
        assertNull(ChargingVoiceSession.disconnected(app, "synthetic-one"))
        assertFalse(ChargingVoiceSession.hasActiveBaseline(app, "synthetic-one"))
        ChargingVoiceSession.connected(app, "synthetic-two")
        assertTrue(p.edit().putInt("start_percent", 20).putInt("start_boot", boot + 1).commit())
        assertFalse(ChargingVoiceSession.hasActiveBaseline(app, "synthetic-two"))
        val afterBoot = requireNotNull(ChargingVoiceSession.disconnected(app, "synthetic-two"))
        assertNull(afterBoot.start)
        assertNull(afterBoot.elapsedMs)
        p.edit().clear().commit()
    }
}
