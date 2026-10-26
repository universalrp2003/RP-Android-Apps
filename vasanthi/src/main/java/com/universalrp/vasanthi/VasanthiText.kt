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

/**
 * Whether a line is written in the Latin alphabet, which for this app means it is English.
 *
 * The recogniser is asked for Tamil and usually gives Tamil, but not always. Most of the
 * letters have to be Latin for the line to count, because a Tamil sentence that happens to
 * contain one English word in it is still a Tamil sentence.
 */
fun isLatinScript(text: String): Boolean {
    val letters = text.count { it.isLetter() }
    if (letters == 0) return false
    val latin = text.count { it.code in 0x0041..0x005A || it.code in 0x0061..0x007A }
    return latin * 2 > letters
}

/**
 * A line naming why recognition is producing no text, when it is not.
 *
 * Returns an empty string when nothing is wrong, so callers can append it unconditionally. A
 * missing Tamil pack, no internet and a microphone somebody else is holding each need a different
 * fix, so they are named separately rather than folded into one apology.
 */
fun recognitionProblemNote(problem: RecognitionProblem): String {
    val (ta, en) = when (problem) {
        RecognitionProblem.NO_RECOGNISER -> Pair(
            "\nஇந்தத் தொலைபேசியில் பேச்சு அறிதல் சேவை இல்லை.",
            "\nThis phone has no speech recognition service.",
        )
        RecognitionProblem.NO_NETWORK -> Pair(
            "\nஇணையம் இல்லை. இணைப்பைச் சரிபார்க்கவும்.",
            "\nNo internet. Check the connection.",
        )
        RecognitionProblem.MIC_BUSY -> Pair(
            "\nமைக் வேறு செயலியால் பயன்பாட்டில் உள்ளது.",
            "\nThe microphone is in use by another app.",
        )
        RecognitionProblem.OTHER -> Pair(
            "\nபேச்சு அறிதல் தொடங்க முடியவில்லை.",
            "\nSpeech recognition could not start.",
        )
        RecognitionProblem.NONE -> return ""
    }
    return tr(ta, en)
}

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
