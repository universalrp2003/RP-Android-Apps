package com.oorarivippu.voiceannouncer

import kotlin.math.cos
import kotlin.math.sin

/** Unit-sphere orthographic projection, viewed slightly from above. Coordinates are relative
 * to the centre/radius of the ball. Longitude zero is directly under the fixed index. */
internal data class CompassPoint(val x: Float, val y: Float, val depth: Float)
internal fun projectCompassPoint(bearing: Float, heading: Float, latitude: Float): CompassPoint {
    val longitude = Math.toRadians((bearing - heading).toDouble())
    val lat = Math.toRadians(latitude.toDouble())
    val tilt = Math.toRadians(18.0)
    val x = cos(lat) * sin(longitude)
    val y = -(sin(lat) * cos(tilt) - cos(lat) * cos(longitude) * sin(tilt))
    val z = sin(lat) * sin(tilt) + cos(lat) * cos(longitude) * cos(tilt)
    return CompassPoint(x.toFloat(), y.toFloat(), z.toFloat())
}
