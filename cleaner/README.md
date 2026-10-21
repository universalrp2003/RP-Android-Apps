# Live Guard (CleanSweep)

Live Guard is an Android phone-care and device-monitoring app. Its latest published APK is **2.54 / code 59**, package `com.universalrp.cleansweep`, minimum Android 8.0 (API 26).

- [Download CleanSweep-v2.54.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/live-guard-v2.54/CleanSweep-v2.54.apk) · [release notes](../releases/live-guard-v2.54.md) · earlier releases remain available on the RP-Android-Apps release list
- Build, required unit/regression suites, APK verification and post-publication asset checks passed in [publication run 39526559599](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39526559599). The asset is 13,818,416 bytes with SHA-256 `b7de1d35845caa9f7e2a640c0868e7d32512a168a9aff021c4e4053c011af1b1`; it has not been installed on a phone. The release ledger is [`../docs/published-release-digests.md`](../docs/published-release-digests.md).
- [Privacy policy](../PRIVACY.md) · [installation advisory](../docs/live-guard-play-protect-install-block.md)

## What it does

Live Guard includes user-selected junk review and cleanup, device-health readings, permission/security review, network and data tools, optional notifications/widgets, and optional AI assistance. Measurements that Android or an OEM does not expose are labelled as unavailable or estimated rather than invented. Nothing is deleted without the user's selection and confirmation.

The app does not include notification/message reading, an Accessibility service, SMS/contact/call-log access, or device-admin capability. Optional histories and persistent monitoring remain under the user's control. Network services and data shared when optional features are used are described in the [privacy policy](../PRIVACY.md).

## Updates and safety

The updater checks RP-Android-Apps first and keeps InternetRadio as a verified fallback. It validates a stable release's canonical URL, size, SHA-256, package, newer version and signing certificate before handing installation to Android. It does not install silently or bypass Play Protect. Android may require per-app “Install unknown apps” permission and always presents its installer confirmation.

2.50 is the first release that asks for **Location**, so the permission-delta gate required a recorded physical-device install. [`../docs/live-guard-2.50-device-evidence.md`](../docs/live-guard-2.50-device-evidence.md) records it: the owner installed `CleanSweep-v2.50.apk` (13,740,236 bytes, SHA-256 `b4c183c876a8da8aff4169c975c1a1111a3229658620e99fed87cb3e645d6176`) over 2.49 with Play Protect on, Play Protect reported "This app looks safe — You can continue to install it", Android's installer completed the update, and the nearby-cell list then appeared with four cells. **The published asset has that same digest**, so the released APK is the installed one. That is one phone: no second device was tested, no device test covers the Play Store bundle, and no screenshot shows an APK hash.

## Build and verify

```sh
./gradlew :cleaner:testDebugUnitTest :cleaner:testReleaseUnitTest
./gradlew :cleaner:assembleRelease
```

The full CI workflow also runs the required CleanSweep regression suites, source/localization checks, merged-manifest and DEX gates, package/version/minimum-SDK checks, and stable-signature verification.
