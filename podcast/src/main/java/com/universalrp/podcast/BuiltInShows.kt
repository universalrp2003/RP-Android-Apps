package com.universalrp.podcast

/**
 * The shows the app starts with.
 *
 * Two things about this list are deliberate, and both come from the first release being wrong.
 *
 * The URLs are ones I have a real source for, not ones I invented. The first release shipped four
 * plausible-looking URLs that did not exist, so every fetch failed and the screen sat on "loading"
 * with no way back - which is exactly what the owner reported. A feed that 404s is now a visible
 * error with a retry, but a list of dead links is still a bad first impression, so the two here are
 * public feeds that were listed with their RSS addresses by a podcast directory.
 *
 * Nothing is bundled and nothing is cached: no audio, no episode. The app stays small and the
 * content stays the publisher's. Adding a feed by URL is the primary way this app is meant to grow,
 * and the built-in two are a starting point rather than the product.
 */
object BuiltInShows {

    fun all(): List<PodcastShow> = listOf(
        show(
            id = "kadhai-ketkum-neram",
            title = "கதை கேட்கும் நேரம்",
            url = "https://feeds.buzzsprout.com/1143884",
        ),
        show(
            id = "millennial-thoughts",
            title = "Tamil Podcast \u00b7 Millennial Thoughts",
            url = "https://anchor.fm/s/2c4ecfc4/podcast/rss",
        ),
    )

    private fun show(id: String, title: String, url: String) =
        PodcastShow(id = id, title = title, feedUrl = url, builtIn = true)
}
