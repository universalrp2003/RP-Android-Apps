package com.universalrp.emergency

/**
 * Everything the screen shows, in one immutable place.
 *
 * A plain object with a lock rather than a ViewModel, because the service that feeds it is a
 * foreground service started before any Activity exists and it has to keep updating the same
 * object across Activity restarts. Vasanthi AI learned the hard way what happens when a store is
 * re-read from disk on the main thread per update.
 */
object EmergencyState {

    private val lock = Any()

    /** Alerts this phone heard itself, newest first. */
    private val localAlerts = mutableListOf<EmergencyAlert>()

    /** Alerts that arrived from other phones on the Wi-Fi, newest first. */
    private val receivedAlerts = mutableListOf<EmergencyAlert>()

    /** Phones seen on the Wi-Fi, by the name they announce themselves as. */
    private val seenSources = LinkedHashSet<String>()

    var listening: Boolean = false
        private set

    /** True once the microphone is actually open, so the screen can say so honestly. */
    var microphoneLive: Boolean = false
        private set

    var classifierReady: Boolean = false
        private set

    /** Last error the user should see, in Tamil, or null. */
    var statusText: String? = null
        private set

    /** Counter incremented on every change, so Compose can snapshot it cheaply. */
    var revision: Int = 0
        private set

    fun markListening(value: Boolean) = synchronized(lock) {
        listening = value
        revision++
    }

    fun markMicrophone(value: Boolean) = synchronized(lock) {
        microphoneLive = value
        revision++
    }

    fun markClassifier(value: Boolean) = synchronized(lock) {
        classifierReady = value
        revision++
    }

    fun setStatus(text: String?) = synchronized(lock) {
        statusText = text
        revision++
    }

    /** Records an alert this phone heard. Returns false if it is a duplicate of the newest one. */
    fun addLocal(alert: EmergencyAlert): Boolean = synchronized(lock) {
        val newest = localAlerts.firstOrNull()
        val duplicate = newest != null &&
            newest.englishLabel == alert.englishLabel &&
            alert.detectedAt - newest.detectedAt < DUPLICATE_WINDOW_MS
        if (!duplicate) {
            localAlerts.add(0, alert)
            while (localAlerts.size > MAX_ALERTS) localAlerts.removeAt(localAlerts.size - 1)
        }
        revision++
        !duplicate
    }

    fun addReceived(alert: EmergencyAlert): Boolean = synchronized(lock) {
        val newest = receivedAlerts.firstOrNull()
        val duplicate = newest != null &&
            newest.sourceName == alert.sourceName &&
            newest.tamilLabel == alert.tamilLabel &&
            alert.detectedAt - newest.detectedAt < DUPLICATE_WINDOW_MS
        if (!duplicate) {
            receivedAlerts.add(0, alert)
            while (receivedAlerts.size > MAX_ALERTS) receivedAlerts.removeAt(receivedAlerts.size - 1)
            seenSources.add(alert.sourceName)
        }
        revision++
        !duplicate
    }

    fun localSnapshot(): List<EmergencyAlert> = synchronized(lock) { localAlerts.toList() }

    fun receivedSnapshot(): List<EmergencyAlert> = synchronized(lock) { receivedAlerts.toList() }

    /** Records a phone that answered a ping, so the screen can list who is connected. */
    fun addSource(name: String) = synchronized(lock) {
        if (name.isNotBlank() && seenSources.add(name)) revision++
    }

    fun sourcesSnapshot(): List<String> = synchronized(lock) { seenSources.toList() }

    fun clear() = synchronized(lock) {
        localAlerts.clear()
        receivedAlerts.clear()
        seenSources.clear()
        revision++
    }

    /**
     * Two detections of the same class inside this window are one event.
     *
     * YAMNet runs about once a second, so a smoke alarm that sounds for thirty seconds would
     * otherwise produce thirty identical alerts, thirty notifications and thirty UDP packets.
     */
    const val DUPLICATE_WINDOW_MS = 15_000L

    const val MAX_ALERTS = 100
}
