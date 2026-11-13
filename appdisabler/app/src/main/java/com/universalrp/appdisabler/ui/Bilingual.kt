package com.universalrp.appdisabler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

/** A string pair: English + Tamil. Both are always shown so the app suits every user. */
data class Bi(val en: String, val ta: String)

/** True when the device language is Tamil — Tamil is then shown first. */
@Composable
fun tamilFirst(): Boolean = LocalConfiguration.current.locales[0].language == "ta"

/** Single-line variant (toasts, buttons, one-line labels). */
@Composable
fun bi(b: Bi): String = if (tamilFirst()) b.ta else b.en

/** Language-neutral variant for use outside composables. */
fun biStatic(b: Bi, tamil: Boolean): String = if (tamil) b.ta else b.en

/**
 * Shows the same sentence in BOTH languages: device language first,
 * the other language underneath in a smaller secondary style.
 */
@Composable
fun BiText(
    b: Bi,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    bold: Boolean = false,
    secondary: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    val taFirst = tamilFirst()
    val first = if (taFirst) b.ta else b.en
    val second = if (taFirst) b.en else b.ta
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.Dp(1f))) {
        Text(
            first,
            style = style,
            fontWeight = if (bold) FontWeight.Bold else null,
            textAlign = textAlign,
            maxLines = maxLines
        )
        if (secondary && second.isNotBlank() && second != first) {
            Text(
                second,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                maxLines = maxLines
            )
        }
    }
}

/** All bilingual copy used by the UI. */
object T {
    val appName = Bi("System App Disabler", "சிஸ்டம் செயலி முடக்கி")
    val appTagline = Bi("Disable only — nothing is uninstalled", "முடக்கல் மட்டும் — எதுவும் நீக்கப்படாது")
    val navSetup = Bi("Setup", "அமைப்பு")
    val navApps = Bi("Apps", "ஆப்கள்")

