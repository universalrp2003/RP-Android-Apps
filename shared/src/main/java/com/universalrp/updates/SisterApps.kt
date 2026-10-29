package com.universalrp.updates

/**
 * One list, all fourteen RP Ideas Android apps + official project links.
 *
 * Every app's About screen renders this shared directory so adding or bumping an app
 * keeps every app in sync automatically.
 */
data class SisterApp(
    /** Stable id, used to mark the app the user is currently in. */
    val id: String,
    /** Name shown in English. */
    val name: String,
    /** Tamil name, where the app has one. */
    val tamilName: String,
    /** Tag prefix, e.g. `live-guard`. */
    val tagPrefix: String,
    /** Current version shown to the user. */
    val version: String,
    /** Android applicationId, used to open the app directly when it is installed. */
    val packageName: String,
    /** The About-screen label. */
    val aboutLabel: String,
    /** Last public stable tag; advance only after its release asset is live and digest-verified. */
    val publishedVersion: String = version,
)

object SisterApps {

    const val OFFICIAL_WEBSITE_URL = "https://universalrp2003.github.io/RP-Android-Apps/"
    const val GITHUB_REPO_URL = "https://github.com/universalrp2003/RP-Android-Apps"
    const val LEGACY_MIRROR_URL = "https://github.com/universalrp2003/InternetRadio"

    private const val RELEASE_BASE =
        "https://github.com/universalrp2003/RP-Android-Apps/releases/tag"

    /** The old repository stays a working fallback until the November migration review. */
    private const val FALLBACK_BASE =
        "https://github.com/universalrp2003/InternetRadio/releases/tag"

