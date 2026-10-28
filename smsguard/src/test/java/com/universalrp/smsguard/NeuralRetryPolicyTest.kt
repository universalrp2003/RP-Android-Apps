package com.universalrp.smsguard

import com.universalrp.voice.NeuralRetryPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class NeuralRetryPolicyTest {
    @Test fun retriesOnlyTransientPeerInternalError() {
        assertEquals(2500L, NeuralRetryPolicy.remainingBudget(4000, 1500, 1011))
        for (code in listOf(0, 1000, 1008, 403, 429)) {
            assertEquals(0L, NeuralRetryPolicy.remainingBudget(4000, 1500, code))
        }
    }
    @Test fun neverRestartsOrExceedsOriginalBudget() {
        assertEquals(0L, NeuralRetryPolicy.remainingBudget(4000, 4000, 1011))
        assertEquals(0L, NeuralRetryPolicy.remainingBudget(4000, 4500, 1011))
        assertEquals(0L, NeuralRetryPolicy.remainingBudget(4000, 3500, 1011))
        assertEquals(0L, NeuralRetryPolicy.remainingBudget(4000, -1, 1011))
    }
}
