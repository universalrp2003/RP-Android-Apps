# TEST0.3 — notebook, zoom and collection-close preview receipt

**Owner-only synthetic-record Android preview. Not a production release.**

## Verified artifact

- Exact tested source: `faf05110d818419c16a767d61a0c268fb66432bd`.
- CI run: https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43735408657 — **SUCCESS**, finished 15 November 2026 at 16:30:34 UTC.
- Artifact: https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43735408657/artifacts/14170102447
- Artifact name: `WeeklyMoney-0.3-notebook-preview-test-records-only`, 10,142,506 bytes, retention 14 days.
- Artifact ZIP SHA-256: `cf2e0be94eb3d7b52f7fd081a72f9aee376a7b4c7f3a8e8c77dee371f5ebe6a7`.
- APK filename: `outputs/apk/debug/weeklymoney-debug.apk` inside the artifact (folder prefixes can differ when extracted).
- **APK SHA-256:** `d5cd638ddbede98c3d7bbd409d00b24d8d474086dd940f802da6ed1dfbd59eae`.
- Verified pinned signing certificate SHA-256: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- Package: `com.universalrp.weeklymoney.preview.notebook`; version `0.3-preview`, code `3`, label `வார வசூல் · TEST0.3`.
- APK compilation, lint, JVM tests, four Python identity/source-contract checks and Android 35 native regression passed. The native XML reports **1 comprehensive test, 0 failures, 0 errors, 0 skipped**, 80.396 seconds. This is emulator coverage, not owner-phone acceptance or a production security audit.
- Artifact includes reports, APK/hash/certificate receipt and the emulator-only synthetic notebook screenshot when collected. Private reference photographs are not packaged or committed.

## Install without risking TEST0.2

1. Sign in to GitHub and download the artifact ZIP above.
2. Extract it and open `weeklymoney-debug.apk` on the Android phone.
3. Install TEST0.3 **beside TEST0.2**. Do **not** uninstall TEST0.2 or clear its storage. The new package has no automatic migration of the older preview's records.
4. Open the app explicitly labelled **TEST0.3**, create a test owner and use invented customer details only.
5. Keep externally exported encrypted backups outside app-owned folders. This preview's backup still uses its owner passphrase; independent recovery-secret handling is not finished. A secret alone cannot recover lost records without a backup.

## Implemented notebook changes

- Place → family head → customer, manual total, ten numbered weekly cells and remaining balance, with ruled cream paper, centre binding crease and directional page-turn shading. This is a native ruled rendering, **not** a photograph or physically simulated paper curl.
- Horizontal and vertical panning use separate layout/semantics nodes. Two-finger zoom adjusts actual layout density from **20–220%**, allowing a whole-spread view; a 100% button restores readable size.
- Tap a customer or payment cell to open the weekly amount/date editor; Save writes the encrypted snapshot atomically before the dialog closes. Long press opens retained history. Mark-missed buttons avoid needing a minus key on a numeric keyboard.
- Calendar date inputs default to the current date. Separate ten-week entry/edit mode supports existing paper-note history.
- Same-name warning, independent loan IDs, Roman I/II/III identifiers, fully repaid repeat-loan prompt and reusable customer details.
- Owner metadata corrections keep before/after snapshots and recalculate projected balances.
- “வசூல் முடிந்தது / Collection finished” shows the book, collection date and affected blank count before confirmation. Cancel makes no changes. Only blanks in that selected collection week become missed-payment dashes. Existing amounts/dashes, earlier blank weeks, future columns, other books and completed loans remain unchanged. Repeating close creates no duplicate records; when there are no blanks the confirmation is disabled. Corrections remain append-only/audited.
- Collection-week mapping follows each loan's start-date seven-day periods, matching its page columns. It is not a command to fill all ten blank columns.

## Native paths covered

Book validation and save; default native calendar; encrypted reload after activity recreation; screenshot toggle persistence; new loan save followed by **physical customer-cell activation**; receipt save; duplicate-name warning and loan II; early completion and renewal as III; historical ten-week entry; owner metadata correction; two-pointer zoom in and zoom out with reset; synthetic screenshot capture via scoped MediaStore; collection-close cancellation, confirmation and idempotent repeat; verifying loan II's dash while loan III's existing ₹50 remains intact; screenshot protection after another activity recreation.

The original repeated “row opening” failure was a **test synchronization bug**: it tapped the same text in the loan input while the async save dialog was still closing, because the ledger existed behind that dialog. Waiting explicitly for `loanSave` to disappear fixed the false target. Separate scroll layout nodes also fixed ambiguous nested scroll semantics when navigating to offscreen customer columns. The packaging gate now reads the actual APK filename from AGP output metadata instead of assuming `app-debug.apk`.

## Still outside this preview

Authenticated collector mode, conflict-safe operation-file exchange/import association, biometric unlock, independent master recovery secrets, separate English UI selection, verified Tamil solar-calendar dates, full physical paper curl and broader production/recovery acceptance remain unfinished. Existing stable applications/catalogs are unchanged; no public release was created.
