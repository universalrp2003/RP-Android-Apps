package com.oorarivippu.voiceannouncer.family

import android.content.Context
import android.location.Location
import android.os.SystemClock
import com.oorarivippu.voiceannouncer.AppPreferences
import com.oorarivippu.voiceannouncer.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** No maps, contacts, coordinates, addresses, routes or family names are sent to this service. */
object FamilyRepository {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val lock=Mutex()
    private val http=OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(6,TimeUnit.SECONDS).callTimeout(8,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    val configured: Boolean get()=BuildConfig.FAMILY_SERVICE_URL.matches(Regex("https://oor-family-relay\\.[a-z0-9-]+\\.workers\\.dev"))
    private fun api(row: JSONObject,method: String,action: String,body: JSONObject?=null,token: String=row.getString("auth")): JSONObject {
        check(configured)
        val request=Request.Builder().url(BuildConfig.FAMILY_SERVICE_URL+"/v1/pairs/"+row.getString("id")+"/"+action)
            .header("Authorization","Bearer $token").header("Cache-Control","no-store")
            .method(method,if(method in listOf("POST","PUT")) (body?:JSONObject()).toString().toRequestBody("application/json".toMediaType()) else null).build()
        return http.newCall(request).execute().use { response ->
            if(!response.isSuccessful) throw FamilyFailure(response.code)
            val input=response.body?.byteStream()?:error("Empty response")
            val bytes=input.readBytesBounded(4096);JSONObject(String(bytes,Charsets.UTF_8))
        }
    }
    private fun java.io.InputStream.readBytesBounded(max: Int): ByteArray {
        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(1024)
        while(true) { val n=read(buffer);if(n<0)break;require(out.size()+n<=max);out.write(buffer,0,n) };return out.toByteArray()
    }
    class FamilyFailure(val code: Int): Exception("Family service request failed")
    private fun update(c: Context,id: String, f: (JSONObject)->Unit)=FamilyStore.change(c){ rows->rows.firstOrNull { it.optString("id")==id }?.let(f) }
    suspend fun create(c: Context,name: String): String = withContext(Dispatchers.IO) { lock.withLock {
        require(FamilyStore.read(c).size<FamilyProtocol.MAX_MEMBERS)
        val row=JSONObject().put("id",FamilyProtocol.random(16)).put("role","a").put("auth",FamilyProtocol.random()).put("key",FamilyProtocol.random()).put("invite",FamilyProtocol.random()).put("name",FamilyProtocol.clean(name)).put("until",0).put("pending","")
        api(row,"POST","create",JSONObject().put("inviteHash",FamilyProtocol.authHash(row.getString("invite"))))
        FamilyStore.change(c){ it.add(row) }
        FamilyProtocol.Invite(row.getString("id"),row.getString("invite"),row.getString("key")).text()
    } }
    suspend fun join(c: Context,name: String,code: String) = withContext(Dispatchers.IO) { lock.withLock {
        val invite=FamilyProtocol.invite(code)
        require(FamilyStore.read(c).size<FamilyProtocol.MAX_MEMBERS && FamilyStore.read(c).none { it.optString("id")==invite.id })
        val row=JSONObject().put("id",invite.id).put("role","b").put("auth",FamilyProtocol.random()).put("key",invite.key).put("name",FamilyProtocol.clean(name)).put("until",0).put("pending","")
        api(row,"POST","claim",JSONObject().put("guestHash",FamilyProtocol.authHash(row.getString("auth"))).put("confirmation",FamilyProtocol.random(16)),invite.token)
        FamilyStore.change(c){ it.add(row) }
    } }
    data class View(val id: String,val name: String,val paired: Boolean,val confirmed: Boolean,val myConfirmed: Boolean,val code: String,val town: FamilyProtocol.Town?,val sharing: Boolean,val pending: Boolean,val unavailable: Boolean)
    suspend fun views(c: Context): List<View> = withContext(Dispatchers.IO) { lock.withLock {
        flush(c)
        FamilyStore.read(c).map { row ->
            var status: JSONObject?=null
            try { status=api(row,"GET","status") } catch(_:Exception) { /* Never expose a raw error/URL/token. */ }
            val role=row.getString("role");val id=row.getString("id");val other=if(role=="a")"b" else "a"
            val confirmation=status?.optString("confirmation").orEmpty()
            if(confirmation.isNotEmpty()) update(c,id){it.put("confirmation",confirmation)}
            val approved=status?.optJSONObject("confirmed")?.let{it.optBoolean("a")&&it.optBoolean("b")}==true
            val now=System.currentTimeMillis()
            val town=if(approved && row.optString("pending").isEmpty()) runCatching { status!!.optJSONObject("update")?.let { FamilyProtocol.decrypt(id,other,row.getString("key"),it,status!!.getLong("now"),now) } }.getOrNull() else null
            View(id,row.getString("name"),status?.optBoolean("paired")==true,approved,status?.optJSONObject("confirmed")?.optBoolean(role)==true,
                if(confirmation.isEmpty()) "" else FamilyProtocol.verification(id,row.getString("key"),confirmation),town,row.optLong("until")>now && row.optString("pending").isEmpty(),row.optString("pending").isNotEmpty(),status==null)
        }
    } }
    suspend fun confirm(c: Context,id: String) = withContext(Dispatchers.IO) { lock.withLock {
        val row=FamilyStore.read(c).first { it.getString("id")==id };check(row.optString("pending").isEmpty())
        api(row,"POST","confirm",JSONObject().put("confirmation",row.getString("confirmation")))
    } }
    suspend fun start(c: Context,id: String,hours: Int) = withContext(Dispatchers.IO) { lock.withLock {
        require(hours in listOf(1,4,8) && AppPreferences.state.value.isTrackingEnabled)
        val row=FamilyStore.read(c).first { it.getString("id")==id };check(row.optString("pending").isEmpty())
        val until=System.currentTimeMillis()+hours*3600_000L
        val result=api(row,"PUT","consent",JSONObject().put("until",until))
        update(c,id){it.put("until",result.getLong("until")).put("generation",result.getInt("generation")).put("lastSent",0)}
    } }
    fun stop(c: Context,id: String,remove: Boolean=false) {
        val app=c.applicationContext
        // Durable local OFF precedes every network request, including when offline.
        update(app,id){it.put("until",0).put("pending",if(remove) "remove" else "stop")}
        scope.launch { lock.withLock { flush(app) } }
    }
    fun stopAll(c: Context) { runCatching { FamilyStore.read(c).forEach { stop(c,it.getString("id")) } } }
    private fun flush(c: Context) {
        for(row in FamilyStore.read(c)) {
            val pending=row.optString("pending");if(pending.isEmpty()) continue
            try {
                if(pending=="remove") api(row,"DELETE","pair") else api(row,"PUT","consent",JSONObject().put("until",0))
            } catch(e:FamilyFailure) { if(e.code !in listOf(403,404,410)) continue } catch(_:Exception) { continue }
            FamilyStore.change(c){ rows->if(pending=="remove") rows.removeAll {it.getString("id")==row.getString("id")} else rows.firstOrNull{it.getString("id")==row.getString("id")}?.put("pending","") }
        }
    }
    suspend fun publish(c: Context,town: String,district: String,fix: Location) = withContext(Dispatchers.IO) { lock.withLock {
        flush(c)
        if(!AppPreferences.state.value.isTrackingEnabled || !configured || !fix.hasAccuracy() || !FamilyProtocol.freshFix(System.currentTimeMillis()-fix.time,(SystemClock.elapsedRealtimeNanos()-fix.elapsedRealtimeNanos)/1_000_000,fix.accuracy)) return@withLock
        val label=runCatching { FamilyProtocol.clean(town) }.getOrNull()?:return@withLock
        val region=runCatching { if(district.isBlank()) "" else FamilyProtocol.clean(district) }.getOrNull()?:return@withLock
        val now=System.currentTimeMillis()
        for(row in FamilyStore.read(c)) {
            if(row.optLong("until")<=now || row.optString("pending").isNotEmpty() || now-row.optLong("lastSent") !in FamilyProtocol.HEARTBEAT..Long.MAX_VALUE) continue
            if(!AppPreferences.state.value.isTrackingEnabled) break
            runCatching {
                api(row,"PUT","town",FamilyProtocol.encrypt(row.getString("id"),row.getString("role"),row.getString("key"),FamilyProtocol.Town(label,region,fix.time),row.getInt("generation")))
                update(c,row.getString("id")){it.put("lastSent",now)}
            }
        }
    } }
}
