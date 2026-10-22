package com.universalrp.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.floor

enum class NeuralVoiceGender {
    MALE,
    FEMALE,
}

data class CuratedNeuralVoiceProfile(
    val id: String,
    val gender: NeuralVoiceGender,
    val titleEn: String,
    val titleTa: String,
    val tamilNeuralId: String,
    val englishNeuralId: String,
    val pitchTa: String = "+0Hz",
    val pitchEn: String = "+0Hz",
)

/**
 * Shared Studio Ultra-HD Online Neural Voice synthesis & playback engine for RP Ideas apps:
 * - 10 curated Studio Ultra-HD voices (5 Male HD + 5 Female HD across all 4 official world Tamil
 *   studios ta-IN / ta-LK / ta-SG / ta-MY + 2026 flagship Multilingual & Expressive English studios)
 * - Automatic Daily Male <-> Female voice rotation ON by default (Day 1 Male -> Day 2 Female ...)
 * - Local LRU MP3 disk cache for instant 0ms replay of repeated alerts & town announcements
 * - Fast bounded timeouts so offline / no-signal travel immediately falls back to System TTS.
 */
object SharedNeuralVoiceEngine {

    val CURATED_VOICES: List<CuratedNeuralVoiceProfile> = listOf(
        // 5 High-Definition Studio Male Neural Voices
        CuratedNeuralVoiceProfile(
            id = "online_male_1",
            gender = NeuralVoiceGender.MALE,
            titleEn = "👨 Male HD 1 · Valluvar / Prabhat (India Studio Ultra-HD)",
            titleTa = "👨 ஆண் குரல் 1 · வள்ளுவர் / பிரபாத் (இந்திய Ultra-HD)",
            tamilNeuralId = "ta-IN-ValluvarNeural",
            englishNeuralId = "en-IN-PrabhatNeural",
        ),
        CuratedNeuralVoiceProfile(
            id = "online_male_2",
            gender = NeuralVoiceGender.MALE,
            titleEn = "👨 Male HD 2 · Kumar / Andrew HD (Deep Cinema Ultra-HD)",
            titleTa = "👨 ஆண் குரல் 2 · குமார் / ஆண்ட்ரூ (ஆழ்ந்த Ultra-HD)",
            tamilNeuralId = "ta-LK-KumarNeural",
            englishNeuralId = "en-US-AndrewMultilingualNeural",
        ),
        CuratedNeuralVoiceProfile(
            id = "online_male_3",
            gender = NeuralVoiceGender.MALE,
            titleEn = "👨 Male HD 3 · Anbu / Brian HD (Warm Natural Ultra-HD)",
            titleTa = "👨 ஆண் குரல் 3 · அன்பு / பிரையன் (மென்மையான Ultra-HD)",
            tamilNeuralId = "ta-SG-AnbuNeural",
            englishNeuralId = "en-US-BrianMultilingualNeural",
        ),
        CuratedNeuralVoiceProfile(
            id = "online_male_4",
            gender = NeuralVoiceGender.MALE,
            titleEn = "👨 Male HD 4 · Surya / Ryan HD (Crisp Broadcast HD)",
            titleTa = "👨 ஆண் குரல் 4 · சூர்யா / ரயான் (தெளிவான செய்தி HD)",
            tamilNeuralId = "ta-MY-SuryaNeural",
            englishNeuralId = "en-GB-RyanNeural",
        ),
        CuratedNeuralVoiceProfile(
            id = "online_male_5",
            gender = NeuralVoiceGender.MALE,
            titleEn = "👨 Male HD 5 · Valluvar Bass / William HD (Rich FM Studio)",
            titleTa = "👨 ஆண் குரல் 5 · வள்ளுவர் பேஸ் / வில்லியம் (இனிய FM HD)",
            tamilNeuralId = "ta-IN-ValluvarNeural",
            englishNeuralId = "en-AU-WilliamNeural",
            pitchTa = "-4Hz",
            pitchEn = "+0Hz",
        ),
        // 5 High-Definition Studio Female Neural Voices
        CuratedNeuralVoiceProfile(
            id = "online_female_1",
            gender = NeuralVoiceGender.FEMALE,
            titleEn = "👩 Female HD 1 · Pallavi / Neerja Expressive (India Ultra-HD)",
            titleTa = "👩 பெண் குரல் 1 · பல்லவி / நீரஜா (இந்திய Ultra-HD)",
            tamilNeuralId = "ta-IN-PallaviNeural",
            englishNeuralId = "en-IN-NeerjaExpressiveNeural",
        ),
        CuratedNeuralVoiceProfile(
            id = "online_female_2",
            gender = NeuralVoiceGender.FEMALE,
            titleEn = "👩 Female HD 2 · Kani / Emma HD (Sweet Melodic Ultra-HD)",
            titleTa = "👩 பெண் குரல் 2 · கனி / எம்மா (இனிய மென்மையான Ultra-HD)",
            tamilNeuralId = "ta-MY-KaniNeural",
            englishNeuralId = "en-US-EmmaMultilingualNeural",
        ),
        CuratedNeuralVoiceProfile(
            id = "online_female_3",
            gender = NeuralVoiceGender.FEMALE,
            titleEn = "👩 Female HD 3 · Venba / Ava HD (Warm Natural Ultra-HD)",
            titleTa = "👩 பெண் குரல் 3 · வெண்பா / அவா (இயல்பான Ultra-HD)",
            tamilNeuralId = "ta-SG-VenbaNeural",
            englishNeuralId = "en-US-AvaMultilingualNeural",
        ),
        CuratedNeuralVoiceProfile(
            id = "online_female_4",
            gender = NeuralVoiceGender.FEMALE,
            titleEn = "👩 Female HD 4 · Saranya / Sonia HD (Crisp Broadcast HD)",
            titleTa = "👩 பெண் குரல் 4 · சரண்யா / சோனியா (தெளிவான செய்தி HD)",
            tamilNeuralId = "ta-LK-SaranyaNeural",
            englishNeuralId = "en-GB-SoniaNeural",
        ),
        CuratedNeuralVoiceProfile(
            id = "online_female_5",
            gender = NeuralVoiceGender.FEMALE,
            titleEn = "👩 Female HD 5 · Pallavi Bright / Natasha HD (Clear Travel HD)",
            titleTa = "👩 பெண் குரல் 5 · பல்லவி தெளிவு / நடாஷா (பயண Ultra-HD)",
            tamilNeuralId = "ta-IN-PallaviNeural",
            englishNeuralId = "en-AU-NatashaNeural",
            pitchTa = "+4Hz",
            pitchEn = "+0Hz",
        ),
    )

