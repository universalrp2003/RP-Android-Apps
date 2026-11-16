# TEST 0.9 — candidate, not public release

Publication is on hold until the owner tests and approves. Do not uninstall or clear app data. Export a backup first. Package/signer remain the notebook preview identity; version code 9. Schema 5 still reads schemas 1–4; do not downgrade after saving.

## Changes

- Separate original loan date from cycle's first collection date. For the reported loan, 12-Nov-2026 is disbursement; 19-Nov is first collection, not loan date.
- Tamil civil date beside original date on starting sheets and customer dialog. **Bounded table: 15-Jan–31-Dec-2026 only.** Outside it explicitly says unavailable, without guessing. Source/boundaries in `docs/weekly-money-tamil-calendar.md`.
- Read-only all-payment history independent of the history-entry switch. Includes recorded week, actual date and amount/missed dash through 30/50+ weeks; no invented receipts for missing records. Audit history remains separate.
- Later ten-week pages link to the exact previous sheet for each debt. Fresh volumes have a direct archive-opening action in the reader as well as Settings.
- Settings → Continue in a fresh volume: same weekday only, explicit successor number/date, explanatory confirmation via Save. A fresh verified encrypted Downloads backup is required before handover even if this week's backup already exists. Failure blocks handover.
- Handover archives predecessor and carries only outstanding debts by membership reference. Loan/customer IDs, original dates, originals/corrections, all receipts and balances remain unchanged; no duplicated loans or synthetic opening-balance receipts. Other weekday books are untouched. Future holidays carry forward.
- New volume starts with each inherited debt's latest ten-week sheet. Older history remains accessible in predecessor/archive links and full payment history. Fully paid customers are not carried.
- About lists all 17 RP Ideas application names, without separate application links.

## Important bounds

The archive is still in the same encrypted snapshot. Rendering fewer sheets is not full lazy archive storage or a guarantee of reduced decryption/memory use. Multi-year Tamil calendar coverage is not complete. Historical owner corrections remain possible and affect the same debt viewed from either volume; archive links are not frozen duplicate financial ledgers. A predecessor with an active successor cannot simply be unarchived into another active ledger. Keep the before-volume backup for recovery; no automatic reverse-handover UI is claimed.

Collector exchange, independent recovery secret, physical biometric/OEM and Android 8/9 SAF acceptance remain unfinished or unverified as previously disclosed. This is a TEST candidate, not an independently audited production financial system.
