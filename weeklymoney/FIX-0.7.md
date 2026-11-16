# TEST0.7 — outstanding-debt marks, imported ink groups and biometric unlock

Owner-only offline test preview. Collector exchange and independent recovery-secret workflows are not a completed production release.

## Updated rules

### Roman marks mean simultaneous outstanding loans

Only **currently unpaid** loans for the same immutable customer ID are counted. One unpaid loan has no suffix, even after earlier fully settled loans. Two unpaid loans show I/II in immutable creation-sequence order; completed loans show no Roman suffix. Once a loan is settled, the remaining active labels are recalculated. This changes only presentation: original loan IDs, lifetime sequence numbers, receipts and revision history are retained. Labels are not stable IDs.

### Old-entry ON/OFF and blue/red selection

Settings now says **பழைய பதிவு உள்ளிட / Old entry ON/OFF**, default OFF. There is no additional per-loan “10-week old entry” checkbox or bulk-ten-week checkbox. The ten-date overview stays visible. With Old entry ON, tap an amount box to select that week, enter its actual amount/date, Save, then select the next week as needed. Blank leaves a record unchanged; missed remains an explicit dash.

When creating or correcting a loan with Old entry ON, select:

- **நீலம் · புதிய பதிவு** — BLUE, the default.
- **சிவப்பு · 10 வாரம் கடந்த பாக்கி** — RED opening group for an imported already-overdue balance.

The selection is persisted as `openingInk`, not inferred from a fictitious extra payment/week. It does not move loan/receipt dates or silently reduce the manual total. Enter the original manual repayment total, original loan date and known old receipts; do not treat an already-reduced balance as a new original total while also importing those receipts. Editing the colour retains before/after metadata in loan correction history. With Old entry OFF, existing ink is retained on edits and new loans default BLUE. Real carry-forward cycles after week 10 continue to appear red automatically.

### Grouping

Within **each actual collection-cycle page**, BLUE rows come first and RED/carry-forward rows follow with a red group caption. Within each colour group, normalized place → family head → customer name → loan date/sequence sorts rows so the same place is adjacent. For example Pudukkottai/Rajesh/Praba and Pudukkottai/Murugesh/Pappa stay in the same place group instead of being mixed with Alangudi. Grouping does not merge different collection-cycle pages, identities, loans, balances or weekly dates. The red/blue presentation does not change amounts.

### Summary colours

Collection total and payer lines are dark green. A divider separates the purple **புதிய கடன்கள்** heading and new-loan lines. These are distinct data categories; no netting of new-loan totals against collections is implied.

## Biometric setup and security

On **Android 9/API 28 or later**, unlock the existing notebook once with its master passphrase, then use **Settings → Biometric இணைக்க / Enable** and approve the phone's system biometric prompt. Subsequent ordinary launches/locks automatically offer that phone's biometric authentication. The password field is hidden in normal biometric mode; a labelled recovery action exposes master-password fallback. Existing background-lock grace settings remain applicable.

This is actual Android Keystore authenticated wrapping, not a bypass after a successful-looking callback:

- A non-exportable AES-GCM key in AndroidKeyStore requires per-use biometric authentication (strong biometrics on API 30+). The platform `BiometricPrompt.CryptoObject` authenticates the cipher operation.
- Only ciphertext and IV for the local notebook passphrase are stored in private preferences. The wrapping key and ciphertext are not part of the portable notebook backup; platform app backup remains disabled. The app still has no INTERNET/storage permission; USE_BIOMETRIC is its only manifest permission.
- New installation and portable backup restore require the original master passphrase. Set up biometrics again on a new device; an old device's key cannot be imported.
- Cancellation, unavailable hardware/enrolment, missing/invalid keys and authentication failure never decode/unlock the notebook. Changing biometric enrolment can invalidate the key. Use master-password recovery and disable/re-enable biometrics in Settings if necessary.
- Master fallback is intentionally retained to prevent permanent lockout. It is not a new universal/recovery password. Keep the real master secret and an external encrypted backup safely. API 26/27 devices continue to use the master-password flow.
- Explicit Lock/expiry clears the unlocked in-memory secret/state. Backgrounding cancels pending biometric prompts; a late callback cannot unlock after the authentication generation has changed. Duplicate pending prompts are blocked.

**Successful hardware biometric enrolment/authentication, enrolment-change invalidation and phone-specific biometric behaviour still require physical-device acceptance.** CI coverage must not be interpreted as proof that every phone's biometric sensor has been exercised.

## Data safety

Schema **4** adds opening ink, with BLUE as the legacy default. Schemas 1–3 remain readable and existing actual amounts/dates/history remain unchanged. Do not downgrade to TEST0.6 after saving schema-4 data. The unchanged invented schema-3 DEMO restore file remains supported. Normal TEST0.7 is version code **7** in `.preview.notebook`, signed with the existing pinned signer; DEMO0.7 is separate `.preview.sample` storage. Export an external encrypted backup before updating. No uninstall/data clear is needed.

## Verification receipt

**CI SUCCESS:** https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43803717584

- Tested source: `37c54d7d8f4ac44f0798edd26c16004606a98e57`.
- [Artifact 14197905836](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43803717584/artifacts/14197905836): `WeeklyMoney-0.7-calendar-preview-and-isolated-demo`, 20,024,245 bytes, 14-day retention.
- ZIP SHA-256: `2056f244a867e5a52e1902596f4b7f99743b0f08304f08250ff73ed10f19764c`.
- TEST0.7 APK SHA-256: `64adc7d6e42fef72f42a516d15a4ef71b7868dc2b478c673446d6a2a9a9f75fd`.
- DEMO0.7 APK SHA-256: `4db0d5f033cb9ffd199de829e149fa3d81647b1f1e7eb5258e81859fc88a2a30`.
- Both APK certificate SHA-256 checks passed: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- Compilation, JVM suites, lint and APK identity/build checks passed. Android 35 comprehensive native regression: **1 test, 0 failures/errors/skips, 103.662 seconds**. Local Python identity/offline checks: **4 passed**.

 Added JVM coverage verifies fully settled renewals without Roman marks, active-debt numbering without rewriting lifetime sequences, same-place grouping within BLUE then RED, red ink persistence and before/after audit roundtrip, plus legacy schema-3 BLUE defaults. The Android regression also exercises the red choice in the owner metadata editor, the simplified weekly history editor, updated active-loan labels and fail-closed biometric unlock when no device key exists. No successful fingerprint simulation is claimed.
