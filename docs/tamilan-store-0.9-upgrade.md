# Store upgrade report — release key versus debug preview

1 November 2026. Owner clarification: **upgrading Store itself fails; fresh installation works**.
Four owner screenshots show the redesigned Store, zero eligible catalog updates, the downloaded
`tamilanstore-debug.apk`, Google Play Protect scanning, then **“Harmful app blocked.”**
Raw phone screenshots remain private in temporary uploads; no originals are published here.

## Two independent problems

1. **Upgrade compatibility:** the supplied preview workflow builds `assembleDebug` with Android's
   generated debug signing key. Published Store0.8 uses the established owner release key,
   SHA-256 `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
   A debug-key APK cannot replace a release-key installation of the same package. A higher
   versionCode alone cannot fix incompatible keys. The previous debug preview was unsuitable
   for testing this owner's release upgrade; providing a proper release-key candidate is the
   correction, not trusting two unrelated keys or asking for fresh installations.
2. **Play Protect:** the harmful-app verdict remains unresolved. The screenshots do not show
   an explicit Android certificate-mismatch error, and do not hash-match the device APK or
   establish its install path. The configuration nevertheless establishes that this debug
   build is not a valid release upgrade. Matching the release key does **not** explain or
   clear Google's separate classification. Do not select “Install anyway,” disable scanning,
   change identity for evasion, or promise that another signature/build will remove the block.

The running screenshots are owner feedback on the UI, not Play Protect clearance or a tested
in-place upgrade. SMS Guard1.1.16, Live Guard2.81, Radio1.31 and DailyVault0.5 shown installed
match their catalog versions, so zero eligible app updates in those screenshots is expected.
This request concerns Store itself, not a demonstrated catalog queue failure.

## Corrective candidate (not publication)

- Source version **0.9-rc1**, code **10**, same package `com.universalrp.tamilanstore`.
  New version/code only; immutable 0.8 and existing website/directory links remain untouched.
- Dedicated `tamilanstore-signed-candidate.yml` workflow prepares the existing owner release key
  using configured Actions secrets and fails closed if the key/fingerprint is absent or wrong.
  It verifies the non-debuggable APK's package/version/code/signature and SHA-256, and requires
  identical old/new **merged APK permission sets**, not just source manifest declarations.
- Pin the exact public Store0.8 fixture's size/SHA/signature. On an isolated Android15 CI emulator,
  install that fixture, choose English and paused motion, seed a synthetic private-file marker,
  reproduce rejection of the debug APK as an update without changing the existing install,
  then test **`adb install -r` without uninstall, data clear or bypass flags**. Verify code10,
  retained UID, file contents and both actual preferences before/after launch and restart.
- This tests Android PackageManager signature/version/data continuity, **not** interactive
  file-manager installation or Play Protect approval. No APK is sent to external scanners.
- Keep the debug workflow for native development tests, but rename its artifact
  `tamilan-store-debug-emulator-only` and display a bilingual debug-only warning in its UI.
  Release-key candidates do not display that badge. No package ID, signer policy or permissions
  are changed for warning evasion.

**CI status:** local **281 Python tests**, localization and Python syntax pass. The release-key
build and actual 0.8 upgrade now pass on Android15 CI; see the exact receipt below. This is
not proof of physical-phone installation or Play Protect clearance.

## Owner-device boundary

A release-key candidate is only an in-place upgrade for a Store already signed with that key.
If a fresh debug-signed Store is now installed, the release candidate cannot replace it either;
Android is correctly enforcing the opposite signature mismatch. No automatic conversion between
those identities is possible. Do not recommend uninstall/reset as an update repair. Stop and
identify the installed signing identity before any further installation attempt, preserve
existing data and keep Play Protect enabled. A successful emulator upgrade does not override
a phone warning. No public release, appeal or third-party APK submission is authorized here.

## Exact release-key candidate verification — 1 November 2026, 07:56:01 UTC

[Signed candidate run **41286382567**](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41286382567),
job **124788418954**, **SUCCESS** on tested source
`215f53a08b69b67e1bf00da2b125e33f0e38300c`.

- Exact inner APK: **Tamilan-Store-v0.9-rc1.apk**, **19,309,110 bytes**, code10, non-debuggable.
- APK SHA-256: `49b62f4a4f1e3e3221b3daccc192202cbd545f274f5b52a72b3bfc9d0f233d50`.
- Signer SHA-256: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- Release unit tests, lint, signed assembly, exact old/new signature and identity gates passed.
  **No added or removed merged-APK permissions of any protection level.**
- The debug APK was rejected with **INSTALL_FAILED_UPDATE_INCOMPATIBLE** without changing
  the release installation; this reproduces the signing incompatibility independently of Google.
- The exact release-key candidate then updated pinned Store0.8 using `adb install -r`, without
  uninstall, data clear, downgrade or bypass flags. UID, synthetic private-file hash, English
  preference and paused-motion preference survived, including app launch and restart.
- Candidate SHA-256 was checked again after the emulator test. The same tested bytes, checksum,
  permission comparison and `verification.json` were uploaded, not rebuilt for the artifact.

| Artifact | ID | ZIP bytes | GitHub-reported archive SHA-256 (not inner APK SHA) |
| --- | --- | ---: | --- |
| `Tamilan-Store-0.9-rc1-release-key-upgrade-candidate` | 13075926175 | 17,897,330 | `ccb25b49c346c0c0820149350d2f307ed5e3982d51d1db8fe67a5cca8ea532c6` |
| `tamilan-store-release-key-upgrade-evidence` | 13075856915 | 1,038,155 | `308429d0f1a4f80d1e5c8fb57e1e174b5513d1c73ecebb4adbf8fd641ba26599` |

Receipt and artifact identities were read from Actions API annotations/metadata; the sandbox
has not independently downloaded or visually inspected these archive contents. No APK was
stored under `/home/user`. The candidate is **unpublished** and not discoverable as a public
stable self-update. Public Store0.8 release430268186, APK696852937 (19,276,334 bytes) and
checksum696852940 remain unchanged; no website/directory promotion was performed.

### Earlier CI attempts, not claimed successful

- **41284788752:** upgrade phase failed without a usable harness checkpoint; only build,
  signature and permission gates passed. No installable candidate artifact was offered.
- **41285411789:** diagnostics exposed a transient/nonzero `adb root` startup result before
  either Store APK was installed. Bounded root-readiness recovery now independently checks UID,
  following the existing guarded emulator approach. It never runs on a physical device.
- **41285904240:** the fixture and its real preferences worked, but the probe's `dumpsys`
  UID-label assumption prevented evidence collection before the candidate upgrade. UID evidence
  now comes from exact-package `cmd package list packages -U --user 0`, with executable old/new
  dump-format and missing-field rejection tests. The UID equality assertion was not removed.
- Superseded in-flight runs were cancelled normally. All upgrade, data-preservation, debug
  rejection, permission and signature assertions remain enforced in the final successful run.

The successful run does **not** repair a phone already switched to an unrelated debug key,
provide physical-phone/file-manager-install evidence, or overturn the harmful-app warning.
Do not interpret the candidate link as advice to install through that warning.

### Separate native regression gate

[Debug/native regression run **41286382547**](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41286382547),
job **124788439433**, **SUCCESS** at the same tested source. Debug unit/lint/build, real archive
verification, legacy-signer rejection, read-only installer handoff and the compact bilingual UI /
consent / cancelled-first-install / explicit-resume / two sequential confirmed catalog updates /
200%-font-scale test all passed. Its `tamilan-store-debug-emulator-only` APK is **not** the owner's
upgrade candidate; only artifact **13075926175** above contains the release-key candidate.

> **Current workflow (updated 12 November 2026).** The dedicated candidate job described above
> was renamed per release and now lives at `.github/workflows/store-012-candidate.yml`, pinned to
> versionCode 14 / 0.12. The old `tamilanstore-signed-candidate.yml` still asserted the 0.11
> identities after the module moved to 0.12, so it failed on every Store push and shared a
> concurrency group with its own successor — it has been removed. The guarantees in this
> document (release key, non-debuggable, exact identity, unchanged merged permissions, no
> publication capability) are unchanged and still enforced by the successor workflow.
