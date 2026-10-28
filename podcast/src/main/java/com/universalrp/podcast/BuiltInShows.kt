package com.universalrp.podcast

/** Published RSS addresses, not constructed guesses. See docs/podcast-feed-evidence.md.
 * Directory discovery is not a playback test: every feed must pass the live CI gate.
 */
object BuiltInShows {
    fun all(): List<PodcastShow> = listOf(
        show("pat-me", "குற்றச் சம்பவங்கள் · பெரியவர்களுக்கு / PAT Me!!!", "https://anchor.fm/s/20b2a208/podcast/rss"),
        show("thriller-tamizha", "திகில் கதைகள் · பெரியவர்களுக்கு / Thriller Tamizha", "https://anchor.fm/s/65ed06d8/podcast/rss"),
        show("deep-talks", "மர்மத் திரில்லர் / Deep Talks (mixed topics)", "https://anchor.fm/s/219c0a60/podcast/rss"),
        show("wrong-play", "காதல் கதைகள் / Wrong Play (mixed topics)", "https://anchor.fm/s/5d28c848/podcast/rss"),
        show("under-the-tree", "மரத்தடியில் சிறுகதைகள் / Tamil Short Stories — Under the tree", "https://anchor.fm/s/1feab1a8/podcast/rss"),
        show("tamil-science-fiction", "தமிழ் அறிவியல் புனைகதை / Tamil Science Fiction", "https://anchor.fm/s/11120d660/podcast/rss"),
        show("sbs-tamil", "எஸ்பிஎஸ் தமிழ் / SBS Tamil (Australia)", "https://sbs-ondemand.streamguys1.com/sbs-tamil/"),
        show("tamil-varalaru", "தமிழ் வரலாறு / Tamil Varalaru", "https://anchor.fm/s/4e1c054/podcast/rss"),
        show("tamil-lit-stories", "தமிழ் இலக்கியக் கதைகள் / Tamil Lit Stories", "https://anchor.fm/s/2e5b1c14/podcast/rss"),
        show("kalakki", "கலக்கி / Kalakki — Science", "https://anchor.fm/s/5662671c/podcast/rss"),
        show("space-news-tamil", "விண்வெளிச் செய்திகள் / Space News Tamil", "https://anchor.fm/s/1140bcc4/podcast/rss"),
        show("bharath-tamil", "பாரத் தமிழ் / Bharath Tamil Podcast", "https://anchor.fm/s/d987ea4/podcast/rss"),
        show("kadhai-neram", "கதை நேரம் / Kadhai Neram", "https://anchor.fm/s/57e45e88/podcast/rss"),
        show("pesum-kadhai", "பேசும் கதை / Pesum Kadhai", "https://anchor.fm/s/289076a8/podcast/rss"),
        show("tamil-story-for-kids", "குழந்தைகளுக்கான தமிழ்க் கதைகள் / Tamil Story For Kids", "https://anchor.fm/s/4210349c/podcast/rss"),
        show("siruvar-kadhaigal", "சிறுவர் கதைகள் / Siruvar Kadhaigal", "https://anchor.fm/s/aee7640/podcast/rss"),
        show("kathai-arasan", "கதை அரசன் / Kathai Arasan", "https://anchor.fm/s/47a2df2c/podcast/rss"),
    )

    fun category(id: String): PodcastCategory = when (id) {
        "pat-me" -> PodcastCategory.CRIME
        "thriller-tamizha" -> PodcastCategory.HORROR
        "deep-talks" -> PodcastCategory.THRILLER
        "wrong-play" -> PodcastCategory.LOVE
        "under-the-tree" -> PodcastCategory.STORIES
        "tamil-science-fiction" -> PodcastCategory.SCIENCE_FICTION
        "sbs-tamil" -> PodcastCategory.NEWS
        "tamil-varalaru" -> PodcastCategory.HISTORY
        "tamil-lit-stories" -> PodcastCategory.LITERATURE
        "kalakki", "space-news-tamil" -> PodcastCategory.SCIENCE
        "bharath-tamil" -> PodcastCategory.LEARNING
        "kadhai-neram", "pesum-kadhai", "tamil-story-for-kids", "siruvar-kadhaigal", "kathai-arasan" -> PodcastCategory.CHILDREN
        else -> PodcastCategory.OTHER
    }

    private fun show(id: String, title: String, url: String) =
        PodcastShow(id = id, title = title, feedUrl = url, builtIn = true)
}

enum class PodcastCategory { NEWS, HISTORY, LITERATURE, SCIENCE, LEARNING, CHILDREN, LOVE, STORIES, SCIENCE_FICTION, CRIME, HORROR, THRILLER, OTHER }

val PodcastCategory.label: String
    get() = when (this) {
        PodcastCategory.CRIME -> "குற்றச் சம்பவங்கள் / Crime"
        PodcastCategory.HORROR -> "திகில் கதைகள் / Horror"
        PodcastCategory.THRILLER -> "மர்மம் · திரில்லர் / Mystery & Thriller"
        PodcastCategory.LOVE -> "காதல் கதைகள் / Love stories"
        PodcastCategory.STORIES -> "சிறுகதைகள் / Short stories"
        PodcastCategory.SCIENCE_FICTION -> "அறிவியல் புனைகதைகள் / Science fiction"
        PodcastCategory.NEWS -> "செய்திகள் · உலகம் / News & World"
        PodcastCategory.HISTORY -> "வரலாறு / History"
        PodcastCategory.LITERATURE -> "இலக்கியம் / Literature"
        PodcastCategory.SCIENCE -> "அறிவியல் · விண்வெளி / Science & Space"
        PodcastCategory.LEARNING -> "கற்றல் · சுயமுன்னேற்றம் / Learning"
        PodcastCategory.CHILDREN -> "சிறுவர் கதைகள் / Children's stories"
        PodcastCategory.OTHER -> "சேர்த்த நிகழ்ச்சிகள் / Added feeds"
    }
