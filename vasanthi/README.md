# Vasanthi AI - வசந்தி AI / Tamil sound and speech window

**Current stable release:** 1.1.2 / code 5

**Package:** `com.universalrp.vasanthi`

**Minimum Android:** 8.0 / API 26

Vasanthi AI turns the sound around you into Tamil text. It listens to the room and shows, in Tamil, what sound is happening, how loud it is, and what somebody just said - and it keeps all of it so it can be read again later.

It is for anyone who wants to see what is being said or what is happening in the room in Tamil: people who are deaf or hard of hearing, people who cannot hear a television or a call clearly, and anyone who would rather read than listen. It started as an app for one person, and it is now published for everyone.

## What it does

- **A floating window that is always there.** A small draggable circle sits over every app, so she can be reading a message and still see that someone is at the door. Tap it and the panel opens; it does not need her to leave what she is doing.
- **Names the sound, in Tamil.** A YAMNet classifier runs entirely on the phone and recognises 521 real-world classes. 194 of them, the ones that actually occur in a home, carry Tamil names: கதவு மணி, தொலைபேசி, குழந்தை அழுகை, நாய் ஓட்டம், கீறல், புகை எச்சரிக்கை. A class with no Tamil name is dropped rather than shown as English, because an English sound name is a guess dressed up as information.
- **Shows how sure it is.** Every named sound carries a confidence bar. The classifier is sometimes wrong, and a wrong sound presented as certain is worse than no sound at all.
- **A loudness bar.** The room's level in dBFS, drawn as a bar and named in five Tamil words from மிக அமைதி to மிக அதிக சத்தம். She has never heard a sound and has nothing to compare a decibel number against.
- **Writes down what is said.** Tamil speech recognition through the platform recogniser, shown as it arrives and kept in the transcript. English that comes back is translated to Tamil on the phone before it is written down, because a line she cannot read is not a line.
- **Two switches, one microphone.** ஒலி names the sounds and பேச்சு writes down speech, and they are separate switches because they want the same microphone and only one of them can have it properly. With ஒலி off the recogniser has the microphone outright, which is both more accurate and reaches further, which is the owner's complaint about speech from a little way away. Either can be on alone, or both together.
- **Vibrates on the sounds that matter.** The doorbell, a smoke alarm, a crying child, breaking glass. This is the part that works when the phone is in her pocket and she is not looking at it.
- **Saves everything.** Every line of speech and every named sound, timestamped, filterable, re-readable, deletable.
- **A built-in guide.** A வழிகாட்டி button on the main screen opens a tap-to-open Tamil and English guide covering the floating circle, the two switches, the translation, the notification and the privacy position - the same shared guide the other RP Ideas apps use.
- **Links to every other RP Ideas app.** The About section lists the whole suite and the project links, so the app is one door into the rest of it.

## What it cannot do

- **It cannot hear a phone call.** Android does not allow any app to capture the other side of a call, at the system level. There is no permission to request and no library that works around it. Speech spoken to her in person is transcribed; a phone call is not. This is stated on the app's own screen so nobody discovers it by surprise.
- **Speech recognition is not perfect.** It is good for clear Tamil and weaker in noise and with fast or dialect-heavy speech. It is the cloud recogniser, so it needs a network connection and the audio of speech is sent to Google for it. A name, a word the recogniser knows only in English, and someone who actually spoke English all come back in English, and those are translated to Tamil on the phone before they are written down.

## Permissions

`RECORD_AUDIO`, `SYSTEM_ALERT_WINDOW`, `INTERNET`, `VIBRATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`.

**No location, no contacts, no storage, no camera, no phone state, no accounts, no advertising id.**

`INTERNET` is present for one reason only: speech recognition uses Google's cloud recogniser, which is the one that actually works on the owner's phone. The on-device recogniser was removed in 1.1 because it never worked on his phone and he asked for it to be gone. **The audio of speech is sent over the internet, and the app says so plainly on its own screen.** Sound classification is entirely on-device either way - YAMNet runs locally and no audio leaves the phone for it.

## Download the stable release

- [Official Website Hub](https://universalrp2003.github.io/RP-Android-Apps/) · [Download Vasanthi-AI-v1.1.2.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/vasanthi-ai-v1.1.2/Vasanthi-AI-v1.1.2.apk) · [release notes](../releases/vasanthi-ai-v1.1.2.md) · [all RP Ideas apps](../README.md)

The published asset for 1.1.2 is 45,773,770 bytes, SHA-256 04f24d557b5971460b26fb6528554f02f27654c8cb9bac2c587908c6100f7c4f, published 2026-10-26T15:21:11Z. It is larger than 1.0.1 because it carries the on-device English-to-Tamil translation library, which is about twenty-eight megabytes; the Tamil model it needs is downloaded the first time the recogniser hands back English, so a Tamil-only household never pays for it. The candidate build that preceded it had a different SHA-256, because a release build is not byte-reproducible, which is why the published digest is the only one worth recording.

## The model, and where it came from

`src/main/assets/yamnet.tflite` is YAMNet, 4,292,304 bytes, SHA-256 `ee0fd63185c5bca7c721981d52b2826bc294d61e5e907ffcc8146578a8fb0145`. YAMNet is Apache-2.0, from the TensorFlow models repository; the AudioSet class names are CC BY 4.0. The file was taken from a public mirror because Google's own model bucket is not reachable from the build environment, and `tools/verify_vasanthi_sound_labels.py` pins the digest so a silently swapped model fails the gate instead of attaching Tamil names to the wrong sounds.

The model takes raw audio, not a spectrogram - its log-mel front end is inside the graph - so the app hands it 0.975 seconds of 16 kHz mono samples and reads back one score per class. The input tensor is `FLOAT32` of shape `[1, 15600]`; the output shape is queried from the model at runtime rather than hard-coded, because how many frames a given input produces is a property of the model file and not of this code.

## Device test status

**1.0, 1.0.1, 1.1 and 1.1.1 were all installed on a phone by the owner**, on his own Xiaomi, and 1.1.1 was installed by his sister as well. The screenshots he sent from those installs are what found the recogniser that never got the microphone, the history writer that rewrote the whole file per line on the main thread until the system killed the process, the "thanglish" the recogniser produced when it returned Tamil with English words left inside it, and the speech that kept missing what was said because both switches were on at once. **This 1.1.2 build has not been installed on a phone by anyone.** The guide, the word-finding, the store, the cooldowns and the wording are covered by unit tests, and the shared guide and sister-app list are the same code the other apps already ship. Whether the guide reads clearly on a small screen and whether the other-app links open the right pages are not, and it is now an app for anyone, so it needs more than one phone before it can be called anything at all.

## Build and test

```
./gradlew :vasanthi:testDebugUnitTest :vasanthi:assembleRelease
python3 tools/verify_vasanthi_sound_labels.py
```
