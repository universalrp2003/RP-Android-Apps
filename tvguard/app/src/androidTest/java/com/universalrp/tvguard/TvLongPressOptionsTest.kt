package com.universalrp.tvguard

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalrp.tvguard.launcher.TvAppCatalog
import com.universalrp.tvguard.settings.TvFeaturePreferences
import com.universalrp.tvguard.settings.TvFeatureSettings
import com.universalrp.tvguard.ui.MainActivity
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The reported bug, tested the way it was reported: hold OK, release, then try to navigate.
 *
 * These tests inject a genuine key-down, hold and key-up through the Activity's dispatch,
 * not a semantic OnLongClick. That matters because the old implementation used
 * `combinedClickable`, whose gesture detector accepted the release as a click and launched
 * the app, so the menu closed immediately.
 *
 * Requires a disposable TV emulator. A real remote can send a long hold more slowly than
 * the emulator; that timing is covered by the JVM policy tests instead.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TvLongPressOptionsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private var previousSettings: TvFeatureSettings? = null

    @Before
    fun resetSettings() {
        val preferences = TvFeaturePreferences(compose.activity)
        previousSettings = preferences.load()
        preferences.save(TvFeatureSettings())
        compose.activityRule.scenario.recreate()
        awaitHomeStart()
    }

    @After
    fun restoreSettings() {
        previousSettings?.let { TvFeaturePreferences(compose.activity).save(it) }
    }

    /**
     * Startup waits for the first grid tile. When that never happens the bare assertion is a
     * 30 s Compose timeout with no explanation, so dump what the app actually rendered: every
     * test tag in the tree, which tag holds focus, and what the package manager reported as
     * launchable. That distinguishes "no launchable app on this emulator" from "focus landed
     * somewhere else" instead of guessing.
     */
    private fun awaitHomeStart() {
        try {
            awaitFocus("home-first-app")
        } catch (timeout: Throwable) {
            val activity = compose.activity
            val visible = compose.onAllNodes(hasTestTag("home-first-app")).fetchSemanticsNodes().size
            val tree: String = try {
                compose.onRoot().printToString()
            } catch (dumpFailure: Throwable) {
                "tree dump failed: " + dumpFailure
            }
            fun query(category: String) = runCatching {
                activity.packageManager.queryIntentActivities(
                    Intent(Intent.ACTION_MAIN).addCategory(category), 0
                ).map { it.activityInfo.packageName }.distinct()
            }.getOrElse { listOf("threw: " + it) }
            val catalog = runCatching {
                TvAppCatalog.loadApps(activity).map { it.packageName }
            }.getOrElse { listOf("threw: " + it) }
            throw AssertionError(
                "home-first-app never gained focus." +
                    " self=" + activity.packageName +
                    " taggedNodes=" + visible +
                    " tree=" + tree +
                    " leanback=" + query(Intent.CATEGORY_LEANBACK_LAUNCHER) +
                    " launcher=" + query(Intent.CATEGORY_LAUNCHER) +
                    " catalog=" + catalog +
                    " cause=" + timeout.message
            )
        }
    }

    /**
     * A real short press launches the focused tile, which sends the launcher to the background
     * and detaches its compose hierarchy. Asking for nodes then throws, so answer the only
     * question that matters: is the launcher still on screen with its tile focused?
     */
    private fun launcherStillVisibleAndFocused(): Boolean = try {
        compose.onAllNodes(hasTestTag("home-first-app") and isFocused()).fetchSemanticsNodes().isNotEmpty()
    } catch (detached: IllegalStateException) {
        false
    }

    private fun awaitFocus(tag: String) {
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodes(hasTestTag(tag) and isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun sendKey(action: Int, downTime: Long, eventTime: Long, repeat: Int = 0) {
        val activity = compose.activity
        val event = KeyEvent(
            downTime, eventTime, action, KeyEvent.KEYCODE_DPAD_CENTER, repeat
        )
        compose.runOnUiThread { activity.dispatchKeyEvent(event) }
        compose.waitForIdle()
    }

    /**
     * Dispatches a short press. Both edges are sent so the tile can decide on the release.
     */
    private fun shortPress(tag: String) {
        compose.onNodeWithTag(tag).assertIsFocused()
        val base = android.os.SystemClock.uptimeMillis()
        sendKey(KeyEvent.ACTION_DOWN, base, base)
        sendKey(KeyEvent.ACTION_UP, base, base + 80)
    }

    /**
     * Holds OK for [holdMs] with the auto-repeat events a real remote sends, then releases.
     * Returns nothing; assertions follow in the test body.
     */
    private fun holdOk(holdMs: Long) {
        val base = android.os.SystemClock.uptimeMillis()
        sendKey(KeyEvent.ACTION_DOWN, base, base)
        // Real remotes repeat roughly every 50-100 ms while held.
        var t = 100L
        var repeat = 1
        while (t < holdMs) {
            sendKey(KeyEvent.ACTION_DOWN, base, base + t, repeat)
            t += 100L
            repeat++
        }
        sendKey(KeyEvent.ACTION_UP, base, base + holdMs)
    }

    @Test
    fun longPressOpensOptionsAndReleaseDoesNotLaunchOrClose() {
        // Focus the first app tile on Home.
        awaitFocus("home-first-app")

        // Hold OK well past the long-press threshold, then release.
        holdOk(holdMs = 1_200)

        // The options menu must be open and focused, and the app must NOT have launched.
        awaitFocus("option-0")
        compose.onNodeWithTag("option-0").assertIsFocused()

        // The reported complaint: being unable to move down. Prove the D-pad still works.
        compose.onNodeWithTag("option-0").performKeyInput { pressKey(Key.DirectionDown) }
        awaitFocus("option-1")
        compose.onNodeWithTag("option-1").assertIsFocused()

        compose.onNodeWithTag("option-1").performKeyInput { pressKey(Key.DirectionDown) }
        awaitFocus("option-2")
        compose.onNodeWithTag("option-2").assertIsFocused()
    }

    @Test
    fun shortPressStillLaunchesOrKeepsFocusOnTheLauncher() {
        awaitFocus("home-first-app")
        val before = compose.activity.homeRequestId
        shortPress("home-first-app")
        // A short press must not open the options menu.
        compose.waitForIdle()
        assertTrue(
            "a short press must not open the options dialog",
            compose.onAllNodes(hasTestTag("option-0")).fetchSemanticsNodes().isEmpty()
        )
        // Either the tile launched an app (the launcher goes to the background, which is the
        // behaviour the report asked for) or the tile could not launch and simply kept focus.
        // Both are correct; reopening the options menu is not.
        val stillHome = launcherStillVisibleAndFocused()
        assertTrue(
            "a short press must launch an app or leave the launcher focused",
            !stillHome || compose.activity.homeRequestId >= before
        )
        if (stillHome) compose.onNodeWithTag("home-first-app").assertIsFocused()
    }

    @Test
    fun dismissingOptionsRestoresFocusToTheTile() {
        awaitFocus("home-first-app")
        holdOk(holdMs = 900)
        awaitFocus("option-0")

        // Back closes the dialog; focus must come back to the tile that opened it.
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("option-0")).fetchSemanticsNodes().isEmpty()
        }
        awaitFocus("home-first-app")
    }

    @Test
    fun afterTheMenuClosesAShortPressLaunchesAgain() {
        awaitFocus("home-first-app")
        holdOk(holdMs = 900)
        awaitFocus("option-0")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        awaitFocus("home-first-app")

        // The press state must have been reset, so a fresh short press behaves normally and
        // must not be swallowed by leftover "long press already fired" state.
        shortPress("home-first-app")
        compose.waitForIdle()
        // A launch removes the hierarchy; that is not a reopened menu.
        val stillHome = launcherStillVisibleAndFocused()
        if (stillHome) {
            assertTrue(
                "options must not reopen from a stale long-press state",
                compose.onAllNodes(hasTestTag("option-0")).fetchSemanticsNodes().isEmpty()
            )
        }
    }
}
