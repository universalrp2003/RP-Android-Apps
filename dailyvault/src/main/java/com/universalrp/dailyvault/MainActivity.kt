package com.universalrp.dailyvault

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal
import java.time.*
import java.time.format.DateTimeFormatter

class MainActivity: FragmentActivity() {
    private val model: VaultModel by viewModels()
    private var restoreUri by mutableStateOf<Uri?>(null)
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { it?.let(model::export) }
    private val restore=registerForActivityResult(ActivityResultContracts.OpenDocument()) { restoreUri=it }
    private var clearSensitive: () -> Unit = {}
    override fun onStart() { super.onStart();if(model.lockMode=="none") model.localUnlock() }
    private fun deviceUnlock(onSuccess: () -> Unit) {
        val allowed=BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if(BiometricManager.from(this).canAuthenticate(allowed)!=BiometricManager.BIOMETRIC_SUCCESS) return
        val prompt=BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("தினக்கணக்கு / Daily Vault")
            .setSubtitle("சாதனப் பூட்டு / Device authentication").setAllowedAuthenticators(allowed).build())
    }
    override fun onStop() { clearSensitive();model.lock();super.onStop() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            var tamil by rememberSaveable { mutableStateOf(getPreferences(0).getBoolean("tamil",true)) }
            fun bi(ta: String,en: String)=if(tamil)ta else en
            fun money(p: Long)="₹"+BigDecimal.valueOf(p,2).toPlainString()
            var password by remember { mutableStateOf("") }
            var confirm by remember { mutableStateOf("") }
            var search by remember { mutableStateOf("") }
            var tab by rememberSaveable { mutableIntStateOf(0) }
            var draft by remember { mutableStateOf<Entry?>(null) }
            var deleting by remember { mutableStateOf<Entry?>(null) }
            var backupPassword by remember { mutableStateOf("") }
            var uiError by remember { mutableStateOf("") }
            var showHelp by rememberSaveable { mutableStateOf(false) }
            var showLockOptions by remember { mutableStateOf(false) }
            var confirmNoLock by remember { mutableStateOf(false) }
            DisposableEffect(Unit) {
                clearSensitive = { password="";confirm="";backupPassword="";draft=null;deleting=null;search="";showLockOptions=false;confirmNoLock=false }
                onDispose { clearSensitive = {} }
            }
            LaunchedEffect(state.unlocked) { if(!state.unlocked) { draft=null;deleting=null;search="";password="";confirm="";uiError="" } }
            MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF5CE0BE),background=Color(0xFF0B1725),surface=Color(0xFF152C3D))) {
                Surface(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.safeDrawingPadding().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f).padding(top=12.dp)) {
                                Text(bi("தினக்கணக்கு","Daily Vault"),style=MaterialTheme.typography.headlineSmall)
                                Text(bi("செலவு · வரலாறு · தனிக் குறிப்புகள்","Spending · history · private notes"),style=MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick={tamil=!tamil;getPreferences(0).edit().putBoolean("tamil",tamil).apply()}) { Text(if(tamil)"English" else "தமிழ்") }
                        } }
                        if(state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth());Text(bi("செயலாக்கம் நடைபெறுகிறது…","Working…")) }
                        if(state.status.isNotEmpty()) item { Text(when(state.status) {
                            "failed"->bi("முடிக்க முடியவில்லை. கடவுச்சொல், கோப்பு அல்லது சேமிப்பிடத்தைச் சரிபார்க்கவும். பழைய விவரங்கள் மாற்றப்படவில்லை எனக் கருதாமல் மீண்டும் திறந்து சரிபார்க்கவும்.","Could not complete. Check password, file or storage. Reopen to verify saved state.")
                            "password"->bi("குறைந்தது 10 எழுத்துகள் தேவை; உறுதிப்படுத்தல் பொருந்த வேண்டும்.","Use 10–256 characters; confirmation must match.")
                            "exported"->bi("மறைகுறியாக்கப்பட்ட காப்பு எழுதப்பட்டது. வேறு போனில் மீட்டுச் சோதிக்கவும்.","Encrypted backup written. Test restore on another device.")
                            "restored"->bi("காப்பு மீட்கப்பட்டது. இனி காப்பின் கடவுச்சொல்லைப் பயன்படுத்தவும்.","Backup restored. Use the backup password from now on.")
                            "saved"->bi("சேமிக்கப்பட்டது","Saved")
                            else->bi("திறக்கப்பட்டது","Unlocked")
                        }) }
                        if(!state.unlocked) {
                            item { Text(bi("காப்புக்கான கடவுச்சொல் தேவை. மறந்தால் மீட்பு இல்லை.","Backup password required. No password recovery."),style=MaterialTheme.typography.bodySmall) }
                            if(model.lockMode!="password") item { Button(onClick={if(model.lockMode=="device") deviceUnlock { model.localUnlock() } else model.localUnlock()},enabled=!state.busy) { Text(bi("சாதனத்தில் திற","Unlock locally")) } }
                            item { OutlinedTextField(password,{password=it.take(256)},label={Text(bi("கடவுச்சொல்","Password"))},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth(),singleLine=true) }
                            if(!model.exists()) item { OutlinedTextField(confirm,{confirm=it.take(256)},label={Text(bi("மீண்டும் கடவுச்சொல்","Confirm password"))},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth(),singleLine=true) }
                            item { Button(onClick={model.unlock(password,confirm);password="";confirm=""},enabled=!state.busy) { Text(if(model.exists())bi("திற","Unlock") else bi("புதிய பெட்டகம் உருவாக்கு","Create vault")) } }
                        } else {
                            item {
                                val now=LocalDate.now();val zone=ZoneId.systemDefault()
                                val today=VaultCore.total(VaultCore.today(state.entries,now,zone))
                                val month=VaultCore.total(state.entries.filter { YearMonth.from(Instant.ofEpochMilli(it.time).atZone(zone))==YearMonth.from(now) })
                                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                                    Text(bi("இன்று: ${money(today)}","Today: ${money(today)}"),style=MaterialTheme.typography.titleMedium)
                                    Text(bi("இந்த மாதம்: ${money(month)}","This month: ${money(month)}"))
                                    Text(bi("எல்லாச் செலவுகள்: ${money(VaultCore.total(state.entries))}","All spending: ${money(VaultCore.total(state.entries))}"))
                                } }
                            }
                            item { Row { TextButton(onClick={tab=0}) { Text(bi("செலவுகள்","Spending")) };TextButton(onClick={tab=1}) { Text(bi("குறிப்புகள் / வரலாறு","Notes / history")) } } }
                            item { Button(onClick={draft=Entry(title="",category=if(tab==0)"பொது" else "குறிப்பு",paise=if(tab==0)0 else null,note="")},enabled=!state.busy) { Text(bi("புதிய பதிவு","Add record")) } }
                            item { OutlinedTextField(search,{search=it},label={Text(bi("பதிவைத் தேடு","Search records"))},modifier=Modifier.fillMaxWidth(),singleLine=true) }
                            val entries=state.entries.filter { (if(tab==0)it.paise!=null else it.paise==null) && "${it.title} ${it.category} ${it.note}".contains(search,true) }.sortedByDescending { it.time }
                            if(entries.isEmpty()) item { Text(bi("இங்கு பதிவுகள் இல்லை","No records here")) }
                            items(entries,key={it.id}) { e -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                Text(e.title,style=MaterialTheme.typography.titleMedium)
                                e.paise?.let { Text(money(it),color=MaterialTheme.colorScheme.primary) }
                                Text(e.category+" · "+Instant.ofEpochMilli(e.time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd-MM-yyyy h:mm a")),style=MaterialTheme.typography.bodySmall)
                                if(e.note.isNotBlank()) Text(e.note)
                                Row { TextButton(onClick={draft=e},enabled=!state.busy) { Text(bi("திருத்து","Edit")) };TextButton(onClick={deleting=e},enabled=!state.busy) { Text(bi("நீக்கு","Delete")) } }
                            } } }
                            item { TextButton(onClick={showLockOptions=true}) { Text(bi("பூட்டு அமைப்புகள்","Lock settings")) } }
                            item { TextButton(onClick={model.lock()}) { Text(bi("பூட்டு","Lock")) } }
                            item { Button(onClick={export.launch("daily-vault-${LocalDate.now()}.tdv")},enabled=!state.busy) { Text(bi("மறைகுறியாக்கப்பட்ட காப்பு","Export encrypted backup")) } }
                        }
                        item { OutlinedButton(onClick={restore.launch(arrayOf("*/*"))},enabled=!state.busy) { Text(bi("கோப்பிலிருந்து மீட்டெடு","Restore from file")) } }
                        item {
                            TextButton(onClick={showHelp=!showHelp}) { Text(bi("உதவி / செயலியைப் பற்றி", "Help / About")) }
                            if(showHelp) Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Text(bi("தினக்கணக்கு · சோதனை 0.2. இணையம் அல்லது விளம்பரம் இல்லை. காப்பு கைமுறை; புதிய போனில் மீட்க காப்புக் கோப்பும் கடவுச்சொல்லும் தேவை. மறந்தால் மீட்பு இல்லை.","Daily Vault · preview 0.2. Offline, no ads. Backups are manual; a new phone needs the backup file and password. No password recovery."),style=MaterialTheme.typography.bodySmall)
                                Text(bi("பூட்டு வேண்டாம் என்றால் போனைப் பயன்படுத்துபவர் பதிவுகளைத் திறக்கலாம். சாதனப் பூட்டு ஒரு வசதி; காப்பின் கடவுச்சொல்லை மாற்றாது. பாதுகாப்பான இடத்தில் காப்பெடுத்து வேறு போனில் சோதிக்கவும்.","No-lock mode lets anyone using this phone open records. Device authentication is a convenience gate, not a new backup password. Save and test a backup on another phone."),style=MaterialTheme.typography.bodySmall)
                                TextButton(onClick={try { startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO,Uri.parse("mailto:universalrp2003@gmail.com"))) } catch(_:Exception){} }) { Text("universalrp2003@gmail.com") }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if(showLockOptions) AlertDialog(onDismissRequest={showLockOptions=false},title={Text(bi("திறக்கும் முறை","Unlock method"))},text={Column {
                        TextButton(onClick={model.configureLock("password");showLockOptions=false}) { Text(bi("ஒவ்வொரு முறையும் கடவுச்சொல்","Password each time")) }
                        TextButton(onClick={
                            val authenticators=BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            if(BiometricManager.from(this@MainActivity).canAuthenticate(authenticators)==BiometricManager.BIOMETRIC_SUCCESS) {
                                model.configureLock("device");showLockOptions=false
                            }
                        }) { Text(bi("சாதனப் பூட்டு / உயிரளவியல்","Screen lock / biometric")) }
                        TextButton(onClick={showLockOptions=false;confirmNoLock=true}) { Text(bi("செயலிப் பூட்டு வேண்டாம்","No app lock")) }
                        Text(bi("சாதனப் பூட்டு அமைக்கப்படாவிட்டால் முதலில் போன் அமைப்புகளில் அமைக்கவும். காப்பின் கடவுச்சொல் எப்போதும் தேவை.","Set up screen lock in phone settings first. Backup password remains required."),style=MaterialTheme.typography.bodySmall)
                    }},confirmButton={TextButton(onClick={showLockOptions=false}){Text(bi("மூடு","Close"))}})
                    if(confirmNoLock) AlertDialog(onDismissRequest={confirmNoLock=false},title={Text(bi("பூட்டு இல்லாமல் திறக்கவா?","Allow opening without app lock?"))},text={Text(bi("போனைப் பயன்படுத்துபவர் பதிவுகளைப் பார்க்கலாம். உள்ளூர் திறப்பு ரகசியம் சாதனத்தில் பாதுகாக்கப்படும்; காப்புக் கோப்பு கடவுச்சொல்லால் பாதுகாக்கப்படும்.","Anyone using this phone can view records. A device-local wrapped credential enables opening; exported backups still require the password."))},confirmButton={TextButton(onClick={model.configureLock("none");confirmNoLock=false}){Text(bi("அனுமதி","Allow"))}},dismissButton={TextButton(onClick={confirmNoLock=false}){Text(bi("ரத்து","Cancel"))}})
                    deleting?.let { e -> AlertDialog(onDismissRequest={deleting=null},title={Text(bi("பதிவை நீக்கவா?","Delete record?"))},text={Text(e.title)},confirmButton={TextButton(onClick={model.delete(e.id);deleting=null}) { Text(bi("நீக்கு","Delete")) }},dismissButton={TextButton(onClick={deleting=null}){Text(bi("ரத்து","Cancel"))}}) }
                    restoreUri?.let { uri -> AlertDialog(onDismissRequest={restoreUri=null;backupPassword=""},title={Text(bi("காப்பை மீட்டெடுக்கவா?","Restore backup?"))},text={Column {
                        Text(bi("தற்போதைய எல்லாப் பதிவுகளும் மாற்றப்படும்; ஒன்றிணைக்கப்படாது. பழைய பதிவுகள் தேவைப்பட்டால் முதலில் காப்பெடுக்கவும். காப்பின் கடவுச்சொல்லை உள்ளிடவும்.","REPLACES all current records; does not merge. Export current records first if needed. Enter the backup password."))
                        OutlinedTextField(backupPassword,{backupPassword=it.take(256)},label={Text(bi("காப்பின் கடவுச்சொல்","Backup password"))},visualTransformation=PasswordVisualTransformation())
                    }},confirmButton={TextButton(onClick={model.restore(uri,backupPassword);backupPassword="";restoreUri=null},enabled=backupPassword.length>=10&&!state.busy){Text(bi("மாற்றி மீட்டெடு","Replace and restore"))}},dismissButton={TextButton(onClick={restoreUri=null;backupPassword=""}){Text(bi("ரத்து","Cancel"))}}) }
                    draft?.let { entry -> EntryEditor(entry,tamil,onClose={draft=null},onSave={model.save(it);draft=null}) }
                }
            }
        }
    }
}

