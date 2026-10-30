# Live Guard (CleanSweep)

Live Guard is a Tamil/English phone-care, battery/data/weather guard, optional news announcer and device-monitoring app. The current public APK is **2.79 / code 84**, package `com.universalrp.cleansweep`, minimum Android 8.0 (API 26).

- [Official website and downloads](https://universalrp2003.github.io/RP-Android-Apps/#app-live-guard) · [CleanSweep-v2.79.apk](https://github.com/universalrp2003/RP-Android-Apps/releases/download/live-guard-v2.79/CleanSweep-v2.79.apk) · [release notes](../releases/live-guard-v2.79.md) · [suite directory](../README.md)
- Exact promoted APK: **14,479,951 bytes**, SHA-256 `95a050510786f1e4a34746cb0b3dcf5d3a3a032ec61052a94745d8f7719941ac`. Candidate run 41049481796 / artifact 12974397087; successful no-rebuild promotion run 41052024326. Pinned signer, package/version, minimum SDK and built-permission delta were verified; no dangerous permission was added over public 2.75.
- CI ran 510 required JVM regressions in each Debug/Release variant. The owner accepted the candidate UI and supplied three screenshots; the installed hash was not independently measured. This is not proof of every-category coverage, audible/background delivery, perfect translations or Play Protect clearance. [Owner acceptance](../docs/live-guard-2.79-owner-acceptance.md) · [release ledger](../docs/published-release-digests.md).
- [Privacy policy](../PRIVACY.md) · [installation advisory](../docs/live-guard-play-protect-install-block.md)

## What it does

User-selected junk review/cleanup, device-health readings, permission/security review, network/data tools, optional notifications/widgets and optional AI assistance. Unavailable/OEM-dependent readings are labelled, not invented. Nothing is deleted without selection and confirmation. The app does not include notification/message reading, an Accessibility service, SMS/contact/call-log access or device-admin capability.

News uses current-local-day publication, quality and recent-read filters, preferring native Tamil. Category ceilings are three Tamil Nadu and one each India, world, science, cyber safety and nature. Up to eight eligible items are selected; missing categories stay empty. RSS publication dates do not independently prove that an event is new, and live sources/complete coverage are not guaranteed.

A configured non-keyless AI provider can edit public news and translate missing specialist headlines, with mechanical guards and a second model comparison. The enhanced path has one shared 60-second budget. These checks reduce errors, not prove perfect fidelity. The existing prepare-all-audio pipeline and Stop cancellation are preserved. Private phone reports are not included in the news payload. Category/source reports describe prepared selection, not proof of playback.

## Updates and safety

The app performs a read-only release-metadata check at most once per rolling 24 hours when opened/resumed. An available-update action opens the official website in the external browser. **Live Guard does not download/install update APKs itself or request installer permission.** The browser download checks the published SHA-256 when supported; a match is file identity, not security approval. Android handles user-confirmed installation. Stop if Play Protect warns; do not bypass it.

Replacement-key versions 2.74 onward can update in place. Do not uninstall or clear data for a same-key update. Only installations using the former exposed key need the documented signing migration: obtain the official replacement first, back up/export important data, and remember that uninstalling may erase local data. Existing release assets remain unchanged.

## Historical device evidence (not transferred to 2.79)

2.50 is the first release that asks for **Location**, so the permission-delta gate required a recorded physical-device install. [`../docs/live-guard-2.50-device-evidence.md`](../docs/live-guard-2.50-device-evidence.md) records it: the owner installed `CleanSweep-v2.50.apk` (13,740,236 bytes, SHA-256 `b4c183c876a8da8aff4169c975c1a1111a3229658620e99fed87cb3e645d6176`) over 2.49 with Play Protect on, Play Protect reported "This app looks safe — You can continue to install it", Android's installer completed the update, and the nearby-cell list then appeared with four cells. **The published asset has that same digest**, so the released APK is the installed one. That is one phone: no second device was tested, no device test covers the Play Store bundle, and no screenshot shows an APK hash.

## Build and verify

With JDK 17, the Android SDK and the CI-pinned Gradle version available:

```sh
gradle :cleaner:testDebugUnitTest :cleaner:testReleaseUnitTest
gradle :cleaner:assembleRelease
python3 tools/verify_cleaner_regressions.py
```

Full CI also runs source/localization checks, merged-manifest/DEX gates, package/version/SDK and signing checks. A new local build is not the promoted release bytes and cannot inherit the owner's acceptance or published digest.
