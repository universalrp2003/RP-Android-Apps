package com.universalrp.updates

/**
 * The RP Ideas guide: one tap on a topic, and its text opens.
 *
 * Owner's wording, 19 October 2026: "all our 6 application have built in detailed tamil and
 * english guide ... like oor arivippu like click and expand show the info like text". So the
 * screen is a list of short topic titles; only the topic the user taps is open. Nothing is
 * longer than a few sentences, because a wall of text on a phone is never read.
 *
 * Why one shared file: the four older apps had no guide at all, and the two newer ones had
 * their own. Keeping the text here means a step that changes on a phone (a menu path, a
 * notification name) is corrected once, for every app, in both languages. It also means the
 * text cannot silently drift apart from the app it explains.
 *
 * Secret Message is the one exception: it is a separate Gradle build (secretmessage/wifi-test has
 * its own settings.gradle.kts and forked the shared updater), so it keeps its own longer 1.1.0
 * guide in its own GuideScreen.kt, using the same tap-to-open shape. Copy the text here only if
 * that app is ever pulled into the shared source set.
 *
 * Text rules, same as the rest of the project:
 *  - Never invent a menu path. Android settings paths differ per maker; the ones written here
 *    are the generic ones, with the Xiaomi/Samsung names only where the app itself needs them.
 *  - Never promise something the app does not do (no "system-wide" equalizer, no silent
 *    install, no background guarantee).
 *  - Tamil and English are separate complete texts, not a machine translation at runtime.
 */
data class GuideTopic(
    /** English title, short enough for one line on a phone. */
    val title: String,
    val titleTa: String,
    /** English body. A few sentences; the remaining detail goes in another topic. */
    val body: String,
    val bodyTa: String,
)

/** One app's guide. [intro] is the single paragraph shown above the topic list. */
data class GuideContent(
    val intro: String,
    val introTa: String,
    val topics: List<GuideTopic>,
) {
    fun intro(tamil: Boolean): String = if (tamil) introTa else intro
}

private fun topic(title: String, titleTa: String, body: String, bodyTa: String) =
    GuideTopic(title, titleTa, body, bodyTa)

object AppGuides {

