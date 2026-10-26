# Voice Notes 1.0

## What this app is

A standalone recorder-and-read-back app for Tamil. Tap the microphone, speak a note, hear it read
back in the HD neural voice, and keep it. There is no account, no cloud and no upload: the notes
file lives in this app's private storage and nowhere else.

It is built on the neural-voice pipeline that Live Guard 2.71 proved — the same curated Studio
Ultra-HD voices, the same caching, the same retry-and-wait behaviour — bundled into an app whose
whole job is that one pipeline.

## What changed

- New app: dictate a note in Tamil, hear it spoken back, keep it on the phone.
- The voice is chosen by the script of the note, not by the app's interface language. A Tamil note
  is read by `ta-IN-ValluvarNeural`; an English note is read by the English voice. Reading Tamil
  with the English voice produces noise rather than wrong words, so the text decides.
- Long notes are split at sentence boundaries into chunks the neural endpoint can actually return,
  and spoken in order. Splitting mid-sentence is the last resort, not the default.
- With no internet the app says the voice is waiting rather than falling back to the phone's robot
  voice or dropping the note silently — the same contract Live Guard uses.
- Permissions: microphone, for dictation, and internet, for the voice. No location, no contacts,
  no SMS, no storage, no camera.

## Device test

Not phone-installed for this release. This is a new app, and no build of it has been installed on
a phone. The note store's round-trip and the speech decisions are covered by unit tests; the
dictation flow, the neural playback and the Tamil wording on a real screen are not verified.

## Why these tests

Two failures here are invisible on a device and both are pinned by tests. A note that encodes but
does not decode is a note the owner dictated and then lost, with nothing on screen to say so. And
a Tamil note read by the English neural voice sounds like the voice service is broken, not like
the wrong voice was chosen.

## Install and test

Package name: `com.universalrp.voicenotes`. It installs beside the other apps and shares nothing
with them, so it can be removed on its own.

What to try, roughly in order of what is most likely to be wrong:

- **The first launch.** Nothing in this app has ever been run on a phone. If it crashes on open, that
   is the first thing to know.
- **Grant the microphone**, then dictate one short Tamil sentence and stop. The text should appear
   with Tamil words, not transliteration.
- **Tap the speaker on a note.** It needs internet, because the HD neural voice is a network call.
   With no internet it should say the voice is waiting rather than going silent.
- **Dictate a long note** - a paragraph, not a line. It is split into chunks and spoken in order, so
   this is where a truncation would show.
- **Switch the interface to Tamil** with the toggle and check the wording reads as Tamil.

The dictation flow, the neural playback and the Tamil wording on a real screen are the parts the
unit tests cannot reach. Everything above is what they do not cover.

## தமிழ்

தமிழில் ஒரு குறிப்பைப் பேசுங்கள், HD நரம்பியல் குரளில் திரும்பக் கேளுங்கள், தொலைபேசியிலேயே சேமிக்கவும். கணக்கு இல்லை, கிளவ் இல்லை, பதிவேற்றம் இல்லை — குறிப்புகள் இந்தச் செயலியின் தனியார் சேமிப்பிலேயே இருக்கும். குறிப்பின் எழுத்து வகைக்கு ஏற்ப குரல் தேர்ந்தெடுக்கப்படும்: தமிழ் குறிப்பு தமிழ் நரம்பியல் குரளில், ஆங்கிலக் குறிப்பு ஆங்கிலக் குரளில். நீண்ட குறிப்புகள் வாக்கிய எல்லையில் பிரிக்கப்பட்டு வரிசையாகப் பேசப்படும். இணையம் இல்லாதபோது குரள் காத்திருக்கும் என்று சொல்லும் — ரோபோ குரளுக்கு மாறாது, குறிப்பை மௌனமாக விடாது. அனுமதிகள்: பேச்சுக்கான மைக் மற்றும் குரளுக்கான இணையம் மட்டும்.
