package com.universalrp.appdisabler.data

/**
 * Minimal UAD-style database for draft. Full DB is ~3000 entries from
 * Universal Android Debloater (MIT). Draft includes representative samples per OEM.
 * Full import will follow from https://github.com/0x192/universal-android-debloater
 */
data class UadEntry(
    val packageName: String,
    val category: DisableCategory,
    val description: String,
    val oem: String = "generic",
)

object UadDatabase {

    // Subset for scaffold — replace with full JSON import in next iteration
    val entries: Map<String, UadEntry> = listOf(
        // Generic safe examples (demo bloat)
        UadEntry("com.facebook.katana", DisableCategory.SAFE, "Facebook — safe to disable if not using", "generic"),
        UadEntry("com.facebook.appmanager", DisableCategory.SAFE, "Facebook App Manager", "generic"),
        UadEntry("com.netflix.mediaclient", DisableCategory.SAFE, "Netflix — safe if not using on phone", "generic"),
        // Xiaomi
        UadEntry("com.miui.analytics", DisableCategory.SAFE, "Xiaomi Analytics", "xiaomi"),
        UadEntry("com.xiaomi.mipicks", DisableCategory.SAFE, "Mi Picks / GetApps", "xiaomi"),
        UadEntry("com.miui.bugreport", DisableCategory.SAFE, "Mi Bugreport", "xiaomi"),
        // Samsung
        UadEntry("com.samsung.android.bixby.agent", DisableCategory.SAFE, "Bixby Voice — safe if not using Bixby", "samsung"),
        UadEntry("com.samsung.android.game.gamingservice", DisableCategory.SAFE, "Samsung Game Service", "samsung"),
        // Oppo / Realme
        UadEntry("com.coloros.phonemanager", DisableCategory.CAUTION, "Oppo Phone Manager — caution", "oppo"),
        UadEntry("com.heytap.pictorial", DisableCategory.SAFE, "Oppo Wallpaper carousel", "oppo"),
        // Critical never-disable samples
        UadEntry("com.android.providers.telephony", DisableCategory.CRITICAL, "Core telephony — never disable", "generic"),
        UadEntry("com.google.android.gms", DisableCategory.CRITICAL, "Play Services — never disable", "generic"),
        UadEntry("android", DisableCategory.CRITICAL, "Android System — never disable", "generic"),
    ).associateBy { it.packageName }

    fun lookup(packageName: String): UadEntry? = entries[packageName]
}
