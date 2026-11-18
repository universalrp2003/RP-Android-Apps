package com.oorarivippu.voiceannouncer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.GeomagneticField
import android.location.Location
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.*

internal fun compassRelativeBearing(trueBearing: Float, magneticHeading: Float, declination: Float): Float =
    ((trueBearing - (magneticHeading + declination)) % 360f + 360f) % 360f

private data class CompassReading(val heading: Float? = null, val status: Int = SensorManager.SENSOR_STATUS_UNRELIABLE, val available: Boolean = true)

@Composable
private fun rememberCompass(): CompassReading {
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    var reading by remember { mutableStateOf(CompassReading()) }
    DisposableEffect(context, lifecycle) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        // A game rotation vector has no north reference and must never be used as a compass.
        val magnetic = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        var accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, value: Int) { accuracy = value; if (value == SensorManager.SENSOR_STATUS_UNRELIABLE) reading = CompassReading() }
            override fun onSensorChanged(event: SensorEvent) {
                val rotation = FloatArray(9)
                val adjusted = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                @Suppress("DEPRECATION")
                val displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
                val axes = when (displayRotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(rotation, axes.first, axes.second, adjusted)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(adjusted, orientation)
                val heading = ((Math.toDegrees(orientation[0].toDouble()).toFloat() % 360f) + 360f) % 360f
                reading = CompassReading(heading, event.accuracy.takeIf { it >= 0 } ?: accuracy)
            }
        }
        if (sensor == null || magnetic == null) reading = CompassReading(available = false)
        else if (lifecycle.isAtLeast(Lifecycle.State.RESUMED)) {
            reading = CompassReading()
            if (!manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)) reading = CompassReading(available = false)
        } else reading = CompassReading()
        onDispose { manager.unregisterListener(listener) }
    }
    return reading
}

@Composable
internal fun CompassSection(state: AppStateSnapshot, tamil: Boolean, content: @Composable (Float?) -> Unit) {
    if (!state.compassEnabled) {
        content(null)
        return
    }
    val reading = rememberCompass()
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycle) {
        while (lifecycle.isAtLeast(Lifecycle.State.RESUMED)) { now = SystemClock.elapsedRealtime(); delay(1000) }
    }
    val heading = reading.heading.takeIf { reading.available && reading.status != SensorManager.SENSOR_STATUS_UNRELIABLE }
    val lat = state.tripLatitude
    val lon = state.tripLongitude
    val targetLat = state.destinationLatitude
    val targetLon = state.destinationLongitude
    val fresh = TripProgressPolicy.fresh(state.isTrackingEnabled, state.isGpsEnabled, state.tripFixElapsedMs, now)
    val relative = if (heading != null && reading.status >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && fresh && lat != null && lon != null && targetLat != null && targetLon != null && state.destinationName.isNotBlank()) {
        val results = FloatArray(2)
        Location.distanceBetween(lat, lon, targetLat, targetLon, results)
        if (results[0] < 10f) null else {
            val declination = GeomagneticField(lat.toFloat(), lon.toFloat(), 0f, System.currentTimeMillis()).declination
            compassRelativeBearing(results[1], heading, declination)
        }
    } else null
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().background(brush = Brush.linearGradient(listOf(Color(0xFFE2F8F4), Color(0xFFE7F0FF), Color(0xFFFFF1DC))), shape = RoundedCornerShape(16.dp)).padding(8.dp)) {
        BallCompass(heading)
        Text(if (!reading.available) { if (tamil) "திசைகாட்டி சென்சார் இல்லை" else "Compass sensor unavailable" }
            else if (heading == null || reading.status == SensorManager.SENSOR_STATUS_ACCURACY_LOW) { if (tamil) "திசைகாட்டியைச் சரிசெய்ய கைபேசியை 8 வடிவில் அசைக்கவும்" else "Calibrate: move phone in a figure eight" }
            else if (tamil) "காந்த வடக்கு • கைபேசியைத் தட்டையாகப் பிடிக்கவும்" else "Magnetic north • hold phone flat", fontSize = 11.sp)
    }
    content(relative)
}

@Composable
private fun BallCompass(heading: Float?) {
    Canvas(Modifier.size(130.dp)) {
        val r = size.minDimension * .46f
        val c = center
        // Jewel-toned face, metallic rim and a soft highlight retain contrast for the labels.
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF69DFD0), Color(0xFF126E80), Color(0xFF102B4F), Color(0xFF071322)), center = Offset(c.x-r*.38f,c.y-r*.50f), radius = r*1.7f), radius = r)
        drawCircle(Color(0xFFFFD47A).copy(alpha = .85f), radius = r * .98f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        drawCircle(Color.White.copy(alpha = .16f), radius = r * .22f, center = Offset(c.x-r*.34f, c.y-r*.52f))
        // Longitude marks are projected onto a sphere; the rear hemisphere is hidden.
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        for (deg in if (heading == null) emptyList() else (0 until 360 step 10).toList()) {
            val angle = Math.toRadians((deg - (heading ?: 0f)).toDouble())
            val depth = cos(angle).toFloat()
            if (depth <= 0f) continue
            val x = c.x + r * sin(angle).toFloat() * .91f
            val y = c.y + r * .17f * (1f - depth)
            val major = deg % 90 == 0
            val tickHeight = if (major) r*.18f else if (deg % 30 == 0) r*.12f else r*.07f
            drawLine(Color(0xFFFFE1A0).copy(alpha = .25f + depth*.7f), Offset(x, y + r*.24f), Offset(x, y + r*.24f + tickHeight), strokeWidth = if (major) 3f else 1.5f)
            if (major || deg % 30 == 0) {
                val label = when (deg) { 0 -> "N"; 90 -> "E"; 180 -> "S"; 270 -> "W"; else -> deg.toString() }
                paint.color = when (deg) { 0 -> android.graphics.Color.rgb(255, 113, 97); 90, 270 -> android.graphics.Color.rgb(140, 244, 230); 180 -> android.graphics.Color.rgb(255, 217, 135); else -> android.graphics.Color.WHITE }
                paint.alpha = (255 * (.3f + depth*.7f)).toInt()
                paint.textSize = r * (if (major) .28f else .13f) * (.55f + .45f*depth)
                drawIntoCanvas { it.nativeCanvas.drawText(label, x, y + r*.18f, paint) }
            }
        }
        drawCircle(Color.White.copy(alpha = .18f), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        drawLine(Color(0xFFFFCA55), Offset(c.x, c.y-r*.72f), Offset(c.x, c.y-r*.48f), strokeWidth = 4f)
    }
}

@Composable
internal fun DestinationDirection(relative: Float?, selected: Boolean, tamil: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (relative != null) {
            Text("➤", modifier = Modifier.graphicsCompassRotation(relative), color = Color(0xFF0F766E), fontSize = 24.sp)
            Text(if (tamil) "இலக்கு (நேர்கோடு)" else "Destination (straight line)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        } else Text(if (!selected) { if (tamil) "இலக்கைத் தேர்வுசெய்க" else "Set destination" } else { if (tamil) "திசை கிடைக்கவில்லை" else "Direction unavailable" }, fontSize = 10.sp, color = Color(0xFF64748B))
    }
}

private fun Modifier.graphicsCompassRotation(degrees: Float): Modifier = this.then(Modifier.rotateForCompass(degrees - 90f))
private fun Modifier.rotateForCompass(degrees: Float): Modifier = this.graphicsLayer(rotationZ = degrees)
