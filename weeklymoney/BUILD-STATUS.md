# Latest: verified TEST0.7 active-debt labels and imported colour groups

[Download TEST0.7 + separate DEMO0.7](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43803717584/artifacts/14197905836).
See [TEST0.7 full receipt, biometric setup and data safety](FIX-0.7.md).

- I/II only for simultaneous unpaid debts. Fully settled renewals are unmarked; immutable lifetime sequences remain intact.
- Settings Old entry ON/OFF; no per-loan/bulk-ten-week checkbox. Tap a week box for old receipt entry.
- Old entry ON exposes BLUE/RED opening-group selection with audited corrections.
- Within each actual cycle page: blue first, red below; same-place/family customers adjacent in each colour group.
- Summary collections green, new loans purple with a divider.
- Android 9+ authenticated Keystore/BiometricPrompt wrapping, configured in Settings after one master unlock.
  Routine biometric mode hides the password field. Master fallback remains for lockout, enrolment changes, new installs and restores.
- Schema 4 reads old schemas 1–3. Export an external encrypted backup before updating; no uninstall/data clear. Do not downgrade after saving schema 4.

CI 43803717584 passed unit suites, lint, builds, Android 35 regression (1 test, 0 failures/errors) and both APK signature/identity checks.
**Successful hardware biometric enrolment/authentication on the owner's phone is not yet verified.**
Earlier receipts below are historical and superseded where these rules differ. Collector exchange and independent recovery-secret workflows remain unfinished.

---

# Latest: verified TEST0.6 receipt-row identification

[Download TEST0.6 + separate DEMO0.6](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43797310251/artifacts/14194734059).
See [TEST0.6 receipt, changes and upgrade safety](FIX-0.6.md).
Faint customer identity behind the ten receipt columns; no outside book-number/date header in entry;
no bottom aggregate loan amount; weekly collection totals distinguished by a pale-green row, green text and outline.
Individual loan amounts, collection-cycle dates, weekly totals and records are preserved.
CI 43797310251 passed unit suites, lint, builds, Android 35 regression (1 test, zero failures/errors) and both APK signature/identity checks.
Export an external encrypted backup before updating; do not uninstall or clear data.

Earlier receipts below are historical; TEST0.6 supersedes their relevant interface descriptions.

---

# Latest: verified TEST0.5 focused notebook preview

See [TEST0.5 changes, lock semantics and verified receipt](FIX-0.5.md).
[Download TEST0.5 + separate DEMO0.5](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43792552224/artifacts/14193158500).
CI 43792552224 passed compilation, tests, lint, Android 35 native regression and both APK identity/signature checks.
Export an external encrypted backup before updating. No uninstall/data clear is required.
Default auto-lock is five minutes **in the background**, not foreground inactivity; process death/restart still requires a passphrase.
Pinch defaults OFF; fixed +/− and 100% reset are available. File-manager MIME support/performance on the owner's phone still needs confirmation.

The earlier TEST0.4 receipt below is retained as history; TEST0.5 supersedes its UI/locking behaviour.

---

# Current receipt — TEST0.4 calendar and isolated sample

**Verified test preview, not a production release.**

- Exact source `45433f2a997bdaaad93dbeef7cda042ee6458cad`.
- [CI 43784113423](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43784113423): **SUCCESS**, 16 November 2026, 01:09:44 UTC.
- [Artifact 14189262735](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43784113423/artifacts/14189262735): normal TEST0.4 APK, separately packaged DEMO0.4 APK, encrypted invented sample, expected dates/balances, Tamil/English guide and test reports.
- Android 35 comprehensive native regression: 1 test, 0 failures/errors/skips, 66.052 seconds. Unit suites, lint, APK builds, identity and pinned-signature checks passed.
- Corrected strict-next-weekday schedule; book holidays that consume no week; conditional Roman suffixes; Settings history toggle; ten-date amount overview; today's additive receipt and new-balance preview; calendar-aware collection closure.
- Normal APK SHA-256 `cd55ed075a4e85436ee85768dc4e6910b3192b8f61bb88aa2c00b4127482e424`.
- DEMO APK SHA-256 `6c4fce861873c6c6d5a53b19873b86a98af40d6139f4f44507f8e0b2a571b646`.
- Full receipt, migration cautions and remaining limitations: [FIX-0.4.md](FIX-0.4.md).

Do not erase or uninstall the owner's TEST0.3 installation. Try the invented restore
file in the **fresh, separate DEMO0.4 app** first. The regular version-4 APK is signed
for an in-place TEST0.3 update; export an external encrypted backup before updating.
No actual owner-phone upgrade, document-picker restore or production release is claimed.

---

# Current notebook preview status — 15 November 2026

**TEST0.3 notebook/zoom/collection-close preview passed CI. Synthetic records only; not production-ready.**