    // ---------- setup window ----------
    val setupTitle = Bi("Shizuku — one-time setup", "Shizuku — ஒரே முறை அமைப்பு")
    val setupDesc = Bi(
        "This app disables (never uninstalls) pre-installed apps. Android allows that only through the shell, and Shizuku provides the shell. No root, no PC.",
        "இந்த ஆப் முன்பே நிறுவப்பட்ட ஆப்களை நீக்காமல் முடக்குகிறது. Android அதை shell மூலம் மட்டுமே அனுமதிக்கும்; அந்த shell-ஐ Shizuku வழங்குகிறது. ரூட் இல்லை, கணினி இல்லை."
    )
    val statusReady = Bi("Shizuku running — ready", "Shizuku இயங்குகிறது — தயார்")
    val statusPermission = Bi("Shizuku running — permission needed", "Shizuku இயங்குகிறது — அனுமதி தேவை")
    val statusMissing = Bi("Shizuku not running", "Shizuku இயங்கவில்லை")
    val bodyReady = Bi(
        "Open the Apps window and turn off what you do not need. Everything can be restored with one tap.",
        "Apps சாளரத்தைத் திறந்து தேவையற்றவற்றை முடக்குங்கள். அனைத்தையும் ஒரு தட்டில் மீட்டெடுக்கலாம்."
    )
    val bodyPermission = Bi(
        "Tap “Allow Shizuku permission”, then Allow in the Shizuku dialog. Without it every action fails.",
        "“Shizuku அனுமதி வழங்கு” என்பதைத் தட்டி, Shizuku சாளரத்தில் Allow செய்யுங்கள். இல்லையெனில் அனைத்தும் தோல்வியாகும்."
    )
    val bodyMissing = Bi(
        "Install Shizuku, start it once, then return here. Nothing is deleted.",
        "Shizuku-ஐ நிறுவி ஒருமுறை இயக்கி, பிறகு இங்கே திரும்புங்கள். எதுவும் நீக்கப்படுவதில்லை."
    )
    val btnOpenShizuku = Bi("Open Shizuku", "Shizuku-ஐத் திற")
    val btnInstallShizuku = Bi("Install Shizuku", "Shizuku-ஐ நிறுவு")
    val btnAllowPermission = Bi("Allow Shizuku permission", "Shizuku அனுமதி வழங்கு")
    val btnGoApps = Bi("Go to Apps", "Apps-க்குச் செல்")
    val btnFullGuide = Bi("Step-by-step guide", "படிப்படியான வழிகாட்டி")
    val btnTestShell = Bi("Check Shizuku shell", "Shizuku shell சரிபார்")
    val testing = Bi("Checking…", "சரிபார்க்கிறது…")
    val quickCheck = Bi("Quick check", "விரைவு சரிபார்ப்பு")
    val q1 = Bi(
        "1. Settings → About phone → tap Build number 7× → Developer options → Wireless debugging ON",
        "1. Settings → About phone → Build number-ஐ 7 முறை தட்டுங்கள் → Developer options → Wireless debugging ON"
    )
    val q2 = Bi(
        "2. Shizuku → Pairing → enter the code shown (Android 11+, no PC)",
        "2. Shizuku → Pairing → காட்டப்படும் கோடை உள்ளிடுங்கள் (Android 11+, கணினி தேவையில்லை)"
    )
    val q3 = Bi(
        "3. Shizuku → Start → return here; this card turns green",
        "3. Shizuku → Start → இங்கே திரும்புங்கள்; இந்த அட்டை பச்சையாகும்"
    )
    val safeTitle = Bi("Disable-only and reversible", "முடக்கல் மட்டும், மீளக்கூடியது")
    val safe1 = Bi(
        "• Turned off = hidden and stopped, never uninstalled. OTA and banking apps stay safe.",
        "• முடக்கம் = மறைக்கப்பட்டு நிறுத்தப்படும், நீக்கப்படாது. OTA மற்றும் வங்கி ஆப்கள் பாதுகாப்பாகும்."
    )
    val safe2 = Bi(
        "• Enable restores it instantly from the Off tab.",
        "• Off தாவலில் Enable தட்டினால் உடனே மீட்டெடுக்கப்படும்."
    )
    val safe3 = Bi(
        "• Never turned off: system core, Play services, telephony, launcher, keyboard — blocked automatically.",
        "• ஒருபோதும் முடக்கப்படாதவை: சிஸ்டம் முக்கிய பகுதிகள், Play services, telephony, launcher, keyboard — தானாகத் தடுக்கப்படும்."
    )
    val helpTitle = Bi("Not sure about an app?", "ஆப் பற்றி சந்தேகமா?")
    val help1 = Bi(
        "Tap an app → Get more info → Google or AI explains what it does and whether it is safe.",
        "ஆப்பைத் தட்டுங்கள் → Get more info → Google அல்லது AI அது என்ன செய்கிறது, பாதுகாப்பானதா என்பதை விளக்கும்."
    )

