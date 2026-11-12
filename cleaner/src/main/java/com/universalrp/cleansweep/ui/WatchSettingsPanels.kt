package com.universalrp.cleansweep.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.data.*
import com.universalrp.cleansweep.voice.NewsBulletinEditor

internal fun watchWords(en: String, ta: String) = if (Lang.isTamil) ta else en

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsTopicSettings(onChange: () -> Unit) {
    val context=LocalContext.current
    var topics by remember { mutableStateOf(NewsPreferences.selected(context)) }
    var language by remember { mutableStateOf(NewsPreferences.language(context)) }
    val editing by NewsBulletinEditor.status.collectAsState()
    Text(watchWords("News topics", "செய்தித் தலைப்புகள்"),style=MaterialTheme.typography.titleMedium)
    Text(watchWords("All topics are selected by default. Customise your topics below; up to eight fresh stories per bulletin, never filler. Unavailable categories may be empty.",
        "இயல்பில் எல்லாத் தலைப்புகளும் தேர்வு செய்யப்படும். கீழே விருப்பத் தலைப்புகளை மாற்றலாம்; ஒரு வாசிப்பில் எட்டு புதிய செய்திகள் வரை; நிரப்புச் செய்தி இல்லை. கிடைக்காத தலைப்பு காலியாக இருக்கலாம்."))
    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        NewsPreferences.allCategories().forEach { category ->
            FilterChip(selected=category in topics,onClick={
                onChange(); topics=if(category in topics && topics.size > 1) topics-category else topics+category
                NewsPreferences.save(context,topics,language)
            }, label={Text(NewsPreferences.label(category,Lang.isTamil))})
        }
    }
    TextButton(onClick={onChange();topics=NewsPreferences.allCategories();NewsPreferences.save(context,topics,language)}) {
        Text(watchWords("Restore default topics", "தொடக்கத் தலைப்புகளை மீட்டமை"))
    }
    Text(watchWords("News language", "செய்தி மொழி"))
    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        NewsPreferences.Language.values().forEach { value ->
            val label=when(value) {
                NewsPreferences.Language.APP -> watchWords("App language", "செயலியின் மொழி")
                NewsPreferences.Language.TAMIL_ONLY -> "தமிழ் ஆதாரங்கள் மட்டும்"
                NewsPreferences.Language.TAMIL_TRANSLATED -> "தமிழ் + ஆங்கிலத்திலிருந்து தமிழாக்கம்"
                NewsPreferences.Language.ENGLISH -> "English"
            }
            FilterChip(selected=language==value,onClick={onChange();language=value;NewsPreferences.save(context,topics,language)},label={Text(label)})
        }
    }
    Text(watchWords("Translation needs your configured AI provider. Failed or unverifiable translations are skipped. Job stories are reports, not verified vacancies—check the official notice and never pay to apply through a news link.",
        "மொழிபெயர்ப்பிற்கு நீங்கள் அமைத்த AI சேவை தேவை. ஏற்க முடியாத மொழிபெயர்ப்புகள் தவிர்க்கப்படும். வேலைச் செய்திகள் உறுதியான காலிப்பணியிடச் சான்றல்ல; அதிகாரப்பூர்வ அறிவிப்பைச் சரிபார்க்கவும்; செய்தி இணைப்பில் பணம் செலுத்த வேண்டாம்."))
    Text(editing,style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun WatchSwitch(en:String,ta:String,on:Boolean,change:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value=on, role=Role.Switch, onValueChange=change),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(watchWords(en,ta),Modifier.weight(1f).padding(end=8.dp))
        Switch(checked=on,onCheckedChange=null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatteryIntakeSettings() {
    val context=LocalContext.current
    var advanced by remember { mutableStateOf(ChargeAnnouncementSettings.advanced(context)) }
    var settings by remember { mutableStateOf(ChargeAnnouncementSettings.thresholds(context)) }
    var quiet by remember { mutableStateOf(ChargeAnnouncementSettings.allowQuiet(context)) }
    var percent by remember { mutableStateOf(ChargeAnnouncementSettings.percentNotice(context)) }
    var early by remember { mutableStateOf(ChargeAnnouncementSettings.earlyReminder(context)) }
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(watchWords("Battery announcements", "பேட்டரி அறிவிப்புகள்"),style=MaterialTheme.typography.titleMedium)
            WatchSwitch("Announce reaching 100%", "100% எட்டியதை அறிவி",percent) {
                percent=it;ChargeAnnouncementSettings.setPercentNotice(context,it)
            }
            WatchSwitch("Advanced battery checks", "மேம்பட்ட பேட்டரி சோதனைகள்",advanced) {
                advanced=it;ChargeAnnouncementSettings.setAdvanced(context,it)
            }
            if (advanced) {
            Text(watchWords("100% + Android Full + a proven current sensor + sustained low intake. Thresholds estimate completion; they do not measure the charger or certify termination. Missing readings never count as zero.",
                "100% + ஆண்ட்ராய்டு முழுநிலை + செயல்படுவது அறியப்பட்ட மின்னோட்ட சென்சார் + நீடித்த குறைந்த மின்னோட்டம். இவை மதிப்பீடு மட்டுமே; சார்ஜரின் வெளியீட்டை அளவிடவோ நிறைவை உறுதிசெய்யவோ முடியாது. கிடைக்காத அளவை பூஜ்ஜியமாகக் கருதாது."))
            Text(watchWords("Battery-side power below", "பேட்டரி சக்தி இதற்குக் கீழ்"))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(0.5f,1f,2f).forEach { value ->
                FilterChip(settings.watts==value,{settings=settings.copy(watts=value);ChargeAnnouncementSettings.save(context,settings)},label={Text("$value W")})
            } }
            Text(watchWords("Battery current at or below", "மின்னோட்டம் இதற்குச் சமம் அல்லது கீழ்"))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(0.05f,0.1f,0.2f).forEach { value ->
                FilterChip(settings.amps==value,{settings=settings.copy(amps=value);ChargeAnnouncementSettings.save(context,settings)},label={Text("${(value*1000).toInt()} mA")})
            } }
            Text(watchWords("Stable duration", "நீடிக்கும் நேரம்"))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(60_000L,120_000L,180_000L).forEach { value ->
                FilterChip(settings.stableMs==value,{settings=settings.copy(stableMs=value);ChargeAnnouncementSettings.save(context,settings)},label={Text("${value/60_000} min / நிமி")})
            } }
            }
            WatchSwitch("Allow charger announcements during quiet hours", "அமைதி நேரத்திலும் சார்ஜ் அறிவிப்புகளை அனுமதி",quiet) {
                quiet=it;ChargeAnnouncementSettings.setAllowQuiet(context,it)
            }
            Text(watchWords("Calls, silent places and the master voice switch still win. Charger removal announces the current level at any percentage when enabled.",
                "அழைப்புகள், அமைதி இடங்கள் மற்றும் முதன்மைக் குரல் முடக்கம் தொடர்ந்து முன்னுரிமை பெறும். இயக்கியிருந்தால் சார்ஜரை அகற்றும்போது எந்த சதவீதத்திலும் தற்போதைய அளவை அறிவிக்கும்."))
            WatchSwitch("Also add the early-removal reminder below 90%", "90%-க்கு முன் அகற்றிய நினைவூட்டலையும் சேர்",early) {
                early=it;ChargeAnnouncementSettings.setEarlyReminder(context,it)
            }
        }
    }
}

