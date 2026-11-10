package com.universalrp.tvguard.debloat

enum class BloatSafetyLevel {
    SAFE_TO_REMOVE,       // Known bloat, adware, pirate streaming, telemetry
    CAUTION_USER_CHOICE,  // User-installed utility, custom launcher, file explorer
    CORE_SYSTEM_PROTECTED // Essential for Android TV functioning (Settings, SystemUI, IME)
}

data class AppDebloatItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val safetyLevel: BloatSafetyLevel,
    val description: String,
    val sizeBytes: Long = 0L
)

object BloatCatalog {

    val KNOWN_BLOATWARE = mapOf(
        "com.hdstreamz.app" to "HD Streamz (Pirated stream player, known ad injection/mining risk)",
        "com.hdstreamz" to "HD Streamz (Pirated stream player)",
        "com.uptodown" to "Uptodown App Store (Third-party untrusted APK market)",
        "com.uptodown.tv" to "Uptodown TV (Third-party untrusted APK market)",
        "com.xapks.installer" to "XAPKS Installer (Ad-heavy sideloading tool)",
        "com.xapks" to "XAPKS Installer",
        "com.apkpure.aegon" to "APKPure (Ad-supported third-party store)",
        "com.adups.fota" to "Adups FOTA (Known firmware backdoor / telemetry service)",
        "com.adups.fota.sysoper" to "Adups System Operator (Telemetry backdoor)",
        "com.fw.upgrade.sysoper" to "Generic TV Box OTA Spyware",
        "com.allwinner.service" to "Allwinner Vendor Telemetry Service",
        "com.rockchip.devicetest" to "Rockchip Factory Test Tool",
        "com.live.streamz" to "Live Streamz (Adware/pirated stream)",
        "com.beetv.android" to "BeeTV (Ad-supported streaming APK)",
        "com.cinema.hd" to "Cinema HD (Ad-supported streaming APK)"
    )

    val UTILITY_WATCHLIST = mapOf(
        "net.i.akihiro.barelauncher" to "Bare Launcher (Lightweight third-party launcher)",
        "net.aisup.simpletvlauncher" to "Simple TV Launcher (Alternative clean launcher)",
        "com.cxinventor.file.explorer" to "Cx File Explorer (Third-party file manager)",
        "com.duckduckgo.mobile.android" to "DuckDuckGo Browser (Privacy browser)",
        "com.tplink.iot" to "Tapo (TP-Link smart camera utility)",
        "com.teamsmart.videomanager.tv" to "SmartTube (Ad-free TV YouTube client)",
        "org.schabi.newpipe" to "NewPipe (YouTube client)"
    )

    val CORE_SYSTEM_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.tv.settings",
        "com.android.settings",
        "com.google.android.gms",
        "com.android.vending",
        "com.google.android.gsf",
        "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin",
        "com.google.android.katniss",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.providers.settings",
        "com.android.keychain"
    )

    fun classifyPackage(packageName: String, isSystemApp: Boolean): Pair<BloatSafetyLevel, String> {
        if (CORE_SYSTEM_PACKAGES.contains(packageName) || packageName.startsWith("com.android.providers")) {
            return BloatSafetyLevel.CORE_SYSTEM_PROTECTED to "Essential Android core component. Removing will soft-brick device."
        }

        KNOWN_BLOATWARE[packageName]?.let { desc ->
            return BloatSafetyLevel.SAFE_TO_REMOVE to desc
        }

        UTILITY_WATCHLIST[packageName]?.let { desc ->
            return BloatSafetyLevel.CAUTION_USER_CHOICE to desc
        }

        // If package name matches sketchy patterns
        if (packageName.contains("adups", ignoreCase = true) ||
            packageName.contains("streamz", ignoreCase = true) ||
            packageName.contains("apkpure", ignoreCase = true) ||
            packageName.contains("xapks", ignoreCase = true)
        ) {
            return BloatSafetyLevel.SAFE_TO_REMOVE to "Matches known adware/streamer pattern: $packageName"
        }

        return if (isSystemApp) {
            BloatSafetyLevel.CORE_SYSTEM_PROTECTED to "System package. Verify before disabling."
        } else {
            BloatSafetyLevel.CAUTION_USER_CHOICE to "User-installed application."
        }
    }
}
