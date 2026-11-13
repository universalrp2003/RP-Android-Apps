# System App Disabler 0.2.0 — device verification after a real reboot

**What was verified, by whom, and when.** Owner report, 13 November 2026, on the physical Xiaomi
HyperOS phone the 0.2.0 screenshots came from. APK: `System-App-Disabler-v0.2.0.apk`, 11,651,613 B,
SHA-256 `1f653dd99a68ae5877a5ebd9c178b790e5761866a2dc7c785393d4f298b0b196` — the published release
`system-app-disabler-v0.2.0`, not a rebuild.

## The check

The owner had 15 packages turned off, restarted the phone, and the phone booted and worked normally.

- Turned off at the time: **15** packages (the app's own ⛔ tile).
- Reboot: **completed**, phone started and was usable.
- Result: **no bootloop.**

## Why this matters more than the earlier screenshots

A bootloop is the one failure this app exists to prevent, and it cannot be observed without a real
restart. Everything verified before this point was a static claim — the blocked rows, the bootloop
warnings in the database, the disabled buttons — i.e. that the *guard prevents attempts*. This run
tests the outcome instead: with those 15 packages actually disabled, a cold boot succeeded.

It also exercises the case the guard cannot fully enumerate: a phone whose vendor image reacts
differently to a disabled package shows up here and nowhere else.

## What this does NOT establish

- **Not every package.** Only the 15 that were turned off. Twenty packages are blocked outright and
  were never attempted; a package outside both sets is still untested on this device.
- **One phone, one vendor image.** No second model, no stock AOSP, no other HyperOS version.
- **Not a Play Protect clearance**, and not a statement that any particular package is safe to
  disable. The Safe / Caution / Never ratings are the Universal Android Debloater's own, not ours.
- **No hash read-back on the phone.** The APK is identified by the published release digest, not by
  hashing the installed file on the device.
- Retained data was not audited: this says the phone boots, not that every app kept its state.

## Standing guidance this confirms

Keep the Never list blocked. The value of the guard is that the owner does not have to reason about
which of 2,394 packages is fatal — the twenty known bootloop packages are refused, and this reboot
shows the remaining set behaves sanely on this device.
