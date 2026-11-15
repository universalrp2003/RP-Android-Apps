package com.universalrp.weeklymoney

import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic only. Exercises the user's actual Save button, encrypted persistence and privacy switch. */
@RunWith(AndroidJUnit4::class)
class BookAndScreenshotTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val password = "synthetic-passphrase-only"
    private fun waitFor(tag: String) = ui.waitUntil(15000) {
        ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    private fun assertSecure(expected: Boolean) = ui.runOnIdle {
        val secure = ui.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        assertEquals(expected, secure)
    }
    @Test fun createBookShowInlineErrorsAndKeepScreenshotChoiceAfterRestart() {
        assertSecure(true)
        ui.onNodeWithTag("allowScreenshots").performClick()
        assertSecure(false)
        ui.onNodeWithTag("ownerName").performScrollTo().performTextInput("Synthetic Owner")
        ui.onNodeWithTag("unlockPassword").performScrollTo().performTextInput(password)
        ui.onNodeWithTag("confirmPassword").performScrollTo().performTextInput(password)
        ui.onNodeWithTag("unlockContinue").performScrollTo().performClick()
        waitFor("newBook")
        ui.onNodeWithTag("newBook").performClick()
        ui.onNodeWithTag("bookSave").performClick()
        waitFor("bookError")
        ui.onNodeWithTag("bookError").assertTextContains("Enter a book number", substring = true)
        ui.onNodeWithTag("bookNumber").performScrollTo().performTextInput("Test ௧")
        ui.onNodeWithTag("weekday-1").performScrollTo().performClick()
        ui.onNodeWithTag("bookDate").performScrollTo().performClick()
        androidx.test.espresso.Espresso.pressBack()
        ui.onNodeWithTag("bookSave").performClick()
        waitFor("bookHeader")
        ui.onNodeWithTag("bookHeader").assertTextContains("Test ௧", substring = true)
        // Activity recreation reloads encrypted disk data, not just Compose memory.
        ui.activityRule.scenario.recreate()
        waitFor("unlockPassword")
        assertSecure(false)
        ui.onNodeWithTag("unlockPassword").performScrollTo().performTextInput(password)
        ui.onNodeWithTag("unlockContinue").performScrollTo().performClick()
        waitFor("bookHeader")
        ui.onNodeWithTag("bookHeader").assertTextContains("Test ௧", substring = true)
        ui.onNodeWithTag("allowScreenshots").performClick()
        assertSecure(true)
        ui.activityRule.scenario.recreate()
        waitFor("unlockPassword")
        assertSecure(true)
    }
}
