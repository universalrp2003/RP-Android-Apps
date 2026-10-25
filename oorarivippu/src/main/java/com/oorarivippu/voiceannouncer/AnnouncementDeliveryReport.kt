package com.oorarivippu.voiceannouncer

/**
 * A5 - the delivery report behind Oor Arivippu's announcement history.
 *
 * The history card used to record the fact that the app *tried* to speak, which is the one thing
 * an owner cannot use: after a trip the real question is "did the voice actually come out?". This
 * file holds the decision rules that turn an announcement attempt into a recorded outcome, plus the
 * summary arithmetic the report card shows.
 *
 * Everything here is a pure function of strings and numbers so it can be tested without a device,
 * an audio focus request, or a network call.
 */
object AnnouncementDeliveryReport {

    /** How many past announcements are kept. Enough for a full day, bounded in storage. */
    const val MAX_HISTORY = 40

    /** Field separator for the persisted line. A control character, so it cannot occur in speech. */
    const val FIELD_SEP = "\u0001"

    /** Why an announcement ended up the way it did. */
    enum class Outcome {
        /** The voice was actually played out loud. */
        SPOKEN,

        /** Held back until the phone call ends, then spoken. */
        QUEUED_CALL,

        /** Dropped because there was no internet and this announcement needed it. */
        NO_INTERNET,

        /** Dropped because GPS was off while tracking. */
        NO_GPS,

        /** Dropped because no usable voice (HD cloud and on-device both unavailable). */
        VOICE_UNAVAILABLE,

        /** Dropped for any other reason (not tracking, no destination, duplicate, ...). */
        SKIPPED,
    }

    /**
     * The stored form of one announcement: when, what was spoken, and what happened.
     *
     * [delivered] is derived from [outcome] rather than stored twice, so the two can never disagree.
     */
    data class Entry(
        val timestamp: String,
        val title: String,
        val spokenText: String,
        val outcome: Outcome,
    ) {
        val delivered: Boolean get() = outcome == Outcome.SPOKEN
    }

    /**
     * A pure summary of the past announcements. Built from the visible history so the report card
     * and any export share exactly the same numbers.
     */
    data class Summary(
        val total: Int,
        val spoken: Int,
        val notSpoken: Int,
        val distinctPlaces: Int,
        val queuedDuringCall: Int,
        val noInternet: Int,
        val noGps: Int,
        val voiceUnavailable: Int,
        val skipped: Int,
    ) {
        val isEmpty: Boolean get() = total == 0
        /** Percent of announcements that actually reached the air, 0..100, rounded. */
        val deliveredPercent: Int get() = if (total == 0) 0 else Math.round(spoken * 100f / total)
    }

    fun summarize(history: List<AnnouncementLogItem>): Summary {
        var spoken = 0
        var queuedDuringCall = 0
        var noInternet = 0
        var noGps = 0
        var voiceUnavailable = 0
        var skipped = 0
        val places = LinkedHashSet<String>()
        for (item in history) {
            when (outcomeOf(item)) {
                Outcome.SPOKEN -> spoken++
                Outcome.QUEUED_CALL -> queuedDuringCall++
                Outcome.NO_INTERNET -> noInternet++
                Outcome.NO_GPS -> noGps++
                Outcome.VOICE_UNAVAILABLE -> voiceUnavailable++
                Outcome.SKIPPED -> skipped++
            }
            val place = item.title.trim()
            if (place.isNotEmpty()) places.add(place)
        }
        return Summary(
            total = history.size,
            spoken = spoken,
            notSpoken = history.size - spoken,
            distinctPlaces = places.size,
            queuedDuringCall = queuedDuringCall,
            noInternet = noInternet,
            noGps = noGps,
            voiceUnavailable = voiceUnavailable,
            skipped = skipped,
        )
    }

    /**
     * The outcome recorded for one row.
     *
     * An older stored row has no outcome at all (the app was rebuilt over it), and those were
     * announcements that were spoken - the old code only logged a line when it spoke - so they
     * read as SPOKEN rather than as a suspicious drop.
     */
    fun outcomeOf(item: AnnouncementLogItem): Outcome =
        if (item.outcome.isBlank()) Outcome.SPOKEN else runCatching { Outcome.valueOf(item.outcome) }
            .getOrDefault(Outcome.SPOKEN)

    /** One persisted line. A malformed line decodes to null and is dropped, not the whole list. */
    fun encode(item: AnnouncementLogItem): String =
        listOf(item.timestamp, item.title, item.spokenText, item.outcome).joinToString(FIELD_SEP)

    /**
     * A persisted line back into a row.
     *
     * The spoken text may itself contain the field separator (a place name with a control character
     * in it is the only way), so the trailing fields are taken from the right and the text keeps
     * everything in between.
     */
    fun decode(line: String): AnnouncementLogItem? {
        if (line.isEmpty()) return null
        val parts = line.split(FIELD_SEP)
        if (parts.size < 4) return null
        val timestamp = parts.first().trim()
        val outcome = parts.last().trim()
        if (timestamp.isEmpty() || outcome.isEmpty()) return null
        val spokenText = parts.subList(2, parts.size - 1).joinToString(FIELD_SEP)
        val title = parts[1]
        return AnnouncementLogItem(
            timestamp = timestamp,
            title = title,
            spokenText = spokenText,
            outcome = outcome,
        )
    }

    /** Encode a whole list, newest first, capped at [MAX_HISTORY]. */
    fun encodeAll(history: List<AnnouncementLogItem>): String =
        history.take(MAX_HISTORY).joinToString("\n") { encode(it) }

    /** Decode a stored block, dropping anything malformed instead of losing the good rows. */
    fun decodeAll(stored: String): List<AnnouncementLogItem> =
        stored.split("\n").mapNotNull { decode(it) }.take(MAX_HISTORY)

    /** A short, shareable text form of the report, one line per announcement. */
    fun toPlainText(history: List<AnnouncementLogItem>, summary: Summary): String {
        if (summary.isEmpty) return ""
        val lines = mutableListOf<String>()
        for (item in history) {
            lines.add(
                item.timestamp + " - " + item.title + " - " +
                    labelFor(outcomeOf(item))
            )
        }
        return lines.joinToString("\n")
    }

    /**
     * The short English label for an outcome. Used in the plain-text export and in tests; the UI
     * uses the bilingual strings so an owner reading Tamil sees Tamil.
     */
    fun labelFor(outcome: Outcome): String = when (outcome) {
        Outcome.SPOKEN -> "spoken"
        Outcome.QUEUED_CALL -> "held for the call"
        Outcome.NO_INTERNET -> "no internet"
        Outcome.NO_GPS -> "GPS off"
        Outcome.VOICE_UNAVAILABLE -> "no voice"
        Outcome.SKIPPED -> "not spoken"
    }
}
