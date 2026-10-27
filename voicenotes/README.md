# Voice Notes — குரல் குறிப்பு / Tamil voice notes

**Current stable release:** 1.1 / code 2

**Package:** `com.universalrp.voicenotes`

**Minimum Android:** 8.0 / API 26

Voice Notes is a standalone recorder-and-read-back app for Tamil. Tap the microphone, speak a note, hear it read back in the HD neural voice, and keep it. There is no account, no cloud and no upload: the notes file lives in this app's private storage and nowhere else. It is part of the eight-app **RP Ideas** open-source suite at [universalrp2003.github.io/RP-Android-Apps](https://universalrp2003.github.io/RP-Android-Apps/).

## What it does

- Dictate a note in Tamil, hear it spoken back, keep it on the phone.
- The voice is chosen by the script of the note, not by the app's interface language. A Tamil note is read by `ta-IN-ValluvarNeural`; an English note by the English voice. Reading Tamil with the English voice produces noise rather than wrong words, so the text decides.
- Long notes are split at sentence boundaries into chunks the neural endpoint can actually return, and spoken in order.
- With no internet the app says the voice is waiting rather than falling back to a robot voice or dropping the note silently.
- One tap on the speaker symbol beside a note reads it back; the bin deletes it.

## Permissions

Microphone, for dictation, and internet, for the voice. **No location, no contacts, no SMS, no storage, no camera.**

## Download the stable release

- [Official Website Hub](https://universalrp2003.github.io/RP-Android-Apps/) · [Download Voice-Notes-v1.1.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/voice-notes-v1.1/Voice-Notes-v1.1.apk) (`12,047,956` bytes · SHA-256 `171e924c68fdde29c8009f754287303b078a078c068a6a0d351ced76c2543f88`) · [release notes](../releases/voice-notes-v1.1.md) · [all 8 RP Ideas apps](../README.md)

> The APK is signed with the same CI keystore as the other RP Ideas apps, so it installs without an "unknown developer" warning.

## Device test status

The 1.0 build was installed on a phone and its screenshots found the layout defect 1.1 repairs — the title and footer drawn under the system bars. The exact 1.1 bytes have **not** been installed on a phone. See [the release note](../releases/voice-notes-v1.1.md) for what is and is not verified.

## Build and test

```
./gradlew :voicenotes:testDebugUnitTest :voicenotes:assembleRelease
```

The note store's round-trip and the speech decisions — which voice reads which note, and how a long note is chunked — are covered by unit tests. The dictation flow, the neural playback and the Tamil wording on a real screen are not.
