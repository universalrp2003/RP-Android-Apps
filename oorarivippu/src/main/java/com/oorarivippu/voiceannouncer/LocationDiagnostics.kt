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

/** No new permissions, no extra location requests, no persisted/uploaded satellite or exit records. */
@SuppressLint("MissingPermission")
@Composable internal fun LocationDiagnostics(state: AppStateSnapshot, tamil: Boolean) {
    fun words(en: String, ta: String) = if(tamil) ta else en
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    var open by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<SatelliteSnapshot?>(null) }
    var status by remember { mutableStateOf("") }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val allowed = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    DisposableEffect(open, resumed, state.isTrackingEnabled, state.isGpsEnabled, allowed) {
        val manager = context.getSystemService(LocationManager::class.java)
        var active = true
        var registered = false
        var lastUpdate = 0L
        snapshot = null
        status = when {
            !allowed -> words("Precise location permission is not granted", "துல்லிய இருப்பிட அனுமதி இல்லை")
            !state.isTrackingEnabled -> words("Start a trip to observe GNSS; no extra location requests are made", "GNSS தகவலுக்குப் பயணத்தைத் தொடங்கவும்; கூடுதல் இருப்பிடக் கோரிக்கை இல்லை")
            !state.isGpsEnabled -> words("GPS is off", "GPS அணைந்துள்ளது")
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
        if(open && resumed && allowed && state.isTrackingEnabled && state.isGpsEnabled) {
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
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal=10.dp, vertical=4.dp), verticalArrangement=Arrangement.spacedBy(2.dp)) {
            TextButton(onClick={open = !open}, contentPadding=PaddingValues(0.dp)) {
                Text(words("Satellite / app diagnostics", "செயற்கைக்கோள் / செயலி விவரங்கள்") + if(open) " −" else " +")
            }
            if(open) {
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
                Text(words("Satellites broadcast signals; ‘used’ is Android's latest GNSS receiver fix, not a network connection. A fused location can also use other sources. Android does not expose exact satellite weights for our displayed location/distance. No new location requests; details remain on this phone.",
                    "செயற்கைக்கோள்கள் சமிக்ஞை அனுப்புகின்றன; ‘பயன்பட்டது’ என்பது Android GNSS பெறுநரின் கடைசிக் கணிப்பு, இணைய இணைப்பு அல்ல. கலப்பு இருப்பிடம் பிற மூலங்களையும் பயன்படுத்தலாம். காட்டும் இடம்/தொலைவிற்கான துல்லிய செயற்கைக்கோள் பங்கை Android தராது. கூடுதல் இருப்பிடக் கோரிக்கை இல்லை; விவரங்கள் இந்தக் கைப்பேசியில் மட்டும்."), style=MaterialTheme.typography.bodySmall)
                val reason = when(exitReason) {
                    ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE -> words("app crash", "செயலி முறிவு")
                    ApplicationExitInfo.REASON_ANR -> words("app not responding", "செயலி பதிலளிக்கவில்லை")
                    ApplicationExitInfo.REASON_LOW_MEMORY -> words("system low-memory termination", "நினைவகக் குறைவால் செயலி நிறுத்தம்")
                    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> words("system resource-usage termination", "வளப் பயன்பாட்டால் செயலி நிறுத்தம்")
                    null -> words("not available", "கிடைக்கவில்லை")
                    else -> words("other / user / system stop ($exitReason)", "பிற / பயனர் / கணினி நிறுத்தம் ($exitReason)")
                }
                Text(words("Last Android-recorded app exit: $reason. This is not a diagnosis of a whole-phone restart.", "Android பதிவு செய்த கடைசி செயலி நிறுத்தம்: $reason. இது கைப்பேசி மறுதொடக்கத்திற்கான காரணச் சான்றல்ல."), style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
