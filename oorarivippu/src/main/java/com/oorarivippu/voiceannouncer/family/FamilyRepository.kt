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
    private val mutableState=kotlinx.coroutines.flow.MutableStateFlow<List<View>>(emptyList())
    val state: kotlinx.coroutines.flow.StateFlow<List<View>> = mutableState
    @Volatile var homeVisible=false
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
        val row=JSONObject().put("id",FamilyProtocol.random(16)).put("role","a").put("auth",FamilyProtocol.random()).put("key",FamilyProtocol.random()).put("invite",FamilyProtocol.random()).put("name",FamilyProtocol.clean(name)).put("until",0).put("enabled",false).put("pending","")
        row.put("pending","create")
        FamilyStore.change(c){ it.add(row) } // Persist capabilities BEFORE HTTP: a lost reply can be retried safely.
        api(row,"POST","create",JSONObject().put("inviteHash",FamilyProtocol.authHash(row.getString("invite"))))
        update(c,row.getString("id")){if(it.optString("pending")=="create")it.put("pending","")}
        FamilyProtocol.Invite(row.getString("id"),row.getString("invite"),row.getString("key")).text()
    } }
    suspend fun join(c: Context,name: String,code: String) = withContext(Dispatchers.IO) { lock.withLock {
        val invite=FamilyProtocol.invite(code)
        require(FamilyStore.read(c).size<FamilyProtocol.MAX_MEMBERS && FamilyStore.read(c).none { it.optString("id")==invite.id })
        val row=JSONObject().put("id",invite.id).put("role","b").put("auth",FamilyProtocol.random()).put("key",invite.key).put("name",FamilyProtocol.clean(name)).put("until",0).put("enabled",false).put("pending","")
        row.put("pending","claim").put("invite",invite.token).put("confirmation",FamilyProtocol.random(16))
        FamilyStore.change(c){ it.add(row) }
        api(row,"POST","claim",JSONObject().put("guestHash",FamilyProtocol.authHash(row.getString("auth"))).put("confirmation",row.getString("confirmation")),invite.token)
        update(c,invite.id){if(it.optString("pending")=="claim"){it.put("pending","");it.remove("invite")}}
    } }
    data class View(val id: String,val name: String,val paired: Boolean,val confirmed: Boolean,val myConfirmed: Boolean,val code: String,val town: FamilyProtocol.Town?,val sharing: Boolean,val pending: Boolean,val unavailable: Boolean,val setupPending: Boolean,val invitation: String,val uploading: Boolean=false,val reportExpiresAt: Long=0)
    private fun cachedTown(row: JSONObject,now: Long): FamilyProtocol.Town? = runCatching {
        row.optJSONObject("latestTown")?.let { if(FamilySharingPolicy.fresh(it.getLong("at"),now) && now<it.getLong("expires")) FamilyProtocol.Town(FamilyProtocol.clean(it.getString("town")),it.getString("district"),it.getLong("at")) else null }
    }.getOrNull()
    /** Cache is encrypted alongside the pairing keys, latest report only. Never a travel history. */
    fun cached(c: Context): List<View> {
        val now=System.currentTimeMillis()
        val rows=FamilyStore.read(c).map { row ->
            val pending=row.optString("pending");val approved=row.optBoolean("approved")
            View(row.getString("id"),row.getString("name"),row.optBoolean("paired"),approved,row.optBoolean("myConfirmed"),
                row.optString("confirmation").takeIf{it.isNotEmpty()}?.let {FamilyProtocol.verification(row.getString("id"),row.getString("key"),it)}.orEmpty(),
                if(approved && pending!="remove")cachedTown(row,now)else null,row.optBoolean("enabled"),pending.isNotEmpty(),row.optBoolean("unavailable",true),
                pending in listOf("create","claim"),
                if(row.getString("role")=="a" && !row.optBoolean("paired") && row.optString("invite").isNotEmpty()) FamilyProtocol.Invite(row.getString("id"),row.getString("invite"),row.getString("key")).text() else "",
                row.optBoolean("enabled") && row.optLong("until")>now && pending.isEmpty() && AppPreferences.state.value.isTrackingEnabled,row.optJSONObject("latestTown")?.optLong("expires")?:0)
        }
        mutableState.value=rows
        return rows
    }
    private fun migrate(c: Context) {
        if(FamilyStore.read(c).any{!it.has("enabled")}) FamilyStore.change(c){rows->rows.filter{!it.has("enabled")}.forEach{
            // A former timed grant is NOT permission to opt into indefinite future trips.
            it.put("enabled",false).put("until",0).put("revision",it.optLong("revision")+1)
            if(it.optString("pending").isEmpty())it.put("pending","stop")
        }}
    }
    suspend fun views(c: Context): List<View> = withContext(Dispatchers.IO) { lock.withLock {
        migrate(c);flush(c)
        for(row in FamilyStore.read(c)) {
            val id=row.getString("id");val role=row.getString("role");val other=if(role=="a")"b" else "a"
            try {
                val status=api(row,"GET","status")
                val approved=status.optJSONObject("confirmed")?.let{it.optBoolean("a")&&it.optBoolean("b")}==true
                val now=System.currentTimeMillis()
                val town=if(approved)runCatching {status.optJSONObject("update")?.let {FamilyProtocol.decrypt(id,other,row.getString("key"),it,status.getLong("now"),now)}}.getOrNull()else null
                update(c,id){ latest ->
                    latest.put("unavailable",false).put("approved",approved).put("paired",status.optBoolean("paired"))
                        .put("myConfirmed",status.optJSONObject("confirmed")?.optBoolean(role)==true)
                    val confirmation=status.optString("confirmation");if(confirmation.isNotEmpty())latest.put("confirmation",confirmation)
                    if(town!=null && latest.optString("pending")!="remove") latest.put("latestTown",JSONObject().put("town",town.town).put("district",town.district).put("at",town.at).put("expires",status.getJSONObject("update").getLong("expires")))
                    else latest.remove("latestTown")
                }
            } catch(cancelled: CancellationException) {throw cancelled}
            catch(e:Exception) {
                update(c,id){ latest ->
                    latest.put("unavailable",true)
                    if(cachedTown(latest,System.currentTimeMillis())==null)latest.remove("latestTown")
                    if(e is FamilyFailure && e.code in listOf(403,404,410)) {
                        latest.put("approved",false).put("enabled",false).put("until",0).put("revision",latest.optLong("revision")+1);latest.remove("latestTown")
                    }
                }
            }
        }
        renewLeases(c)
        cached(c)
    } }
    suspend fun confirm(c: Context,id: String) = withContext(Dispatchers.IO) { lock.withLock {
        val row=FamilyStore.read(c).first { it.getString("id")==id };check(row.optString("pending").isEmpty())
        api(row,"POST","confirm",JSONObject().put("confirmation",row.getString("confirmation")))
    } }
    suspend fun setSharing(c: Context,id: String,enabled: Boolean) {
        if(!enabled) {stop(c,id);return}
        withContext(Dispatchers.IO) { lock.withLock {
            val row=FamilyStore.read(c).first{it.getString("id")==id}
            check(configured && row.optBoolean("approved") && row.optString("pending").isEmpty())
            update(c,id){it.put("enabled",true).put("revision",it.optLong("revision")+1)}
            cached(c)
            renewLeases(c)
            cached(c)
        } }
    }
    private fun renewLeases(c: Context) {
        for(row in FamilyStore.read(c)) {
            val now=System.currentTimeMillis()
            if(!FamilySharingPolicy.renew(row.optBoolean("enabled"),row.optBoolean("approved"),AppPreferences.state.value.isTrackingEnabled,row.optString("pending").isNotEmpty(),row.optLong("until"),now))continue
            val revision=row.optLong("revision")
            try {
                val result=api(row,"PUT","consent",JSONObject().put("until",now+FamilySharingPolicy.LEASE))
                update(c,row.getString("id")){
                    if(FamilySharingPolicy.acceptLease(it.optBoolean("enabled"),revision,it.optLong("revision"),it.optString("pending").isNotEmpty(),AppPreferences.state.value.isTrackingEnabled))
                        it.put("until",result.getLong("until")).put("generation",result.getInt("generation")).put("lastSent",0)
                }
            } catch(_:Exception) { /* Intent remains visible ON, but no active lease means no uploads. */ }
        }
    }
    fun stop(c: Context,id: String,remove: Boolean=false) {
        val app=c.applicationContext
        // Durable local OFF precedes every network request, including when offline.
        update(app,id){it.put("until",0).put("enabled",false).put("revision",it.optLong("revision")+1).put("pending",if(remove || it.optString("pending")=="remove") "remove" else "stop");if(remove)it.remove("latestTown")}
        cached(app)
        scope.launch { runCatching{lock.withLock { flush(app);cached(app) }} }
    }
    fun stopAll(c: Context) { FamilyStore.read(c).forEach { stop(c,it.getString("id")) } }
    /** Stopping a trip pauses its lease, NOT the user's remembered sharing switch. */
    fun pauseTrip(c: Context) {
        val app=c.applicationContext
        if(FamilyStore.read(app).isEmpty())return
        FamilyStore.change(app){rows->rows.forEach{
            it.put("until",0).put("revision",it.optLong("revision")+1)
            if(it.optString("pending") !in listOf("remove","create","claim"))it.put("pending","stop")
        }}
        cached(app)
        scope.launch{runCatching{lock.withLock{flush(app);cached(app)}}}
    }
    /** At-most-once attempt, even across refresh/restart. No history or server speech ledger. */
    fun claimSpeech(c: Context,id: String,at: Long): Boolean {
        var claimed=false
        FamilyStore.change(c){rows->rows.firstOrNull{it.getString("id")==id}?.let{
            if(it.optString("pending")!="remove" && cachedTown(it,System.currentTimeMillis())?.at==at && FamilySharingPolicy.announce(it.optBoolean("approved"),at,it.optLong("announcedAt"),System.currentTimeMillis())) {
                it.put("announcedAt",at);claimed=true
            }
        }}
        return claimed
    }
    private fun flush(c: Context) {
        for(row in FamilyStore.read(c)) {
            val pending=row.optString("pending");if(pending.isEmpty()) continue
            try {
                when(pending) {
                    "create" -> api(row,"POST","create",JSONObject().put("inviteHash",FamilyProtocol.authHash(row.getString("invite"))))
                    "claim" -> api(row,"POST","claim",JSONObject().put("guestHash",FamilyProtocol.authHash(row.getString("auth"))).put("confirmation",row.getString("confirmation")),row.getString("invite"))
                    "remove" -> api(row,"DELETE","pair")
                    "stop" -> api(row,"PUT","consent",JSONObject().put("until",0))
                    else -> continue
                }
            } catch(e:FamilyFailure) {
                if(pending !in listOf("remove","stop") || e.code !in listOf(403,404,410)) continue
            } catch(_:Exception) { continue }
            FamilyStore.change(c){ rows->
                if(pending=="remove") rows.removeAll {it.getString("id")==row.getString("id")}
                else rows.firstOrNull{it.getString("id")==row.getString("id")}?.let{
                    // Do not overwrite a newer local stop/remove while a setup response was in flight.
                    if(it.optString("pending")==pending) { it.put("pending","");if(pending=="claim")it.remove("invite") }
                }
            }
        }
    }
    suspend fun publish(c: Context,town: String,district: String,fix: Location) = withContext(Dispatchers.IO) { lock.withLock {
        migrate(c);flush(c);renewLeases(c)
        if(!AppPreferences.state.value.isTrackingEnabled || !configured || !fix.hasAccuracy() || !FamilyProtocol.freshFix(System.currentTimeMillis()-fix.time,(SystemClock.elapsedRealtimeNanos()-fix.elapsedRealtimeNanos)/1_000_000,fix.accuracy)) return@withLock
        val label=runCatching { FamilyProtocol.clean(town) }.getOrNull()?:return@withLock
        val region=runCatching { if(district.isBlank()) "" else FamilyProtocol.clean(district) }.getOrNull()?:return@withLock
        val now=System.currentTimeMillis()
        for(row in FamilyStore.read(c)) {
            if(!row.optBoolean("enabled") || row.optLong("until")<=now || row.optString("pending").isNotEmpty() || now-row.optLong("lastSent") !in FamilyProtocol.HEARTBEAT..Long.MAX_VALUE) continue
            if(!AppPreferences.state.value.isTrackingEnabled) break
            runCatching {
                api(row,"PUT","town",FamilyProtocol.encrypt(row.getString("id"),row.getString("role"),row.getString("key"),FamilyProtocol.Town(label,region,fix.time),row.getInt("generation")))
                update(c,row.getString("id")){it.put("lastSent",now)}
            }
        }
    } }
}
