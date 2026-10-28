# Voice Notes — குரல் குறிப்பு / Tamil voice notes

**Current stable release:** 1.2 / code 3

**Package:** `com.universalrp.voicenotes`

**Minimum Android:** 8.0 / API 26

Voice Notes is a standalone dictation-and-read-back app for Tamil. Tap the microphone, speak a note, search and edit saved notes, then hear one read back in the HD neural voice. Notes are stored in this app's private storage; the first HD playback asks before sending that selected note's text to the online neural voice service. No account is required. Dictation uses the device's speech-recognition service, whose audio handling depends on the installed provider. It is part of the eight-app **RP Ideas** open-source suite at [universalrp2003.github.io/RP-Android-Apps](https://universalrp2003.github.io/RP-Android-Apps/).

## What it does

- Dictate a note in Tamil, hear it spoken back, keep it on the phone.
- The voice is chosen by the script of the note, not by the app's interface language. A Tamil note is read by `ta-IN-ValluvarNeural`; an English note by the English voice. Reading Tamil with the English voice produces noise rather than wrong words, so the text decides.
- Long notes are split at sentence boundaries into chunks the neural endpoint can actually return, and spoken in order.
- HD neural voice is retained. With no internet, playback reports that HD voice is unavailable; there is deliberately no robotic voice fallback.
- One tap on the speaker symbol beside a note reads it back; the bin deletes it.

## Permissions

Microphone, for dictation; internet, for HD neural playback. **No location, no contacts, no SMS, no camera.** Notes use private app storage.

## Download the stable release

- [Official Website Hub](https://universalrp2003.github.io/RP-Android-Apps/) · [Download Voice-Notes-v1.2.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/voice-notes-v1.2/Voice-Notes-v1.2.apk) (`12,113,492` bytes · SHA-256 `c779d3650af3e9d23e40fd80b05c0d03f7e4189ada31ee20f7bb89df224e28b0`) · [release notes](../releases/voice-notes-v1.2.md) · [all 8 RP Ideas apps](../README.md)

> The APK is signed with the same CI keystore as the other RP Ideas apps, so it installs without an "unknown developer" warning.

## Device test status

The 1.0 build was installed on a phone and its screenshots found the layout defect 1.1 repairs — the title and footer drawn under the system bars. The exact 1.1 bytes have **not** been installed on a phone. See [the release note](../releases/voice-notes-v1.2.md) for what is and is not verified.

## Build and test

```
./gradlew :voicenotes:testDebugUnitTest :voicenotes:assembleRelease
```

The note store's round-trip and the speech decisions — which voice reads which note, and how a long note is chunked — are covered by unit tests. The dictation flow, the neural playback and the Tamil wording on a real screen are not.

## Device review — 28 October 2026

Owner reports version 1.2 works on their phone; screenshots show note editing. Not hash-matched and not a test of every voice/error path.

[தமிழ் / English help and other applications](../docs/three-app-help.md) · [Official website](https://universalrp2003.github.io/RP-Android-Apps/)
