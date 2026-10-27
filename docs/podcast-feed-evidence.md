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


## Expanded candidate catalogue — unpublished

The new source list contains eleven RSS addresses across six categories. The six included additions below were discovered from published provider/directory links on 27 October 2026, not inferred from show titles. Their live CI results are recorded separately after the run completes.

- News/world and Australian Tamil diaspora: **SBS Tamil** — `https://sbs-ondemand.streamguys1.com/sbs-tamil/`. [Official SBS show](https://www.sbs.com.au/language/tamil/en/podcast/sbs-tamil), [published RSS address](https://www.podchaser.com/podcasts/sbs-tamil-sbs-41427).
- History: **Tamil Varalaru** — `https://anchor.fm/s/4e1c054/podcast/rss`. [Published feed and producer description](https://podnews.net/podcast/itp6). This includes an older archive; inclusion does not promise frequent new episodes.
- Literature: **Tamil Lit Stories**, Jeya Maran — `https://anchor.fm/s/2e5b1c14/podcast/rss`. [Published RSS address](https://radiopublic.com/tamil-lit-stories-6r3V5R), [directory corroboration](https://www.podchaser.com/podcasts/tamil-lit-stories-1375619).
- Science: **Kalakki — The Tamil podcast** — `https://anchor.fm/s/5662671c/podcast/rss`. [Published RSS and Science category](https://podnews.net/podcast/irsl6).
- Science/space: **Space News Tamil**, Abdul — `https://anchor.fm/s/1140bcc4/podcast/rss`. [Published RSS and Science/Astronomy description](https://www.podchaser.com/podcasts/space-news-tamil-2074547). An older archive, not a promise of current daily news.
- Learning/self-development: **Bharath Tamil Podcast**, Leema Arockiaraj — `https://anchor.fm/s/d987ea4/podcast/rss`. [Published RSS address](https://www.podbean.com/podcast-detail/gswh3-ada16/Bharath-Tamil-Podcast), [directory corroboration](https://rephonic.com/podcasts/bharath-tamil-podcast). Contains mixed book readings, reflection and stories; categories are navigation labels, not episode-level age ratings.

The original five story shows remain together in Children's stories. Added user feeds have their own category. All episodes retain publisher titles and audio. News, analysis and historical topics can concern adults; no claim is made that the entire catalogue is child-safe.

The extractor now identifies literal URLs in actual `show(id, title, feedUrl)` entries rather than assuming every publisher uses Anchor's URL format. It still rejects zero matches, duplicate URLs and any extra source URL not included in verification. Tests cover BBC/SBS format acceptance and deliberately reintroduce an unmatched URL. Live verification attempts every feed and fails the gate if any fails; a failure no longer hides the status of later sources.


## Researched but excluded from the candidate

- **Not shipped.** News/world: **உலகின் கதை / BBC Tamil** — `https://podcasts.files.bbci.co.uk/p0l97701.rss`. [Official BBC podcast page](https://www.bbc.com/tamil/podcasts/p0l97701) and its indexed webFeed metadata publish this address. International weekly analysis, not a children's story feed.

Candidate run `40479496687` compiled the podcast, passed all fifty JVM tests and inspected its actual APK successfully, but rejected this BBC feed with **Missing channel title or playable HTTPS enclosures**. The gate tried all twelve researched feeds, not only the first failure. BBC is removed rather than counted as playable, converted to a guessed alternate URL, or allowed through by weakening the HTTPS requirement. The other eleven are rechecked in the final candidate run. Directory support and an official podcast page are not substitutes for a passing playback-source check.


## Final live candidate result

[Run 40480048906](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40480048906), source `5eeafc96e26f6c493dcf0f789da0b079820967a4`, passed all eleven included RSS feeds and first-audio-prefix smoke checks. All three candidate app build/test jobs passed, including the podcast. The BBC source above remains excluded. This does not establish whole-episode playback, episode-level suitability, worldwide availability or offline behavior on a phone.

## Crime, horror and mystery expansion

The proposed 1.6 catalogue adds PAT Me!!! (`20b2a208`), Thriller Tamizha (`65ed06d8`) and Deep Talks (`219c0a60`) from discovered public RSS addresses. Exact URLs, publisher attribution, mature-theme cautions and rights limitations are in the [device/catalogue follow-up](2026-10-27-pulseeq-device-review.md). This is seventeen sources, not a claim to index every public Tamil story. Only a passing new CI live RSS/audio-prefix check establishes current reachability; a directory listing alone does not. No adaptation or redistribution licence is inferred.

The new candidate run [40500082478](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40500082478) passed all seventeen live RSS/HTTPS audio-prefix checks at source `eba93da7c24ba37275f3d964f5b7f3b370ec8fdd`. This confirms sampled reachability, not full listening, rights clearance or age suitability.
