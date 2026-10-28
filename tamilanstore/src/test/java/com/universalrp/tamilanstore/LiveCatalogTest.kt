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
        assertEquals(13, LiveCatalog.validate(raw(), Catalog.parse(raw())).size)
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
