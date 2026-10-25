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
  *every* day. The default slot is every day, 9 AM–6 PM — it has always covered all seven days, so
  an owner who never touches the day chips keeps exactly the behaviour 2.71 gave them.

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

## Device test

Installed on a phone through Play Protect on 25 October 2026, alongside the other three apps
published that day. Play Protect reported "This app looks safe" and the install completed. The
silent-place day chips render as designed: seven chips, Monday first, all seven selected, with
"9:00 AM - 6:00 PM" and "+ another slot" underneath. That is the point of this release confirmed on
a real screen, and it also settles the default: the out-of-the-box slot is every day, 9 AM to 6 PM.
An earlier draft of this note said Monday to Friday. That was wrong, the code was right all along,
and this note now says what the code does.

Still to check on a phone: the notification card appearing after a real 30-second HD outage, the
Voice screen waiting line during one, and the day chips on a narrow screen with a scattered day
selection. CI compilation, the APK symbol contract and all local verifiers pass.

## தமிழ்

- **அமைதி இட slot-கள் இப்போது நாட்களைக் குறிக்கும்.** ஒவ்வொரு தொடக்கம்→நிறுத்தம் slot-ம்
  குறிப்பிட்ட வார நாட்களுக்கு மட்டும் வரம்பிடலாம். "திங்கள்–வெள்ளி 9 AM–6 PM, சனி
  10 AM–2 PM" என்பது ஐந்து slot-களை மீண்டும் சேர்க்காமல் இரண்டு slot-ஆக இருக்கும்.
- **ஒவ்வொரு slot-ம் ஏழு நாள் chip-களைக் காட்டும், திங்கள் முதலில்.** ஒரு நாளைத் தட்டினால்
  அது on/off ஆகும்.
- **நாட்கள் இல்லாத slot சாத்தியமில்லை:** கடைசி நாளை அழிப்பது மறுக்கப்படும், ஏனெனில்
  முழு profile-ஐ off செய்வது ஏற்கனவே "ஒருபோதும் இல்லை" என்பதைக் குறிக்கிறது.
- **ஒவ்வொரு slot-ம் கீழும் எந்த நாட்களை உள்ளடக்கியது என்பது தெரியும்** ("ஒவ்வொரு நாளும்",
  "திங்கள் முதல் வெள்ளி வரை", அல்லது சிதறிய தேர்வுக்கான பட்டியல்) மற்றும் இன்று அதில்
  உள்ளதா என்பது.
- **நாட்கள் இல்லாத slot காரணமாக profile on ஆக இருக்கும்போது குரல் நிறுத்தப்பட்டது என்பது
  இனி காட்டப்படாது.** "அமைதி இடம் on, ஆனால் இன்று எந்த slot-ம் இல்லை" என்று சொல்லும்.
- **HD குரல் நீண்ட நேரம் கிடைக்கவில்லை என்றால் அது தெரியும்.** குரல் திரை HD குரலுக்குக்
  காத்திருக்கிறது என்று சொல்லி மீண்டும் முயற்சிக்கும். மென்மையான மாற்று குரல் இல்லை,
  ஒலியின்றி அமைதியாகவும் இல்லை.
- **பழைய version-ல் சேமித்த slot-கள் வேலை செய்யும்:** பழைய format-ல் நாள் இல்லை, அதனால்
  *ஒவ்வொரு நாளும்* என்றே படிக்கப்படும். **இயல்புநிலை slot ஒவ்வொரு நாளும் 9 AM–6 PM** —
  எப்போதும் ஏழு நாட்களையும் உள்ளடக்கியது, அதனால் நாள் chip-ஐ தொடாத உரிமையாளருக்கு 2.71
  வழங்கிய நடத்தையே கிடைக்கிறது.
- **ஃபோனில் நிறுவப்பட்டு சோதிக்கப்பட்டது (25 அக்டோபர் 2026).** Play Protect "இந்த செயலி
  பாதுகாப்பானது" என்று தெரிவித்தது மற்றும் நிறுவல் முடிந்தது. அமைதி இட நாள் chip-கள்
  வடிவமைப்பபடி ஏழு chip-களாக (திங்கள் முதலில், ஏழும் தேர்வு செய்யப்பட்டவை),
  "9:00 AM - 6:00 PM" மற்றும் "+ another slot" உரையுடன் காட்டப்படுகின்றன. இதனால்
  இயல்புநிலை slot உண்மையில் ஒவ்வொரு நாளும் 9 AM முதல் 6 PM வரை என்பது உறுதியாகிறது. இந்த
  குறிப்பின் முந்தைய வடிவத்தில் "திங்கள் முதல் வெள்ளி" என்று தவறாகக் கூறப்பட்டது; code
  எப்போதும் சரியாகவே இருந்தது.
- **இன்னும் ஃபோனில் சோதிக்க வேண்டியவை:** உண்மையான 30 வினாடி HD செயலிழப்புக்குப் பிறகு
  அறிவிப்பு அட்டை தெரிவது, குரல் திரையின் காத்திருப்பு வரி, மற்றும் சிதறிய நாள்
  தேர்வுடன் chip-கள் குறுகிய திரையில் எப்படி இருக்கும் என்பது.
