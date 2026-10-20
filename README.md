# RP Ideas — Android apps

Six actively maintained, open-source Android apps developed by **Ramesh prathap .R** with shared project contributions, including software assistance from **Arena.ai Agent Mode**. The apps offer Tamil-first interfaces with an in-app English option. Current minimum Android versions are listed below; device-dependent features are described honestly in each app's documentation.

| App | Module | Current stable release (RP-Android-Apps) | Android | Purpose |
|---|---|---:|---:|---|
| **Live Guard (CleanSweep)** | `cleaner` | [2.53](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/live-guard-v2.53) | 8.0+ (API 26) | Phone cleanup, health and security review, network tools, and optional assistance. |
| **Ramesh Radio** | `radio` | [1.15](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/ramesh-radio-v1.15) | 8.0+ (API 26) | Tamil and world radio, vote-ranked station lists, local playback, and session boost. |
| **PulseEQ** | `equalizer` | [1.7](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/pulseeq-v1.7) | 8.0+ (API 26) | Twenty-band audio DSP, presets, spectrum view, and local playback. |
| **RP Vocal Remover Studio** | `vocalremover` | [0.1.13](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/rp-vocal-remover-studio-v0.1.13) | 10+ (API 29) | Local vocal reduction with WAV output and optional MP3 export. |
| **Oor Arivippu** | `oorarivippu` | [1.1.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.1.1) | 8.0+ (API 26) | Tamil/English GPS place and destination announcer with configurable offline-status reminders. |
| **Secret Message** | `secretmessage/wifi-test` | [1.1.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/secret-message-v1.1.1) | 8.0+ (API 26) | Two-phone private messenger over the same Wi-Fi or the owner's relay: remembered phones, calls, Tamil and English, verified updater. Not independently security-reviewed. |

**Secret Message is published but is not independently security-reviewed.** No outside security expert has reviewed it, and the mls-rs library has not had a full third-party audit, so do not rely on it where your safety depends on secrecy. The relay sees both phones' IP addresses, when they connect and how much data they send, and the start of each session before the safety-code check; it cannot read messages, drawings or pictures. See [`secretmessage/releases/secret-message-v1.1.1.md`](secretmessage/releases/secret-message-v1.1.1.md) and [`docs/secret-message-security-design.md`](docs/secret-message-security-design.md).

## Stable releases

The four-app batch was rebuilt and published on 20 October 2026: **Live Guard 2.50 / code 55** (nearby cell towers, one pack number everywhere), **Ramesh Radio 1.13 / code 13** (last station on launch, compact search row), **PulseEQ 1.7 / code 8** and **RP Vocal Remover Studio 0.1.13 / code 14** (no functional change; the versions moved so the batch could ship). It was published by [Build APK run 39355649275](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39355649275). The owner had already installed the exact Live Guard 2.50 bytes on his Xiaomi phone (SHA-256 `b4c183c8…6176`), and the published asset is that same APK. The 19 October 2026 batch (Live Guard 2.49 / code 54, Ramesh Radio 1.12 / code 12, PulseEQ 1.6 / code 7, RP Vocal Remover Studio 0.1.12 / code 13) was published by [Build APK run 39272003076](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39272003076), Oor Arivippu 1.1.1 / code 6 by its standalone [release run 39268547086](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39268547086), and Secret Message 1.1.1 / code 3 by `.github/workflows/secret-message-wifi-test.yml` in [run 39268577963](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39268577963). Each run passed the build, unit/regression, permission, package, signature and release-asset checks before publishing, and each re-read the published release to confirm the asset size and SHA-256. The owner reports that all six updates were downloaded and installed on his phone and are working. These six versions are published on `RP-Android-Apps` only: the older `InternetRadio` releases and assets remain available and unchanged. Filenames, sizes and verified SHA-256 values for all six current releases are in [`docs/published-release-digests.md`](docs/published-release-digests.md); release pages are the authoritative download source.

The owner reports the stable Oor Arivippu 1.0.0 app is working and was tested on two devices; those screenshots show package `com.oorarivippu.voiceannouncer` version 1.0.0 (2), Play Protect's “This app looks safe” screen, and the running app. For 1.0.1, the owner tested the debug APK from [run 38776902130](https://github.com/universalrp2003/InternetRadio/actions/runs/38776902130), built from the same app source before the version was raised, and reported that it works, including the Tamil/English voice picker and place announcements; after publication, the owner also installed the debug APK of the exact 1.0.1 source from [run 38826564762](https://github.com/universalrp2003/InternetRadio/actions/runs/38826564762), which reported 1.0.1 (3), and reported that it works. None of the screenshots shows an APK SHA-256, and debug test builds cannot share the stable APK's bytes, so no installed bytes are hash-matched to a public asset. 1.0.2, which shortens place announcements to the place names, was published at the owner's request without a device test. CI success and public-asset verification are not an all-device compatibility claim.

## Install and update

Download each signed APK from its linked GitHub Release page. The apps check for newer stable releases and verify the published APK metadata before offering an update. Downloading does not install silently: Android may require the per-app **Install unknown apps** permission and always shows its installer confirmation. Play Protect and Android's signature checks are not bypassed.

