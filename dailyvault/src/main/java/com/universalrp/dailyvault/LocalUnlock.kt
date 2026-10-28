package com.universalrp.dailyvault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-local convenience credential, excluded from backup. Portable backup still needs password. */
class LocalUnlock(context: Context) {
    private val prefs=context.getSharedPreferences("local-unlock",Context.MODE_PRIVATE)
    val mode: String get()=prefs.getString("mode","password") ?: "password"
    private val alias="daily-vault-local-unlock-v1"
    private fun key(): SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun save(mode: String,password: CharArray) {
        require(mode in listOf("password","device","none"))
        if(mode=="password") { clear();return }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val raw=String(password).toByteArray(Charsets.UTF_8)
        val encoded=try { Base64.encodeToString(cipher.iv+cipher.doFinal(raw),Base64.NO_WRAP) } finally { raw.fill(0) }
        check(prefs.edit().putString("wrapped",encoded).putString("mode",mode).commit())
    }
    fun read(): CharArray {
        val data=Base64.decode(prefs.getString("wrapped",null) ?: error("No local credential"),Base64.NO_WRAP)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,data.copyOfRange(0,12))) }
        val raw=cipher.doFinal(data,12,data.size-12)
        return try { String(raw,Charsets.UTF_8).toCharArray() } finally { raw.fill(0) }
    }
    fun clear() { check(prefs.edit().clear().commit()) }
}