    // ---------------------------------------------------------------------------------------
    // Live Guard (CleanSweep)
    // ---------------------------------------------------------------------------------------
    val liveGuard = GuideContent(
        intro = "Live Guard watches your own phone: battery, storage, memory, data and heat. " +
            "Nothing here reads another app's messages or notifications. Tap a topic to open it.",
        introTa = "லைவ் கார்டு உங்கள் போனையே கவனிக்கிறது: பேட்டரி, சேமிப்பு, நினைவகம், தரவு, வெப்பம். " +
            "மற்ற செயலிகளின் செய்திகளையோ அறிவிப்புகளையோ இது படிக்காது. ஒரு தலைப்பைத் தொட்டு விரிவாக்குங்கள்.",
        topics = listOf(
            topic(
                "Cleaning storage",
                "சேமிப்பு சுத்தம்",
                "Tap Scan to measure junk and unused files, review the list, and only then remove what you " +
                    "choose. Nothing is deleted without your tap, and the app never removes another app's data " +
                    "by itself. If Android asks for file permission, grant it once; without it the scan can only " +
                    "see what Android allows.",
                "கழிவு மற்றும் பயன்படாத கோப்புகளை அளக்க 'ஸ்கேன்' தட்டுங்கள்; பட்டியலைப் பார்த்த பிறகே நீங்கள் " +
                    "தேர்ந்தெடுப்பதை நீக்குங்கள். உங்கள் தொடுதல் இல்லாமல் எதுவும் நீக்கப்படாது. Android கோப்பு " +
                    "அனுமதி கேட்டால் ஒருமுறை வழங்குங்கள்; இல்லாவிட்டால் Android அனுமதிக்கும் அளவை மட்டுமே ஸ்கேன் பார்க்கும்.",
            ),
            topic(
                "Battery and heat",
                "பேட்டரி மற்றும் வெப்பம்",
                "The battery figure and the temperature come from the phone's own sensors. Some phones do not " +
                    "report every value, and then the app says it is unavailable instead of guessing. The 44 °C " +
                    "warning is a warning only: it does not stop any app or start any cooling.",
                "பேட்டரி அளவும் வெப்பநிலையும் போனின் சென்சார்களிலிருந்தே வருகின்றன. சில போன்கள் எல்லா மதிப்புகளையும் " +
                    "தருவதில்லை; அப்போது ஊகிக்காமல் 'கிடைக்கவில்லை' என்று காட்டும். 44 °C எச்சரிக்கை எச்சரிக்கை மட்டுமே: " +
                    "எந்தச் செயலியையும் நிறுத்தாது, குளிர்விக்கவும் செய்யாது.",
            ),
            topic(
                "Mobile data and the daily 4G limit",
                "மொபைல் தரவு மற்றும் தினசரி 4G வரம்பு",
                "Mobile → Data shows today's mobile data and Wi-Fi data separately, plus your recharge total. " +
                    "If your plan gives 1.5 GB, 2 GB or 3 GB a day, set Daily 4G limit once: the app then shows " +
                    "what is left today with a bar, and it starts again at midnight by itself. Nothing is reset " +
                    "by hand, and Wi-Fi is never counted.",
                "மொபைல் → தரவு என்பதில் இன்றைய மொபைல் மற்றும் Wi-Fi தரவு தனித்தனியாகவும், ரீசார்ஜ் மொத்தமும் தெரியும். " +
                    "உங்கள் திட்டம் நாளொன்றுக்கு 1.5, 2 அல்லது 3 GB தந்தால் 'தினசரி 4G வரம்பு' ஒருமுறை அமைக்கவும்: " +
                    "இன்று மீதம் எவ்வளவு என்பதை பட்டையுடன் காட்டும், நள்ளிரவில் தானாகவே மீண்டும் தொடங்கும். " +
                    "கைமுறை மீட்டமைப்பு தேவையில்லை; Wi-Fi எப்போதும் கணக்கில் வராது.",
            ),
            topic(
                "Nearby cell towers and location",
                "அருகிலுள்ள செல் டவர்கள் மற்றும் இருப்பிடம்",
                "Mobile → Data can list the towers your SIM can see: the serving cell and its neighbours, " +
                    "with PCI/TAC and dBm. Android shares that list only with apps that have Precise " +
                    "location — Approximate is not enough — and some carriers hide it. Allow it on that " +
                    "screen; Live Guard reads it only while the screen is open, never in the background, " +
                    "and nothing is uploaded.",
                "மொபைல் → தரவு என்பதில் உங்கள் சிம் பார்க்கும் டவர்களைப் பட்டியலிடலாம்: பயன்பாட்டில் உள்ள செல், " +
                    "அதன் அண்டை செல்கள், PCI/TAC மற்றும் dBm உடன். அந்தப் பட்டியலை Android 'துல்லியமான' " +
                    "இருப்பிடம் உள்ள ஆப்களுக்கு மட்டுமே தரும் — தோராயமானது போதாது — சில நிறுவனங்கள் அதை " +
                    "மறைக்கின்றன. அத்திரையிலேயே அனுமதி தாருங்கள்; திரை திறந்திருக்கும்போது மட்டுமே லைவ் கார்டு " +
                    "அதைப் படிக்கும், பின்னணியில் ஒருபோதும் படிக்காது, எதுவும் பதிவேற்றப்படுவதில்லை.",
            ),
            topic(
                "My plan and the pill colours",
                "என் திட்டம் மற்றும் பட்டி நிறங்கள்",
                "Mobile → My plan records the recharge you actually bought: Daily or Total data, the amount, the " +
                    "start date and days, an optional booster, and whether 5G is free. Enter the pack's total, not " +
                    "what is left today. The floating data pill colours that total: normal below 75% used, amber " +
                    "from 75%, red and flashing from 90%. It warns about the number you entered, not a carrier " +
                    "message, so correct the plan if the number is wrong. A short top-up that expires belongs in " +
                    "its own recharge, not left on as a booster.",
                "மொபைல் → என் திட்டம் என்பது நீங்கள் வாங்கிய ரீசார்ஜைப் பதிவு செய்கிறது: தினசரி அல்லது மொத்த தரவு, " +
                    "அளவு, தொடக்கத் தேதி மற்றும் நாட்கள், விருப்பப் பூஸ்டர், 5G இலவசமா என்பது. இன்று மீதம் உள்ளதை " +
                    "அல்ல, பேக்கின் மொத்தத்தை உள்ளிடுங்கள். மிதக்கும் தரவுப் பட்டி அந்த மொத்தத்தின் அடிப்படையில் " +
                    "நிறம் மாறும்: 75% வரை சாதாரணம், 75% முதல் அம்பர், 90% முதல் சிவப்பு மற்றும் மின்னல். இது " +
                    "நிறுவனத்தின் செய்தி அல்ல, நீங்கள் உள்ளிட்ட எண்ணைப் பற்றிய எச்சரிக்கை; எண் தவறானால் திட்டத்தைத் " +
                    "திருத்துங்கள். காலாவதியாகும் குறுகிய டாப்-அப்பை பூஸ்டராக வைத்திருக்காமல் அதற்கான ரீசார்ஜில் " +
                    "தனியாக வைத்திருங்கள்.",
            ),
            topic(
                "4G and 5G counted separately",
                "4G மற்றும் 5G தனித்தனி கணக்கு",
                "Android keeps one mobile counter, so Live Guard samples it and files each increase under the radio " +
                    "that was active at that moment. Set a 5G plan as unlimited and only your 4G is counted. " +
                    "The Auto / On / Off buttons under the radio card decide whether the app identifies 4G and 5G " +
                    "at all; Off leaves one mobile total, as older versions did.",
                "Android ஒரே மொபைல் கணக்கைத்தான் வைத்திருக்கிறது; எனவே லைவ் கார்டு அதை அளந்து, அந்த நேரத்தில் " +
                    "இயங்கிய ரேடியோவின் கீழ் ஒவ்வொரு கூட்டத்தையும் சேர்க்கிறது. 5G திட்டம் வரம்பற்றது எனில் 4G மட்டும் " +
                    "கணக்கிடப்படும். ரேடியோ அட்டையின் கீழ் உள்ள Auto / On / Off 4G, 5G அடையாளத்தை முடக்கவும் இயக்கவும் உதவும்; " +
                    "Off எனில் பழைய பதிப்புகளைப் போல ஒரே மொபைல் மொத்தம்.",
            ),
            topic(
                "Voice announcements and calls",
                "குரல் அறிவிப்புகள் மற்றும் அழைப்புகள்",
                "Voice can tell you about charging, heat, a transport change (Wi-Fi ⇄ mobile) and a 4G ⇄ 5G hop, " +
                    "each with its own switch, in Tamil or English. Nothing is ever spoken while the phone is " +
                    "ringing or you are on a call: announcements wait, and the Voice screen shows " +
                    "\"Blocked: phone call in progress\" when that happens.",
                "சார்ஜிங், வெப்பம், இணைப்பு மாற்றம் (Wi-Fi ⇄ மொபைல்), 4G ⇄ 5G மாற்றம் ஆகியவற்றை குரல் கூறும்; " +
                    "ஒவ்வொன்றுக்கும் தனி சுவிட்ச், தமிழ் அல்லது ஆங்கிலம். போன் அடிக்கும்போதோ நீங்கள் அழைப்பில் " +
                    "இருக்கும்போதோ எதுவும் ஒலிக்காது: அறிவிப்புகள் காத்திருக்கும், அப்போது குரல் திரையில் " +
                    "\"தடுக்கப்பட்டது: தொலைபேசி அழைப்பு நடைபெறுகிறது\" என்று தெரியும்.",
            ),
            topic(
                "Updating Live Guard",
                "லைவ் கார்டைப் புதுப்பித்தல்",
                "Live Guard checks the official GitHub releases itself and offers only a newer stable version. " +
                    "It checks the file size, SHA-256, package name, version and signing certificate before handing " +
                    "the file to Android. Android still shows its own install screen, and you can cancel there. " +
                    "The app never installs anything silently.",
                "லைவ் கார்டு அதிகாரப்பூர்வ GitHub வெளியீடுகளைத் தானே சரிபார்த்து, புதிய நிலையான பதிப்பை மட்டுமே " +
                    "வழங்கும். கோப்பு அளவு, SHA-256, தொகுப்புப் பெயர், பதிப்பு, கையொப்பச் சான்றிதழ் ஆகியவற்றைச் " +
                    "சரிபார்த்த பிறகே Android-க்கு அளிக்கும். நிறுவல் திரையை Android காட்டும்; அங்கே ரத்து செய்யலாம். " +
                    "எதையும் அமைதியாக நிறுவாது.",
            ),
            topic(
                "The other RP Ideas apps",
                "மற்ற RP Ideas செயலிகள்",
                "About lists all six apps with their official release pages, so you can read what changed or " +
                    "download a fresh copy for another phone. Tapping an app that is installed on this phone " +
                    "opens it; tapping one that is not opens its release page.",
                "About பகுதியில் ஆறு செயலிகளும் அவற்றின் அதிகாரப்பூர்வ வெளியீட்டுப் பக்கங்களுடன் உள்ளன; என்ன மாறியது " +
                    "என்பதைப் படிக்கலாம், வேறு போனுக்கு புதிய நகலை பதிவிறக்கலாம். இந்தப் போனில் நிறுவிய செயலியைத் " +
                    "தொட்டால் அது திறக்கும்; இல்லாதது வெளியீட்டுப் பக்கம் திறக்கும்.",
            ),
        ),
    )

