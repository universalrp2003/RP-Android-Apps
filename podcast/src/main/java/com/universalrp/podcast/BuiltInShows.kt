package com.universalrp.podcast

/**
 * The shows the app starts with.
 *
 * Three things about this list are deliberate, and all three come from the owner's testing.
 *
 * **Every URL is one I have a real source for, not one I invented.** The first release shipped four
 * plausible-looking URLs that did not exist, so every fetch failed and the screen sat on the loading
 * word with no way back. A feed that 404s is now a visible error with a retry, but a list of dead
 * links is still a bad first impression.
 *
 * **Every show is for a family to listen to together.** The owner tested 1.1 and found that one of
 * its two built-in feeds published adult material - love stories, queer fiction, office romance -
 * and asked for children's stories and the stories a grandfather or grandmother tells instead. That
 * feed has been removed. What is here is Panchatantra, Tenali Raman, Thirukkural, the Ramayanam and
 * the puranas: the stories a Tamil household already knows, narrated for children.
 *
 * **Nothing is bundled and nothing is cached**: no audio, no episode, no artwork. The app stays
 * small and the content stays the publisher's. Adding a feed by URL is the primary way this app is
 * meant to grow, and these four are a starting point rather than the product.
 *
 * None of these URLs could be reached from the machine this was built on - only github, npm and pypi
 * are reachable there - so "it resolves" is unverified until a phone opens them. What is verified is
 * that each address was published as the show's RSS feed by a podcast directory, not constructed by
 * guesswork.
 */
object BuiltInShows {

    fun all(): List<PodcastShow> = listOf(
        show(
            id = "kadhai-neram",
            title = "கதை நேரம் | Tamil Kids & Family Bedtime Stories",
            url = "https://anchor.fm/s/57e45e88/podcast/rss",
        ),
        show(
            id = "pesum-kadhai",
            title = "Pesum Kadhai - Tamil audio stories for kids",
            url = "https://anchor.fm/s/289076a8/podcast/rss",
        ),
        show(
            id = "tamil-stories-for-kids",
            title = "Tamil Stories For Kids",
            url = "https://feed.podbean.com/tamilstoriesforkids/feed.xml",
        ),
        show(
            id = "kadhai-ketkum-neram",
            title = "கதை கேட்கும் நேரம்",
            url = "https://feeds.buzzsprout.com/1143884",
        ),
    )

    private fun show(id: String, title: String, url: String) =
        PodcastShow(id = id, title = title, feedUrl = url, builtIn = true)
}
