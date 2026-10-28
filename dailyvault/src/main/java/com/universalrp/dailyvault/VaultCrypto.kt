package com.universalrp.dailyvault

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned, portable encrypted file. Never contains a device-only key or plaintext password. */
object VaultCrypto {
    const val MAX_FILE = 16 * 1024 * 1024
    private const val ITERATIONS = 210_000
    private val magic = byteArrayOf(0x54,0x44,0x56,0x01)
    fun seal(plain: ByteArray, password: CharArray): ByteArray {
        require(plain.size <= MAX_FILE-64 && password.size in 10..256)
        val random=SecureRandom(); val salt=ByteArray(16).also(random::nextBytes); val iv=ByteArray(12).also(random::nextBytes)
        val header=magic+salt+iv
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv));cipher.updateAAD(header)
        return header+cipher.doFinal(plain)
    }
    fun open(file: ByteArray, password: CharArray): ByteArray {
        require(file.size in 48..MAX_FILE && password.size in 10..256)
        require(file.copyOfRange(0,4).contentEquals(magic))
        val salt=file.copyOfRange(4,20);val iv=file.copyOfRange(20,32)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv));cipher.updateAAD(file.copyOfRange(0,32))
        return cipher.doFinal(file,32,file.size-32)
    }
    private fun key(password: CharArray,salt: ByteArray): SecretKeySpec {
        val spec=PBEKeySpec(password,salt,ITERATIONS,256)
        return try {
            val raw=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try { SecretKeySpec(raw,"AES") } finally { raw.fill(0) }
        } finally { spec.clearPassword() }
    }
}
