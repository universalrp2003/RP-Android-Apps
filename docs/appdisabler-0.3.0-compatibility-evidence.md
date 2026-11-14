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

## Candidate with installed-APK identity diagnostic

- [Build run 43529764131](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43529764131),
  source commit `039bd65c6484d9a415b4ef22c1886310fe2c1843`; release APK SHA-256
  `344d1d5cf8c04f5f813a695cc3abe5379be2c2d8b316979ff151bc0915fcdf7a`.
  This is a new candidate, not the APK established as installed on the owner's phone.
- [Stock smoke run 43530059910](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43530059910)
  passed launch/navigation on Android 15 and 16. The [Shizuku cycle run 43530059913](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43530059913)
  verified the in-app installed base hash, `Debuggable: false` and `Installed splits: 0` on both
  emulator versions, and passed the 15-second reboot/restore cycle on both. Android 16's no-added-
  delay cycle passed; Android 15's no-added-delay cycle failed because the fixture returned to the
  default enabled state after the ADB reboot. The overall cycle run therefore failed. The no-delay
  behavior remains inconsistent and is not solved by this diagnostic or the safety-text changes.
- The About and Setup text no longer promises OTA/banking safety or guaranteed recovery. The
  `turned off` count still includes suspensions, so that count and the strength of rating labels
  remain follow-up work. No OEM compatibility conclusion follows from these stock tests.

## Installed APK reported by owner on 14 November 2026

The owner's diagnostic screenshot reports installed base APK SHA-256
`83e302a986df7b9ab7421e0bf1b072a944c9046527cb78baaaff3fd83a80b86c`,
`Debuggable: true`, and `Installed splits: 0`. This is a debug build and does **not** match the
non-debuggable release candidate from run 43529764131 (`344d1d5cf8c04f5f813a695cc3abe5379be2c2d8b316979ff151bc0915fcdf7a`).
The owner did run the new identity diagnostic and showed the corrected About text; neither
establishes the release APK was installed or tested. Do not publish the debug APK or count its
observations as a hash match for the release. No installer action is inferred from a screenshot.

The similarly named debug and release artifacts have now led to repeated confusion. For future
candidate workflows make the debug artifact name explicitly say debug / not for release and keep
release-download directions unambiguous. This is a workflow follow-up, not evidence that existing
artifacts have changed. Avoid rebuilding the candidate under test just to rename an artifact.

## Owner confirmed release identity on 14 November 2026

The owner's subsequent Shizuku diagnostic screenshot reports installed base APK SHA-256
`344d1d5cf8c04f5f813a695cc3abe5379be2c2d8b316979ff151bc0915fcdf7a`, `Debuggable: false`, and
`Installed splits: 0`. The full hash matches the release APK recorded for build run `43529764131`.
The screenshot also shows that the app opened and the Shizuku shell diagnostic ran on this phone.
This establishes the installed candidate's identity and those observed checks only: earlier
screenshots from the debug build do not prove disable/restore or reboot outcomes for these release
bytes. The immediate-ADB-reboot issue and broader OEM testing limitations above remain unresolved.

## Owner's Joyose observation on the hash-matched release candidate

On 14 November 2026 the owner reported Joyose (`com.xiaomi.joyose`) was not running. Their
screenshot shows the toast `Suspended Joyose (this device blocks disable)`, the row says
`suspended — may still run` with an Enable button, and the Settings app shows Force stop greyed
out. This is consistent with the owner's observation, but Settings button state and the app's
saved suspension record are not a direct process-table or independent suspension-state check.
The screenshots do not show the Enable action's result, a later reboot, or behaviour on other
models. The app's imported description says Joyose "can't be stopped"; that categorical statement
is not supported by this observation and needs a cautious wording review. Keep the exact release
hash evidence above; do not rebuild and call new bytes owner-tested.

## State-verification research build — preliminary, superseded

Release candidate build [43537044614](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43537044614), source `13285e5cc6fb5ebafad03ce4806f4f474f90dc71`, APK SHA-256 `a41b1d823f60010b75c5d7ae90ea2facd88dc30c9f02436a6c2e71599258c063`, is **not owner-tested and has been superseded**. Build/test/lint passed. Stock 15/16 [startup smoke 43537234765](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43537234765) passed. In [fixture cycle 43537234748](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43537234748), Android 10, 15 and 16 passed an externally initiated suspension, app-observed state and app-driven unsuspend, followed by disable, 15-second wait, ADB reboot and Enable. Both no-added-delay tests (15 and 16) failed after the emulator reboot: the fixture was again enabled/default. This reproduces the immediate reboot concern, not a fix or proof of its cause.

The Android 9 fixture cycle stopped earlier because that stock image's shell was denied `SUSPEND_APPS`. The previous candidate would attempt unsuspend before enabling a disabled app when suspension was unknown. Source subsequently changed to enable normally disabled packages without requiring an unsuspend on this path, and to state that suspension remains unknown. It needs a new build and retest. The Android 9 failure was not an OEM test. All these runs use disposable fixtures and emulators; they do not prove the behavior of a Xiaomi system package, physical power-menu reboot, multi-suspender policies, or other manufacturer ROMs.

## Corrected candidate for further testing — not owner-tested

Candidate [build 43537944084](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43537944084), source `04c3e07c3a085b47ba7954ca07cf0a217058f499`, produced the release artifact `appdisabler-0.3.0-release-candidate`, artifact id `14090467870`. [Digest probe 43538283537](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43538283537) computed the APK (not ZIP) SHA-256 `70151ee5602f224fe2713d1b6be3a20357db4eefa4bf161275f189fdda859891`, size 11,668,005 bytes. This is different from all earlier owner-tested bytes. Build and lint passed. The builder's digest-commit step lost a concurrent push race; its older `docs/appdisabler-candidate-build-digest.md` is not the pin for this candidate. Further stock emulator testing is in progress and OEM tests remain outstanding. Do not publish as owner-tested.
