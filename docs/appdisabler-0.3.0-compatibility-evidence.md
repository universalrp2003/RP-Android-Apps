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

## Before broader publication

1. Confirm the installed owner's APK hash matches the candidate to be published; confirm the
   remaining owner test script outcomes rather than assuming screenshots cover every action.
2. Test app-driven disable, restore, suspend fallback and reboot using a disposable test package
   and appropriate permissions, first on a stock emulator, then real manufacturer devices where
   available. Log exact device/OS, package state before and after, and restore outcome.
3. Clearly distinguish suspension from a full disable in counts and explanations. Ratings must not
   guarantee safety for every model. Preserve a recovery path and do not claim zero bootloop risk.
4. Treat untested OEMs and versions as unverified, not passed. Ship only the owner-tested artifact
   after the actual scope and results are communicated; never weaken release verification.