@Composable
private fun EntryEditor(entry: Entry,tamil: Boolean,onClose:()->Unit,onSave:(Entry)->Unit) {
    fun bi(ta:String,en:String)=if(tamil)ta else en
    var title by remember(entry.id) { mutableStateOf(entry.title) }
    var category by remember(entry.id) { mutableStateOf(entry.category) }
    var amount by remember(entry.id) { mutableStateOf(if(entry.paise==null || entry.paise==0L) "" else BigDecimal.valueOf(entry.paise,2).toPlainString()) }
    var note by remember(entry.id) { mutableStateOf(entry.note) }
    var date by remember(entry.id) { mutableStateOf(Instant.ofEpochMilli(entry.time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm"))) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onClose,title={Text(bi("பதிவு","Record"))},text={
        androidx.compose.foundation.rememberScrollState().let { scroll ->
            Column(Modifier.heightIn(max=450.dp).then(Modifier.verticalScroll(scroll)),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title,{title=it.take(200)},label={Text(bi("தலைப்பு","Title"))})
                OutlinedTextField(category,{category=it.take(80)},label={Text(bi("வகை: உணவு / கட்டணம் / சிலிண்டர்…","Category: food / fees / gas…"))})
                if(entry.paise!=null) OutlinedTextField(amount,{amount=it.take(14)},label={Text(bi("தொகை ₹","Amount ₹"))},singleLine=true)
                OutlinedTextField(date,{date=it},label={Text(bi("தேதி நேரம்: ஆண்டு-மாதம்-நாள் மணி:நிமிடம்","Date: yyyy-MM-dd HH:mm"))})
                OutlinedTextField(note,{note=it.take(20_000)},label={Text(bi("விவரம் / முக்கியத் தகவல்","Details / important information"))},minLines=3)
                if(error) Text(bi("தலைப்பு, வகை, தொகை, தேதி ஆகியவற்றைச் சரிபார்க்கவும்.","Check title, category, amount and date."))
            }
        }
    },confirmButton={TextButton(onClick={
        try {
            require(title.isNotBlank()&&category.isNotBlank())
            val time=LocalDateTime.parse(date,DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val edited=entry.copy(title=title.trim(),category=category.trim(),paise=if(entry.paise==null)null else VaultCore.amount(amount),note=note,time=time)
            VaultCore.encode(listOf(edited));onSave(edited)
        } catch(_:Exception){error=true}
    }){Text(bi("சேமி","Save"))}},dismissButton={TextButton(onClick=onClose){Text(bi("ரத்து","Cancel"))}})
}
