package com.universalrp.weeklymoney

import java.time.DayOfWeek
import java.time.LocalDate

/** User-facing validation; never requires the user to type numeric weekdays or ISO dates. */
class InputProblem(val explanation: String) : IllegalArgumentException(explanation)
object BookInput {
    fun create(id: String, number: String, day: DayOfWeek, date: LocalDate, heading: String): Book {
        if (number.isBlank()) throw InputProblem("புத்தக எண்ணை உள்ளிடவும் / Enter a book number")
        return Book(id, number.trim(), day, date, heading.trim())
    }
    fun weekday(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "திங்கள் / Monday"
        DayOfWeek.TUESDAY -> "செவ்வாய் / Tuesday"
        DayOfWeek.WEDNESDAY -> "புதன் / Wednesday"
        DayOfWeek.THURSDAY -> "வியாழன் / Thursday"
        DayOfWeek.FRIDAY -> "வெள்ளி / Friday"
        DayOfWeek.SATURDAY -> "சனி / Saturday"
        DayOfWeek.SUNDAY -> "ஞாயிறு / Sunday"
    }
}