    // ---------- apps window ----------
    val searchHint = Bi("Search app or package", "ஆப் அல்லது பேக்கேஜ் தேடு")
    val fAll = Bi("All", "அனைத்தும்")
    val fSystem = Bi("System", "சிஸ்டம்")
    val fOff = Bi("Off", "முடக்கியவை")
    val fSafe = Bi("Safe", "பாதுகாப்பானவை")
    val fCaution = Bi("Caution", "கவனம்")
    val fNever = Bi("Never", "ஒருபோதும் இல்லை")
    val statsApps = Bi("%1\$d apps", "%1\$d ஆப்கள்")
    val offCount = Bi("%1\$d off", "%1\$d முடக்கப்பட்டது")
    // ---- header count tiles (symbols are drawn by StatTile, labels here) ----
    val fAny = Bi("Any", "எதுவும்")
    val statInstalled = Bi("installed", "நிறுவியவை")
    val statPreinstalled = Bi("pre-installed", "முன் நிறுவல்")
    val statUser = Bi("you added", "நீங்கள் சேர்த்தவை")
    val statOff = Bi("turned off", "முடக்கியவை")
    val statRunning = Bi("running", "இயங்குகிறது")
    val statRunningNeedsShizuku = Bi(
        "Start Shizuku to count apps that are running now.",
        "இப்போது இயங்கும் ஆப்களை எண்ண Shizuku-வை இயக்குங்கள்."
    )
    // ---- refuse to turn off the keyboard in use or a live accessibility service ----
    val blockedTitle = Bi("Not turned off — this would lock you out", "முடக்கவில்லை — இது உங்களை முடக்கிவிடும்")
    val blockedRecovery = Bi(
        "Everything here stays reversible: if an app is already off, tap Enable to restore it.",
        "இங்குள்ள அனைத்தும் மீளக்கூடியது: ஏற்கனவே முடக்கப்பட்ட ஆப்பை Enable தட்டி மீட்டெடுக்கலாம்."
    )
    val emptyList = Bi("No app matches this filter", "இந்த வடிகட்டலுக்கு ஆப் இல்லை")
    val actionDisable = Bi("Disable", "முடக்கு")
    val actionEnable = Bi("Enable", "இயக்கு")
    val actionAppInfo = Bi("App info", "ஆப் தகவல்")
    val actionGetInfo = Bi("Get more info", "மேலும் தகவல்")
    val rowSuspended = Bi("● Suspended — stopped, tap Enable to restore", "● இடைநிறுத்தப்பட்டது — நிறுத்தப்பட்டுள்ளது, மீட்ட Enable தட்டவும்")
    val rowDisabled = Bi("● Disabled — tap Enable to restore", "● முடக்கப்பட்டது — மீட்ட Enable தட்டவும்")
    val rowBlocked = Bi("⊘ Never disable — blocked", "⊘ முடக்கக் கூடாது — தடுக்கப்பட்டது")
    val toastDisabled = Bi("Disabled %1\$s", "%1\$s முடக்கப்பட்டது")
    val toastEnabled = Bi("Enabled %1\$s", "%1\$s இயக்கப்பட்டது")
    val toastSuspended = Bi("Suspended %1\$s (this device blocks disable)", "%1\$s இடைநிறுத்தப்பட்டது (இந்த சாதனம் முடக்கலைத் தடுக்கிறது)")
    val toastRestored = Bi("Restored %1\$s", "%1\$s மீட்டெடுக்கப்பட்டது")
    val toastNeedShizuku = Bi("Shizuku not ready — finish setup first", "Shizuku தயாராக இல்லை — முதலில் அமைப்பை முடிக்கவும்")
    val toastProtected = Bi("Protected — cannot be disabled", "பாதுகாக்கப்பட்டது — முடக்க முடியாது")
    val exportDone = Bi("Saved %1\$s", "%1\$s சேமிக்கப்பட்டது")
    val errorTitle = Bi("Action failed", "செயல் தோல்வி")
    val unknownError = Bi("Unknown error", "அறியப்படாத பிழை")
    val actionClose = Bi("Close", "மூடு")

    // ---------- confirm ----------
    val confirmTitle = Bi("Caution — may break a feature", "கவனம் — ஏதேனும் அம்சம் பாதிக்கலாம்")
    val confirmMsg = Bi(
        "%1\$s is marked Caution — it may remove a feature (for example Game tools or Phone Manager). Disable anyway? You can Enable it back.",
        "%1\$s ‘கவனம்’ எனக் குறிக்கப்பட்டுள்ளது — ஏதேனும் அம்சம் (உம். Game tools, Phone Manager) செயலிழக்கலாம். முடக்கவா? மீண்டும் Enable செய்யலாம்."
    )
    val actionDisableAnyway = Bi("Disable anyway", "இருந்தாலும் முடக்கு")
    val actionCancel = Bi("Cancel", "ரத்து")

