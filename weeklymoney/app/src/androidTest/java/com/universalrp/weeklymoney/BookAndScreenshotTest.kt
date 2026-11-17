package com.universalrp.weeklymoney

import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** All identities are invented. Fixture writes are test setup, not a production import shortcut. */
@RunWith(AndroidJUnit4::class)
class BookAndScreenshotTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val password = "synthetic-passphrase-only"
    private val today = LocalDate.now()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun waitFor(tag: String) {
        android.util.Log.i("WeeklyMoneyRegression", "wait $tag")
        try { ui.waitUntil(20000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() } }
        catch (e: Throwable) { throw AssertionError("Missing $tag\n" + ui.onAllNodes(isRoot(), useUnmergedTree=true).onLast().printToString(maxDepth=25)) }
    }
    private fun closed(tag: String) {
        android.util.Log.i("WeeklyMoneyRegression", "closed $tag")
        ui.waitUntil(20000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }
        ui.waitForIdle()
    }
    private fun secure(expected: Boolean) = ui.runOnIdle {
        assertEquals(expected, ui.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
    }
    private fun unlock() {
        waitFor("unlockPassword")
        ui.onNodeWithTag("unlockPassword").performScrollTo().performTextInput(password)
        ui.onNodeWithTag("unlockContinue").assertIsEnabled().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        waitFor("bookHeader")
        openBook()
    }
    private fun openBook() {
        val file = File(context.filesDir, "notebooks.vvb")
        val state = NotebookState.decode(BackupCrypto.open(file.readBytes(), password.toCharArray()))
        ui.onNodeWithTag("openBook-${state.books.first().id}").performScrollTo().performClick()
        waitFor("ledgerBook")
    }
    private fun history(enabled: Boolean) {
        ui.onNodeWithTag("booksHome").performClick()
        ui.onNodeWithTag("settingsMenu").performClick()
        waitFor("historyEntrySetting")
        ui.onNodeWithTag("historyEntrySetting").performScrollTo().performClick()
        if (enabled) ui.onNodeWithTag("historyEntrySetting").assertIsOn() else ui.onNodeWithTag("historyEntrySetting").assertIsOff()
        ui.onNodeWithTag("settingsMenu").performClick()
        closed("historyEntrySetting")
        openBook()
    }
    private fun searchFirstPage() {
        ui.onNodeWithTag("ledgerSearch").performTextReplacement("Synthetic")
        ui.onNodeWithTag("ledgerSearch").performImeAction()
        ui.waitForIdle()
    }
    private fun fixtureBackdate(sequence: Int) {
        ui.waitForIdle()
        val file = File(context.filesDir, "notebooks.vvb")
        val state = NotebookState.decode(BackupCrypto.open(file.readBytes(), password.toCharArray()))
        val l = state.effectiveLoans().single { it.sequence == sequence }
        val next = state.correctLoan(l, l.copy(started=today.minusWeeks(1)))
        file.writeBytes(BackupCrypto.seal(next.encode(),password.toCharArray()))
        ui.activityRule.scenario.recreate()
        unlock()
    }
    @Test fun calendarOverviewTodayEntryHistoryAndHolidayRegression() {
        ui.runOnIdle {
            context.getSharedPreferences("preferences", android.content.Context.MODE_PRIVATE).edit().putBoolean("biometricWanted", false).commit()
        }
        secure(true)
        ui.onNodeWithTag("allowScreenshots").performClick(); secure(false)
        ui.onNodeWithTag("ownerName").performScrollTo().performTextInput("Synthetic Owner")
        ui.onNodeWithTag("unlockPassword").performScrollTo().performTextInput(password)
        ui.onNodeWithTag("confirmPassword").performScrollTo().performTextInput(password)
        ui.onNodeWithTag("unlockContinue").assertIsEnabled().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        waitFor("newBook")
        ui.onNodeWithTag("settingsMenu").performClick()
        ui.onNodeWithTag("historyEntrySetting").assertIsOn()
        ui.onNodeWithTag("historyEntrySetting").performScrollTo().performClick()
        ui.onNodeWithTag("settingsMenu").performClick()
        ui.onNodeWithTag("guidePage").performClick(); waitFor("closeInformation")
        ui.onNodeWithTag("closeInformation").performClick(); closed("closeInformation")
        ui.onNodeWithTag("aboutPage").performClick(); waitFor("closeInformation")
        ui.onNodeWithTag("closeInformation").performClick(); closed("closeInformation")
        ui.onNodeWithTag("newBook").performClick(); waitFor("bookSave")
        ui.onNodeWithTag("bookSave").performClick(); waitFor("bookError")
        ui.onNodeWithTag("bookNumber").performScrollTo().performTextInput("Test ௧")
        ui.onNodeWithTag("weekday-${today.dayOfWeek.value}").performScrollTo().performClick()
        ui.onNodeWithTag("bookDate").performScrollTo().performClick()
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.onNodeWithTag("bookSave").performClick(); waitFor("bookHeader")
        ui.activityRule.scenario.recreate(); unlock(); secure(false)
        ui.onNodeWithTag("newLoan").performClick()
        ui.onNodeWithTag("historicalLoan").assertDoesNotExist()
        ui.onNodeWithTag("loanPlace").performScrollTo().performTextInput("Synthetic Place")
        ui.onNodeWithTag("loanFamily").performScrollTo().performTextInput("Synthetic Family")
        ui.onNodeWithTag("loanName").performScrollTo().performTextInput("Synthetic Customer")
        ui.onNodeWithTag("loanContact").performScrollTo().performTextInput("+91 00000 00000")
        ui.onNodeWithTag("loanAmount").performScrollTo().performTextInput("1000")
        ui.onNodeWithTag("loanSave").performClick(); closed("loanSave")
        ui.onNodeWithText("Synthetic Customer").performScrollTo().performClick()
        waitFor("todayAmount")
        ui.onNodeWithTag("copyContact").assertTextContains("+91 00000 00000", substring = true)
        ui.onNodeWithTag("copyContact").performScrollTo().performClick()
        ui.runOnIdle {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            assertEquals("+91 00000 00000", clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
        ui.onNodeWithTag("dueDate-1").assertTextEquals(today.plusWeeks(1).format(DateTimeFormatter.ofPattern("dd-MM-yy")))
        ui.onNodeWithTag("dueDate-10").assertExists()
        ui.onNodeWithTag("allTenWeeks").assertDoesNotExist()
        ui.onNodeWithTag("receiptSave").assertIsNotEnabled() // disbursement day is NOT week one
        ui.onNodeWithTag("receiptClose").performClick(); closed("receiptClose")
        fixtureBackdate(1) // invented existing loan whose first scheduled collection is today
        ui.onNodeWithText("Synthetic Customer").performScrollTo().performClick(); waitFor("todayAmount")
        ui.onNodeWithTag("todayAmount").performScrollTo().performTextInput("100")
        ui.onNodeWithTag("newBalance").assertTextContains("900",substring=true)
        ui.onNodeWithTag("receiptSave").performClick(); closed("receiptSave")
        ui.onNodeWithTag("bookHeader").assertDoesNotExist()
        ui.onNodeWithTag("noLoanAmountTotal").onChildren().onFirst().assertTextEquals("")
        ui.onNodeWithTag("weeklyTotal-1").onChildren().onFirst().assertTextEquals("100")
        ui.onNodeWithTag("weeklyTotal-2").onChildren().onFirst().assertTextEquals("0")
        ui.onNodeWithTag("newLoan").performClick()
        ui.onNodeWithTag("loanPlace").performScrollTo().performTextInput("Synthetic Place")
        ui.onNodeWithTag("loanFamily").performScrollTo().performTextInput("Synthetic Family")
        ui.onNodeWithTag("loanName").performScrollTo().performTextInput("Synthetic Customer")
        ui.onNodeWithTag("loanAmount").performScrollTo().performTextInput("250")
        ui.onNodeWithTag("loanSave").performClick(); waitFor("duplicatePrompt")
        ui.onNodeWithText("ஆம், இவருக்குப் புதிய கடன் / Yes, new loan for this person").performScrollTo().performClick()
        ui.onNodeWithTag("loanSave").performClick(); closed("loanSave")
        history(true)
        searchFirstPage()
        ui.onNodeWithText("Synthetic Customer I").performScrollTo().performClick(); waitFor("receiptAmount-1")
        ui.onNodeWithTag("allTenWeeks").assertDoesNotExist()
        ui.onNodeWithTag("editLoanDetails").performScrollTo().performClick(); waitFor("openingRed")
        ui.onNodeWithTag("loanContact").performScrollTo().assertTextContains("+91 00000 00000")
        ui.onNodeWithTag("historicalLoan").assertDoesNotExist()
        ui.onNodeWithTag("openingRed").performScrollTo().performClick()
        ui.onNodeWithTag("loanSave").performClick(); closed("loanSave")
        searchFirstPage()
        ui.onNodeWithText("Synthetic Customer I").performScrollTo().performClick(); waitFor("receiptAmount-1")
        ui.onNodeWithTag("receiptAmount-1").performScrollTo().performTextReplacement("50")
        ui.onNodeWithTag("receiptSave").performClick(); closed("receiptSave")
        history(false)
        searchFirstPage()
        ui.onNodeWithText("Synthetic Customer I").performScrollTo().performClick(); waitFor("todayAmount")
        ui.onNodeWithTag("overviewAmount-1", useUnmergedTree = true).assertTextEquals("50")
        ui.onNodeWithTag("todayAmount").performScrollTo().performTextInput("950")
        ui.onNodeWithTag("newBalance").assertTextEquals("புதிய மீதி / New balance ₹0")
        ui.onNodeWithTag("receiptSave").performClick(); waitFor("confirmRenew")
        ui.onNodeWithTag("confirmRenew").performClick(); waitFor("loanAmount")
        ui.onNodeWithTag("loanAmount").performScrollTo().performTextInput("500")
        ui.onNodeWithTag("historicalLoan").assertDoesNotExist()
        ui.onNodeWithTag("loanSave").performClick(); closed("loanSave")
        fixtureBackdate(2)
        ui.onNodeWithTag("finishCollection").performClick(); waitFor("confirmFinishCollection")
        ui.onNodeWithText("ரத்து / Cancel").performClick(); closed("confirmFinishCollection")
        ui.onNodeWithTag("finishCollection").performClick(); waitFor("confirmFinishCollection")
        ui.onNodeWithTag("confirmFinishCollection").performClick(); closed("confirmFinishCollection")
        waitFor("closeSummary"); ui.onNodeWithTag("closeSummary").performClick(); closed("closeSummary")
        ui.waitUntil(20000) { !ui.onNodeWithTag("finishCollection").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        ui.onNodeWithTag("finishCollection").performClick(); waitFor("confirmFinishCollection")
        ui.onNodeWithTag("confirmFinishCollection").assertIsEnabled()
        ui.onNodeWithText("ரத்து / Cancel").performClick(); closed("confirmFinishCollection")
        searchFirstPage()
        ui.onNodeWithText("Synthetic Customer I").performScrollTo().performClick(); waitFor("todayAmount")
        ui.onNodeWithTag("overviewAmount-1", useUnmergedTree = true).assertTextEquals("–")
        ui.onNodeWithTag("receiptClose").performClick(); closed("receiptClose")
        ui.onNodeWithTag("booksHome").performClick()
        ui.onNodeWithTag("settingsMenu").performClick()
        ui.onNodeWithTag("bookHolidays").performScrollTo().performClick(); waitFor("holidayDate")
        ui.onNodeWithTag("holidayReason").performScrollTo().performTextInput("Synthetic closure")
        ui.onNodeWithTag("holidaySave").performClick(); waitFor("holidayError") // receipts already exist today
        ui.onNodeWithText("ரத்து / Cancel").performClick(); closed("holidaySave")
        ui.onNodeWithTag("settingsMenu").performClick(); closed("historyEntrySetting")
        openBook()
        ui.onNodeWithTag("pinchToggle").performClick()
        ui.onNodeWithTag("ledgerBook").performTouchInput {
            pinch(center-Offset(55f,0f),center+Offset(55f,0f),center-Offset(130f,0f),center+Offset(130f,0f),durationMillis=500)
        }
        ui.waitUntil(15000) { !ui.onNodeWithTag("bookZoom").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString().contains("100%") }
        ui.onNodeWithTag("resetBookZoom").performClick()
        ui.onNodeWithTag("ledgerBook").performTouchInput {
            pinch(center-Offset(140f,0f),center+Offset(140f,0f),center-Offset(20f,0f),center+Offset(20f,0f),durationMillis=500)
        }
        val image = ui.onNodeWithTag("ledgerBook").captureToImage().asAndroidBitmap()
        val resolver=context.contentResolver
        val values=android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"synthetic-notebook.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/WeeklyMoneySynthetic")
        }
        val uri=requireNotNull(resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
        requireNotNull(resolver.openOutputStream(uri)).use { assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)) }
        ui.onNodeWithTag("resetBookZoom").performClick()
        val resolverBackup = context.contentResolver
        resolverBackup.query(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(android.provider.MediaStore.Downloads._ID),
            "${android.provider.MediaStore.Downloads.DISPLAY_NAME} LIKE ?", arrayOf("WeeklyMoney-%after-collection%.vvasool"), null)!!.use { cursor ->
            assertTrue("Post-collection backup missing", cursor.moveToFirst())
            val backupUri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0))
            val bytes = resolverBackup.openInputStream(backupUri)!!.use { it.readBytes() }
            val backupState = NotebookState.decode(BackupCrypto.open(bytes, password.toCharArray()))
            assertTrue(backupState.audits.isNotEmpty())
        }
        val demo = InstrumentationRegistry.getInstrumentation().context.assets.open("WeeklyMoney-DEMO.vvasool").use { it.readBytes() }
        val restored=NotebookState.decode(BackupCrypto.open(demo,"Demo-Weekly-2026".toCharArray()))
        assertEquals(LocalDate.of(2026,11,15),restored.dueDate(restored.loans.single { it.id=="demo-sun-first" },7))
        ui.onNodeWithTag("showSummary").performClick(); waitFor("closeSummary")
        ui.onNodeWithText("புதிய கடன்கள்").assertExists()
        ui.onNodeWithTag("closeSummary").performClick()
        ui.onNodeWithTag("settingsMenu").assertDoesNotExist()
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        ui.onNodeWithTag("unlockPassword").assertDoesNotExist() // default five-minute background grace
        ui.onNodeWithTag("booksHome").performClick()
        ui.onNodeWithTag("allowScreenshots").performClick(); secure(true)
        ui.activityRule.scenario.recreate(); waitFor("unlockPassword"); secure(true)
        unlock()
        ui.onNodeWithTag("booksHome").performClick()
        ui.onNodeWithTag("settingsMenu").performClick()
        ui.onNodeWithTag("lockMinutes-0").performScrollTo().performClick()
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        waitFor("unlockPassword")
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            var rejected = false
            ui.runOnIdle {
                val gate = BiometricGate(ui.activity)
                assertFalse(gate.configured())
                gate.authenticate(success = { fail("No key must never authenticate") }, failure = { rejected = true })
            }
            ui.waitUntil(5000) { rejected }
            ui.onNodeWithTag("unlockPassword").assertExists()
        }
        unlock()
        ui.onNodeWithTag("booksHome").performClick()
        ui.onNodeWithTag("settingsMenu").performClick()
        ui.onNodeWithTag("continueBook").performScrollTo().performClick(); waitFor("bookSave")
        ui.onNodeWithTag("bookNumber").performTextInput("Synthetic volume 2")
        ui.onNodeWithTag("bookSave").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        closed("bookSave")
        val continued = NotebookState.decode(BackupCrypto.open(File(context.filesDir, "notebooks.vvb").readBytes(), password.toCharArray()))
        val transfer = continued.continuations.single()
        assertTrue(continued.isArchived(transfer.fromBook))
        assertEquals("Synthetic volume 2", continued.books.single { it.id == transfer.toBook }.number)
        assertTrue(transfer.loanIds.isNotEmpty())
        transfer.loanIds.forEach { loanId ->
            assertEquals(transfer.toBook, continued.activeBookId(continued.effectiveLoans().single { it.id == loanId }))
        }
        // Export only the invented encrypted test fixture for the separate signed-release upgrade test.
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "weeklymoney-synthetic-upgrade.vvb")
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/WeeklyMoneySynthetic")
            }
            val resolver = context.contentResolver
            val uri = checkNotNull(resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            checkNotNull(resolver.openOutputStream(uri)).use { it.write(File(context.filesDir, "notebooks.vvb").readBytes()) }
        }
        assertEquals(continued, NotebookState.decode(continued.encode()))
        ui.onNodeWithTag("openArchivedVolume").performScrollTo().performClick(); waitFor("ledgerBook")
        ui.onNodeWithTag("finishCollection").assertDoesNotExist()
        // Change the master password without altering previously exported encrypted bytes.
        val oldBytes = File(context.filesDir, "notebooks.vvb").readBytes()
        ui.onNodeWithTag("booksHome").performClick()
        if (ui.onAllNodesWithTag("changeMasterPassword").fetchSemanticsNodes().isEmpty()) ui.onNodeWithTag("settingsMenu").performClick()
        ui.onNodeWithTag("changeMasterPassword").performScrollTo().performClick()
        ui.onNodeWithTag("oldMasterPassword").performScrollTo().performTextInput(password)
        ui.onNodeWithTag("newMasterPassword").performScrollTo().performTextInput("synthetic-new-password")
        ui.onNodeWithTag("confirmNewMasterPassword").performScrollTo().performTextInput("synthetic-new-password")
        ui.onNodeWithTag("saveMasterPassword").performClick(); closed("saveMasterPassword")
        val changed = File(context.filesDir, "notebooks.vvb").readBytes()
        assertEquals(continued, NotebookState.decode(BackupCrypto.open(oldBytes, password.toCharArray())))
        assertTrue(runCatching { BackupCrypto.open(oldBytes, "synthetic-new-password".toCharArray()) }.isFailure)
        assertTrue(runCatching { BackupCrypto.open(changed, password.toCharArray()) }.isFailure)
        assertEquals(continued, NotebookState.decode(BackupCrypto.open(changed, "synthetic-new-password".toCharArray())))
        ui.activityRule.scenario.recreate(); waitFor("unlockPassword")
        ui.onNodeWithTag("unlockPassword").performScrollTo().performTextInput("synthetic-new-password")
        ui.onNodeWithTag("unlockContinue").performClick(); waitFor("bookHeader")
    }
}
