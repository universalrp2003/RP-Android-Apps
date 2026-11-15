package com.universalrp.weeklymoney

import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
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
    private fun waitFor(tag: String) {
        android.util.Log.i("WeeklyMoneyRegression", "wait $tag")
        try {
            ui.waitUntil(15000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            val roots = ui.onAllNodes(isRoot(), useUnmergedTree = true)
            val tree = (0 until roots.fetchSemanticsNodes().size).reversed().joinToString("\n") { roots[it].printToString(maxDepth = 40) }
            val cells = ui.onAllNodesWithText("Synthetic Place", useUnmergedTree = true)
            val targets = (0 until cells.fetchSemanticsNodes().size).joinToString("\n") { cells[it].printToString(maxDepth = 2) }
            val tags = ui.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.TestTag), useUnmergedTree = true).fetchSemanticsNodes().joinToString { it.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag] }
            val merged = ui.onAllNodesWithText("Synthetic Place").onFirst().printToString(maxDepth = 6)
            throw AssertionError("Missing $tag. Merged=$merged. Trace=${EntryTrace.snapshot()}. Navigation=${ui.activity.entryDiagnostic()}. Tags=[$tags]. Roots=${roots.fetchSemanticsNodes().size}\nTarget cells:\n$targets\nRoots:\n$tree")
        }
    }
    private fun waitClosed(tag: String) {
        android.util.Log.i("WeeklyMoneyRegression", "close $tag")
        ui.waitUntil(20000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }
        ui.waitForIdle()
    }
    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("test tag starts $prefix") {
        it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith(prefix)
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
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
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
        // Default calendar dates: no YYYY-MM-DD typing in loan or payment forms.
        ui.onNodeWithTag("newLoan").performClick()
        ui.onNodeWithTag("loanPlace").performScrollTo().performTextInput("Synthetic Place")
        ui.onNodeWithTag("loanFamily").performScrollTo().performTextInput("Synthetic Family")
        ui.onNodeWithTag("loanName").performScrollTo().performTextInput("Synthetic Customer")
        ui.onNodeWithTag("loanAmount").performScrollTo().performTextInput("1000")
        ui.onNodeWithTag("loanDate").performScrollTo().assertTextContains(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-uuuu")), substring = true)
        ui.onNodeWithTag("loanSave").performClick()
        waitClosed("loanSave")
        waitFor("ledgerBook")
        val customer = ui.onAllNodes(hasTestTagPrefix("customer-")).onFirst()
        customer.performScrollTo().performClick()
        waitFor("receiptAmount-1")
        ui.onNodeWithTag("receiptAmount-1").performScrollTo().performTextInput("100")
        ui.onNodeWithTag("receiptSave").performClick()
        ui.waitUntil(15000) { ui.onAllNodesWithTag("receiptSave").fetchSemanticsNodes().isEmpty() }
        ui.onNodeWithTag("newLoan").performClick()
        ui.onNodeWithTag("loanPlace").performScrollTo().performTextInput("Synthetic Place")
        ui.onNodeWithTag("loanFamily").performScrollTo().performTextInput("Synthetic Family")
        ui.onNodeWithTag("loanName").performScrollTo().performTextInput("Synthetic Customer")
        ui.onNodeWithTag("loanAmount").performScrollTo().performTextInput("250")
        ui.onNodeWithTag("loanSave").performClick()
        waitFor("duplicatePrompt")
        ui.onNodeWithText("ஆம், இவருக்குப் புதிய கடன் / Yes, new loan for this person").performScrollTo().performClick()
        ui.onNodeWithTag("loanSave").performClick()
        waitClosed("loanSave")
        ui.waitUntil(15000) { ui.onAllNodesWithText("Synthetic Customer II").fetchSemanticsNodes().isNotEmpty() }
        ui.onAllNodesWithText("Synthetic Place").onFirst().performScrollTo().performClick()
        waitFor("receiptAmount-2") // next week auto-selected, not typed
        ui.onNodeWithTag("receiptAmount-2").performScrollTo().performTextInput("900")
        ui.onNodeWithTag("receiptSave").performClick()
        waitFor("confirmRenew")
        ui.onNodeWithTag("confirmRenew").performClick()
        waitFor("loanAmount")
        ui.onNodeWithTag("loanPlace").assertTextContains("Synthetic Place", substring = true)
        ui.onNodeWithTag("loanFamily").assertTextContains("Synthetic Family", substring = true)
        ui.onNodeWithTag("loanName").assertTextContains("Synthetic Customer", substring = true)
        ui.onNodeWithTag("loanAmount").performScrollTo().performTextInput("500")
        ui.onNodeWithTag("historicalLoan").performScrollTo().performClick()
        ui.onNodeWithTag("loanSave").performClick()
        waitClosed("loanSave")
        waitFor("receiptAmount-10") // initial physical-note history exposes all ten editable weeks
        ui.onNodeWithTag("receiptAmount-1").performScrollTo().performTextInput("50")
        ui.onNodeWithTag("receiptSave").performClick()
        ui.waitUntil(15000) { ui.onAllNodesWithTag("receiptSave").fetchSemanticsNodes().isEmpty() }
        ui.onAllNodesWithText("Synthetic Place").onLast().performScrollTo().performClick()
        waitFor("editLoanDetails")
        ui.onNodeWithTag("editLoanDetails").performScrollTo().performClick()
        waitFor("loanPlace")
        ui.onNodeWithTag("loanPlace").performScrollTo().performTextReplacement("Corrected Place")
        ui.onNodeWithTag("loanSave").performClick()
        waitClosed("loanSave")
        ui.onNodeWithText("Corrected Place").assertExists()
        // Two real pointers change the table's layout density, then reset it.
        ui.onNodeWithTag("ledgerBook").performTouchInput {
            pinch(center - Offset(55f, 0f), center + Offset(55f, 0f),
                center - Offset(130f, 0f), center + Offset(130f, 0f), durationMillis = 500)
        }
        ui.waitUntil(15000) { !ui.onNodeWithTag("bookZoom").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString().contains("100%") }
        ui.onNodeWithTag("resetBookZoom").performClick()
        ui.onNodeWithTag("bookZoom").assertTextContains("100%", substring = true)
        // Only loan II is blank for today's week. III has 50, I is completed.
        ui.onNodeWithTag("finishCollection").performClick()
        waitFor("confirmFinishCollection")
        ui.onNodeWithTag("confirmFinishCollection").performClick()
        waitClosed("confirmFinishCollection")
        ui.onNodeWithText("Synthetic Customer II").performScrollTo().performClick()
        waitFor("receiptAmount-2") // closing the collection records week 1 as missed
        ui.onNodeWithTag("week-1").performScrollTo().performClick()
        waitFor("receiptAmount-1")
        ui.onNodeWithTag("receiptAmount-1").assertTextContains("-", substring = true)
        ui.onNodeWithTag("receiptClose").performClick()
        waitClosed("receiptClose")
        ui.onNodeWithText("Synthetic Customer III").performScrollTo().performClick()
        waitFor("receiptAmount-2")
        ui.onNodeWithTag("week-1").performScrollTo().performClick()
        waitFor("receiptAmount-1")
        ui.onNodeWithTag("receiptAmount-1").assertTextContains("50", substring = true)
        ui.onNodeWithTag("receiptClose").performClick()
        waitClosed("receiptClose")
        ui.onNodeWithTag("allowScreenshots").performClick()
        assertSecure(true)
        ui.activityRule.scenario.recreate()
        waitFor("unlockPassword")
        assertSecure(true)
    }
}
