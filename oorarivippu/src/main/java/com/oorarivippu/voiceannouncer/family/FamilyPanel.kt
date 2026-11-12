package com.oorarivippu.voiceannouncer.family

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.oorarivippu.voiceannouncer.AppPreferences
import java.text.DateFormat
import java.util.Date

/** Explicit, private family connections. Opening this panel never enables any sharing. */
@Composable fun FamilyPanel(tamil: Boolean, onChanged: () -> Unit = {}, onFamilyUpdates: () -> Unit = {}) {
    val c=LocalContext.current
    fun words(en: String,ta: String)=if(tamil) ta else en
    var open by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    var outgoing by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val rows by FamilyRepository.state.collectAsState()
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val scope=rememberCoroutineScope()
    fun run(action: suspend ()->Unit) {
        if(busy) return
        busy=true;error=false
        scope.launch {
            try { action();val latest=FamilyRepository.views(c);if(latest.any{it.confirmed && it.town!=null})onFamilyUpdates() }
            catch(e:CancellationException){throw e}
            catch(_:Exception){ error=true }
            finally {busy=false;tick=System.currentTimeMillis();onChanged()}
        }
    }
    LaunchedEffect(Unit) { runCatching {FamilyRepository.cached(c)} }
    LaunchedEffect(lifecycle) {
        while(lifecycle.isAtLeast(Lifecycle.State.RESUMED)) {
            if(!busy && FamilyRepository.configured) run {}
            tick=System.currentTimeMillis();delay(30_000)
        }
    }
    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) {
        Text(words("Family · optional town sharing", "குடும்பம் · விருப்பமான ஊர் பகிர்வு"))
    }
    // This panel is placed directly above the trip-status card on the main screen.
    rows.filter{it.confirmed}.forEach { row ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal=10.dp,vertical=6.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(row.name,style=MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { FamilyLastReport(row,tick,tamil) }
                    Switch(checked=row.sharing,enabled=row.sharing || (!busy && !row.pending && !row.unavailable),
                        onCheckedChange={if(it)run{FamilyRepository.setSharing(c,row.id,true)} else try{FamilyRepository.stop(c,row.id);onChanged();if(!busy)run{}}catch(_:Exception){error=true}})
                }
                Text(if(row.pending) words("Stopped locally · online confirmation pending; old report expires within 15 minutes", "இந்தக் கைப்பேசியில் நிறுத்தம் · இணைய உறுதி காத்திருக்கிறது; பழைய அறிக்கை 15 நிமிடத்தில் காலாவதியாகும்") else if(!row.sharing) words("Sharing OFF", "பகிர்வு அணைப்பு") else if(row.uploading)
                    words("Your outgoing sharing ON · trip uploads enabled", "உங்கள் பகிர்வு ஆன் · பயணப் பதிவேற்றம் இயக்கம்") else
                    words("Sharing ON · waiting for trip / connection", "பகிர்வு ஆன் · பயணம் / இணைப்புக்குக் காத்திருக்கிறது"),style=MaterialTheme.typography.bodySmall)


            }
        }
    }
    if(!open) return
    AlertDialog(onDismissRequest={open=false;invite="";outgoing=""},
        title={Text(words("Family town sharing", "குடும்ப ஊர் பகிர்வு"))},
        confirmButton={TextButton(onClick={open=false;invite="";outgoing=""}){Text(words("Close","மூடு"))}},
        text={Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(words("Only approved people. Both confirm the connection; each separately chooses whether to share. No map, exact coordinates, address or travel history. Town names can be approximate and do not confirm safety. Not an emergency service.",
                "அனுமதித்த நபர்கள் மட்டும். இணைப்பை இருவரும் உறுதிப்படுத்த வேண்டும்; ஊரைப் பகிர்வதை ஒவ்வொருவரும் தனியாகத் தேர்ந்தெடுக்க வேண்டும். வரைபடம், துல்லிய இடம், முகவரி அல்லது பயண வரலாறு இல்லை. ஊர்ப் பெயர் தோராயமானது; பாதுகாப்புச் சான்று அல்ல. அவசரச் சேவை அல்ல."))
            Text(words("Updates normally every 5 minutes while a user-started trip is tracking. Latest encrypted update expires within 15 minutes. Network, GPS and Android restrictions can delay it. Viewing does not require starting a trip.",
                "நீங்கள் தொடங்கிய பயணக் கண்காணிப்பின்போது பொதுவாக ஐந்து நிமிடத்திற்கு ஒருமுறை புதுப்பிப்பு. கடைசி மறையாக்கிய அறிக்கை பதினைந்து நிமிடத்திற்குள் காலாவதியாகும். இணையம், இடச் சேவை மற்றும் கைப்பேசிக் கட்டுப்பாடுகளால் தாமதமாகலாம். மற்றவர் ஊரைப் பார்க்கப் பயணத்தைத் தொடங்க வேண்டியதில்லை."))
            Text(words("Your sharing switch stays ON until you turn it OFF. Uploads happen only during Oor trips; stopping a trip pauses uploads, not your saved switch. New family reports are announced once using an on-device voice, not online HD. Silent mode, DND, calls or a missing device voice can delay or prevent speech.",
                "நீங்கள் அணைக்கும் வரை பகிர்வு இயக்கம் நினைவில் இருக்கும். ஊர் பயணக் கண்காணிப்பின்போது மட்டும் அனுப்பப்படும்; பயணத்தை நிறுத்தினால் பதிவேற்றம் இடைநிறுத்தப்படும். புதிய குடும்ப அறிக்கை ஒருமுறை கைப்பேசிக் குரலில் அறிவிக்கப்படும்; இணைய HD குரலுக்கு அனுப்பப்படாது. அமைதி முறை, தொந்தரவு செய்யாதே முறை, அழைப்பு அல்லது குரல் இல்லாமையால் அறிவிப்பு தாமதமாகலாம் அல்லது வராமல் போகலாம்."))
            if(!FamilyRepository.configured) {
                Text(words("Service setup/testing is not complete. Real family sharing is unavailable; nothing is being uploaded.","சேவை அமைப்பும் சோதனையும் இன்னும் நிறைவடையவில்லை. குடும்பப் பகிர்வு கிடைக்கவில்லை; எந்த இடமும் பதிவேற்றப்படவில்லை."))
            } else {
                TextButton(onClick={
                    try { FamilyRepository.stopAll(c);onChanged();if(!busy)run{} }
                    catch(_:Exception){error=true}
                }) {Text(words("Stop all my family sharing", "எனது குடும்பப் பகிர்வு அனைத்தையும் நிறுத்து"))}
                OutlinedTextField(name,{name=it.take(80)},label={Text(words("Family member's name (on this phone only)","குடும்ப நபரின் பெயர் (இந்தக் கைப்பேசியில் மட்டும்)"))},singleLine=true)
                Button(enabled=!busy && name.isNotBlank(),onClick={run { outgoing=FamilyRepository.create(c,name);name="" }}) {Text(words("Create private invitation","தனிப்பட்ட அழைப்பை உருவாக்கு"))}
                if(outgoing.isNotEmpty()) {
                    Text(words("Send only to the intended person. Invitation expires in 10 minutes and can connect one phone. Compare the confirmation code in person or on a trusted call.",
                        "குறிப்பிட்ட நபருக்கு மட்டும் அனுப்பவும். அழைப்பு பத்து நிமிடத்தில் காலாவதியாகும்; ஒரு கைப்பேசி மட்டும் இணையலாம். நேரில் அல்லது நம்பகமான அழைப்பில் உறுதிக் குறியீட்டை ஒப்பிடவும்."))
                    TextButton(onClick={c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain";putExtra(Intent.EXTRA_TEXT,outgoing) },words("Private invitation","தனிப்பட்ட அழைப்பு")))}) {Text(words("Share invitation","அழைப்பைப் பகிர்"))}
                }
                OutlinedTextField(invite,{invite=it.take(201)},label={Text(words("Paste private invitation","தனிப்பட்ட அழைப்பை ஒட்டு"))},visualTransformation=PasswordVisualTransformation(),singleLine=true)
                Button(enabled=!busy && name.isNotBlank() && invite.isNotBlank(),onClick={run {FamilyRepository.join(c,name,invite);invite="";name=""}}) {Text(words("Accept connection only (sharing stays OFF)","இணைப்பை மட்டும் ஏற்கவும் (பகிர்வு அணைந்திருக்கும்)"))}
                if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(error) Text(words("Could not complete securely. Check connection, invitation expiry and automatic date/time; retry. No sharing was enabled by this error.","பாதுகாப்பாக முடிக்க இயலவில்லை. இணையம், அழைப்புக் காலம் மற்றும் தானியங்கி தேதி/நேரத்தைச் சரிபார்த்து மீண்டும் முயலவும். இந்தப் பிழையால் பகிர்வு இயக்கப்படவில்லை."))
                rows.forEach { row ->
                    HorizontalDivider()
                    Text(row.name,style=MaterialTheme.typography.titleSmall)
                    if(row.setupPending) Text(words("Connection setup pending · sharing OFF. Refresh to retry, or remove and create a new invite if expired.","இணைப்பு அமைப்பு காத்திருக்கிறது · பகிர்வு அணைப்பு. புதுப்பித்து முயலவும்; காலாவதியானால் நீக்கிப் புதிய அழைப்பை உருவாக்கவும்."))
                    else if(row.pending) Text(words("Uploads paused locally. Stop/remove pending online confirmation. An earlier update may remain visible until its 15-minute expiry.","இந்தக் கைப்பேசியில் பகிர்வு அணைந்தது. இணையத்தில் நிறுத்தம் அல்லது நீக்கம் உறுதியாகக் காத்திருக்கிறது. முந்தைய அறிக்கை பதினைந்து நிமிடக் காலம் முடியும் வரை தெரியலாம்."))
                    else if(row.unavailable) Text(words("Unavailable · not a live location","கிடைக்கவில்லை · நேரலை இடம் அல்ல"))
                    else if(!row.paired) Text(words("Waiting for invitation acceptance","அழைப்பை ஏற்பதற்குக் காத்திருக்கிறது"))
                    if(row.invitation.isNotEmpty()) TextButton(enabled=!busy,onClick={c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="text/plain";putExtra(Intent.EXTRA_TEXT,row.invitation)},words("Private invitation","தனிப்பட்ட அழைப்பு")))}) {Text(words("Share this private invitation (10-minute limit)","இந்தத் தனிப்பட்ட அழைப்பைப் பகிர் (பத்து நிமிடக் காலம்)"))}
                    if(row.code.isNotEmpty() && !row.confirmed) {
                        Text(words("Compare on both phones: ","இரு கைப்பேசிகளிலும் ஒப்பிடவும்: ")+row.code)
                        if(!row.myConfirmed) TextButton(enabled=!busy,onClick={run {FamilyRepository.confirm(c,row.id)}}){Text(words("I checked the code with this person","இந்த நபருடன் குறியீட்டை ஒப்பிட்டேன்"))}
                        else Text(words("Waiting for the other person's confirmation","மற்றவரின் உறுதிப்படுத்தலுக்குக் காத்திருக்கிறது"))
                    }
                    FamilyLastReport(row,tick,tamil)
                    if(row.confirmed) FamilySharingControl(row,tamil,busy,
                        onEnable={run{FamilyRepository.setSharing(c,row.id,true)}},
                        onDisable={try{FamilyRepository.stop(c,row.id);onChanged();if(!busy)run{}}catch(_:Exception){error=true}})
                    TextButton(enabled=!busy,onClick={run {FamilyRepository.stop(c,row.id,remove=true)}}){Text(words("Remove connection / cancel invite","இணைப்பை நீக்கு / அழைப்பை ரத்து செய்"))}
                }
                TextButton(enabled=!busy,onClick={run {}}){Text(words("Refresh / retry pending stops","புதுப்பி / காத்திருக்கும் நிறுத்தத்தை மீண்டும் முயல்"))}
            }
        }})
}

