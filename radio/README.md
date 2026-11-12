# Ramesh Radio

> Stable1.31/code31: hold and drag whole category tabs; automatically saved order, accessible arrow controls/reset, reviewed catalogue with28 additional entries. Owner accepted the interface and authorized exact-APK publication; no full-catalogue listening or installed-hash match is claimed.

Ramesh Radio is a Tamil-first radio and local-audio player with an in-app English option. Latest published release: **1.31 / code31**, package `com.universalrp.tamilnadufm`, minimum Android8.0/API26.

- [Official website](https://universalrp2003.github.io/RP-Android-Apps/) · [Download Ramesh-Radio-v1.31.apk](https://github.com/universalrp2003/RP-Android-Apps/releases/download/ramesh-radio-v1.31/Ramesh-Radio-v1.31.apk) (15,179,972 bytes; SHA-256 `fceac7c50758990eaa33228a3ec375f38fb9a307bca8452566eb84d037a5dc22`) · [release notes](../releases/ramesh-radio-v1.31.md).
- Exact accepted candidate run41205378161, source691d176cc00adb59702e53bbae3e99bfc2663ec5; promoted without rebuilding in run41207596858. Existing1.29/1.30 installations should update, not uninstall or clear data. Earlier releases remain available.
- 532 catalogue records;13 shared-provider streams held for content review;42 endpoints unavailable/unresolved. Connection/audio-metadata tests are not station identity, no-promotions or normal-programme guarantees. Tamil80’s normal programme is not restored or bypassed.
- [Owner acceptance and immutable receipt](../docs/radio-1.31-owner-acceptance.md) · [Privacy policy](../PRIVACY.md)

## Features

- Tamil and international station presets, with local favorites and search.
- The bundled station list is available offline. The mixed all-station list keeps health-verified streams ahead of unchecked entries, prioritizes Tamil within each health group, and uses Radio Browser community votes for popularity. Filtered Tamil/category lists likewise keep verified streams ahead and use votes within each health group. Counts are labelled as votes, not listener counts; directory search runs only when requested.
- Play local audio files with app-scoped equalization and playback controls. The 0–10 dB session boost uses DynamicsProcessing input gain on Android 9+ and a LoudnessEnhancer fallback on older/fallback audio engines. It can run independently of the EQ enable switch; the device-volume ceiling remains and excessive boost can distort.
- Foreground update checks use published release metadata and browser downloads; Android performs installation/signature checks. No protection bypass or automatic install.

Radio streams require internet access and may be supplied by third-party stations. Local audio is played on-device and is not uploaded by the app. Android may require the per-app “Install unknown apps” permission for direct APK updates and shows an installer confirmation for every update.

## Build

```sh
./gradlew :radio:testDebugUnitTest :radio:testReleaseUnitTest
./gradlew :radio:assembleRelease
```

The release workflow also creates an Android App Bundle for store delivery. It verifies application identity, version, manifest, signing certificate and release asset digest.

[தமிழ் / English help and other applications](../docs/three-app-help.md) · [Official website](https://universalrp2003.github.io/RP-Android-Apps/)
