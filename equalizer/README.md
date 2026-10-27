# PulseEQ

> Unreleased candidate 1.10: continuous playback / naming follow-up. Not installed on a phone yet; public stable links below are intentionally unchanged. [Changes and phone-test checklist](../docs/2026-10-27-playback-follow-up.md).

PulseEQ is a Tamil-first audio player and 20-band equalizer with an in-app English option. Its latest published release is **1.7 / code 8**, package `com.universalrp.pulseeq`, minimum Android 8.0 (API 26).

- [Download PulseEQ-v1.7.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/pulseeq-v1.7/PulseEQ-v1.7.apk) · [release notes](../releases/pulseeq-v1.7.md) · 1.7 has no functional change over 1.6; it exists so the four-app batch could ship
- Build, unit/regression and APK checks passed in [publication run 39355649275](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39355649275). Published asset size and SHA-256 are in [`../docs/published-release-digests.md`](../docs/published-release-digests.md).
- [Privacy policy](../PRIVACY.md)

## Features and audio privacy

- Twenty-band processing, saved presets, spectrum display and local audio playback.
- Playback and audio processing happen on the device. Whether an equalizer effect applies to audio outside this app depends on the Android version, audio session and device/ROM; system-wide operation is not guaranteed.
- The optional stable-release check contacts GitHub. Selected local audio is not uploaded by PulseEQ.

The updater checks RP-Android-Apps first and InternetRadio as fallback, displays the source of an available release, and validates APK updates before Android's package installer is opened. Android may require per-app “Install unknown apps” access and always presents its own confirmation.

## Build

```sh
./gradlew :equalizer:testDebugUnitTest :equalizer:testReleaseUnitTest
./gradlew :equalizer:assembleRelease
```

The release workflow also produces an Android App Bundle and verifies application identity, version, manifest, signing certificate and release asset digest.
