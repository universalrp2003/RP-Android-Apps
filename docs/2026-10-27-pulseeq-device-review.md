# PulseEQ block and Tamil story catalogue follow-up

## Owner phone report

The owner supplied private screenshots and said: Only pulseeq not installed others are perfect.

- Podcast 1.5 / code 7: upgrade dialog, Play Protect saying This app looks safe, love-story catalogue and playback screenshots. Continuous off and on are shown, including different episodes near their end and then a following episode at its start. The owner's overall confirmation covers successful operation; static screenshots alone do not establish every transition or background/offline test.
- Ramesh Radio 1.27 / code 27: upgrade dialog, accepted Play Protect scan, seven local tracks, playback and seek progress, Tamil/English controls and Random/Repeat all enabled. The owner confirms it works. No independent on-phone APK hash was supplied.
- Vasanthi AI / என்னைச் சுற்றி 1.1.5 / code 8: accepted scan, upgrade dialog and the correct Tamil home-screen name. The screenshot has listening stopped with empty history; do not call that independent proof of speech/sound recognition. The owner reports this app is perfect.
- **PulseEQ 1.10 / code 11 was blocked, not installed.** The warning says Harmful app blocked / This app may be harmful. Do not describe this as a routine unknown-source prompt or ask the owner to use Install anyway or disable protection.

These files came from the instructed candidate run 40495158373, source `4318a7ba23deb0fb3d08820817de6c96ca1daa0c`; the PulseEQ artifact ID is 12729728799. The screenshots do not independently establish the APK hash. Original screenshots remain private.

## PulseEQ review and limited hardening

Source review found a reachable in-app APK updater in About, `REQUEST_INSTALL_PACKAGES`, and an update APK FileProvider. The app also retained INTERNET for the updater. This is evidence of unnecessary capability for an offline local-audio player, **not evidence that any one item caused Google's warning**.

PulseEQ 1.11 / code 12 removes those unused network/install permissions and the update FileProvider/resource, and opens official releases in the external browser. Browser intents need no app INTERNET permission. The local document picker/queue, existing DSP, foreground playback service, cooperating audio-session support, battery settings, Random and Repeat behavior are retained. No package rename, signing-key rotation, scanner bypass, downloaded executable code or hidden alternate installer is introduced.

The source regression gate covers launcher, podcast and PulseEQ; actual candidate APK inspection must reject installer permission/provider reintroduction and PulseEQ app network permission. Unit tests must reject the old PulseEQ updater entry. Other apps retain their existing update behavior.

The warning's cause remains unresolved. A successful compile, hash/signature verification, or removal of unused permissions is not Play Protect/security clearance. If the new candidate is also blocked, stop and preserve the exact receipt for investigation or the official developer appeal process; do not repeatedly change identity/signing to evade classification. The separate existing signing-trust concern is not declared resolved. No appeal has been submitted.

## Additional public Tamil story sources

Keep the fourteen previously checked feeds. Add only the following discovered addresses, subject to the unchanged live RSS and HTTPS audio-prefix gate:

- Crime: PAT Me!!! Tamil Podcast, Monisha Veeranan, `https://anchor.fm/s/20b2a208/podcast/rss`. Public feed includes true-crime narratives and mixed topics, including an older archive. Discovery: [2](https://anchor.fm/s/20b2a208/podcast/rss).
- Horror: Ghost Stories Tamil / Thriller Tamizha, Waran MahaDev, `https://anchor.fm/s/65ed06d8/podcast/rss`. Directory explicitly supplies this webFeed. Discovery: [9](https://podtail.com/en/podcast/tamil-story-from-thriller-tamizha-tamil-thriller-p/).
- Mystery/thriller: Deep Talks - Tamil Audiobooks, `https://anchor.fm/s/219c0a60/podcast/rss`. Its public feed includes crime/mystery/thriller audiobooks and other topics. Discovery: [1](https://anchor.fm/s/219c0a60/podcast/rss).

These expand the catalogue to seventeen sources and twelve populated categories; Added feeds remains a separate fallback. Do not conflate science fiction with science education or these adult topics with children's stories. Crime/horror may include violence, distressing themes and publisher allegations or supernatural claims that have not been independently verified. Public access is not a redistribution licence, publisher authorisation proof, permanent availability or a claim that every episode is free/appropriate. No paywall bypass, scraping of premium audio or invented RSS URLs.

Podcast 1.6 / code 8 distinguishes the expanded catalogue from the owner-approved 1.5 artifact. Continuous/offline playback behavior is unchanged. New candidate APKs still require owner checks before publication. The approved Radio/Vasanthi files remain recorded above; their successful scans do not clear PulseEQ.

## New candidate validation

[Candidate run 40500082478](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40500082478) completed successfully at source `eba93da7c24ba37275f3d964f5b7f3b370ec8fdd`. All five modules compiled and passed their real JVM suites/identity receipts. Actual merged APK capability inspection passed for podcast, launcher and PulseEQ, including no app INTERNET permission in PulseEQ. The unchanged live gate passed every one of the seventeen RSS feeds and sampled HTTPS audio prefixes. This does not prove full-episode playback, licence, content suitability, permanent availability or Play Protect acceptance.

- [Podcast 1.6 / code 8, artifact 12732850437](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40500082478/artifacts/12732850437).
- [PulseEQ 1.11 / code 12, artifact 12732476730](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40500082478/artifacts/12732476730).

Each artifact is a ZIP containing the actual APK, source/identity/hash receipt and JUnit evidence. ZIP size is not the APK size. These two new APKs have not been installed on the owner's phone and are not stable publications. Other modules were rebuilt because shared inputs changed; those rebuilt bytes are not substitutes for the older phone-approved Radio/Vasanthi artifacts. Retain original approvals only for their original files. Broader run 40500082577 compiled the apps and passed the real Cleaner/Vocal suites, then correctly exposed a stale separate APK rule requiring PulseEQ INTERNET for the retired updater. Replace that rule with actual browser-policy APK inspection and merged-manifest network/install/provider rejection; a regression test must reject restoration of the old rule. This is a gate migration, not a reason to restore unnecessary app permissions. Await the replacement broad run before claiming full success.
