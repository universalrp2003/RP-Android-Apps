package com.universalrp.animalvoice

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Large-text layout check. CI runs the emulator with font scale 2.0, so the first-run
 * download control must still be reachable by scrolling, visible, tappable and at least
 * 48 dp tall in both languages, and the Optional settings heading must keep its full width
 * (a side-by-side toggle once squeezed it into one-character lines). No microphone access,
 * no network, no model download.
 */
@RunWith(AndroidJUnit4::class)
class LargeTextLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun tamilDownloadControlStaysReachableAtLargeTextSize() {
        rule.onNodeWithText("உயிரின மாடலைப் பதிவிறக்கு (~79 MB)")
            .performScrollTo().assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(48.dp)
    }

    @Test fun englishDownloadControlStaysReachableAtLargeTextSize() {
        rule.onNodeWithText("English").performClick()
        rule.onNodeWithText("Download species model (~79 MB)")
            .performScrollTo().assertIsDisplayed().assertHasClickAction().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Optional settings").performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(220.dp)
    }

    @Test fun tamilOptionalSettingsHeadingIsNotSqueezedAndStillToggles() {
        rule.onNodeWithText("விருப்ப அமைப்புகள்").performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(220.dp)
        rule.onNodeWithText("மேலதிக அமைப்புகள் ▾").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("மேலதிக அமைப்புகளை மறை ▴").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("இரண்டு கோப்புகளின் பக்கத்தைத் திற").performScrollTo().assertIsDisplayed()
    }
}
