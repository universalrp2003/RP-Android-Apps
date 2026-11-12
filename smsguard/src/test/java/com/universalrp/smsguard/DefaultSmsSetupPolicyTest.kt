package com.universalrp.smsguard

import com.universalrp.smsguard.engine.DefaultSmsSetupPolicy
import com.universalrp.smsguard.engine.DefaultSmsSetupPolicy.Step.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultSmsSetupPolicyTest {
    @Test fun disclosurePrecedesEveryNewDefaultRequestUntilAcknowledged() {
        for (sdk in listOf(26, 28, 29, 35, 36)) for (available in listOf(false, true))
            assertEquals(DISCLOSE_MMS_LIMIT, DefaultSmsSetupPolicy.next(false, false, sdk, available))
    }
    @Test fun acknowledgedModernAndroidUsesActualRoleRequest() {
        for (sdk in listOf(29, 35, 36))
            assertEquals(REQUEST_ROLE, DefaultSmsSetupPolicy.next(false, true, sdk, true))
    }
    @Test fun legacyAndUnavailableRoleHaveExplicitSystemRoutes() {
        for (sdk in listOf(26, 28))
            assertEquals(LEGACY_REQUEST, DefaultSmsSetupPolicy.next(false, true, sdk, false))
        assertEquals(SYSTEM_SETTINGS, DefaultSmsSetupPolicy.next(false, true, 35, false))
    }
    @Test fun existingDefaultDoesNotLoopIntoAnotherPrompt() {
        for (ack in listOf(false, true)) for (available in listOf(false, true))
            assertEquals(ALREADY_DEFAULT, DefaultSmsSetupPolicy.next(true, ack, 35, available))
    }
}
