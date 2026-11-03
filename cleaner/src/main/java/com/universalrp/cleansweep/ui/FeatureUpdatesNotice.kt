package com.universalrp.cleansweep.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.universalrp.cleansweep.data.FeatureDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** First foreground opening after a feature arrives, not a hidden Settings-only control. */
@Composable
fun FeatureUpdatesNotice(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var feature by remember { mutableStateOf<FeatureDiscovery.Feature?>(null) }
    var enabled by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(resumed) {
        if (resumed) {
            val next = withContext(Dispatchers.IO) { FeatureDiscovery.next(context)?.let { it to FeatureDiscovery.enabled(context, it) } }
            feature = next?.first
            enabled = next?.second ?: false
        }
    }
    val current = feature ?: return
    if (!resumed) return
    fun choose(choice: FeatureDiscovery.Choice) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val (saved, state) = withContext(Dispatchers.IO) {
                    FeatureDiscovery.respond(context, current, choice) to FeatureDiscovery.enabled(context, current)
                }
                enabled = state
                error = !saved
                if (saved) {
                    onChanged()
                    val next = withContext(Dispatchers.IO) { FeatureDiscovery.next(context)?.let { it to FeatureDiscovery.enabled(context, it) } }
                    feature = next?.first; enabled = next?.second ?: false
                }
            } finally { busy = false }
        }
    }
    // Exhaustive compiled content: a future feature cannot accidentally inherit this description.
    val title = when (current) {
        FeatureDiscovery.Feature.DISASTER_NOTIFICATIONS -> watchWords("New: silent disaster notifications", "புதிய வசதி: ஒலியில்லா பேரிடர் அறிவிப்புகள்")
        FeatureDiscovery.Feature.SECURITY_HISTORY -> watchWords("New: security change review", "புதிய வசதி: பாதுகாப்பு மாற்ற ஆய்வு")
        FeatureDiscovery.Feature.DISASTER_NEWS -> watchWords("New: disaster updates with news", "புதிய வசதி: செய்தியுடன் பேரிடர் அறிக்கைகள்")
    }
    val ask = current.mode == FeatureDiscovery.Mode.ASK_FIRST
    AlertDialog(
        onDismissRequest = { choose(if (ask) FeatureDiscovery.Choice.NOT_NOW else FeatureDiscovery.Choice.ACKNOWLEDGE) },
        title = { Text(title) },
        text = {
            when (current) {
            FeatureDiscovery.Feature.DISASTER_NOTIFICATIONS -> Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(watchWords("One silent notification for each new verified cyclone advisory or new flood/earthquake event found during existing disaster-with-news checks. Tap it to open the source report. No extra speech, sound, vibration or separate background polling; unchanged/older reports are not repeated. Not an emergency-alert service or a local impact prediction.",
                    "செய்தியுடன் பேரிடர் சரிபார்ப்பு நடக்கும்போது கண்டறியும் புதிய சரிபார்த்த புயல் அறிக்கை அல்லது புதிய வெள்ளம்/நிலநடுக்க நிகழ்வுக்கு ஓர் ஒலியில்லா அறிவிப்பு. அதைத் தொட்டால் மூல அறிக்கை திறக்கும். கூடுதல் பேச்சு, ஒலி, அதிர்வு அல்லது தனிப் பின்னணிச் சரிபார்ப்பு இல்லை; அதே/பழைய அறிக்கை மீளாது. இது அவசர எச்சரிக்கை சேவையோ உள்ளூர் பாதிப்புக் கணிப்போ அல்ல."))
                Text(watchWords("Optional and off until enabled. Disaster updates with news and Android notifications must also be allowed. Your news speech and its read counts stay unchanged. Change this later under Rain & safety → Disaster reports.",
                    "விருப்ப வசதி; நீங்கள் இயக்கும் வரை அணைந்திருக்கும். செய்தியுடன் பேரிடர் அறிக்கைகளும் Android அறிவிப்புகளும் அனுமதிக்கப்பட்டிருக்க வேண்டும். செய்திப் பேச்சும் வாசிப்பு எண்ணிக்கையும் மாறாது. பின்னர் மழை மற்றும் பாதுகாப்பு → பேரிடர் அறிக்கைகளில் மாற்றலாம்."))
                if (error) Text(watchWords("Could not save. Please try again.", "சேமிக்க முடியவில்லை. மீண்டும் முயலவும்."))
            }
            FeatureDiscovery.Feature.SECURITY_HISTORY -> Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(watchWords("Review newly observed access, installer and signing-identity changes during security checks. Android-verified signing rotation is distinguished from an unexplained change; neither proves malware or safety. Powerful access combinations get a clear explanation.",
                    "பாதுகாப்புச் சரிபார்ப்பில் புதிதாகக் கண்ட அணுகல், நிறுவிய மூலம், கையொப்ப அடையாள மாற்றங்களைப் பார்க்கலாம். Android உறுதிசெய்த கையொப்பச் சுழற்சி தனியாகக் காட்டப்படும்; மாற்றம் மட்டும் தீம்பொருள் அல்லது பாதுகாப்புச் சான்றல்ல. பல வலுவான அணுகல்கள் சேர்ந்திருந்தால் காரணம் விளக்கப்படும்."))
                Text(watchWords("Enable stores up to 200 change events privately on this phone, including package/signing evidence. No APK upload, new permission, automatic app removal or continuous spying. First check establishes a baseline. Use Security → What changed? to review or turn recording off. Not now keeps your existing choice.",
                    "இயக்கினால் தொகுப்பு/கையொப்பச் சான்றுடன் அதிகபட்சம் 200 மாற்றங்கள் இந்தப் போனில் மட்டும் சேமிக்கப்படும். APK பதிவேற்றம், புதிய அனுமதி, தானியங்கி செயலி நீக்கம் அல்லது தொடர்ந்த உளவு இல்லை. முதல் சரிபார்ப்பு அடிப்படை நிலை. பாதுகாப்பு → மாற்ற வரலாற்றில் பார்க்கவோ பதிவை அணைக்கவோ முடியும். இப்போது வேண்டாம் என்பது பழைய தேர்வைப் பாதுகாக்கும்."))
                Text(if (enabled) watchWords("Local history is already enabled.", "உள்ளூர் வரலாறு ஏற்கெனவே இயக்கத்தில் உள்ளது.") else watchWords("Local history is currently off.", "உள்ளூர் வரலாறு தற்போது அணைக்கப்பட்டுள்ளது."))
                if (error) Text(watchWords("Could not save. Please try again.", "சேமிக்க முடியவில்லை. மீண்டும் முயலவும்."))
            }
            FeatureDiscovery.Feature.DISASTER_NEWS -> Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(watchWords("Hear the latest verified cyclone advisory once in the next completed news bulletin. Unchanged/older advisories are not repeated; newer pending details replace older ones. Floods and earthquakes still appear in three completed bulletins. Distances use a fresh enabled named phone location, otherwise your named saved reference place. Set or confirm your town/district under Rain & safety; saved coordinates are not moved. These are dated reports, not live local danger warnings.",
                    "புதிய சரிபார்த்த புயல் அறிக்கை அடுத்த முழுமையான செய்தி வாசிப்பில் ஒருமுறை வரும். அதே அல்லது பழைய அறிக்கை மீண்டும் வராது; புதிய அறிக்கை பழைய காத்திருக்கும் விவரங்களை மாற்றும். வெள்ளம், நிலநடுக்கம் மூன்று முழுமையான வாசிப்புகளில் வரும். அனுமதித்த புதிய பெயரிட்ட கைப்பேசி இருப்பிடம் இல்லையெனில் சேமித்த குறிப்பு இடத்தின் பெயர் கூறப்படும். மழை மற்றும் பாதுகாப்பு பகுதியில் ஊர்/மாவட்டத்தை அமைக்கவும் அல்லது உறுதிசெய்யவும்; சேமித்த அட்ச/தீர்க்கரேகைகள் மாற்றப்படாது. இவை நேரமிட்ட அறிக்கைகள்; நேரலை உள்ளூர் அபாய எச்சரிக்கைகள் அல்ல."))
                Text(if (enabled) watchWords("Currently on. You can turn it off here or under Voice → News.", "இப்போது இயக்கத்தில் உள்ளது. இங்கேயோ குரல் → செய்திகள் பகுதியிலோ அணைக்கலாம்.")
                    else watchWords("Your previous OFF choice has been kept. You can enable it here or later under Voice → News.", "நீங்கள் முன்பு அணைத்த தேர்வு பாதுகாக்கப்பட்டுள்ளது. இங்கேயோ பின்னர் குரல் → செய்திகள் பகுதியிலோ இயக்கலாம்."))
                Text(watchWords("All 13 news topics are now selected by default. Your customised topics, language and voice safety settings are kept. At most eight eligible fresh stories per bulletin; not every topic is guaranteed in every reading.",
                    "இயல்பில் 13 செய்தித் தலைப்புகளும் தேர்வு செய்யப்படும். நீங்கள் மாற்றிய தலைப்புகள், மொழி, குரல் பாதுகாப்பு அமைப்புகள் மாறாது. ஒரு வாசிப்பில் அதிகபட்சம் எட்டு தகுதியான புதிய செய்திகள்; ஒவ்வொரு தலைப்பும் ஒவ்வொரு முறையும் கிடைக்கும் என உறுதி இல்லை."))
                if (error) Text(watchWords("Could not save. Please try again.", "சேமிக்க முடியவில்லை. மீண்டும் முயலவும்."))
            }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = { choose(if (ask) FeatureDiscovery.Choice.ENABLE else FeatureDiscovery.Choice.ACKNOWLEDGE) }) {
            Text(if (ask) watchWords("Enable", "இயக்கு") else watchWords("Got it", "புரிந்தது"))
        } },
        dismissButton = { TextButton(enabled = !busy, onClick = { choose(if (ask) FeatureDiscovery.Choice.NOT_NOW else if (enabled) FeatureDiscovery.Choice.DISABLE else FeatureDiscovery.Choice.ENABLE) }) {
            Text(if (ask) watchWords("Not now", "இப்போது வேண்டாம்") else if (enabled) watchWords("Turn off", "அணை") else watchWords("Enable", "இயக்கு"))
        } },
    )
}
