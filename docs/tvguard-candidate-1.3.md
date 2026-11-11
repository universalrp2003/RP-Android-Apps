# TV Guard 1.3 — candidate for the Sony Bravia

**Status: built, CI-verified, not published. Waiting for the physical test.**

## The file

| | |
|---|---|
| Artifact | `tvguard-1.3-stable-signed-tested-candidate` (workflow run 42987940978) |
| APK inside | `Android-TV-Guard-1.3-signed-candidate.apk` |
| Size | 11,248,409 bytes |
| SHA-256 | `9a66703eafce69be51faa379ca3edfb2778a97861b02dfcfd6e9d6cc1f3d0e2d` |
| Package | `com.universalrp.tvguard` |
| Version | `versionName 1.3`, `versionCode 5` |
| Signing | same key as 1.2, so it installs straight over it |

Installs in place over the 1.2 you rejected; favourites, hidden apps and feature
choices are preserved by the same-key update path the emulator exercises.

## What changed, against each thing you reported

**1. The strip is at the top and is one short line.**
The position was not a placement nudge. 1.2 had saved where it was and the app
restored that saved position on every start, so any new default never applied.
The saved position now carries a schema version: an old one is dropped once and
the strip returns to the top. Moving it from Tools still works and is
remembered from then on.

The five readings are `62%`, `832 MB`, `26.0 GB`, `50.3 °C` and one
colour-coded word for internet quality. No words like "free" or "good" are
appended any more, and there is no panel behind it. Preview:
`docs/preview/tvguard-1.3-strip-preview.png`.

**2. The garbled vertical text.**
This was a real layout bug, not a rendering quirk. The cells were
`WRAP_CONTENT` inside a horizontal row: one longer value squeezed the rest until
Android wrapped them one character per line. The row now has a fixed height, the
window an explicit width, and every cell is single-line with an equal share of
the width. That is measured by tests on the real view, not assumed.

**3. Turning the status bar off disabled everything below it.**
The floating strip was tied to the in-app header switch, so turning the header
off switched the strip off with it. The strip has its own switch now and the
header switch affects only the header. The note that looked like everything was
disabled ("paused until the required parent switch is ON") was rewritten.

**4. The duplicate in-app readings row is gone.** The header is the title, the
clock and the refresh action only.

**5. AI is gone completely.** The voice package, the microphone permission, the
assistant dialog, the mic and "AI" cards on Tools, the remote mic key and the
long HOME press were all removed, with their strings and tests. The app no
longer requests the microphone at all.

**6. The per-metric switches are gone.** There is one status-pill switch. The
five readings have no individual toggles.

**7. The long-press menu is gone.** It still opened the app on your remote after
three attempts, so it was removed as you asked rather than patched again. The
tile options open with the **Menu** key on a focused tile. The policy class and
its tests are deleted.

**8. Uploaded APKs now appear in the hub.** The share screen listed only
screenshots, so an upload was invisible. It now lists files from Downloads too,
newest first, and the existing install action installs them directly.

## What CI proved, and what it did not

Proved on a disposable Android TV emulator, on this exact APK:

- 61 unit tests, including five that measure the real pill layout.
- Five D-pad/focus tests, including the Menu-key options path.
- A different-signature APK is rejected; a same-key APK updates in place and
  keeps favourites, hidden apps, feature choices and app-private files.
- The installed APK's hash matches the artifact byte for byte.

Not proved: how it looks and behaves on your Bravia. The strip's size and
colour over real video, and whether the top position clears your TV's own
status bar, can only be judged on the TV. There is no emulator screenshot in
this run: Robolectric's canvas draws nothing, so the layout is verified by
measurement, and that limit is stated rather than papered over.

## Please check on the TV

1. The strip is at the **top**, one line, five values, no background.
2. Values read `%`, `MB`, `GB`, `°C` and a colour word, with no extra text.
3. Turning the in-app status bar off leaves the strip running.
4. No in-app readings row under the header.
5. No mic, no AI switch, no per-metric switches in Tools → Expert.
6. A long press does nothing; **Menu** on a focused app tile opens the options.
7. Upload an APK through the hub page and install it from the Share tab.

Tell me which of these still fail and I will fix them before publishing.
