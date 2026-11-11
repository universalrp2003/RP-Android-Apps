package com.universalrp.tvguard.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Minimal OpenAI-compatible chat client.
 *
 * Uses HttpURLConnection on purpose: it is in the platform, so enabling the AI feature adds
 * no dependency, no background scheduler and no third-party SDK to the APK.
 *
 * Every call is one-shot and caller-driven. There is no retry loop and no polling; the only
 * automatic behaviour is trying the next keyless endpoint after a keyless failure.
 */
object TvAiClient {

    sealed interface Result {
        data class Answer(val text: String, val engine: String) : Result
        /** [message] is safe to show: it never contains the API key. */
        data class Failure(val message: String, val retryable: Boolean = true) : Result
    }

    private const val TIMEOUT_MS = 30_000

    /** Asks for an answer. Blocking; call from a background dispatcher. */
    fun ask(config: TvAiConfig, question: String, systemPrompt: String): Result =
        when (config.provider) {
            TvAiProvider.FREE -> askKeyless(config, question, systemPrompt)
            else -> askOpenAiCompatible(config, question, systemPrompt)
        }

    private fun askKeyless(config: TvAiConfig, question: String, systemPrompt: String): Result {
        val endpoints = buildList {
            TvAiSettings.KEYLESS.firstOrNull { it.url == config.freeEndpointUrl }?.let { add(it) }
            TvAiSettings.KEYLESS.filter { it.url != config.freeEndpointUrl }.forEach { add(it) }
        }
        var lastFailure: Result.Failure? = null
        for (endpoint in endpoints) {
            val result = post(
                url = endpoint.url,
                key = null,
                model = endpoint.model,
                question = question,
                systemPrompt = systemPrompt,
            )
            when (result) {
                is Result.Answer -> return result.copy(engine = "Free AI · ${endpoint.label.substringBefore(" (")}")
                is Result.Failure -> lastFailure = result
            }
        }
        return lastFailure ?: Result.Failure("No keyless AI endpoint is reachable right now.")
    }

    private fun askOpenAiCompatible(config: TvAiConfig, question: String, systemPrompt: String): Result {
        val base = config.resolvedBaseUrl
        if (base.isBlank()) return Result.Failure("No server address is set for ${config.provider.label}.", retryable = false)
        if (config.provider.needsKey && config.apiKey.isBlank()) {
            return Result.Failure("No API key is saved for ${config.provider.label}.", retryable = false)
        }
        val model = config.resolvedModel
        if (model.isBlank()) return Result.Failure("No model is selected.", retryable = false)
        return post(
            url = "$base/chat/completions",
            key = config.apiKey.takeIf { it.isNotBlank() },
            model = model,
            question = question,
            systemPrompt = systemPrompt,
        ).let { if (it is Result.Answer) it.copy(engine = config.engineLabel) else it }
    }

    /** Lists the models the user's own key can actually use. */
    fun listModels(config: TvAiConfig): List<String> {
        if (config.provider == TvAiProvider.FREE) return TvAiSettings.KEYLESS.map { it.model }
        val base = config.resolvedBaseUrl
        if (base.isBlank()) return emptyList()
        val connection = try {
            (URL("$base/models").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                if (config.apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                    setRequestProperty("Accept", "application/json")
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return try {
            if (connection.responseCode !in 200..299) return emptyList()
            val body = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
            (0 until data.length()).mapNotNull { i ->
                data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private fun post(
        url: String,
        key: String?,
        model: String,
        question: String,
        systemPrompt: String,
    ): Result {
        val payload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", question))
            })
            put("max_tokens", 700)
            put("temperature", 0.4)
        }.toString()

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (key != null) setRequestProperty("Authorization", "Bearer $key")
            }
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) {
                // Read a short error body but never echo headers or the key back.
                val detail = runCatching {
                    (connection.errorStream ?: connection.inputStream)?.bufferedReader()
                        ?.use(BufferedReader::readText)?.take(300)
                }.getOrNull()
                return Result.Failure(httpMessage(code, detail))
            }
            val body = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            val text = extractAnswer(body)
            if (text.isBlank()) Result.Failure("The provider returned an empty answer.")
            else Result.Answer(text, "")
        } catch (_: Exception) {
            Result.Failure("Could not reach the AI provider. Check the TV's internet connection.")
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    /** Pulls choices[0].message.content out of an OpenAI-style response. */
    internal fun extractAnswer(body: String): String = try {
        val root = JSONObject(body)
        val choices = root.optJSONArray("choices") ?: return ""
        val first = choices.optJSONObject(0) ?: return ""
        first.optJSONObject("message")?.optString("content")?.trim().orEmpty()
    } catch (_: Exception) {
        ""
    }

    private fun httpMessage(code: Int, detail: String?): String {
        val hint = when (code) {
            401, 403 -> "The provider rejected the API key."
            404 -> "The provider does not recognise that model or address."
            429 -> "The provider's rate limit was reached. Wait and try again."
            in 500..599 -> "The provider had a server problem."
            else -> "The provider refused the request."
        }
        val clean = detail?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotEmpty() }
        return if (clean == null) hint else "$hint ($code)"
    }

    /** The instruction sent ahead of the user's question. */
    fun systemPrompt(answerLanguage: String, tamilQuestion: Boolean): String {
        val language = when {
            answerLanguage == "ta" -> "Answer in Tamil."
            answerLanguage == "en" -> "Answer in English."
            tamilQuestion -> "Answer in Tamil."
            else -> "Answer in the same language as the question."
        }
        return "You are the assistant inside Android TV Guard, an app that reports TV hardware " +
            "and storage readings. Keep answers short and easy to read on a television: at most " +
            "a few sentences, no markdown tables. If you are unsure, say so plainly rather than " +
            "guessing. $language"
    }

    /** Language for spoken answers, based on the answer text. */
    fun spokenLanguage(text: String): String =
        if (text.any { it in '\u0B80'..'\u0BFF' }) "ta-IN" else Locale.getDefault().toLanguageTag()
}
