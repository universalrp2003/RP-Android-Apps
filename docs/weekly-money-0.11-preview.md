# Weekly Money TEST 0.11 — home artwork and optional contact

Candidate only; public release/catalog remain at owner-approved 0.10 until this update is reviewed. Do not replace the existing public APK.

## Changes

- Centered Tamil home title with a gold rupee medallion and faint local vector coin/banknote artwork on cream/green paper; no image downloads, ads, or network calls. Notebook reader keeps its existing layout.
- Removed the hard-coded, stale `0.8 · Test records only` home subtitle. DEMO retains an explicit dynamic-version sample label. About retains dynamic version and honest TEST/unfinished-feature disclosure.
- Optional contact field below customer name for new/edit loan forms; phone keyboard, no new permissions. Place/family/name rules unchanged.
- Saved contact is a bilingual Copy button inside the customer's receipt/details dialog (open by tapping the customer). Copy is explicitly user initiated; number can then be pasted into Phone. No automatic calling, contacts access or clipboard reads. Android 13+ sensitive-clipboard flag suppresses number preview.
- Contact is preserved when reusing customer details, encrypted backups, corrections (original and edited values), and reference-based archive continuation. Editing/clearing is loan-specific, not an unlogged global overwrite of historical customer records.
- Schema6 reads schemas1–5 with absent contacts defaulting to empty. Older apps reject schema6 rather than silently discard contacts. Keep external encrypted backups and do not downgrade after saving in0.11.
- Same notebook package and persistent signing certificate; versionCode11. Install over0.10, do not uninstall or clear data. DEMO is a separate sample package, not the normal update APK.

## Verification

- Local: 546 tools tests and4 Weekly identity/offline checks passed.
- Added JVM tests: optional/legacy contact decode for schemas1–5; schema6 encode; encrypted round trip; audited contact edit/clear; unchanged balances; invalid/oversized contact rejection.
- Extended native regression: enter/save optional contact, tap to copy and assert exact clipboard, recreate activity, reuse customer and verify persisted contact in edit dialog, alongside existing collection/history/holiday/privacy regression.
- CI [43852699502](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43852699502) **SUCCESS**, source `c482e4b`: unit tests, lint, builds, native Android15 regression1 test/0 failures/0 errors (69.264s). Native test includes exact clipboard assertion and saved contact after recreation/edit. Emulator testing does not prove physical-device clipboard/dialer behavior.
- [Signed handover artifact14219741171](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43852699502/artifacts/14219741171): `notebook/WeeklyMoney-TEST0.11.apk` is the normal update. `demo/WeeklyMoney-DEMO0.11.apk` is optional isolated sample only.
- Notebook SHA-256: `83600bfa2b50a9989cf9cfbffb1a72b1da3f54b27163d5d591e31ba16c3aa2a4`.
- DEMO SHA-256: `f0b428df140c9b861bed0a89684e8cf8fbdd401903c1d1ceb2e76fd5b1997802`.
- Workflow verified permanent signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`, notebook package `com.universalrp.weeklymoney.preview.notebook`, code11. Actual0.10→0.11 in-place device installation is not separately claimed by this regression.

Only synthetic names/numbers are used in tests. User reference screenshots are not committed.
