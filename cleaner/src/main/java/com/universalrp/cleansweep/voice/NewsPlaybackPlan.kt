package com.universalrp.cleansweep.voice

import com.universalrp.cleansweep.data.NewsHeadline
import com.universalrp.cleansweep.data.NewsSpeechSanitizer

/** A clip never straddles two stories, so completion can be attributed without guessing. */
object NewsPlaybackPlan {
    data class Part(val text: String, val completedHeadline: Int? = null)

    fun build(headlines: List<NewsHeadline>, tamil: Boolean, lead: String = ""): List<Part> = buildList {
        headlines.forEachIndexed { index, item ->
            val story = item.spokenText.ifBlank {
                NewsSpeechSanitizer.cleanHeadline(item.title, item.source)
            }.trimEnd('.', '!', '?', ' ', ',', ';', ':', '·')
            if (story.isBlank()) return@forEachIndexed
            val intro = if (isEmpty()) {
                lead.trim().let { if (it.isBlank()) "" else "$it " } +
                    if (tamil) "இன்றைய முக்கியச் செய்திகள். " else "Today's top news headlines. "
            } else ""
            val humour = if(com.universalrp.cleansweep.data.BalancedNewsPolicy.category(item) == com.universalrp.cleansweep.data.BalancedNewsPolicy.Category.HUMOUR)
                (if(tamil) "நகைச்சுவை; உண்மைச் செய்தியல்ல. " else "Humour, not factual news. ") else ""
            val clips = NewsSpeechSanitizer.splitForHd("$intro$humour$story.")
            clips.forEachIndexed { clip, text ->
                add(Part(text, index.takeIf { clip == clips.lastIndex }))
            }
        }
    }

    /** isMusicActive includes our own player, including its between-clip release tail. */
    fun checkOtherMedia(playbackStarted: Boolean): Boolean = !playbackStarted
}
