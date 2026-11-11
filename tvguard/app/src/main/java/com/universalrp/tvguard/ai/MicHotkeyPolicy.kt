package com.universalrp.tvguard.ai

import android.view.KeyEvent

/**
 * Which remote key can wake the microphone, and what it can honestly be claimed to do.
 *
 * What was investigated:
 *  - HOME and BACK are unreliable for an app-level hotkey: the system or the TV firmware
 *    consumes them, and a launcher only sees them while it is the foreground app.
 *  - VOICE_ASSIST / SEARCH / ASSIST reach the app only on remotes that send them, and OEM
 *    firmware often routes them to a built-in assistant instead.
 *  - MENU, INFO, and the colour/media keys usually are delivered to the focused app.
 *
 * So the honest model is: TV Guard can provide a *foreground* microphone shortcut that is
 * guaranteed while TV Guard is on screen, and cannot promise a system-wide one. [Scope]
 * records that distinction so the UI never over-claims.
 *
 * The key list is ordered from most to least likely to be free on a TV; which of them
 * actually arrives can only be confirmed on real hardware, which is why the Expert screen
 * offers a key-capture test rather than a promise.
 */
object MicHotkeyPolicy {

    enum class Scope {
        /** Works while TV Guard is the foreground app; verified by the in-app test. */
        FOREGROUND_ONLY,
    }

    /** Keys tried as microphone shortcuts, best first. */
    val candidates: List<Int> = listOf(
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_INFO,
        KeyEvent.KEYCODE_PROG_RED,
        KeyEvent.KEYCODE_VOICE_ASSIST,
        KeyEvent.KEYCODE_SEARCH,
        KeyEvent.KEYCODE_ASSIST,
    )

    fun label(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_MENU -> "Menu"
        KeyEvent.KEYCODE_INFO -> "Info / i"
        KeyEvent.KEYCODE_PROG_RED -> "Red colour key"
        KeyEvent.KEYCODE_VOICE_ASSIST -> "Voice / microphone key"
        KeyEvent.KEYCODE_SEARCH -> "Search key"
        KeyEvent.KEYCODE_ASSIST -> "Assist key"
        else -> "Key $keyCode"
    }

    /**
     * True when this event should open the microphone instead of being handled normally.
     *
     * A long press of MENU is used because MENU is also the options shortcut on a tile; the
     * tile handler consumes the short press, and this only fires for the long hold.
     */
    fun isMicShortcut(keyCode: Int, action: Int, repeatCount: Int, longPress: Boolean): Boolean {
        if (action != KeyEvent.ACTION_DOWN) return false
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> longPress
            KeyEvent.KEYCODE_INFO -> repeatCount == 0
            KeyEvent.KEYCODE_PROG_RED -> repeatCount == 0
            KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_ASSIST -> repeatCount == 0
            else -> false
        }
    }

    /** Honest wording used by the settings screen. */
    fun scopeDescription(): String =
        "This shortcut works while Android TV Guard is on screen. Android does not let an " +
            "ordinary app capture the remote system-wide, and several keys (including Home and " +
            "Back) are reserved by the system or the TV maker. Whether your particular remote " +
            "delivers the key above can only be confirmed by pressing it in the test below."
}
