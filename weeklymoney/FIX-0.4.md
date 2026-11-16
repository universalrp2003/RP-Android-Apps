# TEST0.4 — collection-day calendar and isolated demonstration receipt

**Owner-only offline test preview. No production release is claimed.**

## Verified build

- Tested source: `45433f2a997bdaaad93dbeef7cda042ee6458cad`.
- CI: https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43784113423 — **SUCCESS**, completed 16 November 2026 at 01:09:44 UTC.
- Artifact: https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43784113423/artifacts/14189262735
- Name: `WeeklyMoney-0.4-calendar-preview-and-isolated-demo`; size 19,725,933 bytes; retention 14 days.
- ZIP SHA-256: `b37130e332ad498ed40a3a7124a559c7a63e14c26127b1d381cda22d1cbe95b1`.
- Regular APK: `WeeklyMoney-TEST0.4.apk`, package `com.universalrp.weeklymoney.preview.notebook`, version code 4, version name `0.4-preview`.
- Regular APK SHA-256: `cd55ed075a4e85436ee85768dc4e6910b3192b8f61bb88aa2c00b4127482e424`.
- Separate demo APK: `WeeklyMoney-DEMO0.4.apk`, package `com.universalrp.weeklymoney.preview.sample`, version code 4.
- Demo APK SHA-256: `6c4fce861873c6c6d5a53b19873b86a98af40d6139f4f44507f8e0b2a571b646`.
- Both APK signatures verified against pinned SHA-256 certificate `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- Sample restore file SHA-256: `c8059127ec9933bd2838f681ac4aba64e869e1874ea533bf56485635f407c556`.

CI passed compilation, lint, the JVM suites (including 11 notebook-rule tests), four Python identity/source-contract tests and the comprehensive Android 35 native regression: **1 test, 0 failures/errors/skips, 66.052 seconds**. The separate demo build and package/version/certificate checks also passed. Native testing authenticates and decodes the exact downloadable sample file; the Android document-picker restore interaction and an upgrade on the owner's particular phone have not been independently exercised in this run.

## What changed

1. **Roman suffixes only for repeated borrowing by the same customer ID.** A single loan shows just the name. Once a second independent loan exists, that customer's loans show I/II; immutable identities and audit records do not change. Equal text names alone are not an identity.
2. **History entry is a Settings switch, OFF by default.** Normal use has no old ten-week entry checkbox or bulk editor. Turn ON to backfill old paper notes, edit weekly entries and access metadata correction, then return OFF for daily collection. The preference is local to the installation and survives activity recreation.
3. **Schedules use the book's collection weekday, strictly after disbursement.** A Thursday loan dated 05-11-2026 has week 1 on 12-11-2026, week 2 on 19-11-2026. A Monday disbursement in a Thursday book starts collection on the upcoming Thursday, never an arbitrary Monday column.
4. **Book holidays consume no week.** An owner enters a skipped book-day and a reason in Settings → Book holidays. All affected planned dates move to subsequent active collection weekdays. Example: Sunday loan dated 20-09-2026, skipped 08-11-2026: week 7 is 15-11-2026, week 8 is 22-11-2026. No holiday column or missed-payment dash is created for that skipped day. There is no automatic festival assumption.
5. **Holiday changes are append-only and reversible.** No permanent calendar event deletion. A day with an existing loan or actual receipt cannot be marked skipped; the owner must review those records first. Planned dates can move, but actual stored receipt dates, amounts, week identities and before/after history are not automatically reassigned. A resumed duty day is another event with a reason.
6. **Normal customer dialog shows all ten dates above their amount boxes.** Blank future/unentered stays blank; explicit missed is a dash. Actual received dates differing from the corrected schedule are separately labelled. The current ten-week cycle opens, with previous/next cycle controls where applicable.
7. **Today's additional receipt appears below the overview.** It previews the new balance before Save. A second receipt on the same duty date adds to the first amount through a retained before/after revision; it does not replace the original amount silently. Normal Save is disabled where today has no valid collection slot, including disbursement day, an off-weekday or a skipped date. History mode is for owner reconstruction/correction of past duty dates. Existing paid amounts cannot be turned into a missed visit through the normal additional-receipt control.
8. **Collection finished** still confirms book/date/count, only fills that active duty day's blank cells and remains idempotent. It refuses skipped dates and other weekdays; completed loans, not-yet-due new loans and existing amounts are untouched.
9. **Search action dismisses the keyboard before jumping to the paper row**, preventing a zero-height notebook viewport on smaller screens.

The private Drive notebook photo was re-opened as a layout/date-pattern reference. No private customer names, amounts or photograph bytes were included in source, samples or APKs. The sample uses invented Tamil names and mixed paid/missed/unentered cells.

## Safe sample verification

Download the artifact and install **DEMO0.4 first**, beside the existing TEST0.3 app. Do not uninstall TEST0.3, clear its data or restore over it to try the sample. In the fresh DEMO app, before creating an owner, choose **காப்பிலிருந்து மீட்க / Restore a full backup**, select `WeeklyMoney-DEMO.vvasool`, enter the public demonstration passphrase **Demo-Weekly-2026**, decrypt/preview and explicitly confirm restore.

The file contains two invented books and four loans: one complete set of ten old weekly entries with a remaining carry-forward balance; the Thursday 05-11-2026 example; and two independent Sunday loans for the same invented person with the 08-11-2026 holiday. Detailed expected dates and balances are in `samples/மாதிரி-சரிபார்ப்பு.md` and `sample-expectations.json`. If an old loan opens its current carry-forward cycle, press ‹ to view its original cycle 1.

Normal Save works only on that book's actual collection day. On an off-day the user can inspect all dates and type an amount to preview the balance, but cannot record a non-duty-day collection. Use Settings → History entry ON to verify/reconstruct past entries, then OFF for ordinary use.

The regular TEST0.4 APK uses the same package and pinned signer as TEST0.3 and a higher version code, for an in-place update. **Export an external encrypted backup before updating.** No uninstall/data clear is required. Schema 1/2 records remain readable; their original audit amounts and actual dates are retained even where the newly corrected planned schedule differs. Schema 3 adds calendar changes and is not readable by TEST0.3; never downgrade after saving new-format records.

## Native regression scope

Book validation/save and native calendar; encrypted reload; screenshot switch; no first-loan Roman suffix; first due strictly next weekday; no collection on disbursement day; physical customer tap; today's amount/new-balance preview/save; duplicate prompt and I/II; Settings history ON and ten-field correction; Settings OFF and additional receipt leading to early completion; renewal as III without exposing history checkbox; closure cancellation/confirmation/idempotency and preserved missed dash; existing-record holiday guard; two-finger zoom/reset; synthetic screenshot; Android AES authentication/schema decoding of the exact sample file; screenshot protection after activity recreation.

Synthetic fixture backdating inside the test is explicitly test setup, not an app import API or an authorization bypass. Pure JVM tests verify skipped-date shifting/reversal/roundtrip, exact Thursday/Sunday examples, retained actual receipts, forbidden skipped-day loans/closures, additive receipt audits, identity suffix rules and sample expected balances.

## Still unfinished for production

Authenticated collectors and conflict-safe operation-file exchange/import association; biometrics and independent master recovery secrets; English-only UI selection; verified Tamil solar-calendar dates; full physical paper-curl rendering; comprehensive phone-loss/reinstall/upgrade and large-book acceptance. This is a test preview, not permission to keep the only copy of real financial records in it. Keep backups outside app-owned folders and on a second device/location.
