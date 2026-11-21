package com.universalrp.launcher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Writes only synthetic data using Android APIs compatible with the published 1.7 target. */
@RunWith(AndroidJUnit4::class)
class SeedUpgradeFixtureTest {
    @Test fun seed() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val values = listOf("Test Alpha\u001f1110001", "Test Beta\u001f1110002", "Test Long Name Example\u001f1110003", "Test Delta\u001f1110004").joinToString("\u001e")
        assertTrue(ctx.getSharedPreferences("launcher_favourites", 0).edit().putString("list", values).commit())
        assertTrue(ctx.getSharedPreferences("launcher", 0).edit().putBoolean("asked_for_home", true).commit())
    }
}
