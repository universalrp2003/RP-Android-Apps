package com.universalrp.weeklymoney

import android.annotation.TargetApi
import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-local authenticated wrapping only. Neither the key nor the plaintext secret is backed up. */
@TargetApi(28)
class BiometricGate(private val activity: Activity) {
    private val prefs = activity.getSharedPreferences("biometric_wrap", Activity.MODE_PRIVATE)
    private val alias = "weeklymoney.biometric.v1"
    fun configured() = prefs.contains("ciphertext") && prefs.contains("iv")
    fun disable() {
        prefs.edit().clear().commit()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias(alias)) deleteEntry(alias) }
    }
    fun authenticate(enroll: CharArray? = null, success: (CharArray?) -> Unit, failure: () -> Unit): CancellationSignal {
        val signal = CancellationSignal()
        val plain = enroll?.concatToString()?.toByteArray(Charsets.UTF_8)
        try {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = if (plain != null) {
                val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true)
                if (Build.VERSION.SDK_INT >= 30) spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                else spec.setUserAuthenticationValidityDurationSeconds(-1)
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(spec.build()) }.generateKey()
            } else store.getKey(alias, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            if (plain != null) cipher.init(Cipher.ENCRYPT_MODE, key)
            else cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)))
            val prompt = BiometricPrompt.Builder(activity).setTitle("வார வசூல் · Biometric")
                .setDescription(if (plain != null) "இந்த phone-ல் திறக்க biometric இணைக்கவும்" else "பதிவுகளைத் திறக்க அடையாளம் உறுதிப்படுத்தவும்")
                .setNegativeButton("ரத்து / Cancel", activity.mainExecutor) { _, _ -> plain?.fill(0); failure() }
                .build()
            prompt.authenticate(BiometricPrompt.CryptoObject(cipher), signal, activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(code: Int, text: CharSequence) { plain?.fill(0); failure() }
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        try {
                            val authenticated = requireNotNull(result.cryptoObject?.cipher)
                            if (plain != null) {
                                val wrapped = authenticated.doFinal(plain)
                                check(prefs.edit().putString("iv", Base64.encodeToString(authenticated.iv, Base64.NO_WRAP))
                                    .putString("ciphertext", Base64.encodeToString(wrapped, Base64.NO_WRAP)).commit())
                                success(null)
                            } else {
                                val decoded = authenticated.doFinal(Base64.decode(prefs.getString("ciphertext", ""), Base64.NO_WRAP))
                                try { success(decoded.toString(Charsets.UTF_8).toCharArray()) } finally { decoded.fill(0) }
                            }
                        } catch (_: Exception) { failure() } finally { plain?.fill(0) }
                    }
                })
        } catch (_: Exception) { plain?.fill(0); failure() }
        return signal
    }
}
