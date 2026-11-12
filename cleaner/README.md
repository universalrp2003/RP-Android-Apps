# Live Guard (CleanSweep)

Current public APK: **3.00 / code 105**, package `com.universalrp.cleansweep`, Android 8.0/API 26+.

[Download CleanSweep-v3.00.apk](https://github.com/universalrp2003/RP-Android-Apps/releases/download/live-guard-v3.00/CleanSweep-v3.00.apk) · [release notes](../releases/live-guard-v3.00.md) · [guide and screenshots](https://universalrp2003.github.io/RP-Android-Apps/live-guard-guide.html) · [verification](../docs/live-guard-3.00-publication-receipt.json).

## This update — 3.00 (12 November 2026)

**The expired-booster bug is fixed.** A booster that had already ended kept inflating the data limit, so the floating card could disagree with My plan by gigabytes. Reproduced on the owner's phone: My plan said 2.24 GB left while the card said 5.24 GB. An ended booster now adds exactly zero.

**My plan is simpler.** Choose one amount and what resets it:

- **Daily reset** — 1, 1.5, 2, 2.5 or 3 GB, or type your own. Resets at midnight.
- **Total pack** — one amount, or unlimited, until you reset it.
- **Booster** — you enter the amount and last day; after that day it adds nothing.

Start date, day count, end date and operator end-of-day rule are removed. If you entered those on 2.99 they are ignored; your numbers and packs are untouched.

**Both “count from now” buttons now live in one place**, and they really differ: *Start fresh from 0* re-bases today and the pack together; *Keep today's usage* re-bases the pack window only.

**The card and widget agree with the screen.** The widget no longer prints “3 days left” from removed date fields.

**No alarm colour on readings.** 4G and 5G are listed separately and never added together. **SIM-signal choice moved to Settings**, where it states plainly the percentage is a rough signal-strength estimate, not reception reliability.

**Update:** install over **2.99** without uninstalling or clearing data — your saved plan, history and settings are kept. No new permissions. Same signing key, proven to update over the exact published 2.99 APK. 954 JVM tests/93 suites + 16 Android 15 native checks passed. Owner installed the published 3.00 APK on a physical phone — About reads v3.00, screenshots of My plan (65.6 MB used, 2.20 GB left of 2.2 GB), 4G/5G split (40.74 MB / 24.87 MB, never added), and pill size 100%→90% are in the [guide](https://universalrp2003.github.io/RP-Android-Apps/live-guard-guide.html). No on-device hash comparison was taken, so hash-match stays unverified. See [publication receipt](../docs/live-guard-3.00-publication-receipt.json).

## What it does

User-selected junk review/cleanup, device-health readings, permission/security review, network/data tools, optional notifications/widgets and optional AI assistance. Unavailable/OEM-dependent readings are labelled, not invented. Nothing is deleted without selection and confirmation. The app does not include notification/message reading, an Accessibility service, SMS/contact/call-log access or device-admin capability.

News uses current-local-day publication, quality and recent-read filters, preferring native Tamil. All 13 categories are eligible by default; custom selection and balanced category ceilings/rotation are preserved. Up to eight eligible items are selected; missing categories stay empty. RSS publication dates do not independently prove that an event is new, and live sources/complete coverage are not guaranteed.

A configured non-keyless AI provider can edit public news and translate missing specialist headlines, with mechanical guards and a second model comparison. The enhanced path has one shared 60-second budget. These checks reduce errors, not prove perfect fidelity. The existing prepare-all-audio pipeline and Stop cancellation are preserved. Private phone reports are not included in the news payload. Category/source reports describe prepared selection, not proof of playback.

## Updates and safety

The app performs a read-only release-metadata check at most once per rolling 24 hours when opened/resumed. An available-update action opens the official website in the external browser. **Live Guard does not download/install update APKs itself or request installer permission.** The browser download checks the published SHA-256 when supported; a match is file identity, not security approval. Android handles user-confirmed installation. Stop if Play Protect warns; do not bypass it.

Replacement-key versions 2.74 onward can update in place. Do not uninstall or clear data for a same-key update. Only installations using the former exposed key need the documented signing migration: obtain the official replacement first, back up/export important data, and remember that uninstalling may erase local data. Existing release assets remain unchanged.

## Previous release — 2.96 (and why 3.00 replaces it)

2.96 added clearer bilingual rain forecast details. See [live-guard-v2.96.md](../releases/live-guard-v2.96.md) and [rain review](../docs/live-guard-2.96-rain-review.md) for its limits. 3.00 fixes the expired-booster inconsistency found on the owner's device and simplifies My plan; it carries no new permission over 2.99.

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
