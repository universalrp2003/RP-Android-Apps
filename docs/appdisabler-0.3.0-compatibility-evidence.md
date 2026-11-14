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

## Corrected candidate for further testing

Candidate [build 43537944084](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43537944084), source `04c3e07c3a085b47ba7954ca07cf0a217058f499`, produced the release artifact `appdisabler-0.3.0-release-candidate`, artifact id `14090467870`. [Digest probe 43538283537](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43538283537) computed the APK (not ZIP) SHA-256 `70151ee5602f224fe2713d1b6be3a20357db4eefa4bf161275f189fdda859891`, size 11,668,005 bytes. This is different from the earlier owner-tested bytes. Build and lint passed. The builder's digest-commit step lost a concurrent push race; its older `docs/appdisabler-candidate-build-digest.md` is not the pin for this candidate. See the results and owner observation below; outstanding behavior and OEM tests remain release blockers.

### Corrected candidate stock results

[Smoke 43538333903](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43538333903) passed on Android 15 and 16. [Fixture cycle 43538333889](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43538333889) passed 15-second-delay disable/reboot/restore on Android 9, 10, 15 and 16. On 10, 15 and 16, it also detected an externally suspended fixture and restored it through the app; Android 9 cannot perform that shell suspension on this image and did not test that case. Both no-added-delay Android 15 and Android 16 jobs failed: the fixture was enabled/default after ADB reboot. Thus the persistence issue is unresolved, and this run overall failed. The tests verify the candidate APK hash in-app and via pulled base APK on completed cycles; none tests a manufacturer ROM, a system package, real-device power-menu reboot, multiple suspending actors, or an owner phone.

## Owner screenshot: corrected candidate installed and displayed

On 14 November 2026, the owner's diagnostic screenshot reports the installed base APK SHA-256
`70151ee5602f224fe2713d1b6be3a20357db4eefa4bf161275f189fdda859891`, `Debuggable: false`, and
`Installed splits: 0`, matching the release artifact from run `43537944084`. The app opens and
shows Shizuku ready. In the Apps screenshots it reports 323 installed, 271 pre-installed, 52 added,
5 disabled, 10 suspended and 5 running. The suspended filter displays Earphones as disabled; the
implementation permits overlap and prioritizes the disabled row label. These are app-reported
states, not independent process or package-manager measurements. The screenshots do not show a
new disable, Enable/restore, or reboot result and do not establish behavior on other OEMs.

The screenshots also show Aadhaar, AadhaarFaceRd and an IDBI app labelled `Safe to disable`. These
packages are absent from the bundled UAD list; the current code defaults unlisted non-system apps
to SAFE. This is a generic classification, not a package-specific safety assessment. Track it as
a safety-label issue rather than interpreting it as advice to disable those apps.

## Unlisted and Ask Google AI candidate

Release build [43554849554](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43554849554) from source `d5345069f0f0041ffba6423c3b744f305774dd14` passed Android compilation, unit tests and lint. Release artifact id `14097473782`, `appdisabler-release.apk`, size 11,668,005 bytes, APK SHA-256 `4dec0221f1bdc9bfa42b9d69ee3b0b40bb581a76a5dda7326de7a3ea76f1b912`. This is not the earlier APK whose hash the owner showed. It does not contain a persistence modification. See the later exact-candidate emulator checks and limited owner observations below.

## Bounded synchronous-write research (not shipped)

[Lab run 43554849635](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43554849635) used a temporary shell `app_process` Java probe and an inert fixture, not the Disabler APK. One immediate-ADB-reboot trial for each mode on stock Android 15 and 16 yielded:

| Mode | Android 15 | Android 16 |
| --- | --- | --- |
| `pm disable-user` flags-0 baseline | passed | failed after reboot |
| direct hidden binder disable with `SYNCHRONOUS` | passed | passed |
| `pm suspend` baseline | failed after reboot | failed after reboot |
| `pm suspend`, then hidden restriction flush | passed | passed |

Failures are retained in the workflow; the overall run failed because baseline failures are real results. This is a promising lead, not proof of durability, statistical reliability, or root cause. The probe used shell privileges and hidden framework APIs in a separate process: an ordinary app API call or Shizuku integration has not been validated. The lab did not perform app-driven Enable after reboot, nor test Android 9/10, an OEM system package, vendor firmware, multi-user profiles, physical power-menu reboot or power loss. Do not copy this hidden-API approach into the release as a compatibility guarantee. Future work must test actual Shizuku integration, repeated cycles, failure reporting and restore, plus OEM policy/SELinux and a safe fallback.

A second stock lab run, [43555215803](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43555215803), repeated these modes and also verified shell-driven restore followed by another immediate ADB reboot. Both synchronous-disable/enable and suspension/unsuspension plus flush passed on Android 15 and 16; both baseline modes failed on both versions. This is two small lab trials, with restore tested in the second—not evidence about Shizuku integration, physical devices, or a guaranteed fix. The app candidate does not contain this experimental code.

