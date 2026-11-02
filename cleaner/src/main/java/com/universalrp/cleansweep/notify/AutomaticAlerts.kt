package com.universalrp.cleansweep.notify

import android.content.*
import android.net.*
import android.os.Handler
import android.os.Looper
import com.universalrp.cleansweep.data.BatteryReader
import com.universalrp.cleansweep.voice.Announcer
import com.universalrp.cleansweep.voice.VoiceDeliveryPolicy
import com.universalrp.cleansweep.voice.VoiceObserverStatus
import kotlinx.coroutines.*

/** Immediate listeners only while the app is visible or its EXISTING charging/background service is running. */
object AutomaticAlerts {
    @Volatile private var latestBattery: com.universalrp.cleansweep.data.BatteryReading? = null
    private val main = Handler(Looper.getMainLooper())
    private val owners = hashSetOf<String>()
    @Volatile private var registered = false
    private var context: Context? = null
    private var connectivity: ConnectivityManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var batteryJob: Job? = null
    private val observerState = kotlinx.coroutines.flow.MutableStateFlow(VoiceObserverStatus())
    val observers: kotlinx.coroutines.flow.StateFlow<VoiceObserverStatus> = observerState
    private val powerReceiver = SystemEventWatcher()
    private val chargingReceiver = ChargingWatcher()
    private var knownTransport: String? = null
    @Volatile private var generation = 0
    private val networkRefresh = Runnable { context?.let { observeNetwork(it) } }
    // Poll only while an existing UI/service owner is active. Near-zero current may not
    // cause another BATTERY_CHANGED broadcast, so the 60-second check must not rely on one.
    private val batteryRefresh = object : Runnable {
        override fun run() {
            if (!registered) return
            context?.let { sampleBattery(it) }
            main.postDelayed(this, 10_000L)
        }
    }
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { changed() }
        override fun onAvailable(network: Network) { changed() }
        override fun onLost(network: Network) { changed() }
        private fun changed() { main.post { if (registered) { main.removeCallbacks(networkRefresh); main.postDelayed(networkRefresh, 900) } } }
    }
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { context?.applicationContext?.let { sampleBattery(it) } }
    }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_OFF) return
            val app = context?.applicationContext ?: return
            com.universalrp.cleansweep.voice.NewsAutoSchedule.requestDueCheck(app, delayMs = 15_000L)
        }
    }

    fun start(context: Context, owner: String) { main.post {
        owners.add(owner)
        if (registered) { observerState.value = observerState.value.copy(owners = owners.size); return@post }
        this.context = context.applicationContext; registered = true; generation++
        connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        knownTransport = transport(context).takeIf { it in setOf("wifi", "mobile") }
        knownTransport?.let { Announcer.prefs(context).edit().putString("last_transport", it).apply() }
        val network = connectivity != null && runCatching { connectivity?.registerDefaultNetworkCallback(callback) }.isSuccess
        val battery = runCatching { context.applicationContext.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.isSuccess
        val powerFilter = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) }
        // Android8+ manifest implicit-power delivery can be restricted/deferred. Explicit runtime owners are required.
        val power = runCatching { context.applicationContext.registerReceiver(powerReceiver, powerFilter) }.isSuccess
        runCatching { context.applicationContext.registerReceiver(chargingReceiver, powerFilter) }
        runCatching { context.applicationContext.registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF)) }
        observerState.value = VoiceObserverStatus(owners.size, battery, network, power)
        main.postDelayed(batteryRefresh, 10_000L)
    } }

    fun stop(owner: String) { main.post {
        owners.remove(owner)
        if (owners.isNotEmpty() || !registered) { observerState.value = observerState.value.copy(owners = owners.size); return@post }
        registered = false; generation++
        main.removeCallbacks(batteryRefresh)
        main.removeCallbacks(networkRefresh)
        runCatching { connectivity?.unregisterNetworkCallback(callback) }
        runCatching { context?.unregisterReceiver(batteryReceiver) }
        runCatching { context?.unregisterReceiver(powerReceiver) }
        runCatching { context?.unregisterReceiver(chargingReceiver) }
        runCatching { context?.unregisterReceiver(screenReceiver) }
        observerState.value = VoiceObserverStatus()
        batteryJob?.cancel(); batteryJob = null; context = null; knownTransport = null
    } }

    private fun transport(context: Context): String {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "none"
        val caps = runCatching { manager.activeNetwork?.let { manager.getNetworkCapabilities(it) } }.getOrNull() ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "mobile"
            else -> "none"
        }
    }

    private fun observeNetwork(context: Context) {
        val current = transport(context)
        val before = knownTransport
        if (current !in setOf("wifi", "mobile")) return
        knownTransport = current
        Announcer.prefs(context).edit().putString("last_transport", current).apply()
        if (!VoiceDeliveryPolicy.changedTransport(before, current)) return // Initial state/VPN/unknown is not an invented transition.
        if (current == "mobile") Announcer.speakIfStillRelevant(context, "You are now on mobile data; this uses your data plan.",
            "இப்போது மொபைல் டேட்டாவில் உள்ளீர்கள். இது உங்கள் தரவுத் திட்டத்தைப் பயன்படுத்தும்.", Announcer.Event.NETWORK_CHANGE, stillRelevant = { transport(context) == current })
        else Announcer.speakIfStillRelevant(context, "You are back on Wi-Fi.", "நீங்கள் மீண்டும் வைஃபையில் உள்ளீர்கள்.", Announcer.Event.NETWORK_CHANGE, stillRelevant = { transport(context) == current })
    }

    private fun sampleBattery(context: Context) {
        if (batteryJob?.isActive == true) return
        val mine = generation
        batteryJob = scope.launch {
            val battery = runCatching { BatteryReader.read(context) }.getOrNull() ?: return@launch
            latestBattery = battery
            if (mine != generation || !registered || !Announcer.enabled(context)) return@launch
            val prefs = Announcer.prefs(context); val now = System.currentTimeMillis()
            fun alert(key: String, interval: Long, event: Announcer.Event, english: String, tamil: String) {
                if (VoiceDeliveryPolicy.cooldownReady(now, prefs.getLong(key, 0), interval))
                    Announcer.speak(context, english, tamil, event, onStarted = { prefs.edit().putLong(key, System.currentTimeMillis()).apply() })
            }
            if (com.universalrp.cleansweep.data.BatteryAlertPolicy.low(battery.percent, battery.powerConnected, Announcer.lowPercent(context)))
                alert(Announcer.KEY_LAST_LOW, 6 * 60 * 60 * 1000L, Announcer.Event.BATTERY_LOW,
                    "Battery low: ${battery.percent} percent. Please recharge.", "பேட்டரி குறைவு: ${battery.percent} சதவீதம். சார்ஜ் செய்யவும்.")
            // A percentage-only notice is not a charge-termination claim. Deduplicate per connection,
            // and mark only after playback starts (quiet/call/offline rejection must not consume it).
            if (!battery.powerConnected) prefs.edit().remove("charge_283_spoken").remove("charge_283_rank").apply()
            if (battery.powerConnected && battery.percent == 100) {
                val kind = when {
                    com.universalrp.cleansweep.data.ChargeAnnouncementSettings.advanced(context) && battery.fullChargeConfirmed -> "confirmed"
                    com.universalrp.cleansweep.data.ChargeAnnouncementSettings.advanced(context) && battery.chargeEstimate.ready -> "estimated"
                    com.universalrp.cleansweep.data.ChargeAnnouncementSettings.percentNotice(context) -> "percentage"
                    else -> ""
                }
                val token = prefs.getString("voice_connection_token", "observed").orEmpty()
                val key = "$token:$kind"
                val rank = when(kind) { "confirmed" -> 3; "estimated" -> 2; "percentage" -> 1; else -> 0 }
                val previousRank = com.universalrp.cleansweep.data.ChargeNoticeRankPolicy.previous(token,
                    prefs.getString("charge_283_spoken", "").orEmpty(),prefs.getInt("charge_283_rank",0))
                if (rank > previousRank) {
                    val en = when (kind) {
                        "confirmed" -> "Battery reports full with sustained near-zero measured battery current. Unplug when convenient."
                        "estimated" -> "Battery is 100 percent with sustained low battery intake. Charge completion is estimated, not verified."
                        else -> "Battery reached 100 percent."
                    }
                    val ta = when (kind) {
                        "confirmed" -> "பேட்டரி முழு நிலையைக் காட்டுகிறது; அளந்த மின்னோட்டம் தொடர்ந்து பூஜ்ஜியத்திற்கு அருகில் உள்ளது. வசதியாக சார்ஜரை அகற்றலாம்."
                        "estimated" -> "பேட்டரி நூறு சதவீதம். குறைந்த மின்னோட்டம் நீடிக்கிறது. சார்ஜ் நிறைவு ஒரு மதிப்பீடு மட்டுமே; உறுதிப்படுத்தப்படவில்லை."
                        else -> "பேட்டரி நூறு சதவீதத்தை எட்டியது."
                    }
                    Announcer.speakIfStillRelevant(context, en, ta, Announcer.Event.BATTERY_FULL,
                        stillRelevant = {
                            val current = latestBattery
                            prefs.getString("voice_connection_token", "observed").orEmpty()==token && current?.powerConnected==true && current.percent==100 &&
                                when(kind) { "confirmed" -> com.universalrp.cleansweep.data.ChargeAnnouncementSettings.advanced(context) && current.fullChargeConfirmed; "estimated" -> com.universalrp.cleansweep.data.ChargeAnnouncementSettings.advanced(context) && current.chargeEstimate.ready; else -> com.universalrp.cleansweep.data.ChargeAnnouncementSettings.percentNotice(context) }
                        },
                        onStarted = { if(prefs.getString("voice_connection_token", "observed").orEmpty()==token) {
                            val next=com.universalrp.cleansweep.data.ChargeNoticeRankPolicy.started(token,
                                prefs.getString("charge_283_spoken", "").orEmpty(),prefs.getInt("charge_283_rank",0),rank)
                            prefs.edit().putString("charge_283_spoken", key).putInt("charge_283_rank",next).apply()
                        } })
                }
            }
            if ((battery.temperatureC ?: -100f) >= Announcer.HOT_BATTERY_C)
                alert(Announcer.KEY_LAST_HOT, 3 * 60 * 60 * 1000L, Announcer.Event.OVERHEAT,
                    "The battery is running hot. Please review charging and phone use.", "பேட்டரி அதிக வெப்பமாக உள்ளது. சார்ஜ் மற்றும் போன் பயன்பாட்டை சரிபார்க்கவும்.")
        }
    }
}