    // ---------- details dialog ----------
    val catSafeBody = Bi(
        "Safe to disable — recommended by UAD. No feature is expected to break.",
        "முடக்கலாம் — UAD பரிந்துரைத்தது. எந்த அம்சமும் பாதிக்க வாய்ப்பில்லை."
    )
    val catCautionBody = Bi(
        "Caution — may remove a feature. You can Enable it back anytime.",
        "கவனம் — ஏதேனும் அம்சம் செயலிழக்கலாம். எப்போது வேண்டுமானாலும் மீட்ட Enable செய்யலாம்."
    )
    val catCriticalBody = Bi(
        "Never disable — system/core package. Blocked by this app.",
        "ஒருபோதும் முடக்க வேண்டாம் — சிஸ்டம் முக்கிய பேக்கேஜ். இந்த ஆப் தடுக்கிறது."
    )
    val catUnlistedBody = Bi(
        "Not in the UAD list — check Google/AI before disabling.",
        "UAD பட்டியலில் இல்லை — முடக்கும் முன் Google/AI-ல் சரிபாருங்கள்."
    )
    val whatIsApp = Bi("What is this app?", "இந்த ஆப் என்ன?")
    val descMissingSystem = Bi(
        "Pre-installed system package added by the manufacturer or carrier. Search the web for its exact role.",
        "உற்பத்தியாளர் அல்லது நிறுவனம் முன்பே நிறுவிய சிஸ்டம் பேக்கேஜ். அதன் பயனை இணையத்தில் தேடுங்கள்."
    )
    val descMissingUser = Bi("User-installed app. Safe to manage normally.", "நீங்கள் நிறுவிய ஆப். சாதாரணமாக நிர்வகிக்கலாம்.")
    val whatHappens = Bi("What happens if turned off?", "முடக்கினால் என்ன ஆகும்?")
    val happenSuspended = Bi(
        "It is suspended: it cannot run or receive broadcasts. Tap Enable to restore.",
        "இது இடைநிறுத்தப்பட்டுள்ளது: இயங்கவோ அறிவிப்புகளைப் பெறவோ முடியாது. மீட்ட Enable தட்டுங்கள்."
    )
    val happenDisabled = Bi(
        "Currently off — hidden and stopped. Enable restores it.",
        "தற்போது முடக்கப்பட்டுள்ளது — மறைந்து நிறுத்தப்பட்டுள்ளது. Enable மீட்டெடுக்கும்."
    )
    val happenSafe = Bi(
        "It disappears from the launcher and stops running in the background. Reversible from the Off tab.",
        "Launcher-ல் இருந்து மறைந்து பின்னணியில் இயங்குவது நிற்கும். Off தாவலில் மீட்டெடுக்கலாம்."
    )
    val happenCaution = Bi(
        "Its feature stops working until you Enable it again.",
        "மீண்டும் Enable செய்யும் வரை அதன் அம்சம் செயல்படாது."
    )
    val happenCritical = Bi("Would break the system — blocked.", "சிஸ்டத்தைப் பாதிக்கும் — தடுக்கப்பட்டுள்ளது.")
    val happenUnlisted = Bi(
        "It is hidden and stopped. If something breaks, Enable it from the Off tab.",
        "அது மறைந்து நிறுத்தப்படும். ஏதேனும் பாதிப்பு ஏற்பட்டால் Off தாவலில் Enable செய்யுங்கள்."
    )
    val howToDisable = Bi("How to turn it off?", "எப்படி முடக்குவது?")
    val howReady = Bi(
        "Tap Disable below and confirm — it turns off instantly.",
        "கீழே Disable தட்டி உறுதிப்படுத்துங்கள் — உடனே முடக்கப்படும்."
    )
    val howNotReady = Bi(
        "Finish Shizuku setup in window 1 first, then tap Disable.",
        "முதலில் சாளரம் 1-ல் Shizuku அமைப்பை முடிக்கவும், பிறகு Disable தட்டவும்."
    )
    val webHead = Bi("Get more info from the web", "இணையத்தில் கூடுதல் தகவல்")
    val btnAskAi = Bi("Ask AI", "AI-யிடம் கேள்")
    val btnReddit = Bi("Reddit / XDA", "Reddit / XDA")
    val btnAllResults = Bi("All results", "அனைத்து முடிவுகள்")
    val btnGoogle = Bi("Google", "Google")