The candidate's [first stock cycle 43555171637](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43555171637) passed Android 9 but its other jobs failed during UI navigation: after the default filter changed to All, other packages sorted ahead of the test fixture, which was below the viewport. Those navigation failures did not reach the package-action test and must not be counted as package-state failures. The cycle was adjusted to select the user-added filter before the fixture.

The navigation-fixed candidate cycle [43555570692](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43555570692) passed Android 9, 10 and 15 with 15-second delay, and Android 15 and 16 with no added delay. The Android 16 15-second job confirmed disabled state after reboot but failed to continue UI recovery because an emulator wireless-debugging network prompt covered the app; this is a test-navigation failure, not a restore result. Completed jobs verified the fixture's Unlisted label and Ask Google AI button, the confirmation, and app-driven disable/recovery; Android 10, 15 and 16 also checked the combined disabled-and-suspended row. The tests verify button presence, not Google availability or answer quality. The earlier intermittent immediate-reboot failures remain unresolved despite these successful samples.

### Final candidate stock checks

[Smoke 43555171705](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43555171705) passed on Android 15 and 16. After fixing emulator-only prompt handling, [cycle 43556124108](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43556124108) passed on stock Android 9, 10, 15 and 16 with a 15-second delay, plus Android 15 and 16 with no added delay. The exact candidate was hash-checked; completed cycles test a disposable unlisted fixture's label, AI button presence, confirmation, disable, ADB reboot and restore. Android 10/15/16 also exercised overlapping disabled/suspended states; Android 9 could not test suspension under shell permissions. The workflow did not verify Google AI Mode availability or the quality of its output. Although this particular no-added-delay run passed, previous runs on Android 15/16 and the lab baselines failed; immediate-reboot durability is still not established. Those stock workflows did not test manufacturer ROMs or physical-device power-menu reboots; see the later owner observation for the limited physical-device coverage.

## Owner's hash-matched unlisted / Google AI observation

The owner’s 14 November screenshots show installed APK SHA-256
`4dec0221f1bdc9bfa42b9d69ee3b0b40bb581a76a5dda7326de7a3ea76f1b912`, `Debuggable: false`,
and zero splits: this matches release build `43554849554`. The device shows Shizuku ready and
the neutral Unlisted label for Aadhaar, AadhaarFaceRd and HyperComm; Earphones shows both disabled
and suspended (the end of the status is clipped). The running filter displays app-reported running
apps, including suspended HyperComm. Google AI Mode opened in the owner's browser for the IDBI
package and for Live Guard, `com.universalrp.cleansweep`. This confirms the observed links opened
on that phone, not the correctness of the generated responses. The Google AI answer speculates
about OEM/carrier/enterprise origins for Live Guard; the repository's `cleaner/README.md` and
`cleaner/build.gradle.kts` identify it as this project's Live Guard. The banking answer's
absolute safety and no-impact claims are not verified by these screenshots. Treat AI as fallible
research, not an independent safety guarantee.

The screenshots do not show a new disable/Enable cycle or a reboot for this APK. The app is
owner-tested only for the displayed identity, navigation and link observations, not fully validated
for persistence or other OEMs. The unlisted cards have substantial vertical space, and the
combined state text is visibly truncated: retain these as UI follow-ups without rebuilding or
misrepresenting new bytes as the same owner-tested candidate.

## Compact-row candidate and Shizuku persistence lab

Candidate [43557688697](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43557688697), source `3c512b9f5ed1f893ad86fe5d6b90fe87d6d4fb9c`, release artifact id `14098824005`, APK size 11,668,005 bytes, SHA-256 `d09ab49e57e7ac3daed7e4edac4a9d8b7fc9292ccbce6ff2a4b48065977f30a0`. Build, lint and tests passed; a CI gate checks that the debug-only persistence-lab activity is absent from the release APK's DEX and manifest. This APK changes the AI-link row layout and shortens the dual state text; it is not the hash-matched APK shown by the owner. Stock candidate tests and new owner verification are required. It does not implement synchronous persistence.

The initial [debug-only Shizuku lab 43557688749](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43557688749) used Shizuku shell and a probe loaded from the debug APK against a disposable fixture. Suspension plus flush and restoration each survived immediate ADB reboots on both stock Android 15 and 16. The two synchronous-disable jobs reached the post-reboot state check, but their restoration attempts failed because the test activity reached Shizuku before the client attached (`binder haven't been received` / `Not an attached client`). Do not count these as successful disable/restore cycles. The test was updated to retry only those pre-command connection errors. Results remain emulator-only; this is not the release action path, a physical reboot, OEM behavior, or a proven general solution.
