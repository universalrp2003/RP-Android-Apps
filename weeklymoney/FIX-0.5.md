# TEST0.5 — focused notebook and daily summary

Owner-only offline preview; not a production release. This document supersedes the TEST0.4 interface description, not its retained calendar/audit contract.

## Changes requested from phone screenshots

- **Separate home and paper view.** Home contains the app title, screenshot preference, book creation/selection and Settings. Selecting a book opens only that book's paper, compact back/heading/summary/new-loan controls, search and collection-close action. Back returns to book selection; it does not require unlocking. No other book's tabs or settings occupy the paper view.
- **Background auto-lock grace.** Settings offers immediately, 1, 5, 15 or 30 minutes, default **5 minutes**. A brief switch to another app/document picker no longer always destroys the unlocked session. The timeout uses monotonic elapsed time and a scheduled lock; expiry clears the in-memory passphrase/state (or waits for an in-flight disk write to finish safely). Explicit Lock remains immediate. Process death, activity recreation or phone restart still require the passphrase: the secret is never stored in preferences. This is a **background** timeout, not an inactivity timer while the app remains in the foreground. No biometrics or recovery bypass is implied.
- **Automatic editable book heading.** New-book heading defaults to the selected Tamil weekday and follows weekday changes until the user edits it. Once edited, selecting another weekday does not overwrite the custom heading. An explicitly blank heading stays allowed.
- **Clearer ruled cells.** Week numbers are centred, bold red; dates are separately centred, bold blue. Cell text is bold and minimum ruled-row height is reduced from 64dp to 48dp.
- **Page totals row.** After the visible customer rows, totals show opening amount, each of the ten weekly columns and remaining balance. Totals reflect the current page and search results, not an unlabelled all-book aggregate. Explicit missed/unentered cells contribute zero without changing their original display/state.
- **Daily summary.** A Summary button is available on the paper toolbar. Completing a collection automatically opens the summary for the chosen closure date. It shows that book/date's total paid amount, payer names and amounts, and new loans with names and manual repayment totals. The date can be selected to review another day. It uses effective corrected loans and the latest audited receipt versions, not a sum of every revision. It is a current ledger summary, not an immutable cash transaction journal.
- **Lower-work zoom option.** Fixed minus/plus controls change layout scale in 15-point steps from 40% to 160%; 100% reset remains. Two-finger pinch is **OFF by default**, with explicit ON/OFF control; enabled pinch retains the existing 20–220% range. No measured CPU reduction or universal phone smoothness is claimed. Rendering still uses layout scaling for readable text rather than magnifying a bitmap.
- **File association.** Android VIEW registration accepts `application/x-vvasool` and generic `application/octet-stream` on content/file URIs. An external file open on an empty installation reads bounded ciphertext, asks for its passphrase, displays the existing backup preview and requires explicit confirmation. It does not auto-restore. Existing local records are never replaced: the app explains that an empty/demo installation is required. File managers that mislabel the file as another MIME type may still require the app's own Restore picker; Android URI/MIME matching varies by provider. Collector-operation imports and merge/conflict handling remain unfinished.

## Installation safety

Normal TEST0.5 uses `com.universalrp.weeklymoney.preview.notebook`, version code **5**, with the pinned existing notebook signer. DEMO0.5 uses `.preview.sample` and separate app storage. Export an external encrypted backup before installing the normal APK over TEST0.4. Do not uninstall, clear storage, downgrade schemas or restore invented samples over existing records. The existing schema-3 `samples/WeeklyMoney-DEMO.vvasool` and its public sample passphrase `Demo-Weekly-2026` remain unchanged.

## Verification

**CI SUCCESS:** https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43792552224

- Tested source: `91f531e0b66412598ea5650ce6620296a8be3e3c`.
- Artifact: https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43792552224/artifacts/14193158500
- Artifact name: `WeeklyMoney-0.5-calendar-preview-and-isolated-demo`, 19,978,188 bytes; 14-day retention.
- ZIP SHA-256: `ceefb2ce415e2593ee3fe291d1e705192e9815d5c5e8daaba5bc1fc42fbe410d`.
- TEST0.5 APK SHA-256: `db09f16bda3522bd1e124777575eca4ad540a59057b0300d7402e5218a8c7b73`.
- DEMO0.5 APK SHA-256: `dd571718fd1253301a4d0a97a97a5e977a2d2a86832d0246b339e7a422467fc9`.
- Both signatures verified: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- Unit tests/lint/build and source identity contract passed. Comprehensive Android 35 test: **1 test, 0 failures/errors/skips, 131.435 seconds**. Local Python source-contract checks: **4 passed**.

 Native coverage has been adapted for the new home/paper separation and includes summary visibility, brief-background session retention, immediate background lock, screenshot protection and pinch opt-in, in addition to the calendar/history/holiday/loan regression retained from TEST0.4. A passing emulator run is not a verification of the owner's particular file manager, phone performance, or an in-place phone upgrade.