@Composable
fun BatteryCompletionReason(battery: BatteryReading) {
    val context = LocalContext.current
    if(!ChargeAnnouncementSettings.advanced(context) || !battery.powerConnected || battery.percent<100) return
    val en=battery.chargeEstimate.reason
    val ta=when(en) {
        "Waiting for Android Full status" -> "ஆண்ட்ராய்டு முழு சார்ஜ் நிலைக்காகக் காத்திருக்கிறது"
        "Current sensor unavailable; zero is not assumed" -> "மின்னோட்ட சென்சார் கிடைக்கவில்லை; பூஜ்ஜியமாகக் கருதவில்லை"
        "Waiting for evidence of a working current sensor in this connection" -> "இந்த இணைப்பில் சென்சார் செயல்படுவதற்கான ஆதாரத்திற்காகக் காத்திருக்கிறது"
        "Battery voltage unavailable" -> "பேட்டரி மின்னழுத்தம் கிடைக்கவில்லை"
        "Battery intake is above your thresholds" -> "பேட்டரியின் மின்னோட்டம் தேர்ந்தெடுத்த வரம்பிற்கு மேல் உள்ளது"
        "Low battery intake sustained; completion is estimated, not verified" -> "குறைந்த மின்னோட்டம் நீடிக்கிறது; நிறைவு மதிப்பீடு மட்டுமே, உறுதிப்படுத்தப்படவில்லை"
        "Waiting for sustained low battery intake" -> "குறைந்த மின்னோட்டம் நீடிக்கக் காத்திருக்கிறது"
        else -> "பேட்டரி அளவீடுகளுக்காகக் காத்திருக்கிறது"
    }
    Text(watchWords(en,ta)+" · ${battery.chargeEstimate.elapsedSeconds}s",style=MaterialTheme.typography.bodySmall)
}

/** Separate opt-in; changing it withdraws any already-prepared bulletin immediately. */
@Composable
fun NewsDisasterSettings() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(NewsDisasterUpdates.enabled(context)) }
    val preferences = remember(context) { context.getSharedPreferences(NewsDisasterUpdates.PREFS, android.content.Context.MODE_PRIVATE) }
    DisposableEffect(preferences) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "enabled") enabled = NewsDisasterUpdates.enabled(context)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(watchWords("Disaster updates with news", "செய்தியுடன் பேரிடர் அறிக்கைகள்"))
            Text(watchWords("Cyclone: latest verified new advisory once in the next completed news read; no unchanged/older repeats. Newer details replace older pending reports. Flood/earthquake: next 3 completed news reads. Fresh phone location if enabled, otherwise explicitly saved home. Dated report values, not local danger predictions.",
                "புயல்: புதிய சரிபார்த்த அறிக்கை அடுத்த முழுமையான வாசிப்பில் ஒருமுறை; அதே/பழைய அறிக்கை மீண்டும் வராது. புதிய விவரங்கள் பழைய காத்திருக்கும் அறிக்கையை மாற்றும். வெள்ளம்/நிலநடுக்கம்: அடுத்த 3 முழுமையான வாசிப்புகள். அனுமதித்த புதிய கைப்பேசி இருப்பிடம்; இல்லையெனில் சேமித்த வீடு எனத் தெரிவிக்கப்படும். நேரமிட்ட அறிக்கையே; உள்ளூர் அபாயக் கணிப்பு அல்ல."),
                style = MaterialTheme.typography.labelSmall)
        }
        Switch(enabled, { enabled = it; NewsDisasterUpdates.setEnabled(context, it) })
    }
}
