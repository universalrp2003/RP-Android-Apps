package com.universalrp.weeklymoney

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Bounded published civil-date table, not a recurring Gregorian approximation or holiday engine.
 * Sources/limitations: docs/weekly-money-tamil-calendar.md.
 */
object TamilLoanDate {
    private val starts = listOf(
        "2026-01-15" to "தை", "2026-02-13" to "மாசி", "2026-03-15" to "பங்குனி",
        "2026-04-14" to "சித்திரை", "2026-05-15" to "வைகாசி", "2026-06-15" to "ஆனி",
        "2026-07-17" to "ஆடி", "2026-08-18" to "ஆவணி", "2026-09-18" to "புரட்டாசி",
        "2026-10-18" to "ஐப்பசி", "2026-11-17" to "கார்த்திகை", "2026-12-16" to "மார்கழி",
    ).map { LocalDate.parse(it.first) to it.second }
    fun label(date: LocalDate): String? {
        if (date < starts.first().first || date > LocalDate.of(2026, 12, 31)) return null
        val (start, month) = starts.last { it.first <= date }
        return "$month ${ChronoUnit.DAYS.between(start, date) + 1}"
    }
    fun display(date: LocalDate) = label(date)?.let { "தமிழ் தேதி: $it" }
        ?: "தமிழ் தேதி: இந்த தேதிக்குச் சரிபார்க்கப்பட்ட அட்டவணை இல்லை / Calendar date unavailable"
}
