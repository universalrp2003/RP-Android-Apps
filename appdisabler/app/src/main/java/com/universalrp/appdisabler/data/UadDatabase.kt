package com.universalrp.appdisabler.data

import android.content.Context
import org.json.JSONObject

/**
 * Full UAD database (2394 entries) from Universal Android Debloater (0x192) — GPL-3.0
 * Source: resources/assets/uad_lists.json → appdisabler/app/src/main/assets/uad_lists.json
 * Attribution: appdisabler/UAD_ATTRIBUTION.md
 * Mapping: Recommended -> SAFE, Advanced/Expert -> CAUTION, Unsafe -> CRITICAL
 * Disable-only: never auto-acts on CRITICAL
 */
data class UadEntry(
    val packageName: String,
    val category: DisableCategory,
    val description: String,
    val oem: String = "generic",
    val removal: String = "",
)

object UadDatabase {

    private var cache: Map<String, UadEntry>? = null

    fun load(context: Context): Map<String, UadEntry> {
        cache?.let { return it }
        return try {
            val json = context.assets.open("uad_lists.json").bufferedReader().readText()
            val obj = JSONObject(json)
            val map = mutableMapOf<String, UadEntry>()
            obj.keys().forEach { pkg ->
                val e = obj.getJSONObject(pkg)
                map[pkg] = UadEntry(
                    packageName = pkg,
                    category = when (e.optString("category")) {
                        "SAFE" -> DisableCategory.SAFE
                        "CAUTION" -> DisableCategory.CAUTION
                        "CRITICAL" -> DisableCategory.CRITICAL
                        else -> DisableCategory.UNLISTED
                    },
                    description = e.optString("description"),
                    oem = e.optString("list"),
                    removal = e.optString("removal"),
                )
            }
            cache = map
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // For non-Context callers, fallback to lazy load with app context if available — otherwise empty
    fun lookup(packageName: String): UadEntry? = cache?.get(packageName)

    fun lookup(packageName: String, context: Context): UadEntry? {
        if (cache == null) load(context)
        return cache?.get(packageName)
    }
}
