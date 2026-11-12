package com.universalrp.podcast

/** What the player is doing, in one place so the screen and the notification agree. */
object PodcastState {

    enum class Status { STOPPED, BUFFERING, PLAYING, PAUSED, FAILED }

    private val lock = Any()

    var status: Status = Status.STOPPED
        private set

    /** Title of the episode currently loaded, or empty. */
    var nowPlaying: String = ""
        private set

    var positionMs: Int = 0
        private set
    var durationMs: Int = 0
        private set

    fun progress(position: Int, duration: Int) = synchronized(lock) {
        positionMs = position.coerceAtLeast(0)
        durationMs = duration.coerceAtLeast(0)
        revision++
    }

    var revision: Int = 0
        private set

    fun markBuffering(title: String = nowPlaying) = synchronized(lock) {
        status = Status.BUFFERING; nowPlaying = title; positionMs = 0; durationMs = 0; revision++
    }

    fun markPlaying(title: String = nowPlaying) = synchronized(lock) {
        status = Status.PLAYING; nowPlaying = title; revision++
    }

    fun markPaused() = synchronized(lock) {
        status = Status.PAUSED; revision++
    }

    fun markStopped() = synchronized(lock) {
        status = Status.STOPPED; nowPlaying = ""; revision++
    }

    fun markFailed() = synchronized(lock) {
        status = Status.FAILED; positionMs = 0; durationMs = 0; revision++
    }

    fun nowPlayingTitle(): String = nowPlaying

    fun isActive(): Boolean = status == Status.PLAYING || status == Status.PAUSED ||
        status == Status.BUFFERING
}
