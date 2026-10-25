# Live Guard 2.72 — Silent place by day, and voice waits visibly

**Version:** 2.72 · **version code 77** · **App:** Live Guard (CleanSweep)

This release does three things: silent-place slots can now be limited to specific days of the week, a
sustained HD-voice outage is visible instead of looking like plain silence, and the dead system
text-to-speech code that 2.71 made unreachable has been removed from the announcer.

No new dangerous permissions. `POST_NOTIFICATIONS` was already declared and is unchanged; the new
voice channel adds no permission of its own.

## 1. Silent place by day of week (A1)

The office / working-people profile still has one on/off switch and up to six start→stop hour slots,
but each slot now also names the days it applies on. A real pattern — "Mon–Fri 9 AM–6 PM, Sat
10 AM–2 PM" — is two slots instead of five repeated ones.

- Each slot shows a row of seven day chips, Monday first. Tapping a day toggles it.
- A slot that names no days is impossible: removing the last one is refused, because switching the
  whole profile off already means "never".
- The line under each slot says which days it covers ("Every day", "Monday to Friday", or the list
  for a scattered selection) and whether that includes today.
- The card no longer claims voice is paused when the profile is on but no slot covers today; it says
  "Silent place is on, but no slot covers today." instead.
- Slots saved by an earlier version keep working: the old format carried no day, so it is read as
  *every* day, and the default slot is now Mon–Fri 9–18.

## 2. Sustained HD-voice outage is visible (A2)

The HD neural voice is the only voice Live Guard has. When it cannot be reached there is nothing to
fall back to, and silence alone looks identical to "nothing happened" — which is exactly what the
owner must not be left to guess at.

- A quiet, no-badge notification card appears after the HD voice has been unreachable for 30
  seconds. A short network blip never reaches it; only a sustained outage does.
- The card says the announcement was not spoken and was not switched to another voice, and that it
  will play when the network returns.
- The card is cancelled the moment a clip actually starts playing, and also when the owner stops
  voice from any screen.
- The Voice screen shows the same "HD voice is waiting for the internet; announcements are paused,
  not dropped" line as soon as the stage says so, without waiting for the notification.

## 3. Dead system-voice code removed (C3)

2.71 removed the built-in phone voice. This release removes the code that used to drive it, so the
file now describes what actually happens rather than carrying a second, unreachable path:

- `installedAnswerVoices`, `choiceToken`, `previewAnswerVoice`, `saveAnswerVoice`, `voices`,
  `startUtterance`, `voiceError`, `ensureEngine`, `failInitialization` and `canSpeak` are gone.
- The `TextToSpeech`, `UtteranceProgressListener`, `Voice` and `java.util.Locale` imports, the
  `tts`, `ready`, `initializing`, `generation`, `systemVoiceCursor`, `previewedChoices` and
  `TAMIL` fields, and the unused `retried` / `attemptId` request fields are gone with them.
- `tamilVoice` stays, and now means what it says: the last announcement actually spoke Tamil.

`InstalledAnswerVoice` is kept because `AnswerVoicePolicyTest` still covers it.

## Not verified

The exact bytes in this release were not installed on a phone before publication: no device test was
run for 2.72. Compilation, the APK symbol contract and all local verifiers pass in CI. HD voice
delivery, the notification card appearing after a real outage, and the day chips rendering on a
narrow screen still need a physical-device check.
