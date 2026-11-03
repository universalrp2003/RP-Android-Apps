package com.oorarivippu.voiceannouncer.family

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/** Versioned standard AES-256-GCM envelopes; HTTPS capabilities authorize each direction separately. */
object FamilyProtocol {
    const val TTL = 15 * 60_000L
    const val HEARTBEAT = 5 * 60_000L
    const val MAX_MEMBERS = 6
    private val hex32 = Regex("[a-f0-9]{32}")
    private val hex64 = Regex("[a-f0-9]{64}")
    private val label = Regex("[\\p{L}\\p{M} .’'()\\-]{1,80}")
    fun random(bytes: Int = 32): String = ByteArray(bytes).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun bytes(hex: String): ByteArray { require(hex64.matches(hex));return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray() }
    fun authHash(secret: String): String { require(hex64.matches(secret));return sha("oor-family-auth-v1:$secret").joinToString("") { "%02x".format(it.toInt() and 255) } }
    private fun sha(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
    data class Invite(val id: String, val token: String, val key: String) {
        fun text() = "oorfamily:v1:$id:$token:$key"
    }
    fun invite(raw: String): Invite {
        require(raw.length <= 200)
        val p=raw.trim().split(':');require(p.size==5 && p[0]=="oorfamily" && p[1]=="v1" && hex32.matches(p[2]) && hex64.matches(p[3]) && hex64.matches(p[4]))
        return Invite(p[2],p[3],p[4])
    }
    fun verification(id: String,key: String,confirmation: String): String {
        require(hex32.matches(id) && hex64.matches(key) && hex32.matches(confirmation))
        return sha("oor-family-verify/v1|$id|$key|$confirmation").take(5).joinToString("") { "%02x".format(it.toInt() and 255) }.chunked(5).joinToString(" ")
    }
    fun clean(raw: String): String = raw.trim().replace(Regex("\\s+")," ").also { require(label.matches(it) && it.any(Char::isLetter)) }
    fun freshFix(wallAge: Long, elapsedAge: Long, accuracy: Float): Boolean = wallAge in 0..120_000 && elapsedAge in 0..120_000 && accuracy.isFinite() && accuracy in 0f..5000f
    data class Town(val town: String,val district: String,val at: Long)
    private fun aad(id: String,role: String,at: Long): ByteArray { require(hex32.matches(id) && role in setOf("a","b"));return "oor-family-town/v1|$id|$role|$at".toByteArray(Charsets.UTF_8) }
    fun encrypt(id: String,role: String,key: String,town: Town,generation: Int): JSONObject {
        val plain=JSONObject().put("town",clean(town.town)).put("district",if(town.district.isBlank()) "" else clean(town.district)).put("at",town.at).toString().toByteArray(Charsets.UTF_8)
        require(plain.size<=768 && town.at>0 && generation>0)
        val iv=ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,SecretKeySpec(bytes(key),"AES"),GCMParameterSpec(128,iv));cipher.updateAAD(aad(id,role,town.at))
        val enc=Base64.getUrlEncoder().withoutPadding()
        return JSONObject().put("sampleAt",town.at).put("generation",generation).put("iv",enc.encodeToString(iv)).put("ciphertext",enc.encodeToString(cipher.doFinal(plain)))
    }
    fun decrypt(id: String,role: String,key: String,update: JSONObject,serverNow: Long,localNow: Long): Town {
        val at=update.getLong("sampleAt");val expires=update.getLong("expires")
        require(serverNow-at in 0..TTL && localNow-at in 0..TTL && expires>serverNow && expires<=at+TTL)
        val ivText=update.getString("iv");val ctText=update.getString("ciphertext");require(ivText.length==16 && ctText.length in 24..1536)
        val iv=Base64.getUrlDecoder().decode(ivText);require(iv.size==12)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(bytes(key),"AES"),GCMParameterSpec(128,iv));cipher.updateAAD(aad(id,role,at))
        val plain=cipher.doFinal(Base64.getUrlDecoder().decode(ctText));require(plain.size<=768)
        val j=JSONObject(String(plain,Charsets.UTF_8));require(j.length()==3 && j.getLong("at")==at && j.get("town") is String && j.get("district") is String)
        return Town(clean(j.getString("town")),j.getString("district").let { if(it.isBlank()) "" else clean(it) },at)
    }
}
