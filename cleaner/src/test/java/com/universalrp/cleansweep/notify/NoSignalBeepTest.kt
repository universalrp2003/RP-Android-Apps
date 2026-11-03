package com.universalrp.cleansweep.notify
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import com.universalrp.cleansweep.voice.Announcer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
class NoSignalBeepTest {
    private val c:Context get()=RuntimeEnvironment.getApplication()
    private val audio get()=c.getSystemService(AudioManager::class.java)
    private val nm get()=c.getSystemService(NotificationManager::class.java)
    @Before fun init() { c.getSharedPreferences(NoSignalBeep.PREFS,0).edit().clear().commit();Announcer.prefs(c).edit().clear().commit();Announcer.setQuietHours(c,0,0);audio.mode=AudioManager.MODE_NORMAL;audio.ringerMode=AudioManager.RINGER_MODE_NORMAL;audio.setStreamVolume(AudioManager.STREAM_MUSIC,5,0);shadowOf(nm).setNotificationPolicyAccessGranted(true);nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);Settings.Global.putInt(c.contentResolver,Settings.Global.AIRPLANE_MODE_ON,0) }
    private fun enable() { c.getSharedPreferences(NoSignalBeep.PREFS,0).edit().putBoolean("enabled",true).commit() }
    @Test fun defaultsOff() { assertFalse(NoSignalBeep.enabled(c));assertFalse(NoSignalBeep.canPlay(c)) }
    @Test fun safeGateDoesNotChangeMediaVolumeOrVoiceMaster() { enable();val before=audio.getStreamVolume(AudioManager.STREAM_MUSIC);val voice=Announcer.enabled(c);assertTrue(NoSignalBeep.canPlay(c));NoSignalBeep().sample(c);assertEquals(before,audio.getStreamVolume(AudioManager.STREAM_MUSIC));assertEquals(voice,Announcer.enabled(c)) }
    @Test fun silentAndVibrateBlock() { enable();audio.ringerMode=AudioManager.RINGER_MODE_SILENT;assertFalse(NoSignalBeep.canPlay(c));audio.ringerMode=AudioManager.RINGER_MODE_VIBRATE;assertFalse(NoSignalBeep.canPlay(c)) }
    @Test fun quietHoursAndAirplaneBlock() { enable();val hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);Announcer.setQuietHours(c,hour,(hour+1)%24);assertFalse(NoSignalBeep.canPlay(c));Announcer.setQuietHours(c,0,0);Settings.Global.putInt(c.contentResolver,Settings.Global.AIRPLANE_MODE_ON,1);assertFalse(NoSignalBeep.canPlay(c)) }
    @Test fun callAndDndBlock() { enable();audio.mode=AudioManager.MODE_IN_COMMUNICATION;assertFalse(NoSignalBeep.canPlay(c));audio.mode=AudioManager.MODE_NORMAL;nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);assertFalse(NoSignalBeep.canPlay(c)) }
    @Test fun mutedMediaBlocks() { enable();audio.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);assertFalse(NoSignalBeep.canPlay(c));NoSignalBeep().sample(c);assertEquals(0,audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
}
