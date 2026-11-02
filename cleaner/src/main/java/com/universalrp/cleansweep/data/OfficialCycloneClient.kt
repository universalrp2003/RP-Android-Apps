package com.universalrp.cleansweep.data

import android.content.*
import android.os.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Bounded, keyless public products, queried only during an existing disaster-news preparation.
 * No user coordinates, app inventory, keys or other private fields are sent to these hosts.
 */
object OfficialCycloneClient {
    private val client by lazy { OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(2,TimeUnit.SECONDS).readTimeout(2,TimeUnit.SECONDS).callTimeout(3,TimeUnit.SECONDS).build() }
    private fun get(url: String, deadline: Long, max: Int): ByteArray {
        require(OfficialCyclonePolicy.allowed(url))
        val left = deadline-SystemClock.elapsedRealtime(); require(left > 0)
        val call = client.newCall(Request.Builder().url(url).header("User-Agent","RP-LiveGuard-OfficialCyclone/1.0").build())
        call.timeout().timeout(minOf(left,3000),TimeUnit.MILLISECONDS)
        return call.execute().use { response ->
            require(response.isSuccessful)
            val body = response.body ?: error("No body")
            require(body.contentLength() <= max)
            body.byteStream().use { input ->
                val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while(out.size() <= max) {
                    val n = input.read(buffer,0,minOf(buffer.size,max+1-out.size()))
                    if(n<0) break
                    out.write(buffer,0,n)
                }
                require(out.size() <= max); out.toByteArray()
            }
        }
    }
    internal suspend fun pdfText(c: Context, bytes: ByteArray, timeoutMillis: Long = 3500): String? {
        if (timeoutMillis <= 0 || Build.VERSION.SDK_INT < 35 || bytes.size > 2_000_000 || !bytes.take(5).toByteArray().contentEquals("%PDF-".toByteArray())) return null
        val file = File.createTempFile("imd-", ".pdf", c.cacheDir)
        return try {
            file.writeBytes(bytes)
            withContext(Dispatchers.Main) {
                withTimeoutOrNull(minOf(3500, timeoutMillis)) {
                    suspendCancellableCoroutine<String?> { continuation ->
                        var descriptor: ParcelFileDescriptor? = null
                        val connection = object : ServiceConnection {
                            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                                if (!continuation.isActive) return
                                runCatching {
                                    descriptor = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
                                    val reply = Messenger(Handler(Looper.getMainLooper()) { result ->
                                        if (continuation.isActive) continuation.resume(result.data.getString("text")?.takeIf { it.isNotBlank() && it.length <= 60_000 })
                                        true
                                    })
                                    Messenger(service).send(Message.obtain().apply { replyTo=reply; data=Bundle().apply { putParcelable("pdf",descriptor) } })
                                }.onFailure { if(continuation.isActive) continuation.resume(null) }
                            }
                            override fun onServiceDisconnected(name: ComponentName) { if(continuation.isActive) continuation.resume(null) }
                            override fun onNullBinding(name: ComponentName) { if(continuation.isActive) continuation.resume(null) }
                            override fun onBindingDied(name: ComponentName) { if(continuation.isActive) continuation.resume(null) }
                        }
                        val bound = runCatching { c.bindService(Intent(c,ImdPdfTextService::class.java),connection,Context.BIND_AUTO_CREATE) }.getOrDefault(false)
                        if(!bound && continuation.isActive) continuation.resume(null)
                        continuation.invokeOnCancellation {
                            Handler(Looper.getMainLooper()).post { if(bound) runCatching { c.unbindService(connection) }; runCatching { descriptor?.close() } }
                        }
                        // A completion handler, not only cancellation: release the isolated process/FD on success too.
                        continuation.context[Job]?.invokeOnCompletion {
                            Handler(Looper.getMainLooper()).post { if(bound) runCatching { c.unbindService(connection) }; runCatching { descriptor?.close() } }
                        }
                    }
                }
            }
        } finally { file.delete() }
    }
    suspend fun fetch(c: Context, now: Long, deadline: Long): List<OfficialCycloneBulletin> = withContext(Dispatchers.IO) {
        val found = mutableListOf<OfficialCycloneBulletin>()
        // Primary authority for India. Native isolated PDF text extraction on Android15+;
        // older Android uses the separately attributed public JTWC text, not an old PDF dependency.
        if(Build.VERSION.SDK_INT >= 35) try {
            val html = get("https://rsmcnewdelhi.imd.gov.in/",deadline,400_000).toString(Charsets.UTF_8)
            for(url in OfficialCyclonePolicy.imdLinks(html)) {
                currentCoroutineContext().ensureActive()
                val text = pdfText(c,get(url,deadline,2_000_000),deadline-SystemClock.elapsedRealtime()) ?: continue
                OfficialCyclonePolicy.imd(text,url,now)?.let { found.add(it) }
            }
        } catch(cancelled: CancellationException) { throw cancelled } catch(_: Exception) { }
        try {
            val html = get("https://www.metoc.navy.mil/jtwc/jtwc.html",deadline,400_000).toString(Charsets.UTF_8)
            for(url in OfficialCyclonePolicy.jtwcLinks(html)) {
                currentCoroutineContext().ensureActive()
                OfficialCyclonePolicy.jtwc(get(url,deadline,80_000).toString(Charsets.UTF_8),url,now)?.let { found.add(it) }
            }
        } catch(cancelled: CancellationException) { throw cancelled } catch(_: Exception) { }
        found
    }
    fun enrich(event: NewsDisasterPolicy.Event, candidates: List<OfficialCycloneBulletin>, origin: NewsDisasterPolicy.Origin, now: Long): NewsDisasterPolicy.Event {
        val matched = candidates.filter { it.fresh(now) && it.matches(event) &&
            (event.positionAt == null || it.observedAt >= event.positionAt) }
        val best = matched.filter { it.authority=="IMD" }.maxByOrNull { it.observedAt }
            ?: matched.filter { it.authority=="JTWC" }.maxByOrNull { it.observedAt } ?: return event
        val distance = com.universalrp.weather.WeatherForecastClient.haversineKm(origin.lat,origin.lon,best.lat,best.lon)
        return event.copy(positionAt=best.observedAt,windKmh=best.windHigh.toDouble(),distanceKm=distance,official=best)
    }
}
