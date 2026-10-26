# Vasanthi AI - வசந்தி AI / Tamil sound and speech window

**Current stable release:** 1.1.1 / code 4

**Package:** `com.universalrp.vasanthi`

**Minimum Android:** 8.0 / API 26

This app is for one person. The owner's sister is deaf from birth - there is no neural connection between her ear and her brain, so she has never heard a sound and cannot speak. She reads Tamil. Vasanthi AI listens to the room and shows her, in Tamil, what it heard: what sound is happening, how loud it is, and what someone just said. It is named after her.

## What it does

- **A floating window that is always there.** A small draggable circle sits over every app, so she can be reading a message and still see that someone is at the door. Tap it and the panel opens; it does not need her to leave what she is doing.
- **Names the sound, in Tamil.** A YAMNet classifier runs entirely on the phone and recognises 521 real-world classes. 194 of them, the ones that actually occur in a home, carry Tamil names: கதவு மணி, தொலைபேசி, குழந்தை அழுகை, நாய் ஓட்டம், கீறல், புகை எச்சரிக்கை. A class with no Tamil name is dropped rather than shown as English, because an English sound name is a guess dressed up as information.
- **Shows how sure it is.** Every named sound carries a confidence bar. The classifier is sometimes wrong, and a wrong sound presented as certain is worse than no sound at all.
- **A loudness bar.** The room's level in dBFS, drawn as a bar and named in five Tamil words from மிக அமைதி to மிக அதிக சத்தம். She has never heard a sound and has nothing to compare a decibel number against.
- **Writes down what is said.** Tamil speech recognition through the platform recogniser, shown as it arrives and kept in the transcript. English that comes back is translated to Tamil on the phone before it is written down, because a line she cannot read is not a line.
- **Two switches, one microphone.** ஒலி names the sounds and பேச்சு writes down speech, and they are separate switches because they want the same microphone and only one of them can have it properly. With ஒலி off the recogniser has the microphone outright, which is both more accurate and reaches further, which is the owner's complaint about speech from a little way away. Either can be on alone, or both together.
- **Vibrates on the sounds that matter.** The doorbell, a smoke alarm, a crying child, breaking glass. This is the part that works when the phone is in her pocket and she is not looking at it.
- **Saves everything.** Every line of speech and every named sound, timestamped, filterable, re-readable, deletable.

## What it cannot do

- **It cannot hear a phone call.** Android does not allow any app to capture the other side of a call, at the system level. There is no permission to request and no library that works around it. Speech spoken to her in person is transcribed; a phone call is not. This is stated on the app's own screen so nobody discovers it by surprise.
- **Speech recognition is not perfect.** It is good for clear Tamil and weaker in noise and with fast or dialect-heavy speech. It is the cloud recogniser, so it needs a network connection and the audio of speech is sent to Google for it. A name, a word the recogniser knows only in English, and someone who actually spoke English all come back in English, and those are translated to Tamil on the phone before they are written down.

## Permissions

`RECORD_AUDIO`, `SYSTEM_ALERT_WINDOW`, `INTERNET`, `VIBRATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`.

**No location, no contacts, no storage, no camera, no phone state, no accounts, no advertising id.**

`INTERNET` is present for one reason only: speech recognition uses Google's cloud recogniser, which is the one that actually works on the owner's phone. The on-device recogniser was removed in 1.1 because it never worked on his phone and he asked for it to be gone. **The audio of speech is sent over the internet, and the app says so plainly on its own screen.** Sound classification is entirely on-device either way - YAMNet runs locally and no audio leaves the phone for it.

## Download the stable release

- [Official Website Hub](https://universalrp2003.github.io/RP-Android-Apps/) · [Download Vasanthi-AI-v1.1.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/vasanthi-ai-v1.1/Vasanthi-AI-v1.1.apk) · [release notes](../releases/vasanthi-ai-v1.1.md) · [all RP Ideas apps](../README.md)

> The APK is signed with the same CI keystore as the other RP Ideas apps, so it installs without an "unknown developer" warning. The published asset for 1.1 is 45,771,966 bytes, SHA-256 4f9a8d1f3a62a684ac71f6b8cfdffefdc155933ce0861513b2ac2365cc7cd5b6, published 2026-10-26T11:54:50Z. It is larger than 1.0.1 because it carries the on-device English-to-Tamil translation library, which is about twenty-eight megabytes; the Tamil model it needs is downloaded the first time the recogniser hands back English, so a Tamil-only household never pays for it. The candidate build that preceded it had a different SHA-256, because a release build is not byte-reproducible, which is why the published digest is the only one worth recording.

## The model, and where it came from

`src/main/assets/yamnet.tflite` is YAMNet, 4,292,304 bytes, SHA-256 `ee0fd63185c5bca7c721981d52b2826bc294d61e5e907ffcc8146578a8fb0145`. YAMNet is Apache-2.0, from the TensorFlow models repository; the AudioSet class names are CC BY 4.0. The file was taken from a public mirror because Google's own model bucket is not reachable from the build environment, and `tools/verify_vasanthi_sound_labels.py` pins the digest so a silently swapped model fails the gate instead of attaching Tamil names to the wrong sounds.

The model takes raw audio, not a spectrogram - its log-mel front end is inside the graph - so the app hands it 0.975 seconds of 16 kHz mono samples and reads back one score per class. The input tensor is `FLOAT32` of shape `[1, 15600]`; the output shape is queried from the model at runtime rather than hard-coded, because how many frames a given input produces is a property of the model file and not of this code.

## Device test status

**1.0 and 1.0.1 were installed on a phone by the owner**, on his own Xiaomi. The screenshots he sent from those two installs are what found the recogniser that never got the microphone, and the history writer that rewrote the whole file per line on the main thread until the system killed the process. **This 1.1 build has not been installed on a phone by anyone.** The store, the translator wiring, the cooldowns and the wording are covered by unit tests. Whether the app now survives a television left on all afternoon, whether the English-to-Tamil model downloads and works on his phone, and whether two switches read clearly on a small screen are not, and this app exists precisely to be used by one particular person, so it needs her phone before it can be called anything at all.

## Build and test

```
./gradlew :vasanthi:testDebugUnitTest :vasanthi:assembleRelease
python3 tools/verify_vasanthi_sound_labels.py
```
