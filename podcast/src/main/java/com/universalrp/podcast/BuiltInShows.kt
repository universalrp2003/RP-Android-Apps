package com.universalrp.podcast

/** Published feed addresses, not constructed URLs. See docs/podcast-feed-evidence.md.
 * Display aliases stay bilingual after loading; the feed's own episode titles are unchanged.
 */
object BuiltInShows {
    fun all(): List<PodcastShow> = listOf(
        show("kadhai-neram", "கதை நேரம் / Kadhai Neram", "https://anchor.fm/s/57e45e88/podcast/rss"),
        show("pesum-kadhai", "பேசும் கதை / Pesum Kadhai", "https://anchor.fm/s/289076a8/podcast/rss"),
        show("tamil-story-for-kids", "குழந்தைகளுக்கான தமிழ்க் கதைகள் / Tamil Story For Kids", "https://anchor.fm/s/4210349c/podcast/rss"),
        show("siruvar-kadhaigal", "சிறுவர் கதைகள் / Siruvar Kadhaigal", "https://anchor.fm/s/aee7640/podcast/rss"),
        show("kathai-arasan", "கதை அரசன் / Kathai Arasan", "https://anchor.fm/s/47a2df2c/podcast/rss"),
    )
    private fun show(id: String, title: String, url: String) =
        PodcastShow(id = id, title = title, feedUrl = url, builtIn = true)
}
