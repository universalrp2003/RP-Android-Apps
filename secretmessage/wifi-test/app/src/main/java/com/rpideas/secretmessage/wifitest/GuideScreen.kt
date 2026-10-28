package com.rpideas.secretmessage.wifitest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.rpideas.secretmessage.preview.ui.Panel
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rpideas.secretmessage.preview.ui.Accent
import com.rpideas.secretmessage.preview.ui.Ink

/** The guide in Tamil and English (owner request for 1.1.0); it follows the app's language. */
@Composable
internal fun GuideScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    val sections = if (Tr.tamil) TAMIL_GUIDE else ENGLISH_GUIDE
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Guide"), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            LanguageToggle()
        }
        // Owner request: the same "tap a topic, the text opens" shape as the other five apps.
        // This app keeps its own longer 1.1.0 text because it is a separate Gradle build that
        // cannot see shared/AppGuides.kt; the interaction is deliberately identical.
        var open by remember { mutableStateOf<String?>(null) }
        sections.forEach { (title, body) ->
            val isOpen = open == title
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Panel,
                modifier = Modifier.fillMaxWidth().clickable { open = if (isOpen) null else title },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            color = if (isOpen) Accent else Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(if (isOpen) "−" else "+", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    if (isOpen) AppNote(body)
                }
            }
        }
        TextButton(onClick = onBack) { Text(tr("Back")) }
        Spacer(Modifier.height(12.dp))
    }
}

private val ENGLISH_GUIDE = listOf(
    "1. Start a chat with a new phone" to
        "Choose \"Start a chat\" to connect through the Internet relay, or \"Start on the same Wi-Fi\" when both " +
        "phones are on the same trusted Wi-Fi network. Send the invite by SMS or another app. On the other phone, " +
        "tap the link, or copy it and choose \"Paste invite and join\". Both people compare every digit of the " +
        "20-digit safety code. Tap \"Codes match\" only when all digits are identical; otherwise tap \"Codes differ\".",
    "2. Call a remembered phone" to
        "After the first chat the phone appears under \"Remembered phones\". Tap its name to give it a name. Tap " +
        "\"Chat\": the other phone gets a notification, \"Someone you remember wants to chat\", and the app asks there " +
        "too. Tap it or \"Answer\": the chat opens on both phones without comparing the code again, because the key " +
        "is unchanged. If both phones tap Chat at the same time, they connect by themselves. A green dot " +
        "means the phone is ready now; grey means it is not reachable now. Both phones need Secret Message 1.1.0 " +
        "or newer and must have chatted once with it.",
    "3. Messages" to
        "Type with the built-in English keyboard, or open the drawing pad to draw a picture (the default) or letters " +
        "in any language. Messages disappear from both phones after they are viewed and their timer ends (60 to 300 " +
        "seconds, set in Settings). Undelivered messages are removed after 30 seconds. Message content is not " +
        "saved as chat history; remembered phones and settings are stored on this device.",
    "4. Timer, tone, and app lock" to
        "Open Settings to choose the message-view timer (60–300 seconds), select and preview the incoming-message " +
        "tone, and optionally protect the app with the device's biometric or screen lock. The timer cannot be " +
        "turned off; it begins when the recipient views the message. Use app lock on a device whose screen lock " +
        "you control.",
    "5. Keep Secret Message ready for chats" to
        "In Settings keep \"Ready for chats\" on, tap \"Allow background use\" and allow it. Keep the \"Ready for " +
        "chats\" notification; it says nothing else. Xiaomi, Redmi and POCO: Settings > Apps > Manage apps > Secret " +
        "Message > Autostart: on, and Battery saver: No restrictions. Samsung: Settings > Battery > Background usage " +
        "limits: do not put Secret Message in sleeping apps. If a call says \"Not reachable\", open Secret Message " +
        "on the other phone once.",
    "6. Safety and privacy" to
        "If the app shows \"WARNING: the key ... has changed\", compare every digit, and tap \"Codes differ\" if you " +
        "are unsure. Reset key and Forget remembered phones are in Settings. When you use the Internet relay, it can " +
        "see both phones' IP addresses, connection times and traffic sizes, plus the invite token, device IDs and public " +
        "keys sent before the safety-code check. Chat requests and messages are encrypted; the relay cannot read " +
        "messages, drawings or " +
        "pictures, and it does not keep chat history; Cloudflare may keep account-level service statistics. The same-Wi-Fi " +
        "option avoids the Internet relay but does not make an untrusted Wi-Fi network safe. This app has not been " +
        "independently security-reviewed.",
    "7. Updates" to
        "Check updates in About. Explicit user action opens Tamilan Store or the app download section on our website. Browser is always optional. This app never downloads or installs APKs. Daily in-app checks are opt-in; Later and Skip version are available. Stop if a security warning appears.",
)

