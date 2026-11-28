# Ramesh Radio

> Stable 1.33 / code 33: compact stations and Artists tabs, side navigation and clearer searches. Artist cards search station names; many have no matching in-app audio. External artist collection links have been removed.

Ramesh Radio is a Tamil-first radio and local-audio player with an in-app English option. Latest published release: **1.33 / code 33**, package `com.universalrp.tamilnadufm`, minimum Android 8.0/API 26.

- [Download Ramesh-Radio-v1.33.apk](https://github.com/universalrp2003/RP-Android-Apps/releases/download/ramesh-radio-v1.33/Ramesh-Radio-v1.33.apk) (15,231,296 bytes; SHA-256 `2be7401b03f054fd985a39d791713c09fa56b207dcbd6747b0a604148c574290`) · [release notes](../releases/ramesh-radio-v1.33.md).
- Candidate [46139290240](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/46139290240), source `ed65d011eeea1ad94433a4fcade519f27847e38c`, published without rebuilding by [46139916692](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/46139916692). The exact release was not tested on a physical phone.
- Weather settings are available in Settings & more. Saved places and existing alert choices are retained; weather can auto-enable when device location is on and no explicit choice was saved. Turning it off records your choice.
- Of 616 catalogue entries, 67 unheld streams were unavailable in one network probe and 13 remain held for review. Metadata checks do not verify content, identity or rights. No verified continuous Tamil news audio was identified.
- Update without uninstalling when signatures match. Uninstalling can erase local data, favourites and custom stations. [Privacy policy](../PRIVACY.md)

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
