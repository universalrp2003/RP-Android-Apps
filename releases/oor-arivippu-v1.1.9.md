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

## தமிழ் சுருக்கம்

- **அறிவிப்பு பட்டியல் இயங்கியது என்ன ஆனது என்பதை பதிவு செய்கிறது.** இதுவரை செயலி *முயற்சித்தது*
  என்பது மட்டும் பதிவாகியது — பயணம் முடிந்த பிறகு அது பயன்படாத ஒன்று. உண்மையான கேள்வி
  "குரல் உண்மையில் வெளியே வந்ததா?".
- **ஆறு வகை முடிவுகள், ஒவ்வொரு வரிசையிலும் தெரியும்:** பேசப்பட்டது · அழைப்புக்காகக் காத்திருந்தது ·
  இணையம் இல்லை · GPS அணைந்திருந்தது · குரல் கிடைக்கவில்லை · பேசப்படவில்லை.
- **பட்டியலுக்கு மேலே ஒரு சுருக்க வரி:** "M அறிவிப்புகளில் N பேசப்பட்டது", மற்றும் பயணத்தில்
  எத்தனை வேறுபட்ட ஊர்கள் உண்டு — வரிசை வரிசையாக பார்க்காமல் ஒரே பார்வையில் சரிபார்க்கலாம்.
- **செயலி மூடினாலும் பட்டியல் அழியாது.** சாதனத்தில் சேமிக்கப்படுகிறது; 40 அறிவிப்புகள் வரை
  vaikkappadum.
- **காலி நிலை இப்போது உண்மையைச் சொல்கிறது.** "இன்னும் எந்த ஊர் அறிவிப்பும் செய்யப்படவில்லை"
  என்று மட்டும் இருக்காது; பயணத்தில் எதுவும் அறிவிக்கப்படவில்லை என்றால் பட்டியல் காலியாக
  இருக்கும் — குரல் வழங்கப்படவில்லை என்பது இதன் பொருள்.
- **குரல் சோதனை (Test Voice) பட்டியலில் இல்லை.** அது குரல் வேலை செய்கிறது என்பதை நிரூபிக்கும்,
  கண்காணிக்கப்படும் அறிவிப்பு அல்ல.

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