    val MALE_VOICES: List<CuratedNeuralVoiceProfile> =
        CURATED_VOICES.filter { it.gender == NeuralVoiceGender.MALE }

    val FEMALE_VOICES: List<CuratedNeuralVoiceProfile> =
        CURATED_VOICES.filter { it.gender == NeuralVoiceGender.FEMALE }

    /**
     * Automatically alternates Male <-> Female every single calendar day:
     * - Even epochDay -> Male HD voice, cycling cleanly across all 5 Male Ultra-HD voices
     * - Odd epochDay  -> Female HD voice, cycling cleanly across all 5 Female Ultra-HD voices
     */
    fun scheduledVoiceForDate(date: LocalDate = LocalDate.now()): CuratedNeuralVoiceProfile {
        val epochDay = Math.floorMod(date.toEpochDay(), 60_000L)
        val isFemaleDay = (epochDay % 2L) == 1L
        val variantIdx = ((epochDay / 2L) % MALE_VOICES.size.toLong()).toInt()
        return if (isFemaleDay) {
            FEMALE_VOICES[variantIdx]
        } else {
            MALE_VOICES[variantIdx]
        }
    }

    /** Returns today's variant for an explicitly requested gender (e.g. Assistant Slot B swap). */
    fun scheduledGenderVoiceForDate(
        gender: NeuralVoiceGender,
        date: LocalDate = LocalDate.now(),
    ): CuratedNeuralVoiceProfile {
        val epochDay = Math.floorMod(date.toEpochDay(), 60_000L)
        val pool = if (gender == NeuralVoiceGender.FEMALE) FEMALE_VOICES else MALE_VOICES
        val variantIdx = ((epochDay / 2L) % pool.size.toLong()).toInt()
        return pool[variantIdx]
    }

    /**
     * Resolves active online voice profile:
     * - When `autoDailyGenderSwap` is true (default ON), automatically rotates Male <-> Female daily!
     * - When `autoDailyGenderSwap` is false and `preferredVoiceId` names a curated voice, locks it.
     */
    fun resolveActiveProfile(
        autoDailyGenderSwap: Boolean,
        preferredVoiceId: String,
        today: LocalDate = LocalDate.now(),
    ): CuratedNeuralVoiceProfile {
        if (!autoDailyGenderSwap) {
            CURATED_VOICES.firstOrNull { it.id == preferredVoiceId }?.let { return it }
        }
        return scheduledVoiceForDate(today)
    }

