package com.universalrp.weather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.universalrp.updates.CallGuard
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SharedWeatherState(
    val enabled: Boolean = false,
    val gpsTurnedOn: Boolean = false,
    val permissionGranted: Boolean = false,
    val locationMode: LocationResolutionMode = LocationResolutionMode.NEEDS_SAVED_PLACE,
    val activePlaceName: String = "",
    val activePlaceDetails: String = "",
    val savedPlaceName: String = "",
    val savedPlaceDetails: String = "",
    val savedPostalCode: String = "",
    val hasTomorrowKey: Boolean = false,
    val lastCheckedAtMs: Long = 0L,
    val severity: WeatherSeverity = WeatherSeverity.CLEAR_OR_NORMAL,
    val previewSeverity: WeatherSeverity? = null,
    val minutesUntilRain: Int? = null,
    val source: WeatherSourceKind = WeatherSourceKind.OPEN_METEO_DEFAULT,
    val checkingNow: Boolean = false,
    val searchingPlaces: Boolean = false,
    val searchResults: List<WeatherPlaceCandidate> = emptyList(),
    val statusNote: String = "",
) {
    val bannerSeverity: WeatherSeverity
        get() = previewSeverity ?: if (enabled) severity else WeatherSeverity.CLEAR_OR_NORMAL
}

/**
 * Shared encrypted storage, auto-enable-when-GPS-is-on migration, and portable speech + audio-ducking
 * delivery engine used across Live Guard, Ramesh Radio, and Oor Arivippu.
 */
object SharedWeatherRuntime {

    const val TOMORROW_KEYS_URL = "https://app.tomorrow.io/development/keys"
    const val TOMORROW_DOCS_URL = "https://docs.tomorrow.io/reference/welcome"

    const val GUIDE_EN =
        "1. Tap 'Get free Tomorrow.io API key — official site' and sign in (Google or email).\n" +
            "2. Open 'API Management' in your Tomorrow.io dashboard and copy your Default API Key (free tier includes 500 calls/day — plenty for one personal phone).\n" +
            "3. Come back here, paste the key into the Tomorrow.io API key box, tap 'Save key', and tap 'Check now'.\n" +
            "4. Your key is encrypted with Android Keystore on this phone and never shared. If quota ever runs out, the app automatically falls back to the built-in free radar."

    const val GUIDE_TA =
        "1. 'இலவச Tomorrow.io API விசை பெறுக' பொத்தானைத் தட்டி உள்நுழையவும் (Google அல்லது மின்னஞ்சல்).\n" +
            "2. Tomorrow.io பக்கத்தில் 'API Management' பகுதியைத் திறந்து உங்கள் Default API Key-ஐ நகலெடுக்கவும் (இலவச திட்டத்தில் ஒரு நாளைக்கு 500 அழைப்புகள் கிடைக்கும் — ஒரு போனுக்கு இது தாராளமானது).\n" +
            "3. இங்கே திரும்பி வந்து Tomorrow.io API விசைப் பெட்டியில் ஒட்டி, 'விசையைச் சேமி' தட்டி, 'இப்போது சரிபார்' அழுத்தவும்.\n" +
            "4. உங்கள் விசை போனின் Android Keystore-இல் பாதுகாப்பாக மறையாக்கம் செய்யப்படும். விசை இல்லாவிட்டாலும் உள்ளமைந்த இலவச மழை & புயல் ரேடார் தானாக வேலை செய்யும்."

    private const val PREFS_NAME = "rp_shared_weather_watch"
    private const val KEY_ENABLED = "weather_enabled"
    private const val KEY_USER_SET = "weather_user_explicit"
    private const val KEY_GPS_AUTO_MIGRATED = "weather_gps_auto_migrated_v1"
    private const val KEY_SAVED_NAME = "saved_place_name"
    private const val KEY_SAVED_DETAILS = "saved_place_details"
    private const val KEY_SAVED_PIN = "saved_place_pin"
    private const val KEY_SAVED_LAT = "saved_place_lat"
    private const val KEY_SAVED_LON = "saved_place_lon"
    private const val KEY_SECRET_BLOB = "tomorrow_keystore_v1"

