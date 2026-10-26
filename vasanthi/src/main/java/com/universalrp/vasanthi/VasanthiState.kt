package com.universalrp.vasanthi

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.atomic.AtomicLong

/**
 * Everything Vasanthi AI currently knows, in one observable place.
 *
 * The floating window is owned by a Service and the full screen by an Activity, and the two have
 * to agree at all times: the bubble shows what the panel shows, and the panel shows what the
 * history screen shows. Putting the state in a plain singleton rather than in a ViewModel means
 * the Service, which has no composition scope of its own, can update it just as easily as the
 * Activity can read it.
 */
object VasanthiState {

    /** True while the microphone is open. */
    var listening by mutableStateOf(false)
        private set

    /** The sound the classifier is most confident about right now, if it has a Tamil name. */
    var currentSound by mutableStateOf<SoundScore?>(null)
        private set

    /** The room's loudness, 0..1. */
    var level by mutableStateOf(0f)
        private set

    /** The most recent line of recognised Tamil speech. */
    var liveSpeech by mutableStateOf("")
        private set

    /** Which recogniser is in use, so the screen can say so rather than imply either. */
    var mode by mutableStateOf(RecognitionMode.UNAVAILABLE)
        private set

    /** The saved history, newest first. */
    var entries by mutableStateOf<List<VasanthiEntry>>(emptyList())
        private set

    /**
     * When an urgent sound was last heard. The floating window watches this to vibrate, and the
     * value is a timestamp rather than a boolean so a repeat of the same sound still triggers it.
     */
    var urgentAt by mutableStateOf(0L)
        private set

    /** Whether the floating window is on screen at all. */
    var overlayVisible by mutableStateOf(false)
        private set

    /** Whether the on-device-only speech mode is selected. */
    var preferOffline by mutableStateOf(true)
        private set

    private var store: VasanthiStore? = null

    /** Monotonic ids, seeded from the clock so a restart does not reuse yesterday's ids. */
    private val idSource = AtomicLong(System.currentTimeMillis())

    private var lastSoundLabel = ""
    private var lastSoundAt = 0L

    fun attach(context: Context) {
        if (store == null) {
            store = VasanthiStore.create(context)
        }
        entries = store?.load().orEmpty()
    }

    fun setPreferOffline(value: Boolean) {
        preferOffline = value
    }

    fun setListening(value: Boolean) {
        listening = value
    }

    fun setMode(value: RecognitionMode) {
        mode = value
    }

    fun setLevel(value: Float) {
        level = value
    }

    fun setOverlayVisible(value: Boolean) {
        overlayVisible = value
    }

    /** Records a spoken line. Only called with final results, so the history is not flooded. */
    fun recordSpeech(text: String, timestampMs: Long = System.currentTimeMillis()) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        liveSpeech = trimmed
        // The same sentence repeated by a recogniser that restarted is one sentence, not two.
        if (trimmed == entries.firstOrNull()?.text) return
        val entry = VasanthiEntry(
            id = idSource.incrementAndGet(),
            timestampMs = timestampMs,
            kind = EntryKind.SPEECH,
            text = trimmed,
            detail = mode.name,
            confidence = 0f,
            urgent = false,
        )
        entries = prepend(entry)
    }

    /**
     * Records a named sound.
     *
     * The classifier runs about once a second and will happily report the same sound sixty times
     * a minute. A history that says "Speech" sixty times in a row is not a history, so a repeat of
     * the same label is dropped until the cooldown passes, and a genuinely new sound is recorded
     * the moment it appears.
     */
    fun recordSound(score: SoundScore, timestampMs: Long = System.currentTimeMillis()) {
        currentSound = score
        val sameAsLast = score.englishLabel == lastSoundLabel
        if (sameAsLast && timestampMs - lastSoundAt < SOUND_COOLDOWN_MS) return
        lastSoundLabel = score.englishLabel
        lastSoundAt = timestampMs
        val urgent = SoundLabelMap.isUrgent(score.englishLabel)
        val entry = VasanthiEntry(
            id = idSource.incrementAndGet(),
            timestampMs = timestampMs,
            kind = EntryKind.SOUND,
            text = score.tamilLabel,
            detail = score.englishLabel,
            confidence = score.confidence,
            urgent = urgent,
        )
        entries = prepend(entry)
        if (urgent) urgentAt = timestampMs
    }

    fun removeEntry(id: Long) {
        entries = store?.delete(id) ?: entries.filterNot { it.id == id }
    }

    fun clearHistory() {
        store?.clear()
        entries = emptyList()
    }

    /** Called when the microphone closes, so the screen stops claiming it is listening. */
    fun onStopped() {
        listening = false
        level = 0f
        currentSound = null
        lastSoundLabel = ""
    }

    private fun prepend(entry: VasanthiEntry): List<VasanthiEntry> {
        val written = store?.add(entry)
        return written ?: (listOf(entry) + entries).take(VasanthiStore.MAX_ENTRIES)
    }

    /** How long the same sound must stay quiet before it can be recorded again. */
    const val SOUND_COOLDOWN_MS = 12_000L
}
