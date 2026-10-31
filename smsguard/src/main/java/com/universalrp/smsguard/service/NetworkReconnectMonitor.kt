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

/** Validated-network recovery schedules durable, bounded background checks. */
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
                            PendingLinkCheckWorker.schedule(appCtx)
                        }
                    }
                }
            })
        }
    }

    /** At most three remote URL attempts per pass, including UI-triggered drains. */
    suspend fun drainPendingQueueNow(
        context: Context, announceVoice: Boolean = true, shouldStop: () -> Boolean = { false },
    ): Int {
        val appCtx = context.applicationContext
        if (!GuardSettingsStore.ensureLoaded(appCtx).internetEnabled || !SmsRepository.hasValidatedInternet(appCtx)) return 0
        if (!draining.compareAndSet(false, true)) return 0
        return try {
            var attempts = 0
            var resolvedCount = 0
            val cached = GuardSettingsStore.getCachedLinkResultsMap(appCtx)
            for (item in GuardSettingsStore.getPendingQueue(appCtx)) {
                if (shouldStop() || attempts >= 3) break
                val prefs = GuardSettingsStore.ensureLoaded(appCtx)
                if (!prefs.internetEnabled) break
                val sender = SenderIdentityEngine.identify(item.rawAddress)
                val mute = GuardSettingsStore.senderMuteFor(appCtx, sender.normalizedAddress)
                if (mute?.blocked == true) {
                    GuardSettingsStore.advancePendingLinkCheck(appCtx, item, item.urls.toSet(), emptySet())
                    continue // No request or speech for a sender blocked after the SMS arrived.
                }
                val verified = ArrayList<LinkScanResult>()
                val attempted = LinkedHashSet<String>()
                for (url in item.urls) {
                    if (shouldStop() || !GuardSettingsStore.ensureLoaded(appCtx).internetEnabled) break
                    val previous = cached[url]
                    if (previous != null) {
                        verified.add(previous)
                        continue
                    }
                    if (attempts >= 3) break
                    attempts++
                    attempted.add(url)
                    verified.add(LinkSecurityScanner.scanSingleUrl(
                        rawUrl = url, sender = sender, internetAllowed = true,
                        hasValidatedNetwork = SmsRepository.hasValidatedInternet(appCtx),
                        safeBrowsingApiKey = GuardSettingsStore.getSafeBrowsingApiKey(appCtx),
                    ))
                }
                GuardSettingsStore.cacheLinkResults(appCtx, verified)
                val completed = verified.filter { it.verdict == LinkVerdict.SAFE || it.verdict == LinkVerdict.UNSAFE }
                GuardSettingsStore.advancePendingLinkCheck(appCtx, item, completed.map { it.originalUrl }.toSet(), attempted)
                resolvedCount += completed.size
                // Speak each completed URL separately; a mixed batch must not name a safe URL as the threat.
                val freshPrefs = GuardSettingsStore.ensureLoaded(appCtx)
                val freshMute = GuardSettingsStore.senderMuteFor(appCtx, sender.normalizedAddress)
                if (freshMute?.blocked != true) {
                    completed.firstOrNull { it.verdict == LinkVerdict.UNSAFE }?.let {
                        IncomingSmsProcessor.notifyDeferredThreat(appCtx, sender.normalizedAddress, it.displayDomain, freshPrefs.tamil)
                    }
                }
                if (announceVoice && freshPrefs.voiceEnabled && freshMute?.muteVoice != true && freshMute?.blocked != true &&
                    !SleepScheduleManager.isSleepingNow(freshPrefs)) {
                    for (result in completed) {
                        SmsAnnouncementPlanner.buildReconnectedLinkAnnouncement(
                            item.senderNameEn, item.senderNameTa, listOf(result), freshPrefs.tamil,
                        )?.let { SmsVoiceAnnouncer.speak(appCtx, it, flushExisting = false) }
                    }
                }
            }
            SmsEvents.notifyChanged()
            resolvedCount
        } finally {
            draining.set(false)
        }
    }
}