private val TAMIL_GUIDE = listOf(
    "1. புதிய தொலைபேசியுடன் உரையாடலைத் தொடங்க" to
        "இணைய relay வழியாக இணைக்க “உரையாடலைத் தொடங்கு” (Start a chat) என்பதைத் தேர்ந்தெடுக்கவும்; இரு போன்களும் " +
        "ஒரே நம்பகமான Wi-Fi-யில் இருந்தால் “இணைப்புச் சேவையகம் இல்லாமல், அதே Wi-Fi-இல் தொடங்கு” " +
        "(Start on the same Wi-Fi, without the relay) என்பதைத் " +
        "தேர்ந்தெடுக்கவும். அழைப்பிதழை SMS அல்லது மற்றொரு செயலி மூலம் அனுப்புங்கள். மற்ற போனில் இணைப்பைத் தொடவும் " +
        "அல்லது நகலெடுத்து “அழைப்பிதழை ஒட்டி இணை” என்பதைத் தேர்ந்தெடுக்கவும். இருவரும் 20 இலக்கப் பாதுகாப்புக் " +
        "குறியீட்டின் ஒவ்வொரு இலக்கத்தையும் ஒப்பிடுங்கள். எல்லா இலக்கங்களும் ஒன்றாக இருந்தால் மட்டுமே “குறியீடுகள் " +
        "பொருந்துகின்றன”; இல்லையெனில் “குறியீடுகள் வேறுபடுகின்றன” தட்டவும்.",
    "2. நினைவில் உள்ள தொலைபேசியை அழைக்க" to
        "முதல் உரையாடலுக்குப் பிறகு அந்தத் தொலைபேசி “நினைவில் உள்ள தொலைபேசிகள்” பட்டியலில் வரும். பெயர் வைக்க " +
        "அதன் பெயரைத் தொடவும். “உரையாடு” என்பதைத் தொட்டால், மற்ற தொலைபேசிக்கு “நீங்கள் நினைவில் வைத்த " +
        "ஒருவர் உரையாட அழைக்கிறார்” என்ற அறிவிப்பு வரும்; செயலியிலும் கேட்கும். அதை அல்லது “பதிலளி” என்பதைத் " +
        "தொட்டதும், சாவி மாறாததால் குறியீட்டை மீண்டும் ஒப்பிடாமலே இரண்டு தொலைபேசிகளிலும் உரையாடல் திறக்கும். " +
        "இரண்டு தொலைபேசிகளும் ஒரே நேரத்தில் “உரையாடு” என்பதைத் தொட்டால், அவை தானாகவே இணையும். பச்சைப் புள்ளி: " +
        "இப்போது தயார். சாம்பல் புள்ளி: " +
        "இப்போது தொடர்பு கொள்ள முடியாது. இரண்டு தொலைபேசிகளிலும் Secret Message 1.1.0 அல்லது புதிய பதிப்பு " +
        "இருக்க வேண்டும்; அதில் ஒருமுறை உரையாடியிருக்க வேண்டும்.",
    "3. செய்திகள்" to
        "உள்ளமைந்த ஆங்கில விசைப்பலகையில் தட்டச்சு செய்யலாம், அல்லது வரைபலகையைத் திறந்து படம் (இயல்பு) அல்லது " +
        "எந்த மொழியிலும் எழுத்துகளை வரையலாம். செய்திகள் பார்க்கப்பட்டு அவற்றின் நேரம் முடிந்ததும் இரண்டு " +
        "தொலைபேசிகளிலிருந்தும் மறைந்துவிடும் (60 முதல் 300 வினாடிகள், அமைப்புகளில் மாற்றலாம்). சேராத செய்திகள் " +
        "30 வினாடிகளில் நீக்கப்படும். செய்தி உள்ளடக்கம் chat history ஆகச் சேமிக்கப்படாது; நினைவில் உள்ள " +
        "தொலைபேசிகளும் அமைப்புகளும் இந்தச் சாதனத்தில் சேமிக்கப்படும்.",
    "4. நேரம், ஒலி, செயலிப் பூட்டு" to
        "அமைப்புகள் (Settings)-இல் பெறுநர் பார்க்கும் நேரத்தை (60–300 வினாடிகள்) தேர்வு செய்யலாம்; வரும் " +
        "செய்திக்கான ஒலியைத் " +
        "தேர்ந்தெடுத்து முன்னோட்டமாகக் கேட்கலாம்; விருப்பமாக சாதனத்தின் கைரேகை அல்லது திரைப் பூட்டால் " +
        "செயலியைப் பாதுகாக்கலாம். நேரத்தை முழுமையாக முடக்க முடியாது; பெறுபவர் செய்தியைப் பார்த்ததும் அது " +
        "தொடங்கும். நீங்கள் கட்டுப்படுத்தும் சாதனத் திரைப் பூட்டை பயன்படுத்துங்கள்.",
    "5. Secret Message-ஐ உரையாடலுக்குத் தயாராக வைத்திருக்க" to
        "அமைப்புகளில் “உரையாடலுக்குத் தயார்” என்பதை இயக்கத்தில் வைத்து, “பின்னணியில் இயங்க அனுமதி” என்பதைத் " +
        "தொட்டு அனுமதியுங்கள். “உரையாடலுக்குத் தயார்” அறிவிப்பை அணைக்க வேண்டாம்; அதில் வேறு எதுவும் இருக்காது. " +
        "Xiaomi, Redmi, POCO: Settings > Apps > Manage apps > Secret Message > Autostart இயக்கவும்; Battery saver: " +
        "No restrictions. Samsung: Settings > Battery > Background usage limits-இல் Secret Message-ஐ sleeping apps " +
        "பட்டியலில் சேர்க்க வேண்டாம். அழைப்பில் “இப்போது தொடர்பு கொள்ள முடியாது” என்று வந்தால், மற்ற " +
        "தொலைபேசியில் Secret Message-ஐ ஒருமுறை திறக்கவும்.",
    "6. பாதுகாப்பும் தனியுரிமையும்" to
        "“எச்சரிக்கை: சாவி மாறியுள்ளது” என்று வந்தால் ஒவ்வொரு இலக்கத்தையும் ஒப்பிடுங்கள்; சந்தேகம் இருந்தால் " +
        "“குறியீடுகள் வேறுபடுகின்றன” என்பதைத் தொடவும். “சாவியை மீட்டமை” மற்றும் “நினைவில் உள்ள தொலைபேசிகளை " +
        "மறத்தல்” ஆகியவை அமைப்புகளில் உள்ளன. இணைய relay பயன்படுத்தினால், இரு போன்களின் IP முகவரி, இணைப்பு நேரம், தரவு அளவு, " +
        "குறியீடு ஒப்பிடுவதற்கு முன் அனுப்பப்படும் invite token, சாதன ID, பொது விசைகள் ஆகியவற்றை அது பார்க்க முடியும். " +
        "உரையாடல் அழைப்புகளும் செய்திகளும் குறியாக்கப்பட்டவை; செய்தி, வரைபடம், படங்களை relay படிக்க முடியாது; " +
        "அரட்டை வரலாற்றையும் " +
        "வைத்திருக்காது. Cloudflare கணக்கு நிலைச் சேவை புள்ளிவிவரங்களை வைத்திருக்கலாம். ஒரே Wi-Fi வழி இணைய relay-ஐத் " +
        "தவிர்க்கும்; ஆனால் நம்பகமற்ற Wi-Fi-ஐப் பாதுகாப்பானதாக்காது. இந்தச் செயலி சுயாதீனப் பாதுகாப்பு மதிப்பாய்வு " +
        "செய்யப்படவில்லை.",
    "7. புதுப்பிப்புகள்" to
        "செயலி விவரத்தில் புதுப்பிப்பைச் சரிபார்க்கலாம். உங்கள் தேர்வில் தமிழன் அங்காடி அல்லது இணையதளப் பதிவிறக்கப் பகுதி திறக்கும்; உலாவியை எப்போதும் தேர்ந்தெடுக்கலாம். இந்தச் செயலி APK பதிவிறக்கவோ நிறுவவோ செய்யாது. தினசரி சரிபார்ப்பு விருப்பமானது; பிறகு மற்றும் பதிப்பைத் தவிர்க்கும் வசதிகள் உள்ளன. பாதுகாப்பு எச்சரிக்கை வந்தால் நிறுத்துங்கள்.",
)
