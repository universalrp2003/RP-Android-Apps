package com.oorarivippu.voiceannouncer.family

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

/** All pairing secrets and local display names encrypted at rest. Never included in travel history. */
object FamilyStore {
    private const val ALIAS="oor_family_local_v1"
    private fun key(): SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    }
    @Synchronized fun read(c: Context): List<JSONObject> {
        val raw=c.getSharedPreferences("oor_family_v1",0).getString("sealed",null) ?: return emptyList()
        require(raw.length<=40_000)
        val parts=raw.split('.');require(parts.size==2)
        val d=Base64.getDecoder();val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,d.decode(parts[0])))
        val rows=JSONArray(String(cipher.doFinal(d.decode(parts[1])),Charsets.UTF_8));require(rows.length()<=FamilyProtocol.MAX_MEMBERS)
        return (0 until rows.length()).map { rows.getJSONObject(it) }
    }
    @Synchronized fun change(c: Context, f: (MutableList<JSONObject>)->Unit) {
        val rows=read(c).toMutableList()
        fun encode()=JSONArray().also { a->rows.forEach { a.put(it) } }.toString()
        val before=encode();f(rows);require(rows.size<=FamilyProtocol.MAX_MEMBERS)
        val after=encode();if(after==before)return // Refresh/replay must not cause redundant encrypted disk writes.
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
        val plain=after.toByteArray(Charsets.UTF_8)
        val e=Base64.getEncoder();val sealed=e.encodeToString(cipher.iv)+"."+e.encodeToString(cipher.doFinal(plain))
        check(c.getSharedPreferences("oor_family_v1",0).edit().putString("sealed",sealed).commit())
    }
    fun active(c: Context): Int = runCatching { read(c).count { it.optBoolean("enabled") && it.optLong("until")>System.currentTimeMillis() && it.optString("pending").isEmpty() } }.getOrDefault(0)
}
