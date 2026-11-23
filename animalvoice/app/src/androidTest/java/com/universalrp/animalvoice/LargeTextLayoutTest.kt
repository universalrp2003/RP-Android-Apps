package com.universalrp.animalvoice

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Large-text layout check. CI runs the emulator with font scale 2.0, so controls must stay
 * reachable by scrolling, visible, tappable and at least 48 dp tall in both languages.
 * The main screen holds only the identify controls; everything optional lives behind the
 * ⚙ Settings button, with the built-in Guide and About at the end of Settings. No microphone
 * access and no model download here; the app's only network use is the read-only, once-a-day
 * release-metadata check, which these tests do not exercise.
 */
@RunWith(AndroidJUnit4::class)
class LargeTextLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun startEveryTestInTamil() {
        // 0.21 keeps the interface language in the shared RP Ideas preference, like every other
        // RP Ideas app, so a test that switched to English would otherwise leak into the next one.
        AppLanguageState.set(InstrumentationRegistry.getInstrumentation().targetContext, AppLanguage.TAMIL)
        dismissUpdateNoticeIfShown()
    }

    private fun openSettings(description: String) {
        dismissUpdateNoticeIfShown()
        rule.onNodeWithContentDescription(description).performClick()
    }

    /** The daily update check may show "Update available" once a newer public release exists; it is not part of these tests. */
    private fun dismissUpdateNoticeIfShown() {
        rule.waitForIdle()
        if (rule.onAllNodesWithText("பிறகு").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithText("பிறகு").performClick()
        else if (rule.onAllNodesWithText("Later").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithText("Later").performClick()
    }

    @Test fun tamilDownloadControlStaysReachableAtLargeTextSize() {
        rule.onNodeWithText("உயிரின மாடலைப் பதிவிறக்கு (~79 MB)")
            .performScrollTo().assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(48.dp)
    }

    @Test fun englishDownloadControlStaysReachableAtLargeTextSize() {
        rule.onNodeWithText("English").performClick()
        rule.onNodeWithText("Download species model (~79 MB)")
            .performScrollTo().assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(48.dp)
    }

    @Test fun mainScreenHasNoOptionalControls() {
        rule.onNodeWithText("விருப்ப அமைப்புகள்").assertDoesNotExist()
        rule.onNodeWithText("மாதிரிப் படங்களைத் தானாகக் காட்டு").assertDoesNotExist()
        rule.onNodeWithText("ஒலி அறிதல் · சோதனை").assertDoesNotExist()
        rule.onNodeWithText("YAMNet / AudioSet", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Listen with basic model").assertDoesNotExist()
        rule.onNodeWithText("எதை அறியலாம்? ▾").assertDoesNotExist()
    }

    @Test fun settingsGearOpensTheSettingsScreen() {
        openSettings("அமைப்புகள்")
        rule.onNodeWithText("அமைப்புகள்").assertIsDisplayed()
        rule.onNodeWithText("மாதிரிப் படங்களைத் தானாகக் காட்டு").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("விலங்கு ஒலிகள்", substring = true).assertDoesNotExist()
    }

    @Test fun settingsGearOpensTheSettingsScreenInEnglish() {
        rule.onNodeWithText("English").performClick()
        openSettings("Settings")
        rule.onNodeWithText("Settings").assertIsDisplayed()
        rule.onNodeWithText("Load reference photos automatically").performScrollTo().assertIsDisplayed()
    }

    @Test fun tamilSettingsShowModelFilesAndCoverageWithoutADropdown() {
        openSettings("அமைப்புகள்")
        // 0.22: every detail is shown directly; no "Advanced settings" or "What can it identify?" toggle.
        rule.onNodeWithText("மேலதிக அமைப்புகள் ▾").assertDoesNotExist()
        rule.onNodeWithText("எதை அறியலாம்? ▾").assertDoesNotExist()
        rule.onNodeWithText("இரண்டு கோப்புகளின் பக்கத்தைத் திற").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("எதை அறியலாம்?").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("BirdNET+ V3 பட்டியலில் 11,709 உயிரினங்கள்", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun tamilSettingsHeadingsKeepFullWidthAtLargeTextSize() {
        openSettings("அமைப்புகள்")
        rule.onNodeWithText("உயிரின மாடல் கோப்புகள்").performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(220.dp)
        rule.onNodeWithText("அடிப்படை இணையமில்லாத கேட்பு").performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(220.dp)
    }

    @Test fun backFromSettingsReturnsToMainScreen() {
        openSettings("அமைப்புகள்")
        rule.onNodeWithText("←  பின்").performClick()
        // The header is the top of the main screen; the species card sits lower at 2x font scale.
        rule.onNodeWithText("விலங்கு ஒலிகள்", substring = true).assertIsDisplayed()
        rule.onNodeWithText("உயிரின மாடல்").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("அமைப்புகள்").assertDoesNotExist()
    }

    @Test fun tamilGuideOpensExpandsAndReturns() {
        openSettings("அமைப்புகள்")
        rule.onNodeWithText("வழிகாட்டி — எப்படிப் பயன்படுத்துவது").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithText("வழிகாட்டி").assertIsDisplayed()
        rule.onNodeWithText("விலங்கு ஒலிகள் நீங்கள் அழுத்தியபிறகு", substring = true).assertIsDisplayed()
        rule.onNodeWithText("மதிப்பெண்கள் சரிசெய்யப்படாத ஊகங்களே", substring = true).assertDoesNotExist()
        rule.onNodeWithText("முதல் பதிவிறக்கம் (சுமார் 79 MB)").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithText("SHA-256 மூலம் சரிபார்க்கப்படும்", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("முதல் பதிவிறக்கம் (சுமார் 79 MB)").performScrollTo().performClick()
        rule.onNodeWithText("SHA-256 மூலம் சரிபார்க்கப்படும்", substring = true).assertDoesNotExist()
        rule.onNodeWithText("பின்செல்").performScrollTo().performClick()
        rule.onNodeWithText("அமைப்புகள்").assertIsDisplayed()
    }

    @Test fun tamilAboutListsCreditsLinksModelLicencesAndEveryOtherApp() {
        openSettings("அமைப்புகள்")
        rule.onNodeWithText("இந்தச் செயலி பற்றி — எமது செயலிகள், இணைப்புகள், நன்றிகள்").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithText("இந்தச் செயலி").assertIsDisplayed()
        rule.onNodeWithText("Ramesh prathap .R (universalrp2003@gmail.com) · RP Ideas").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Arena.ai Agent Mode", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("RP-Android-Apps GitHub Repository").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("அதிகாரப்பூர்வ இணையதளம் — இந்தச் செயலியின் பக்கம்").performScrollTo().assertHasClickAction()
        rule.onNodeWithText("YAMNet / AudioSet", substring = true).performScrollTo().assertIsDisplayed()
        // Owner choice (November 2026): the apps are a plain name list, Store and DailyVault first; downloads live on the website.
        rule.onNodeWithText("தமிழன் · Tamilan Store").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("தினக்கணக்கு · DailyVault").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("விலங்கு ஒலிகள் · Animal Sounds — இந்தச் செயலி").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("டிவி காவலன் · Android TV Guard").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("GitHub release", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("official download", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("Legacy Mirror", substring = true).assertCountEquals(0)
        rule.onNodeWithText("அதிகாரப்பூர்வ இணையதளம் — எல்லாச் செயலிகளும்").performScrollTo().assertHasClickAction()
        rule.onNodeWithText("←  பின்").performScrollTo().performClick()
        rule.onNodeWithText("அமைப்புகள்").assertIsDisplayed()
    }

    @Test fun englishGuideAndAboutReadCorrectly() {
        rule.onNodeWithText("English").performClick()
        openSettings("Settings")
        rule.onNodeWithText("Guide — how to use this app").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithText("Guide").assertIsDisplayed()
        rule.onNodeWithText("Animal Sounds listens for a few seconds after you tap", substring = true).assertIsDisplayed()
        rule.onNodeWithText("What this app does").performScrollTo().performClick()
        rule.onNodeWithText("BirdNET+ 3.0, about 79 MB", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.onNodeWithText("Settings").assertIsDisplayed()
        rule.onNodeWithText("About this app — our apps, links & credits").performScrollTo().performClick()
        rule.onNodeWithText("About").assertIsDisplayed()
        rule.onNodeWithText("Created by").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("RP-Android-Apps Official Website").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Animal Sounds (விலங்கு ஒலிகள்) — this app").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Tamilan Store").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("official download", substring = true).assertCountEquals(0)
    }
}
