package com.oorarivippu.voiceannouncer

import kotlin.math.cos
import kotlin.math.sin

/** A point on the spherical compass surface, projected into the fixed phone case. */
internal data class CompassPoint(val x: Float, val y: Float, val depth: Float)

/**
 * Android's 3x3 rotation matrix maps screen-adjusted device axes to magnetic East,
 * North and Up. Its transpose maps each earth-fixed marking back to the phone. This
 * applies the complete rotation, not just the azimuth: pitch and roll move the
 * markings under the stationary glass. A small camera tilt exposes the ball's top.
 */
internal fun projectCompassPoint(bearing: Float, latitude: Float, rotation: FloatArray): CompassPoint {
    require(rotation.size == 9)
    val longitude = Math.toRadians(bearing.toDouble())
    val lat = Math.toRadians(latitude.toDouble())
    val east = cos(lat) * sin(longitude)
    val north = cos(lat) * cos(longitude)
    val up = sin(lat)
    val x = rotation[0] * east + rotation[3] * north + rotation[6] * up
    val y = rotation[1] * east + rotation[4] * north + rotation[7] * up
    val z = rotation[2] * east + rotation[5] * north + rotation[8] * up
    val cameraTilt = Math.toRadians(18.0)
    return CompassPoint(x.toFloat(), (-(z*cos(cameraTilt) - y*sin(cameraTilt))).toFloat(),
        (z*sin(cameraTilt) + y*cos(cameraTilt)).toFloat())
}

/** Heading and destination arrow are offered only while the screen faces sufficiently upward. */
internal fun compassPhoneIsFlat(rotation: FloatArray): Boolean =
    rotation.size == 9 && rotation[8].isFinite() && rotation[8] >= .70f
