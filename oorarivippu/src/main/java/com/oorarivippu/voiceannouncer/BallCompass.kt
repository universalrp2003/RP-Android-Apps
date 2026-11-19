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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.*

internal fun compassRelativeBearing(trueBearing: Float, magneticHeading: Float, declination: Float): Float =
    ((trueBearing - (magneticHeading + declination)) % 360f + 360f) % 360f

private data class CompassReading(val heading: Float? = null, val rotation: FloatArray? = null, val status: Int = SensorManager.SENSOR_STATUS_UNRELIABLE, val available: Boolean = true)

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
                if (!SensorManager.remapCoordinateSystem(rotation, axes.first, axes.second, adjusted) || adjusted.any { !it.isFinite() }) {
                    reading = CompassReading()
                    return
                }
                val orientation = FloatArray(3)
                SensorManager.getOrientation(adjusted, orientation)
                val heading = ((Math.toDegrees(orientation[0].toDouble()).toFloat() % 360f) + 360f) % 360f
                reading = CompassReading(heading = heading, rotation = adjusted, status = event.accuracy.takeIf { it >= 0 } ?: accuracy)
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
    val rotation = reading.rotation.takeIf { reading.available && reading.status != SensorManager.SENSOR_STATUS_UNRELIABLE }
    val heading = reading.heading.takeIf { rotation != null && compassPhoneIsFlat(rotation) }
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
        BallCompass(heading, rotation, tamil)
        Text(if (!reading.available) { if (tamil) "திசைகாட்டி சென்சார் இல்லை" else "Compass sensor unavailable" }
            else if (rotation == null || reading.status == SensorManager.SENSOR_STATUS_ACCURACY_LOW) { if (tamil) "திசைகாட்டியைச் சரிசெய்ய கைபேசியை 8 வடிவில் அசைக்கவும்" else "Calibrate: move phone in a figure eight" }
            else if (tamil) "காந்த வடக்கு • கைபேசியைத் தட்டையாகப் பிடிக்கவும்" else "Magnetic north • hold phone flat", fontSize = 11.sp)
    }
    content(relative)
}

