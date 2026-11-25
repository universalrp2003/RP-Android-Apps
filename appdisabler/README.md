# System App Disabler (சிஸ்டம் செயலி முடக்கி)

[Download 0.5.0](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/system-app-disabler-v0.5.0) · package `com.universalrp.appdisabler` · version code 6 · Android 8.0+

Use the separately installed [Shizuku](https://shizuku.rikka.app/guide/setup/) to request that Android disable selected packages, without uninstalling them. The device may refuse; the app may attempt suspension instead. A suspended app may still run. Shizuku may need to be started again after reboot.

Apps missing from the UAD database are **Unlisted**, not automatically safe. The optional Google and Google AI links send the app name and package to Google. AI answers can be wrong; research and decide yourself.

## Safety and recovery

Ratings—including Safe—are not safety guarantees. Android Accessibility Suite, for example, provides TalkBack and other accessibility features. The app retains critical-package, bootloop-warning, active-keyboard and enabled-accessibility-service protections, but these cannot cover every dependency or device. Do not disable essential packages. Enable attempts recovery, but recovery can fail; immediate-reboot persistence is not established.

Disabled and suspended states may overlap. Unknown suspension state and stale local records are possible. Counts and process observations are not guarantees that an app cannot run. Android and manufacturer restrictions vary.

## Verification

The published 0.5.0 APK passed a signed 0.4.1 upgrade and four remote focus tests on a disposable TV emulator. The owner reported an earlier candidate working on their TV and remote; this corrected APK was not retested on physical TV. No real package was disabled in these tests. Earlier 0.3.0 testing on one Redmi 13 5G covered a disposable user-app disable, delayed restart, Enable and another delayed restart. Those earlier checks do not establish behavior for this release, immediate-reboot persistence, system-package behavior, or broad OEM compatibility. Experimental synchronous-write research is debug-only and not part of the release.

APK SHA-256: `1f9846d8d995520530ea1d9b2602daf16f8856c4371191c8d0561cba3d97b920`. A checksum identifies bytes, not safety. Update in place if Android accepts the signature; do not uninstall or clear app data to force an update. Do not bypass device security warnings.
