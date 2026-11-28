package com.universalrp.tamilnadufm.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.universalrp.tamilnadufm.data.RadioStation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    color: Color = SurfaceC,
    content: @Composable () -> Unit,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = color) {
        content()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) Color.White else TextPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Saffron else SurfaceHigh)
            .border(1.dp, if (selected) Saffron else OutlineC, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun IconChip(
    icon: ImageVector,
    label: String,
    tint: Color = Saffron,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceHigh)
            .border(1.dp, OutlineC, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun Pill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Bounded artwork downloads: station-provided logos are optional and can be unavailable. */
private object LogoCache {
    private const val MAX_IMAGE_BYTES = 2 * 1024 * 1024
    private const val TARGET_PIXELS = 512
    private val loads = Semaphore(4)
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    suspend fun load(url: String): Bitmap? {
        cache.get(url)?.let { return it }
        if (!url.startsWith("https://", true) && !url.startsWith("http://", true)) return null
        return withContext(Dispatchers.IO) {
            loads.withPermit {
                cache.get(url)?.let { return@withPermit it }
                try {
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                        setRequestProperty("User-Agent", "RameshRadio/1.1")
                    }
                    try {
                        if (connection.contentLengthLong > MAX_IMAGE_BYTES) return@withPermit null
                        val bytes = connection.inputStream.use { stream ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                coroutineContext.ensureActive()
                                val count = stream.read(buffer)
                                if (count < 0) break
                                if (output.size() + count > MAX_IMAGE_BYTES) return@withPermit null
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                            bounds.outWidth > 16384 || bounds.outHeight > 16384) return@withPermit null
                        var sample = 1
                        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > TARGET_PIXELS) sample *= 2
                        val options = BitmapFactory.Options().apply { inSampleSize = sample }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also { cache.put(url, it) }
                    } finally { connection.disconnect() }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            }
        }
    }
}

private val MONOGRAM_COLORS = listOf(
    Color(0xFF356B21), Color(0xFF72570B), Color(0xFF006D68),
    Color(0xFF5B4E88), Color(0xFF9A3F5C), Color(0xFF275F8A),
)

/** Large, uncropped logos for the station gallery; missing or failed logos get a monogram. */
@Composable
fun StationArtwork(station: RadioStation, modifier: Modifier = Modifier, decorative: Boolean = false, crop: Boolean = false) {
    val bitmap by produceState<Bitmap?>(null, station.favicon) {
        value = null // Never show the previous station while a new URL is loading.
        value = LogoCache.load(station.favicon)
    }
    val color = MONOGRAM_COLORS[Math.floorMod(station.name.hashCode(), MONOGRAM_COLORS.size)]
    Box(modifier.background(if (bitmap != null) Color.White else SurfaceHigh),
        contentAlignment = Alignment.Center) {
        val picture = bitmap
        if (picture != null) {
            Image(bitmap = picture.asImageBitmap(), contentDescription = if (decorative) null else station.name,
                contentScale = if (crop) ContentScale.Crop else ContentScale.Fit,
                alignment = if (crop) Alignment.TopCenter else Alignment.Center, modifier = Modifier.fillMaxSize())
        } else {
            Text(station.monogram, style = MaterialTheme.typography.headlineSmall,
                color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StationLogo(station: RadioStation, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    StationArtwork(station, modifier.size(size).clip(RoundedCornerShape(size / 4)))
}
