# Launcher 1.6 candidate gate diagnostics — 1 November 2026

Unpublished Launcher-only work, PR80. Public 1.5 and all prior release assets remain unchanged.
These are synthetic CI results, not physical-phone, audible-volume or Play Protect evidence.

- e011d65 / run41302470428 / job124836104829: `compileReleaseAndroidTestKotlin`,
  `HomeFollowupTest.kt:84:72 Unresolved reference 'label'`. Hoisted the Settings label
  to the enclosing test scope; kept the actual remove-button assertion.
- e628cd6 / run41302753821 / job124836921655: strict `lintRelease` exposed the existing
  `PermissionImpliesUnsupportedChromeOsHardware` CALL_PHONE/telephony declaration mismatch.
  Declared telephony optional rather than suppressing lint or adding permissions.
- ac7598b / run41303021411 / job124837707767: emulator bootstrap exited before instrumentation.
  UID extraction depended on dumpsys' changing `userId` field. Replaced it with an exact
  user-0 `cmd package list packages -U` match (same method as the Store smoke), and retained
  bootstrap stdout/stderr. The subsequent run reached the native test.
- a067002 / run41303512036 / job124839133948: native adjacency assertion compared a text
  child against the other button's full semantic node (`1562` versus `1512` Y centre).
  Prefer the same content-description node type for both buttons. Both controls now also
  share an intrinsic growing height so different line wraps cannot unbalance them.
- 6aa0ea4 / run41303968736 / job124840476608: first native phase PASS (`Time: 63.458`),
  including picker/add/open/recreate/remove and an asserted actual 2.0 font configuration.
  Fresh-process phase failed: expected Settings shortcut, got an empty list. The test had
  removed it through the UI, closed the activity, then called asynchronous `add` directly
  from instrumentation immediately before process termination. Replace that final out-of-
  lifecycle setup with a real UI picker/add before activity close, which invokes Android's
  normal pending-apply lifecycle flush. Keep the same independent fresh-process assertion;
  do not seed or repair preferences between the two phases.

The replacement full gate must pass before any tested-candidate success claim. No failing
run's APK is eligible for publication. Candidate artifacts upload only after both phases pass.
