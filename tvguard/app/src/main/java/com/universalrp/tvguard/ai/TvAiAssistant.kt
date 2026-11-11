package com.universalrp.tvguard.ai

import android.content.Context
import com.universalrp.tvguard.R
import com.universalrp.tvguard.settings.TvFeature
import com.universalrp.tvguard.settings.TvFeaturePreferences
import com.universalrp.tvguard.voice.TvVoiceAssistant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Coordinates an AI question: gating, redaction, the request, the spoken answer.
 *
 * Gating is deliberate. A question is only sent when the user has switched Expert on, has
 * switched the AI feature on, and has a provider configured. If any of those is missing the
 * assistant says which one, and nothing leaves the TV.
 */
object TvAiAssistant {

    sealed interface Outcome {
        data class Answer(val text: String, val engine: String) : Outcome
        /** Local, rule-based reply. Not general AI, and labelled as such. */
        data class Local(val text: String) : Outcome
        data class Error(val message: String) : Outcome
        data object Disabled : Outcome
    }

    /** True when a real provider can answer right now. */
    fun isUsable(context: Context): Boolean {
        val settings = TvFeaturePreferences(context).load()
        if (!settings.enabled(TvFeature.AI_ASSISTANT)) return false
        return TvAiSettings.load(context).ready
    }

    fun isEnabled(context: Context): Boolean =
        TvFeaturePreferences(context).load().enabled(TvFeature.AI_ASSISTANT)

    /**
     * Answers a question. [localAnswer] is used only when the AI feature is off, so the
     * caller can keep its existing rule-based helper without pretending it is a model.
     */
    suspend fun ask(
        context: Context,
        question: String,
        localAnswer: suspend () -> String,
    ): Outcome {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return Outcome.Error(context.getString(R.string.ai_empty_question))
        if (!isEnabled(context)) return Outcome.Local(localAnswer())
        val config = TvAiSettings.load(context)
        if (!config.ready) {
            return Outcome.Error(context.getString(R.string.ai_not_configured, config.provider.label))
        }
        val tamilQuestion = trimmed.any { it in '\u0B80'..'\u0BFF' }
        val prompt = TvAiClient.systemPrompt(TvAiSettings.answerLanguage(context), tamilQuestion)
        return try {
            val result = withContext(Dispatchers.IO) { TvAiClient.ask(config, trimmed, prompt) }
            when (result) {
                is TvAiClient.Result.Answer -> {
                    if (TvAiSettings.speakAnswers(context)) TvVoiceAssistant.speak(result.text)
                    Outcome.Answer(result.text, result.engine)
                }
                is TvAiClient.Result.Failure -> Outcome.Error(result.message)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Outcome.Error(context.getString(R.string.ai_request_failed))
        }
    }

    /** Models offered for the current provider, from the provider itself where possible. */
    suspend fun availableModels(context: Context, config: TvAiConfig): List<String> = try {
        withContext(Dispatchers.IO) { TvAiClient.listModels(config) }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyList()
    }

    fun languageLabel(context: Context): String = when (TvAiSettings.answerLanguage(context)) {
        "en" -> "English"
        "ta" -> "Tamil"
        else -> "Auto"
    }

    fun normalizeLanguage(value: String): String =
        if (value.lowercase(Locale.ROOT).startsWith("ta")) "ta" else "auto"
}
