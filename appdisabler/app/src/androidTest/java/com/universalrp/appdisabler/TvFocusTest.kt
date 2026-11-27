package com.universalrp.appdisabler

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalrp.appdisabler.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable TV emulator only. Never grant Shizuku or disable a real package in these tests. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TvFocusTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun awaitFocus(tag: String) {
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodes(hasTestTag(tag) and isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun requestFocus(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        awaitFocus(tag)
    }

    @Test fun navigationAndPlanAreReachableByRemote() {
        awaitFocus("disabler-nav-setup")
        compose.onNodeWithTag("disabler-nav-setup").performKeyInput { pressKey(Key.DirectionRight) }
        awaitFocus("disabler-nav-apps")
        compose.onNodeWithTag("disabler-nav-apps").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodes(hasTestTag("disabler-compare")).fetchSemanticsNodes().isNotEmpty()
        }
        requestFocus("disabler-compare")
        compose.onNodeWithTag("disabler-compare").performKeyInput { pressKey(Key.DirectionRight) }
        awaitFocus("disabler-plan")
        compose.onNodeWithTag("disabler-plan").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("disabler-plan-dialog").assertExists()
    }

    @Test fun guideBackButtonAndSystemBackReturnToSetup() {
        compose.onNodeWithTag("disabler-setup-guide").performScrollTo().performClick()
        awaitFocus("disabler-guide-back")
        compose.onNodeWithTag("disabler-guide-back").performKeyInput { pressKey(Key.DirectionDown) }
        awaitFocus("disabler-guide-start")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("disabler-nav-setup").assertExists()
        // Read-only guidance is reachable on TV without granting Shizuku or changing a package.
        compose.onNodeWithText("Any app may be needed", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Back up important data separately", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun searchAndFilterControlsWorkWithRemote() {
        compose.onNodeWithTag("disabler-nav-apps").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodes(hasTestTag("disabler-search-open")).fetchSemanticsNodes().isNotEmpty()
        }
        requestFocus("disabler-search-open")
        compose.onNodeWithTag("disabler-search-open").performKeyInput { pressKey(Key.DirectionCenter) }
        requestFocus("disabler-search-field")
        compose.onNodeWithTag("disabler-search-field").performTextInput("no-such-package-for-test")
        compose.onNodeWithTag("disabler-search-close").performClick()
        compose.onNodeWithTag("disabler-search-field").assertDoesNotExist()
        requestFocus("disabler-filter-all")
        compose.onNodeWithTag("disabler-filter-all").performKeyInput { pressKey(Key.DirectionRight) }
        awaitFocus("disabler-filter-system")
        compose.onNodeWithTag("disabler-filter-system").performKeyInput { pressKey(Key.DirectionCenter) }
    }

    @Test fun appCardOpensDetailsWithoutDisablingAnything() {
        compose.onNodeWithTag("disabler-nav-apps").performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodes(hasTestTag("disabler-first-app")).fetchSemanticsNodes().isNotEmpty()
        }
        requestFocus("disabler-first-app")
        compose.onNodeWithTag("disabler-first-app").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("disabler-details-dialog").assertExists()
    }
}
