package com.universalrp.smsguard.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.universalrp.smsguard.MainActivity
import com.universalrp.smsguard.R
import com.universalrp.smsguard.data.GuardSettingsStore

/** No PDU parsing, sender/body capture, download, speech or upload: this is NOT MMS support. */
object UnsupportedMmsNotice {
    fun record(context: Context) {
        GuardSettingsStore.recordUnsupportedMms(context)
        val tamil = GuardSettingsStore.state.value.tamil
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel("smsguard_mms_unavailable",
            if (tamil) "MMS ஆதரவு இல்லை" else "Unsupported MMS notices", NotificationManager.IMPORTANCE_HIGH))
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(context, 7105, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = if (tamil) "MMS பெறப்படவில்லை — SMS-only பயன்முறை" else "MMS not received — SMS-only mode"
        val body = if (tamil) "முழுமையான செய்திச் செயலிக்கு மாறவும்; அனுப்புநர் மீண்டும் அனுப்ப வேண்டியிருக்கலாம்."
            else "Switch to a full messaging app; the sender may need to resend it."
        val notification = NotificationCompat.Builder(context, "smsguard_mms_unavailable")
            .setSmallIcon(R.drawable.ic_stat_shield).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET).setAutoCancel(true).build()
        runCatching { manager.notify(7105, notification) }
    }
}