    // ---------------------------------------------------------------------------------------
    // Ramesh Radio
    // ---------------------------------------------------------------------------------------
    val rameshRadio = GuideContent(
        intro = "Ramesh Radio plays Tamil FM stations from the internet and your own audio files. " +
            "Tap a topic to open it.",
        introTa = "ரமேஷ் ரேடியோ இணையத்திலிருந்து தமிழ் FM நிலையங்களையும் உங்கள் சொந்த ஒலிக் கோப்புகளையும் " +
            "இயக்கும். ஒரு தலைப்பைத் தொட்டு விரிவாக்குங்கள்.",
        topics = listOf(
            topic(
                "Playing a live station",
                "நேரடி நிலையம் இயக்குதல்",
                "Open the station list, tap a station, and playback starts. Opening the app starts the " +
                    "station you played last; More has a switch to open quietly instead. Live radio has no " +
                    "\"forward\": the seek bar shows LIVE and moves only inside the few seconds the app has " +
                    "already buffered. If a station does not start, it is usually the station's own server; " +
                    "try another station before changing anything.",
                "நிலையப் பட்டியலைத் திறந்து ஒரு நிலையத்தைத் தட்டுங்கள், இயக்கம் தொடங்கும். ஆப்பைத் திறந்தால் " +
                    "நீங்கள் கடைசியாகக் கேட்ட நிலையம் தொடங்கும்; அமைதியாகத் திறக்க 'More' பகுதியில் சுவிட்ச் உள்ளது. " +
                    "நேரடி வானொலிக்கு \"முன்னோக்கி\" கிடையாது: சீக் பட்டை LIVE என்று காட்டும், ஏற்கனவே இடையகத்தில் " +
                    "உள்ள சில வினாடிகளுக்குள் மட்டுமே நகரும். நிலையம் தொடங்கவில்லை என்றால் பெரும்பாலும் அந்த " +
                    "நிலையத்தின் சேவையகம்தான் காரணம்; வேறு நிலையத்தை முயற்சியுங்கள்.",
            ),
            topic(
                "Playing a file from your phone",
                "உங்கள் போனிலிருந்து கோப்பு இயக்குதல்",
                "Choose a song or an audio file with the folder button. Recorded audio can be dragged anywhere " +
                    "with the seek bar, unlike live radio. Playback stays inside the app, so you always have " +
                    "pause and stop on screen.",
                "கோப்புறை பொத்தானைத் தட்டி பாடலையோ ஒலிக் கோப்பையோ தேர்ந்தெடுங்கள். பதிவு செய்த ஒலியை சீக் " +
                    "பட்டையால் எங்கு வேண்டுமானாலும் இழுக்கலாம்; நேரடி வானொலியில் அது இல்லை. இயக்கம் செயலிக்குள்ளேயே " +
                    "நடப்பதால் இடைநிறுத்தம், நிறுத்தம் எப்போதும் திரையில் இருக்கும்.",
            ),
            topic(
                "News and local pages",
                "செய்திகள் மற்றும் உள்ளூர் பக்கங்கள்",
                "The news list and the local page load from their sources when you open them, so they need " +
                    "internet. If a page is empty, it is the source that did not answer; the app does not invent " +
                    "headlines. Reading an item marks it read for that page.",
                "செய்திப் பட்டியலும் உள்ளூர் பக்கமும் திறக்கும்போது அவற்றின் மூலங்களிலிருந்து ஏற்றப்படும்; எனவே " +
                    "இணையம் தேவை. பக்கம் காலியாக இருந்தால் மூலம் பதிலளிக்கவில்லை என்று பொருள்; செயலி செய்திகளை " +
                    "கற்பனையாக உருவாக்காது. ஒரு செய்தியைப் படித்தால் அது அந்தப் பக்கத்தில் படித்ததாகக் குறியிடப்படும்.",
            ),
            topic(
                "Sound controls",
                "ஒலிக் கட்டுப்பாடுகள்",
                "The equalizer, the bass and clarity controls and the recording tools work on audio played " +
                    "inside Ramesh Radio. Android does not let one app equalize another app's sound on every " +
                    "phone, so nothing here claims to change YouTube or your music player.",
                "சமநிலைப்படுத்தி, பாஸ் மற்றும் தெளிவு கட்டுப்பாடுகள், பதிவுக் கருவிகள் அனைத்தும் ரேமஷ் ரேடியோவுக்குள் " +
                    "இயங்கும் ஒலியிலேயே வேலை செய்யும். எல்லா போன்களிலும் ஒரு செயலி மற்றொரு செயலியின் ஒலியை மாற்ற " +
                    "Android அனுமதிக்காது; எனவே YouTube அல்லது உங்கள் இசை செயலியை இது மாற்றும் என்று கூறவில்லை.",
            ),
            topic(
                "Updating Ramesh Radio",
                "ரமேஷ் ரேடியோவைப் புதுப்பித்தல்",
                "The app checks the official GitHub releases and offers only a newer stable version, after " +
                    "checking the file's size, SHA-256, package name, version and signing certificate. Android " +
                    "shows its own install screen. If your phone blocks the update, it is usually the signer: " +
                    "uninstall the older test build first, then install once.",
                "அதிகாரப்பூர்வ GitHub வெளியீடுகளைச் சரிபார்த்து, புதிய நிலையான பதிப்பை மட்டுமே வழங்கும்; கோப்பு அளவு, " +
                    "SHA-256, தொகுப்புப் பெயர், பதிப்பு, கையொப்பச் சான்றிதழ் ஆகியவை சரிபார்க்கப்படும். நிறுவல் " +
                    "திரையை Android காட்டும். போன் தடுத்தால் பெரும்பாலும் கையொப்பம் காரணம்: பழைய சோதனைப் பதிப்பை " +
                    "முதலில் நீக்கி, பின்னர் ஒருமுறை நிறுவுங்கள்.",
            ),
        ),
    )

