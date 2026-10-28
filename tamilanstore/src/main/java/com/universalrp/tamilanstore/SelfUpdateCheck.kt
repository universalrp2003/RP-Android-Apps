package com.universalrp.tamilanstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Read-only public release discovery, not an automatic APK installer or a remotely trusted catalog. */
object SelfUpdateCheck {
    const val RELEASES = "https://github.com/universalrp2003/RP-Android-Apps/releases?q=tamilan-store"
    suspend fun latestRelease(): Pair<String,String>? = withContext(Dispatchers.IO) {
        val connection=URL("https://api.github.com/repos/universalrp2003/RP-Android-Apps/releases?per_page=100").openConnection() as HttpURLConnection
        connection.connectTimeout=15_000;connection.readTimeout=15_000;connection.instanceFollowRedirects=false
        connection.setRequestProperty("Accept","application/vnd.github+json")
        try {
            check(connection.responseCode==200)
            val bytes=connection.inputStream.use { it.readBytesLimited(2_000_000) }
            val releases=JSONArray(String(bytes,Charsets.UTF_8))
            val versions=mutableListOf<Pair<String,String>>()
            for(i in 0 until releases.length()) {
                val r=releases.getJSONObject(i);val tag=r.getString("tag_name")
                if(r.getBoolean("draft")||r.getBoolean("prerelease")||!tag.matches(Regex("tamilan-store-v[0-9]+(\\.[0-9]+){1,3}"))) continue
                val version=tag.removePrefix("tamilan-store-v")
                val url="https://github.com/universalrp2003/RP-Android-Apps/releases/tag/$tag"
                val assets=r.getJSONArray("assets")
                if((0 until assets.length()).any { j -> assets.getJSONObject(j).getString("name")=="Tamilan-Store-v$version.apk" }) versions.add(version to url)
            }
            versions.maxWithOrNull { a,b ->
                val av=a.first.split('.').map { it.toLong() };val bv=b.first.split('.').map { it.toLong() }
                var result=0
                for(i in 0 until maxOf(av.size,bv.size)) { result=av.getOrElse(i){0}.compareTo(bv.getOrElse(i){0});if(result!=0)break }
                result
            }
        } finally { connection.disconnect() }
    }
    private fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) { val n=read(buffer);if(n<0)break;check(out.size()+n<=max);out.write(buffer,0,n) }
        return out.toByteArray()
    }
}
