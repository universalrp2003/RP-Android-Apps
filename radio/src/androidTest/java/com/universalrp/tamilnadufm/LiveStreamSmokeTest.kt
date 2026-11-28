package com.universalrp.tamilnadufm

import android.content.ComponentName
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.universalrp.tamilnadufm.player.PlaybackService
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Network-dependent smoke observations, not identity, content, rights or audible checks. */
@RunWith(AndroidJUnit4::class)
class LiveStreamSmokeTest {
    @Test fun thoniMp3ReachesPlayingState() = checkStream("https://stream.zeno.fm/qbzdpeuqz7zuv")
    @Test fun kadalOsaiHlsReachesPlayingState() = checkStream("https://d3lbpn4ocx96ou.cloudfront.net/kadalosaifm.m3u8")

    private fun checkStream(url: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").close()
        // Android 15 requires the app to be foreground to request audio focus.
        val activity = instrumentation.startActivitySync(
            android.content.Intent(context, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        lateinit var future: ListenableFuture<MediaController>
        instrumentation.runOnMainSync {
            future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        }
        val controller = future.get(20, TimeUnit.SECONDS)
        try {
            instrumentation.runOnMainSync {
                controller.setMediaItem(MediaItem.Builder().setUri(url).setMediaId(url).build())
                controller.prepare()
                controller.play()
            }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45)
            var playing = false
            var diagnostic = "not started"
            while (System.nanoTime() < deadline && !playing) {
                instrumentation.runOnMainSync {
                    playing = controller.playbackState == Player.STATE_READY && controller.isPlaying
                    diagnostic = "state=${controller.playbackState}, playWhenReady=${controller.playWhenReady}, suppression=${controller.playbackSuppressionReason}, error=${controller.playerError}"
                }
                if (!playing) Thread.sleep(500)
            }
            assertTrue("Stream did not reach playing state: $diagnostic", playing)
        } finally {
            instrumentation.runOnMainSync {
                controller.pause()
                controller.clearMediaItems()
                MediaController.releaseFuture(future)
                activity.finish()
            }
        }
    }
}
