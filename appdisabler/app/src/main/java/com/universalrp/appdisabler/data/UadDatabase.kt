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
    /** The database's own words say removing this can leave the phone unable to boot. */
    val bootloopRisk: Boolean = false,
)

/**
 * Sentences that say a package does NOT bootloop, so a plain "bootloop" search does not block them.
 * Real examples in this database: "Safe to remove (no bootloop)", "Disabling does not cause a
 * bootloop and package installation still works fine."
 */
private val NO_BOOTLOOP = Regex(
    """\b(no|not|never|does not|doesn't)\b(?:(?!\bbut\b)[^.!?]){0,40}\bboot-?\s?loop""",
    RegexOption.IGNORE_CASE
)

/**
 * Restricting the negation to 40 characters and stopping at "but" is what keeps the real warning in
 * `com.google.android.packageinstaller` ("… you can still install APKs without it … but since
 * Android 9 … could bootloop your device if removed") from reading as a reassurance.
 */
private val SECOND_HAND_BOOTLOOP = Regex(
    """(apparently|some people said|others said|may|might|probably|would|i didn't test)""",
    RegexOption.IGNORE_CASE
)

/**
 * True when the description warns that removal/disable can bootloop THIS device.
 *
 * Errs towards blocking: a wrong "unsafe" only hides a row behind a warning, while a wrong "safe"
 * can leave a phone that will not boot. Unlisted packages are unaffected (they have no text).
 */
fun claimsBootloopRisk(description: String): Boolean {
    if (description.isBlank()) return false
    val sentences = Regex("""(?<=[.!?])\s+""").split(description)
    return sentences.any { sentence ->
        sentence.contains("bootloop", ignoreCase = true) ||
            sentence.contains("boot loop", ignoreCase = true)
    } && sentences.none { sentence ->
        sentence.contains("bootloop", ignoreCase = true) &&
            NO_BOOTLOOP.containsMatchIn(sentence) &&
            !SECOND_HAND_BOOTLOOP.containsMatchIn(sentence)
    }
}

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
                    bootloopRisk = claimsBootloopRisk(e.optString("description")),
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
