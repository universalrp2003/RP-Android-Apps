package com.universalrp.weather

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private fun pickLang(tamil: Boolean, en: String, ta: String): String = if (tamil) ta else en

/**
 * Compact mini floating weather animation strip displayed near Live News / app header across
 * Live Guard, Ramesh Radio, and Oor Arivippu ONLY when rain (`~30 min`), heavy rain, or a cyclone
 * alert is active (or while user previews animations). Completely invisible on dry normal days.
 */
@Composable
fun WeatherMiniAlertBanner(
    severity: WeatherSeverity,
    placeName: String,
    minutesUntilRain: Int?,
    tamil: Boolean,
    isPreview: Boolean = false,
    onClick: () -> Unit,
    onDismissPreview: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (severity == WeatherSeverity.CLEAR_OR_NORMAL) return

    val transition = rememberInfiniteTransition(label = "weather_mini_anim")
    val dropPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (severity == WeatherSeverity.RAIN_SOON) 1100 else 650,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rain_phase",
    )
    val spinDeg by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cyclone_spin",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "storm_pulse",
    )

    val primaryAccent = when (severity) {
        WeatherSeverity.CYCLONE_SEVERE -> Color(0xFFFF4D6D)
        WeatherSeverity.VERY_HEAVY_RAIN -> Color(0xFFFFB703)
        WeatherSeverity.RAIN_SOON -> Color(0xFF38BDF8)
        WeatherSeverity.CLEAR_OR_NORMAL -> Color(0xFF38BDF8)
    }
    val secondaryAccent = when (severity) {
        WeatherSeverity.CYCLONE_SEVERE -> Color(0xFF991B1B)
        WeatherSeverity.VERY_HEAVY_RAIN -> Color(0xFF0284C7)
        WeatherSeverity.RAIN_SOON -> Color(0xFF0EA5E9)
        WeatherSeverity.CLEAR_OR_NORMAL -> Color(0xFF0EA5E9)
    }

    val areaPrefix = placeName.trim().takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()
    val previewPrefix = if (isPreview) {
        pickLang(tamil, "[Preview] ", "[முன்னோட்டம்] ")
    } else {
        ""
    }
    val headline = previewPrefix + areaPrefix + when (severity) {
        WeatherSeverity.CYCLONE_SEVERE ->
            pickLang(tamil, "URGENT CYCLONE / STORM ALERT!", "அவசர புயல் / அதி கனமழை எச்சரிக்கை!")
        WeatherSeverity.VERY_HEAVY_RAIN ->
            pickLang(tamil, "Very heavy rain warning — stay safe", "மிகக் கனமழை எச்சரிக்கை — கவனம்")
        WeatherSeverity.RAIN_SOON -> {
            val mins = ((minutesUntilRain ?: 30).coerceIn(5, 60) + 2) / 5 * 5
            if ((minutesUntilRain ?: 30) <= 3) {
                pickLang(tamil, "Rain starting now", "இப்போது மழை தொடங்குகிறது")
            } else {
                pickLang(
                    tamil,
                    "Rain expected in ~$mins min",
                    "சுமார் $mins நிமிடங்களில் மழை வாய்ப்பு",
                )
            }
        }
        WeatherSeverity.CLEAR_OR_NORMAL -> ""
    }
    val subline = when (severity) {
        WeatherSeverity.CYCLONE_SEVERE ->
            pickLang(tamil, "24×7 emergency voice active · Tap for details", "24×7 அவசர எச்சரிக்கை · விவரத்திற்கு தட்டவும்")
        WeatherSeverity.VERY_HEAVY_RAIN ->
            pickLang(tamil, "Heavy downpour / squall · Tap for details", "பலத்த மழை / காற்று · விவரத்திற்கு தட்டவும்")
        WeatherSeverity.RAIN_SOON ->
            pickLang(tamil, "30-min rain watch · Tap for details", "30 நிமிட மழை கண்காணிப்பு · விவரத்திற்கு தட்டவும்")
        WeatherSeverity.CLEAR_OR_NORMAL -> ""
    }

    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0B1324),
                        secondaryAccent.copy(alpha = 0.24f),
                        Color(0xFF0B1324),
                    )
                )
            )
            .border(
                width = 1.dp,
                color = primaryAccent.copy(alpha = if (severity.bypassQuietHours) pulse else 0.55f),
                shape = shape,
            )
            .clickable { onClick() },
    ) {
        // Subtle floating background raindrops across the mini banner
        Canvas(modifier = Modifier.matchParentSize()) {
            val count = if (severity == WeatherSeverity.RAIN_SOON) 8 else 14
            for (i in 0 until count) {
                val xSeed = ((i * 73) % 100) / 100f
                val yOffset = ((dropPhase + i * 0.17f) % 1f)
                val startX = size.width * xSeed
                val startY = size.height * yOffset
                drawLine(
                    color = primaryAccent.copy(alpha = 0.20f),
                    start = Offset(startX, startY),
                    end = Offset(startX - 4.dp.toPx(), startY + 9.dp.toPx()),
                    strokeWidth = 1.3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Animated mini weather icon orb (34 dp)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(primaryAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension / 2f
                    when (severity) {
                        WeatherSeverity.CYCLONE_SEVERE -> {
                            // Spinning 4-arm tropical cyclone spiral + eye
                            rotate(degrees = spinDeg, pivot = Offset(cx, cy)) {
                                for (arm in 0 until 4) {
                                    val baseRad = Math.toRadians((arm * 90).toDouble())
                                    val path = Path()
                                    for (step in 0..12) {
                                        val t = step / 12f
                                        val rad = baseRad + t * 2.1
                                        val dist = r * (0.20f + 0.78f * t)
                                        val px = cx + (cos(rad) * dist).toFloat()
                                        val py = cy + (sin(rad) * dist).toFloat()
                                        if (step == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                    }
                                    drawPath(
                                        path = path,
                                        color = primaryAccent,
                                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                                    )
                                }
                            }
                            drawCircle(color = Color.White, radius = r * 0.16f, center = Offset(cx, cy))
                        }

                        WeatherSeverity.VERY_HEAVY_RAIN -> {
                            // Cloud + animated heavy rain streaks + flashing lightning bolt
                            drawCircle(
                                color = Color(0xFFE2E8F0),
                                radius = r * 0.42f,
                                center = Offset(cx - r * 0.18f, cy - r * 0.22f),
                            )
                            drawCircle(
                                color = Color(0xFFCBD5E1),
                                radius = r * 0.36f,
                                center = Offset(cx + r * 0.22f, cy - r * 0.18f),
                            )
                            for (d in -1..1) {
                                val dy = ((dropPhase + (d + 1) * 0.31f) % 1f) * r * 0.85f
                                drawLine(
                                    color = Color(0xFF38BDF8),
                                    start = Offset(cx + d * r * 0.42f, cy + dy * 0.5f),
                                    end = Offset(cx + d * r * 0.42f - 2.dp.toPx(), cy + dy * 0.5f + 5.dp.toPx()),
                                    strokeWidth = 1.6.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                            val bolt = Path().apply {
                                moveTo(cx + r * 0.08f, cy - r * 0.10f)
                                lineTo(cx - r * 0.16f, cy + r * 0.34f)
                                lineTo(cx + r * 0.06f, cy + r * 0.34f)
                                lineTo(cx - r * 0.08f, cy + r * 0.82f)
                            }
                            drawPath(
                                path = bolt,
                                color = primaryAccent.copy(alpha = pulse),
                                style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }

                        else -> {
                            // Cloud + smooth falling raindrops
                            drawCircle(
                                color = Color(0xFFE0F2FE),
                                radius = r * 0.42f,
                                center = Offset(cx - r * 0.16f, cy - r * 0.24f),
                            )
                            drawCircle(
                                color = Color(0xFFBAE6FD),
                                radius = r * 0.36f,
                                center = Offset(cx + r * 0.22f, cy - r * 0.18f),
                            )
                            for (d in -1..1) {
                                val dy = ((dropPhase + (d + 1) * 0.33f) % 1f) * r * 0.85f
                                drawLine(
                                    color = primaryAccent,
                                    start = Offset(cx + d * r * 0.40f, cy + dy * 0.55f),
                                    end = Offset(cx + d * r * 0.40f - 2.dp.toPx(), cy + dy * 0.55f + 5.dp.toPx()),
                                    strokeWidth = 1.8.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subline,
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isPreview && onDismissPreview != null) {
                IconButton(
                    onClick = onDismissPreview,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = primaryAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
