# Tamil Podcast Player (தமிழ் போட்காஸ்ட் பிளேயர்)

Plays Tamil podcast shows, online or offline, and keeps playing with the screen off.

Built as part of the RP Ideas suite. Package `com.universalrp.podcast`, version 1.0.

## What it does

- **Four Tamil shows are built in**, so the app is useful the moment it opens. They are public RSS feeds chosen because they publish in Tamil. Nothing is bundled: no audio, no cached episode.
- **Any RSS feed can be added** by pasting its URL.
- **Playback continues with the screen off**, via a partial wake lock and a foreground service typed `mediaPlayback`.
- **The episode's real length is shown**, read from the feed's `itunes:duration` in any of `HH:MM:SS`, `MM:SS` or bare seconds.

## Build

```
./gradlew :podcast:testDebugUnitTest :podcast:assembleRelease
```

ARM only, `minSdk` 26, `targetSdk` 36.

## Dependencies

None beyond what the repository already carries. Playback is `android.media.MediaPlayer`, which is in the platform and streams an HTTP URL directly. Adding ExoPlayer would be several megabytes and one more dependency that can fail to resolve.

## The RSS reader

`RssReader.kt` is hand-rolled, for one reason: this app's parsing has to be testable on the JVM. Android's `XmlPullParser` lives in the platform stub, so a test that parses anything fails with "not mocked". RSS 2.0 is a small, regular subset of XML, so the title, enclosure URL, duration and date are extracted with string operations.

It deliberately does **not** handle namespaces, entities beyond the five predefined ones, or Atom. A feed that needs any of those shows as empty rather than as wrong.

Dates are parsed from RFC 822 by hand rather than with `SimpleDateFormat`, which is not safe to call from a test and from a service at once.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | to fetch feeds and stream episodes |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | to keep playing with the screen off |
| `POST_NOTIFICATIONS` | the player notification |
| `WAKE_LOCK` | held while an episode plays |

No location, contacts, storage, camera, phone state, advertising identifier or accounts.

## Status

Version 1.0 has **not been installed on a phone**, so it has had no device test. The RSS reader, the duration and date parsers, the episode cap and the store's rules are covered by unit tests. Whether a real Tamil feed parses, whether streaming survives a network change, and whether playback continues through a full night with the screen off are not.
