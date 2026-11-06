package com.oorarivippu.voiceannouncer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/** Explicit foreground-only sample. Never changes trip, family, destination or saved location data. */
internal class GpsDiagnosticProbe(private val context: Context, private val lifecycle: androidx.lifecycle.Lifecycle? = null, private val ended: () -> Unit) {
    companion object { const val DURATION_MS = 30_000L }
    private val manager = context.getSystemService(LocationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    var running = false
        private set
    private val deadline = Runnable { stop() }
    private val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
        if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP || event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) stop()
    }
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) { /* Receiver observation only; no storage/upload. */ }
        override fun onProviderDisabled(provider: String) { stop() }
        override fun onProviderEnabled(provider: String) {}
        @Deprecated("Platform callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (lifecycle != null && !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return false
        if (running || context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return false
        return try {
            if (manager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != true) return false
            running = true
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                val request = android.location.LocationRequest.Builder(1_000L).setDurationMillis(DURATION_MS).build()
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, request, context.mainExecutor, listener)
            } else {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, listener, Looper.getMainLooper())
            }
            lifecycle?.addObserver(observer)
            handler.postDelayed(deadline, DURATION_MS)
            true
        } catch (_: Exception) { stop(); false }
    }
    fun stop() {
        handler.removeCallbacks(deadline)
        lifecycle?.removeObserver(observer)
        runCatching { manager?.removeUpdates(listener) }
        val wasRunning = running
        running = false
        if (wasRunning) ended()
    }
}
