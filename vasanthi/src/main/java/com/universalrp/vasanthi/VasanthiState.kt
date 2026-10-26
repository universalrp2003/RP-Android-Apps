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
 *
 * The properties are deliberately settable rather than `private set` with wrapper functions. A
 * `private set` still emits a JVM setter called setXxx, and a public function of the same name
 * emits the same signature, so Kotlin rejects the pair as a platform declaration clash. Wrappers
 * would have to be named something else to compile, which is a naming constraint bought for no
 * gain on an object that exists to be written from two places.
 *
 * Sound naming and speech are two separate switches, because the owner asked for them to be and
 * because they compete for one microphone. Turning sound naming off gives the recogniser the
 * microphone outright, which is both more accurate and further-reaching; leaving both on shares
 * it between them.
 */
object VasanthiState {

    /** True while the microphone is open and at least one of the two functions is on. */
    var listening by mutableStateOf(false)

    /** Whether the app names the sounds in the room. */
    var soundEnabled by mutableStateOf(true)

    /** Whether the app writes down what someone says. */
    var voiceEnabled by mutableStateOf(true)

    /** Whether anything at all is on, so the microphone has a reason to be open. */
    val anyEnabled: Boolean
        get() = soundEnabled || voiceEnabled

    /** The sound the classifier is most confident about right now, if it has a Tamil name. */
    var currentSound by mutableStateOf<SoundScore?>(null)

    /** The room's loudness, 0..1. */
    var level by mutableStateOf(0f)

    /** The most recent line of recognised Tamil speech. */
    var liveSpeech by mutableStateOf("")

    /**
     * Why speech is producing no text, if it is not.
     *
     * "No speech recogniser" alone sends the owner looking for a setting that does not exist. A
     * missing Tamil pack, no internet and a held microphone each need a different fix, so the
     * screen can name which one it is.
     */
    var problem by mutableStateOf(RecognitionProblem.NONE)

    /** Whether the English-to-Tamil translator has its language model ready. */
    var translatorReady by mutableStateOf(false)

    /** The saved history, newest first. */
    var entries by mutableStateOf<List<VasanthiEntry>>(emptyList())

    /**
     * When an urgent sound was last heard. The floating window watches this to vibrate, and the
     * value is a timestamp rather than a boolean so a repeat of the same sound still triggers it.
     */
    var urgentAt by mutableStateOf(0L)

    /** Whether the floating window is on screen at all. */
    var overlayVisible by mutableStateOf(false)

    private var store: VasanthiStore? = null

    /** Monotonic ids, seeded from the clock so a restart does not reuse yesterday's ids. */
    private val idSource = AtomicLong(System.currentTimeMillis())

    private var lastSpeechText = ""
    private var lastSpeechAt = 0L
    private var lastSoundLabel = ""
    private var lastSoundAt = 0L

    fun attach(context: Context) {
        if (store == null) {
            store = VasanthiStore.create(context)
        }
        entries = store?.load().orEmpty()
    }

    /**
     * Records a spoken line. Only called with final results, so the history is not flooded.
     *
     * The recogniser restarts constantly and usually returns the same sentence again, so a repeat
     * inside the cooldown is dropped. Without this, a television left on fills the history with
     * the same line dozens of times, and a history nobody can read is worse than no history.
     */
    fun recordSpeech(text: String, timestampMs: Long = System.currentTimeMillis()) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        // Shown at once, in whatever the recogniser gave, so she is never looking at a blank line
        // while a translation is still arriving.
        liveSpeech = trimmed
        if (isLatinScript(trimmed)) {
            // English. Translated before it is written down, because a line she cannot read is
            // not a line. The Tamil arrives on the main thread and replaces what is on screen.
            TamilTranslator.translate(trimmed) { tamil ->
                liveSpeech = tamil
                storeSpeech(tamil, timestampMs)
            }
            return
        }
        storeSpeech(trimmed, timestampMs)
    }

    private fun storeSpeech(text: String, timestampMs: Long) {
        if (text.isEmpty()) return
        if (text == lastSpeechText && timestampMs - lastSpeechAt < SPEECH_COOLDOWN_MS) return
        lastSpeechText = text
        lastSpeechAt = timestampMs
        val entry = VasanthiEntry(
            id = idSource.incrementAndGet(),
            timestampMs = timestampMs,
            kind = EntryKind.SPEECH,
            text = text,
            // Nothing is written here. The English name of the recogniser used to be, and a
            // Tamil-only reader should not be shown a word she does not read.
            detail = "",
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
            detail = "",
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
        lastSpeechText = ""
    }

    private fun prepend(entry: VasanthiEntry): List<VasanthiEntry> {
        val written = store?.add(entry)
        return written ?: (listOf(entry) + entries).take(VasanthiStore.MAX_ENTRIES)
    }

    /** How long the same sentence must stay unheard before it can be recorded again. */
    const val SOUND_COOLDOWN_MS = 12_000L

    /**
     * How long the same spoken line must wait before it is written again.
     *
     * Longer than the sound cooldown, because a recogniser that restarts every few seconds returns
     * the same sentence each time and the sentence is longer than a sound label.
     */
    const val SPEECH_COOLDOWN_MS = 25_000L
}
