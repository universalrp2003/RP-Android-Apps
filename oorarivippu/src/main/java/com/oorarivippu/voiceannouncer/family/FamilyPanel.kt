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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.oorarivippu.voiceannouncer.AppPreferences
import java.text.DateFormat
import java.util.Date

/** Explicit, private family connections. Opening this panel never enables any sharing. */
@Composable fun FamilyPanel(tamil: Boolean) {
    val c=LocalContext.current
    fun words(en: String,ta: String)=if(tamil) ta else en
    var open by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    var outgoing by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<FamilyRepository.View>>(emptyList()) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val scope=rememberCoroutineScope()
    fun run(action: suspend ()->Unit) {
        if(busy) return
        busy=true;error=false
        scope.launch {
            try { action();rows=FamilyRepository.views(c) }
            catch(e:CancellationException){throw e}
            catch(_:Exception){ error=true;rows=emptyList() }
            finally {busy=false;tick=System.currentTimeMillis()}
        }
    }
    LaunchedEffect(open,lifecycle) {
        while(open && lifecycle.isAtLeast(Lifecycle.State.RESUMED)) {
            if(!busy && FamilyRepository.configured) run {}
            tick=System.currentTimeMillis();delay(30_000)
        }
    }
    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) {
        Text(words("Family · optional town sharing", "குடும்பம் · விருப்பமான ஊர் பகிர்வு"))
    }
    if(!open) return
    AlertDialog(onDismissRequest={open=false;invite="";outgoing=""},
        title={Text(words("Family town sharing", "குடும்ப ஊர் பகிர்வு"))},
        confirmButton={TextButton(onClick={open=false;invite="";outgoing=""}){Text(words("Close","மூடு"))}},
        text={Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(words("Only approved people. Both confirm the connection; each separately chooses whether to share. No map, exact coordinates, address or travel history. Town names can be approximate. Not an emergency service.",
                "அனுமதித்த நபர்கள் மட்டும். இணைப்பை இருவரும் உறுதிப்படுத்த வேண்டும்; ஊரைப் பகிர்வதை ஒவ்வொருவரும் தனியாகத் தேர்ந்தெடுக்க வேண்டும். வரைபடம், துல்லிய இடம், முகவரி அல்லது பயண வரலாறு இல்லை. ஊர்ப் பெயர் தோராயமாக இருக்கலாம். அவசரச் சேவை அல்ல."))
            Text(words("Updates normally every 5 minutes while a user-started trip is tracking. Latest encrypted update expires within 15 minutes. Network, GPS and Android restrictions can delay it. Viewing does not require starting a trip.",
                "நீங்கள் தொடங்கிய பயணக் கண்காணிப்பின்போது பொதுவாக ஐந்து நிமிடத்திற்கு ஒருமுறை புதுப்பிப்பு. கடைசி மறையாக்கிய அறிக்கை பதினைந்து நிமிடத்திற்குள் காலாவதியாகும். இணையம், இடச் சேவை மற்றும் கைப்பேசிக் கட்டுப்பாடுகளால் தாமதமாகலாம். மற்றவர் ஊரைப் பார்க்கப் பயணத்தைத் தொடங்க வேண்டியதில்லை."))
            if(!FamilyRepository.configured) {
                Text(words("Service setup/testing is not complete. Real family sharing is unavailable; nothing is being uploaded.","சேவை அமைப்பும் சோதனையும் இன்னும் நிறைவடையவில்லை. குடும்பப் பகிர்வு கிடைக்கவில்லை; எந்த இடமும் பதிவேற்றப்படவில்லை."))
            } else {
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
                    if(row.pending) Text(words("Sharing OFF locally. Stop/remove pending online confirmation. An earlier update may remain visible until its 15-minute expiry.","இந்தக் கைப்பேசியில் பகிர்வு அணைந்தது. இணையத்தில் நிறுத்தம் அல்லது நீக்கம் உறுதியாகக் காத்திருக்கிறது. முந்தைய அறிக்கை பதினைந்து நிமிடக் காலம் முடியும் வரை தெரியலாம்."))
                    else if(row.unavailable) Text(words("Unavailable · not a live location","கிடைக்கவில்லை · நேரலை இடம் அல்ல"))
                    else if(!row.paired) Text(words("Waiting for invitation acceptance","அழைப்பை ஏற்பதற்குக் காத்திருக்கிறது"))
                    if(row.code.isNotEmpty() && !row.confirmed) {
                        Text(words("Compare on both phones: ","இரு கைப்பேசிகளிலும் ஒப்பிடவும்: ")+row.code)
                        if(!row.myConfirmed) TextButton(enabled=!busy,onClick={run {FamilyRepository.confirm(c,row.id)}}){Text(words("I checked the code with this person","இந்த நபருடன் குறியீட்டை ஒப்பிட்டேன்"))}
                        else Text(words("Waiting for the other person's confirmation","மற்றவரின் உறுதிப்படுத்தலுக்குக் காத்திருக்கிறது"))
                    }
                    val town=row.town?.takeIf { tick-it.at in 0..FamilyProtocol.TTL }
                    if(town!=null) {
                        Text(listOf(town.town,town.district).filter { it.isNotBlank() }.joinToString(" · "))
                        Text(words("Last reported (not live): ","கடைசியாகத் தெரிவித்த ஊர் (நேரலை அல்ல): ")+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(town.at)))
                    } else if(row.confirmed) Text(words("No fresh town shared · not an all-clear","புதிய ஊர் பகிரப்படவில்லை · பாதுகாப்பாக உள்ளார் என்பதற்கான உறுதி அல்ல"))
                    Text(if(row.sharing) words("You are sharing during this trip","இந்தப் பயணத்தில் உங்கள் ஊரைப் பகிர்கிறீர்கள்") else words("Your sharing: OFF","உங்கள் பகிர்வு: அணைப்பு"))
                    if(row.confirmed && !row.sharing && !row.pending && !row.unavailable) {
                        Text(words("Start trip tracking first, then choose a sharing duration. This does not enable the other person's sharing.","முதலில் பயணக் கண்காணிப்பைத் தொடங்கி, பகிரும் காலத்தைத் தேர்ந்தெடுக்கவும். இது மற்றவரின் பகிர்வை இயக்காது."))
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            listOf(1,4,8).forEach { hours ->
                                TextButton(enabled=!busy && AppPreferences.state.value.isTrackingEnabled,onClick={run {FamilyRepository.start(c,row.id,hours)}}){Text(if(tamil) "$hours மணி" else "$hours h")}
                            }
                        }
                    }
                    if(row.sharing) TextButton(onClick={FamilyRepository.stop(c,row.id);run {}}){Text(words("Stop sharing now","பகிர்வை இப்போது நிறுத்து"))}
                    TextButton(enabled=!busy,onClick={FamilyRepository.stop(c,row.id,remove=true);run {}}){Text(words("Remove connection / cancel invite","இணைப்பை நீக்கு / அழைப்பை ரத்து செய்"))}
                }
                TextButton(enabled=!busy,onClick={run {}}){Text(words("Refresh / retry pending stops","புதுப்பி / காத்திருக்கும் நிறுத்தத்தை மீண்டும் முயல்"))}
            }
        }})
}
