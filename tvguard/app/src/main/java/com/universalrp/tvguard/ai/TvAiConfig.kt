package com.universalrp.tvguard.ai

import android.content.Context
import android.content.SharedPreferences
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Optional general-purpose AI for TV Guard.
 *
 * This is a real question-and-answer client, not the local rule-based helper in
 * `voice/TvAssistantAnswers`. It talks to an OpenAI-compatible chat-completions endpoint
 * that the user chooses and, for every provider except the keyless pool, pays for with
 * their own key.
 *
 * Design rules kept deliberately:
 *  - Off by default. Nothing is sent anywhere until the user enables it and asks.
 *  - No bundled key and no proxy: requests go from the TV straight to the provider.
 *  - No background work: a request is made only for a question the user just asked.
 *  - The key is encrypted at rest with an Android Keystore AES key. There is no
 *    plaintext fallback if Keystore is unavailable.
 *
 * Model IDs below are *placeholders the provider is asked to confirm*; the app calls the
 * provider's own /models endpoint. Listing a model here never means the user's key can use it.
 */
enum class TvAiProvider(
    val id: String,
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val needsKey: Boolean,
    val note: String,
) {
    /**
     * Keyless pool. Truthfully: these are shared community services. One of them answered
     * when this list was last checked, but they are rate limited and can disappear without
     * notice. TV Guard shows that warning and never pretends it is a guarantee.
     */
    FREE(
        id = "free",
        label = "Free (no key)",
        baseUrl = "",
        defaultModel = "",
        needsKey = false,
        note = "Shared community endpoints with no signup. Rate limited and not guaranteed; " +
            "they may stop working at any time.",
    ),
    GEMINI(
        id = "gemini",
        label = "Google Gemini",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultModel = "gemini-2.5-flash",
        needsKey = true,
        note = "Key from Google AI Studio. Free tier and paid usage depend on your own account.",
    ),
    GROQ(
        id = "groq",
        label = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        needsKey = true,
        note = "Key from console.groq.com. Has a free tier.",
    ),
    NVIDIA(
        id = "nvidia",
        label = "NVIDIA NIM",
        baseUrl = "https://integrate.api.nvidia.com/v1",
        defaultModel = "meta/llama-3.3-70b-instruct",
        needsKey = true,
        note = "Key from build.nvidia.com. Developer credits may be free.",
    ),
    OPENROUTER(
        id = "openrouter",
        label = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "",
        needsKey = true,
        note = "Key from openrouter.ai. Some models are marked :free; check their list.",
    ),
    OPENAI(
        id = "openai",
        label = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        needsKey = true,
        note = "Key from platform.openai.com. Paid per use.",
    ),
    CUSTOM(
        id = "custom",
        label = "Custom (OpenAI-style)",
        baseUrl = "",
        defaultModel = "",
        needsKey = true,
        note = "Any OpenAI-compatible server, including Ollama or LM Studio on your own network.",
    );

    companion object {
        fun fromId(id: String?): TvAiProvider =
            entries.firstOrNull { it.id == id } ?: GEMINI

        /** Recognises a pasted key by the prefix the provider itself uses. */
        fun forKey(key: String): TvAiProvider? {
            val k = key.trim()
            if (k.isEmpty()) return null
            return when {
                k.startsWith("AIza") -> GEMINI
                k.startsWith("gsk_") -> GROQ
                k.startsWith("nvapi-") -> NVIDIA
                k.startsWith("sk-or-") -> OPENROUTER
                k.startsWith("sk-proj-") || k.startsWith("sk-") -> OPENAI
                else -> null
            }
        }
    }
}

/** One keyless endpoint. Verified availability is recorded, not assumed. */
data class KeylessEndpoint(val label: String, val url: String, val model: String)

