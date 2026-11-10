package com.universalrp.tvguard

import com.universalrp.tvguard.debloat.BloatCatalog
import com.universalrp.tvguard.debloat.BloatSafetyLevel
import com.universalrp.tvguard.debloat.DebloatEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BloatCatalogTest {

    @Test
    fun testKnownBloatwareClassification() {
        val (levelHd, _) = BloatCatalog.classifyPackage("com.hdstreamz.app", false)
        assertEquals(BloatSafetyLevel.SAFE_TO_REMOVE, levelHd)

        val (levelHdStealth, _) = BloatCatalog.classifyPackage("topic.election.sector", false)
        assertEquals(BloatSafetyLevel.SAFE_TO_REMOVE, levelHdStealth)

        val (levelApkmody, _) = BloatCatalog.classifyPackage("io.apkmody.sai", false)
        assertEquals(BloatSafetyLevel.SAFE_TO_REMOVE, levelApkmody)

        val (levelUptodown, _) = BloatCatalog.classifyPackage("com.uptodown.tv", false)
        assertEquals(BloatSafetyLevel.SAFE_TO_REMOVE, levelUptodown)

        val (levelAdups, _) = BloatCatalog.classifyPackage("com.adups.fota", true)
        assertEquals(BloatSafetyLevel.SAFE_TO_REMOVE, levelAdups)
    }

    @Test
    fun testCoreSystemProtection() {
        val (levelSettings, _) = BloatCatalog.classifyPackage("com.android.tv.settings", true)
        assertEquals(BloatSafetyLevel.CORE_SYSTEM_PROTECTED, levelSettings)

        val (levelSystemUi, _) = BloatCatalog.classifyPackage("com.android.systemui", true)
        assertEquals(BloatSafetyLevel.CORE_SYSTEM_PROTECTED, levelSystemUi)

        val (levelLock, _) = BloatCatalog.classifyPackage("com.rjio.lockunlock", true)
        assertEquals(BloatSafetyLevel.CORE_SYSTEM_PROTECTED, levelLock)

        val (levelDfu, _) = BloatCatalog.classifyPackage("com.google.android.tv.dfuservice", true)
        assertEquals(BloatSafetyLevel.CORE_SYSTEM_PROTECTED, levelDfu)
    }

    @Test
    fun testAirtelAndJioBloatware() {
        val (levelAirtel, descAirtel) = BloatCatalog.classifyPackage("com.airtel.tv", true)
        assertEquals(BloatSafetyLevel.SAFE_TO_REMOVE, levelAirtel)
        assertTrue(descAirtel.contains("Airtel"))

        val (levelJio, descJio) = BloatCatalog.classifyPackage("com.jio.jiogames", true)
        assertEquals(BloatSafetyLevel.SAFE_TO_REMOVE, levelJio)
        assertTrue(descJio.contains("JioGames"))
    }

    @Test
    fun testUtilityWatchlist() {
        val (levelBare, _) = BloatCatalog.classifyPackage("net.i.akihiro.barelauncher", false)
        assertEquals(BloatSafetyLevel.CAUTION_USER_CHOICE, levelBare)

        val (levelBareAlt, _) = BloatCatalog.classifyPackage("com.bare.launcher", false)
        assertEquals(BloatSafetyLevel.CAUTION_USER_CHOICE, levelBareAlt)

        val (levelSmartTube, _) = BloatCatalog.classifyPackage("org.smarttube.stable", false)
        assertEquals(BloatSafetyLevel.CAUTION_USER_CHOICE, levelSmartTube)

        val (levelSunNxt, _) = BloatCatalog.classifyPackage("com.suntv.sunnxt.mitv", false)
        assertEquals(BloatSafetyLevel.CAUTION_USER_CHOICE, levelSunNxt)

        val (levelCx, _) = BloatCatalog.classifyPackage("com.cxinventor.file.explorer", false)
        assertEquals(BloatSafetyLevel.CAUTION_USER_CHOICE, levelCx)
    }

    @Test
    fun testAdbScriptGeneration() {
        val packages = listOf("com.hdstreamz.app", "com.uptodown.tv")
        val script = DebloatEngine.generateAdbScript(packages)
        assertTrue(script.contains("adb connect"))
        assertTrue(script.contains("adb shell pm uninstall -k --user 0 com.hdstreamz.app"))
        assertTrue(script.contains("adb shell pm uninstall -k --user 0 com.uptodown.tv"))
    }
}
