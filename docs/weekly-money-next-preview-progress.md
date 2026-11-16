# Next preview — work in progress, publication held

Owner requested testing before publication. TEST 0.8 phone screenshots confirm the candidate hash, and the owner reported other features working, but requested additional changes. This is NOT final publication approval.

Implemented locally, not yet compiled/native verified:
- Distinguish original disbursement date from first collection date on cycle sheets.
- Per-debt links from later ten-week sheets to their exact preceding sheet.
- Read-only full payment history across all recorded weeks, separate from correction audit history and available when history entry is OFF.
- Regression test for 12 Nov loan / 19 Nov first collection and predecessor at cycle 6.

Still required before delivering another TEST:
- Accurate Tamil month/day using a verified calendar source, without fabricated fixed solar boundaries.
- Same-weekday successor-book operation: archive old volume without touching other weekday books; preserve stable debt/customer identities, original dates, full immutable audits and last ten-week references. Must not duplicate debt or receipts.
- Lazy archive loading (current NotebookState loads all data; do not claim performance isolation yet).
- Atomic backup/restore/schema migration and undo/correction rules for volume transfer; unit/native tests.
- Version/code advance and next candidate CI. Do not overwrite TEST 0.8 artifact identity.

Pending Store 0.14/catalog-v3/website files are separate unfinished work and must NOT be published until explicit owner approval after next TEST.
