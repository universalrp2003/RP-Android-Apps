# TEST 0.10 — owner testing only

- Fully settled debts show derived dashes after the settlement week; no synthetic missed receipts are saved. Later cells cannot open amount entry. The light watermark reads `கடன் தீர்க்கப்பட்டது`. Earlier genuine receipts remain visible and correctable with retained audit history. Correcting a genuine earlier receipt can reopen the balance, so the derived lock is recomputed rather than permanently destroying history.
- Additional amount today is disabled where collection is not allowed (including settled debts). Domain validation also rejects new post-settlement-week records, including dashes.
- Reader heading shows only the date (no loan-date/cycle-first-collection captions). First loan sheet uses disbursement date, not first collection date.
- Red date-only touch links open the debt's preceding ten-week sheet; an inherited sheet can open the precise older page in the original archive. No private sample photos/records are included.
- Settings → Tamil date ON/OFF, default OFF, persisted. ON shows only dates supported by the bounded 2026 table; unknown years are omitted, not fabricated. Gregorian dates and financial records are independent of this switch and continue working next year without a calendar update. No all-year Tamil conversion is claimed.

Same notebook package/signer, code 10, schema 5. Back up first, update without uninstall/clear data, and do not downgrade after saving. No public release/Store publication/website deployment. Prior TEST bounds (same-file archives, unfinished collector/recovery workflows and physical-device acceptance gaps) remain.
