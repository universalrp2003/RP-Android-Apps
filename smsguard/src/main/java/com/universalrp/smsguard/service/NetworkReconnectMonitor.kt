package com.universalrp.smsguard.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.smsguard.data.SmsEvents
import com.universalrp.smsguard.data.SmsRepository
import com.universalrp.smsguard.engine.LinkScanResult
import com.universalrp.smsguard.engine.LinkSecurityScanner
import com.universalrp.smsguard.engine.LinkVerdict
import com.universalrp.smsguard.engine.SenderIdentityEngine
import com.universalrp.smsguard.engine.SmsAnnouncementPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Watches real validated network recovery (`ConnectivityManager.NetworkCallback`) AND manual
 * Internet-ON switches, immediately re-verifying all offline queued SMS links and speaking
 * the real verified Green-Tick / Red-Danger details aloud!
 */
object NetworkReconnectMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val registered = AtomicBoolean(false)
    private val draining = AtomicBoolean(false)

    fun start(context: Context) {
        val appCtx = context.applicationContext
        if (!registered.compareAndSet(false, true)) return
        val cm = appCtx.getSystemService(ConnectivityManager::class.java) ?: return

        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (hasValidated) {
                        scope.launch {
                            delay(800L)
                            drainPendingQueueNow(appCtx, announceVoice = true)
                        }
                    }
                }
            })
        }
    }

    /**
     * Verifies all pending offline SMS links when internet is back and speaks the real verdict.
     * Returns count of resolved link items.
     */
    suspend fun drainPendingQueueNow(context: Context, announceVoice: Boolean = true): Int {
        val appCtx = context.applicationContext
        val prefs = GuardSettingsStore.ensureLoaded(appCtx)
        if (!prefs.internetEnabled || !SmsRepository.hasValidatedInternet(appCtx)) {
            return 0
        }
        if (!draining.compareAndSet(false, true)) return 0

        return try {
            val queue = GuardSettingsStore.getPendingQueue(appCtx)
            if (queue.isEmpty()) return 0

            val apiKey = GuardSettingsStore.getSafeBrowsingApiKey(appCtx)
            val completedIds = LinkedHashSet<Long>()
            var resolvedCount = 0

            for (item in queue) {
                val sender = SenderIdentityEngine.identify(item.rawAddress)
                val verified = ArrayList<LinkScanResult>()
                for (url in item.urls) {
                    val res = LinkSecurityScanner.scanSingleUrl(
                        rawUrl = url,
                        sender = sender,
                        internetAllowed = true,
                        hasValidatedNetwork = true,
                        safeBrowsingApiKey = apiKey,
                    )
                    verified.add(res)
                }
                GuardSettingsStore.cacheLinkResults(appCtx, verified)

                val allResolved = verified.all {
                    it.verdict == LinkVerdict.SAFE || it.verdict == LinkVerdict.UNSAFE
                }
                if (allResolved) {
                    completedIds.add(item.messageId)
                    resolvedCount += verified.size
                    if (announceVoice && prefs.voiceEnabled) {
                        SmsAnnouncementPlanner.buildReconnectedLinkAnnouncement(
                            senderNameEn = item.senderNameEn,
                            senderNameTa = item.senderNameTa,
                            results = verified,
                            tamil = prefs.tamil,
                        )?.let { script ->
                            SmsVoiceAnnouncer.speak(appCtx, script, flushExisting = false)
                        }
                    }
                }
            }

            if (completedIds.isNotEmpty()) {
                GuardSettingsStore.removePendingLinkChecks(appCtx, completedIds)
                SmsEvents.notifyChanged()
            }
            resolvedCount
        } finally {
            draining.set(false)
        }
    }
}
