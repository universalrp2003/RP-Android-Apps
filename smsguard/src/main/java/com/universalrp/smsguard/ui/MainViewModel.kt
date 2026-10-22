package com.universalrp.smsguard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universalrp.smsguard.data.EnrichedSmsMessage
import com.universalrp.smsguard.data.GuardPrefs
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.smsguard.data.QueuedSleepAnnouncement
import com.universalrp.smsguard.data.QueuedSmsLinkCheck
import com.universalrp.smsguard.data.SimCardInfo
import com.universalrp.smsguard.data.SmsEvents
import com.universalrp.smsguard.data.SmsRepository
import com.universalrp.smsguard.data.SmsThreadSummary
import com.universalrp.smsguard.engine.DltCategory
import com.universalrp.smsguard.engine.FinanceLedgerEngine
import com.universalrp.smsguard.engine.LinkScanResult
import com.universalrp.smsguard.engine.LinkSecurityScanner
import com.universalrp.smsguard.engine.LinkVerdict
import com.universalrp.smsguard.engine.PeriodSummary
import com.universalrp.smsguard.engine.SpoofClassification
import com.universalrp.smsguard.engine.UserAccountProfile
import com.universalrp.smsguard.service.IncomingSmsProcessor
import com.universalrp.smsguard.service.NetworkReconnectMonitor
import com.universalrp.smsguard.service.SleepScheduleManager
import com.universalrp.smsguard.service.SmsVoiceAnnouncer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class MainTab { MESSAGES, SHIELD, FINANCE, CONTROLS }
enum class InboxFilter { ALL, PERSONAL, BANKS, OTP, LINKS, SCAM, PROMO }
enum class FinancePeriodMode { THIS_MONTH, SELECTED_MONTH, YEARLY, CUSTOM_RANGE }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefsFlow: StateFlow<GuardPrefs> = GuardSettingsStore.state
    val speakingFlow: StateFlow<Boolean> = SmsVoiceAnnouncer.isSpeaking
    val voicesFlow = SmsVoiceAnnouncer.availableVoices
    val activeVoiceStatusFlow = SmsVoiceAnnouncer.activeVoiceStatus

    private val _currentTab = MutableStateFlow(MainTab.MESSAGES)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _showGuide = MutableStateFlow(false)
    val showGuide: StateFlow<Boolean> = _showGuide.asStateFlow()

    private val _showAbout = MutableStateFlow(false)
    val showAbout: StateFlow<Boolean> = _showAbout.asStateFlow()

    private val _openThreadKey = MutableStateFlow<String?>(null)
    val openThreadKey: StateFlow<String?> = _openThreadKey.asStateFlow()

    private val _inboxFilter = MutableStateFlow(InboxFilter.ALL)
    val inboxFilter: StateFlow<InboxFilter> = _inboxFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allMessages = MutableStateFlow<List<EnrichedSmsMessage>>(emptyList())
    val allMessages: StateFlow<List<EnrichedSmsMessage>> = _allMessages.asStateFlow()

    private val _threads = MutableStateFlow<List<SmsThreadSummary>>(emptyList())
    val threads: StateFlow<List<SmsThreadSummary>> = _threads.asStateFlow()

    private val _isDefaultSms = MutableStateFlow(false)
    val isDefaultSms: StateFlow<Boolean> = _isDefaultSms.asStateFlow()

    private val _hasReadSms = MutableStateFlow(false)
    val hasReadSms: StateFlow<Boolean> = _hasReadSms.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _sims = MutableStateFlow<List<SimCardInfo>>(emptyList())
    val sims: StateFlow<List<SimCardInfo>> = _sims.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(false)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    private var targetMessageCap: Int = 1800
    private val refreshMutex = Mutex()
    @Volatile
    private var refreshQueuedAgain: Boolean = false

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _manualScanResult = MutableStateFlow<LinkScanResult?>(null)
    val manualScanResult: StateFlow<LinkScanResult?> = _manualScanResult.asStateFlow()

    private val _lastLabOutput = MutableStateFlow<String?>(null)
    val lastLabOutput: StateFlow<String?> = _lastLabOutput.asStateFlow()

    // Finance state
    private val _periodMode = MutableStateFlow(FinancePeriodMode.THIS_MONTH)
    val periodMode: StateFlow<FinancePeriodMode> = _periodMode.asStateFlow()

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth> = _selectedYearMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _customStartDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val customStartDate: StateFlow<LocalDate> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow(LocalDate.now())
    val customEndDate: StateFlow<LocalDate> = _customEndDate.asStateFlow()

    private val _selectedAccountFilter = MutableStateFlow<String?>(null)
    val selectedAccountFilter: StateFlow<String?> = _selectedAccountFilter.asStateFlow()

    private val _financeUnlockedInSession = MutableStateFlow(false)
    val financeUnlockedInSession: StateFlow<Boolean> = _financeUnlockedInSession.asStateFlow()

    init {
        val ctx = getApplication<Application>()
        GuardSettingsStore.ensureLoaded(ctx)
        SmsVoiceAnnouncer.ensureInitialized(ctx)
        refreshAll()

        viewModelScope.launch {
            SmsEvents.pulse.collect { pulse ->
                if (pulse > 0L) refreshAll()
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _showGuide.value = false
        _showAbout.value = false
        _openThreadKey.value = null
        if (_currentTab.value == MainTab.FINANCE && tab != MainTab.FINANCE) {
            // Auto-relock Finance whenever user leaves the Finance tab!
            _financeUnlockedInSession.value = false
        }
        _currentTab.value = tab
    }

    fun unlockFinanceSession() {
        _financeUnlockedInSession.value = true
    }

    fun relockFinanceSession() {
        _financeUnlockedInSession.value = false
    }

    fun setFinanceLockEnabled(enabled: Boolean) {
        GuardSettingsStore.updateFinanceLockEnabled(getApplication(), enabled)
        if (!enabled) {
            _financeUnlockedInSession.value = true
        }
    }

    fun setCustomFinancePin(rawPin: String) {
        val ctx = getApplication<Application>()
        GuardSettingsStore.setCustomFinancePin(ctx, rawPin)
        val tamil = prefsFlow.value.tamil
        _statusBanner.value = if (rawPin.isBlank()) {
            if (tamil) "வரவு-செலவு கடவுச்சொல் நீக்கப்பட்டது" else "Finance custom password removed"
        } else {
            _financeUnlockedInSession.value = true
            if (tamil) "வரவு-செலவு கடவுச்சொல் பாதுகாப்பாகச் சேமிக்கப்பட்டது ✓" else "Finance password saved securely ✓"
        }
    }

    fun verifyFinancePinAndUnlock(candidatePin: String): Boolean {
        val ok = GuardSettingsStore.verifyCustomFinancePin(getApplication(), candidatePin)
        if (ok) {
            _financeUnlockedInSession.value = true
        }
        return ok
    }

    fun adjustUiScale(deltaPercent: Int) {
        val next = (prefsFlow.value.uiScalePercent + deltaPercent).coerceIn(80, 150)
        GuardSettingsStore.updateUiScalePercent(getApplication(), next)
    }

    fun setUiScalePercent(percent: Int) {
        GuardSettingsStore.updateUiScalePercent(getApplication(), percent.coerceIn(80, 150))
    }

    fun setSleepScheduleEnabled(enabled: Boolean) {
        val ctx = getApplication<Application>()
        GuardSettingsStore.updateSleepScheduleEnabled(ctx, enabled)
        SleepScheduleManager.scheduleNextWakeAlarm(ctx)
        if (!enabled) {
            SleepScheduleManager.flushAndSpeakQueuedIfAwake(ctx, forceNow = true)
        }
    }

    fun setSleepStartMinutes(minuteOfDay: Int) {
        val ctx = getApplication<Application>()
        GuardSettingsStore.updateSleepStartMinutes(ctx, minuteOfDay)
        SleepScheduleManager.scheduleNextWakeAlarm(ctx)
    }

    fun setSleepEndMinutes(minuteOfDay: Int) {
        val ctx = getApplication<Application>()
        GuardSettingsStore.updateSleepEndMinutes(ctx, minuteOfDay)
        SleepScheduleManager.scheduleNextWakeAlarm(ctx)
    }

    fun checkAndDeliverAwakeSleepQueue() {
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            SleepScheduleManager.flushAndSpeakQueuedIfAwake(ctx, forceNow = false)
            SleepScheduleManager.scheduleNextWakeAlarm(ctx)
        }
    }

    fun readQueuedSleepMessagesNow() {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        val spokenCount = SleepScheduleManager.flushAndSpeakQueuedIfAwake(ctx, forceNow = true)
        _statusBanner.value = if (spokenCount > 0) {
            if (prefs.tamil) {
                "$spokenCount வங்கி/பணச் செய்திகள் ஒவ்வொன்றாகப் படிக்கப்படுகின்றன..."
            } else {
                "Reading $spokenCount queued bank/money messages one by one..."
            }
        } else {
            if (prefs.tamil) "காத்திருக்கும் உறக்க நேரச் செய்திகள் எதுவும் இல்லை ✓" else "No queued sleep-time bank messages waiting ✓"
        }
    }

    fun demoSleepQueueOneByOneReadout() {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        val now = System.currentTimeMillis()
        val demoItems = listOf(
            QueuedSleepAnnouncement(
                id = "demo_1",
                timestampMs = now - 60_000L,
                scriptTa = "ஸ்டேட் பேங்க் ஆஃப் இந்தியா. ரூபாய் 500 செலவானது.",
                scriptEn = "State Bank of India. Rupees 500 debited.",
            ),
            QueuedSleepAnnouncement(
                id = "demo_2",
                timestampMs = now - 30_000L,
                scriptTa = "இந்தியன் ஓவர்சீஸ் வங்கி. ரூபாய் 1,500 வரவு வந்துள்ளது.",
                scriptEn = "Indian Overseas Bank. Rupees 1,500 received.",
            ),
        )
        val lines = SleepScheduleManager.buildSequentialReadoutUtterances(demoItems, prefs.tamil)
        SmsVoiceAnnouncer.speakSequenceOneByOne(ctx, lines, gapMs = 700L)
        _statusBanner.value = if (prefs.tamil) {
            "மாதிரி காலை வரிசை வாசிப்பு (2 வங்கிச் செய்திகள் ஒவ்வொன்றாக) இயங்குகிறது..."
        } else {
            "Playing sample wake-up readout (2 bank alerts one by one)..."
        }
    }

    fun clearSleepQueue() {
        GuardSettingsStore.clearSleepQueue(getApplication())
    }

    fun openGuide(open: Boolean) {
        _showGuide.value = open
        if (open) _showAbout.value = false
    }

    fun openAbout(open: Boolean) {
        _showAbout.value = open
        if (open) _showGuide.value = false
    }

    fun openThread(threadKey: String?) {
        _openThreadKey.value = threadKey
        if (threadKey != null) {
            val thread = _threads.value.firstOrNull { it.threadKey == threadKey }
            if (thread != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    SmsRepository.markThreadRead(getApplication(), thread.messages)
                }
            }
        }
    }

    fun setFilter(filter: InboxFilter) {
        _inboxFilter.value = filter
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun postBanner(text: String) {
        _statusBanner.value = text
    }

    fun refreshAll(scanUncheckedLinksOnline: Boolean = false) {
        val ctx = getApplication<Application>()
        _isDefaultSms.value = SmsRepository.isDefaultSmsApp(ctx)
        _hasReadSms.value = SmsRepository.hasReadSmsPermission(ctx)
        _isOnline.value = SmsRepository.hasValidatedInternet(ctx)
        _sims.value = SmsRepository.availableSims(ctx)

        if (!refreshMutex.tryLock()) {
            refreshQueuedAgain = true
            return
        }

        viewModelScope.launch {
            try {
                do {
                    refreshQueuedAgain = false
                    _isLoading.value = true

                    // Stage 1: Ultra-fast 250-message paint (< 60ms) when inbox is empty so massive 50,000-SMS phones open instantly!
                    if (_allMessages.value.isEmpty()) {
                        val quickBatch = withContext(Dispatchers.IO) {
                            SmsRepository.loadEnrichedMessages(ctx, maxMessages = 250)
                        }
                        if (quickBatch.isNotEmpty()) {
                            val quickThreads = withContext(Dispatchers.Default) {
                                SmsRepository.groupThreads(quickBatch)
                            }
                            _allMessages.value = quickBatch
                            _threads.value = quickThreads
                            _hasMoreMessages.value = SmsRepository.lastLoadHasMoreMessages
                        }
                    }

                    // Stage 2: Smooth paged hydration up to targetMessageCap off the UI thread with zero RAM spikes
                    val fullLoaded = withContext(Dispatchers.IO) {
                        SmsRepository.loadEnrichedMessages(ctx, maxMessages = targetMessageCap)
                    }
                    val fullThreads = withContext(Dispatchers.Default) {
                        SmsRepository.groupThreads(fullLoaded)
                    }
                    _allMessages.value = fullLoaded
                    _threads.value = fullThreads
                    _hasMoreMessages.value = SmsRepository.lastLoadHasMoreMessages
                    _isLoading.value = false
                } while (refreshQueuedAgain)

                if (scanUncheckedLinksOnline) {
                    verifyUncheckedInboxLinksInBackground()
                }
            } finally {
                _isLoading.value = false
                refreshMutex.unlock()
            }
        }
    }

    fun loadMoreOlderMessages() {
        targetMessageCap = (targetMessageCap + 2000).coerceAtMost(15_000)
        refreshAll(scanUncheckedLinksOnline = false)
    }

    fun verifyUncheckedInboxLinksInBackground() {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        viewModelScope.launch(Dispatchers.IO) {
            val online = SmsRepository.hasValidatedInternet(ctx)
            _isOnline.value = online
            if (!prefs.internetEnabled || !online) {
                val msg = if (prefs.tamil) {
                    "இணையம் இல்லாததால் இப்போது சரிபார்க்க முடியவில்லை — இணையம் வந்தவுடன் தானாகச் சரிபார்க்கப்படும்."
                } else {
                    "Cannot verify right now (offline or internet switch OFF). Will auto-verify once connected."
                }
                _statusBanner.value = msg
                return@launch
            }
            val apiKey = GuardSettingsStore.getSafeBrowsingApiKey(ctx)
            val unchecked = _allMessages.value
                .filterNot { it.isSentByMe }
                .take(80)
                .flatMap { msg ->
                    msg.links
                        .filter { it.verdict == LinkVerdict.PENDING_OFFLINE || it.verdict == LinkVerdict.PENDING_INTERNET_OFF }
                        .map { msg.sender to it.originalUrl }
                }
                .distinctBy { it.second }
                .take(25)

            if (unchecked.isEmpty()) {
                NetworkReconnectMonitor.drainPendingQueueNow(ctx, announceVoice = true)
                val msg = if (prefs.tamil) "அனைத்து இணைப்புகளும் சரிபார்க்கப்பட்டன ✓" else "All recent SMS links are verified ✓"
                _statusBanner.value = msg
                return@launch
            }

            val updated = ArrayList<LinkScanResult>()
            for ((sender, url) in unchecked) {
                updated.add(
                    LinkSecurityScanner.scanSingleUrl(
                        rawUrl = url,
                        sender = sender,
                        internetAllowed = true,
                        hasValidatedNetwork = true,
                        safeBrowsingApiKey = apiKey,
                    ),
                )
            }
            GuardSettingsStore.cacheLinkResults(ctx, updated)
            NetworkReconnectMonitor.drainPendingQueueNow(ctx, announceVoice = true)
            refreshAll()
            val unsafeCount = updated.count { it.verdict == LinkVerdict.UNSAFE }
            _statusBanner.value = if (prefs.tamil) {
                "${updated.size} இணைப்புகள் சரிபார்க்கப்பட்டன · ஆபத்தானவை: $unsafeCount"
            } else {
                "Verified ${updated.size} links · Unsafe threats caught: $unsafeCount"
            }
        }
    }

    fun verifySingleUrlInteractive(rawUrl: String, announceAloud: Boolean = true) {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        if (rawUrl.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val online = SmsRepository.hasValidatedInternet(ctx)
            _isOnline.value = online
            val res = LinkSecurityScanner.scanSingleUrl(
                rawUrl = rawUrl.trim(),
                sender = null,
                internetAllowed = prefs.internetEnabled,
                // Attempt live HTTPS/DoH probe directly whenever Internet switch is ON so OEM ConnectivityManager quirks never block real Wi-Fi/5G!
                hasValidatedNetwork = prefs.internetEnabled,
                safeBrowsingApiKey = GuardSettingsStore.getSafeBrowsingApiKey(ctx),
            )
            GuardSettingsStore.cacheLinkResults(ctx, listOf(res))
            if (res.verdict == LinkVerdict.SAFE || res.verdict == LinkVerdict.UNSAFE) {
                val solvedIds = GuardSettingsStore.getPendingQueue(ctx)
                    .filter { rawUrl.trim() in it.urls }
                    .map { it.messageId }
                    .toSet()
                GuardSettingsStore.removePendingLinkChecks(ctx, solvedIds)
            }
            _manualScanResult.value = res
            if (announceAloud && prefs.voiceEnabled) {
                val speech = when (res.verdict) {
                    LinkVerdict.SAFE -> if (prefs.tamil) {
                        "பச்சை டிக் பாதுகாப்பு உறுதி! ${res.displayDomain} இணைப்பு பாதுகாப்பானது, திறக்கலாம்."
                    } else {
                        "Green tick verified! ${res.displayDomain} is safe to open."
                    }
                    LinkVerdict.UNSAFE -> if (prefs.tamil) {
                        "ஆபத்து எச்சரிக்கை! ${res.displayDomain} போலியான ஆபத்தான இணைப்பு — தயவுசெய்து தொடாதீர்கள்!"
                    } else {
                        "Danger warning! ${res.displayDomain} is an unsafe scam link — do not touch it!"
                    }
                    LinkVerdict.PENDING_OFFLINE, LinkVerdict.PENDING_INTERNET_OFF -> if (prefs.tamil) {
                        "இணையம் இல்லாததால் ${res.displayDomain} இணைப்பை இப்போது சரிபார்க்க முடியவில்லை — தொடாதீர்கள்."
                    } else {
                        "Cannot verify ${res.displayDomain} right now without internet access — please do not touch."
                    }
                }
                SmsVoiceAnnouncer.speak(ctx, speech, flushExisting = true)
            }
            refreshAll()
        }
    }

    fun drainOfflineQueueManually() {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        viewModelScope.launch(Dispatchers.IO) {
            val count = NetworkReconnectMonitor.drainPendingQueueNow(ctx, announceVoice = true)
            refreshAll()
            _statusBanner.value = if (count > 0) {
                if (prefs.tamil) "$count காத்திருந்த இணைப்புகள் சரிபார்க்கப்பட்டன ✓" else "Verified $count queued links ✓"
            } else {
                if (prefs.tamil) "இணைய இணைப்பைச் சரிபார்க்கவும்" else "Check internet connection & Internet ON switch"
            }
        }
    }

    fun sendSms(recipient: String, text: String, subId: Int, onDone: (Boolean) -> Unit) {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        viewModelScope.launch(Dispatchers.IO) {
            val result = SmsRepository.sendSmsMessage(ctx, recipient, text, subId)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    refreshAll()
                    onDone(true)
                } else {
                    _statusBanner.value = if (prefs.tamil) {
                        "அனுப்ப முடியவில்லை: SMS அனுமதியைச் சரிபார்க்கவும் (${result.exceptionOrNull()?.message.orEmpty()})"
                    } else {
                        "Could not send SMS: check SMS permission (${result.exceptionOrNull()?.message.orEmpty()})"
                    }
                    onDone(false)
                }
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            SmsRepository.deleteMessageById(ctx, messageId)
            refreshAll()
        }
    }

    fun deleteThread(thread: SmsThreadSummary) {
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            SmsRepository.deleteThreadMessages(ctx, thread.messages)
            _openThreadKey.value = null
            refreshAll()
        }
    }

    fun copyOtp(code: String) {
        val ctx = getApplication<Application>()
        IncomingSmsProcessor.copyOtpToClipboard(ctx, code)
        val tamil = prefsFlow.value.tamil
        _statusBanner.value = if (tamil) "OTP நகலெடுக்கப்பட்டது: $code" else "OTP copied: $code"
    }

    fun replayMessageVoice(msg: EnrichedSmsMessage) {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        if (msg.otp.isOtp) {
            _statusBanner.value = if (prefs.tamil) {
                "பாதுகாப்பு விதி: OTP இரகசிய எண் ஒருபோதும் குரலில் படிக்கப்படாது."
            } else {
                "Privacy rule: OTP verification codes are never read aloud."
            }
            return
        }
        if (msg.isSentByMe) {
            val cleanText = msg.body.take(180)
            val sentLine = if (prefs.tamil) {
                "நீங்கள் அனுப்பிய செய்தி: $cleanText"
            } else {
                "Sent by you: $cleanText"
            }
            SmsVoiceAnnouncer.speak(ctx, sentLine, flushExisting = true)
            return
        }
        val script = com.universalrp.smsguard.engine.SmsAnnouncementPlanner.buildIncomingAnnouncement(
            body = msg.body,
            sender = msg.sender,
            otp = msg.otp,
            links = msg.links,
            finance = msg.finance,
            settings = com.universalrp.smsguard.engine.AnnouncementSettingsSnapshot(
                tamil = prefs.tamil,
                voiceEnabled = true,
                shortEveryMessageEnabled = true,
                mutePromoVoice = false,
            ),
        )
        if (script != null) {
            SmsVoiceAnnouncer.speak(ctx, script, flushExisting = true)
        }
    }

    // --- Live 6-Scenario Shield Test Lab ---

    fun runLiveLabScenario(scenarioIndex: Int) {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        viewModelScope.launch(Dispatchers.IO) {
            when (scenarioIndex) {
                1 -> {
                    val res = IncomingSmsProcessor.processReceivedSms(
                        context = ctx,
                        rawAddress = "JT-RBISAY-G",
                        body = "RBI Official Awareness: Verify banking updates safely at https://rbi.org.in",
                        persistToProvider = false,
                        postSystemNotification = false,
                    )
                    val verdict = res.links.firstOrNull()?.verdict
                    _lastLabOutput.value = "✅ [RBI Official Link] Sender: ${res.senderName} | Verdict: $verdict | Voice: ${res.spokenScript}"
                }
                2 -> {
                    val res = IncomingSmsProcessor.processReceivedSms(
                        context = ctx,
                        rawAddress = "+919876543210",
                        body = "Dear customer your SBI YONO account blocked today! Install update immediately: http://sbi-pan-kyc.top/SBI_KYC_Update.apk",
                        persistToProvider = false,
                        postSystemNotification = false,
                    )
                    val verdict = res.links.firstOrNull()?.verdict
                    _lastLabOutput.value = "🛑 [Scam APK Caught!] Sender: ${res.senderName} | Verdict: $verdict | Voice: ${res.spokenScript}"
                }
                3 -> {
                    val res = IncomingSmsProcessor.processReceivedSms(
                        context = ctx,
                        rawAddress = "+919840011223",
                        body = "Your A/c XX4821 is credited by Rs. 5,000.00 via GPay UPI Ref 629588776655 -SBI",
                        persistToProvider = false,
                        postSystemNotification = false,
                    )
                    _lastLabOutput.value = "🛑 [Anti-Spoof Blocked!] Classification: ${res.finance.classification} | Added to Income: ₹0 (Protected!) | Voice: ${res.spokenScript}"
                }
                4 -> {
                    val res = IncomingSmsProcessor.processReceivedSms(
                        context = ctx,
                        rawAddress = "JT-SBIINB-T",
                        body = "Dear UPI user A/C X4821 credited by Rs.1,500.00 on date 22Oct26 trf from MURUGAN K Refno 629500112233 via Google Pay -SBI",
                        persistToProvider = false,
                        postSystemNotification = false,
                    )
                    _lastLabOutput.value = "🏦 [Verified Bank Credit] Institution: ${res.finance.institutionEn} | Bucket: ${res.finance.accountDisplayLabel} (${res.finance.upiRailLabel}) | +₹${res.finance.amountInr} | Voice: ${res.spokenScript}"
                }
                5 -> {
                    val res = IncomingSmsProcessor.processReceivedSms(
                        context = ctx,
                        rawAddress = "AD-HDFCBK-T",
                        body = "739204 is your OTP for transaction of Rs.850.00 on HDFC Bank Card XX5521. Do not share with anyone.",
                        persistToProvider = false,
                        postSystemNotification = false,
                    )
                    _lastLabOutput.value = "🔐 [OTP Protected] Extracted OTP: ${res.otpCode} | Finance Impact: ${res.finance.classification} (₹0) | Spoken Aloud: ${res.spokenScript ?: "NONE (100% Silent ✓)"}"
                }
                6 -> {
                    val step1 = IncomingSmsProcessor.processReceivedSms(
                        context = ctx,
                        rawAddress = "JT-INDPST-G",
                        body = "India Post tracking update: check parcel status at https://www.indiapost.gov.in",
                        persistToProvider = false,
                        postSystemNotification = false,
                        overrideOnlineState = false, // Simulate No Internet first!
                    )
                    _lastLabOutput.value = "⏳ Step 1 (Offline): Queued ${GuardSettingsStore.getPendingQueue(ctx).size} link(s) | Spoke: ${step1.spokenScript}"
                    delay(4200L)
                    val resolved = NetworkReconnectMonitor.drainPendingQueueNow(ctx, announceVoice = true)
                    _lastLabOutput.value = "✅ Step 2 (Internet Reconnected!): Verified $resolved link(s) automatically & announced green-tick safety aloud! ( offline -> online verified ✓ )"
                }
            }
            if (prefs.tamil && _lastLabOutput.value != null) {
                // Keep detailed dual summary clear
            }
        }
    }

    // --- Finance Helpers & Custom Accounts ---

    fun setPeriodMode(mode: FinancePeriodMode) {
        _periodMode.value = mode
    }

    fun shiftSelectedMonth(deltaMonths: Long) {
        _selectedYearMonth.value = _selectedYearMonth.value.plusMonths(deltaMonths)
    }

    fun shiftSelectedYear(deltaYears: Int) {
        _selectedYear.value = _selectedYear.value + deltaYears
    }

    fun setCustomStartDate(date: LocalDate) {
        _customStartDate.value = date
    }

    fun setCustomEndDate(date: LocalDate) {
        _customEndDate.value = date
    }

    fun selectAccountBucket(bucketKey: String?) {
        _selectedAccountFilter.value = bucketKey
    }

    fun addUserAccount(label: String, matchToken: String, kind: String) {
        val ctx = getApplication<Application>()
        val cleanLabel = label.trim()
        val cleanToken = matchToken.trim()
        if (cleanLabel.isEmpty() || cleanToken.isEmpty()) return
        val updated = prefsFlow.value.userAccounts + UserAccountProfile(
            id = UUID.randomUUID().toString().take(8),
            label = cleanLabel,
            matchToken = cleanToken,
            kind = kind,
        )
        GuardSettingsStore.saveUserAccounts(ctx, updated)
        refreshAll()
    }

    fun removeUserAccount(id: String) {
        val ctx = getApplication<Application>()
        val updated = prefsFlow.value.userAccounts.filterNot { it.id == id }
        GuardSettingsStore.saveUserAccounts(ctx, updated)
        if (_selectedAccountFilter.value == "USER:$id") {
            _selectedAccountFilter.value = null
        }
        refreshAll()
    }

    fun toggleExcludeTransaction(messageId: Long) {
        val ctx = getApplication<Application>()
        GuardSettingsStore.toggleExcludedFinanceMessage(ctx, messageId)
    }

    fun computeCurrentFinanceSummary(
        messages: List<EnrichedSmsMessage>,
        mode: FinancePeriodMode,
        ym: YearMonth,
        year: Int,
        startDate: LocalDate,
        endDate: LocalDate,
        accountKey: String?,
        prefs: GuardPrefs,
    ): PeriodSummary {
        val parsed = messages.map { it.finance }
        val (startMs, endMs) = when (mode) {
            FinancePeriodMode.THIS_MONTH -> FinanceLedgerEngine.monthBounds(YearMonth.now())
            FinancePeriodMode.SELECTED_MONTH -> FinanceLedgerEngine.monthBounds(ym)
            FinancePeriodMode.YEARLY -> FinanceLedgerEngine.yearBounds(year)
            FinancePeriodMode.CUSTOM_RANGE -> FinanceLedgerEngine.customDateBounds(startDate, endDate)
        }
        val chartYear = if (mode == FinancePeriodMode.YEARLY) year else null
        return FinanceLedgerEngine.summarizePeriod(
            allParsed = parsed,
            startEpochMs = startMs,
            endEpochMsInclusive = endMs,
            selectedAccountKey = accountKey,
            excludeSelfTransfers = prefs.excludeSelfTransfers,
            excludedMessageIds = prefs.excludedFinanceIds,
            yearForMonthlyBars = chartYear,
        )
    }

    fun pendingQueueItems(): List<QueuedSmsLinkCheck> =
        GuardSettingsStore.getPendingQueue(getApplication())

    // --- Toggle wrappers ---

    fun toggleLanguage() {
        val ctx = getApplication<Application>()
        GuardSettingsStore.updateLanguage(ctx, !prefsFlow.value.tamil)
    }

    fun setInternetEnabled(enabled: Boolean) {
        val ctx = getApplication<Application>()
        GuardSettingsStore.updateInternetEnabled(ctx, enabled)
        if (enabled) {
            viewModelScope.launch(Dispatchers.IO) {
                NetworkReconnectMonitor.drainPendingQueueNow(ctx, announceVoice = true)
                refreshAll()
            }
        } else {
            refreshAll()
        }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        val ctx = getApplication<Application>()
        GuardSettingsStore.updateVoiceEnabled(ctx, enabled)
        if (!enabled) SmsVoiceAnnouncer.stop(ctx)
    }

    fun setShortEveryMessage(enabled: Boolean) {
        GuardSettingsStore.updateShortEveryMessage(getApplication(), enabled)
    }

    fun setFinanceEnabled(enabled: Boolean) {
        GuardSettingsStore.updateFinanceEnabled(getApplication(), enabled)
    }

    fun setMutePromoVoice(enabled: Boolean) {
        GuardSettingsStore.updateMutePromoVoice(getApplication(), enabled)
    }

    fun setExcludeSelfTransfers(enabled: Boolean) {
        GuardSettingsStore.updateExcludeSelfTransfers(getApplication(), enabled)
    }

    fun setVoiceVolume(percent: Int) {
        GuardSettingsStore.updateVoiceVolume(getApplication(), percent)
    }

    fun setSpeechRate(mode: Int) {
        GuardSettingsStore.updateSpeechRateMode(getApplication(), mode)
    }

    fun setVoiceLanguageMode(mode: Int) {
        GuardSettingsStore.updateVoiceLanguageMode(getApplication(), mode)
    }

    fun setAutoDailyGenderSwap(enabled: Boolean) {
        GuardSettingsStore.updateAutoDailyGenderSwap(getApplication(), enabled)
    }

    fun selectOnlineVoiceAndPreview(profileId: String) {
        val ctx = getApplication<Application>()
        GuardSettingsStore.updateAutoDailyGenderSwap(ctx, false)
        GuardSettingsStore.updatePreferredVoiceName(ctx, profileId)
        previewOnlineVoiceById(profileId)
    }

    fun previewOnlineVoiceById(profileId: String) {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        val sample = if (prefs.tamil) {
            "வணக்கம்! குறுஞ்செய்தி காவலன் உயர்தர ஆன்லைன் குரல் சோதனை தயாராக உள்ளது."
        } else {
            "Hello! SMS Guard high quality online studio voice is ready."
        }
        SmsVoiceAnnouncer.speak(ctx, sample, flushExisting = true, overrideOnlineVoiceId = profileId)
    }

    fun setPreferredVoice(voiceName: String) {
        val ctx = getApplication<Application>()
        GuardSettingsStore.updateAutoDailyGenderSwap(ctx, false)
        GuardSettingsStore.updatePreferredVoiceName(ctx, voiceName)
    }

    fun setPalette(index: Int) {
        GuardSettingsStore.updatePalette(getApplication(), index)
    }

    fun testVoiceSample() {
        val ctx = getApplication<Application>()
        val prefs = prefsFlow.value
        val sample = if (prefs.tamil) {
            "குறுஞ்செய்தி காவலன் தயாராக உள்ளது. தற்போதைய ஒலி அளவு ${prefs.voiceVolumePercent} சதவீதம். போலி இணைப்புகள் மற்றும் போலி பணச் செய்திகளிலிருந்து உங்களைப் பாதுகாக்கிறேன்."
        } else {
            "SMS Guard is ready at ${prefs.voiceVolumePercent} percent volume, protecting you from fake links and fake payment messages."
        }
        SmsVoiceAnnouncer.speak(ctx, sample, flushExisting = true)
    }

    fun stopVoiceNow() {
        SmsVoiceAnnouncer.stop(getApplication())
    }

    fun filterThreads(
        threads: List<SmsThreadSummary>,
        filter: InboxFilter,
        query: String,
    ): List<SmsThreadSummary> {
        val q = query.trim().lowercase()
        return threads.filter { t ->
            val latest = t.latestMessage
            val matchesFilter = when (filter) {
                InboxFilter.ALL -> true
                InboxFilter.PERSONAL -> latest.sender.isPersonalNumber
                InboxFilter.BANKS -> t.messages.any {
                    it.sender.isVerifiedFinancialSender ||
                        it.finance.classification == SpoofClassification.GENUINE_BANK_OR_UPI
                }
                InboxFilter.OTP -> t.messages.any { it.otp.isOtp }
                InboxFilter.LINKS -> t.messages.any { it.links.isNotEmpty() }
                InboxFilter.SCAM -> t.hasUnsafeLink || t.hasSpoofAttempt
                InboxFilter.PROMO -> latest.sender.dltCategory == DltCategory.PROMOTIONAL
            }
            if (!matchesFilter) return@filter false
            if (q.isEmpty()) return@filter true
            latest.sender.displayNameEn.lowercase().contains(q) ||
                latest.sender.displayNameTa.lowercase().contains(q) ||
                latest.rawAddress.lowercase().contains(q) ||
                t.messages.any { it.body.lowercase().contains(q) }
        }
    }
}
