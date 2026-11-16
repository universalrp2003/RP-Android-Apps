# வார வசூல் — SAMPLE ONLY / மாதிரி மட்டும்

**These are invented identities and amounts, not copies of the private notebook photos.**

Passphrase for this demonstration file only: **Demo-Weekly-2026**

1. Keep TEST0.3/TEST0.4 and its existing records. Never clear its data to try this sample.
2. Install the separately labelled **வார வசூல் · DEMO 0.4** APK (`.preview.sample`). It has separate local storage from `.preview.notebook`.
3. In the fresh DEMO app choose **Restore backup**, select `WeeklyMoney-DEMO.vvasool`, enter the passphrase above, decrypt/preview, and explicitly confirm restore. Do not create a new owner first; full-backup restore is empty-install-only and will not overwrite an existing book.
4. Use this public demo password only with these invented records, never for real financial records.
5. Settings → History entry ON exposes the old ten-week editing mode. It is OFF by default. Screenshots can be enabled for reporting a bug.

## What to verify

- `DEMO-THU`: “மாதிரி அ” has ten old weekly entries, including explicit missed dashes, with a remaining balance/carry-forward. Because this person has only one loan there is **no I suffix**.
- `DEMO-THU`: “மாதிரி ஆ”, loan date **05-11-2026**, has first due **12-11-2026**, second due **19-11-2026**. All ten dates are Thursdays, not dates derived by adding a week to a Monday disbursement.
- `DEMO-SUN`: “மாதிரி இ I” starts **20-09-2026**. Weeks 1–6 are **27-09, 04-10, 11-10, 18-10, 25-10, 01-11**. Book holiday **08-11-2026** is omitted: week **7 = 15-11-2026**, week **8 = 22-11**, week **9 = 29-11**, week **10 = 06-12**.
- “மாதிரி இ II” is a second independent loan for that same customer, so **only this repeated customer** gets I/II. Its schedule uses the same book-wide skipped date. Names alone are never used as an ID.
- In the customer dialog all ten scheduled dates appear above their amount boxes. An unentered future box stays empty; an explicit missed visit is `–`. Actual received dates, if different, are separately labelled rather than rewriting the scheduled date.
- Normal mode shows today's **additional receipt** below the ten-week overview and previews the new balance. It is enabled only when today has a valid collection slot. A second receipt adds to the earlier amount and retains the before/after history.
- A book holiday is different from an unpaid customer: it consumes **no week**. “Collection finished” only fills blanks for an active duty date and never fills the holiday. Holidays are set manually; no automatic festival assumption is made.
- Settings → Book holidays records an auditable skip or reversal with a reason. A day already containing a loan or receipt cannot be marked skipped. Planned dates can move; stored actual receipt amounts/dates are never automatically reassigned.

Exact ten-date schedules and expected balances for each invented loan are in `sample-expectations.json` (amounts there are rupees).

Schema 3 includes holiday changes. TEST0.3 does **not** support this file; use TEST0.4 / DEMO0.4. TEST0.4 can still read schema 1/2 backups without changing original payment audit events. Existing old preview dates may disagree with the newly corrected planned schedule; their actual dates are retained and shown separately.