    @SuppressLint("MissingPermission")
    fun hasInternet(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@runCatching false
        val active = cm.activeNetwork ?: return@runCatching false
        val caps = cm.getNetworkCapabilities(active) ?: return@runCatching false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)

    private val SHORT_VOICE_REGEX = Regex("^([a-z]{2,})-([A-Z]{2,})-(.+Neural)$")

    /** Matches edge-tts 7.3.1 canonical server voice name formatting. */
    fun formatServerVoiceName(voice: String): String {
        val match = SHORT_VOICE_REGEX.matchEntire(voice.trim()) ?: return voice.trim()
        val lang = match.groupValues[1]
        var region = match.groupValues[2]
        var name = match.groupValues[3]
        val dashIdx = name.indexOf('-')
        if (dashIdx >= 0) {
            region = "$region-${name.substring(0, dashIdx)}"
            name = name.substring(dashIdx + 1)
        }
        return "Microsoft Server Speech Text to Speech Voice ($lang-$region, $name)"
    }

    fun rateFromPercent(speechRatePercent: Int): String {
        val delta = (speechRatePercent - 100).coerceIn(-35, 45)
        return if (delta >= 0) "+${delta}%" else "${delta}%"
    }

    private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
    private const val CHROMIUM_MAJOR_VERSION = "143"
    private const val SEC_MS_GEC_VERSION = "2-$CHROMIUM_FULL_VERSION"
    private const val WSS_BASE =
        "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=$TRUSTED_CLIENT_TOKEN"
    private const val WIN_EPOCH_SECONDS = 11_644_473_600.0
    private const val U32_MASK = 0xFFFFFFFFL

    @Volatile
    private var clockSkewSeconds: Double = 0.0

    private val secureRandom = SecureRandom()

    private val wsClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(4200, TimeUnit.MILLISECONDS)
            .writeTimeout(2500, TimeUnit.MILLISECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    }

