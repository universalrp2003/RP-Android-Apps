package com.universalrp.smsguard

import com.universalrp.smsguard.data.InboxLoadResult
import com.universalrp.smsguard.data.InboxLoadFailureStage
import com.universalrp.smsguard.data.messagesOrPrevious
import org.junit.Assert.assertEquals
import org.junit.Test

class InboxLoadResultTest {
    @Test fun hydrationFailureMustNotEraseQuickPaint() {
        val quickPaint = listOf("synthetic recent message")
        assertEquals(quickPaint, InboxLoadResult.Unavailable(InboxLoadFailureStage.PROVIDER_QUERY).messagesOrPrevious(quickPaint))
    }
    @Test fun repeatedFailureMustRetainLastGoodSnapshot() {
        val previous = listOf(1, 2, 3)
        val first = InboxLoadResult.Unavailable(InboxLoadFailureStage.PROVIDER_QUERY).messagesOrPrevious(previous)
        assertEquals(previous, InboxLoadResult.Unavailable(InboxLoadFailureStage.PROVIDER_QUERY).messagesOrPrevious(first))
    }
    @Test fun successfulEmptyInboxMustClearDeletedMessages() {
        assertEquals(emptyList<Int>(), InboxLoadResult.Success(emptyList<Int>(), false).messagesOrPrevious(listOf(1)))
    }
    @Test fun successfulHydrationReplacesQuickPaint() {
        assertEquals(listOf(1, 2), InboxLoadResult.Success(listOf(1, 2), true).messagesOrPrevious(listOf(1)))
    }
    @Test fun revokedPermissionMustClearPrivateSnapshot() {
        assertEquals(emptyList<Int>(), InboxLoadResult.PermissionRequired.messagesOrPrevious(listOf(1)))
    }
}
