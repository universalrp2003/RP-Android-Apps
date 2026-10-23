# Ramesh Radio

Ramesh Radio is a Tamil-first radio and local-audio player with an in-app English option, multi-source mobile cell-tower local weather resolution (`ACCESS_COARSE_LOCATION` runtime prompt + exact place/district display + saved PIN fallback), and active-listening rain alerts that always announce exact place details. Its latest published release is **1.21 / code 21**, package `com.universalrp.tamilnadufm`, minimum Android 8.0 (API 26).

- [Official Website Hub](https://universalrp2003.github.io/RP-Android-Apps/) · [Download Ramesh-Radio-v1.21.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/ramesh-radio-v1.21/Ramesh-Radio-v1.21.apk) (`15,076,612` bytes · SHA-256 `b5cd68048a05fb52de998401194eabf23c2da8695238bf737e452de520ab033d`) · [release notes](../releases/ramesh-radio-v1.21.md) · earlier releases remain available on the RP-Android-Apps release list
- Build, unit/regression and APK checks passed in [publication run 39917424951](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39917424951). Published asset size and SHA-256 are in [`../docs/published-release-digests.md`](../docs/published-release-digests.md).
- [Privacy policy](../PRIVACY.md)

## Features

- Tamil and international station presets, with local favorites and search.
- The bundled station list is available offline. The mixed all-station list keeps health-verified streams ahead of unchecked entries, prioritizes Tamil within each health group, and uses Radio Browser community votes for popularity. Filtered Tamil/category lists likewise keep verified streams ahead and use votes within each health group. Counts are labelled as votes, not listener counts; directory search runs only when requested.
- Play local audio files with app-scoped equalization and playback controls. The 0–10 dB session boost uses DynamicsProcessing input gain on Android 9+ and a LoudnessEnhancer fallback on older/fallback audio engines. It can run independently of the EQ enable switch; the device-volume ceiling remains and excessive boost can distort.
- Stable APK updates verify URL, digest, package, version and signer before Android's installer opens. The updater checks `RP-Android-Apps` first, retains `InternetRadio` as the grace-period fallback, and identifies the repository serving an available release.

Radio streams require internet access and may be supplied by third-party stations. Local audio is played on-device and is not uploaded by the app. Android may require the per-app “Install unknown apps” permission for direct APK updates and shows an installer confirmation for every update.

## Build

```sh
./gradlew :radio:testDebugUnitTest :radio:testReleaseUnitTest
./gradlew :radio:assembleRelease
```

The release workflow also creates an Android App Bundle for store delivery. It verifies application identity, version, manifest, signing certificate and release asset digest.
