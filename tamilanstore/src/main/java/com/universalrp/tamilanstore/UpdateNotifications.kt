package com.universalrp.tamilanstore

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

object UpdateNotifications {
    fun enabled(context: Context) = context.getSharedPreferences("updates",0).getBoolean("enabled",false)
    fun setEnabled(context: Context,enabled: Boolean) {
        context.getSharedPreferences("updates",0).edit().putBoolean("enabled",enabled).apply()
        val manager=WorkManager.getInstance(context)
        if(enabled) manager.enqueueUniquePeriodicWork("daily-catalog-updates",ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CatalogUpdateWorker>(24,TimeUnit.HOURS).setInitialDelay(24,TimeUnit.HOURS).build())
        else { manager.cancelUniqueWork("daily-catalog-updates");context.getSystemService(NotificationManager::class.java).cancel(1301) }
    }
}
class CatalogUpdateWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val context=applicationContext
        if(!UpdateNotifications.enabled(context)) return Result.success()
        val prefs=context.getSharedPreferences("updates",0)
        val now=System.currentTimeMillis();val last=prefs.getLong("last-notified",0)
        if(now-last<TimeUnit.HOURS.toMillis(24)) return Result.success()
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return Result.success()
        return try {
            val apps=Catalog.parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
            val updates=apps.filter { app -> InstalledStatus.resolve(StoreInventory.inspect(context,app),app.version)==AppStatus.UPDATE_AVAILABLE }
            if(updates.isEmpty()) return Result.success()
            val manager=context.getSystemService(NotificationManager::class.java)
            val channel=NotificationChannel("catalog-updates-silent-v1","Updates · ஒலியில்லா புதுப்பிப்புகள்",NotificationManager.IMPORTANCE_LOW).apply { setSound(null,null);enableVibration(false) }
            manager.createNotificationChannel(channel)
            if(!manager.areNotificationsEnabled() || manager.getNotificationChannel(channel.id).importance==NotificationManager.IMPORTANCE_NONE) return Result.success()
            val tamil=context.getSharedPreferences("MainActivity",0).getBoolean("tamil",true)
            val intent=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(1301,NotificationCompat.Builder(context,channel.id).setSmallIcon(R.drawable.ic_store)
                .setContentTitle(if(tamil)"புதுப்பிப்புகள் உள்ளன" else "Updates available")
                .setContentText(if(tamil)"பட்டியலுடன் ஒப்பிடும்போது ${updates.size} செயலிகள். திறந்து சரிபார்க்கவும்." else "${updates.size} apps compared with the catalog. Open to review.")
                .setContentIntent(intent).setAutoCancel(true).setSilent(true).setOnlyAlertOnce(true).build())
            prefs.edit().putLong("last-notified",now).apply()
            Result.success()
        } catch(_:Exception) { Result.success() } // never spam retries or failure notifications
    }
}
