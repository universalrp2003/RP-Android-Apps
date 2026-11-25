# System App Disabler (சிஸ்டம் செயலி முடக்கி)

[Download 0.4.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/system-app-disabler-v0.4.1) · package `com.universalrp.appdisabler` · version code 5 · Android 8.0+

Use the separately installed [Shizuku](https://shizuku.rikka.app/guide/setup/) to request that Android disable selected packages, without uninstalling them. The device may refuse; the app may attempt suspension instead. A suspended app may still run. Shizuku may need to be started again after reboot.

Apps missing from the UAD database are **Unlisted**, not automatically safe. The optional Google and Google AI links send the app name and package to Google. AI answers can be wrong; research and decide yourself.

## Safety and recovery

Ratings—including Safe—are not safety guarantees. Android Accessibility Suite, for example, provides TalkBack and other accessibility features. The app retains critical-package, bootloop-warning, active-keyboard and enabled-accessibility-service protections, but these cannot cover every dependency or device. Do not disable essential packages. Enable attempts recovery, but recovery can fail; immediate-reboot persistence is not established.

Disabled and suspended states may overlap. Unknown suspension state and stale local records are possible. Counts and process observations are not guarantees that an app cannot run. Android and manufacturer restrictions vary.

## Verification

The published 0.4.1 APK was tested as a signed 0.3.0 upgrade on an Android 15 emulator; this exact release was not phone-tested. Earlier 0.3.0 testing on one Redmi 13 5G covered a disposable user-app disable, delayed restart, Enable and another delayed restart. Those earlier checks do not establish behavior for this release, immediate-reboot persistence, system-package behavior, or broad OEM compatibility. Experimental synchronous-write research is debug-only and not part of the release.

APK SHA-256: `d4a51560e1cd77eb240d2132cfad568796be5f4f642f6dfce5bcecdbd5294aea`. A checksum identifies bytes, not safety. Update in place if Android accepts the signature; do not uninstall or clear app data to force an update. Do not bypass device security warnings.
