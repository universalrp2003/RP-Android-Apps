# TEST0.6 — receipt-row identification and collection totals

Owner-only offline test preview. No ledger schema/calendar/amount/audit changes.

## Screenshot corrections

- Receipt columns 1–10 have a faint light-grey identity line **behind** each customer's amounts: place · family head · customer label. It is confined to that row's receipt area. It has no click action and is excluded from accessibility semantics, so name searches/announcements and customer taps are not duplicated. The actual amounts remain blue/red/brown as before, drawn above the faint text.
- The outer `Book number · starting date` header appears only on home/book selection, **not in the entry/paper window**. The paper's own collection-cycle Start date and weekly dates remain because they identify actual receipt columns.
- Individual customer loan amounts are retained. Only the bottom **summed loan-amount cell** is blank (e.g. the circled ₹25,000 in the reference is no longer shown).
- Weekly collection totals remain in their own row, now with a pale-green fill, dark-green text, a distinct green outline and the label **வசூல் மொத்தம்**. Existing page/search scoping is unchanged. Remaining-balance total stays in that distinguished row; it is not the removed aggregate loan amount.
- Week numbers remain red and dates blue; the new light grey applies to the customer-identification guide, not the payable amounts or calendar headings.

## Upgrade safety

Regular TEST0.6 package is `com.universalrp.weeklymoney.preview.notebook`, version code **6**, using the same pinned notebook signer. Separate DEMO0.6 uses `.preview.sample`. Export an external encrypted backup before updating over TEST0.5; do not uninstall or clear data. The invented schema-3 sample restore file and sample password remain unchanged. No production-readiness, collector exchange, recovery bypass or phone-specific visual acceptance is claimed.

## Verification

**SUCCESS:** https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43797310251

- Tested source: `5a61a36c52707c64120f8b3a2688dd8ddee9c8cf`.
- [Artifact 14194734059](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43797310251/artifacts/14194734059), `WeeklyMoney-0.6-calendar-preview-and-isolated-demo`, 19,859,001 bytes, retention 14 days.
- ZIP SHA-256 `cea5e43928609d89aed682a35291488663952625ece970c4d6e7f2d644eb7ecd`.
- TEST0.6 APK SHA-256 `bee82668c41cfac486830f7db91d2fd7c7d6cc875f1fc0bffca44ae21427a7c1`.
- DEMO0.6 APK SHA-256 `e0176eaa87c8b2e38af2a46c0fe0f779ec693f750994448303dadb073aac3837`.
- Both APK signatures verified against pinned certificate `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- Unit suites, lint and builds passed. Android 35 comprehensive native test: **1 test, 0 failures/errors/skips, 79.005 seconds**. Local Python source-contract suite: **4 passed**.

 Regression adds checks that the entry header is absent, the loan aggregate cell is empty and the first two weekly totals match a synthetic ₹100 receipt and an unentered week (₹0). Existing calendar/history/holiday/summary/locking/zoom/screenshot tests are retained.
