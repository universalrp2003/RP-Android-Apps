package com.universalrp.tvguard

import android.content.Intent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
import com.universalrp.tvguard.launcher.TvAppCatalog
import com.universalrp.tvguard.settings.TvFeature
import com.universalrp.tvguard.settings.TvFeaturePreferences
import com.universalrp.tvguard.settings.TvFeatureSettings
import com.universalrp.tvguard.ui.MainActivity
import com.universalrp.tvguard.web.LocalWebTransferServer
import java.net.Socket
import java.net.URLEncoder
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        awaitHomeStart()
    }

    @After
    fun restoreSettings() {
        LocalWebTransferServer.stop()
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
            val tags = runCatching {
                compose.onAllNodes(SemanticsMatcher("has a test tag") { node ->
                    node.config.getOrNull(SemanticsProperties.TestTag) != null
                }).fetchSemanticsNodes().mapNotNull { node ->
                    node.config.getOrNull(SemanticsProperties.TestTag)
                }
            }.getOrElse { listOf("dump failed: " + it) }
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
                    " tags=" + tags +
                    " leanback=" + query(Intent.CATEGORY_LEANBACK_LAUNCHER) +
                    " launcher=" + query(Intent.CATEGORY_LAUNCHER) +
                    " catalog=" + catalog +
                    " cause=" + timeout.message
            )
        }
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

    private fun hubResponse(path: String): String = Socket("127.0.0.1", LocalWebTransferServer.activePort).use { socket ->
        socket.soTimeout = 3_000
        socket.getOutputStream().write("GET $path HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".toByteArray())
        socket.getOutputStream().flush()
        socket.getInputStream().bufferedReader().readText()
    }

    private fun hubStatus(path: String): String = hubResponse(path).substringBefore("\r\n")

    /**
     * The switches exercised by the contiguous D-pad walk.
     *
     * This list is deliberately explicit rather than `TvFeature.entries`: the Expert screen
     * is a LazyColumn, and a switch added at the end is not composed until it is scrolled to,
     * so including it in a pure D-pad walk would ask focus to move to a node that does not
     * exist yet. Newly added switches get their own scrolled test instead, below.
     */
    private val DpadTraversalFeatures = listOf(
        TvFeature.STATUS_BAR,
        TvFeature.CPU_PILL,
        TvFeature.RAM_PILL,
        TvFeature.STORAGE_PILL,
        TvFeature.NETWORK_PILL,
        TvFeature.VOICE_HELPER,
        TvFeature.SYSTEM_ASSISTANT,
        TvFeature.SCREENSHOT,
        TvFeature.FILE_HUB,
        TvFeature.WEB_REMOTE,
    )

    /**
     * The switches appended after WEB_REMOTE are reachable, can be scrolled to, and are off
     * by default. This is the honest coverage for a lazy list: scrolling first, then acting.
     */
    @Test
    fun appendedExpertSwitchesAreReachableScrolledAndOffByDefault() {
        openExpert()
        toggleFocused("expert-master")
        compose.onNodeWithTag("expert-master").assertIsOn()

        for (feature in listOf(TvFeature.STATUS_OVERLAY, TvFeature.AI_ASSISTANT)) {
            val tag = "feature-${feature.key}"
            compose.onNodeWithTag("expert-list").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).assertIsOff().performClick().assertIsOn()
            // A dependent switch stays paused until its parent is on.
            if (feature == TvFeature.STATUS_OVERLAY) {
                compose.onNodeWithTag("expert-list").performScrollToNode(hasTestTag("feature-status_bar"))
                compose.onNodeWithTag("feature-status_bar").assertIsOff()
            }
        }

        // Turning Expert off must stop both without erasing the saved choices.
        compose.onNodeWithTag("expert-list").performScrollToNode(hasTestTag("expert-master"))
        toggleFocused("expert-master")
        compose.onNodeWithTag("expert-master").assertIsOff()
        compose.onNodeWithTag("expert-list").performScrollToNode(hasTestTag("feature-ai_assistant"))
        compose.onNodeWithTag("feature-ai_assistant").assertIsOn()
    }

    private fun sendLauncherHomeIntent() {
        val activity = compose.activity
        val originalIntent = Intent(activity.intent)
        val previous = activity.homeRequestId
        try {
            compose.runOnUiThread {
                activity.startActivity(Intent(activity, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
            }
            compose.waitUntil(timeoutMillis = 10_000) { activity.homeRequestId != previous }
            awaitFocus("home-first-app")
        } finally {
            // ActivityScenario matches lifecycle events against the launch intent. The
            // real onNewIntent correctly calls setIntent(homeIntent), so put back the
            // test's launch intent after observing the Home effect; otherwise scenario
            // teardown ignores DESTROYED and reports a false lifecycle timeout.
            compose.runOnUiThread { activity.setIntent(originalIntent) }
        }
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
        // D-pad traversal covers the switches that are composed together on a 1080p TV.
        for (feature in DpadTraversalFeatures) {
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
                    val untrusted = URLEncoder.encode("invalid.package\",\"injected\":true,\"tail\":\"", "UTF-8")
                    val response = hubResponse("/api/launch?app=$untrusted")
                    assertEquals("HTTP/1.1 200 OK", response.substringBefore("\r\n"))
                    val body = JSONObject(response.substringAfter("\r\n\r\n"))
                    assertFalse(body.getBoolean("success"))
                    assertFalse(body.has("injected"))
                    assertFalse(body.has("app"))
                }
                else -> Unit
            }
            current = tag
        }
        press(current, Key.DirectionDown, "feature-${DpadTraversalFeatures.last().key}")
        for (feature in DpadTraversalFeatures.dropLast(1).asReversed()) {
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
