package com.universalrp.animalvoice

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.universalrp.updates.AppGuides
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import com.universalrp.updates.BrowserReleaseUpdates
import com.universalrp.updates.DailyWebsiteUpdateNotice
import com.universalrp.updates.GuideEntryRow
import com.universalrp.updates.GuideScreen
import com.universalrp.updates.SisterApp
import com.universalrp.updates.SisterApps
import com.universalrp.updates.StoreUpdateRouter
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class MainActivity : ComponentActivity() {
    private var engine: AudioEngine? = null
    private var generation = 0
    private var job: Job? = null
    private var samples by mutableStateOf<ShortArray?>(null)
    private var guesses by mutableStateOf<List<SoundGuess>?>(null)
    private var busy by mutableStateOf(false)
    private var messageText by mutableStateOf(BilingualText.EMPTY)
    private val message: String get() = messageText.display(tamil)
    private fun setMessage(ta: String, en: String) { messageText = BilingualText(ta, en) }
    private var v3NoticeText by mutableStateOf(BilingualText.EMPTY)
    private val v3Notice: String get() = v3NoticeText.display(tamil)
    private var noticeSeq = 0
    private fun setV3Notice(ta: String, en: String) { noticeSeq++; v3NoticeText = BilingualText(ta, en) }
    /** Success confirmations fade after a few seconds; errors and waiting states stay until the next action. */
    private fun showV3Success(ta: String, en: String) {
        v3NoticeOk = true; setV3Notice(ta, en)
        val seq = noticeSeq
        lifecycleScope.launch { delay(6_000); if (noticeSeq == seq && v3NoticeOk) setV3Notice("", "") }
    }
    private var v3NoticeOk by mutableStateOf(false)
    /** Interface language lives in the shared RP Ideas preference so this app, Guide and About always agree. */
    private val tamil: Boolean get() = AppLanguageState.current == AppLanguage.TAMIL
    private fun toggleLanguage() = AppLanguageState.set(this, if (tamil) AppLanguage.ENGLISH else AppLanguage.TAMIL)
    /** "" = main/Settings, "guide" = built-in guide, "about" = About screen. */
    private var overlay by mutableStateOf("")
    private var confirmReplay by mutableStateOf(false)
    private var confirmDownload by mutableStateOf(false)
    private var downloadingV3 by mutableStateOf(false)
    private var downloadBytes by mutableStateOf(0L)
    private var downloadTotal by mutableStateOf<Long?>(null)
    private var downloadIsModel by mutableStateOf(true)
    private var downloadWaiting by mutableStateOf(false)
    private val lastProgressUpdate = AtomicLong(0)
    private val photos = mutableStateMapOf<String, ReferencePhoto.Result>()
    private val photoFailures = mutableStateMapOf<String, Boolean>()
    private var autoPhotos by mutableStateOf(true)
    private var confirmAutoPhotos by mutableStateOf(false)
    private var requestedPhoto by mutableStateOf("")
    private var imageSearchSpecies by mutableStateOf("")
    private var photoLoading by mutableStateOf(false)
    private var confirmPhoto by mutableStateOf(false)
    private var photoJob: Job? = null
    private var photoEpoch = 0
    private var showSettings by mutableStateOf(false)
    private var sampleRate = AudioEngine.RATE
    private var birdnetReady by mutableStateOf(false)
    private var wantV3 by mutableStateOf(false)
    private var v3ModelReady by mutableStateOf(false)
    private var v3LabelsReady by mutableStateOf(false)
    private var v3Guesses by mutableStateOf<List<BirdNetV3.Guess>?>(null)
    private var v3WindowCount by mutableIntStateOf(1)
    private var v3Candidates by mutableStateOf<List<BirdNetV3.Candidate>>(emptyList())
    private var v3ResultWindows by mutableIntStateOf(1)
    private var importRunning: AtomicBoolean? = null
    private fun tr(ta: String, en: String) = if (tamil) ta else en
    /**
     * Tamil glyphs fill more of the em box than Latin letters, so the same sp reads larger and wraps earlier
     * (the 0.21 phone screenshots showed the Tamil title and heading breaking into two lines). Drawing Tamil
     * at 85% keeps both interfaces the same visual size; dp paddings and 48 dp touch targets are unchanged.
     */
    private val TAMIL_TEXT_SCALE = 0.85f
    /** About list name: "தமிழன் · Tamilan Store" in Tamil, the catalog name in English. */
    private fun appListName(app: SisterApp): String =
        if (tamil && app.tamilName.isNotBlank()) "${app.tamilName} · ${app.name.substringBefore(" (")}" else app.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Owner choice (November 2026): reference photos are on from the first launch; Settings can turn them off.
        autoPhotos = getPreferences(MODE_PRIVATE).getBoolean("auto_photos", true)
        birdnetReady = BirdNet.installed(this)
        updateV3Status()
        AppLanguageState.initialize(this)
        setContent {
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) { if (wantV3) listenV3() else listen() } else setMessage("ஒலியைக் கேட்க மைக்ரோஃபோன் அனுமதி தேவை.", "Microphone permission is needed to listen.")
            }
            val v3ModelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) importV3(uri, true)
            }
            val v3LabelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) importV3(uri, false)
            }
            // Settings is Compose state, not a separate activity: Back returns to the main screen instead of leaving the app.
            BackHandler(enabled = showSettings) { showSettings = false }
            // Both screens share one scroll container; start each screen at the top.
            val scroll = rememberScrollState()
            LaunchedEffect(showSettings, overlay) { scroll.scrollTo(0) }
            val base = LocalDensity.current
            val density = if (tamil) Density(base.density, base.fontScale * TAMIL_TEXT_SCALE) else base
            CompositionLocalProvider(LocalDensity provides density) {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF276A54), secondary = Color(0xFF94703A), surface = Color(0xFFF7FAF4))) {
                Surface(Modifier.fillMaxSize()) {
                    when (overlay) {
                        "guide" -> GuideScreen(AppGuides.animalSounds, tamil, onBack = { overlay = "" }, accent = Color(0xFF276A54), surface = Color(0xFFEAF3EB))
                        "about" -> AboutContent { overlay = "" }
                        else -> Column(
                            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(scroll).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (showSettings) SettingsContent(permission, v3ModelPicker, v3LabelPicker) else MainContent(permission)
                        }
                    }
                }
                // Read-only, at most once-a-day check of public release metadata; opens the official website, never installs.
                DailyWebsiteUpdateNotice("animalvoice", tamil)
                if (imageSearchSpecies.isNotEmpty()) AlertDialog(
                    onDismissRequest = { imageSearchSpecies = "" },
                    title = { Text(tr("படங்களை இணையத்தில் தேடவா?", "Search images online?")) },
                    text = { Text(tr("இனத்தின் பெயர் Google-க்கு அனுப்பப்பட்டு உலாவி திறக்கும். ஒலி அனுப்பப்படாது. உங்கள் இணைய முகவரி Google-க்குத் தெரியும். தேடல் படங்கள் சரியாகவோ சுதந்திரமாகப் பயன்படுத்தக்கூடியதாகவோ இருக்க வேண்டியதில்லை.", "The species name will be sent to Google and your browser will open. No audio is sent; Google can see your IP address. Search images may be inaccurate or copyrighted.")) },
                    confirmButton = { TextButton(onClick = {
                        val uri = Uri.parse("https://www.google.com/search").buildUpon().appendQueryParameter("tbm", "isch").appendQueryParameter("q", imageSearchSpecies).build()
                        imageSearchSpecies = ""
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure { setMessage("உலாவியைத் திறக்க முடியவில்லை.", "Could not open a browser.") }
                    }) { Text(tr("தேடு", "Search")) } },
                    dismissButton = { TextButton(onClick = { imageSearchSpecies = "" }) { Text(tr("வேண்டாம்", "Cancel")) } }
                )
                if (confirmAutoPhotos) AlertDialog(
                    onDismissRequest = { confirmAutoPhotos = false },
                    title = { Text(tr("படங்களைத் தானாகப் பதிவிறக்கவா?", "Load photos automatically?")) },
                    text = { Text(tr("ஒவ்வொரு முடிவின் இனப் பெயரும் Wikipedia-க்கு அனுப்பப்பட்டு, படம் Wikimedia-லிருந்து பதிவிறக்கப்படும். உங்கள் இணைய முகவரி அவர்களுக்குத் தெரியும்; ஒலி அனுப்பப்படாது. இதை எப்போது வேண்டுமானாலும் அணைக்கலாம்.", "After each identification, candidate species names are sent to Wikipedia and photos downloaded from Wikimedia. They can see your IP address; no audio is sent. You can turn this off at any time.")) },
                    confirmButton = { TextButton(onClick = {
                        confirmAutoPhotos = false; autoPhotos = true
                        getPreferences(MODE_PRIVATE).edit().putBoolean("auto_photos", true).apply()
                        loadReferencePhotos(v3Guesses.orEmpty().map { it.scientific })
                    }) { Text(tr("இயக்கு", "Enable")) } },
                    dismissButton = { TextButton(onClick = { confirmAutoPhotos = false }) { Text(tr("வேண்டாம்", "Cancel")) } }
                )
                if (confirmPhoto) AlertDialog(
                    onDismissRequest = { confirmPhoto = false },
                    title = { Text(tr("மாதிரிப் படத்தைப் பார்க்கவா?", "Show a reference photo?")) },
                    text = { Text(tr("இனத்தின் பெயர் Wikipedia-க்கு அனுப்பப்படும்; படம் Wikimedia Commons-லிருந்து வரும். இது நீங்கள் கேட்ட உயிரினம் அல்ல. ஒலி அனுப்பப்படாது; உங்கள் இணைய முகவரி அச்சேவைகளுக்குத் தெரியும்.", "Sends the species name to Wikipedia and loads a reference image from Wikimedia Commons. It is not the animal you heard. No audio is sent; the providers can see your IP address.")) },
                    confirmButton = { TextButton(onClick = { confirmPhoto = false; loadReferencePhotos(listOf(requestedPhoto)) }) { Text(tr("படத்தைக் காட்டு", "Show photo")) } },
                    dismissButton = { TextButton(onClick = { confirmPhoto = false }) { Text(tr("வேண்டாம்", "Cancel")) } }
                )
                if (confirmDownload) AlertDialog(
                    onDismissRequest = { confirmDownload = false },
                    title = { Text(tr("மாடலைப் பதிவிறக்கவா?", "Download the model?")) },
                    text = { Text(tr("இணையத்தில் சுமார் 79 MB வரை பயன்படுத்தும். மொபைல் டேட்டாவுக்கு கட்டணம் வரலாம். Zenodo-விலிருந்து விடுபட்ட கோப்புகள் மட்டும் வரும்; உங்கள் ஒலி பதிவை அனுப்பாது. இணையம் இல்லாவிட்டால், செயலி திறந்திருக்கும் வரை காத்திருக்கும். பதிவிறக்க முன்னேற்றம் காட்டப்படும்.", "Downloads up to about 79 MB from Zenodo; mobile data charges may apply. Only missing files are downloaded. No audio is uploaded. If offline, it waits while the app stays open. Download progress is shown.")) },
                    confirmButton = { TextButton(onClick = { confirmDownload = false; downloadV3() }) { Text(tr("பதிவிறக்கு", "Download")) } },
                    dismissButton = { TextButton(onClick = { confirmDownload = false }) { Text(tr("வேண்டாம்", "Cancel")) } }
                )
                if (confirmReplay) AlertDialog(onDismissRequest = { confirmReplay = false }, title = { Text(tr("பதிவை மீண்டும் கேட்கவா?", "Replay the recording?")) },
                    text = { Text(tr("இது AI பதில் அல்ல. உங்கள் பதிவில் மனிதர்களின் குரலும் இருக்கலாம். விலங்குகள் அருகே இயக்காதீர்கள்; இயர்போன் பயன்படுத்துங்கள்.", "This is not an AI reply. Your sample may contain people's voices. Do not play near animals; use headphones.")) },
                    confirmButton = { TextButton(onClick = { confirmReplay = false; replay() }) { Text(tr("மீண்டும் கேள்", "Replay")) } },
                    dismissButton = { TextButton(onClick = { confirmReplay = false }) { Text(tr("வேண்டாம்", "Cancel")) } })
            }
            }
        }
    }

    @Composable private fun MainContent(permission: ActivityResultLauncher<String>) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("🌿  விலங்கு ஒலிகள்", "🌿  Animal Sounds"), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showSettings = true }, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = tr("அமைப்புகள்", "Settings") }) { Text("⚙", fontSize = 22.sp) }
                            TextButton(onClick = { toggleLanguage() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (tamil) "English" else "தமிழ்") }
                        }
                        Text(tr("ஒலியைக் கேட்டு அறியுங்கள்", "Listen and identify"), fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                        Text(tr("இது ஒலி வகையை ஊகிக்கும் ஒரு சோதனை. விலங்கின் எண்ணத்தை மொழிபெயர்க்காது. ஒலி இந்தப் போனிலிருந்து வெளியே போகாது.", "This experiment guesses sound categories. It does not translate an animal's thoughts. Audio never leaves this phone."), style = MaterialTheme.typography.bodyMedium)
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1E7)), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(tr("உயிரின மாடல்", "Species model"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                if (v3ModelReady && v3LabelsReady) {
                                    Text(tr("தயார் ✓ · பதிவிறக்கத்திற்குப் பிறகு இணையமின்றி வேலை செய்யும். மதிப்பெண்கள் சரிசெய்யப்படாத ஊகங்கள்.", "Ready ✓ · works offline after download. Scores are uncalibrated guesses."), style = MaterialTheme.typography.bodySmall)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(Modifier.weight(1f).heightIn(min = 48.dp).selectable(selected = v3WindowCount == 1, role = Role.RadioButton, onClick = { v3WindowCount = 1 }, enabled = !busy), verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = v3WindowCount == 1, onClick = null, enabled = !busy)
                                            Text(tr("3 விநாடி", "3 seconds"))
                                        }
                                        Row(Modifier.weight(1f).heightIn(min = 48.dp).selectable(selected = v3WindowCount == 3, role = Role.RadioButton, onClick = { v3WindowCount = 3 }, enabled = !busy), verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = v3WindowCount == 3, onClick = null, enabled = !busy)
                                            Text(tr("9 விநாடி", "9 seconds"))
                                        }
                                    }
                                    if (v3WindowCount == 3) Text(tr("மெல்லிய ஒலிக்காக அதிக நேரம் கேட்கும். தவறான ஊகங்களும் அதிகரிக்கலாம்.", "Longer listening may catch a quiet call, but may also increase false guesses."), style = MaterialTheme.typography.bodySmall)
                                    Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = {
                                        wantV3 = true
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listenV3()
                                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                                    }) { Text(tr("🎙  உயிரின ஒலியைக் கேள்", "🎙  Identify animal sound")) }
                                } else {
                                    Text(tr("உயிரின ஊகங்களுக்கு ஒரு முறை சுமார் 79 MB பதிவிறக்கவும். அடிப்படை இணையமில்லாத மாடல் ⚙ அமைப்புகளில் உள்ளது. ஒலி எப்போதும் பதிவேற்றப்படாது.", "Download about 79 MB once for species guesses. The basic offline model is in ⚙ Settings. Audio is never uploaded."), style = MaterialTheme.typography.bodyMedium)
                                    Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = { confirmDownload = true }) { Text(tr("உயிரின மாடலைப் பதிவிறக்கு (~79 MB)", "Download species model (~79 MB)")) }
                                    Text(tr("பதிவிறக்கத்திற்குப் பிறகு கோப்புகள் சரிபார்க்கப்படும். செயலியைத் திறந்தே வைக்கவும்.", "Files are verified after download. Keep the app open."), style = MaterialTheme.typography.bodySmall)
                                }
                                if (v3Notice.isNotEmpty()) Text(v3Notice, color = if (v3NoticeOk || downloadingV3) Color(0xFF276A54) else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                if (busy && !downloadingV3) {
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                    TextButton(onClick = { stopAndClear() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("நிறுத்து", "Stop")) }
                                }
                                if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (downloadingV3) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F2EE)), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (downloadIsModel) tr("உயிரின மாடல் பதிவிறக்கப்படுகிறது", "Downloading species model") else tr("பெயர் பட்டியல் பதிவிறக்கப்படுகிறது", "Downloading label list"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    if (downloadWaiting) {
                                        LinearProgressIndicator(Modifier.fillMaxWidth())
                                        Text(tr("இணையத்திற்காகக் காத்திருக்கிறது…", "Waiting for internet…"), style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        val fraction = DownloadProgress.fraction(downloadBytes, downloadTotal)
                                        if (fraction != null) {
                                            val progress = fraction
                                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                                        } else LinearProgressIndicator(Modifier.fillMaxWidth())
                                        Text(DownloadProgress.summary(downloadBytes, downloadTotal, tamil), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(tr("செயலியைத் திறந்தே வைக்கவும். நிறுத்தினால் நிலுவையில் உள்ள கோப்பு மட்டும் ரத்தாகும்; சரிபார்க்கப்பட்ட கோப்புகள் நீக்கப்படாது.", "Keep the app open. Stop cancels the missing file; verified files stay installed."), style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { stopAndClear(); v3NoticeOk = false; setV3Notice("பதிவிறக்கம் நிறுத்தப்பட்டது.", "Download stopped.") }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("பதிவிறக்கத்தை நிறுத்து", "Stop download")) }
                                }
                            }
                        }
                        v3Guesses?.let { found ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F2EE)), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(tr("சாத்தியமான ஒலி வகைகள்", "Possible sound sources"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    if (v3ResultWindows == 3) Text(tr("மூன்று பகுதிகளிலிருந்தும் அதிக மதிப்பெண் பெற்ற மூன்று ஊகங்கள் வரை; இவை உறுதி அல்ல.", "Up to three highest-scoring guesses across all three windows; these are not confirmations."), style = MaterialTheme.typography.bodySmall)
                                    if (found.isEmpty()) Text(tr("தெளிவாகத் தெரியவில்லை.", "Unknown: no qualifying match."))
                                    else found.forEach { guess ->
                                        Text("• " + SpeciesPresentation.display(guess.scientific, guess.common, tamil))
                                        SpeciesPresentation.taxonGroup(guess.taxon, tamil)?.let { group -> Text(tr("வகை: ", "Group: ") + group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                                        if (v3ResultWindows == 3) {
                                            val ranges = v3Candidates.firstOrNull { it.guess == guess }?.windows.orEmpty().joinToString(", ") { "${it * 3}–${it * 3 + 3}" }
                                            Text(tr("பகுதிகள்: $ranges விநாடி", "Qualifying windows: $ranges seconds"), style = MaterialTheme.typography.bodySmall)
                                        }
                                        SpeciesPresentation.geographicCaveat(guess.scientific, tamil)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A5A00)) }
                                        SpeciesPresentation.speechNote(guess.scientific, tamil)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A5A00)) }
                                        if (ReferencePhoto.validScientific(guess.scientific) && !guess.scientific.equals("Homo sapiens", true)) {
                                            TextButton(onClick = { imageSearchSpecies = guess.scientific }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("இணையத்தில் படங்களைத் தேடு", "Search images in browser")) }
                                            val photo = photos[guess.scientific]
                                            if (photo != null) {
                                                Image(photo.bitmap.asImageBitmap(), contentDescription = tr("மாதிரிப் படம்", "Reference image"), modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp))
                                                Text(tr("மாதிரிப் படம்; இது கேட்ட உயிரினத்தை உறுதி செய்யாது.", "Reference image; it does not confirm what you heard."), style = MaterialTheme.typography.bodySmall)
                                                Text("${photo.title} · ${photo.artist} · ${photo.licence} · Wikimedia Commons", style = MaterialTheme.typography.bodySmall)
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    TextButton(onClick = { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(photo.creditUrl))) } }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(tr("பட விவரம்", "Photo details")) }
                                                    TextButton(onClick = { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(photo.licenceUrl))) } }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(tr("உரிமம்", "Licence")) }
                                                }
                                            } else {
                                                OutlinedButton(enabled = !photoLoading, onClick = { requestedPhoto = guess.scientific; confirmPhoto = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (photoLoading && requestedPhoto == guess.scientific) tr("படம் வருகிறது…", "Loading photo…") else tr("மாதிரிப் படம் காட்டு", "Show reference photo")) }
                                                if (photoFailures[guess.scientific] == true) Text(tr("படம் கிடைக்கவில்லை. இணையத்தைச் சரிபார்க்கவும்; எல்லா இனங்களுக்கும் படம் கிடைக்காது.", "Photo unavailable. Check your connection; not every species has a usable image."), style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        SpeciesPresentation.description(guess.scientific, tamil)?.let { detail -> Text(detail, style = MaterialTheme.typography.bodySmall) }
                                    }
                                    Text(tr("இவை உறுதியற்ற ஊகங்கள். மனிதர் அல்லது பிற சத்தங்களையும் காட்டலாம். இடம் பயன்படுத்தப்படவில்லை. படங்கள் ஊகத்தை உறுதிப்படுத்தாது.", "Unverified guesses; results can include human or other sounds. No location filter is used. Images do not confirm the guess."), style = MaterialTheme.typography.bodySmall)
                                    if (samples != null) OutlinedButton(enabled = !busy, onClick = { confirmReplay = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("பதிவைக் கேள்", "Replay my sample")) }
                                }
                            }
                        }
                        guesses?.let { found ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(tr("அடிப்படை மாடல் முடிவு", "Basic model result"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                            if (found.isEmpty()) Text(tr("தெளிவாக அடையாளம் காண முடியவில்லை. அமைதியான இடத்தில் மீண்டும் முயற்சிக்கவும்.", "Unknown. The sound was too quiet, ambiguous or not supported. Try again in a quieter place."))
                                            else found.forEach { Text("• " + (if (tamil) "${it.tamil} (${it.label})" else it.label)) }
                                            Text(tr("இவை பரந்த ஒலி வகைகள் மட்டுமே; இனம் அல்லது பொருள் அல்ல. இதே ஒலி வேறு விலங்குகளுக்கும் இருக்கலாம்.", "Broad categories only; not species or meaning. Similar sounds can come from different animals."), style = MaterialTheme.typography.bodySmall)
                                            if (samples != null) OutlinedButton(enabled = !busy, onClick = { confirmReplay = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("பதிவைக் கேள்", "Replay my sample")) }
                                }
                            }
                        }
        }
    }

    @Composable private fun SettingsContent(
        permission: ActivityResultLauncher<String>,
        v3ModelPicker: ActivityResultLauncher<Array<String>>,
        v3LabelPicker: ActivityResultLauncher<Array<String>>,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showSettings = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("←  பின்", "←  Back")) }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { toggleLanguage() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (tamil) "English" else "தமிழ்") }
                        }
                        Text(tr("அமைப்புகள்", "Settings"), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                        Text(tr("இங்கே உள்ளவை அனைத்தும் விருப்பமானவை. உயிரின ஒலியைக் கேட்க முதன்மைத் திரைக்குத் திரும்பவும்.", "Everything here is optional. Return to the main screen to identify animal sounds."), style = MaterialTheme.typography.bodyMedium)
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tr("மாதிரிப் படங்கள்", "Reference photos"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(tr("மாதிரிப் படங்களைத் தானாகக் காட்டு", "Load reference photos automatically"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Switch(checked = autoPhotos, onCheckedChange = { enabled ->
                                        if (enabled) confirmAutoPhotos = true else {
                                            autoPhotos = false
                                            getPreferences(MODE_PRIVATE).edit().putBoolean("auto_photos", false).apply()
                                            photoEpoch++; photoJob?.cancel(); photoJob = null; photoLoading = false; photos.clear(); photoFailures.clear(); requestedPhoto = ""
                                        }
                                    })
                                }
                                Text(tr("இயல்பாக இயக்கப்பட்டுள்ளது. ஒவ்வொரு அடையாளத்திற்கும் பிறகு இனப் பெயர்கள் மட்டும் Wikipedia/Wikimedia-க்கு அனுப்பப்படும்; ஒலி அனுப்பப்படாது. படங்கள் ஊகத்தை உறுதிப்படுத்தாது. விரும்பினால் இங்கே அணைக்கலாம்.", "On by default. After each identification only the species names are sent to Wikipedia/Wikimedia; no audio is sent. Photos do not confirm the guess. Turn it off here if you prefer."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tr("அடிப்படை இணையமில்லாத கேட்பு", "Basic offline listening"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                Text(tr("உட்பொதிக்கப்பட்ட YAMNet: பரந்த ஒலி வகைகள் மட்டும்; பல்லி வகை இல்லை. பதிவிறக்கம் தேவையில்லை; இணையமில்லாமல் வேலை செய்யும்.", "Bundled YAMNet: broad sound categories only; no lizard class. No download; works offline."), style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(enabled = !busy, onClick = {
                                    wantV3 = false; showSettings = false
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
                                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(tr("🎙  பொது ஒலியைக் கேள்", "🎙  Listen with basic model")) }
                                Text(tr("இது பரந்த ஒலி வகைகளை மட்டுமே காட்டும்; முடிவு முதன்மைத் திரையில் தோன்றும்.", "It shows broad sound categories only; the result appears on the main screen."), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Stacked on purpose: a side-by-side toggle squeezed the Tamil title into one-character lines.
                                Text(tr("உயிரின மாடல் கோப்புகள்", "Species model files"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                if (v3Notice.isNotEmpty()) Text(v3Notice, color = if (v3NoticeOk || downloadingV3) Color(0xFF276A54) else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    if (birdnetReady) {
                                        Text(tr("பழைய பறவை மாடல் இனி பயன்படுத்தப்படாது.", "The old bird-only model is no longer used."), style = MaterialTheme.typography.bodySmall)
                                        TextButton(enabled = !busy, onClick = {
                                            val removed = BirdNet.file(this@MainActivity).delete()
                                            birdnetReady = BirdNet.installed(this@MainActivity)
                                            if (!removed && birdnetReady) setMessage("பழைய மாடலை நீக்க முடியவில்லை.", "Could not remove the old model.")
                                        }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("பழைய பறவை மாடலை நீக்கு", "Remove old bird model")) }
                                    }
                                    Text(tr("புதிய உயிரின ஒலி ஆய்வு · சோதனை", "New animal species recognizer · experimental"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(tr("BirdNET+ 3.0: பறவைகள் மற்றும் பிற விலங்குகளை ஊகிக்கும். தவறாக இருக்கலாம். பொருளையோ பதிலையோ அறியாது.", "BirdNET+ 3.0 guesses birds and other species. It can be wrong. It does not determine meaning or generate replies."), style = MaterialTheme.typography.bodySmall)
                                Text(tr("கோப்புகள் நிறுவுவதற்கு முன் SHA-256 மூலம் சரிபார்க்கப்படும்.", "Files are verified by SHA-256 before installation."), style = MaterialTheme.typography.bodySmall)
                                if (v3ModelReady && v3LabelsReady) Text(tr("இரண்டு கோப்புகளும் தயார் ✓", "Both files are ready ✓"), style = MaterialTheme.typography.bodySmall)
                                else Text(tr("நிறுவப்படவில்லை. முதன்மைத் திரையில் உள்ள Download species model பொத்தானைப் பயன்படுத்தவும்.", "Not installed. Use Download species model on the main screen."), style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(enabled = !busy, onClick = {
                                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zenodo.org/records/24162518"))) }
                                        .onFailure { setMessage("உலாவியைத் திறக்க முடியவில்லை.", "Could not open a browser.") }
                                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("இரண்டு கோப்புகளின் பக்கத்தைத் திற", "Open both files' download page")) }
                                Text(tr("மாடல் கோப்பு .tflite என முடிய வேண்டும்; .onnx கோப்பைத் தேர்வு செய்ய வேண்டாம்.", "The model must end in .tflite, not .onnx."), style = MaterialTheme.typography.bodySmall)
                                TextButton(enabled = !busy, onClick = {
                                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zenodo.org/records/24162518/files/BirdNET+_V3.0_Global_12K_FP16_pruned.tflite?download=1"))) }
                                        .onFailure { v3NoticeOk = false; setV3Notice("உலாவியைத் திறக்க முடியவில்லை.", "Could not open a browser.") }
                                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("சரியான .tflite மாடலைப் பதிவிறக்கு", "Download the correct .tflite model")) }
                                OutlinedButton(enabled = !busy, onClick = { v3ModelPicker.launch(arrayOf("*/*")) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("புதிய மாடல் கோப்பைத் தேர்வு செய்", "Select v3 model file") + if (v3ModelReady) " ✓" else "") }
                                OutlinedButton(enabled = !busy, onClick = { v3LabelPicker.launch(arrayOf("*/*")) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("பெயர் பட்டியல் CSV கோப்பைத் தேர்வு செய்", "Select v3 labels CSV") + if (v3LabelsReady) " ✓" else "") }
                                if (v3ModelReady && v3LabelsReady) {
                                    TextButton(enabled = !busy, onClick = { BirdNetV3.model(this@MainActivity).delete(); BirdNetV3.labels(this@MainActivity).delete(); updateV3Status(); v3Guesses = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("புதிய மாடல் கோப்புகளை நீக்கு", "Remove v3 files")) }
                                    }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Owner choice (November 2026): every detail is shown directly; nothing hides behind a dropdown.
                                Text(tr("எதை அறியலாம்?", "What can it identify?"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                    Text(tr("BirdNET+ V3 பட்டியலில் 11,709 உயிரினங்கள்: 9,788 பறவைகள், 802 பூச்சிகள், 649 நீர்நில வாழ்வன, 419 பாலூட்டிகள் (மனிதர் உட்பட), 35 மீன்கள், 15 ஊர்வன, 1 சிலந்தி வகை. மேலும் 16 உயிரினமல்லாத ஒலி வகைகள்.", "BirdNET+ V3 lists 11,709 animal species: 9,788 birds, 802 insects, 649 amphibians, 419 mammals (including humans), 35 fishes, 15 reptiles and 1 arachnid; plus 16 non-animal sound classes."), style = MaterialTheme.typography.bodyMedium)
                                Text(tr("பட்டியலில் இருப்பது சரியாக அறியும் என்பதற்கு உறுதியல்ல. பெரும்பாலும் பறவைகளே உள்ளன. பூனை, மாடு, வீட்டுக் காகம், வீட்டுப் பல்லி பட்டியலில் உள்ளன. எல்லா நாய் அல்லது எலி இனங்களும் உள்ளனவா என உறுதியில்லை. மெல்லிய ஒலி தவறவிடப்படலாம்.", "A listed species is not a guarantee of identification; most are birds. House Crow, Domestic Cat, Domestic Cattle and Asian House Gecko are listed. Coverage of dogs and rats is not established. Quiet calls can be missed."), style = MaterialTheme.typography.bodyMedium)
                                Text(tr("சில எலி ஒலிகள் மீயொலி; மீன் ஒலிக்குத் தண்ணீருக்கேற்ற ஒலிவாங்கி தேவைப்படலாம். எறும்புகளின் இரசாயனத் தகவலைக் கேட்க முடியாது. பொருள் அறியவோ பதில் உருவாக்கவோ முடியாது.", "Some rat calls are ultrasonic; fish sounds may need an underwater microphone. Ant chemical signals cannot be heard. The app does not determine meaning or generate replies."), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tr("ஒலி அறிதல் · சோதனை", "SOUND IDENTIFICATION · EXPERIMENTAL"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF276A54))
                                Text(tr("இது ஒரு சோதனை முன்னோட்டம். ஊகங்களும் மாதிரிப் படங்களும் உயிரினத்தை உறுதிப்படுத்தாது. மதிப்பெண்கள் சரிசெய்யப்படாதவை; கூடுதல் பகுதிகள் தவறான ஊகங்களை அதிகரிக்கலாம். இடம் பயன்படுத்தப்படவில்லை. ஒலி இந்தப் போனிலிருந்து வெளியே போகாது.", "Experimental prototype. Guesses and reference photos do not confirm a species. Scores are uncalibrated; more windows can increase false guesses. No location filter is used. Audio never leaves this phone."), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tr("வழிகாட்டியும் இந்தச் செயலியும்", "Guide & about"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                GuideEntryRow(tamil, onClick = { overlay = "guide" }, accent = Color(0xFF276A54))
                                TextButton(onClick = { overlay = "about" }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(tr("இந்தச் செயலி பற்றி — எமது செயலிகள், இணைப்புகள், நன்றிகள்", "About this app — our apps, links & credits")) }
                            }
                        }
        }
    }

    /** The RP Ideas About screen: credits, official links, update policy, model licences and the shared app directory. */
    @Composable private fun AboutContent(onBack: () -> Unit) {
        BackHandler { onBack() }
        var browserFailed by remember { mutableStateOf(false) }
        val version = remember { runCatching { packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() }.getOrDefault("") }
        fun openLink(url: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure { browserFailed = true } }
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("←  பின்", "←  Back")) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { toggleLanguage() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (tamil) "English" else "தமிழ்") }
            }
            Text(tr("இந்தச் செயலி", "About"), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1E7)), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("விலங்கு ஒலிகள் · Animal Sounds", "Animal Sounds · விலங்கு ஒலிகள்"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Text(tr("பதிப்பு $version · சோதனை முன்னோட்டம்", "Version $version · experimental prototype"), style = MaterialTheme.typography.bodySmall)
                    Text(tr("ஒலியைக் கேட்டுச் சாத்தியமான ஒலி வகைகளை இந்தப் போனிலேயே ஊகிக்கும் செயலி. விலங்கின் எண்ணத்தை மொழிபெயராது; பொருளை அறியாது; பதில் உருவாக்காது. ஒலி இந்தப் போனிலிருந்து வெளியே போகாது.", "Listens and guesses possible sound sources on this phone. It does not translate an animal's thoughts, determine meaning or generate replies. Audio never leaves this phone."), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("உருவாக்கம்", "Created by"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Text(tr("Ramesh prathap .R (universalrp2003@gmail.com) · RP Ideas", "Ramesh prathap .R (universalrp2003@gmail.com) · RP Ideas"), style = MaterialTheme.typography.bodyMedium)
                    Text(tr("மென்பொருள் உதவி: Arena.ai Agent Mode. மூல நிரலும் பங்களிப்பாளர் விவரங்களும் திட்ட இணைப்புகளில் உள்ளன.", "Software assistance: Arena.ai Agent Mode. Source and contributor credits are in the project links."), style = MaterialTheme.typography.bodyMedium)
                    Text(tr("இலவசம். விளம்பரங்களோ கண்காணிப்போ இல்லை. புதுப்பிப்பை நிறுவ உங்கள் ஒப்புதல் தேவை.", "Free, no ads or tracking. Updates require your confirmation."), style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("இணைப்புகள்", "Links"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { if (!StoreUpdateRouter.openWebsite(this@MainActivity, "animalvoice")) browserFailed = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(tr("அதிகாரப்பூர்வ இணையதளம் — இந்தச் செயலியின் பக்கம்", "Official website — this app's page")) }
                    // Owner choice (November 2026): Links carry only the project website and repository.
                    // The apps are a plain name list in the next card; downloads happen on the website.
                    listOf(
                        "RP-Android-Apps Official Website" to SisterApps.OFFICIAL_WEBSITE_URL,
                        "RP-Android-Apps GitHub Repository" to SisterApps.GITHUB_REPO_URL,
                    ).forEach { (label, url) ->
                        TextButton(onClick = { openLink(url) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label, style = MaterialTheme.typography.bodyMedium) }
                    }
                    if (browserFailed) Text(tr("உலாவியைத் திறக்க முடியவில்லை.", "Could not open a browser."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("புதுப்பிப்புகள்", "Updates"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    BrowserReleaseUpdates("Animal-Sounds", tamil)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("மாடல் நன்றிகளும் உரிமங்களும்", "Model credits & licences"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Text("BirdNET+ V3.0 · Cornell Lab of Ornithology / Chemnitz University · Apache 2.0 · Zenodo 24162518", style = MaterialTheme.typography.bodySmall)
                    Text("YAMNet / AudioSet · TensorFlow · Apache 2.0; AudioSet ontology CC BY-SA 4.0. github.com/tensorflow/models/tree/master/research/audioset/yamnet", style = MaterialTheme.typography.bodySmall)
                    Text(tr("மாதிரிப் படங்கள் Wikimedia Commons-லிருந்து; ஒவ்வொரு படத்தின் நன்றியும் உரிமமும் படத்தின் கீழே காட்டப்படும்.", "Reference photos come from Wikimedia Commons; each photo shows its own credit and licence."), style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("எமது செயலிகள்", "Our apps"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Text(tr("பதிவிறக்கங்கள் அதிகாரப்பூர்வ இணையதளத்தில் உள்ளன; இங்கே பெயர்கள் மட்டும்.", "Downloads are on the official website; this list is names only."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Every app from the shared directory, Store and DailyVault first (owner choice, November 2026), so this list never drifts from the website.
                    (listOf("tamilanstore", "dailyvault") + SisterApps.all.map { it.id }.filter { it != "tamilanstore" && it != "dailyvault" }).forEach { id ->
                        val app = SisterApps.byId(id) ?: return@forEach
                        val mine = id == "animalvoice"
                        Text(appListName(app) + if (mine) tr(" — இந்தச் செயலி", " — this app") else "", style = MaterialTheme.typography.bodyMedium, color = if (mine) Color(0xFF276A54) else Color.Unspecified)
                    }
                    TextButton(onClick = { if (!StoreUpdateRouter.openWebsite(this@MainActivity, "animalvoice")) browserFailed = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(tr("அதிகாரப்பூர்வ இணையதளம் — எல்லாச் செயலிகளும்", "Official website — all our apps"), style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }

    private fun clearPhoto() {
        photoEpoch++; photoJob?.cancel(); photoJob = null; photos.clear(); photoFailures.clear(); requestedPhoto = ""; imageSearchSpecies = ""; photoLoading = false; confirmPhoto = false; confirmAutoPhotos = false
    }
    private fun loadReferencePhotos(names: List<String>) {
        if (photoLoading) return
        val eligible = names.distinct().filter { ReferencePhoto.validScientific(it) && !it.equals("Homo sapiens", true) && !photos.containsKey(it) }.take(3)
        if (eligible.isEmpty()) return
        val connectivity = getSystemService(ConnectivityManager::class.java)
        if (!V3Download.isOnline(connectivity)) {
            eligible.forEach { photoFailures[it] = true }
            return
        }
        photoLoading = true
        val thisGeneration = generation
        val thisPhotoEpoch = ++photoEpoch
        photoJob = lifecycleScope.launch {
            try {
                for (species in eligible) {
                    requestedPhoto = species
                    photoFailures.remove(species)
                    try {
                        val image = withContext(Dispatchers.IO) { ReferencePhoto.load(species) }
                        if (generation == thisGeneration && photoEpoch == thisPhotoEpoch) photos[species] = image
                    } catch (e: CancellationException) { throw e }
                      catch (_: Exception) { if (generation == thisGeneration && photoEpoch == thisPhotoEpoch) photoFailures[species] = true }
                }
            } finally { if (generation == thisGeneration && photoEpoch == thisPhotoEpoch) photoLoading = false }
        }
    }
    private fun listen() {
        if (busy) return
        clearPhoto()
        samples?.fill(0); samples = null; guesses = null; v3Guesses = null; sampleRate = AudioEngine.RATE; messageText = BilingualText.EMPTY; setV3Notice("", ""); busy = true
        val thisGeneration = ++generation
        val activeEngine = AudioEngine(this); engine = activeEngine
        job = lifecycleScope.launch {
            try {
                val captured = withContext(Dispatchers.IO) { activeEngine.capture() }
                val found = withContext(Dispatchers.Default) { activeEngine.analyze(captured) }
                if (generation == thisGeneration) { samples = captured; guesses = found.guesses } else captured.fill(0)
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) setMessage("மைக்ரோஃபோன் அல்லது ஒலி ஆய்வு செயல்படவில்லை. மீண்டும் முயற்சிக்கவும்.", "Microphone or sound analysis failed. Please try again.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun updateV3Status() {
        v3ModelReady = BirdNetV3.model(this).isFile; v3LabelsReady = BirdNetV3.labels(this).isFile
    }
    private fun importV3(uri: Uri, isModel: Boolean) {
        if (busy) return
        busy = true; messageText = BilingualText.EMPTY; setV3Notice("", ""); v3NoticeOk = false
        val thisGeneration = ++generation
        val isActive = AtomicBoolean(true); importRunning = isActive
        job = lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { BirdNetV3.import(this@MainActivity, uri, isModel) { isActive.get() } }
                updateV3Status()
                if (generation == thisGeneration) showV3Success("கோப்பு சரிபார்க்கப்பட்டு சேர்க்கப்பட்டது.", "File verified and imported.")
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) setV3Notice("சரியான பதிப்பு 3.0 கோப்பைத் தேர்வு செய்யவும். இடத்தையும் சரிபார்க்கவும்.", "Import failed. Select the exact v3.0 file and check storage space.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun downloadV3() {
        if (busy || BirdNetV3.ready(this)) return
        busy = true; downloadingV3 = true; v3NoticeOk = false; setV3Notice("", "")
        downloadBytes = 0L; downloadTotal = null; downloadIsModel = true; downloadWaiting = false; lastProgressUpdate.set(0L)
        val thisGeneration = ++generation
        val active = AtomicBoolean(true); importRunning = active
        val cm = getSystemService(ConnectivityManager::class.java)
        job = lifecycleScope.launch {
            try {
                for (isModel in listOf(true, false)) {
                    val target = if (isModel) BirdNetV3.model(this@MainActivity) else BirdNetV3.labels(this@MainActivity)
                    if (target.isFile) continue
                    downloadIsModel = isModel; downloadBytes = 0L; downloadTotal = null
                    var disconnects = 0
                    while (true) {
                        if (!V3Download.isOnline(cm)) {
                            downloadWaiting = true
                            setV3Notice("இணையத்திற்குக் காத்திருக்கிறது. செயலியைத் திறந்தே வைக்கவும்.", "Waiting for internet. Keep the app open.")
                        }
                        V3Download.awaitNetwork(cm)
                        downloadWaiting = false
                        if (!active.get()) throw CancellationException()
                        if (isModel) setV3Notice("மாடல் பதிவிறக்கப்படுகிறது…", "Downloading model…") else setV3Notice("பெயர் பட்டியல் பதிவிறக்கப்படுகிறது…", "Downloading labels…")
                        try {
                            withContext(Dispatchers.IO) {
                                V3Download.fetch(this@MainActivity, isModel, { active.get() }) { bytes, total ->
                                    val now = SystemClock.elapsedRealtime()
                                    val last = lastProgressUpdate.get()
                                    if ((now - last >= 200 || (total != null && bytes >= total)) && lastProgressUpdate.compareAndSet(last, now)) {
                                        runOnUiThread { if (generation == thisGeneration) { downloadBytes = bytes; downloadTotal = total } }
                                    }
                                }
                            }
                            updateV3Status()
                            break
                        } catch (e: IOException) {
                            if (!active.get()) throw CancellationException()
                            if (V3Download.isOnline(cm) || ++disconnects > 3) throw e
                        }
                    }
                }
                if (generation == thisGeneration) showV3Success("மாடல் சரிபார்க்கப்பட்டு சேர்க்கப்பட்டது.", "Model downloaded and verified.")
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) { v3NoticeOk = false; setV3Notice("பதிவிறக்கம் முடியவில்லை. இணையம் அல்லது சேமிப்பு இடத்தைச் சரிபார்த்து மீண்டும் முயற்சிக்கவும்.", "Download failed. Check internet and storage, then try again.") } }
            finally { if (generation == thisGeneration) { updateV3Status(); downloadingV3 = false; downloadWaiting = false; downloadBytes = 0L; downloadTotal = null; busy = false } }
        }
    }
    private fun listenV3() {
        if (busy || !BirdNetV3.ready(this)) return
        clearPhoto()
        samples?.fill(0); samples = null; guesses = null; v3Guesses = null; messageText = BilingualText.EMPTY; setV3Notice("", ""); busy = true
        sampleRate = BirdNetV3.RATE
        val windows = v3WindowCount
        v3Candidates = emptyList(); v3ResultWindows = windows
        val thisGeneration = ++generation
        val activeEngine = AudioEngine(this); engine = activeEngine
        job = lifecycleScope.launch {
            try {
                val captured = withContext(Dispatchers.IO) { activeEngine.capture(BirdNetV3.RATE, BirdNetV3.SAMPLES * windows) }
                val found = withContext(Dispatchers.Default) {
                    val active = coroutineContext
                    BirdNetV3.analyzeRecording(this@MainActivity, captured) { active.isActive }
                }
                if (generation == thisGeneration) {
                    samples = captured
                    v3Candidates = BirdNetV3.candidates(found.windows)
                    v3Guesses = v3Candidates.map { it.guess }
                    if (autoPhotos) loadReferencePhotos(v3Guesses.orEmpty().map { it.scientific })
                } else captured.fill(0)
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) setMessage("புதிய மாடல் செயல்படவில்லை. மீண்டும் முயற்சிக்கவும்.", "Could not record or analyse with the new model on this device.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun replay() {
        val captured = samples ?: return
        val activeEngine = engine ?: return
        if (busy) return
        val thisGeneration = generation
        messageText = BilingualText.EMPTY; busy = true
        job = lifecycleScope.launch {
            try { withContext(Dispatchers.IO) { activeEngine.replay(captured, sampleRate) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (generation == thisGeneration) setMessage("ஒலியை இயக்க முடியவில்லை.", "Could not play the sample.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun stopAndClear() {
        clearPhoto()
        generation++; importRunning?.set(false); importRunning = null; engine?.stop(); engine = null; job?.cancel(); job = null; samples?.fill(0); samples = null
        guesses = null; v3Guesses = null; confirmReplay = false; confirmDownload = false; downloadingV3 = false; downloadWaiting = false; downloadBytes = 0L; downloadTotal = null; busy = false
    }
    override fun onStop() { stopAndClear(); super.onStop() }
}
