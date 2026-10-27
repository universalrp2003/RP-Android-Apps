package com.universalrp.podcast

/**
 * The shows the app starts with.
 *
 * These are public RSS feeds, chosen because they publish in Tamil and because they are long-running
 * rather than because of any relationship with this project. A feed that stops publishing simply
 * shows an empty list; nothing here is a promise about availability.
 *
 * They are held as URLs and titles only, never as a bundled audio file or a cached episode, so the
 * app stays small and the content stays the publisher's.
 */
object BuiltInShows {

    fun all(): List<PodcastShow> = listOf(
        show(
            id = "tamil-news",
            title = "தமிழ் செய்திகள்",
            url = "https://feeds.simplecast.com/54nAGcIl",
        ),
        show(
            id = "tamil-kathai",
            title = "தமிழ் கதைகள்",
            url = "https://feeds.megaphone.fm/kathaikaga",
        ),
        show(
            id = "tamil-thai",
            title = "தாய் கற்றுத் தரும் உண்மை",
            url = "https://anchor.fm/s/7c0e4c8/podcast/rss",
        ),
        show(
            id = "tamil-sirukathaigal",
            title = "சிறுகதைகள்",
            url = "https://feeds.transistor.fm/sirukathaigal",
        ),
    )

    private fun show(id: String, title: String, url: String) =
        PodcastShow(id = id, title = title, feedUrl = url, builtIn = true)
}
