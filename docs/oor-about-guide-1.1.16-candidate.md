# Verified Oor1.1.16/code21 — private About/Guide update

Owner said “Ok do” to preparation of a new same-key update after the missed built-in About/Guide correction. **Preparation/verification complete; not public-release authorization.** Public1.1.15, its website/catalog and masked gallery remain unchanged. No automatic merge or promotion. Install this candidate only over the existing same-key app, never uninstall/clear data/downgrade.

## Exact deliverable

- [Candidate ZIP](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41669768816/artifacts/13249211486) — GitHub sign-in may be needed; extract `Oor-Arivippu-v1.1.16-about-guide-candidate.apk`.
- APK: **12891948 bytes**, SHA256 **`3d7119ae7d843ee57c4ca2a69df5ef6bfedf4e90210af20ceb4e74a6f5b42fee`**.
- Signer SHA256: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`; package `com.oorarivippu.voiceannouncer`, no added/removed permissions; existing relay configured, no redeployment.
- Archive: 12311035 bytes/SHA256 `de64f76f955d68b0541ef2efaab188dce6189a092f22d03e9c66a0d8857d8ac5`; expires 2026-12-03T16:12:13Z.
- [Successful exact-source native run](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41669768816), job126034701119. Build/pins source `6858701e2885005ca822cc1958618aaaa4674da2`; application/test source `cb7c8e307719b8f81c4f44e61aaf52eb224203c7`. Retain this exact artifact for any later separately authorized publication; **do not rebuild or substitute a generic unconfigured APK**.

## What is included

Installed-version About, credits/official repositories/labelled legacy mirror retained; shared13OtherApps public links and live own-app section replace stale duplicated tags. Tamil/English optional-family summary, live bilingual web guide/masked gallery link, four family guide topics, bounded truthful weather guidance. Family sharing, weather runtime, relay and other applications unchanged. No new permissions, background receiver, map or precise-coordinate/history sharing. LG font/gap work remains deferred.

## Verification completed

- Same-key actual public1.1.15/code20 → candidate1.1.16/code21 using `adb install -r`; same UID, selected saved settings/file, bilingual consent/default-no-sharing and cold restart retained.
- Baseline native execution seeds an isolated **synthetic** row through the actual1.1.15 Android-Keystore-backed FamilyStore. Exact encrypted ciphertext survives update;1.1.16 successfully decrypts selected pair metadata, remembered ON with no lease and announcedAt high-water. This is **not every real connection's migration**.
- Three actual Android35 native tests, zero skips/failures: actual English/Tamil About version/summary/latest-public/web-guide controls and built-in Guide opened from About, all four family and weather sections expanded; existing Keystore/tamper and two-logical-phone real HTTPS/consent/stop/cache/main-card/persistent-choice/dedupe regressions.
- Thirty family + four About/Guide JVM tests required, zero skips/failures; nineteen backend policy tests and live HTTPS regression;379Python/localization checks; release build/lint passed.
- Separate source report41669764664/job126034686031 confirms98unit passes including4About/Guide, no failures/unexpected skips; exactly one pre-existing opt-in external live-speech probe explicitly not run. No physical audio claim.
- After native tests, read the **installed** base.apk via bounded emulator-only root transport recovery and non-PTY `adb exec-out cat`; exact length and SHA256 equal the inspected/signed archived candidate. No app reinstall/repackaging/test-target uninstall after verification.
- Independent public release432774743/asset705821389 reread:1.1.15 remains12891952bytes/SHA256 `ec625cbfb6faa1bab680b71e4184d945913908662e56f898e695dea932c1abe5`. No original/private screenshot changes or new store submission.

## Limits and earlier held attempts

No independent owner-phone installation/hash, physical GPS, audible speech, all-OEM/background guarantee, Play Protect clearance or security-audit claim. Source-only compile checks are distinct from configured native APK evidence. The earlier run41665557781 passed native/UI but failed its final adb pull;41667865440 failed a fast-scroll Tamil UI lookup. Both strict gates held delivery. Final run uses idle barriers/slow overlapping drags and raw exact installed-byte hashing; **only final artifact13249211486 is the deliverable**. Receipt: `oor-about-guide-1.1.16-candidate-verification.json`.
