# வார வசூல் · Weekly Money

A Tamil-first, fully offline, single-owner weekly collection notebook (Android 8+). [Download version 1.0](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/weekly-money-v1.0) · [தமிழ் / English guide](https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html) · [Build and publication verification](BUILD-STATUS.md).

## Scope and use

- Create configurable collection books; today's weekday is listed first. Enter the final repayment total manually; the app does not calculate interest.
- Record customers by place and family, loans, receipts, holidays and corrections. Existing history remains available, and archive is reversible. Same-weekday continuation carries unpaid loans and their history into a new volume.
- Old-entry mode allows historical amounts and dates. The notebook shows ten-week pages, a full payment history and a separate collection-finished confirmation. Optional saved phone numbers are encrypted with the notebook and can be copied for manual dialling.
- Encrypted backups are written to Downloads around changes and collection closure; copy them to another device, USB or Drive. Restore on an empty installation requires **both the encrypted backup and the master password**. Keep both safe: a forgotten password cannot be reset. Backups stored only on the phone can be lost with it.
- Biometric unlock is optional and device-dependent; the master password remains necessary for fresh installations, backup restore and fallback. The app has no Internet permission. Screenshots may expose private data when enabled.
- This completed scope is a single-owner notebook; it does not include a collector app or synchronization. Future changes can be added separately.

## Updates and limits

Update the installed public notebook **without uninstalling or clearing data**. Version 1.0 uses the permanent package `com.universalrp.weeklymoney.preview.notebook`, version code 12 and the same signing certificate as the public 0.11 release. A different older signing key cannot update in place. Make an external encrypted backup first; do not downgrade after saving. Schema 6 reads schemas 1–5.

The optional Tamil date table is bounded to supported 2026 dates; ordinary Gregorian dates continue to work. Unit, lint, Android emulator and synthetic same-key upgrade checks are recorded in [BUILD-STATUS.md](BUILD-STATUS.md). They are not independent security/financial certification or proof of functionality on every physical device. Historical versions and their original receipts remain available in Git history and [release pages](https://github.com/universalrp2003/RP-Android-Apps/releases?q=weekly-money).
