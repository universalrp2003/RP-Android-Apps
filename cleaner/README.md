# Live Guard (CleanSweep)

Live Guard is an Android phone-care and device-monitoring app. Its latest published APK is **2.49 / code 54**, package `com.universalrp.cleansweep`, minimum Android 8.0 (API 26).

- [Download CleanSweep-v2.49.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/live-guard-v2.49/CleanSweep-v2.49.apk) · [release notes](../releases/live-guard-v2.49.md) · the previous 2.46 release stays available on the InternetRadio fallback
- Build, regression, permission-delta and APK checks passed in [publication run 39272003076](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39272003076). Published asset size and SHA-256 are in [`../docs/published-release-digests.md`](../docs/published-release-digests.md); no exact-build phone test is claimed.
- [Privacy policy](../PRIVACY.md) · [installation advisory](../docs/live-guard-play-protect-install-block.md)

## What it does

Live Guard includes user-selected junk review and cleanup, device-health readings, permission/security review, network and data tools, optional notifications/widgets, and optional AI assistance. Measurements that Android or an OEM does not expose are labelled as unavailable or estimated rather than invented. Nothing is deleted without the user's selection and confirmation.

The app does not include notification/message reading, an Accessibility service, SMS/contact/call-log access, or device-admin capability. Optional histories and persistent monitoring remain under the user's control. Network services and data shared when optional features are used are described in the [privacy policy](../PRIVACY.md).

## Updates and safety

The updater checks RP-Android-Apps first and keeps InternetRadio as a verified fallback. It validates a stable release's canonical URL, size, SHA-256, package, newer version and signing certificate before handing installation to Android. It does not install silently or bypass Play Protect. Android may require per-app “Install unknown apps” permission and always presents its installer confirmation.

The 2.49 permission-delta gate passed without requiring a new physical-device evidence record: 2.49 adds no dangerous runtime permission that 2.46 did not already declare. The publication run re-read its own public release and verified the asset name, size and SHA-256. The owner reports installing 2.49 on his phone and using it; CI/API validation cannot hash-match the installed bytes to the published asset.

## Build and verify

```sh
./gradlew :cleaner:testDebugUnitTest :cleaner:testReleaseUnitTest
./gradlew :cleaner:assembleRelease
```

The full CI workflow also runs the required CleanSweep regression suites, source/localization checks, merged-manifest and DEX gates, package/version/minimum-SDK checks, and stable-signature verification.
