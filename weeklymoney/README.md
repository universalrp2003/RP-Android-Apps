# வார வசூல் · Weekly Money

A Tamil-first offline weekly collection notebook with one-editor-at-a-time handoff and an optional manual update check (Android 8+). [Download version 1.7](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/weekly-money-v1.7) · [தமிழ் / English guide](https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html) · [Build and publication verification](BUILD-STATUS.md).

## Scope and use

- Create configurable collection books; today's weekday is listed first. Enter the final repayment total manually; the app does not calculate interest.
- Record customers by place and family, loans, receipts, holidays and corrections. Existing history remains available, and archive is reversible. Same-weekday continuation carries unpaid loans and their history into a new volume.
- Old-entry mode allows historical amounts and dates. The notebook shows ten-week pages, a full payment history and a separate collection-finished confirmation. Optional saved phone numbers are encrypted with the notebook and can be copied for manual dialling.
- Encrypted backups are written to Downloads around changes and collection closure; copy them to another device, USB or Drive. Restore on an empty installation requires **both the encrypted backup and the master password**. Keep both safe: a forgotten password cannot be reset. Backups stored only on the phone can be lost with it.
- Biometric unlock is optional and device-dependent; the master password remains necessary for fresh installations, backup restore and fallback. Internet is used only when the user requests an update check. Screenshots may expose private data when enabled.
- This completed scope is a single-owner notebook; it does not include a collector app or synchronization. Future changes can be added separately.

## Updates and limits

Update the installed public notebook **without uninstalling or clearing data**. Version 1.0 uses the permanent package `com.universalrp.weeklymoney.preview.notebook`, version code 12 and the same signing certificate as the public 0.11 release. A different older signing key cannot update in place. Make an external encrypted backup first; do not downgrade after saving. Version 1.4 writes schema 7 and reads schemas 1–6. Older versions cannot open schema-7 backups; update every sharing device before handoff.

The optional Tamil date table is bounded to supported 2026 dates; ordinary Gregorian dates continue to work. Unit, lint, Android emulator and synthetic same-key upgrade checks are recorded in [BUILD-STATUS.md](BUILD-STATUS.md). They are not independent security/financial certification or proof of functionality on every physical device. Historical versions and their original receipts remain available in Git history and [release pages](https://github.com/universalrp2003/RP-Android-Apps/releases?q=weekly-money).

### Passwords, sharing and updates (1.1)

Changing the master password creates a verified encrypted backup with the new password before replacing local storage. Copy that backup off the phone. Backups made earlier still require the password used when each was created. Re-enable biometric access after a change. A failed backup prevents the password change.

For a handoff, only one owner should edit at a time: send the latest full encrypted backup and its password securely, then stop editing the old copy. The next owner unlocks their own notebook and selects **Open shared backup** in Settings (or opens a `.vvasool` file). The app decrypts it with the backup password, checks that its existing history is an unchanged prefix, and shows a preview. After confirmation, it first saves an encrypted local recovery backup, then replaces the local snapshot using the local master password. Older or divergent histories are refused; this is not a merge or a lock on the other phone. Keep both copies if there is a conflict and reconcile manually. All owners must coordinate which device is current; the app cannot detect unshared changes on another device. Backup passwords can differ between devices.

The login screen's **Check for updates** uses the internet only when tapped; it reads the official public catalog and offers the official release page when a newer version is listed. No notebook data or password is sent. It does not download or install updates automatically. Normal notebook use remains offline. Screenshots are controlled in Settings. After unlocking, the first unarchived book for today's weekday opens automatically, if it has started; otherwise the book list remains available.

Android Back dismisses the current dialog, returns from a linked archived book to its previous book, from the reader to the book list, and from Settings to the main menu. At the main or unlock screen it asks before exiting; cancelling stays in the app.

The notebook owner / business name is chosen once at creation and is displayed on the main and reader screens after unlock. It cannot currently be changed. Restore brings back the original name and notebook identity; a matching name alone does not make independently created notebooks the same. Shared owners use a common business name and take turns; individual editor attribution is not supported.

Version 1.4 adds an undated old-balance page with dated receipts, audited corrections and unpaid-only continuation. Weekly-sheet row ordering is user-controlled within colour groups. Full backups include these records; use 1.4 or newer on every sharing device and keep an external backup before updating.

Version 1.7 keeps the selected page after saving and includes old-balance receipts in the selected-date collection summary. The in-app About screen shows version 1.7 (19).

Version 1.5 adds collection-week shortcuts, grouped old balances, full-identity duplicate checks and indexed search. Pages open with buttons; swipe/page-turn animation is removed.
