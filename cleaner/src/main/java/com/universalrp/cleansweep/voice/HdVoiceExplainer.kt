package com.universalrp.cleansweep.voice

import android.content.Context
import android.content.SharedPreferences

/**
 * A3 - the one-time explainer for the HD-only voice change.
 *
 * 2.71 removed the phone's built-in system voice from Live Guard entirely and replaced the
 * "HD voice / system voice" switch with a paragraph. Owners who had toggled that switch for two
 * years now come to the Voice screen looking for it and find prose instead, which reads as though
 * the feature was taken away rather than replaced.
 *
 * So the first time the Voice screen is opened after the upgrade, a short, dismissible card
 * explains what happened and why. One tap on "Got it" records it and it never appears again -
 * the point is to answer the question once, not to nag on every visit.
 *
 * The flag lives in plain SharedPreferences because a worker or service can wake the process and
 * read it without an Activity, and because losing it costs nothing worse than showing the card
 * one extra time.
 *
 * The [SharedPreferences] overloads are the real logic; the Context overloads are the wiring.
 * That split is what lets the "shown once" contract be tested without a device.
 */
object HdVoiceExplainer {

    private const val PREFS = "live_guard_hd_voice_explainer"
    private const val KEY_SEEN = "seen"

    fun seen(context: Context): Boolean = seen(prefs(context))

    fun markSeen(context: Context) {
        // commit(), not apply(): this runs from a click handler that is about to dispose the
        // screen, and a lost write only means the owner sees the card twice more.
        markSeen(prefs(context))
    }

    internal fun seen(prefs: SharedPreferences): Boolean = prefs.getBoolean(KEY_SEEN, false)

    internal fun markSeen(prefs: SharedPreferences) {
        prefs.edit().putBoolean(KEY_SEEN, true).commit()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