    private val JS_UTC_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US)

    private fun jsUtcDateString(): String =
        ZonedDateTime.now(ZoneOffset.UTC).format(JS_UTC_FORMATTER)

    private fun randomHexLower(bytesCount: Int): String {
        val bytes = ByteArray(bytesCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sanitizeSsmlText(raw: String): String {
        val sb = StringBuilder(raw.length + 16)
        for (ch in raw) {
            val code = ch.code
            val clean = if (code in 0..8 || code == 11 || code == 12 || code in 14..31) ' ' else ch
            when (clean) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '\'' -> sb.append("&apos;")
                '"' -> sb.append("&quot;")
                else -> sb.append(clean)
            }
        }
        return sb.toString().trim()
    }

    /**
     * Synthesizes a curated voice profile to cached 24kHz MP3, automatically falling back to
     * primary India Neural voice if a regional edge model transiently hiccups.
     */
    fun synthesizeProfileToFileOrNull(
        cacheDir: File?,
        text: String,
        profile: CuratedNeuralVoiceProfile,
        useTamil: Boolean,
        ratePercent: Int = 98,
        timeoutMs: Long = 3800L,
    ): File? {
        val primaryVoice = if (useTamil) profile.tamilNeuralId else profile.englishNeuralId
        val primaryPitch = if (useTamil) profile.pitchTa else profile.pitchEn
        val rate = rateFromPercent(ratePercent)

        synthesizeToFileOrNull(
            cacheDir = cacheDir,
            text = text,
            neuralVoiceName = primaryVoice,
            rate = rate,
            pitch = primaryPitch,
            timeoutMs = timeoutMs,
        )?.let { return it }

        val fallbackVoice = when {
            useTamil && profile.gender == NeuralVoiceGender.FEMALE -> "ta-IN-PallaviNeural"
            useTamil -> "ta-IN-ValluvarNeural"
            profile.gender == NeuralVoiceGender.FEMALE -> "en-IN-NeerjaNeural"
            else -> "en-IN-PrabhatNeural"
        }
        if (fallbackVoice != primaryVoice) {
            return synthesizeToFileOrNull(
                cacheDir = cacheDir,
                text = text,
                neuralVoiceName = fallbackVoice,
                rate = rate,
                pitch = primaryPitch,
                timeoutMs = 2800L,
            )
        }
        return null
    }

    fun synthesizeToFileOrNull(
        cacheDir: File?,
        text: String,
        neuralVoiceName: String,
        rate: String = "+0%",
        pitch: String = "+0Hz",
        timeoutMs: Long = 3800L,
    ): File? {
        val trimmed = text.trim().take(1200)
        if (trimmed.isEmpty()) return null

        val cacheKey = sha256Hex("v2|$neuralVoiceName|$rate|$pitch|$trimmed")
        val targetDir = cacheDir?.let { File(it, "rp_neural_hd_tts").apply { mkdirs() } }
        val cachedFile = targetDir?.let { File(it, "$cacheKey.mp3") }
        if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 256L) {
            return cachedFile
        }

        val mp3Bytes = synthesizeRawMp3(
            text = trimmed,
            voiceName = neuralVoiceName,
            rate = rate,
            pitch = pitch,
            timeoutMs = timeoutMs,
        ) ?: return null

        if (mp3Bytes.size < 256) return null

        return if (targetDir != null && cachedFile != null) {
            runCatching {
                pruneCacheIfNeeded(targetDir)
                val tmp = File(targetDir, "$cacheKey.tmp")
                tmp.writeBytes(mp3Bytes)
                tmp.renameTo(cachedFile)
                if (cachedFile.exists() && cachedFile.length() > 256L) cachedFile else tmp
            }.getOrNull()
        } else {
            val temp = File.createTempFile("rp_neural_hd_", ".mp3")
            temp.writeBytes(mp3Bytes)
            temp
        }
    }

    /** Plays a synthesized MP3 file synchronously with custom volume and instant cancellation check. */
    fun playMp3FileBlocking(
        file: File,
        volumeScalar: Float,
        audioAttributes: AudioAttributes,
        onStarted: (() -> Unit)? = null,
        shouldStop: () -> Boolean = { false },
        activePlayerSink: ((MediaPlayer?) -> Unit)? = null,
        maxDurationMs: Long = 60_000L,
    ): Boolean {
        val done = CountDownLatch(1)
        val mp = MediaPlayer()
        var succeeded = false
        try {
            activePlayerSink?.invoke(mp)
            mp.setAudioAttributes(audioAttributes)
            FileInputStream(file).use { fis ->
                mp.setDataSource(fis.fd)
            }
            val clampedVol = volumeScalar.coerceIn(0f, 1f)
            mp.setVolume(clampedVol, clampedVol)
            mp.setOnCompletionListener {
                succeeded = true
                done.countDown()
            }
            mp.setOnErrorListener { _, _, _ ->
                done.countDown()
                true
            }
            mp.prepare()
            if (shouldStop()) return false
            mp.start()
            runCatching { onStarted?.invoke() }
            var waited = 0L
            while (waited < maxDurationMs && !shouldStop()) {
                if (done.await(150L, TimeUnit.MILLISECONDS)) break
                waited += 150L
            }
            return succeeded && !shouldStop()
        } catch (_: Throwable) {
            return false
        } finally {
            activePlayerSink?.invoke(null)
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
    }

    private fun pruneCacheIfNeeded(dir: File) {
        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".mp3") } ?: return
        if (files.size <= 60) return
        files.sortedBy { it.lastModified() }.take(files.size - 48).forEach { runCatching { it.delete() } }
    }

    fun synthesizeRawMp3(
        text: String,
        voiceName: String,
        rate: String = "+0%",
        pitch: String = "+0Hz",
        timeoutMs: Long = 4200L,
    ): ByteArray? {
        val firstAttempt = streamWebSocketOnce(
            text = text,
            voiceName = formatServerVoiceName(voiceName),
            rate = rate,
            pitch = pitch,
            timeoutMs = timeoutMs,
        )
        return if (firstAttempt != null && firstAttempt.size >= 256) firstAttempt else null
    }

    private fun streamWebSocketOnce(
        text: String,
        voiceName: String,
        rate: String,
        pitch: String,
        timeoutMs: Long,
        allowClockRetry: Boolean = true,
    ): ByteArray? {
        val escaped = sanitizeSsmlText(text)
        if (escaped.isEmpty()) return null

        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val nowUnix = (System.currentTimeMillis() / 1000.0) + clockSkewSeconds
        val secMsGec = generateSecMsGecV2(nowUnix)
        val muid = randomHexLower(16).uppercase(Locale.US)
        val url = "$WSS_BASE&ConnectionId=$connectionId&Sec-MS-GEC=$secMsGec&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION"

        val request = Request.Builder()
            .url(url)
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0 Safari/537.36 " +
                    "Edg/$CHROMIUM_MAJOR_VERSION.0.0.0",
            )
            .header("Accept-Encoding", "gzip, deflate, br, zstd")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cookie", "muid=$muid;")
            .build()

        val audioBuffer = ByteArrayOutputStream(16 * 1024)
        val doneLatch = CountDownLatch(1)
        val saw403ServerDate = AtomicReference<String?>(null)
        val completedCleanly = AtomicBoolean(false)

        val ws = wsClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val ts = jsUtcDateString()
                    val configMsg =
                        "X-Timestamp:$ts\r\n" +
                            "Content-Type:application/json; charset=utf-8\r\n" +
                            "Path:speech.config\r\n\r\n" +
                            "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{" +
                            "\"sentenceBoundaryEnabled\":\"true\",\"wordBoundaryEnabled\":\"false\"}," +
                            "\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}\r\n"
                    val reqId = UUID.randomUUID().toString().replace("-", "")
                    val ssml =
                        "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                            "<voice name='$voiceName'>" +
                            "<prosody pitch='$pitch' rate='$rate' volume='+0%'>$escaped</prosody>" +
                            "</voice></speak>"
                    val ssmlMsg =
                        "X-RequestId:$reqId\r\n" +
                            "Content-Type:application/ssml+xml\r\n" +
                            "X-Timestamp:${ts}Z\r\n" +
                            "Path:ssml\r\n\r\n" +
                            ssml
                    webSocket.send(configMsg)
                    webSocket.send(ssmlMsg)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("Path:turn.end")) {
                        completedCleanly.set(true)
                        runCatching { webSocket.close(1000, null) }
                        doneLatch.countDown()
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val raw = bytes.toByteArray()
                    if (raw.size < 2) return
                    val headerLen = ((raw[0].toInt() and 0xFF) shl 8) or (raw[1].toInt() and 0xFF)
                    val audioStart = headerLen + 2
                    if (audioStart <= raw.size) {
                        val headerText = String(raw, 2, headerLen.coerceAtMost(raw.size - 2), Charsets.ISO_8859_1)
                        if (headerText.contains("Path:audio") && raw.size > audioStart) {
                            synchronized(audioBuffer) {
                                audioBuffer.write(raw, audioStart, raw.size - audioStart)
                            }
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (response?.code == 403) {
                        saw403ServerDate.set(response.header("Date"))
                    }
                    doneLatch.countDown()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    doneLatch.countDown()
                }
            },
        )

        val finished = runCatching { doneLatch.await(timeoutMs, TimeUnit.MILLISECONDS) }.getOrDefault(false)
        if (!finished) {
            runCatching { ws.cancel() }
        }

        val serverDateHeader = saw403ServerDate.get()
        if (allowClockRetry && serverDateHeader != null) {
            val parsedEpoch = runCatching {
                ZonedDateTime.parse(serverDateHeader, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond().toDouble()
            }.getOrNull()
            if (parsedEpoch != null) {
                clockSkewSeconds = parsedEpoch - (System.currentTimeMillis() / 1000.0)
                return streamWebSocketOnce(text, voiceName, rate, pitch, timeoutMs, allowClockRetry = false)
            }
        }

        val out = synchronized(audioBuffer) { audioBuffer.toByteArray() }
        return if (completedCleanly.get() && out.size >= 256) out else null
    }

    private fun sha256Hex(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    // ---------------------------------------------------------------------------------------
    // Exact v2 Sec-MS-GEC generator matching 2026 edge-tts 7.3.1 (<16-hex CS64>-<64-hex SHA256>)
    // ---------------------------------------------------------------------------------------

    private val SHA256_INITIAL = longArrayOf(
        0x6A09E667L, 0xBB67AE85L, 0x3C6EF372L, 0xA54FF53AL,
        0x510E527FL, 0x9B05688CL, 0x1F83D9ABL, 0x5BE0CD19L,
    )
    private val SHA256_STATE = longArrayOf(
        0xFD9FBEA2L, 0x91243CE7L, 0xBE499094L, 0xCD2E1E0AL,
        0x6C006F49L, 0x04BD35ABL, 0xFEB0A7E4L, 0x03EFFBD2L,
    )
    private val SHA256_SCHEDULE = longArrayOf(
        0xEB24BF9EL, 0x1558691FL, 0xA0C09EE2L, 0x0CFA32BAL,
        0xA622EE8CL, 0xD1DD9491L, 0xC0DD25A0L, 0xF9E70AB6L,
        0xF0C31573L, 0x7058CE4EL, 0x7188E606L, 0x00E5AB33L,
        0xD258B060L, 0x72766564L,
    )
    private val SHA256_ROUND_CONSTANTS = longArrayOf(
        0x428A2F98L, 0x71374491L, 0xB5C0FBCFL, 0xE9B5DBA5L, 0x3956C25BL, 0x59F111F1L, 0x923F82A4L, 0xAB1C5ED5L,
        0xD807AA98L, 0x12835B01L, 0x243185BEL, 0x550C7DC3L, 0x72BE5D74L, 0x80DEB1FEL, 0x9BDC06A7L, 0xC19BF174L,
        0xE49B69C1L, 0xEFBE4786L, 0x0FC19DC6L, 0x240CA1CCL, 0x2DE92C6FL, 0x4A7484AAL, 0x5CB0A9DCL, 0x76F988DAL,
        0x983E5152L, 0xA831C66DL, 0xB00327C8L, 0xBF597FC7L, 0xC6E00BF3L, 0xD5A79147L, 0x06CA6351L, 0x14292967L,
        0x27B70A85L, 0x2E1B2138L, 0x4D2C6DFCL, 0x53380D13L, 0x650A7354L, 0x766A0ABBL, 0x81C2C92EL, 0x92722C85L,
        0xA2BFE8A1L, 0xA81A664BL, 0xC24B8B70L, 0xC76C51A3L, 0xD192E819L, 0xD6990624L, 0xF40E3585L, 0x106AA070L,
        0x19A4C116L, 0x1E376C08L, 0x2748774CL, 0x34B0BCB5L, 0x391C0CB3L, 0x4ED8AA4AL, 0x5B9CCA4FL, 0x682E6FF3L,
        0x748F82EEL, 0x78A5636FL, 0x84C87814L, 0x8CC70208L, 0x90BEFFFAL, 0xA4506CEBL, 0xBEF9A3F7L, 0xC67178F2L,
    )
    private const val SHA256_ADJUSTMENT = 0xC9DC06A7L
    private const val SHA256_SIGMA0_ADJUSTMENT = 0x059C0B80L
    private const val SHA256_SIGMA1_ADJUSTMENT = 0x000B92C0L

    private val CS64_COEFFS = arrayOf(
        0xE82E91B1L to 0xB32700D0L,
        0x44DE24C6L to 0x88DFF51DL,
        0x3BEB65BCL to 0x29514038L,
        0xC89C5933L to 0xB328CBDFL,
        0x8D1EC211L to 0xD405B4D5L,
        0x314EF5D5L to 0x26E55F3CL,
        0xE92491B1L to 0x607801C6L,
        0xF0A5B9D5L to 0x82B2C1FBL,
        0x06064CF3L to 0xFA51A3DAL,
        0xDA79FA60L to 0x207CC464L,
        0xBED20560L to 0x3C3C74F3L,
        0x89F9C9F8L to 0xD2D14B61L,
        0x35FA38C6L to 0xBB04A2C1L,
        0xCB49E3C6L to 0xC5626167L,
    )
    private const val CS64_FACTOR = 0x82893933L
    private const val CS64_ADJUSTMENT0 = 0xF2F90B1AL
    private const val CS64_ADJUSTMENT1 = 0x0B1A0000L

    private fun rol16(value: Long): Long {
        val v = value and U32_MASK
        return ((v shl 16) or (v ushr 16)) and U32_MASK
    }

    private fun rotr32(v: Long, n: Int): Long {
        val x = v and U32_MASK
        return ((x ushr n) or (x shl (32 - n))) and U32_MASK
    }

    private fun sigma0(value: Long): Long {
        val v = value and U32_MASK
        return (rotr32(v, 7) xor rotr32(v, 18) xor (v ushr 3)) and U32_MASK
    }

    private fun sigma1(value: Long): Long {
        val v = value and U32_MASK
        return (rotr32(v, 17) xor rotr32(v, 19) xor (v ushr 10)) and U32_MASK
    }

    private fun expandSchedule(words: LongArray, firstWord: Int) {
        for (i in firstWord until 64) {
            words[i] = (sigma1(words[i - 2]) + words[i - 7] + sigma0(words[i - 15]) + words[i - 16]) and U32_MASK
        }
    }

    private fun sha256Rounds(state: LongArray, words: LongArray, firstRound: Int = 0): LongArray {
        var a = state[0]
        var b = state[1]
        var c = state[2]
        var d = state[3]
        var e = state[4]
        var f = state[5]
        var g = state[6]
        var h = state[7]
        for (idx in firstRound until 64) {
            val k = if (firstRound == 14 && idx == 14) SHA256_ADJUSTMENT else SHA256_ROUND_CONSTANTS[idx]
            val bigSigma1 = rotr32(e, 6) xor rotr32(e, 11) xor rotr32(e, 25)
            val ch = g xor (e and (f xor g))
            val t1 = (h + bigSigma1 + ch + k + words[idx]) and U32_MASK
            val bigSigma0 = rotr32(a, 2) xor rotr32(a, 13) xor rotr32(a, 22)
            val maj = (a and b) or (c and (a or b))
            val t2 = (bigSigma0 + maj) and U32_MASK
            h = g
            g = f
            f = e
            e = (d + t1) and U32_MASK
            d = c
            c = b
            b = a
            a = (t1 + t2) and U32_MASK
        }
        return longArrayOf(a, b, c, d, e, f, g, h)
    }

    private fun sha256CustomDigest(data: ByteArray): ByteArray {
        val initWords = LongArray(64)
        initWords[14] = (((data[0].toLong() and 0xFF) shl 16) or ((data[1].toLong() and 0xFF) shl 8) or (data[2].toLong() and 0xFF)) and U32_MASK
        initWords[15] = (
            ((data[3].toLong() and 0xFF) shl 24) or
                ((data[4].toLong() and 0xFF) shl 16) or
                ((data[5].toLong() and 0xFF) shl 8) or
                (data[6].toLong() and 0xFF)
            ) and U32_MASK
        for (index in 16 until 30) {
            var v = SHA256_SCHEDULE[index - 16]
            if (index - 2 >= 14) {
                v += if (index == 16) (sigma1(initWords[index - 2]) xor SHA256_SIGMA1_ADJUSTMENT) else sigma1(initWords[index - 2])
            }
            if (index - 7 >= 14) {
                v += initWords[index - 7]
            }
            if (index - 15 >= 14) {
                v += if (index == 29) (sigma0(initWords[index - 15]) xor SHA256_SIGMA0_ADJUSTMENT) else sigma0(initWords[index - 15])
            }
            initWords[index] = v and U32_MASK
        }
        initWords[30] = (
            sigma1(initWords[28]) +
                initWords[23] +
                sigma0(initWords[15]) +
                initWords[14] +
                SHA256_ADJUSTMENT -
                SHA256_ROUND_CONSTANTS[14]
            ) and U32_MASK
        expandSchedule(initWords, 31)
        val initWorking = sha256Rounds(SHA256_STATE, initWords, 14)
        var state = LongArray(8) { i -> (SHA256_INITIAL[i] + initWorking[i]) and U32_MASK }

        val length = 57 + data.size
        val tail = data.copyOfRange(7, data.size)
        val zeroPadLen = Math.floorMod(55 - length, 64)
        val padded = ByteArray(tail.size + 1 + zeroPadLen + 8)
        System.arraycopy(tail, 0, padded, 0, tail.size)
        padded[tail.size] = 0x80.toByte()
        ByteBuffer.wrap(padded, padded.size - 8, 8).order(ByteOrder.BIG_ENDIAN).putLong(length.toLong() * 8L)

        var offset = 0
        while (offset < padded.size) {
            val blockWords = LongArray(64)
            val bb = ByteBuffer.wrap(padded, offset, 64).order(ByteOrder.BIG_ENDIAN)
            for (w in 0 until 16) {
                blockWords[w] = bb.int.toLong() and U32_MASK
            }
            expandSchedule(blockWords, 16)
            val working = sha256Rounds(state, blockWords, 0)
            state = LongArray(8) { i -> (state[i] + working[i]) and U32_MASK }
            offset += 64
        }

        val out = ByteArray(32)
        val outBuf = ByteBuffer.wrap(out).order(ByteOrder.BIG_ENDIAN)
        for (s in state) outBuf.putInt(s.toInt())
        return out
    }

    private fun wordsLe4Even(prefixedData: ByteArray): List<Long> {
        val rem = Math.floorMod(-prefixedData.size, 4)
        val padded = ByteArray(prefixedData.size + rem)
        System.arraycopy(prefixedData, 0, padded, 0, prefixedData.size)
        val list = ArrayList<Long>((padded.size / 4) + 1)
        val bb = ByteBuffer.wrap(padded).order(ByteOrder.LITTLE_ENDIAN)
        repeat(padded.size / 4) {
            list.add(bb.int.toLong() and U32_MASK)
        }
        if (list.size % 2 == 1) list.add(0L)
        return list
    }

    private fun cs64Digest(data: ByteArray, digest: ByteArray): Pair<Long, Long> {
        val leDigest = ByteBuffer.wrap(digest, 0, 8).order(ByteOrder.LITTLE_ENDIAN)
        val keyA = (leDigest.int.toLong() and U32_MASK) or 1L
        val keyB = (leDigest.int.toLong() and U32_MASK) or 1L
        val factor0 = (keyA * CS64_FACTOR) and U32_MASK
        val factor1 = (keyB * CS64_FACTOR) and U32_MASK

        val terms = ArrayList<Long>(32)
        val offsets = ArrayList<Long>(32)
        for ((index, pair) in CS64_COEFFS.withIndex()) {
            val f = if (index % 2 == 0) factor0 else factor1
            terms.add((pair.first * f) and U32_MASK)
            offsets.add(pair.second)
        }

        val zeroPrefixed = ByteArray(data.size + 1)
        System.arraycopy(data, 0, zeroPrefixed, 1, data.size)
        for ((index, word) in wordsLe4Even(zeroPrefixed).withIndex()) {
            var term = word * (if (index % 2 == 0) keyA else keyB)
            var offset = rol16(word) * (if (index % 2 == 0) 0xEF0569FBL else 0xC31713DBL)
            if (index == 0) {
                term += CS64_ADJUSTMENT0 * factor0
                offset += CS64_ADJUSTMENT1
            }
            terms.add(term and U32_MASK)
            offsets.add(offset and U32_MASK)
        }

        var swapAcc = 0L
        var swapLast = 0L
        var state = 0L
        var acc = 0L
        for (index in terms.indices step 2) {
            val x = terms[index]
            val y = terms[index + 1]
            var t = rol16((offsets[index] + x) and U32_MASK)
            var q = (t * 0x76F50F34L) and U32_MASK
            swapAcc += (rol16(q) * 0xC3EFEA97L + q) and U32_MASK
            t = rol16((offsets[index + 1] + y) and U32_MASK)
            q = (t * 0x1881CE3CL) and U32_MASK
            swapLast = (rol16(q) * 0x35BD1EC9L + q) and U32_MASK
            swapAcc = (swapAcc + swapLast) and U32_MASK

            state = (state * keyA + x) and U32_MASK
            state = (rol16(state) * 0xCF98B111L) and U32_MASK
            state = (rol16(state) * 0x87085B9FL) and U32_MASK
            state = (rol16(state) * 0x12CEB96DL) and U32_MASK
            state = (rol16(state) * 0x257E1D83L) and U32_MASK
            acc += state
            t = (state * keyB + y) and U32_MASK
            t = (rol16(t) * 0xA27416F5L) and U32_MASK
            t = (rol16(t) * 0xD38396FFL) and U32_MASK
            state = (rol16((rol16(t) * 0x7C932B89L) and U32_MASK) * 0xBFA49F69L) and U32_MASK
            acc = (acc + state) and U32_MASK
        }
        return (swapLast xor state) to (swapAcc xor acc)
    }

    fun generateSecMsGecV2(unixTimestampSeconds: Double): String {
        val rounded = floor((unixTimestampSeconds + WIN_EPOCH_SECONDS) / 300.0).toLong() * 300L
        val ticks = rounded * 10_000_000L
        val raw = "$ticks$TRUSTED_CLIENT_TOKEN".toByteArray(Charsets.US_ASCII)
        val digest = sha256CustomDigest(raw)
        val (cs0, cs1) = cs64Digest(raw, digest)
        val csBytes = ByteArray(8)
        ByteBuffer.wrap(csBytes).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(cs0.toInt())
            .putInt(cs1.toInt())
        val csHex = csBytes.joinToString("") { "%02X".format(it) }
        val digestHex = digest.joinToString("") { "%02X".format(it) }
        return "$csHex-$digestHex"
    }
}
