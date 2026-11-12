package com.universalrp.voicenotes

import com.universalrp.updates.UpdateReminderPolicy as Policy
import org.junit.Assert.*
import org.junit.Test

class UpdateReminderPolicyTest {
    @Test fun disabledNeverChecks() { assertFalse(Policy.shouldCheck(false, Policy.DAY_MS * 3, 0)) }
    @Test fun firstEnabledOpenChecks() { assertTrue(Policy.shouldCheck(true, 100, 0)) }
    @Test fun attemptsAreBoundedToDaily() {
        assertFalse(Policy.shouldCheck(true, 100 + Policy.DAY_MS - 1, 100))
        assertTrue(Policy.shouldCheck(true, 100 + Policy.DAY_MS, 100))
    }
    @Test fun clockRollbackDoesNotSpam() { assertFalse(Policy.shouldCheck(true, 50, 100)) }
    @Test fun skipOnlySuppressesSameVersionAutomatically() {
        assertFalse(Policy.shouldOffer("1.3", "1.3", false))
        assertTrue(Policy.shouldOffer("1.4", "1.3", false))
        assertTrue(Policy.shouldOffer("1.3", "1.3", true))
        assertFalse(Policy.shouldOffer(null, "1.3", true))
    }
}