data class TvAiConfig(
    val provider: TvAiProvider = TvAiProvider.GEMINI,
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = "",
    /** Which keyless endpoint last answered, so a working one is retried first. */
    val freeEndpointUrl: String = "",
    val keyStored: Boolean = false,
    val keyProtectedAtRest: Boolean = true,
) {
    val resolvedModel: String get() = model.trim().ifBlank { provider.defaultModel }
    val resolvedBaseUrl: String get() = baseUrl.trim().ifBlank { provider.baseUrl }.trimEnd('/')

    val ready: Boolean
        get() = when (provider) {
            TvAiProvider.FREE -> true
            TvAiProvider.CUSTOM -> resolvedBaseUrl.isNotBlank() && resolvedModel.isNotBlank()
            else -> apiKey.isNotBlank() && resolvedModel.isNotBlank()
        }

    val engineLabel: String
        get() = when (provider) {
            TvAiProvider.FREE -> {
                val known = TvAiSettings.KEYLESS.firstOrNull { it.url == freeEndpointUrl }
                "Free AI · ${known?.label?.substringBefore(" (") ?: "best available"}"
            }
            TvAiProvider.CUSTOM -> "Custom · ${resolvedModel.ifBlank { "no model" }}"
            else -> "${provider.label} · ${resolvedModel.ifBlank { "no model" }}"
        }

    /** Never let a key reach a log, a crash report or a screenshot of a toString(). */
    override fun toString(): String =
        "TvAiConfig(provider=${provider.id}, apiKey=${if (apiKey.isBlank()) "<empty>" else "<redacted>"}, " +
            "model=$model, baseUrl=${if (baseUrl.isBlank()) "<empty>" else "<redacted>"}, " +
            "keyStored=$keyStored, keyProtectedAtRest=$keyProtectedAtRest)"
}

/** AES-GCM at rest using an Android Keystore key; no plaintext fallback. */
internal object TvAiSecretStorage {
    private const val PREFIX = "keystore.aesgcm.v1:"
    private const val KEY_ALIAS = "com.universalrp.tvguard.ai-secrets.v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private val lock = Any()

    data class ReadResult(val value: String, val protectedAtRest: Boolean, val hadStoredValue: Boolean) {
        override fun toString(): String =
            "ReadResult(value=${if (value.isBlank()) "<empty>" else "<redacted>"}, " +
                "protectedAtRest=$protectedAtRest, hadStoredValue=$hadStoredValue)"
    }

    fun read(prefs: SharedPreferences, key: String): ReadResult = synchronized(lock) {
        val stored = prefs.getString(key, null)
        if (stored.isNullOrBlank()) return@synchronized ReadResult("", true, false)
        if (stored.startsWith(PREFIX)) {
            val plain = runCatching { decrypt(stored, key) }.getOrNull()
            return@synchronized if (plain == null) ReadResult("", false, true)
            else ReadResult(plain, true, true)
        }
        // A plaintext value should never exist in this app; migrate it and then drop it.
        val encrypted = runCatching { encrypt(stored, key) }.getOrNull()
        if (encrypted != null && prefs.edit().putString(key, encrypted).commit()) {
            ReadResult(stored, true, true)
        } else {
            runCatching { prefs.edit().remove(key).commit() }
            ReadResult(stored, false, true)
        }
    }

    fun write(prefs: SharedPreferences, key: String, value: String): Boolean = synchronized(lock) {
        if (value.isBlank()) return@synchronized prefs.edit().remove(key).commit()
        val encrypted = runCatching { encrypt(value, key) }.getOrNull() ?: return@synchronized false
        prefs.edit().putString(key, encrypted).commit()
    }

    fun remove(prefs: SharedPreferences, key: String): Boolean = synchronized(lock) {
        prefs.edit().remove(key).commit()
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    internal fun encrypt(plaintext: String, preferenceKey: String, key: SecretKey = keystoreKey()): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(preferenceKey.toByteArray(StandardCharsets.UTF_8))
        val encrypted = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val iv = cipher.iv
        require(iv.size == IV_BYTES) { "Unexpected AES-GCM IV size" }
        val payload = ByteBuffer.allocate(iv.size + encrypted.size).put(iv).put(encrypted).array()
        return PREFIX + Base64.getEncoder().encodeToString(payload)
    }

    internal fun decrypt(encoded: String, preferenceKey: String, key: SecretKey = keystoreKey()): String? = runCatching {
        require(encoded.startsWith(PREFIX))
        val payload = Base64.getDecoder().decode(encoded.removePrefix(PREFIX))
        require(payload.size > IV_BYTES + GCM_TAG_BITS / 8)
        val buffer = ByteBuffer.wrap(payload)
        val iv = ByteArray(IV_BYTES).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(preferenceKey.toByteArray(StandardCharsets.UTF_8))
        String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }.getOrNull()
}

object TvAiSettings {
    private const val PREFS = "tvguard_ai"

    /**
     * Keyless endpoints, in the order they are tried.
     *
     * Honesty note carried into the UI: only Pollinations was reachable and advertising an
     * anonymous tier when this list was compiled. Kilo returned an empty document and OVH
     * was not reached, so neither is described as working. The app reports which one, if
     * any, actually answered and remembers it.
     */
    val KEYLESS: List<KeylessEndpoint> = listOf(
        KeylessEndpoint("Pollinations (shared)", "https://text.pollinations.ai/openai", "openai-fast"),
        KeylessEndpoint("OVHcloud AI (2/min)", "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions", "gpt-oss-120b"),
    )

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): TvAiConfig {
        val p = prefs(context)
        val provider = TvAiProvider.fromId(p.getString("provider", null))
        val stored = TvAiSecretStorage.read(p, "api_key_${provider.id}")
        return TvAiConfig(
            provider = provider,
            apiKey = stored.value,
            model = p.getString("model_${provider.id}", "") ?: "",
            baseUrl = p.getString("base_url_${provider.id}", "") ?: "",
            freeEndpointUrl = p.getString("free_endpoint_url", "") ?: "",
            keyStored = stored.hadStoredValue,
            keyProtectedAtRest = stored.protectedAtRest,
        )
    }

    /**
     * Saves the configuration.
     *
     * [updateKey] is false when the user only changed the provider, model or address and did
     * not retype a key. In that case the existing secret for that provider is left alone.
     * This matters because keys are stored per provider: carrying one provider's key into
     * another provider's slot would send the wrong credential to the wrong service.
     */
    fun save(context: Context, config: TvAiConfig, updateKey: Boolean = true): Boolean {
        val p = prefs(context)
        val keyOk = if (updateKey) {
            TvAiSecretStorage.write(p, "api_key_${config.provider.id}", config.apiKey.trim())
        } else {
            true
        }
        p.edit()
            .putString("provider", config.provider.id)
            .putString("model_${config.provider.id}", config.model.trim())
            .putString("base_url_${config.provider.id}", config.baseUrl.trim())
            .putString("free_endpoint_url", config.freeEndpointUrl)
            .apply()
        return keyOk
    }

    fun forgetKey(context: Context, provider: TvAiProvider): Boolean =
        TvAiSecretStorage.remove(prefs(context), "api_key_${provider.id}")

    /** Clear everything, used when the user removes the AI feature. */
    fun clear(context: Context) {
        val p = prefs(context)
        TvAiProvider.entries.forEach { TvAiSecretStorage.remove(p, "api_key_${it.id}") }
        p.edit().clear().apply()
    }

    fun answerLanguage(context: Context): String =
        prefs(context).getString("answer_language", "auto") ?: "auto"

    fun setAnswerLanguage(context: Context, value: String) {
        prefs(context).edit().putString("answer_language", value).apply()
    }

    /** Speak answers aloud. Off unless asked, because it is intrusive on a shared TV. */
    fun speakAnswers(context: Context): Boolean = prefs(context).getBoolean("speak_answers", true)

    fun setSpeakAnswers(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("speak_answers", on).apply()
    }
}
