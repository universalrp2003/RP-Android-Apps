package com.universalrp.tvguard

import android.view.KeyEvent
import com.universalrp.tvguard.ai.MicHotkeyPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The microphone shortcut must be described honestly and not collide with other keys. */
class MicHotkeyPolicyTest {

    @Test
    fun menuOpensTheMicOnlyOnALongPress() {
        // A short Menu press stays available for the per-tile options shortcut.
        assertFalse(
            MicHotkeyPolicy.isMicShortcut(
                KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_DOWN, repeatCount = 0, longPress = false
            )
        )
        assertTrue(
            MicHotkeyPolicy.isMicShortcut(
                KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_DOWN, repeatCount = 1, longPress = true
            )
        )
    }

    @Test
    fun keyUpNeverTriggersTheMic() {
        listOf(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_VOICE_ASSIST).forEach {
            assertFalse(MicHotkeyPolicy.isMicShortcut(it, KeyEvent.ACTION_UP, 0, true))
        }
    }

    @Test
    fun homeAndBackAreNotClaimed() {
        // The system owns these; claiming them would be dishonest.
        assertFalse(MicHotkeyPolicy.isMicShortcut(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_DOWN, 0, true))
        assertFalse(MicHotkeyPolicy.isMicShortcut(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_DOWN, 0, true))
        assertFalse(MicHotkeyPolicy.candidates.contains(KeyEvent.KEYCODE_HOME))
        assertFalse(MicHotkeyPolicy.candidates.contains(KeyEvent.KEYCODE_BACK))
    }

    @Test
    fun repeatedVoiceKeyDoesNotRetrigger() {
        assertTrue(MicHotkeyPolicy.isMicShortcut(KeyEvent.KEYCODE_INFO, KeyEvent.ACTION_DOWN, 0, false))
        // Auto-repeat must not reopen the dialog on every repeat event.
        assertFalse(MicHotkeyPolicy.isMicShortcut(KeyEvent.KEYCODE_INFO, KeyEvent.ACTION_DOWN, 3, false))
    }

    @Test
    fun labelsAreUserReadableAndScopeIsStated() {
        assertEquals("Menu", MicHotkeyPolicy.label(KeyEvent.KEYCODE_MENU))
        val scope = MicHotkeyPolicy.scopeDescription()
        assertTrue(scope.contains("on screen"))
        assertTrue(scope.contains("system-wide"))
    }
}
