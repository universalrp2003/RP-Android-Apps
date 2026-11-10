package com.universalrp.tvguard

import android.content.Intent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalrp.tvguard.settings.TvFeature
import com.universalrp.tvguard.settings.TvFeaturePreferences
import com.universalrp.tvguard.settings.TvFeatureSettings
import com.universalrp.tvguard.ui.MainActivity
import com.universalrp.tvguard.web.LocalWebTransferServer
import java.net.Socket
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run on a disposable TV emulator. The test APK includes a launchable fixture.
 * The explicit Home intent exercises delivery to the launcher; it does not prove
 * that any OEM will route the physical Home key or assistant keys to this app.
 * Optional feature selections are temporarily reset and restored; favorites and
 * hidden apps are untouched.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TvRemoteFocusTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private var previousSettings: TvFeatureSettings? = null

    @Before
    fun useLightweightSettings() {
        val preferences = TvFeaturePreferences(compose.activity)
        previousSettings = preferences.load()
        preferences.save(TvFeatureSettings())
        compose.activityRule.scenario.recreate()
        awaitFocus("home-first-app")
    }

    @After
    fun restoreSettings() {
        LocalWebTransferServer.stop()
        previousSettings?.let { TvFeaturePreferences(compose.activity).save(it) }
    }

    private fun awaitFocus(tag: String) {
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodes(hasTestTag(tag) and isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun press(tag: String, key: Key, next: String) {
        compose.onNodeWithTag(tag).assertIsFocused().performKeyInput { pressKey(key) }
        awaitFocus(next)
    }

    private fun toggleFocused(tag: String) {
        compose.onNodeWithTag(tag).assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
    }

    private fun openExpert() {
        compose.onNodeWithTag("tab-tools").performClick()
        compose.onNodeWithTag("tools-expert").performClick()
        awaitFocus("expert-back")
        press("expert-back", Key.DirectionDown, "expert-master")
    }

    private fun enableFeature(feature: TvFeature) {
        openExpert()
        compose.onNodeWithTag("expert-master").assertIsOff()
        toggleFocused("expert-master")
        compose.onNodeWithTag("expert-master").assertIsOn()
        val featureTag = "feature-${feature.key}"
        compose.onNodeWithTag("expert-list").performScrollToNode(hasTestTag(featureTag))
        compose.onNodeWithTag(featureTag).assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("expert-back").performClick()
    }

    private fun awaitHub(running: Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) { LocalWebTransferServer.isRunning == running }
    }

    private fun hubStatus(path: String): String = Socket("127.0.0.1", LocalWebTransferServer.activePort).use { socket ->
        socket.soTimeout = 3_000
        socket.getOutputStream().write("GET $path HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".toByteArray())
        socket.getOutputStream().flush()
        socket.getInputStream().bufferedReader().readLine()
    }

    private fun sendLauncherHomeIntent() {
        val activity = compose.activity
        val previous = activity.homeRequestId
        compose.runOnUiThread {
            activity.startActivity(Intent(activity, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
        compose.waitUntil(timeoutMillis = 10_000) { activity.homeRequestId != previous }
        awaitFocus("home-first-app")
    }

    @Test
    fun appOptionsDirectionsStayInDialogAndHomeResetsEvenWhileAlreadyHome() {
        repeat(2) {
            compose.onNodeWithTag("home-first-app").performSemanticsAction(SemanticsActions.OnLongClick)
            awaitFocus("option-0")
            press("option-0", Key.DirectionDown, "option-1")
            press("option-1", Key.DirectionRight, "option-2")
            press("option-2", Key.DirectionLeft, "option-1")
            press("option-1", Key.DirectionUp, "option-0")
            press("option-0", Key.DirectionUp, "option-close")
            press("option-close", Key.DirectionDown, "option-0")
            sendLauncherHomeIntent()
            compose.onNodeWithTag("option-0").assertDoesNotExist()
            compose.onNodeWithTag("home-first-app").assertIsFocused()
        }
    }

    @Test
    fun expertSwitchesNavigateWithDpadAndRevokeAndRestoreHub() {
        awaitHub(false)
        compose.onNodeWithTag("tab-share").assertDoesNotExist()
        openExpert()
        compose.onNodeWithTag("expert-master").assertIsOff()
        press("expert-master", Key.DirectionDown, "expert-master")
        toggleFocused("expert-master")
        compose.onNodeWithTag("expert-master").assertIsOn()

        var current = "expert-master"
        for (feature in TvFeature.entries) {
            val tag = "feature-${feature.key}"
            press(current, Key.DirectionDown, tag)
            compose.onNodeWithTag(tag).assertIsOff()
            when (feature) {
                TvFeature.FILE_HUB -> {
                    toggleFocused(tag)
                    compose.onNodeWithTag(tag).assertIsOn()
                    awaitHub(true)
                    assertEquals("HTTP/1.1 403 Forbidden", hubStatus("/api/key?code=0"))
                }
                TvFeature.WEB_REMOTE -> {
                    toggleFocused(tag)
                    compose.onNodeWithTag(tag).assertIsOn()
                    assertEquals("HTTP/1.1 400 Bad Request", hubStatus("/api/key?code=0"))
                    assertEquals("HTTP/1.1 403 Forbidden", hubStatus("/api/screenshot"))
                    assertEquals("HTTP/1.1 403 Forbidden", hubStatus("/api/boost"))
                }
                else -> Unit
            }
            current = tag
        }
        press(current, Key.DirectionDown, current)
        for (feature in TvFeature.entries.dropLast(1).asReversed()) {
            val tag = "feature-${feature.key}"
            press(current, Key.DirectionUp, tag)
            current = tag
        }
        press(current, Key.DirectionUp, "expert-master")
        toggleFocused("expert-master")
        compose.onNodeWithTag("expert-master").assertIsOff()
        compose.onNodeWithTag("feature-file_hub").assertDoesNotExist()
        compose.onNodeWithTag("tab-share").assertDoesNotExist()
        awaitHub(false)

        // The master preserves choices. Re-enabling it starts the selected hub again.
        toggleFocused("expert-master")
        compose.onNodeWithTag("expert-master").assertIsOn()
        awaitHub(true)
        current = "expert-master"
        for (feature in TvFeature.entries.takeWhile { it != TvFeature.WEB_REMOTE }) {
            val tag = "feature-${feature.key}"
            press(current, Key.DirectionDown, tag)
            current = tag
        }
        compose.onNodeWithTag("feature-file_hub").assertIsOn()
        press(current, Key.DirectionDown, "feature-web_remote")
        compose.onNodeWithTag("feature-web_remote").assertIsOn()
        press("feature-web_remote", Key.DirectionUp, "feature-file_hub")
        toggleFocused("feature-file_hub")
        compose.onNodeWithTag("feature-file_hub").assertIsOff()
        awaitHub(false)
        press("feature-file_hub", Key.DirectionDown, "feature-web_remote")
        compose.onNodeWithTag("feature-web_remote").assertIsOn() // saved, but parent is off
    }

    @Test
    fun downFromDebloaterHeaderEntersItsControlsAndHomeReturnsToFirstApp() {
        enableFeature(TvFeature.DEBLOATER)
        compose.onNodeWithTag("tools-debloat").performClick()
        awaitFocus("debloat-back")
        press("debloat-back", Key.DirectionDown, "debloat-refresh")

        val inDebloater = hasTestTag("debloat-adb-guide") or hasTestTag("debloat-row") or
            hasAnyAncestor(hasTestTag("debloat-row"))
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodes(inDebloater).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("debloat-refresh").performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(inDebloater and isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("debloat-back").assertExists()
        sendLauncherHomeIntent()
        compose.onNodeWithTag("debloat-back").assertDoesNotExist()
        compose.onNodeWithTag("home-first-app").assertIsFocused()
    }

    @Test
    fun networkAndAboutHeadersLeadIntoTheirOwnContent() {
        enableFeature(TvFeature.NETWORK_TOOLS)
        compose.onNodeWithTag("tools-network").performClick()
        awaitFocus("network-back")
        press("network-back", Key.DirectionDown, "network-retest")
        press("network-retest", Key.DirectionDown, "network-info-0")
        for (index in 0 until 7) press("network-info-$index", Key.DirectionDown, "network-info-${index + 1}")
        press("network-info-7", Key.DirectionDown, "network-info-7")
        sendLauncherHomeIntent()

        compose.onNodeWithTag("tab-tools").performClick()
        compose.onNodeWithTag("tools-about").performClick()
        awaitFocus("about-back")
        press("about-back", Key.DirectionDown, "about-website")
        press("about-website", Key.DirectionUp, "about-back")
        sendLauncherHomeIntent()
    }
}
