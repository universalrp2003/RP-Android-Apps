package com.universalrp.tvguard.voice

import com.universalrp.tvguard.settings.TvFeature
import java.util.Locale

enum class TvVoiceCommand { SLEEP, RAM_BOOST, SCREENSHOT, SETTINGS, OPEN_APP, QUESTION }

object TvVoiceCommands {
    private fun normalized(text: String) = text.lowercase(Locale.ROOT).trim().trimEnd('.', '!', '?').trim()
    private val sleep = Regex("^(?:please )?(?:turn off|power off|sleep|standby)(?: (?:the |my )?(?:tv|television|screen|box))?(?: now)?$")
    private val ram = Regex("^(?:please )?(?:boost(?: the)? ram|clean (?:the )?(?:ram|memory)|free (?:up )?(?:the )?(?:ram|memory))(?: now)?$")
    private val screenshot = Regex("^(?:please )?(?:take (?:a )?screenshot|screenshot|capture (?:the )?screen)(?: now)?$")
    private val settings = Regex("^(?:please )?open (?:the )?(?:tv |system )?(?:settings|preferences)$")
    private val open = Regex("^(?:please )?(?:open|launch|start) (.+)$")

    /** Questions mentioning an action must not execute that action. */
    fun classify(text: String): TvVoiceCommand {
        if (text.trim().endsWith('?')) return TvVoiceCommand.QUESTION
        val q = normalized(text)
        return when {
            sleep.matches(q) -> TvVoiceCommand.SLEEP
            ram.matches(q) -> TvVoiceCommand.RAM_BOOST
            screenshot.matches(q) -> TvVoiceCommand.SCREENSHOT
            settings.matches(q) -> TvVoiceCommand.SETTINGS
            open.matches(q) -> TvVoiceCommand.OPEN_APP
            else -> TvVoiceCommand.QUESTION
        }
    }

    /** Voice cannot bypass the separate switches for actions with side effects. */
    fun requiredFeature(command: TvVoiceCommand): TvFeature? = when (command) {
        TvVoiceCommand.SCREENSHOT -> TvFeature.SCREENSHOT
        TvVoiceCommand.RAM_BOOST -> TvFeature.RAM_BOOST
        else -> null
    }

    fun appQuery(text: String): String? = open.matchEntire(normalized(text))
        ?.groupValues?.getOrNull(1)?.removePrefix("the ")?.trim()?.takeIf { it.isNotEmpty() }
}
