# PulseEQ

> Unreleased candidate 1.11: browser-only updates and removal of unnecessary network/install capabilities. Not installed on a phone yet; public stable links below are intentionally unchanged. [Changes and phone-test checklist](../docs/2026-10-27-playback-follow-up.md).

PulseEQ is a Tamil-first audio player and 20-band equalizer with an in-app English option. Its latest published release is **1.9 / code 10**, package `com.universalrp.pulseeq`, minimum Android 8.0 (API 26).

- [Download PulseEQ-v1.9.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/pulseeq-v1.9/PulseEQ-v1.9.apk) · [release notes](../releases/pulseeq-v1.9.md)
- The published 1.9 asset is 12,100,805 bytes, confirmed from the GitHub release API. Published digests are in [`../docs/published-release-digests.md`](../docs/published-release-digests.md). Integrity checks are not Play Protect clearance.
- [Privacy policy](../PRIVACY.md)

## Features and audio privacy

- Twenty-band processing, saved presets, spectrum display and local audio playback.
- Playback and audio processing happen on the device. Whether an equalizer effect applies to audio outside this app depends on the Android version, audio session and device/ROM; system-wide operation is not guaranteed.
- Candidate 1.11 opens official releases in an external browser; it has no app INTERNET or APK-install permission/provider. Published 1.9 used the older in-app updater. Selected local audio is not uploaded by PulseEQ.

Candidate 1.11 links to official release pages in the external browser; it does not download APKs or open the package installer itself. The older 1.10 candidate was blocked by Play Protect. Do not disable protection or use Install anyway. Its cause is unresolved; neither a rebuild nor a signature/digest check is security clearance.

## Build

```sh
./gradlew :equalizer:testDebugUnitTest :equalizer:testReleaseUnitTest
./gradlew :equalizer:assembleRelease
```

The release workflow also produces an Android App Bundle and verifies application identity, version, manifest, signing certificate and release asset digest.

See [latest device report and PulseEQ hold](../docs/2026-10-27-pulseeq-device-review.md). The new candidate has not been installed on a phone.
