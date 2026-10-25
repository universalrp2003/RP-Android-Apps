# Oor Arivippu 1.1.9 — The voice now tells you what actually happened

**Version:** 1.1.9 · **version code 14** · **App:** Oor Arivippu

The announcement list used to record that the app *tried* to speak. That is the one thing that
cannot be used after a trip, because the real question is "did the voice actually come out?".

No new permissions. Nothing about when, where, or in which language places are announced has
changed. This release only reports the outcome of each announcement honestly.

## What changed

**Every announcement is now recorded with its outcome.** Six possible endings, each shown on its own
row:

- **Spoken** — the voice actually started playing.
- **Held for the call** — a phone call was in progress, so it waited and then played.
- **No internet** — dropped because this announcement needs internet and there was none.
- **GPS off** — dropped because location was switched off while tracking.
- **No voice** — neither the HD cloud voice nor an on-device voice could be used.
- **Not spoken** — dropped for any other reason.

A row is written when the speech engine reports that it *started*, and the same row is corrected to
"no voice" if the engine then reports an error. One utterance, one row: a failed announcement can
never also read as delivered.

**A summary line above the list.** "N of M announcements were spoken", plus how many distinct
places were covered, so a trip can be checked at a glance instead of row by row.

**The list now survives the process dying.** It is stored on the device, so closing the app or
letting Android reclaim the service no longer wipes the record. Up to 40 announcements are kept.

**The empty state now says something useful.** It used to read "no places announced yet in this
session", which said nothing. It now tells you that if a trip announced nothing and the list is
empty, the voice was not delivered.

Voice previews are deliberately not part of the report: tapping "Test Voice" proves the voice works,
it is not a tracked announcement, and it never appears here.

## How it is built

The decision rules and the summary arithmetic are a pure object over strings and numbers
(`AnnouncementDeliveryReport`), so they are unit-tested without a device, audio focus, or a network
call. The service only decides *which* outcome applies at each point where it currently drops an
announcement; it does not decide what the numbers mean.

Storage is one line per announcement with control-character separators, so a malformed line costs
you one row rather than the whole history. Rows written by 1.1.8 carry no outcome; they all read as
spoken, because the old code only ever logged a line when it spoke.

## Not verified

The exact bytes in this release were not installed on a phone before publication: no device test
was run for 1.1.9. CI unit, lint, regression, signature and release-asset checks pass. A real trip
with a real phone call, a real dead spot, and the app being killed mid-announcement still needs a
physical-device check.
