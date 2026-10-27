# Tamil Podcast Player (கதை சொல்லி)

**Current stable release:** 1.3 / code 5

Package `com.universalrp.podcast`. Android 8.0 / API 26 or later.

A Tamil-first story player with persistent transport controls and saved-position resume.

## Unreleased candidate — not in the APK linked below

- Real episode downloads and a Downloads library: persistent system transfers, default Wi-Fi-only requests, progress, stored sizes, retry, cancellation/deletion and offline-only playback without streaming fallback.
- Twelve published RSS sources across news/world, history, literature, science/space, learning and children's stories; category filters and show search. [Source evidence](../docs/podcast-feed-evidence.md). Live validation is required; this is not every Tamil podcast on the internet.
- Package-install permission and update APK provider removed. About opens official releases in the browser instead of in-app APK download/installation.
- **Play Protect report unresolved. Do not bypass its warning.** No new stable APK has been published. [Investigation, limitations and device checklist](../docs/2026-10-27-device-follow-up.md).

## Published 1.3 features

- **Play, pause and seek:** A persistent player with elapsed time, duration, a draggable seek bar and fifteen-second skip buttons.
- **Continue listening:** Restores your last episode and saved position. Progress is saved every five seconds and on pause/stop.
- **Five show playlists:** Tamil and English show names, publisher cover art, episode search and thirty-at-a-time browsing, up to 300 episodes per feed.
- **Real media session:** Lock-screen/headset controls, audio-focus handling and pause when headphones disconnect.
- **Help and updates:** Built-in Tamil/English guide, About, sister-app links and verified APK updates with Android confirmation.

## Permissions and privacy

No account, ads, analytics, microphone, contacts or storage access. Streams publisher feeds/audio/artwork; the optional updater contacts official GitHub releases. Notification permission is requested when first playing on Android 13+; denial does not block listening.

## Release and testing

[Official release](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/tamil-podcast-v1.3) - [APK](https://github.com/universalrp2003/RP-Android-Apps/releases/download/tamil-podcast-v1.3/Tamil-Podcast-Player-v1.3.apk) - [release notes](../releases/tamil-podcast-v1.3.md).

Published APK: 11,950,127 bytes; SHA-256 `15770ec6cfcf671b81cb886bceff7d2b9f3ea620d23f795145f7940b5075bb3e`.

At publication, 1.3 had no physical-device test. The owner subsequently reported working basic behavior and a Play Protect block; not every scenario has been tested. CI verified all five RSS feeds and sampled audio responses, not full playback or every story. Publisher content and availability can change. No offline queue, playback-speed control or sleep timer.

## தமிழ்

கதைகளைத் தேர்ந்தெடுத்துக் கேளுங்கள். இயக்கக் கட்டுப்பாடுகள் திரையின் கீழே எப்போதும் இருக்கும்.

இயக்கம், இடைநிறுத்தம், இழுத்துத் தேடும் பட்டை, நேரம், நீளம், பதினைந்து நொடிகள் முன்பின் நகர்த்தல் உள்ளன. தொடர்ந்து கேள் பொத்தான் சேமித்த இடத்திலிருந்து தொடங்கும். ஐந்து ஊட்டங்களுக்கும் தமிழ் மற்றும் ஆங்கிலப் பெயர்கள் உள்ளன. பகுதிகளைத் தேடலாம்; மேலும் பகுதிகள் பொத்தானால் பட்டியலை விரிவாக்கலாம்.

இந்தப் பதிப்பு தொலைபேசியில் நிறுவிச் சோதிக்கப்படவில்லை. ஊட்டங்களின் இணைப்புகள் சரிபார்க்கப்பட்டன; ஒலி முழுவதையும் கேட்கும் சோதனை நடக்கவில்லை. உள்ளடக்கமும் இணையக் கிடைப்பும் வெளியீட்டாளரின் கட்டுப்பாட்டில் உள்ளன. இணையம் தேவை.
