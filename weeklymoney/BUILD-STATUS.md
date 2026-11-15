# Current notebook preview status — 15 November 2026

**TEST0.3 is blocked; do not distribute for real records. TEST0.2 remains the last phone-confirmed build.**

Latest requested behavior added to source:
- Two-finger zoom, bounded 65–220%, with a 100% reset; scaled layout rather than a visual-only transform so cells scale with their hit areas.
- Customer/cell activation is intended to open the amount editor; this remains blocked by the native regression below.
- “வசூல் முடிந்தது / Collection finished” opens a date/count confirmation scoped to the selected book.
- Confirming appends explicit missed-payment records only for that collection week's still-blank cells of active, started loans. Existing receipts/dashes, completed loans, older blanks, future weeks and other books stay unchanged. Future dates and archived books are refused. Repeating the operation is idempotent. A missed payment does not reduce debt; it remains correctable through retained audit history.
- Week selection currently follows each loan's start-date seven-day periods, matching the notebook page model. This is not a blanket fill of all ten columns.

Validation: seven notebook rule tests (including collection-close/idempotency/protection cases), other JVM tests, lint, APK compilation and four Python identity checks passed in CI at `138ca4d` (run 43729725532). **Native emulator regression still fails at the first receipt editor opening.** No successful TEST0.3 APK artifact or final digest/signer receipt has been produced. Pinch and collection-close UI behavior have not yet passed native gesture coverage. Existing ruled horizontal pages are not yet a verified photographic/paper-curl match.

Remaining broader production work includes independent recovery secrets, biometric unlock, collector authority and conflict-safe offline operations exchange. Do not uninstall or clear the installed TEST0.2 app to try this source.

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
