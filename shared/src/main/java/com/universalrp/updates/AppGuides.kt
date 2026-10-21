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
 * its own settings.gradle.kts and forked the shared updater), so it keeps its own detailed
 * bilingual guide in GuideScreen.kt, using the same tap-to-open shape. Copy the text here only if
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
                "Finding the tools",
                "கருவிகளைக் கண்டறிதல்",
                "The Home screen groups related readings into sections that open when tapped. Use the menu " +
                    "button at the top left (or swipe in from the left edge) to open the grouped cleaning, phone, " +
                    "network, security, assistant and settings screens. The small book button at the top of Home " +
                    "opens this guide directly.",
                "முகப்புத் திரையில் தொடர்புடைய அளவீடுகள் தலைப்புகளாகக் குழுவாக உள்ளன; தொட்டால் திறக்கும். " +
                    "சுத்தம், போன், நெட்வொர்க், பாதுகாப்பு, உதவியாளர், அமைப்புகள் திரைகளுக்குச் செல்ல மேல் இடது " +
                    "மெனு பொத்தானைத் தட்டுங்கள் அல்லது இடது ஓரத்திலிருந்து உள்ளே இழுக்கவும். Home மேல் உள்ள சிறிய " +
                    "புத்தகப் பொத்தான் இந்த வழிகாட்டியை நேரடியாகத் திறக்கும்.",
            ),
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
                "Open Local at the bottom. Choose a folder once to list only audio in that folder and its " +
                    "subfolders, or choose Open a file for one item. Tap a song to play it. While a local file is " +
                    "playing, the Local screen and Now Playing sheet show elapsed time, total length and a seek bar; " +
                    "drag it to any point in the recording. Live stations have no total duration and can seek only " +
                    "inside the short buffered window. Folder access is granted by Android's picker; the app does " +
                    "not scan the whole phone.",
                "கீழே உள்ள பாடல்கள் (Local) தாவலைத் திறக்கவும். ஒரு கோப்புறையைத் தேர்ந்தெடுத்தால் அதிலும் அதன் துணைக் " +
                    "கோப்புறைகளிலும் உள்ள ஒலி மட்டும் பட்டியலாகும்; அல்லது ஒரு கோப்பிற்கு கோப்பைத் திற (Open a file)-ஐத் " +
                    "தேர்ந்தெடுக்கவும். பாடலைத் தொட்டு இயக்குங்கள். உள்ளூர் கோப்பு இயங்கும்போது பாடல்கள் (Local) திரையிலும் " +
                    "Now Playing திரையிலும் கடந்த நேரம், மொத்த நீளம், சீக் பட்டை தெரியும்; பதிவின் எந்த இடத்திற்கும் " +
                    "இழுக்கலாம். நேரடி நிலையத்திற்கு மொத்த நேரம் கிடையாது; ஏற்கனவே இடையகத்தில் உள்ள சிறிய " +
                    "பகுதிக்குள் மட்டுமே நகரலாம். கோப்புறை அனுமதியை Android தேர்வி தரும்; போன் முழுவதையும் " +
                    "செயலி ஸ்கேன் செய்யாது.",
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
                "Favourites, your stations, and sleep timer",
                "விருப்ப நிலையங்கள், உங்கள் நிலையங்கள், தூக்க நேரம்",
                "Tap the star on a station to add or remove it from Favourites; open the Favourites category " +
                    "to find it again. In More, add your own station with its name and a direct stream URL (prefer " +
                    "HTTPS; some legacy HTTP streams are not encrypted). The Sleep timer stops playback after " +
                    "the selected time. More also has a switch to stop the last station from auto-starting when " +
                    "you open the app; your favourites and settings stay on this phone.",
                "நிலையத்தில் உள்ள நட்சத்திரத்தைத் தொட்டு விருப்பத்தில் சேர்க்கவும் அல்லது நீக்கவும்; மீண்டும் காண " +
                    "விருப்பமானவை (Favourites) வகையைத் திறக்கவும். மேலும் (More) பகுதியில் பெயரும் நேரடி ஒளிபரப்பு இணைப்பும் கொடுத்து " +
                    "உங்கள் நிலையத்தைச் சேர்க்கலாம் (HTTPS விரும்பத்தக்கது; பழைய HTTP இணைப்புகள் குறியாக்கம் " +
                    "செய்யப்படாமல் இருக்கலாம்). தேர்ந்த நேரம் முடிந்ததும் தூக்க நேரக் கணிப்பான் (Sleep timer) ஒலிப்பை நிறுத்தும். " +
                    "செயலியைத் திறக்கும்போது கடைசி நிலையம் தானாகத் தொடங்குவதை மேலும் (More)-இல் முடக்கலாம்; விருப்பங்களும் " +
                    "அமைப்புகளும் இந்தப் போனிலேயே இருக்கும்.",
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
                "Choose a local audio file, then use Play / pause or Stop. The player shows the current position " +
                    "and the total length when Android can read it. There is not yet a drag-to-seek control, so " +
                    "use Stop and choose the file again when you need to restart it. Streaming is not supported " +
                    "in this built-in player.",
                "சாதனத்தில் உள்ள ஒலிக் கோப்பைத் தேர்ந்தெடுத்து Play / pause அல்லது Stop பயன்படுத்துங்கள். Android " +
                    "கண்டறிந்தால் தற்போதைய இடமும் மொத்த நீளமும் தெரியும். இழுத்துத் தேடும் கட்டுப்பாடு இன்னும் " +
                    "இல்லை; மீண்டும் தொடங்க Stop செய்து கோப்பை மறுபடியும் தேர்ந்தெடுக்கவும். இந்த உள்ளமைந்த " +
                    "இயக்கியில் இணைய ஒலிபரப்பு ஆதரிக்கப்படாது.",
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
                "Reset, bypass, and the spectrum",
                "மீட்டமை, பைபாஸ், ஒலி அலைவடிவம்",
                "Drag a band to change its frequency range; double-tap its label to return just that band to " +
                    "zero. Reset to flat clears the whole curve. Bypass temporarily turns processing off without " +
                    "erasing the curve, so you can compare the original and adjusted sound. The animated spectrum " +
                    "is a visual aid, not a measurement of sound quality.",
                "ஒரு அதிர்வெண் பகுதியை மாற்ற அதன் பட்டையை இழுக்கவும்; அந்தப் பட்டையை மட்டும் பூஜ்யத்திற்குத் " +
                    "திருப்ப அதன் பெயரை இருமுறை தட்டவும். Reset to flat முழு வளைவையும் சமமாக்கும். Bypass " +
                    "வளைவை அழிக்காமல் செயலாக்கத்தைத் தற்காலிகமாக அணைத்து, அசல் ஒலியுடன் ஒப்பிட உதவும். நகரும் " +
                    "ஒலி அலைவடிவம் காட்சிக்கான உதவி மட்டுமே; ஒலி தர அளவீடு அல்ல.",
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
                    "works offline. Choose one or more MP3 files, select Slow, Medium or Fast, then start the " +
                    "queue. Keep the phone cool and connected to power for long songs; you can pause or cancel " +
                    "the job from its progress card.",
                "முதல் முறை செயலாக்க மாதிரியைப் பதிவிறக்கும்; அதற்கு இணையம் தேவை, பிறகு இணையம் இல்லாமலும் " +
                    "இயங்கும். ஒன்று அல்லது பல MP3 கோப்புகளைத் தேர்ந்தெடுத்து Slow, Medium அல்லது Fast முறையைத் " +
                    "தேர்ந்து வரிசையைத் தொடங்குங்கள். நீண்ட பாடலுக்கு போனைக் குளிர்ச்சியாகவும் மின்சாரத்தில் " +
                    "இணைத்தும் வைத்திருங்கள்; முன்னேற்ற அட்டையிலிருந்து இடைநிறுத்தவோ ரத்து செய்யவோலாம்.",
            ),
            topic(
                "Choosing a processing speed",
                "செயலாக்க வேகத்தைத் தேர்வு",
                "Slow uses one inference thread and longer cool-downs for lower device load. Medium balances " +
                    "time and CPU use. Fast can use more threads when Android reports a normal thermal state; it " +
                    "may run hotter and is not guaranteed to finish sooner on every phone. The app may pause a " +
                    "job for safety and show you what to do next.",
                "Slow ஒரு inference thread மற்றும் நீண்ட குளிர்விப்பு இடைவெளிகளைப் பயன்படுத்தி சாதனச் சுமையைக் " +
                    "குறைக்கும். Medium நேரத்தையும் CPU பயன்பாட்டையும் சமப்படுத்தும். Android வெப்ப நிலை இயல்பாக " +
                    "இருக்கும்போது Fast அதிக thread-களைப் பயன்படுத்தலாம்; போன் அதிகம் சூடாகலாம், எல்லா " +
                    "போன்களிலும் விரைவாக முடியும் என உறுதி இல்லை. பாதுகாப்பிற்காக வேலை இடைநிறுத்தப்பட்டால் " +
                    "அடுத்து என்ன செய்ய வேண்டும் என்பதைச் செயலி காட்டும்.",
            ),
            topic(
                "What you get",
                "என்ன கிடைக்கும்",
                "The first pass saves a separate instrumental stereo WAV in Music/Vocal Remover Studio. The " +
                    "original is never changed. Listen to the WAV before doing anything else; if singing remains, " +
                    "you can start one optional extra-reduction pass, which may also soften some instruments and " +
                    "creates another separate WAV.",
                "முதல் சுற்று Music/Vocal Remover Studio-வில் தனி இசைக்கருவி ஸ்டீரியோ WAV-ஐச் சேமிக்கும். " +
                    "அசல் கோப்பு எப்போதும் மாறாது. வேறு செயலைச் செய்வதற்கு முன் WAV-ஐக் கேளுங்கள்; பாடுதல் " +
                    "மீதமிருந்தால் விருப்பமான ஒரு கூடுதல் குறைப்புச் சுற்றைத் தொடங்கலாம்; அது சில கருவியிசையையும் " +
                    "மென்மையாக்கி இன்னொரு தனி WAV உருவாக்கும்.",
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
                "Queue, saved work, and safety pauses",
                "வரிசை, சேமித்த பணி, பாதுகாப்பு இடைநிறுத்தங்கள்",
                "Songs in the queue are processed one at a time. The app saves progress checkpoints so a long " +
                    "job can resume after a restart. A low-memory pause may resume automatically after recovery; " +
                    "thermal, storage and user pauses wait for you. Check the reason shown on the job card, free " +
                    "space or let the phone cool, then tap Resume when it is safe.",
                "வரிசையிலுள்ள பாடல்கள் ஒவ்வொன்றாகச் செயலாக்கப்படும். நீண்ட வேலை மறுதொடக்கத்திற்குப் பிறகும் " +
                    "தொடர முன்னேற்றச் சோதனைப் புள்ளிகளைச் செயலி சேமிக்கும். நினைவகம் குறைந்து நின்றது மீண்டதும் " +
                    "தானாகத் தொடரலாம்; வெப்பம், சேமிப்பு, பயனர் இடைநிறுத்தங்கள் உங்கள் செயலைக் காத்திருக்கும். " +
                    "வேலை அட்டையில் காட்டும் காரணத்தைப் பார்த்து, இடம் காலி செய்யவோ போனை குளிரவைக்கவோ செய்து, " +
                    "பாதுகாப்பானதும் Resume தட்டுங்கள்.",
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
                "As you travel, the app can announce a new town or village, an optional new sub-area in the " +
                    "same town, a changed district, and a recognized highway/major landmark name. It speaks " +
                    "place names without filler such as \"Entering\" and gives no turn-by-turn directions. " +
                    "Repeat protection avoids saying the same town again for ten minutes; a language/script " +
                    "change alone does not trigger another place announcement.",
                "பயணத்தின் போது புதிய ஊர் அல்லது கிராமம், ஒரே ஊருக்குள் விருப்பமாக புதிய பகுதி, மாறிய மாவட்டம், " +
                    "அடையாளம் காணப்பட்ட நெடுஞ்சாலை/முக்கிய இடப் பெயர் ஆகியவற்றைச் செயலி அறிவிக்கலாம். \"நுழைகிறீர்கள்\" " +
                    "போன்ற நிரப்பு வார்த்தைகள் இல்லாமல் இடப் பெயர்களைச் சொல்லும்; திருப்பம்-திருப்பமான வழிகாட்டல் " +
                    "இல்லை. அதே ஊரை பத்து நிமிடங்களுக்கு மீண்டும் சொல்லாமல் தடுக்கும்; மொழி/எழுத்து மாற்றம் மட்டும் " +
                    "இன்னொரு அறிவிப்பைத் தூண்டாது.",
            ),
            topic(
                "Choosing a destination",
                "இலக்கைத் தேர்ந்தெடுத்தல்",
                "Enter a destination and tap Set destination. When names match multiple places, compare the district, " +
                    "state, PIN code and distance, then choose the right result yourself; the app never silently " +
                    "selects the first match. With internet available, it uses GPS/geocoder distance for alerts " +
                    "at about 10 km, 5 km, 2 km and 500 m (arrival). GPS and geocoder accuracy can vary.",
                "இலக்கை உள்ளிட்டு “இலக்கை அமை” தட்டுங்கள். ஒரே பெயரில் பல இடங்கள் வந்தால் மாவட்டம், மாநிலம், அஞ்சல் " +
                    "குறியீடு, தூரத்தை ஒப்பிட்டு சரியான முடிவை நீங்களே தேர்ந்தெடுக்கவும்; முதல் முடிவைச் செயலி " +
                    "அமைதியாகத் தேர்ந்தெடுக்காது. இணையம் இருக்கும்போது GPS/Geocoder தூரத்தைப் பயன்படுத்தி சுமார் " +
                    "10 km, 5 km, 2 km, 500 m (வந்தடைந்தது) இடங்களில் அறிவிக்கும். GPS/Geocoder துல்லியம் மாறலாம்.",
            ),
            topic(
                "Voices and speech speed",
                "குரல்கள் மற்றும் பேச்சு வேகம்",
                "The compact App language row changes the interface only; it is separate from the compact " +
                    "Voice language row. Tap Voice language to expand it, then choose the announcement language, " +
                    "an Android TTS voice, Slow/Normal/Fast speech speed and this app's own speech-volume slider; " +
                    "test a voice after changing it. Voices that need internet are marked and may send spoken text to " +
                    "their TTS provider. For offline reminders, the app uses an installed on-device voice or a " +
                    "short beep, never a network voice.",
                "சுருக்கமான செயலி மொழி (App language) வரி திரை மொழியை மட்டுமே மாற்றும்; அது குரல் மொழி " +
                    "(Voice language) வரியிலிருந்து தனி. குரல் மொழி வரியைத் தொட்டு விரிவாக்கி அறிவிப்பு மொழி, Android TTS குரல், " +
                    "மெதுவான/இயல்பு/வேகமான " +
                    "பேச்சு, இந்தச் செயலிக்கான குரல் ஒலி ஸ்லைடரைத் தேர்ந்தெடுக்கவும்; மாற்றிய பிறகு குரலைச் " +
                    "சோதியுங்கள். இணையம் தேவைப்படும் குரல்கள் குறிக்கப்படும்; பேசிய உரை " +
                    "அந்த TTS வழங்குநருக்கு அனுப்பப்படலாம். இணையமில்லா நினைவூட்டலுக்கு நிறுவிய சாதனக் குரலையோ " +
                    "சிறு பீப்பையையோ பயன்படுத்தும்; இணையக் குரல் இல்லை.",
            ),
            topic(
                "Travel and repeat settings",
                "பயணம் மற்றும் மீண்டும் அறிவிப்பு அமைப்புகள்",
                "Bike/car detection can keep announcements quiet while you are stationary. The minimum-distance " +
                    "control filters small GPS changes. Turn on the same-city option only if you want new areas " +
                    "within one town announced; it is off by default. The option to announce immediately when " +
                    "tracking starts can also be changed. Adaptive location intervals vary by movement; actual " +
                    "battery use differs by phone.",
                "பைக்/கார் பயணக் கண்டறிதல், ஒரே இடத்தில் இருக்கும்போது அறிவிப்பை அமைதியாக வைத்திருக்கலாம். " +
                    "குறைந்தபட்ச தூரக் கட்டுப்பாடு சிறிய GPS மாற்றங்களை வடிகட்டும். ஒரே ஊருக்குள் புதிய பகுதிகளையும் " +
                    "அறிவிக்க வேண்டும் என்றால் same-city விருப்பத்தை இயக்குங்கள்; இயல்பாக அது முடக்கப்பட்டுள்ளது. " +
                    "கண்காணிப்பைத் தொடங்கியவுடன் அறிவிக்கும் விருப்பத்தையும் மாற்றலாம். நகர்வைப் பொறுத்து இருப்பிட " +
                    "இடைவெளிகள் மாறும்; உண்மையான பேட்டரி பயன்பாடு போனைப் பொறுத்தது.",
            ),
            topic(
                "No internet, location and privacy",
                "இணையம், இருப்பிடம், தனியுரிமை",
                "Place and destination lookups/announcements pause when validated internet is unavailable; " +
                    "offline reminders are optional and use only an on-device TTS voice or a beep. Location " +
                    "permission is needed for trip tracking, and Android may require background-location access " +
                    "to continue with the screen off. GPS/geocoder results and battery use vary by device. The " +
                    "app keeps its settings and announcement history on this phone; Android's geocoder provider " +
                    "may process lookups under its own policy.",
                "சரிபார்க்கப்பட்ட இணையம் இல்லாதபோது இடம்/இலக்கு தேடலும் அறிவிப்பும் நிற்கும்; இணையமில்லா " +
                    "நினைவூட்டல் விருப்பமானது, சாதன TTS குரல் அல்லது பீப்பை மட்டுமே பயன்படுத்தும். பயணத்தைக் " +
                    "கண்காணிக்க இருப்பிட அனுமதி தேவை; திரை அணைந்தபோதும் தொடர Android பின்னணி இருப்பிட அனுமதி " +
                    "கேட்கலாம். GPS/Geocoder முடிவுகளும் பேட்டரி பயன்பாடும் சாதனத்தைப் பொறுத்து மாறும். " +
                    "அமைப்புகளையும் அறிவிப்பு வரலாறையும் செயலி இந்தப் போனில் வைத்திருக்கும்; Android " +
                    "Geocoder வழங்குநர் தனது கொள்கைப்படி தேடலைச் செயலாக்கலாம்.",
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
