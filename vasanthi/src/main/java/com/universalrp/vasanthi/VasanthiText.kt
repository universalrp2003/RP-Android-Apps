package com.universalrp.vasanthi

import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState

/**
 * Tamil-first copy for Vasanthi AI.
 *
 * The user is deaf from birth and reads Tamil, so Tamil is the default and the only language the
 * sound names are written in. English is there for the developer and for anyone helping set the
 * app up, not as an equal alternative.
 *
 * This is deliberately a plain function rather than a @Composable: the floating window is built
 * by a Service, which has no composition scope to borrow.
 */
fun tr(ta: String, en: String): String =
    if (AppLanguageState.current == AppLanguage.TAMIL) ta else en

/** A number formatted the way an Indian screen reads it: 1,23,456. */
fun indianGrouping(value: Long): String {
    val negative = value < 0
    val digits = kotlin.math.abs(value).toString()
    val grouped = when {
        digits.length <= 3 -> digits
        else -> {
            val last3 = digits.substring(digits.length - 3)
            var rest = digits.substring(0, digits.length - 3)
            val parts = mutableListOf<String>()
            while (rest.length > 2) {
                parts.add(0, rest.substring(rest.length - 2))
                rest = rest.substring(0, rest.length - 2)
            }
            if (rest.isNotEmpty()) parts.add(0, rest)
            parts.joinToString(",") + "," + last3
        }
    }
    return if (negative) "-$grouped" else grouped
}