    private const val KEY_EPISODE_ACTIVE = "episode_active"
    private const val KEY_HIGHEST_RANK = "episode_highest_rank"
    private const val KEY_DRY_SINCE_MS = "episode_dry_since_ms"
    private const val KEY_LAST_RAIN_MS = "episode_last_rain_ms"
    private const val KEY_LAST_SEVERE_MS = "episode_last_severe_ms"
    private const val KEY_NEXT_CHECK_AFTER_MS = "next_check_after_ms"
    private const val KEY_LAST_CHECKED_MS = "last_checked_ms"
    private const val KEY_LAST_SEVERITY = "last_severity"
    private const val KEY_LAST_PLACE_NAME = "last_place_name"
    private const val KEY_LAST_PLACE_DETAILS = "last_place_details"
    private const val KEY_LAST_MINS_TO_RAIN = "last_mins_to_rain"
    private const val KEY_LAST_SOURCE = "last_source"

    private const val KEYSTORE_ALIAS = "com.universalrp.weather.tomorrow.v1"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Enforces the owner's update rule across Live Guard, Ramesh Radio, and Oor Arivippu:
     * When installing/updating, if phone GPS is ON and the user has NOT manually turned the switch
     * OFF themselves, automatically enable Rain & Cyclone Alerts once!
     */
    @Synchronized
    fun ensureAutoEnabledWhenGpsOn(
        context: Context,
        currentEnabled: Boolean,
        userExplicitlySet: Boolean,
        enableNow: () -> Unit,
    ): Boolean {
        val p = prefs(context)
        val alreadyMigrated = p.getBoolean(KEY_GPS_AUTO_MIGRATED, false)
        if (currentEnabled) {
            if (!alreadyMigrated) p.edit().putBoolean(KEY_GPS_AUTO_MIGRATED, true).apply()
            return true
        }
        if (userExplicitlySet) return false
        val gpsOn = WeatherPlaceResolver.isDeviceLocationTurnedOn(context)
        if (gpsOn) {
            enableNow()
            p.edit().putBoolean(KEY_GPS_AUTO_MIGRATED, true).apply()
            return true
        }
        return false
    }

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)
    fun wasExplicitlySet(context: Context): Boolean = prefs(context).getBoolean(KEY_USER_SET, false)

    fun setEnabled(context: Context, on: Boolean, userInitiated: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, on)
            .apply { if (userInitiated) putBoolean(KEY_USER_SET, true) }
            .apply()
    }

    fun savePlace(context: Context, candidate: WeatherPlaceCandidate) {
        prefs(context).edit()
            .putString(KEY_SAVED_NAME, candidate.name)
            .putString(KEY_SAVED_DETAILS, candidate.details)
            .putString(KEY_SAVED_PIN, candidate.postalCode)
            .putString(KEY_SAVED_LAT, candidate.latitude.toString())
            .putString(KEY_SAVED_LON, candidate.longitude.toString())
            .putLong(KEY_NEXT_CHECK_AFTER_MS, 0L)
            .apply()
    }

    fun clearSavedPlace(context: Context) {
        prefs(context).edit()
            .remove(KEY_SAVED_NAME)
            .remove(KEY_SAVED_DETAILS)
            .remove(KEY_SAVED_PIN)
            .remove(KEY_SAVED_LAT)
            .remove(KEY_SAVED_LON)
            .apply()
    }

    fun savedPlace(context: Context): WeatherPlaceCandidate? {
        val p = prefs(context)
        val name = p.getString(KEY_SAVED_NAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val lat = p.getString(KEY_SAVED_LAT, null)?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        val lon = p.getString(KEY_SAVED_LON, null)?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        return WeatherPlaceCandidate(
            name = name,
            details = p.getString(KEY_SAVED_DETAILS, "").orEmpty(),
            postalCode = p.getString(KEY_SAVED_PIN, "").orEmpty(),
            latitude = lat,
            longitude = lon,
        )
    }

    fun saveTomorrowKey(context: Context, plainKey: String): Boolean {
        val trimmed = plainKey.trim()
        val p = prefs(context)
        if (trimmed.isEmpty()) {
            p.edit().remove(KEY_SECRET_BLOB).apply()
            return true
        }
        val encrypted = runCatching { encryptWithKeystore(trimmed) }.getOrNull() ?: return false
        p.edit().putString(KEY_SECRET_BLOB, encrypted).apply()
        return true
    }

    fun readTomorrowKey(context: Context): String {
        val blob = prefs(context).getString(KEY_SECRET_BLOB, null)?.takeIf { it.isNotBlank() } ?: return ""
        return runCatching { decryptWithKeystore(blob) }.getOrDefault("")
    }

    fun hasTomorrowKey(context: Context): Boolean = readTomorrowKey(context).isNotBlank()

    fun loadMemory(context: Context): WeatherEpisodeMemory {
        val p = prefs(context)
        return WeatherEpisodeMemory(
            episodeActive = p.getBoolean(KEY_EPISODE_ACTIVE, false),
            highestAnnouncedRank = p.getInt(KEY_HIGHEST_RANK, 0),
            drySinceMs = p.getLong(KEY_DRY_SINCE_MS, 0L),
            lastRainAnnouncedAtMs = p.getLong(KEY_LAST_RAIN_MS, 0L),
            lastSevereAnnouncedAtMs = p.getLong(KEY_LAST_SEVERE_MS, 0L),
        )
    }

    fun nextCheckAfterMs(context: Context): Long =
        prefs(context).getLong(KEY_NEXT_CHECK_AFTER_MS, 0L)

    fun saveEvaluation(
        context: Context,
        placeName: String,
        placeDetails: String,
        snapshot: WeatherNowcastSnapshot,
        decision: WeatherAlertDecision,
    ) {
        val mem = decision.updatedMemory
        prefs(context).edit()
            .putBoolean(KEY_EPISODE_ACTIVE, mem.episodeActive)
            .putInt(KEY_HIGHEST_RANK, mem.highestAnnouncedRank)
            .putLong(KEY_DRY_SINCE_MS, mem.drySinceMs)
            .putLong(KEY_LAST_RAIN_MS, mem.lastRainAnnouncedAtMs)
            .putLong(KEY_LAST_SEVERE_MS, mem.lastSevereAnnouncedAtMs)
            .putLong(KEY_NEXT_CHECK_AFTER_MS, snapshot.fetchedAtMs + decision.nextCheckDelayMs)
            .putLong(KEY_LAST_CHECKED_MS, snapshot.fetchedAtMs)
            .putString(KEY_LAST_SEVERITY, decision.severity.name)
            .putString(KEY_LAST_PLACE_NAME, placeName)
            .putString(KEY_LAST_PLACE_DETAILS, placeDetails)
            .putInt(KEY_LAST_MINS_TO_RAIN, decision.minutesUntilRain ?: -1)
            .putString(KEY_LAST_SOURCE, snapshot.source.name)
            .apply()
    }

    fun readState(
        context: Context,
        allowGps: Boolean,
        fallbackCandidate: WeatherPlaceCandidate? = null,
    ): SharedWeatherState {
        val p = prefs(context)
        val saved = savedPlace(context) ?: fallbackCandidate
        val gpsOn = WeatherPlaceResolver.isDeviceLocationTurnedOn(context)
        val permitted = allowGps && WeatherPlaceResolver.hasLocationPermission(context)
        val resolved = if (allowGps) {
            WeatherPlaceResolver.resolveActiveLocation(
                context = context,
                savedName = saved?.name.orEmpty(),
                savedDetails = saved?.details.orEmpty(),
                savedLat = saved?.latitude ?: Double.NaN,
                savedLon = saved?.longitude ?: Double.NaN,
                tamil = true,
            )
        } else if (saved != null) {
            ResolvedWeatherLocation(
                mode = LocationResolutionMode.SAVED_PLACE_FALLBACK,
                gpsTurnedOn = gpsOn,
                permissionGranted = false,
                name = saved.name,
                details = saved.details,
                latitude = saved.latitude,
                longitude = saved.longitude,
            )
        } else {
            ResolvedWeatherLocation(
                mode = LocationResolutionMode.NEEDS_SAVED_PLACE,
                gpsTurnedOn = gpsOn,
                permissionGranted = false,
            )
        }

        val severity = runCatching {
            WeatherSeverity.valueOf(p.getString(KEY_LAST_SEVERITY, WeatherSeverity.CLEAR_OR_NORMAL.name).orEmpty())
        }.getOrDefault(WeatherSeverity.CLEAR_OR_NORMAL)
        val source = runCatching {
            WeatherSourceKind.valueOf(p.getString(KEY_LAST_SOURCE, WeatherSourceKind.OPEN_METEO_DEFAULT.name).orEmpty())
        }.getOrDefault(WeatherSourceKind.OPEN_METEO_DEFAULT)
        val minsRaw = p.getInt(KEY_LAST_MINS_TO_RAIN, -1)

        return SharedWeatherState(
            enabled = isEnabled(context),
            gpsTurnedOn = gpsOn,
            permissionGranted = permitted,
            locationMode = resolved.mode,
            activePlaceName = resolved.name.ifBlank { p.getString(KEY_LAST_PLACE_NAME, "").orEmpty() },
            activePlaceDetails = resolved.details.ifBlank { p.getString(KEY_LAST_PLACE_DETAILS, "").orEmpty() },
            savedPlaceName = savedPlace(context)?.name.orEmpty(),
            savedPlaceDetails = savedPlace(context)?.details.orEmpty(),
            savedPostalCode = savedPlace(context)?.postalCode.orEmpty(),
            hasTomorrowKey = hasTomorrowKey(context),
            lastCheckedAtMs = p.getLong(KEY_LAST_CHECKED_MS, 0L),
            severity = severity,
            minutesUntilRain = minsRaw.takeIf { it >= 0 },
            source = source,
        )
    }

    /** True between 10:00 PM and 7:00 AM local clock. */
    fun inDefaultQuietHours(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Boolean =
        hourOfDay >= 22 || hourOfDay < 7

    /**
     * Speaks a bilingual weather alert while enforcing:
     *  1. `CallGuard` (never interrupts active calls),
     *  2. Quiet Hours (`22:00..07:00` silent for normal rain, 24×7 NO-SKIP for heavy rain / cyclone),
     *  3. Optional `duckAudio(true -> false)` callback so Ramesh Radio smoothly lowers FM/song
     *     playback volume during the weather bulletin and restores full volume right afterwards!
     */
    suspend fun speakAlertWithOptionalDuck(
        context: Context,
        english: String,
        tamilText: String,
        preferTamil: Boolean,
        manual: Boolean,
        bypassQuietHours: Boolean,
        duckAudio: ((Boolean) -> Unit)? = null,
    ): Boolean {
        val app = context.applicationContext
        if (CallGuard.inCall(app)) return false
        if (!manual && !bypassQuietHours && inDefaultQuietHours()) return false

        val mainHandler = Handler(Looper.getMainLooper())
        val finished = CompletableDeferred<Boolean>()
        var engineRef: TextToSpeech? = null

        duckAudio?.let { mainHandler.post { runCatching { it(true) } } }
        return try {
            mainHandler.post {
                engineRef = TextToSpeech(app) { status ->
                    val tts = engineRef
                    if (status != TextToSpeech.SUCCESS || tts == null) {
                        finished.complete(false)
                        return@TextToSpeech
                    }
                    val taLocale = Locale("ta", "IN")
                    val canTamil = preferTamil && tts.isLanguageAvailable(taLocale) >= TextToSpeech.LANG_AVAILABLE
                    tts.language = if (canTamil) taLocale else Locale.US
                    val chosenText = if (canTamil) tamilText else english
                    val utteranceId = "weather_" + UUID.randomUUID().toString()
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) = Unit
                        override fun onDone(id: String?) {
                            finished.complete(true)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            finished.complete(false)
                        }
                        override fun onError(id: String?, errorCode: Int) {
                            finished.complete(false)
                        }
                    })
                    val params = Bundle().apply {
                        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                    }
                    val code = tts.speak(chosenText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                    if (code != TextToSpeech.SUCCESS) {
                        finished.complete(false)
                    }
                }
            }
            withTimeoutOrNull(35_000L) { finished.await() } ?: false
        } finally {
            mainHandler.post {
                runCatching { engineRef?.stop(); engineRef?.shutdown() }
                duckAudio?.let { runCatching { it(false) } }
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission", "NotificationPermission")
    fun postWeatherNotification(
        context: Context,
        smallIconRes: Int,
        decision: WeatherAlertDecision,
        tamil: Boolean,
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val severe = decision.bypassQuietHours
        val channelId = if (severe) "rp_weather_severe" else "rp_weather_rain"
        val channelName = if (severe) {
            if (tamil) "மிகக் கனமழை & புயல் எச்சரிக்கை" else "Severe rain & cyclone alerts"
        } else {
            if (tamil) "30 நிமிட மழை முன்னறிவிப்பு" else "30-minute rain heads-up"
        }
        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName,
                if (severe) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = launchIntent?.let {
            android.app.PendingIntent.getActivity(
                context,
                42_580,
                it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val title = when (decision.severity) {
            WeatherSeverity.CYCLONE_SEVERE -> if (tamil) "அவசர புயல் எச்சரிக்கை" else "Urgent Cyclone / Storm Alert"
            WeatherSeverity.VERY_HEAVY_RAIN -> if (tamil) "மிகக் கனமழை எச்சரிக்கை" else "Very Heavy Rain Warning"
            else -> if (tamil) "இன்னும் சற்று நேரத்தில் மழை" else "Rain Approaching Soon"
        }
        val body = if (tamil) decision.tamilSpeech else decision.englishSpeech
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (severe) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { if (pi != null) setContentIntent(pi) }
            .build()
        runCatching { nm.notify(42_580, notif) }
    }

    private fun getOrCreateKeystoreSecret(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(
            android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        gen.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun encryptWithKeystore(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreSecret())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
        val packed = ByteBuffer.allocate(4 + iv.size + ciphertext.size)
            .putInt(iv.size)
            .put(iv)
            .put(ciphertext)
            .array()
        return Base64.getEncoder().encodeToString(packed)
    }

    private fun decryptWithKeystore(encoded: String): String {
        val packed = Base64.getDecoder().decode(encoded)
        val buf = ByteBuffer.wrap(packed)
        val ivLen = buf.int
        require(ivLen in 12..16 && buf.remaining() > ivLen)
        val iv = ByteArray(ivLen).also { buf.get(it) }
        val ciphertext = ByteArray(buf.remaining()).also { buf.get(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreSecret(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }
}

/**
 * Shared Tomorrow.io API key official links + step-by-step popup & voice guide (same UX pattern
 * as Live Guard's AI & VirusTotal API key setup guides!).
 */
@Composable
fun TomorrowApiKeyGuideLinks(
    tamil: Boolean,
    accentColor: Color = Color(0xFF38BDF8),
    onReadGuideAloud: (String) -> Unit,
    onStopVoice: () -> Unit,
) {
    val context = LocalContext.current
    var showGuideDialog by remember { mutableStateOf(false) }

    val getKeyLabel = if (tamil) {
        "இலவச Tomorrow.io API விசை பெறுக — அதிகாரப்பூர்வ தளம்"
    } else {
        "Get free Tomorrow.io API key — official site"
    }
    val docsLabel = if (tamil) {
        "Tomorrow.io API விசை வழிகாட்டி பக்கம்"
    } else {
        "Tomorrow.io API key help & docs"
    }
    val stepGuideLabel = if (tamil) {
        "படிப்படியான வழிகாட்டி & குரல் விளக்கம்"
    } else {
        "Step-by-step setup & voice guide"
    }
    val dialogTitle = if (tamil) {
        "Tomorrow.io இலவச API விசை பெறும் முறை"
    } else {
        "How to get a free Tomorrow.io API key"
    }
    val dialogScript = if (tamil) SharedWeatherRuntime.GUIDE_TA else SharedWeatherRuntime.GUIDE_EN
    val readAloudLabel = if (tamil) "வழிகாட்டியை வாசி" else "Read guide aloud"
    val stopVoiceLabel = if (tamil) "குரலை நிறுத்து" else "Stop voice"
    val closeLabel = if (tamil) "மூடு" else "Close"

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TextButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(SharedWeatherRuntime.TOMORROW_KEYS_URL))
                    )
                }
            },
        ) {
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = getKeyLabel, color = accentColor, style = MaterialTheme.typography.labelLarge)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(SharedWeatherRuntime.TOMORROW_DOCS_URL))
                        )
                    }
                },
            ) {
                Text(text = docsLabel, color = accentColor, style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = { showGuideDialog = true }) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(text = stepGuideLabel, color = accentColor, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = { Text(text = dialogTitle) },
            text = { Text(text = dialogScript, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(onClick = { onReadGuideAloud(dialogScript) }) {
                    Text(text = readAloudLabel)
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = onStopVoice) { Text(text = stopVoiceLabel) }
                    TextButton(onClick = { showGuideDialog = false }) { Text(text = closeLabel) }
                }
            },
        )
    }
}
