package com.universalrp.updates

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.telephony.TelephonyManager

/**
 * "Is a phone call happening right now?"
 *
 * Live Guard speaks through the media stream. Speaking while the user is on a call is
 * never acceptable: it is heard by the other side, it fights the call audio, and on many
 * phones it ducks or drops the speaker path. Oor Arivippu learned this first and queues
 * its announcements until the call ends; this object is the same rule, shared so every
 * app applies it identically.
 *
 * Two signals, cheapest first:
 *
 *  1. [AudioManager.getMode] — no permission, flips to `MODE_IN_CALL` /
 *     `MODE_IN_COMMUNICATION` the moment a call is active and back when it ends.
 *     `MODE_RINGTONE` covers an incoming call before it is answered.
 *  2. [TelephonyManager.getCallState] — catches the ringing window on phones that keep
 *     the audio mode at normal until pickup. Reading it needs no runtime permission, but
 *     it is wrapped anyway because OEM telephony stacks differ.
 *
 * Deliberately not a listener: the answer is needed at the instant of speaking, and a
 * stale flag would either talk over a call or stay silent after one ended.
 */
object CallGuard {

    /** True while ringing, dialling, in a call, or screen-calling. */
    fun inCall(context: Context): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        when (audio?.mode) {
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION,
            AudioManager.MODE_RINGTONE -> return true
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                audio?.mode == AudioManager.MODE_CALL_SCREENING
            ) return true
        }

        val state = runCatching {
            (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.callState
        }.getOrNull() ?: return false

        return state != TelephonyManager.CALL_STATE_IDLE
    }
}
