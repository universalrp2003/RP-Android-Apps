package com.universalrp.smsguard.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.universalrp.smsguard.data.GuardSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** OS-persisted scheduling, not a process-only network callback or broadcast-sized HTTP task. */
class PendingLinkCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!GuardSettingsStore.ensureLoaded(applicationContext).internetEnabled) return@withContext Result.success()
        NetworkReconnectMonitor.drainPendingQueueNow(applicationContext, shouldStop = { isStopped })
        if (GuardSettingsStore.getPendingQueue(applicationContext).isEmpty()) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "smsguard-pending-link-checks-v1"

        fun schedule(context: Context, awaitPersistence: Boolean = false) {
            if (GuardSettingsStore.getPendingQueue(context).isEmpty()) return
            val request = OneTimeWorkRequestBuilder<PendingLinkCheckWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                // No message body, URL, sender or API key goes in WorkManager input or tags.
                .build()
            val operation = WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request,
            )
            // The goAsync caller is already on IO. Confirm scheduler persistence before finish.
            // A durable prefs queue remains available for recovery if local scheduler storage fails.
            if (awaitPersistence) runCatching { operation.result.get(3, TimeUnit.SECONDS) }
        }
    }
}
