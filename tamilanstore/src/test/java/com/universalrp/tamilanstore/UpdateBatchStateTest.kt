package com.universalrp.tamilanstore

import org.junit.Assert.*
import org.junit.Test

class UpdateBatchStateTest {
    @Test fun serialOrderRequiresVerifiedReadyHandoffAndInstalledEvidence() {
        var queue = UpdateBatchState.start(listOf("dailyvault", "radio"))
        assertEquals("dailyvault", queue.currentId)
        assertThrows(IllegalStateException::class.java) { queue.handedOff() }
        assertThrows(IllegalStateException::class.java) { queue.returned(true) }
        queue = queue.ready().handedOff()
        assertEquals(0, queue.index)
        queue = queue.returned(true)
        assertEquals(BatchStage.DOWNLOADING, queue.stage)
        assertEquals("radio", queue.currentId)
        queue = queue.ready().handedOff().returned(true)
        assertEquals(BatchStage.DONE, queue.stage)
        assertFalse(queue.active)
        assertEquals(0, queue.remaining)
        assertNull(queue.currentId)
    }
    @Test fun cancelOrRefusalNeverAdvancesOrReplaysWithoutExplicitResume() {
        val waiting = UpdateBatchState.start(listOf("dailyvault", "radio")).ready().handedOff()
        val cancelled = waiting.returned(false)
        assertEquals(BatchStage.PAUSED, cancelled.stage)
        assertEquals(0, cancelled.index)
        assertThrows(IllegalStateException::class.java) { cancelled.handedOff() }
        assertThrows(IllegalStateException::class.java) { cancelled.returned(true) }
        assertEquals(BatchStage.DOWNLOADING, cancelled.resume().stage)
        assertEquals("dailyvault", cancelled.resume().currentId)
    }
    @Test fun downloadVerificationFailurePermissionDenialAndBackgroundPauseSameItem() {
        for (state in listOf(UpdateBatchState.start(listOf("radio")), UpdateBatchState.start(listOf("radio")).ready())) {
            val paused = state.pause()
            assertEquals(BatchStage.PAUSED, paused.stage)
            assertEquals(0, paused.index)
            assertEquals(BatchStage.DOWNLOADING, paused.resume().stage)
        }
    }
    @Test fun planIsBoundedUniqueAndCopiedAndProcessDeathHasNoApproval() {
        for (bad in listOf(emptyList(), List(16) { "app$it" }, listOf("radio", "radio"), listOf("../radio")))
            assertThrows(IllegalArgumentException::class.java) { UpdateBatchState.start(bad) }
        val plan = mutableListOf("radio")
        val queue = UpdateBatchState.start(plan)
        plan.clear()
        assertEquals(listOf("radio"), queue.ids)
        assertFalse(UpdateBatchState().active)
        assertEquals(BatchStage.IDLE, UpdateBatchState().stage)
    }
    @Test fun allFifteenCanCompleteAndAlreadyUpdatedItemsNeedNoPrompt() {
        var state = UpdateBatchState.start(List(15) { "app$it" })
        repeat(15) { index ->
            assertEquals(index, state.index)
            state = if (index % 2 == 0) state.alreadyUpdated() else state.ready().handedOff().returned(true)
        }
        assertEquals(BatchStage.DONE, state.stage)
        assertEquals(15, state.index)
    }
    @Test fun installerActivityResultIsNotProofAndSameNameOldCodeCannotAdvance() {
        assertFalse(BatchInstallEvidence.confirmed(null, "0.5", 5))
        assertFalse(BatchInstallEvidence.confirmed(InstalledApp("0.5", true, 5), "0.5", null))
        assertFalse(BatchInstallEvidence.confirmed(InstalledApp("0.5", true, 4), "0.5", 5))
        assertFalse(BatchInstallEvidence.confirmed(InstalledApp("0.5", false, 5), "0.5", 5))
        assertFalse(BatchInstallEvidence.confirmed(InstalledApp("0.4", true, 4), "0.5", 5))
        assertFalse(BatchInstallEvidence.confirmed(InstalledApp("unknown", true, 6), "0.5", 5))
        assertFalse(BatchInstallEvidence.confirmed(InstalledApp("0.5", true), "0.5", 5))
        assertTrue(BatchInstallEvidence.confirmed(InstalledApp("0.5", true, 5), "0.5", 5))
        assertTrue(BatchInstallEvidence.confirmed(InstalledApp("0.6", true, 6), "0.5", 5))
    }
}
