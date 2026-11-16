# TEST 0.10 — owner testing only

- Fully settled debts show derived dashes after the settlement week; no synthetic missed receipts are saved. Later cells cannot open amount entry. The light watermark reads `கடன் தீர்க்கப்பட்டது`. Earlier genuine receipts remain visible and correctable with retained audit history. Correcting a genuine earlier receipt can reopen the balance, so the derived lock is recomputed rather than permanently destroying history.
- Additional amount today is disabled where collection is not allowed (including settled debts). Domain validation also rejects new post-settlement-week records, including dashes.
- Reader heading shows only the date (no loan-date/cycle-first-collection captions). First loan sheet uses disbursement date, not first collection date.
- Red date-only touch links open the debt's preceding ten-week sheet; an inherited sheet can open the precise older page in the original archive. No private sample photos/records are included.
- Settings → Tamil date ON/OFF, default OFF, persisted. ON shows only dates supported by the bounded 2026 table; unknown years are omitted, not fabricated. Gregorian dates and financial records are independent of this switch and continue working next year without a calendar update. No all-year Tamil conversion is claimed.

Same notebook package/signer, code 10, schema 5. Back up first, update without uninstall/clear data, and do not downgrade after saving. Owner authorized public TEST distribution after phone testing. Store and website rollout are tracked separately. Prior TEST bounds (same-file archives, unfinished collector/recovery workflows and physical-device acceptance gaps) remain.

## Install and future updates

Exact owner-tested CI bytes, not rebuilt or re-signed. Package `com.universalrp.weeklymoney.preview.notebook`, code10. The preview package remains the update identity; no switch to the formerly planned production package. Persistent signer SHA-256 `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`. Future updates must use the same package/key and higher versionCode. Update in place; do not uninstall or clear data. Export a backup as a safety precaution, not as a routine reinstall requirement. No universal guarantee for other signers, Android downgrades or OEM restrictions.

Collector exchange and independent recovery secret remain unfinished; this public release remains visibly TEST-labelled. Offline Guide is sufficient. Optional browser guide: https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html (website rollout tracked separately).