@Composable
internal fun BallCompass(heading: Float?, rotation: FloatArray?, tamil: Boolean) {
    val description = if (heading == null) {
        if (rotation != null) { if (tamil) "திசை அறிய கைபேசியைத் தட்டையாகப் பிடிக்கவும்" else "Hold phone flat for direction" }
        else { if (tamil) "திசைகாட்டி தகவல் இல்லை" else "Compass reading unavailable" }
    } else {
        val degree = ((heading.roundToInt() % 360) + 360) % 360
        if (tamil) "காந்தத் திசை $degree பாகை" else "Magnetic heading $degree degrees"
    }
    Canvas(Modifier.size(170.dp).semantics { contentDescription = description }) {
        val r = size.minDimension * .43f
        val c = Offset(center.x, center.y - r * .02f)
        // Rounded base and metallic bezel give the sphere a physical body.
        drawOval(brush = Brush.radialGradient(listOf(Color(0x660D2431), Color.Transparent),
            center = Offset(c.x, c.y+r*.97f), radius = r*.98f),
            topLeft = Offset(c.x-r*1.12f,c.y+r*.75f), size = Size(r*2.24f,r*.43f))
        drawCircle(brush = Brush.linearGradient(listOf(Color(0xFF708A8C), Color(0xFF1E3539), Color(0xFFACB6AE), Color(0xFF1B272C))), radius = r * 1.045f, center = c)
        drawCircle(Color(0xFF071115), radius = r * 1.005f, center = c)
        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF285459), Color(0xFF102A32), Color(0xFF061319), Color(0xFF010609)),
            center = Offset(c.x-r*.38f,c.y-r*.53f), radius = r*1.8f), radius = r, center = c)

        fun point(bearing: Float, latitude: Float) = projectCompassPoint(bearing, latitude, rotation!!)
        fun offset(p: CompassPoint) = Offset(c.x+p.x*r, c.y+p.y*r)
        // Latitude line projected on the sphere, not a flat horizontal ruler.
        if (rotation != null) {
            val belt = Path()
            var started = false
            for (deg in 0..360 step 2) {
                val p = point(deg.toFloat(), -14f)
                if (p.depth > 0f) {
                    val q = offset(p)
                    if (!started) belt.moveTo(q.x, q.y) else belt.lineTo(q.x, q.y)
                    started = true
                } else started = false
            }
            drawPath(belt, Color(0xFF81C0B2).copy(alpha = .46f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = r*.012f))
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
            }
            for (degree in 0 until 360 step 10) {
                val cardinal = degree % 90 == 0
                val near = point(degree.toFloat(), -3f)
                val far = point(degree.toFloat(), if (degree % 30 == 0) -14f else -10f)
                if (near.depth > .08f && far.depth > .08f) {
                    drawLine(Color(0xFFE5F8E9).copy(alpha = (.25f + .75f*near.depth).coerceIn(0f,1f)),
                        offset(near), offset(far), strokeWidth = r * if (degree % 30 == 0) .025f else .013f)
                }
                if (degree % 30 == 0 && !cardinal) {
                    val p = point(degree.toFloat(), 5f)
                    if (p.depth > .24f) {
                        paint.color = android.graphics.Color.WHITE
                        paint.alpha = (255 * p.depth.coerceIn(0f,1f)).toInt()
                        paint.textSize = r*.14f
                        drawIntoCanvas { canvas ->
                            val native = canvas.nativeCanvas
                            native.save()
                            native.translate(c.x+p.x*r, c.y+p.y*r)
                            native.scale(p.depth.coerceIn(.35f,1f), 1f)
                            native.drawText(degree.toString(), 0f, paint.textSize*.35f, paint)
                            native.restore()
                        }
                    }
                }
            }
            // Eight directions share the same spherical longitude as their tick.
            for (degree in 0 until 360 step 45) {
                val p = point(degree.toFloat(), 25f)
                if (p.depth <= .20f) continue
                val label = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[degree / 45]
                paint.color = if (degree % 90 == 0) android.graphics.Color.rgb(92, 244, 169) else android.graphics.Color.WHITE
                paint.alpha = (255 * p.depth.coerceIn(0f,1f)).toInt()
                paint.textSize = r * if (degree % 90 == 0) .30f else .19f
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    native.save()
                    native.translate(c.x+p.x*r, c.y+p.y*r)
                    val up = point(degree.toFloat(), 25.5f)
                    val angle = Math.toDegrees(atan2((up.x - p.x).toDouble(), (p.y - up.y).toDouble())).toFloat()
                    native.rotate(angle)
                    native.scale(p.depth.coerceIn(.35f,1f), 1f)
                    native.drawText(label, 0f, paint.textSize*.34f, paint)
                    native.restore()
                }
            }
        }
        // Glass reflections stay on the case; the magnetic markings rotate underneath.
        drawOval(brush = Brush.radialGradient(listOf(Color.White.copy(alpha=.28f), Color.Transparent),
            center = Offset(c.x-r*.38f,c.y-r*.55f), radius = r*.66f),
            topLeft = Offset(c.x-r*.78f,c.y-r*.86f), size = Size(r*.85f,r*.55f))
        drawCircle(Color.White.copy(alpha=.25f), radius = r*.98f, center = c,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = r*.015f))
        // Fixed case index: the centre of the moving compass scale is the phone's heading.
        if (heading != null) {
            val index = Path().apply {
                moveTo(c.x, c.y-r*.32f)
                lineTo(c.x-r*.075f, c.y-r*.51f)
                lineTo(c.x+r*.075f, c.y-r*.51f)
                close()
            }
            drawPath(index, Color(0xFFFFC65C))
        }
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
