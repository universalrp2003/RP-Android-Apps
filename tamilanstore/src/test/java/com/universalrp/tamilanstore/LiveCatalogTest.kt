package com.universalrp.tamilanstore

import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LiveCatalogTest {
    private fun raw() = File("src/main/assets/catalog.json").readText()
    private fun changed(edit: (JSONObject) -> Unit): String {
        val root = JSONObject(raw()); edit(root.getJSONArray("apps").getJSONObject(0)); return root.toString()
    }
    @Test fun acceptsUnchangedSnapshot() {
        assertEquals(15, LiveCatalog.validate(raw(), Catalog.parse(raw())).size)
    }
    /** A feed regenerated before this build shipped still lists 13 apps; it must not be rejected. */
    @Test fun acceptsShorterFeedAndKeepsTheAppsThisBuildShips() {
        val root = JSONObject(raw())
        val apps = root.getJSONArray("apps")
        val kept = (0 until apps.length()).filter { apps.getJSONObject(it).getString("id") != "appdisabler" }
        val shortList = org.json.JSONArray()
        kept.forEach { shortList.put(apps.getJSONObject(it)) }
        root.put("apps", shortList)
        val merged = LiveCatalog.validate(root.toString(), Catalog.parse(raw()))
        assertEquals(15, merged.size)
        assertTrue(merged.any { it.id == "appdisabler" })
    }
    @Test fun rejectsUnknownOrRolledBackFeed() {
        val baseline = Catalog.parse(raw())
        val unknown = JSONObject(raw())
        val extra = JSONObject().put("id", "intruder").put("name", "x").put("tamilName", "x")
            .put("packageName", "com.attacker.intruder").put("version", "1.0")
            .put("url", "https://github.com/universalrp2003/RP-Android-Apps/releases/download/x-v1.0/x-v1.0.apk")
            .put("size", 1).put("sha256", "a".repeat(64))
            .put("releaseUrl", "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/x-v1.0")
        unknown.getJSONArray("apps").put(extra)
        try { LiveCatalog.validate(unknown.toString(), baseline); fail("unknown app accepted") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun rejectsIdentityRollbackAndSameVersionMutation() {
        val baseline = Catalog.parse(raw())
        for (bad in listOf(
            changed { it.put("packageName", "com.attacker.app") },
            changed { it.put("version", "0.1") },
            changed { it.put("sha256", "a".repeat(64)) },
            changed { it.put("url", it.getString("url").replace("github.com", "user@github.com")) }
        )) {
            try { LiveCatalog.validate(bad, baseline); fail("invalid feed accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
