# Superseded progress log

Current state: TEST0.10 is public after owner approval. See [publication receipt](weekly-money-0.10-publication-receipt.json) and [permanent signing policy](weekly-money-signing-policy.md). The publication holds below describe earlier stages, not the current authorization. Multi-year Tamil dates and separate archive storage remain bounded as stated in the current release notes.

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

## TEST 0.9 implementation update

Name-only About directory added (17 apps). Same-weekday continuation is represented by schema-5 membership events, never by copied loans or opening-balance receipts. It archives the predecessor, leaves other weekdays alone, preserves loan/customer IDs and all audits, and exposes the inherited latest ten-week sheet plus an archive link. Schema 1–4 are still readable; do not downgrade after saving schema 5. Unit tests cover balance preservation, chains, paid exclusion and backup round-trip; native test now exercises fresh-volume creation and archive opening.

Tamil civil-date display currently has a bounded 15-Jan–31-Dec-2026 offline table; outside that range it explicitly reports unavailable rather than guessing. See calendar source document. Universal multi-year coverage is NOT complete. Archive records still reside in the encrypted snapshot: this change reduces rendered sheets, not total file/decryption memory. Separate lazy archive storage is NOT complete. Do not claim either limitation solved.

Publication remains held for owner testing. Pending Store 0.14 is not part of this preview verification.