    // ---------------------------------------------------------------------------------------
    // PulseEQ
    // ---------------------------------------------------------------------------------------
    val pulseEq = GuideContent(
        intro = "PulseEQ shapes the sound of audio you play inside it, and can also tune a few phones' " +
            "system sound. Tap a topic to open it.",
        introTa = "பல்ஸ்இக்யூ தனக்குள் இயக்கும் ஒலியின் தன்மையை மாற்றும்; சில போன்களில் கணினி ஒலியையும் " +
            "சரிசெய்யும். ஒரு தலைப்பைத் தொட்டு விரிவாக்குங்கள்.",
        topics = listOf(
            topic(
                "Choosing a file and playing it",
                "கோப்பைத் தேர்ந்தெடுத்து இயக்குதல்",
                "Choose an audio file, then use play and pause. The screen shows the position and, when the file " +
                    "has a known length, the total time; the seek bar for dragging is still to come, so today " +
                    "you restart a track to hear it from the beginning.",
                "ஒலிக் கோப்பைத் தேர்ந்தெடுத்து இயக்கு, இடைநிறுத்து பொத்தான்களைப் பயன்படுத்துங்கள். திரையில் தற்போதைய " +
                    "நிலையும், நீளம் தெரிந்தால் மொத்த நேரமும் தெரியும்; இழுக்கக்கூடிய சீக் பட்டை இன்னும் வரவில்லை, " +
                    "எனவே இப்போது மீண்டும் தொடக்கத்திலிருந்து கேட்க வேண்டும்.",
            ),
            topic(
                "The equalizer bands",
                "சமநிலைப்படுத்தி பட்டைகள்",
                "Each slider changes one frequency range: the left side is bass, the middle is voices, and the " +
                    "right side is treble. Small changes sound natural; pulling every band to the top mostly " +
                    "makes it louder and harsher. Preamp lowers the whole curve so big boosts cannot clip.",
                "ஒவ்வொரு ஸ்லைடரும் ஒரு அதிர்வெண் பகுதியை மாற்றும்: இடது பக்கம் பாஸ், நடு பகுதி குரல்கள், " +
                    "வலது பக்கம் ட்ரெபிள். சிறிய மாற்றங்கள் இயல்பாக ஒலிக்கும்; எல்லாப் பட்டைகளையும் மேலே " +
                    "இழுத்தால் பெரும்பாலும் சத்தம் கூடும், கடினமாகும். Preamp முழு வளைவையும் தாழ்த்தி, " +
                    "அதிகரிப்புகள் சிதைவடையாமல் தடுக்கும்.",
            ),
            topic(
                "Presets",
                "முன்னமைவுகள்",
                "A preset is only a saved set of band positions. Loading one does not change your file, and you " +
                    "can move any band afterwards. Saving your own preset keeps it on this phone; it is not " +
                    "uploaded anywhere.",
                "முன்னமைவு என்பது பட்டைகளின் நிலைகளின் சேமிக்கப்பட்ட தொகுப்பு மட்டுமே. ஒன்றை ஏற்றினால் உங்கள் " +
                    "கோப்பு மாறாது; பிறகு எந்தப் பட்டையையும் நகர்த்தலாம். உங்கள் சொந்த முன்னமைவு இந்தப் போனில் " +
                    "மட்டுமே சேமிக்கப்படும்; எங்கும் பதிவேற்றப்படாது.",
            ),
            topic(
                "Changing other apps' sound",
                "மற்ற செயலிகளின் ஒலியை மாற்றுதல்",
                "Only a few phones let one app tune the sound of other apps, and it depends on the maker and the " +
                    "Android version. The app says plainly whether this phone allows it; where it does not, " +
                    "PulseEQ only shapes its own playback. Nothing here can be forced on a phone that refuses.",
                "சில போன்களில் மட்டுமே ஒரு செயலி மற்ற செயலிகளின் ஒலியை மாற்ற முடியும்; அது தயாரிப்பாளரையும் " +
                    "Android பதிப்பையும் பொறுத்தது. இந்தப் போன் அனுமதிக்கிறதா என்பதை செயலி தெளிவாகக் கூறும்; " +
                    "இல்லாத போன்களில் பல்ஸ்இக்யூ தன் இயக்கத்தை மட்டுமே மாற்றும். மறுக்கும் போனில் எதையும் " +
                    "வலுக்கட்டாயமாகச் செய்ய முடியாது.",
            ),
            topic(
                "Updating PulseEQ",
                "பல்ஸ்இக்யூவைப் புதுப்பித்தல்",
                "The app checks the official GitHub releases itself and offers only a newer stable version after " +
                    "checking size, SHA-256, package, version and signing certificate. Android shows its own " +
                    "install screen and you can cancel there.",
                "அதிகாரப்பூர்வ GitHub வெளியீடுகளைத் தானே சரிபார்த்து, புதிய நிலையான பதிப்பை மட்டுமே வழங்கும்; " +
                    "அளவு, SHA-256, தொகுப்பு, பதிப்பு, கையொப்பம் சரிபார்க்கப்படும். நிறுவல் திரையை Android " +
                    "காட்டும்; அங்கே ரத்து செய்யலாம்.",
            ),
        ),
    )

