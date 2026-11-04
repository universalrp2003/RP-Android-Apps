# Live Guard (CleanSweep)

Current public APK: **2.95 / code100**, package `com.universalrp.cleansweep`, Android8.0/API26+.

- [Download CleanSweep-v2.95.apk](https://github.com/universalrp2003/RP-Android-Apps/releases/download/live-guard-v2.95/CleanSweep-v2.95.apk) · [release notes](../releases/live-guard-v2.95.md) · [official website](https://universalrp2003.github.io/RP-Android-Apps/#app-live-guard).
- Finer approximate dBm-derived SIM percentage; unknown remains unknown, not calibrated reception reliability. About removes Legacy and places DailyVault only under Other Apps, without changing sibling layouts.
- Widget forecasts are snapshots, not continuous monitoring. Source/time-labelled regional reports or explicit not-checked/unavailable/no-matching-report status; never all-clear. Refresh queues one coalesced bounded silent saved-home report/weather check, without enabling news, speaking or consuming notification/read ledgers. No new periodic disaster poller or permissions.
- Preserve accepted concise cyclone/rain/news speech, top Read, default-OFF optional quiet beep, three-segment D/U, home/settings, language/topics and saved OFF choices. New cyclone advisories speak once; flood/earthquake three-read policy is unchanged.
- 923 executed required JVM tests/89suites,418 Python checks, build/lint/signing/full permissions, exact public2.94 same-key same-UID/saved-data update and all9 Android35 tests passed. Exact tested bytes promoted without rebuilding. [Candidate evidence](../docs/live-guard-2.95-candidate-verification.json) · [publication receipt](../docs/live-guard-2.95-publication-receipt.json).
- No physical-device, audible-output, all-OEM, live-provider accuracy or Play Protect clearance proof. Same-key update: do not uninstall or clear data. All sibling and older public APK assets are unchanged.

## This update

First installed SIM signal in the movable pill by default; choose Both SIMs or Off. Coloured estimates use subscription-local dBm; unavailable stays hidden. Missing normal readings use only a fresh registered serving cell when one active SIM, a prior precise-location grant and visible app UI make attribution safe. No background cell polling, stronger neighbour, copied SIM or new permission prompt. Charging remains watts-only. Unset news language defaults to Tamil+English-to-Tamil; explicit saved choices remain. Home top gap and Scan button sizing corrected without disabling font scaling. See the built-in bilingual guide.

## What it does

User-selected junk review/cleanup, device-health readings, permission/security review, network/data tools, optional notifications/widgets and optional AI assistance. Unavailable/OEM-dependent readings are labelled, not invented. Nothing is deleted without selection and confirmation. The app does not include notification/message reading, an Accessibility service, SMS/contact/call-log access or device-admin capability.

News uses current-local-day publication, quality and recent-read filters, preferring native Tamil. All13 categories are eligible by default; custom selection and balanced category ceilings/rotation are preserved. Up to eight eligible items are selected; missing categories stay empty. RSS publication dates do not independently prove that an event is new, and live sources/complete coverage are not guaranteed.

A configured non-keyless AI provider can edit public news and translate missing specialist headlines, with mechanical guards and a second model comparison. The enhanced path has one shared 60-second budget. These checks reduce errors, not prove perfect fidelity. The existing prepare-all-audio pipeline and Stop cancellation are preserved. Private phone reports are not included in the news payload. Category/source reports describe prepared selection, not proof of playback.

## Updates and safety

The app performs a read-only release-metadata check at most once per rolling 24 hours when opened/resumed. An available-update action opens the official website in the external browser. **Live Guard does not download/install update APKs itself or request installer permission.** The browser download checks the published SHA-256 when supported; a match is file identity, not security approval. Android handles user-confirmed installation. Stop if Play Protect warns; do not bypass it.

Replacement-key versions 2.74 onward can update in place. Do not uninstall or clear data for a same-key update. Only installations using the former exposed key need the documented signing migration: obtain the official replacement first, back up/export important data, and remember that uninstalling may erase local data. Existing release assets remain unchanged.

## Historical device evidence (not transferred to 2.95)

2.50 is the first release that asks for **Location**, so the permission-delta gate required a recorded physical-device install. [`../docs/live-guard-2.50-device-evidence.md`](../docs/live-guard-2.50-device-evidence.md) records it: the owner installed `CleanSweep-v2.50.apk` (13,740,236 bytes, SHA-256 `b4c183c876a8da8aff4169c975c1a1111a3229658620e99fed87cb3e645d6176`) over 2.49 with Play Protect on, Play Protect reported "This app looks safe — You can continue to install it", Android's installer completed the update, and the nearby-cell list then appeared with four cells. **The published asset has that same digest**, so the released APK is the installed one. That is one phone: no second device was tested, no device test covers the Play Store bundle, and no screenshot shows an APK hash.

## Build and verify

With JDK 17, the Android SDK and the CI-pinned Gradle version available:

```sh
gradle :cleaner:testDebugUnitTest :cleaner:testReleaseUnitTest
gradle :cleaner:assembleRelease
python3 tools/verify_cleaner_regressions.py
```

Full CI also runs source/localization checks, merged-manifest/DEX gates, package/version/SDK and signing checks. A new local build is not the promoted release bytes and cannot inherit the owner's acceptance or published digest.
