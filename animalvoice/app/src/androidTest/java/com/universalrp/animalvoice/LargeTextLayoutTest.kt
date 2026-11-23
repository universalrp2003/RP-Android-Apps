package com.universalrp.animalvoice

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Large-text layout check. CI runs the emulator with font scale 2.0, so controls must stay
 * reachable by scrolling, visible, tappable and at least 48 dp tall in both languages.
 * The main screen holds only the identify controls; everything optional lives behind the
 * ⚙ Settings button. No microphone access, no network, no model download.
 */
@RunWith(AndroidJUnit4::class)
class LargeTextLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun openSettings(description: String) {
        rule.onNodeWithContentDescription(description).performClick()
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

    @Test fun tamilAdvancedToggleWorksFromSettings() {
        openSettings("அமைப்புகள்")
        rule.onNodeWithText("மேலதிக அமைப்புகள் ▾").performScrollTo().performClick()
        rule.onNodeWithText("மேலதிக அமைப்புகளை மறை ▴").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("இரண்டு கோப்புகளின் பக்கத்தைத் திற").performScrollTo().assertIsDisplayed()
    }

    @Test fun tamilSettingsHeadingsKeepFullWidthAtLargeTextSize() {
        openSettings("அமைப்புகள்")
        rule.onNodeWithText("உயிரின மாடல் கோப்புகள்").performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(220.dp)
        rule.onNodeWithText("அடிப்படை இணையமில்லாத கேட்பு").performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(220.dp)
    }

    @Test fun backFromSettingsReturnsToMainScreen() {
        openSettings("அமைப்புகள்")
        rule.onNodeWithText("←  பின்").performClick()
        rule.onNodeWithText("உயிரின மாடல்").assertIsDisplayed()
    }
}