- Tested source: `faf05110d818419c16a767d61a0c268fb66432bd`.
- CI [43735408657](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43735408657): **SUCCESS**, 16:30:34 UTC.
- Native Android 35: 1 comprehensive test, 0 failures/errors/skips, 80.396 seconds. Physical customer activation, receipt save, duplicate/renewal/history/correction, zoom in/out/reset, collection cancellation/confirmation/repeat-close safety and screenshot persistence covered.
- Unit tests, lint, APK build, four Python identity checks and pinned-certificate verification passed.
- [Download artifact 14170102447](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43735408657/artifacts/14170102447).
- APK SHA-256: `d5cd638ddbede98c3d7bbd409d00b24d8d474086dd940f802da6ed1dfbd59eae`.
- APK filename is **weeklymoney-debug.apk**, determined from AGP metadata, not app-debug.apk.
- Full receipt/install instructions/limits: [FIX-0.3.md](FIX-0.3.md).

TEST0.2 remains the owner-phone-confirmed build; TEST0.3 phone acceptance is not yet established. Install beside it; never require uninstall/data clear. The former row-opening blocker was a test race against an async loan-save dialog, now corrected by waiting for that dialog to close. Scroll axes now have separate layout nodes. Two-finger layout zoom covers 20–220%; collection close appends only current-week blank dashes with count/date confirmation and retained audit history.

Broader production work remains: authenticated collectors and conflict-safe offline exchange, independent recovery secrets/biometrics, English-only UI selection, verified Tamil solar-calendar dates, and full physical paper curl. Private reference photographs remain excluded. No stable release was created.

---

# Weekly Money — first Android preview build receipt

**Not a production release. Synthetic test records only.**

- Portfolio: RP Ideas application **17**, வார வசூல் / Weekly Money.
- Package: `com.universalrp.weeklymoney.preview` (isolated debug package).
- Version: `0.1-preview` / code `1`.
- Exact source: `3129f4756f704d544d73ff057b564c63698a9099`.
- CI: [run 43712248695](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43712248695), conclusion **success**, finished 15 November 2026, 12:09 UTC.
- Tasks passed: `:weeklymoney:testDebugUnitTest`, `:weeklymoney:lintDebug`, `:weeklymoney:assembleDebug`.
- Four Python source-contract checks passed (identity/preview publication status, offline permissions/platform-backup exclusion, resource XML, dedicated module).
- JVM suite covers domain policy/payment/correction/rollover/replay checks, encrypted portable roundtrip/wrong password/tamper rejection, archive/unarchive retention/serialization, and missing-book rejection.
- Artifact [14161215877](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43712248695/artifacts/14161215877), `WeeklyMoney-UNVERIFIED-PREVIEW-not-for-real-records`, 9,522,490 bytes **archive size**, retention 14 days.
- GitHub reports archive SHA-256 `39beb5e39a60306cd55c6e8b3cb5e601718becd350a888521c47c0578353a27c`.
  **This is the artifact ZIP digest, NOT an APK digest.** APK bytes/hash have not been independently inspected in Arena.
- Artifact includes debug APK plus test reports and lint HTML. Download while signed into GitHub, extract ZIP, locate `app-debug.apk`.
- Sandbox download attempt was blocked at GitHub's artifact-storage host; no local APK is presented or claimed downloaded.

## Implemented in this build

Owner-only password unlock; encrypted atomic local snapshot; lock on backgrounding;
screenshot protection; book/loan creation; today's book ordering; prefix search;
receipts/no-payment dashes; exact integer-paise balances; append-only owner payment/date
corrections; audit display; completion; external user-picked encrypted backup export
with readback verification; fresh-install full-backup decrypt/validate/preview/confirm
restore preserving IDs/history; reversible audited book archive/unarchive; no book delete.

Backups currently use the preview passphrase, **not** the final independent master
recovery-key design. Existing-record restore is intentionally refused: safe merge and
collector imports are not implemented. No internet, storage or install permissions.

## Not verified / not finished

No emulator/device tests, physical uninstall/reinstall restore, biometric/master-key
recovery, collector authorization/file exchange, automatic external backup schedule,
conflict/replay importer, realistic paper ledger/page curl, Tamil calendar conversion,
Roman loan/customer matching, editable name/loan metadata, void/reconciliation events,
full guide/About/accessibility checks or long-history performance validation.

The debug signer is not a stable publication identity. No production-signing continuity,
Play Protect clearance, security review, all-device reliability or financial-record
safety is claimed. Do not uninstall an app containing real records to try this preview.
The existing 16 applications and stable release/catalog metadata are unchanged. Unrelated
candidate workflows automatically triggered by broad repository path filters were
cancelled; the generic multi-app build was explicitly skipped on the marked branch push.
No stable publication was requested or performed by this workflow.

## Next acceptance gates

1. Finish reviewed owner/collector key, approval, portable recovery and exchange design.
2. Add safe transactional imports, replay/conflict tests and missing-period recovery.
3. Test backup restore after actual uninstall/new-device setup, including interruptions.
4. Verify persistence/archives/history, larger datasets and restored rollover balances.
5. Finish notebook UI, Tamil dates, search navigation, guide and accessibility.
6. Verify exact signed candidate on emulator and owner phones before real-data use.

Agreed requirements remain in [README](README.md); a successful preview build does not
mean those remaining requirements are completed.
