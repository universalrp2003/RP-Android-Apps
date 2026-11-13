# System App Disabler 0.3.0 compatibility evidence

## Scope and publication gate

The owner asked for testing across manufacturers, particularly current Android versions. Stock
Android emulators do **not** contain Samsung One UI, Oppo ColorOS, OnePlus OxygenOS or Xiaomi
HyperOS firmware; no such OEM device is validated by an emulator result below. Device model,
firmware and privileges can change package-manager behavior. Passing these checks does not prove
that disabling, suspending, restoring, rebooting, or preventing a bootloop works on any device.

Publication remains restricted to the exact owner-tested APK. The owner's screenshots demonstrate
that the updated suspension wording appeared on their Xiaomi phone and show HyperComm and Joyose
in its running list, but do not establish the hash of the APK installed there. Do not infer an
owner-tested hash from the screenshots. Keep 0.2.0 published until that identity and the remaining
release checks are established; do not rebuild and publish different bytes.

## Stock emulator smoke

- Candidate: release APK from Actions run `43452727978`, source commit `d49d5cf19b97ae23b19876b84ebf050bdd274054`.
- APK SHA-256: `9c59908b340a0d7ddc5d1033c26a20b60c5bd1b7d72a08276c4add75539c6b7c`.
- [Actions run 43456923915](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43456923915): stock Google APIs emulator Android 15/API 35 and Android 16/API 36 **passed**.
- Each job downloaded that candidate, checked its hash, installed it, launched it, checked the
  setup screen and Apps navigation without Shizuku, checked the app process/log and verified that
  the installed APK hash matched. Screenshots, UI hierarchies and device fingerprints are in the
  Actions artifacts while retained. The checks are only a startup/navigation smoke test.
- The smoke does **not** install or exercise Shizuku, issue a disable/suspend/enable command through
  the app, test reboot persistence, assess the safety ratings, or test an OEM phone. An initial
  Android 16 run and a later Android 15 run failed during UI capture; retries were added and the
  final run passed both. These earlier failures are not evidence of universal app compatibility.

## Stock emulator Shizuku and reboot cycle

- [Actions run 43462158768](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43462158768)
  passed on stock Android 15 and 16, using the exact candidate hash above and Shizuku v13.6.0.
- On each disposable emulator, the test installed an inert user-app fixture, granted Shizuku
  permission, used **System App Disabler's UI** to disable it, checked the OS disabled-package list,
  waited **30 seconds**, rebooted, verified the fixture was still disabled, restarted Shizuku, and
  used the UI to restore the fixture. The OS enabled/disabled lists verified the result; the test
  also pulled the installed Disabler base APK and matched its hash to the candidate. The emulator
  is disposable; test recovery via ADB is not counted as a successful app-driven restore.
- This does not exercise system-package restrictions, the suspension fallback, actual OEM firmware,
  or a physical device. It is one test fixture per stock emulator, not proof of universal safety.
- **Immediate ADB reboot remains unresolved:** [run 43464951948](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43464951948)
  passed with a 15-second added delay on both Android 15 and 16 and with no added delay on Android
  15. The Android 16 job with no added delay failed: the OS showed the installed fixture in its
  default enabled state after reboot, despite reporting it disabled before reboot. The test captures
  UI and state before reboot, so "no added delay" is not literally instantaneous. An earlier
  [run 43461486057](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43461486057)
  also saw this Android 16 result; Android 15's earlier failure used an incremental install, so it
  does not establish whether the fixture was re-enabled or absent. The newer test forces a full
  fixture install. One Android 16 job in run 43463106757 was cancelled after hanging, and another
  job in that run had a UI navigation failure; neither is counted as a persistence result.
- Relevant AOSP code in [Android 16 PackageManagerService](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-16.0.0_r1/services/core/java/com/android/server/pm/PackageManagerService.java)
  schedules non-synchronous package restriction writes with a 10-second delay; the corresponding
  [shell command](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-16.0.0_r1/services/core/java/com/android/server/pm/PackageManagerShellCommand.java)
  calls `setApplicationEnabledSetting` with flags `0`. This offers a plausible explanation for the
  timing difference, **not a proven cause**. These tests use `adb reboot`, not a physical phone's
  power menu, and do not establish behavior on OEM devices. Do not make a general persistence claim.

## Before broader publication

1. Confirm the installed owner's APK hash matches the candidate to be published; confirm the
   remaining owner test script outcomes rather than assuming screenshots cover every action.
2. Investigate the inconsistent immediate-reboot result; test app-driven suspend fallback and
   reboot using disposable test packages and appropriate permissions on real manufacturer devices
   where available. Log exact device/OS, package state before and after, and restore outcome.
3. Clearly distinguish suspension from a full disable in counts and explanations. Ratings must not
   guarantee safety for every model. Preserve a recovery path and do not claim zero bootloop risk.
4. Treat untested OEMs and versions as unverified, not passed. Ship only the owner-tested artifact
   after the actual scope and results are communicated; never weaken release verification.

## Owner screenshot: version and variant are not hash evidence

The owner showed an About screen with 0.3.0 and a file-manager installer dialog for
`appdisabler-debug.apk` from the debug artifact, showing the installed version also as 0.3.0 (3).
This identifies the **file displayed** as a debug APK and shows matching version numbers; it does
not establish whether the installed app came from that file or identify its hash. Debug APKs are
not acceptable publication candidates. A subsequent source change adds an installed-base SHA-256,
`Debuggable` and split-count readout to the existing Shizuku shell diagnostic so a future release
candidate can be identified on the phone. Its hash must be compared with the exact built release
APK after the owner installs and tests that candidate; earlier candidate emulator results do not
transfer automatically to new bytes.
