package com.universalrp.appdisabler

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
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
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("disabler-nav-setup").assertExists()
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