    // ---------------------------------------------------------------------------------------
    // RP Vocal Remover Studio
    // ---------------------------------------------------------------------------------------
    val vocalRemover = GuideContent(
        intro = "RP Vocal Remover Studio takes the singing out of a song on this phone. Nothing is uploaded. " +
            "Tap a topic to open it.",
        introTa = "வோக்கல் ரிமூவர் ஸ்டுடியோ பாடலின் பாடும் குரலை இந்தப் போனிலேயே பிரிக்கும். எதுவும் " +
            "பதிவேற்றப்படாது. ஒரு தலைப்பைத் தொட்டு விரிவாக்குங்கள்.",
        topics = listOf(
            topic(
                "First steps",
                "முதல் படிகள்",
                "The first time, the app downloads its processing model, which needs internet; after that it " +
                    "works offline. Choose an audio file, choose Slow, Medium or Fast, and start. Slow gives the " +
                    "cleanest split; Fast is for a quick listen. Keep the phone cool and plugged in for a long song.",
                "முதல் முறை செயலாக்க மாதிரியைப் பதிவிறக்கும்; அதற்கு இணையம் தேவை, பிறகு இணையம் இல்லாமலும் " +
                    "இயங்கும். ஒலிக் கோப்பைத் தேர்ந்தெடுத்து Slow, Medium அல்லது Fast தேர்ந்து தொடங்குங்கள். " +
                    "Slow மிகத் தெளிவான பிரிப்பு; Fast விரைவாகக் கேட்க. நீண்ட பாடலுக்கு போனைக் குளிர்ச்சியாகவும் " +
                    "சார்ஜில் இணைத்தும் வைத்திருங்கள்.",
            ),
            topic(
                "What you get",
                "என்ன கிடைக்கும்",
                "The first pass writes an instrumental WAV next to your original. Your original file is never " +
                    "changed. Listen to the WAV before doing anything else; if singing remains, you can start one " +
                    "optional extra reduction pass, which may also soften some instruments.",
                "முதல் சுற்று உங்கள் மூலக் கோப்பருகே ஒரு இசைக்கருவி WAV-ஐ உருவாக்கும். மூலக் கோப்பு " +
                    "எப்போதும் மாறாது. மற்றதைச் செய்வதற்கு முன் WAV-ஐக் கேளுங்கள்; பாடுதல் மீதம் இருந்தால் " +
                    "விருப்பமான ஒரு கூடுதல் குறைப்புச் சுற்றைத் தொடங்கலாம்; அது சில இசைக்கருவிகளையும் " +
                    "மென்மையாக்கலாம்.",
            ),
            topic(
                "Pause, resume and long files",
                "இடைநிறுத்தம், தொடர்வு, நீண்ட கோப்புகள்",
                "Processing keeps running while the screen is off, and a long song can take many minutes. " +
                    "Pause and resume whenever you like. If Android stops the work because of low memory, heat " +
                    "or a storage problem, the app says so and keeps the job, and you choose when to continue.",
                "திரை அணைந்திருந்தாலும் செயலாக்கம் தொடரும்; நீண்ட பாடலுக்கு பல நிமிடங்கள் ஆகலாம். எப்போது " +
                    "வேண்டுமானாலும் இடைநிறுத்தி மீண்டும் தொடங்குங்கள். நினைவகக் குறைவு, வெப்பம் அல்லது " +
                    "சேமிப்புப் பிரச்சினையால் Android வேலையை நிறுத்தினால், செயலி அதைக் கூறி வேலையை வைத்திருக்கும்; " +
                    "எப்போது தொடர்வது என்பதை நீங்கள் தேர்வு செய்வீர்கள்.",
            ),
            topic(
                "Saving an MP3 as well",
                "MP3 ஆகவும் சேமித்தல்",
                "You can also export a 320 kb/s MP3 for sharing. MP3 is lossy: it throws away some detail to be " +
                    "small, so keep the WAV as the good copy. The MP3 is a separate file; the WAV is not replaced.",
                "பகிர்வதற்காக 320 kb/s MP3 ஆகவும் ஏற்றுமதி செய்யலாம். MP3 இழப்பு வடிவம்: சிறியதாக இருக்கச் சில " +
                    "விவரங்களை விட்டுவிடும், எனவே நல்ல நகலாக WAV-ஐ வைத்திருங்கள். MP3 தனிக் கோப்பு; WAV " +
                    "மாற்றப்படாது.",
            ),
            topic(
                "Playing the result",
                "முடிவை இயக்குதல்",
                "The player stays inside the app, so pause and stop are always on screen, and the volume slider " +
                    "sets how loud this app plays. A bar for jumping to a part of the song is not there yet.",
                "இயக்கி செயலிக்குள்ளேயே இருப்பதால் இடைநிறுத்தம், நிறுத்தம் எப்போதும் திரையில் இருக்கும்; " +
                    "ஒலி அளவு ஸ்லைடர் இந்தச் செயலி எவ்வளவு சத்தமாக இயக்கும் என்பதை அமைக்கும். பாடலின் ஒரு " +
                    "பகுதிக்குத் தாவும் பட்டை இன்னும் இல்லை.",
            ),
            topic(
                "Updating Vocal Remover",
                "வோக்கல் ரிமூவரைப் புதுப்பித்தல்",
                "The app checks the official GitHub releases itself and offers only a newer stable version, after " +
                    "checking the file's size, SHA-256, package, version and signing certificate. Android shows " +
                    "its own install screen.",
                "அதிகாரப்பூர்வ GitHub வெளியீடுகளைத் தானே சரிபார்த்து, புதிய நிலையான பதிப்பை மட்டுமே வழங்கும்; " +
                    "கோப்பின் அளவு, SHA-256, தொகுப்பு, பதிப்பு, கையொப்பம் சரிபார்க்கப்படும். நிறுவல் திரையை " +
                    "Android காட்டும்.",
            ),
        ),
    )

