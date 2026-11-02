# Live Guard (CleanSweep)

Current public APK: **2.88 / code 93**, package `com.universalrp.cleansweep`, Android8.0/API26+.

- [Download CleanSweep-v2.88.apk](https://github.com/universalrp2003/RP-Android-Apps/releases/download/live-guard-v2.88/CleanSweep-v2.88.apk) · [release notes](../releases/live-guard-v2.88.md) · [official website](https://universalrp2003.github.io/RP-Android-Apps/#app-live-guard)
- Exact tested source **ec25204b64fdce2e83ed83e3e50c7a66caf9c882**, APK **14,680,987 bytes**, SHA-256 `87be21168e9f7d619ee8709163f632ebfd79b1a731d2b45e8b4d6618ca43a70c`. Signed regression run41414340270:689 required JVM tests/69suites, release lint/build, full permission/signer continuity, same-key2.87 saved-data update and native notice/Turn off/persistence/all-topic reset controls passed. Exact artifact13127658323 promoted without rebuild by run41416558931; release431241194/asset701116629. All previous public assets unchanged.
- A one-time foreground notice explains disaster news, shows its actual state and offers Got it / Turn off (or Enable). Disaster news defaults ON for never-configured users only; explicit OFF stays OFF. All13 topics default selected; custom selections/language stay intact. At most eight eligible stories per reading. Future registered optional features support Enable / Not now; not all settings are automatically enabled. Advanced battery stays optional.
- Each nearby GDACS cyclone/flood/earthquake appears once in each of the next three successful news bulletins. Fresh optional GPS else explicitly saved home; dated approximate distance/observed advisory wind only. Movement unavailable without sourced units. No standalone alarm, live tracking or local-danger prediction. Stop/failure/preparation does not consume a slot; all master/news/HD/call/quiet/silent-place gates remain.
- Owner accepted2.86; no repeat accepted phone tests requested.2.88 has automated checks, not new physical/audible/provider-accuracy or Play Protect proof. Normal same-key update only; do not uninstall/clear data. [Scope and residuals](../docs/live-guard-2.88-candidate.md) · [publication receipt](../docs/live-guard-2.88-publication-receipt.json) · [privacy](../PRIVACY.md).
- Accepted weather/maps, home-first + optionalGPS, fresh authenticated Tomorrow minute-only15/30/60rain, simple100%-reached battery speech/Advanced opt-in retained. Other public apps unchanged; historical2.87/2.82 retained.

## What it does

User-selected junk review/cleanup, device-health readings, permission/security review, network/data tools, optional notifications/widgets and optional AI assistance. Unavailable/OEM-dependent readings are labelled, not invented. Nothing is deleted without selection and confirmation. The app does not include notification/message reading, an Accessibility service, SMS/contact/call-log access or device-admin capability.

News uses current-local-day publication, quality and recent-read filters, preferring native Tamil. All13 categories are eligible by default; custom selection and balanced category ceilings/rotation are preserved. Up to eight eligible items are selected; missing categories stay empty. RSS publication dates do not independently prove that an event is new, and live sources/complete coverage are not guaranteed.

A configured non-keyless AI provider can edit public news and translate missing specialist headlines, with mechanical guards and a second model comparison. The enhanced path has one shared 60-second budget. These checks reduce errors, not prove perfect fidelity. The existing prepare-all-audio pipeline and Stop cancellation are preserved. Private phone reports are not included in the news payload. Category/source reports describe prepared selection, not proof of playback.

## Updates and safety

The app performs a read-only release-metadata check at most once per rolling 24 hours when opened/resumed. An available-update action opens the official website in the external browser. **Live Guard does not download/install update APKs itself or request installer permission.** The browser download checks the published SHA-256 when supported; a match is file identity, not security approval. Android handles user-confirmed installation. Stop if Play Protect warns; do not bypass it.

Replacement-key versions 2.74 onward can update in place. Do not uninstall or clear data for a same-key update. Only installations using the former exposed key need the documented signing migration: obtain the official replacement first, back up/export important data, and remember that uninstalling may erase local data. Existing release assets remain unchanged.

## Historical device evidence (not transferred to 2.88)

2.50 is the first release that asks for **Location**, so the permission-delta gate required a recorded physical-device install. [`../docs/live-guard-2.50-device-evidence.md`](../docs/live-guard-2.50-device-evidence.md) records it: the owner installed `CleanSweep-v2.50.apk` (13,740,236 bytes, SHA-256 `b4c183c876a8da8aff4169c975c1a1111a3229658620e99fed87cb3e645d6176`) over 2.49 with Play Protect on, Play Protect reported "This app looks safe — You can continue to install it", Android's installer completed the update, and the nearby-cell list then appeared with four cells. **The published asset has that same digest**, so the released APK is the installed one. That is one phone: no second device was tested, no device test covers the Play Store bundle, and no screenshot shows an APK hash.

## Build and verify

With JDK 17, the Android SDK and the CI-pinned Gradle version available:

```sh
gradle :cleaner:testDebugUnitTest :cleaner:testReleaseUnitTest
gradle :cleaner:assembleRelease
python3 tools/verify_cleaner_regressions.py
```

Full CI also runs source/localization checks, merged-manifest/DEX gates, package/version/SDK and signing checks. A new local build is not the promoted release bytes and cannot inherit the owner's acceptance or published digest.
