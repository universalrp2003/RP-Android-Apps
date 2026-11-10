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
        "topic.election.sector" to "HD Streamz (Pirated stream player with disguised package name)",
        "com.uptodown" to "Uptodown App Store (Third-party untrusted APK market)",
        "com.uptodown.tv" to "Uptodown TV (Third-party untrusted APK market)",
        "com.xapks.installer" to "XAPKS Installer (Ad-heavy sideloading tool)",
        "com.xapks" to "XAPKS Installer",
        "io.apkmody.sai" to "XAPKS Installer (Modded third-party installer with adware risk)",
        "com.apkpure.aegon" to "APKPure (Ad-supported third-party store)",
        "com.adups.fota" to "Adups FOTA (Known firmware backdoor / telemetry service)",
        "com.adups.fota.sysoper" to "Adups System Operator (Telemetry backdoor)",
        "com.fw.upgrade.sysoper" to "Generic TV Box OTA Spyware",
        "com.allwinner.service" to "Allwinner Vendor Telemetry Service",
        "com.rockchip.devicetest" to "Rockchip Factory Test Tool",
        "com.live.streamz" to "Live Streamz (Adware/pirated stream)",
        "com.beetv.android" to "BeeTV (Ad-supported streaming APK)",
        "com.cinema.hd" to "Cinema HD (Ad-supported streaming APK)",

        // Airtel Xstream Smart Box Carrier Bloatware
        "com.airtel.tv" to "Airtel Xstream TV (Operator launcher, ads, and recharge banner)",
        "com.uei.uas.airtel" to "Airtel UAS Remote Telemetry",
        "com.airtel.fti" to "Airtel First Time Install Setup Bloat",
        "com.airtel.thanks" to "Airtel Thanks Promo Client",
        "com.wynk.music" to "Wynk Music (Preloaded music app)",

        // Jio Set Top Box / JioAirFiber Carrier Bloatware
        "com.jio.media.stb.ondemand" to "JioCinema / JioTV+ (Preloaded OTT platform)",
        "com.jio.jioplay.tv" to "JioPlay TV Client",
        "com.jio.jiogames" to "JioGames (Heavy preloaded game store)",
        "com.jio.jiostore" to "JioStore (Proprietary locked app market)",
        "com.jio.photos" to "JioPhotos Preload",
        "com.jio.jiopages" to "JioPages Browser",
        "com.jio.saavn.tv" to "JioSaavn TV (Preloaded music player)",
        "com.jio.join" to "JioJoin Telephony Client",
        "com.jio.tr069client" to "Jio TR-069 Carrier Remote Management Service",
        "com.jio.httpupdate" to "Jio Background Firmware Updater",
        "com.jio.usbupdate" to "Jio USB Firmware Updater",
        "com.jio.home" to "JioHome / Operator Launcher",
        "com.jio.stblauncher" to "Jio STB Launcher"
    )

    val UTILITY_WATCHLIST = mapOf(
        "net.i.akihiro.barelauncher" to "Bare Launcher (Lightweight third-party launcher)",
        "com.bare.launcher" to "Bare Launcher (Minimalist TV launcher)",
        "net.aisup.simpletvlauncher" to "Simple TV Launcher (Alternative clean launcher)",
        "org.cosinus.launchertv" to "Simple TV Launcher (Alternative clean TV launcher)",
        "com.cxinventor.file.explorer" to "Cx File Explorer (Third-party file manager)",
        "com.duckduckgo.mobile.android" to "DuckDuckGo Browser (Privacy browser)",
        "com.tplink.iot" to "Tapo (TP-Link smart camera utility)",
        "com.teamsmart.videomanager.tv" to "SmartTube (Ad-free TV YouTube client)",
        "org.smarttube.stable" to "SmartTube (Ad-free TV YouTube client)",
        "com.suntv.sunnxt.mitv" to "Sun NXT TV (Indian OTT streaming client)",
        "com.google.android.apps.bard" to "Google Gemini AI Assistant",
        "notrack.ai" to "NoTrack AI Privacy Utility",
        "com.droidlogic.allapps" to "DroidLogic TV App Drawer",
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
        "com.google.android.tv.dfuservice",
        "com.android.inputmethod.latin",
        "com.google.android.inputmethod.latin",
        "com.google.android.katniss",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.providers.settings",
        "com.android.keychain",
        "com.rjio.lockunlock",
        "com.rjio.slc"
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
