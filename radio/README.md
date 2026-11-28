# Ramesh Radio

> Stable 1.32 / code 32: compact stations and Artists tabs, side navigation and clearer searches. Artist cards search station names; many have no matching in-app audio. External artist collection links have been removed.

Ramesh Radio is a Tamil-first radio and local-audio player with an in-app English option. Latest published release: **1.32 / code 32**, package `com.universalrp.tamilnadufm`, minimum Android 8.0/API 26.

- [Download Ramesh-Radio-v1.32.apk](https://github.com/universalrp2003/RP-Android-Apps/releases/download/ramesh-radio-v1.32/Ramesh-Radio-v1.32.apk) (15,231,296 bytes; SHA-256 `3f0973d1fa56c322171d4e34908624f41e68688d1f81cc1dde972faf23c70033`) · [release notes](../releases/ramesh-radio-v1.32.md).
- Candidate [46135353151](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/46135353151), source `17dae9d7d54841bef01634cb68c87c175d7aacc3`, published without rebuilding by [46136124785](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/46136124785). The exact release was not tested on a physical phone.
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