    val all: List<SisterApp> = listOf(
        SisterApp(
            id = "smsguard",
            name = "SMS Guard (குறுஞ்செய்தி காவலன்)",
            tamilName = "குறுஞ்செய்தி காவலன்",
            tagPrefix = "sms-guard",
            version = "1.1.11",
            publishedVersion = "1.1.9",
            packageName = "com.universalrp.smsguard",
            aboutLabel = "SMS Guard (குறுஞ்செய்தி காவலன்) — GitHub release",
        ),
        SisterApp(
            id = "liveguard",
            name = "Live Guard",
            tamilName = "கைபேசி காவலன்",
            tagPrefix = "live-guard",
            version = "2.74",
            publishedVersion = "2.73",
            packageName = "com.universalrp.cleansweep",
            aboutLabel = "Live Guard — GitHub release",
        ),
        SisterApp(
            id = "radio",
            name = "Ramesh Radio",
            tamilName = "ரமேஷ் ரேடியோ",
            tagPrefix = "ramesh-radio",
            version = "1.29",
            publishedVersion = "1.28",
            packageName = "com.universalrp.tamilnadufm",
            aboutLabel = "Ramesh Radio — GitHub release",
        ),
        SisterApp(
            id = "pulseeq",
            name = "PulseEQ",
            tamilName = "பல்ஸ்இக்யூ",
            tagPrefix = "pulseeq",
            version = "1.11",
            publishedVersion = "1.9",
            packageName = "com.universalrp.pulseeq",
            aboutLabel = "PulseEQ — GitHub release",
        ),
        SisterApp(
            id = "vocalremover",
            name = "RP Vocal Remover Studio",
            tamilName = "வோக்கல் ரிமூவர் ஸ்டுடியோ",
            tagPrefix = "rp-vocal-remover-studio",
            version = "0.1.16",
            publishedVersion = "0.1.15",
            packageName = "com.universalrp.vocalremover",
            aboutLabel = "RP VOCAL REMOVER STUDIO — GitHub release",
        ),
        SisterApp(
            id = "oorarivippu",
            name = "Oor Arivippu",
            tamilName = "ஊர் அறிவிப்பு",
            tagPrefix = "oor-arivippu",
            version = "1.1.10",
            publishedVersion = "1.1.9",
            packageName = "com.oorarivippu.voiceannouncer",
            aboutLabel = "Oor Arivippu — GitHub release",
        ),
        SisterApp(
            id = "secretmessage",
            name = "Secret Message",
            tamilName = "இரகசிய செய்தி",
            tagPrefix = "secret-message",
            version = "1.1.3",
            packageName = "com.rpideas.secretmessage",
            aboutLabel = "Secret Message — GitHub release",
        ),
        SisterApp(
            id = "expensetracker",
            name = "Expense Tracker",
            tamilName = "செலவு கண்காணிப்பு",
            tagPrefix = "expense-tracker",
            version = "1.2",
            publishedVersion = "1.1",
            packageName = "com.universalrp.expensetracker",
            aboutLabel = "Expense Tracker — GitHub release",
        ),
        SisterApp(
            id = "voicenotes",
            name = "Voice Notes",
            tamilName = "குரல் குறிப்பு",
            tagPrefix = "voice-notes",
            version = "1.3.3",
            publishedVersion = "1.2",
            packageName = "com.universalrp.voicenotes",
            aboutLabel = "Voice Notes — GitHub release",
        ),
        SisterApp(
            id = "vasanthi",
            name = "Vasanthi AI",
            tamilName = "என்னைச் சுற்றி",
            tagPrefix = "vasanthi-ai",
            version = "1.1.5",
            publishedVersion = "1.1.4",
            packageName = "com.universalrp.vasanthi",
            aboutLabel = "Vasanthi AI — GitHub release",
        ),
        SisterApp(
            id = "podcast",
            name = "Tamil Podcast Player",
            tamilName = "கதை சொல்லி",
            tagPrefix = "tamil-podcast",
            version = "1.6",
            publishedVersion = "1.4",
            packageName = "com.universalrp.podcast",
            aboutLabel = "Tamil Podcast Player — GitHub release",
        ),
        SisterApp(
            id = "launcher",
            name = "Elderly Launcher",
            tamilName = "முதியவர் திரை",
            tagPrefix = "elderly-launcher",
            version = "1.5",
            publishedVersion = "1.4",
            packageName = "com.universalrp.launcher",
            aboutLabel = "Elderly Launcher — GitHub release",
        ),
        SisterApp(
            id = "tamilanstore", name = "Tamilan Store", tamilName = "தமிழன்",
            tagPrefix = "tamilan-store", version = "0.6", publishedVersion = "0.5",
            packageName = "com.universalrp.tamilanstore", aboutLabel = "தமிழன் / Tamilan Store — GitHub release",
        ),
        SisterApp(
            id = "dailyvault", name = "DailyVault", tamilName = "தினக்கணக்கு",
            tagPrefix = "daily-vault", version = "0.4", publishedVersion = "0.3",
            packageName = "com.universalrp.dailyvault", aboutLabel = "தினக்கணக்கு / DailyVault — GitHub release",
        ),
    )

    /** Official project hub links shown consistently across all RP Ideas About screens. */
    fun projectLinks(): List<Pair<String, String>> = listOf(
        "தமிழன் / Tamilan Store — official download" to "$RELEASE_BASE/tamilan-store-v0.5",
        "தினக்கணக்கு / DailyVault — official download" to "$RELEASE_BASE/daily-vault-v0.3",
        "RP-Android-Apps Official Website" to OFFICIAL_WEBSITE_URL,
        "RP-Android-Apps GitHub Repository" to GITHUB_REPO_URL,
        "Legacy Mirror Repository" to LEGACY_MIRROR_URL,
    )

    /** `label to url` for all fourteen RP Ideas Android apps. */
    fun releaseLinks(): List<Pair<String, String>> =
        all.map { it.aboutLabel to releaseUrl(it) }

    /** Same list, without the app the user is already looking at. */
    fun releaseLinks(excludeId: String?): List<Pair<String, String>> =
        all.filter { it.id != excludeId }.map { it.aboutLabel to releaseUrl(it) }

    fun releaseUrl(app: SisterApp): String = "$RELEASE_BASE/${app.tagPrefix}-v${app.publishedVersion}"

    /** The verified fallback used by the updater while the migration grace period runs. */
    fun fallbackUrl(app: SisterApp): String = "$FALLBACK_BASE/${app.tagPrefix}-v${app.publishedVersion}"

    fun byId(id: String): SisterApp? = all.firstOrNull { it.id == id }
}
