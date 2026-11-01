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
  It verifies the non-debuggable APK's package/version/code/signature and SHA-256.
- Pin the exact public Store0.8 fixture's size/SHA/signature. On an isolated Android15 CI emulator,
  install that fixture, choose English and paused motion, seed a synthetic private-file marker,
  then test **`adb install -r` without uninstall, data clear or bypass flags**. Verify code10,
  retained UID, file contents and both actual preferences before/after launch and restart.
- This tests Android PackageManager signature/version/data continuity, **not** interactive
  file-manager installation or Play Protect approval. No APK is sent to external scanners.
- Keep the debug workflow for native development tests, but rename its artifact
  `tamilan-store-debug-emulator-only` and display a bilingual debug-only warning in its UI.
  Release-key candidates do not display that badge. No package ID, signer policy or permissions
  are changed for warning evasion.

**CI status:** local 274 Python tests, localization and Python syntax pass. New release-key
build/upgrade and the debug native regression run are pending; source tests are not runtime proof.
The candidate artifact must not be represented as verified until those jobs pass.

## Owner-device boundary

A release-key candidate is only an in-place upgrade for a Store already signed with that key.
If a fresh debug-signed Store is now installed, the release candidate cannot replace it either;
Android is correctly enforcing the opposite signature mismatch. No automatic conversion between
those identities is possible. Do not recommend uninstall/reset as an update repair. Stop and
identify the installed signing identity before any further installation attempt, preserve
existing data and keep Play Protect enabled. A successful emulator upgrade does not override
a phone warning. No public release, appeal or third-party APK submission is authorized here.
