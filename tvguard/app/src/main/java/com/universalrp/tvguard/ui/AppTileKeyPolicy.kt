package com.universalrp.tvguard.ui

import android.view.KeyEvent

/**
 * Decides what a physical OK press on an app tile means, from raw key events.
 *
 * Why this exists: `Modifier.combinedClickable` decides long-press from its own gesture
 * detector. On a TV remote, holding OK and then releasing produced a *click* as well, so
 * the app launched and the options menu was dismissed in the same gesture. The user could
 * not move down into the menu.
 *
 * This state machine therefore owns both decisions explicitly:
 *  - short press  -> [Outcome.LAUNCH]  (only ever on key-up, so release cannot double-fire)
 *  - long press   -> [Outcome.OPEN_OPTIONS], either from an auto-repeat while still held or,
 *                    for remotes that send no repeat, when the key is finally released
 *  - after a long press, key-up emits [Outcome.CONSUMED] so no launch happens
 *
 * It is deliberately free of Android/Compose state so JVM unit tests can drive a real
 * key-down / hold / key-up sequence, including D-pad movement afterwards.
 */
object AppTileKeyPolicy {

    /** A long press is at least this long; matches the platform's ~500 ms convention. */
    const val LONG_PRESS_MS = 500L

    enum class Outcome {
        /** Do nothing yet; the key is still down. */
        NONE,

        /** Open the options menu now. Emitted once per long press. */
        OPEN_OPTIONS,

        /** Launch the app. Emitted only on key-up after a short press. */
        LAUNCH,

        /** Swallow the event. Emitted on key-up after a long press. */
        CONSUMED,
    }

    /** Mutable hold state for one tile. */
    class State {
        // Mutable so the sibling policy functions in this file can drive it; the UI only
        // reads isHeld and the test asserts the transitions.
        internal var downTime: Long? = null
        internal var longPressFired: Boolean = false

        val isHeld: Boolean get() = downTime != null
    }

    fun onKeyDown(state: State, eventTime: Long): Outcome {
        if (state.downTime == null) {
            state.downTime = eventTime
            state.longPressFired = false
            return Outcome.NONE
        }
        // Auto-repeat while held: fire the long press once, as soon as the threshold passes.
        if (!state.longPressFired) {
            val down = state.downTime
            if (down != null && eventTime - down >= LONG_PRESS_MS) {
                state.longPressFired = true
                return Outcome.OPEN_OPTIONS
            }
        }
        return Outcome.NONE
    }

    /**
     * The decisive rule for a TV remote.
     *
     * Many remotes (Sony BRAVIA ones included) send a single ACTION_DOWN and then nothing until
     * ACTION_UP: there is no auto-repeat while the key is held. Relying on a repeating key-down
     * to notice a long press therefore worked on the emulator - whose injected events do repeat -
     * and never fired on the real remote, so releasing OK launched the app instead of opening
     * the menu. The hold length must be measured when the key is released.
     */
    fun onKeyUp(state: State, eventTime: Long): Outcome {
        val down = state.downTime
        val fired = state.longPressFired
        state.downTime = null
        state.longPressFired = false
        if (down == null) return Outcome.CONSUMED
        // A long press already fired from an auto-repeat: releasing must not launch or close it.
        if (fired) return Outcome.CONSUMED
        // No repeat arrived, but the key was held long enough: this is still a long press, and
        // it must open the menu on release rather than launch the app.
        if (eventTime - down >= LONG_PRESS_MS) return Outcome.OPEN_OPTIONS
        return Outcome.LAUNCH
    }

    /** Pause/resume or focus loss while held: forget the press without firing anything. */
    fun cancel(state: State) {
        state.downTime = null
        state.longPressFired = false
    }

    /** True when this key event is the OK/center key that should be tracked. */
    fun isActivationKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
}
