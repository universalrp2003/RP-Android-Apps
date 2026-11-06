package com.oorarivippu.voiceannouncer

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.Locale

internal data class SatelliteRow(val system: String, val id: Int, val used: Boolean, val cn0: Float)
internal data class SatelliteSnapshot(val rows: List<SatelliteRow>, val reported: Int, val atMs: Long)
internal object SatellitePolicy {
    fun fresh(atMs: Long, nowMs: Long) = atMs > 0 && nowMs >= atMs && nowMs - atMs <= 10_000L
    fun constellation(type: Int): String = when(type) {
        1 -> "GPS"; 2 -> "SBAS"; 3 -> "GLONASS"; 4 -> "QZSS"; 5 -> "BeiDou"; 6 -> "Galileo"; 7 -> "NavIC"; else -> "Unknown"
    }
}

/** No new permissions; explicit bounded foreground GPS check; no persisted/uploaded satellite or exit records. */
@SuppressLint("MissingPermission")
@Composable internal fun LocationDiagnostics(state: AppStateSnapshot, tamil: Boolean) {
    fun words(en: String, ta: String) = if(tamil) ta else en
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    var open by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<SatelliteSnapshot?>(null) }
    var status by remember { mutableStateOf("") }
    var sampling by remember { mutableStateOf(false) }
    var sampleFinished by remember { mutableStateOf(false) }
    val probe = remember(context) { GpsDiagnosticProbe(context) { sampling = false; sampleFinished = true } }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val allowed = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    LaunchedEffect(open, resumed, allowed) {
        if (!open || !resumed || !allowed) probe.stop()
    }
    DisposableEffect(probe) { onDispose { probe.stop() } }
    DisposableEffect(open, resumed, allowed, tamil) {
        val manager = context.getSystemService(LocationManager::class.java)
        var active = true
        var registered = false
        var lastUpdate = 0L
        snapshot = null
        status = when {
            !allowed -> words("Precise location permission is not granted", "துல்லிய இருப்பிட அனுமதி இல்லை")
            !state.isTrackingEnabled -> words("GPS check works while walking or stationary; no trip required", "நடக்கும்போதும் நின்றபடியும் GPS சோதிக்கலாம்; பயணம் தேவையில்லை")
            else -> words("Waiting for receiver status · not reported yet", "பெறுநர் தகவலுக்குக் காத்திருக்கிறது · இன்னும் கிடைக்கவில்லை")
        }
        val callback = object : GnssStatus.Callback() {
            override fun onStopped() { if(active) { snapshot = null; status = words("GNSS receiver stopped", "GNSS பெறுநர் நின்றுள்ளது") } }
            override fun onSatelliteStatusChanged(value: GnssStatus) {
                if(!active) return
                val elapsed = SystemClock.elapsedRealtime()
                if(elapsed - lastUpdate < 2_000L) return
                lastUpdate = elapsed
                val rows = (0 until value.satelliteCount.coerceAtMost(256)).map { i ->
                    SatelliteRow(SatellitePolicy.constellation(value.getConstellationType(i)), value.getSvid(i),
                        value.usedInFix(i), value.getCn0DbHz(i))
                }
                snapshot = SatelliteSnapshot(rows, value.satelliteCount, elapsed)
            }
        }
        if(open && resumed && allowed) {
            try {
                registered = manager?.registerGnssStatusCallback(callback, Handler(Looper.getMainLooper())) == true
                if(!registered) status = words("GNSS status unavailable on this device", "இந்தச் சாதனத்தில் GNSS தகவல் கிடைக்கவில்லை")
            } catch(_: Exception) { status = words("GNSS status unavailable", "GNSS தகவல் கிடைக்கவில்லை") }
        }
        onDispose { active = false; if(registered) runCatching { manager?.unregisterGnssStatusCallback(callback) } }
    }
    LaunchedEffect(open, resumed) {
        while(open && resumed) { now = SystemClock.elapsedRealtime(); delay(2_000) }
    }
    val exitReason = remember(open) {
        if(open && Build.VERSION.SDK_INT >= 30) runCatching {
            context.getSystemService(ActivityManager::class.java)?.getHistoricalProcessExitReasons(context.packageName, 0, 1)?.firstOrNull()?.reason
        }.getOrNull() else null
    }
    OutlinedButton(onClick={open = true}, modifier=Modifier.fillMaxWidth()) {
        Text(words("Satellite / app diagnostics", "செயற்கைக்கோள் / செயலி விவரங்கள்"))
    }
    if(open) AlertDialog(
        onDismissRequest={probe.stop(); open = false},
        title={Text(words("Satellite / app diagnostics", "செயற்கைக்கோள் / செயலி விவரங்கள்"))},
        confirmButton={TextButton(onClick={probe.stop(); open = false}) { Text(words("Close diagnostics", "விவரங்களை மூடு")) }},
        text={Column(Modifier.fillMaxWidth().heightIn(max=480.dp).verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(4.dp)) {
                if(sampling) {
                    Text(words("GPS check running · up to 30 seconds", "GPS சோதனை இயங்குகிறது · அதிகபட்சம் 30 வினாடிகள்"))
                    TextButton(onClick={probe.stop()}) { Text(words("Stop GPS check", "GPS சோதனையை நிறுத்து")) }
                } else {
                    Button(enabled=resumed && allowed, onClick={
                        sampleFinished = false
                        sampling = probe.start()
                        if(!sampling) status = words("Cannot start GPS check. Enable GPS and existing precise-location permission.", "GPS சோதனையைத் தொடங்க இயலவில்லை. GPS மற்றும் துல்லிய இருப்பிட அனுமதியைச் சரிபார்க்கவும்.")
                    }) { Text(words("Check GPS now · 30 seconds", "இப்போது GPS சோதி · 30 வினாடிகள்")) }
                    if(sampleFinished) Text(words("GPS check stopped · no ongoing diagnostic location request", "GPS சோதனை நின்றது · இருப்பிடக் கோரிக்கை தொடரவில்லை"))
                }
                val current = snapshot?.takeIf { SatellitePolicy.fresh(it.atMs, now) }
                if(current == null) Text(if(snapshot != null) words("Receiver status is stale · counts unavailable", "பெறுநர் தகவல் பழையது · எண்ணிக்கை கிடைக்கவில்லை") else status)
                else {
                    Text(words("Reported: ${current.reported} · Used in receiver fix: ${current.rows.count { it.used }}", "தெரிவித்தவை: ${current.reported} · பெறுநர் கணிப்பில் பயன்பட்டவை: ${current.rows.count { it.used }}"))
                    if(current.reported > current.rows.size) Text(words("Used count covers the first 256 reported satellites only", "பயன்பாட்டு எண்ணிக்கை முதல் 256 செயற்கைக்கோள்களுக்கு மட்டும்"))
                    Text(current.rows.groupingBy { it.system }.eachCount().entries.joinToString(" · ") { "${it.key}: ${it.value}" })
                    TextButton(onClick={details = !details},contentPadding=PaddingValues(0.dp)) { Text(words("Signal details (up to 24)", "சமிக்ஞை விவரம் (24 வரை)")) }
                    if(details) current.rows.take(24).forEach { row ->
                        val strength = if(row.cn0.isFinite() && row.cn0 >= 0f) String.format(Locale.ROOT, "%.1f dB-Hz", row.cn0) else words("Not reported", "கிடைக்கவில்லை")
                        Text("${row.system} ${row.id} · $strength · " + if(row.used) words("used", "பயன்பட்டது") else words("not used", "பயன்படவில்லை"), style=MaterialTheme.typography.bodySmall)
                    }
                }
                Text(words("Satellites broadcast signals; ‘used’ is Android's latest GNSS receiver fix, not a network connection. A fused location can also use other sources. Android does not expose exact satellite weights for our displayed location/distance. Only Check GPS now starts an extra GPS request, for at most 30 seconds. Closing or backgrounding stops it. No trip or vehicle is required. Details remain on this phone.",
                    "செயற்கைக்கோள்கள் சமிக்ஞை அனுப்புகின்றன; ‘பயன்பட்டது’ என்பது Android GNSS பெறுநரின் கடைசிக் கணிப்பு, இணைய இணைப்பு அல்ல. கலப்பு இருப்பிடம் பிற மூலங்களையும் பயன்படுத்தலாம். காட்டும் இடம்/தொலைவிற்கான துல்லிய செயற்கைக்கோள் பங்கை Android தராது. GPS சோதனை பொத்தானால் மட்டும் அதிகபட்சம் 30 வினாடி கூடுதல் GPS கோரிக்கை தொடங்கும். மூடினாலோ பின்னணிக்குச் சென்றாலோ நின்றுவிடும். வாகனம் தேவையில்லை; விவரங்கள் கைப்பேசியில் மட்டும்."), style=MaterialTheme.typography.bodySmall)
                val reason = when(exitReason) {
                    ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE -> words("app crash", "செயலி முறிவு")
                    ApplicationExitInfo.REASON_ANR -> words("app not responding", "செயலி பதிலளிக்கவில்லை")
                    ApplicationExitInfo.REASON_LOW_MEMORY -> words("system low-memory termination", "நினைவகக் குறைவால் செயலி நிறுத்தம்")
                    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> words("system resource-usage termination", "வளப் பயன்பாட்டால் செயலி நிறுத்தம்")
                    ApplicationExitInfo.REASON_USER_REQUESTED -> words("user-requested app stop (10), not proof of a crash", "பயனர் கோரிய செயலி நிறுத்தம் (10), முறிவுக்கான சான்றல்ல")
                    null -> words("not available", "கிடைக்கவில்லை")
                    else -> words("other / user / system stop ($exitReason)", "பிற / பயனர் / கணினி நிறுத்தம் ($exitReason)")
                }
                Text(words("Last Android-recorded app exit: $reason. This is not a diagnosis of a whole-phone restart.", "Android பதிவு செய்த கடைசி செயலி நிறுத்தம்: $reason. இது கைப்பேசி மறுதொடக்கத்திற்கான காரணச் சான்றல்ல."), style=MaterialTheme.typography.bodySmall)
        }}
    )
}