    // ---------------------------------------------------------------------------------------
    // Oor Arivippu
    // ---------------------------------------------------------------------------------------
    val oorArivippu = GuideContent(
        intro = "Oor Arivippu says the place and district you reach while you travel. Tap a topic to open it.",
        introTa = "ஊர் அறிவிப்பு நீங்கள் பயணிக்கும்போது அடையும் ஊர் மற்றும் மாவட்டத்தைக் கூறும். ஒரு தலைப்பைத் " +
            "தொட்டு விரிவாக்குங்கள்.",
        topics = listOf(
            topic(
                "Turning it on",
                "இயக்குதல்",
                "Tap the main switch, allow location, and keep the ongoing notification: Android needs it for " +
                    "tracking, and it is also your visible off switch. Tracking stops when you tap stop, and the " +
                    "notification says so.",
                "முதன்மை சுவிட்சைத் தட்டி, இருப்பிடத்தை அனுமதித்து, நிலை அறிவிப்பை வைத்திருங்கள்: கண்காணிப்புக்கு " +
                    "Android அதைக் கேட்கிறது, அதுவே உங்கள் தெரியும் நிறுத்தும் பொத்தான். நிறுத்தும் பொத்தானைத் " +
                    "தட்டியதும் கண்காணிப்பு நிற்கும்; அறிவிப்பும் அதைக் கூறும்.",
            ),
            topic(
                "What is announced",
                "என்ன அறிவிக்கப்படும்",
                "Only the names: the area, then the town, then the district as you move. No filler words, no " +
                    "\"Entering\", no walking directions. The same town is not repeated for ten minutes, so a " +
                    "Tamil/English spelling switch does not make it speak twice, and a road with no name is " +
                    "never announced.",
                "பெயர்கள் மட்டுமே: நகரும் போது பகுதி, பின்னர் ஊர், பின்னர் மாவட்டம். நிரப்பு வார்த்தைகள் இல்லை, " +
                    "\"நுழைகிறீர்கள்\" இல்லை, வழிகாட்டல் இல்லை. அதே ஊர் பத்து நிமிடங்களுக்கு மீண்டும் அறிவிக்கப்படாது; " +
                    "எனவே தமிழ்/ஆங்கில எழுத்து மாற்றம் இருமுறை பேசாது, பெயரில்லாத சாலை எப்போதும் அறிவிக்கப்படாது.",
            ),
            topic(
                "Choosing a destination",
                "இலக்கைத் தேர்ந்தெடுத்தல்",
                "Search a place and you get every match with its district, state, PIN code and distance. Pick " +
                    "one deliberately: the app never quietly takes the first result, and it speaks the name you " +
                    "chose as you approach it.",
                "ஓர் இடத்தைத் தேடினால், மாவட்டம், மாநிலம், அஞ்சல் குறியீடு, தூரத்துடன் எல்லாப் பொருத்தங்களும் " +
                    "கிடைக்கும். ஒன்றை வேண்டுமென்றே தேர்ந்தெடுங்கள்: முதல் முடிவை அமைதியாக எடுத்துக்கொள்ளாது; " +
                    "நீங்கள் தேர்ந்த இடத்தின் பெயரையே நெருங்கும்போது கூறும்.",
            ),
            topic(
                "Voices and speech speed",
                "குரல்கள் மற்றும் பேச்சு வேகம்",
                "Every Android voice of the chosen language is listed, best quality first, with a speed setting. " +
                    "A voice that needs internet is marked; without internet the app stays silent rather than " +
                    "guessing a place. The offline reminder uses a downloaded voice, or a short beep, and never a " +
                    "network voice.",
                "தேர்ந்த மொழியின் எல்லா Android குரல்களும், சிறந்த தரம் முதலில், வேக அமைப்புடன் பட்டியலிடப்படும். " +
                    "இணையம் தேவைப்படும் குரல் குறியிடப்படும்; இணையம் இல்லாதபோது இடத்தை ஊகிக்காமல் செயலி அமைதியாக " +
                    "இருக்கும். இணையமில்லா நினைவூட்டல் பதிவிறக்கிய குரலையோ சிறு பீப்பையோ பயன்படுத்தும்; " +
                    "இணையக் குரலை ஒருபோதும் இல்லை.",
            ),
            topic(
                "Calls and the headset button",
                "அழைப்புகள் மற்றும் ஹெட்செட் பொத்தான்",
                "Nothing is spoken while the phone is ringing or you are on a call: the announcement waits and " +
                    "plays after the call ends. With a headset, its button asks \"Where am I?\" and you get the " +
                    "current place once.",
                "போன் அடிக்கும்போதோ நீங்கள் அழைப்பில் இருக்கும்போதோ எதுவும் ஒலிக்காது: அறிவிப்பு காத்திருந்து " +
                    "அழைப்பு முடிந்த பிறகு ஒலிக்கும். ஹெட்செட்டின் பொத்தான் \"நான் எங்கே இருக்கிறேன்?\" என்று " +
                    "கேட்கும்; தற்போதைய இடம் ஒருமுறை கூறப்படும்.",
            ),
            topic(
                "Updating Oor Arivippu",
                "ஊர் அறிவிப்பைப் புதுப்பித்தல்",
                "The app checks the official GitHub releases itself and offers only a newer stable version after " +
                    "checking size, SHA-256, package, version and signing certificate. Android shows its own " +
                    "install screen.",
                "அதிகாரப்பூர்வ GitHub வெளியீடுகளைத் தானே சரிபார்த்து, புதிய நிலையான பதிப்பை மட்டுமே வழங்கும்; " +
                    "அளவு, SHA-256, தொகுப்பு, பதிப்பு, கையொப்பம் சரிபார்க்கப்படும். நிறுவல் திரையை Android " +
                    "காட்டும்.",
            ),
        ),
    )
}
