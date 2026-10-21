package com.universalrp.cleansweep.voice

/** Pure gates. An enabled switch/accepted request is never proof that sound was heard. */
object VoiceDeliveryPolicy {
    enum class Stage { NOT_REQUESTED, DISABLED, CALL_IN_PROGRESS, EVENT_OFF, QUIET_HOURS, QUEUED, INITIALIZING, NO_ENGINE,
        INIT_FAILED, INIT_TIMEOUT, LANGUAGE_MISSING, ENGINE_REJECTED, PLAYING, ENGINE_FINISHED,
        ENGINE_ERROR, TIMED_OUT, CANCELLED, DUPLICATE, STALE_EVENT }
    /**
     * @param inCall true while the phone is ringing or in a call. It outranks every other
     * gate, including a manual test: nothing may be spoken into a live call.
     * @param bypassQuietHours true only for life/property-critical severe weather warnings
     * (very heavy rain or cyclone), which must speak at any hour without skipping quiet hours.
     */
    fun blocked(master: Boolean, eventAllowed: Boolean, quiet: Boolean, manual: Boolean,
                inCall: Boolean = false, bypassQuietHours: Boolean = false): Stage? = when {
        !master -> Stage.DISABLED
        inCall -> Stage.CALL_IN_PROGRESS
        !eventAllowed -> Stage.EVENT_OFF
        quiet && !manual && !bypassQuietHours -> Stage.QUIET_HOURS
        else -> null
    }
    fun cooldownReady(nowMs: Long, lastMs: Long, intervalMs: Long): Boolean = lastMs <= 0 || nowMs < lastMs || nowMs - lastMs >= intervalMs
    fun changedTransport(previous: String?, current: String): Boolean = previous in setOf("wifi", "mobile") &&
        current in setOf("wifi", "mobile") && previous != current
    fun retryOffline(networkVoice: Boolean, retried: Boolean, offlineInstalled: Boolean): Boolean = networkVoice && !retried && offlineInstalled
    fun validStatus(code: Int): Boolean = code >= 0 // ERROR (-1), missing data (-2), unsupported (-3) are not ready.
}

data class VoiceDeliveryStatus(
    val stage: VoiceDeliveryPolicy.Stage = VoiceDeliveryPolicy.Stage.NOT_REQUESTED,
    val event: String = "",
    val atMs: Long = 0,
    val tamilAvailable: Boolean? = null,
    val mediaVolume: Int? = null,
    val online: Boolean = false,
    val language: String = "",
)

/** Registration evidence only, never proof that a physical event arrived or audio was heard. Runtime-only. */
data class VoiceObserverStatus(val owners: Int = 0, val battery: Boolean? = null, val network: Boolean? = null, val power: Boolean? = null)