For download filenames, sizes and verified SHA-256 values, see [`docs/published-release-digests.md`](docs/published-release-digests.md).

> **Live Guard test note:** 2.50 adds Location (`ACCESS_COARSE_LOCATION` beside `ACCESS_FINE_LOCATION`), which the release gate counts as a newly added dangerous permission, so publication required a recorded physical-device install. `docs/live-guard-2.50-device-evidence.md` records it: the owner installed `CleanSweep-v2.50.apk` (13,740,236 bytes, SHA-256 `b4c183c876a8da8aff4169c975c1a1111a3229658620e99fed87cb3e645d6176`) over 2.49 with Play Protect on, Play Protect said "This app looks safe", the installer completed the update, and the nearby-cell list then showed four cells. The published asset has that same digest. One phone only: no second device was tested, and no device test covers the Play Store bundle. See the [installation advisory](docs/live-guard-play-protect-install-block.md) and [release notes](releases/live-guard-v2.50.md).

## Privacy and audio

- Live Guard stores its local settings and optional histories on-device. Optional AI, web, hash and network tools contact the selected third-party services when used; see [`PRIVACY.md`](PRIVACY.md) before enabling them.
- Ramesh Radio contacts the selected station to stream and searches the public station directory only when requested. Local songs stay on the device.
- PulseEQ processes audio locally. Its optional update check contacts GitHub; system-wide effects remain device/ROM-dependent.
- RP Vocal Remover Studio downloads its checksum-pinned model from the upstream GitHub release. Selected songs are processed on-device; the app has no selected-audio upload path. The first output is a WAV, the optional second pass is user-started, and MP3 export is lossy while the WAV is retained.
- Oor Arivippu uses location and Android's Geocoder for trip announcements and destination lookup. Settings and the selected destination are stored locally; the Android/OEM Geocoder provider may process lookups according to its own policy. See [`PRIVACY.md`](PRIVACY.md).

The developer does not operate an app backend, ad network or analytics service. Third-party services may receive requests when an online feature is used or enabled and during update checks, under their own policies; see the privacy policy for details.

## Build

The project uses JDK 17, Gradle 8.14.3, the Android SDK and NDK. Open the repository root in Android Studio or use the wrapper:

```sh
./gradlew :cleaner:testDebugUnitTest :cleaner:testReleaseUnitTest \
  :radio:testDebugUnitTest :radio:testReleaseUnitTest \
  :equalizer:testDebugUnitTest :equalizer:testReleaseUnitTest \
  :vocalremover:testDebugUnitTest :vocalremover:testReleaseUnitTest

./gradlew :cleaner:assembleRelease :radio:assembleRelease \
  :equalizer:assembleRelease :vocalremover:assembleRelease

./gradlew :oorarivippu:testReleaseUnitTest :oorarivippu:lintRelease :oorarivippu:assembleRelease
```

For the **non-shipping Secret Message preview only**, run `./gradlew :secretmessage:testDebugUnitTest :secretmessage:lintDebug :secretmessage:assembleDebug`; this produces a debug preview, not a release APK. GitHub Actions builds the four legacy batch apps and performs the required localization, regression, package/version/signature and release-asset checks. Oor Arivippu has its own test and marker-gated stable release workflows. The preview is excluded from every release and bundle task and has its own test-only workflow (`secret-message-preview.yml`): contract test, unit tests, lint, debug APK, a merged-permission allowlist, and a hosted API 35 emulator UI smoke test. All of these passed in [run 38835582621](https://github.com/universalrp2003/InternetRadio/actions/runs/38835582621); that is not a physical-device test. The four-app batch workflow generates Play bundles for CleanSweep, Ramesh Radio and PulseEQ; RP Vocal Remover Studio and Oor Arivippu are distributed as standalone APKs.

## Project scope and repository URLs

There are five active products: the four legacy batch apps listed above plus Oor Arivippu, which uses its own standalone stable release workflow. Secret Message remains a separate local-only development preview. Earlier app modules were retired from the active source tree; their already-published GitHub Releases and tags are retained as historical records and are not built by current CI.

`RP-Android-Apps` is the primary updater source and `InternetRadio` is the verified fallback. The four legacy bridge clients check the new repository first and keep the old endpoint during the agreed migration grace period that began 14 October 2026; do not archive or rename `InternetRadio` until the migration checks are complete. On 18 October 2026 the owner decided to move the source and future development to `RP-Android-Apps`, keeping `InternetRadio` as the update backup; see [`docs/source-migration.md`](docs/source-migration.md). Oor Arivippu 1.0.0 and 1.0.1 were published to both repositories separately and are not part of that four-app bridge batch. See [`docs/repository-migration.md`](docs/repository-migration.md).

## Project documents

- [`PRIVACY.md`](PRIVACY.md) — data flows, permissions and retention.
- [`PUBLISHING.md`](PUBLISHING.md) — current build and release process.
- [`CHANGELOG.md`](CHANGELOG.md) — release history and current scope.
- [`docs/published-release-digests.md`](docs/published-release-digests.md) — verified public APK hashes.
- [`store/README.md`](store/README.md) — store-art and metadata inventory.
