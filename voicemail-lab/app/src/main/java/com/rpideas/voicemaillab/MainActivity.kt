package com.rpideas.voicemaillab

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** Deliberately NOT a voicemail: no answering, greeting, background service or audio storage. */
class MainActivity : Activity() {
    private lateinit var result: TextView
    private lateinit var micButton: Button
    private lateinit var callButton: Button
    private val busy = AtomicBoolean(false)
    private val cancel = AtomicBoolean(false)
    private var pendingSource: Int? = null
    private val lock = Any()
    private var recorder: AudioRecord? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(26, 28, 26, 28)
            setBackgroundColor(0xfff3faf8.toInt())
        }
        fun label(text: String, size: Float = 16f): TextView = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(0xff173e39.toInt()); setPadding(0, 8, 0, 8)
            column.addView(this)
        }
        label("Voice Mail · feasibility lab", 24f)
        label("சோதனை மட்டும் · இது குரல் அஞ்சல் செயலி அல்ல", 17f)
        label("This is NOT voicemail. It cannot answer calls, play a greeting or save messages. It only tests whether Android permits audio access on this phone.")
        label("No internet, contacts, phone permissions or background service. Audio is analysed for 5 seconds in memory only; no recording is saved or uploaded. Results do not identify whose voice was captured.")
        label("1. Without a call, test the microphone. 2. With another consenting person, make a normal test call and keep this screen open. Ask them to speak only with permission, then tap Test caller audio. Do not use an emergency call.")
        label("மற்றவர் அனுமதியுடன் மட்டுமே சோதிக்கவும். ஒலி சேமிக்கப்படாது. எண்ணிக்கை வந்தாலும் மறுமுனைக் குரல் உறுதி அல்ல.")
        micButton = Button(this).apply { text = "1 · Test microphone"; setOnClickListener { requestTest(MediaRecorder.AudioSource.MIC) }; column.addView(this) }
        callButton = Button(this).apply { text = "2 · Test caller audio (during call)"; setOnClickListener { requestTest(MediaRecorder.AudioSource.VOICE_DOWNLINK) }; column.addView(this) }
        result = label("Not tested. / இன்னும் சோதிக்கவில்லை.")
        result.gravity = Gravity.START
        setContentView(ScrollView(this).apply { addView(column) })
    }
    private fun requestTest(source: Int) {
        if (busy.get()) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingSource = source
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 21)
        } else startTest(source)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 21) return
        val source = pendingSource; pendingSource = null
        // Require another deliberate tap after permission; never start while permission UI closes.
        result.text = if (source != null && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
            "Microphone permission granted. Tap the test button again to start."
        else "Permission denied. No test was run."
    }
    private fun startTest(source: Int) {
        if (!busy.compareAndSet(false, true)) return
        cancel.set(false); micButton.isEnabled = false; callButton.isEnabled = false
        result.text = "Testing for up to 5 seconds… Keep this screen open."
        Thread {
            val stats = SignalStats()
            var message: String
            var local: AudioRecord? = null
            try {
                val rate = 16000
                val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                check(minimum > 0) { "Audio format unavailable" }
                local = AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, 4096))
                synchronized(lock) { recorder = local }
                check(local.state == AudioRecord.STATE_INITIALIZED) { "Audio source unavailable or not permitted" }
                if (cancel.get()) throw InterruptedException()
                local.startRecording()
                check(local.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Audio source did not start" }
                val samples = ShortArray(1024)
                val deadline = SystemClock.elapsedRealtime() + 5000
                while (!cancel.get() && SystemClock.elapsedRealtime() < deadline) {
                    val n = local.read(samples, 0, samples.size)
                    if (n < 0) throw IllegalStateException("Audio read failed ($n)")
                    if (n > 0) stats.add(samples, n)
                }
                samples.fill(0)
                message = if (cancel.get()) "Cancelled. No audio was saved." else if (stats.count == 0L)
                    "No samples received. This does not prove caller audio is available."
                else String.format(Locale.US, "Test complete: %d samples, peak %d, RMS %.1f.\nThese numbers do NOT prove caller speech was captured. Android may supply silence or other audio. No audio saved.", stats.count, stats.peak, stats.rms())
            } catch (e: SecurityException) {
                message = "Blocked by Android: ${e.javaClass.simpleName}. Caller audio is not available to this ordinary app on this attempt. No audio saved."
            } catch (e: Exception) {
                message = if (cancel.get()) "Cancelled. No audio was saved." else "Test could not run: ${e.javaClass.simpleName}. The phone may not allow this audio source. No audio saved."
            } finally {
                synchronized(lock) { if (recorder === local) recorder = null }
                try { local?.stop() } catch (_: Exception) { }
                try { local?.release() } catch (_: Exception) { }
            }
            busy.set(false)
            runOnUiThread { if (!isFinishing && !isDestroyed) { result.text = message; micButton.isEnabled = true; callButton.isEnabled = true } }
        }.start()
    }
    override fun onStop() {
        cancel.set(true)
        synchronized(lock) { try { recorder?.stop() } catch (_: Exception) { } }
        super.onStop()
    }
}
