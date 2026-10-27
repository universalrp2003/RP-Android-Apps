# Tamil Podcast Player (தமிழ் போட்காஸ்ட் பிளேயர்)

Plays Tamil podcast shows, online or offline, and keeps playing with the screen off.

Built as part of the RP Ideas suite. Package `com.universalrp.podcast`, version 1.2.

## What it does

- **Four Tamil shows are built in**, so the app is useful the moment it opens. Every one of them is
  a public RSS feed that publishes stories for a family to listen to together: Panchatantra, Tenali
  Raman, Thirukkural, the Hosur Thaatha stories a grandfather tells, the puranas, the Ramayanam and
  Tamil folk tales. Nothing is bundled: no audio, no cached episode.
- **Any RSS feed can be added** by pasting its URL.
- **Playback continues with the screen off**, via a partial wake lock and a foreground service typed
  `mediaPlayback`.
- **The episode's real length is shown**, read from the feed's `itunes:duration` in any of
  `HH:MM:SS`, `MM:SS` or bare seconds.
- **A show that cannot be read says why.** "Unreachable" is a network problem worth retrying; "this
  link is not a podcast feed" is a link that will never work. The two need different words, and
  before the distinction existed a Cloudflare challenge page was shown as a podcast called
  "Attention Required! | Cloudflare".

## The screen

Each show carries the artwork its feed publishes, on the show card and at the top of its own episode
list. A feed with no artwork gets its first letter on a tinted tile, so the list never has empty grey
boxes. No image library was added: OkHttp was already there and this is one small bitmap.

Episodes are a numbered playlist, each row showing its length and the date it arrived, which is the
shape every player of this kind uses.

## Build

```
./gradlew :podcast:testDebugUnitTest :podcast:assembleRelease
```

ARM only, `minSdk` 26, `targetSdk` 36.

## Dependencies

None beyond what the repository already carries. Playback is `android.media.MediaPlayer`, which is in
the platform and streams an HTTP URL directly. Adding ExoPlayer would be several megabytes and one
more dependency that can fail to resolve.

## The RSS reader

`RssReader.kt` is hand-rolled, for one reason: this app's parsing has to be testable on the JVM.
Android's `XmlPullParser` lives in the platform stub, so a test that parses anything fails with "not
mocked". RSS 2.0 is a small, regular subset of XML, so the title, enclosure URL, image, duration and
date are extracted with string operations.

It deliberately does **not** handle namespaces beyond `itunes`, or entities beyond the five predefined
ones. CDATA is unwrapped, because a podcast puts a description in it more often than not.

`looksLikeFeed` is the guard that matters most. A host that blocks a request answers with HTML, and
HTML has a `<title>`, so a parser that only looks for titles will happily read a block page as a
show. A body has to declare an `<rss>`, `<rdf>` or `<feed>` root and carry at least one `<item>`
before anything is read out of it, and the fetch sends a `User-Agent` and `Accept` header because
that is the ordinary thing a feed reader does. A login page, a challenge page and a mistyped link all
land on the same honest message.

Dates are parsed from RFC 822 by hand rather than with `SimpleDateFormat`, which is not safe to call
from a test and from a service at once. The trailing `+HHMM` offset is honoured, because a podcast
published at 12:00 +0530 is the same instant as 06:30 UTC, and ignoring it puts every episode from a
non-UTC publisher hours out of order.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | to fetch feeds and stream episodes |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | to keep playing with the screen off |
| `POST_NOTIFICATIONS` | the player notification |
| `WAKE_LOCK` | held while an episode plays |

No location, contacts, storage, camera, phone state, advertising identifier or accounts.

## Status

Versions 1.0 through 1.1.1 were installed on a phone by the owner, and it was his installs that found
the fabricated feeds, the wedged screen, the dead back button and the Cloudflare block page.
**This 1.2 build has not been installed on a phone by anyone**, so it has had no device test. The RSS
reader, the duration and date parsers, the episode cap and the store's rules are covered by unit
tests. Whether a real Tamil feed parses, whether streaming survives a network change, and whether
playback continues through a full night with the screen off are not.
