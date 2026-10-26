# Emergency Alert (அவசர எச்சரிக்கை)

Hears the sounds that mean somebody needs help and immediately tells the other phones in the house, in Tamil. No internet, no server, no account.

Built as part of the RP Ideas suite. Package `com.universalrp.emergency`, version 1.0.

## What it does

It listens for eighteen specific sounds and only those eighteen can raise an alert:

| Class | Tamil name | Severity |
|---|---|---|
| Smoke detector, smoke alarm | புகை எச்சரிக்கை | fire |
| Fire alarm | தீ எச்சரிக்கை | fire |
| Fire | தீ | fire |
| Siren | சைரன் | fire |
| Civil defense siren | பேரழிவ்ச்சை சைரன் | fire |
| Fire engine, fire truck (siren) | தீ அணியன் வாகனம் | fire |
| Screaming | கத்தும் ஒலி | help |
| Shout | கூச்சல் | help |
| Yell | அலறும் ஒலி | help |
| Children shouting | குழந்தைகள் கூச்சல் | help |
| Crying, sobbing | அழும் ஒலி | help |
| Baby cry, infant cry | குழந்தை அழுகை | help |
| Shatter | உடைந்து விழும் ஒலி | help |
| Glass | கண்ணாடி உடைவு | help |
| Gunshot, gunfire | துப்பாக்கி ஒலி | help |
| Doorbell | கதவு மணி | door |
| Ding-dong | டிங்-டாங் | door |
| Ding | மணி ஒலி | door |

YAMNet names 521 classes. A ceiling fan, a television, a washing machine and typing are all deliberately absent: an app that wakes a family at three in the morning for a fan is worse than no app.

## How the alert travels

A UDP packet on the home network. Every phone on the same Wi-Fi that has the app open and listening receives it.

- **No internet needed.** The router losing its upstream connection does not stop this working.
- **The broadcast address is read from the Wi-Fi interface**, not assumed, because many home routers drop a global broadcast while passing the directed one.
- **Three copies per packet.** UDP on a shared network loses packets and this is not a stream where a retransmit is expected.
- **A ping and a reply**, so the screen can show which phones are actually connected.
- **A repeat of the same sound within fifteen seconds is one event**, not thirty.

The protocol is three plain-text packet kinds, chosen so a JVM unit test can parse them without `org.json` and so a log dump is readable:

```
RPEMERGENCY1|name|severity|tamil|message|confidence|millis
RPEMERGENCY-PING|model
RPEMERGENCY-PONG|name
```

## How it is put together

One microphone, one reader thread, one classifier call per window. That is not a matter of taste: Vasanthi AI crashed repeatedly in 1.0 because two things shared one microphone and a reader was released while still inside a native `read()`. Here there is exactly one reader, it owns the `AudioRecord` from open to close, and nothing else touches the microphone.

- `SoundClassifier.kt` - YAMNet over a rolling 0.975 s window, output shape read at runtime.
- `EmergencyLabelMap.kt` - the eighteen classes, their Tamil names, their alert sentences, their severities and their confidence thresholds.
- `AlertBroadcaster.kt` / `AlertListener.kt` - the UDP send and receive.
- `EmergencyState.kt` - the shared, locked, bounded store the screen reads.
- `EmergencyService.kt` - the foreground service, the microphone loop, the notification.
- `AlertReceiver.kt` - restarts listening after a reboot, only if the owner left it on.

The microphone loop uses `VOICE_RECOGNITION` rather than `MICROPHONE`, and a partial wake lock, so a phone on a table with its screen off keeps listening through Doze.

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | to hear the room at all |
| `INTERNET` | for the local socket only; no internet connection is used |
| `VIBRATE` | so the alert is felt in a pocket |
| `FOREGROUND_SERVICE` | to keep listening while the phone does something else |
| `FOREGROUND_SERVICE_MICROPHONE` | because that service has the microphone open |
| `POST_NOTIFICATIONS` | Android 13+ requires it before any notice can be shown |
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | to find the Wi-Fi broadcast address |
| `RECEIVE_BOOT_COMPLETED` | to resume listening after the phone restarts |

**`SEND_SMS` is deliberately not declared.** The chosen scope was the local network. If SMS is ever wanted it will be a separate, explicitly granted step.

No location, no contacts, no storage, no camera, no phone state, no accounts, no advertising identifier.

## What it cannot do

- **Android blocks every app from hearing the other side of a phone call.** This app hears the room, not the call. The same is true of every app on the phone.
- **It is an alert, not a guard.** An ordinary sound will sometimes be mistaken for an emergency one. The confidence percentage is shown beside every alert so a false one can be judged.

## Model and labels

`assets/yamnet.tflite` and `assets/yamnet_labels.txt` are the same files Vasanthi AI ships, byte for byte:

- model: 4,292,304 bytes, SHA-256 `ee0fd63185c5bca7c721981d52b2826bc294d61e5e907ffcc8146578a8fb0145`
- labels: 521 AudioSet class names

`tools/verify_vasanthi_sound_labels.py` checks every key in `EmergencyLabelMap.kt` exists in the shipped label file, that the two label files are identical, that every value is Tamil script, and that the model carries `TFL3` magic and matches the pinned digest. The gate runs in `build-apk.yml` and `emergency-release.yml`.

YAMNet's AudioSet class names are CC BY 4.0; the model itself is Apache-2.0.

## Build

```
./gradlew :emergency:testDebugUnitTest :emergency:assembleRelease
```

ARM only (`arm64-v8a`, `armeabi-v7a`) - the same restriction Vasanthi AI and Vocal Remover carry. `minSdk` 26, `targetSdk` 36.

## Status

**Published 2026-10-26T16:40:39Z** as [emergency-v1.0](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/emergency-v1.0):

- Asset: `Emergency-Alert-v1.0.apk`, **17,348,330 bytes**
- SHA-256: `40144825bf8dbada29c30de924a2df65527fba8eadc377464d0a0aafb8b30538`
- Release run: [40360826395](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40360826395)

Version 1.0 has **not been installed on a phone**, so it has had no device test. What is covered by unit tests: the wire protocol, the duplicate suppression, the emergency vocabulary against the shipped model, and the state store. What is not: whether the microphone stays open for a whole night, whether the packets cross a real home router, and behaviour on a phone with aggressive battery management.