@Composable private fun FamilyLastReport(row: FamilyRepository.View,now: Long,tamil: Boolean) {
    fun words(en:String,ta:String)=if(tamil)ta else en
    val town=row.town?.takeIf{FamilySharingPolicy.fresh(it.at,now) && now<row.reportExpiresAt}
    if(town!=null) {
        Text(listOf(town.town,town.district).filter{it.isNotBlank()}.distinct().joinToString(" · "))
        Text(words("Last reported (not live): ","கடைசியாகத் தெரிவித்த ஊர் (நேரலை அல்ல): ")+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(town.at)),style=MaterialTheme.typography.bodySmall)
        if(row.unavailable) Text(words("Offline / unavailable · showing the last unexpired report","இணையம் இல்லை / கிடைக்கவில்லை · காலாவதியாகாத கடைசி அறிக்கை"),style=MaterialTheme.typography.bodySmall)
    } else if(row.confirmed) Text(words("No recent incoming town update","புதிய பெறப்பட்ட ஊர் தகவல் இல்லை"), fontSize=13.sp)
}

@Composable private fun FamilySharingControl(row: FamilyRepository.View,tamil: Boolean,busy: Boolean,onEnable: ()->Unit,onDisable: ()->Unit) {
    fun words(en:String,ta:String)=if(tamil)ta else en
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
        Text(if(row.sharing) words("My sharing: ON","எனது பகிர்வு: ஆன்") else words("My sharing: OFF","எனது பகிர்வு: ஆஃப்"),modifier=Modifier.weight(1f),fontSize=13.sp)
        Switch(checked=row.sharing,enabled=row.sharing || (!busy && !row.pending && !row.unavailable),onCheckedChange={if(it)onEnable()else onDisable()})
    }
    if(row.sharing) Text(if(row.uploading) words("Sharing your town with this member during this trip","இந்தப் பயணத்தில் இந்த நபருக்கு உங்கள் ஊர் பகிரப்படுகிறது") else words("Sharing ON · waiting for trip / connection","பகிர்வு ஆன் · பயணம் / இணைப்புக்குக் காத்திருக்கிறது"),style=MaterialTheme.typography.bodySmall)
    if(row.pending && !row.setupPending) Text(words("Stop/remove pending online confirmation; the old report may remain until its 15-minute expiry.","இணையத்தில் நிறுத்தம்/நீக்கம் உறுதியாகக் காத்திருக்கிறது; பழைய அறிக்கை பதினைந்து நிமிடக் காலம் வரை தெரியலாம்."),style=MaterialTheme.typography.bodySmall)
}
