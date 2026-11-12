package com.universalrp.cleansweep.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import kotlin.math.*

/** Small, user-opened native tile viewport; no JavaScript, key-in-URL, hidden geolocation or prefetch. */
object WeatherMapData {
    const val ZOOM = 7
    fun tamilNadu(lat: Double, lon: Double) = lat.isFinite() && lon.isFinite() && lat in 8.0..13.6 && lon in 76.0..80.6
    fun tile(lat: Double, lon: Double): Pair<Double, Double> {
        require(SavedRainWatches.validCoordinates(lat,lon))
        val n = (1 shl ZOOM).toDouble(); val r = Math.toRadians(lat)
        return (lon + 180) / 360 * n to (1 - ln(tan(r) + 1/cos(r)) / PI) / 2 * n
    }
    fun coordinates(x: Double, y: Double): Pair<Double, Double> {
        val n = (1 shl ZOOM).toDouble()
        return Math.toDegrees(atan(sinh(PI * (1 - 2*y/n)))) to (x/n*360 - 180)
    }
    fun url(x: Int, y: Int, weather: Boolean, time: String): String {
        require(x in 0 until (1 shl ZOOM) && y in 0 until (1 shl ZOOM))
        require(time == "now" || runCatching { java.time.Instant.parse(time) }.isSuccess)
        return if (weather) "https://api.tomorrow.io/v4/map/tile/$ZOOM/$x/$y/precipitationIntensity/$time.png"
            else "https://tile.openstreetmap.org/$ZOOM/$x/$y.png"
    }
    fun fetch(context: Context, x: Int, y: Int, weather: Boolean, time: String, key: String): Bitmap {
        require(!weather || key.isNotBlank())
        val cache = File(context.cacheDir,"small-weather-map").apply { mkdirs() }
        val file = File(cache,"$ZOOM-$x-$y.png")
        val bytes = if (!weather && file.isFile && System.currentTimeMillis()-file.lastModified() in 0..7L*24*60*60_000) file.readBytes()
        else {
            val conn=(URL(url(x,y,weather,time)).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects=false; connectTimeout=8_000; readTimeout=8_000
                setRequestProperty("User-Agent","LiveGuard/2.83 (+https://github.com/universalrp2003/RP-Android-Apps)")
                if (weather) setRequestProperty("apikey",key)
            }
            try {
                check(conn.responseCode==200) { "Map tile unavailable" }
                check(conn.contentType?.substringBefore(';') == "image/png") { "Not a map tile" }
                val out=java.io.ByteArrayOutputStream(); val block=ByteArray(8192)
                conn.inputStream.use { input -> while (true) { val count=input.read(block); if(count<0) break
                    check(out.size()+count<=512_000); out.write(block,0,count) } }
                out.toByteArray().also { data ->
                    if(!weather) {
                        // Bound normal view cache; never fetch outside this explicit viewport.
                        if((cache.listFiles()?.size ?: 0)>100) cache.listFiles()?.minByOrNull { it.lastModified() }?.delete()
                        file.writeBytes(data)
                    }
                }
            } finally { conn.disconnect() }
        }
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        check(bounds.outWidth==256 && bounds.outHeight==256)
        return checkNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size))
    }
}
