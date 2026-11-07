# Live Guard 2.97 / code 102 — rain home controls

## Scope and behavior

Owner request: remove a saved rain-alert home and offer an explicit way to save the current GPS place as home. No user screenshots are published.

- The Rain & safety screen offers **Remove saved home** with locality and confirmation. Cancel preserves home. Removing clears only home coordinates/label, its cached forecasts, episode/throttle state and notification. Other saved watches, GPS-follow choice, lead time, weather key and voice preferences remain. Existing home-first weather checks resume when a home is chosen again.
- **Save current GPS place as home** is available even without a home. It requests existing location permission if necessary and runs a user-started, bounded lookup through the existing resolver with `gpsOnly=true`. A fallback home, unknown/generic locality, invalid coordinates, denied permission, disabled Location or absent fresh fix is not offered as current GPS.
- A named place is shown for review. Only confirmation saves/replaces home; it checks current fix validity again. Cancel, failed lookup and expired/moved fix preserve home. GPS following is not enabled by saving home.
- Clearing/replacing home and worker result/notification writes use the same lock, preventing an old in-flight result from restoring the removed home forecast. Queued speech still uses the existing relevance checks.
- Both dialogs keep their body scrollable and their actions reachable at large font sizes. Existing forecast details, manual IMD/SACHET links, strict voice-source/freshness rules, quiet hours and deduplication are unchanged. No new provider endpoint, permission, background poller or diagnostic upload.

## Exact signed candidate and upgrade

[Candidate 42378243860](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/42378243860), job128420199094, source `7d532c29bf37d76abbbba56508e156237b6640b9`, artifact13577480015. Archive SHA-256 `826f6dcb1cdbab1f6c8ce07931f847ea1002eb7e22867f6c6678d2074e19757d`.

- **940 required JVM tests / 92 suites** actually ran without missing/skipped required cases, including 13 new GPS eligibility and home persistence cases. Release lint and signed release/instrumentation compilation passed.
- The full merged permission set equals immutable public2.96. Both APKs verify signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- A normal `adb install -r` over exact public2.96 (SHA `92c7b0ffb4a2d714fe8dbcab190161558c06b7cb086754b1cbf61d30b2da384c`) preserved UID, synthetic file, existing synthetic preferences and restart behavior. No uninstall, data clear, downgrade or warning bypass.
- Retained native settings/news/security/disaster-window smoke passed. **14 required Android35 native tests** passed: five official-status, four pill defaults/rendering, one bilingual rain-detail and four home-control cases. Home controls exercise remove/cancel, replace/recreate without home, expired/moved-fix rejection and missing-fix retry in both English/Tamil at100%/200% fonts. GPS lookup values in these UI tests are synthetic, not hardware-location proof.
- The raw installed APK matched the archive before instrumentation; each required native test checks its target APK hash. Receipts retain this distinction from owner-phone installation.

## Exact publication and website

[Publisher42379406992](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/42379406992), job128423662833, succeeded. Public release437610698 / APK720860166 at2026-11-07T13:59:18Z. Published the checked archive bytes without rebuilding; all previous public release assets remain unchanged.

`CleanSweep-v2.97.apk`: **14,784,299 bytes**, SHA-256 `121b2c757996910849c76b70f8aa980548e65a84f5fc1e8e0bd6ffda793e92fe`.

Publication pins, candidate verification and publication receipts are separate JSON records alongside this document. Website, current online Store feeds, shared release directory and Pages mirrors move to those exact public bytes. The accepted Store APK and its older offline fallback are not rebuilt. The older2.96 Pages mirror is retired to retain the existing800MB budget; its immutable GitHub APK/checksum assets remain available. Later source/metadata builds are not the canonical published APK.

## Limits and safe installation

No independent installation/hash match on the owner's phone, real GPS accuracy, actual rain/weather accuracy, audible delivery, all-OEM/background reliability or Google/Play Protect clearance is claimed. Forecasts remain forecasts, not local measurements or automatically imported official warnings.

Existing same-key2.96 users update normally without uninstalling or clearing data. Review the GPS locality before saving home. If Android/Play Protect warns, stop; do not bypass it. Main Pages deployment must still pass strict source-equal live metadata and exact mirrored APK/ZIP checks before website completion is reported.
