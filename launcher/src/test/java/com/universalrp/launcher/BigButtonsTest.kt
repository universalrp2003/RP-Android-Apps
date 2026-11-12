package com.universalrp.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The home screen's shape.
 *
 * The count and the order of the big buttons are the product decision this app is built around, so
 * they are pinned rather than left to be rearranged by accident. Somebody whose phone this is will
 * have learned where the phone button is, and moving it is a real cost to a real person.
 */
class BigButtonsTest {

    @Test
    fun theHomeScreenHasEightButtons() {
        assertEquals(8, BigButtons.home().size)
    }

    @Test
    fun theFourMostUsedButtonsComeFirst() {
        val order = BigButtons.home().map { it.action }
        assertEquals(
            listOf(
                BigButtons.Action.DIAL,
                BigButtons.Action.MESSAGES,
                BigButtons.Action.CAMERA,
                BigButtons.Action.CONTACTS,
            ),
            order.take(4),
        )
    }

    @Test
    fun everyButtonHasALabel() {
        for (button in BigButtons.home() + BigButtons.emergency() + BigButtons.appList()) {
            assertTrue("${button.action} has no label resource", button.labelRes != 0)
        }
    }

    @Test
    fun noButtonIsRepeated() {
        val actions = (BigButtons.home() + BigButtons.emergency() + BigButtons.appList())
            .map { it.action }
        assertEquals(actions.size, actions.toSet().size)
    }

    @Test
    fun theVolumeButtonsAreBothThereAndAreSeparate() {
        val actions = BigButtons.home().map { it.action }
        assertTrue(BigButtons.Action.VOLUME_UP in actions)
        assertTrue(BigButtons.Action.VOLUME_DOWN in actions)
    }

    @Test
    fun theEmergencyButtonIsSeparateFromTheGrid() {
        // It is not in home(), so it can be drawn alone and in a different colour.
        assertFalse(BigButtons.home().any { it.action == BigButtons.Action.EMERGENCY })
        assertEquals(BigButtons.Action.EMERGENCY, BigButtons.emergency().action)
    }

    @Test
    fun theAppListButtonIsSeparateFromTheGrid() {
        assertFalse(BigButtons.home().any { it.action == BigButtons.Action.APP_LIST })
        assertEquals(BigButtons.Action.APP_LIST, BigButtons.appList().action)
    }

    @Test
    fun onlyTheIntentButtonsAreLaunchable() {
        // TORCH, VOLUME_UP, VOLUME_DOWN and EMERGENCY act on the phone directly, so treating them
        // as launchable intents would be a bug that only shows up as a dead button.
        val launchable = BigButtons.home().filter { BigButtons.isLaunchable(it.action) }
            .map { it.action }
            .toSet()
        assertEquals(
            setOf(
                BigButtons.Action.DIAL,
                BigButtons.Action.MESSAGES,
                BigButtons.Action.CAMERA,
                BigButtons.Action.CONTACTS,
                BigButtons.Action.SETTINGS,
            ),
            launchable,
        )
    }
}
