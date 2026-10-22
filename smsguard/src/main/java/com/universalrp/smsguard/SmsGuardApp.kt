package com.universalrp.smsguard

import android.app.Application
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.smsguard.service.IncomingSmsProcessor
import com.universalrp.smsguard.service.NetworkReconnectMonitor

class SmsGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GuardSettingsStore.ensureLoaded(this)
        IncomingSmsProcessor.ensureChannels(this)
        NetworkReconnectMonitor.start(this)
    }
}