    // ---------- about ----------
    val aboutTitle = Bi("About System App Disabler", "சிஸ்டம் செயலி முடக்கி பற்றி")
    val aboutIntro = Bi(
        "Disable-only pre-installed app remover powered by Shizuku. No uninstall, no root, every disable is reversible.",
        "Shizuku மூலம் இயங்கும், முன்பே நிறுவப்பட்ட ஆப்களை முடக்கும் கருவி. நீக்கம் இல்லை, ரூட் இல்லை, ஒவ்வொரு முடக்கலும் மீளக்கூடியது."
    )
    val aboutApps = Bi("Our applications", "எங்கள் ஆப்கள்")
    val aboutLinks = Bi("Links", "இணைப்புகள்")
    val btnWebsite = Bi("Official website", "அதிகாரப்பூர்வ இணையதளம்")
    val btnGithub = Bi("GitHub repository", "GitHub களஞ்சியம்")
    val btnStore = Bi("Tamilan Store — download our apps", "தமிழன் ஸ்டோர் — எங்கள் ஆப்களைப் பதிவிறக்கு")
    val btnGetShizuku = Bi("Get Shizuku", "Shizuku-ஐப் பெறு")
    val credits = Bi(
        "Shizuku by Rikka (Apache 2.0) • UAD lists by 0x192 (GPL-3.0, 2394 entries) • Disable-only, OTA and banking safe.",
        "Shizuku — Rikka (Apache 2.0) • UAD பட்டியல் — 0x192 (GPL-3.0, 2394 பதிவுகள்) • முடக்கல் மட்டும், OTA மற்றும் வங்கி பாதுகாப்பு."
    )
    val thisApp = Bi("this app", "இந்த ஆப்")

    // ---------- guide screen ----------
    val guideTitle = Bi("Shizuku — 2 minute guide", "Shizuku — 2 நிமிட வழிகாட்டி")
    val guideIntro = Bi(
        "This app never uninstalls anything. Shizuku gives a safe shell so apps can be turned off. No root, fully reversible.",
        "இந்த ஆப் எதையும் நீக்காது. ஆப்களை முடக்க Shizuku பாதுகாப்பான shell தருகிறது. ரூட் இல்லை, முழுமையாக மீளக்கூடியது."
    )
    val g1Title = Bi("Step 1 — Get Shizuku", "படி 1 — Shizuku பெறுங்கள்")
    val g1Body = Bi(
        "Install Shizuku from the official site, Play Store or GitHub.",
        "Shizuku-ஐ அதிகாரப்பூர்வ தளம், Play Store அல்லது GitHub-இல் இருந்து நிறுவுங்கள்."
    )
    val g2Title = Bi("Step 2 — Turn on Wireless debugging", "படி 2 — Wireless debugging இயக்கு")
    val g2Body = Bi(
        "Settings → About phone → tap Build number 7× → Developer options → Wireless debugging ON. In Shizuku choose Pairing and enter the code. Android 11+ needs no PC.",
        "Settings → About phone → Build number-ஐ 7 முறை தட்டுங்கள் → Developer options → Wireless debugging ON. Shizuku-வில் Pairing தேர்ந்தெடுத்து கோடை உள்ளிடுங்கள். Android 11+ கணினி தேவையில்லை."
    )
    val g3Title = Bi("Step 3 — Start Shizuku", "படி 3 — Shizuku தொடங்கு")
    val g3Body = Bi(
        "In Shizuku tap Start. When it says running, come back here — the card turns green.",
        "Shizuku-வில் Start தட்டுங்கள். இயங்குகிறது என்றதும் இங்கே திரும்புங்கள் — அட்டை பச்சையாகும்."
    )
    val gNever = Bi(
        "Never turned off: system core, com.google.android.gms, telephony providers, keyboard, launcher. This app blocks them automatically.",
        "ஒருபோதும் முடக்கப்படாதவை: சிஸ்டம் முக்கிய பகுதிகள், com.google.android.gms, telephony providers, keyboard, launcher. இந்த ஆப் அவற்றைத் தானாகத் தடுக்கிறது."
    )
    val gBack = Bi("Back", "திரும்பு")
    val gFullSite = Bi("Open full guide with screenshots", "திரைப்படங்களுடன் முழு வழிகாட்டி")
}
