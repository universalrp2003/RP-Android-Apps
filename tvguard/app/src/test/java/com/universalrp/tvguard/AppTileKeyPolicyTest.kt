package com.universalrp.tvguard

import android.view.KeyEvent
import com.universalrp.tvguard.ui.AppTileKeyPolicy
import com.universalrp.tvguard.ui.AppTileKeyPolicy.Outcome
import com.universalrp.tvguard.ui.AppTileKeyPolicy.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reported bug: holding OK opened the app on release and the options menu vanished.
 * These tests drive a real key-down / hold / key-up sequence, not a semantic long-click.
 */
class AppTileKeyPolicyTest {

    @Test
    fun shortPressLaunchesExactlyOnceOnRelease() {
        val state = State()
        // Down, then up 120 ms later: a normal tap.
        assertEquals(Outcome.NONE, AppTileKeyPolicy.onKeyDown(state, 1_000))
        assertEquals(Outcome.LAUNCH, AppTileKeyPolicy.onKeyUp(state, 1_120))
        assertFalse(state.isHeld)
    }

    @Test
    fun longPressOpensOptionsAndReleaseDoesNotLaunch() {
        val state = State()
        val down = 5_000L
        assertEquals(Outcome.NONE, AppTileKeyPolicy.onKeyDown(state, down))

        // The remote auto-repeats while held. Nothing fires before the threshold.
        assertEquals(Outcome.NONE, AppTileKeyPolicy.onKeyDown(state, down + 100))
        assertEquals(Outcome.NONE, AppTileKeyPolicy.onKeyDown(state, down + 400))

        // The threshold passes: the menu opens once, while the key is still down.
        assertEquals(Outcome.OPEN_OPTIONS, AppTileKeyPolicy.onKeyDown(state, down + 550))
        // Further repeats must NOT open it again.
        assertEquals(Outcome.NONE, AppTileKeyPolicy.onKeyDown(state, down + 900))
        assertEquals(Outcome.NONE, AppTileKeyPolicy.onKeyDown(state, down + 1_500))

        // Releasing after a long press is swallowed: this is the bug that was reported.
        assertEquals(Outcome.CONSUMED, AppTileKeyPolicy.onKeyUp(state, down + 1_700))
        assertFalse(state.isHeld)
    }

    @Test
    fun longPressWithNoAutoRepeatOpensOptionsOnRelease() {
        // The Sony BRAVIA remote sends one ACTION_DOWN, no repeat, then ACTION_UP. The previous
        // policy only noticed a long press from a repeating key-down, so on the real TV the
        // menu never opened and the release launched the app - exactly what was reported.
        val state = State()
        assertEquals(Outcome.NONE, AppTileKeyPolicy.onKeyDown(state, 10_000))
        // No events in between at all: this is the whole sequence a real remote delivers.
        assertEquals(Outcome.OPEN_OPTIONS, AppTileKeyPolicy.onKeyUp(state, 10_800))
        assertFalse(state.isHeld)
    }

    @Test
    fun holdJustUnderTheThresholdStillLaunches() {
        // The fix must not turn an ordinary press into a menu.
        val state = State()
        assertEquals(Outcome.NONE, AppTileKeyPolicy.onKeyDown(state, 30_000))
        assertEquals(Outcome.LAUNCH, AppTileKeyPolicy.onKeyUp(state, 30_000 + AppTileKeyPolicy.LONG_PRESS_MS - 1))
    }

    @Test
    fun stateIsCleanForTheNextPress() {
        val state = State()
        val down = 20_000L
        AppTileKeyPolicy.onKeyDown(state, down)
        assertEquals(Outcome.OPEN_OPTIONS, AppTileKeyPolicy.onKeyDown(state, down + 600))
        assertEquals(Outcome.CONSUMED, AppTileKeyPolicy.onKeyUp(state, down + 700))

        // A fresh short press after the menu must launch normally.
        AppTileKeyPolicy.onKeyDown(state, down + 5_000)
        assertEquals(Outcome.LAUNCH, AppTileKeyPolicy.onKeyUp(state, down + 5_050))
    }

    @Test
    fun focusLossCancelsWithoutFiring() {
        val state = State()
        AppTileKeyPolicy.onKeyDown(state, 30_000)
        AppTileKeyPolicy.cancel(state)
        assertFalse(state.isHeld)
        // The matching key-up must not launch after a cancel.
        assertEquals(Outcome.CONSUMED, AppTileKeyPolicy.onKeyUp(state, 30_100))
    }

    @Test
    fun strayKeyUpWithoutDownIsConsumed() {
        val state = State()
        assertEquals(Outcome.CONSUMED, AppTileKeyPolicy.onKeyUp(state, 40_000))
    }

    @Test
    fun activationKeysCoverCentreAndEnter() {
        assertTrue(AppTileKeyPolicy.isActivationKey(KeyEvent.KEYCODE_DPAD_CENTER))
        assertTrue(AppTileKeyPolicy.isActivationKey(KeyEvent.KEYCODE_ENTER))
        assertTrue(AppTileKeyPolicy.isActivationKey(KeyEvent.KEYCODE_NUMPAD_ENTER))
        assertFalse(AppTileKeyPolicy.isActivationKey(KeyEvent.KEYCODE_DPAD_DOWN))
        assertFalse(AppTileKeyPolicy.isActivationKey(KeyEvent.KEYCODE_BACK))
    }
}
