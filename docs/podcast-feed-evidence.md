# Built-in podcast feed evidence - 27 October 2026

A directory listing is evidence of a published address, not proof that the phone can play it. No feed URL below was invented from a show name.

- Kadhai Neram: `https://anchor.fm/s/57e45e88/podcast/rss`. The owner reported it loading on his phone. [Directory](https://rephonic.com/podcasts/ktai-neerm-tamil-bedtime-stories).
- Pesum Kadhai: `https://anchor.fm/s/289076a8/podcast/rss`. The owner reported it loading on his phone. [Directory](https://www.listennotes.com/podcasts/pesum-kadhai-tamil-audio-stories-for-kids-nYUlVwvodcL/).
- Tamil Story For Kids: `https://anchor.fm/s/4210349c/podcast/rss`. [Published RSS address](https://radiopublic.com/tamil-story-for-kids-Wd002j). The web retrieval tool returned the feed title, children's-story description and episode content in this session.
- Siruvar Kadhaigal: `https://anchor.fm/s/aee7640/podcast/rss`. [Published RSS address](https://www.podchaser.com/podcasts/-2467669). The web retrieval tool returned the Tamil title, children's-story description and episode content in this session.
- Kathai Arasan: `https://anchor.fm/s/47a2df2c/podcast/rss`. [Published RSS address](https://www.podchaser.com/podcasts/tamil-stories-kathai-arasan-a-1762151). The web retrieval tool returned the Tamil/English title, bedtime-story description and episode content in this session.

The failed Podbean and Buzzsprout built-ins reported by the owner were removed, not silently retried under fabricated replacements. Their removal does not prevent the user adding any valid HTTP/HTTPS RSS URL manually.

`tools/verify_podcast_feeds.py` runs in the podcast candidate/release workflow. It refuses an empty URL match set, unrecognised/duplicate URLs, non-RSS responses, missing titles, missing playable HTTPS enclosures, and empty/HTML responses from the first audio enclosure of each show. It streams a sample of up to twenty feed items and reads only a small audio prefix, not an entire episode. Large multi-year RSS archives are not rejected just because the full archive exceeds eight megabytes. The app itself reads at most eight megabytes or three hundred complete items, whichever comes first. Gate tests deliberately supply HTML, empty feeds and unmatched source to prove these failures. Network access to publishers is unavailable in the coding sandbox; CI is the live-network gate.

This is not proof of every episode's suitability, a full playback test, or physical-device testing. Publishers can change content, URLs, redirects and availability at any time. The app labels errors and keeps Back and retry available. Tamil aliases are display translations; original episode titles and publisher audio remain unmodified.
