# Weekly Money 1.4 publication

[Download the signed APK](https://github.com/universalrp2003/RP-Android-Apps/releases/download/weekly-money-v1.4/Weekly-Money-v1.4.apk) · [Tamil guide](https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html) · [release notes](../releases/weekly-money-v1.4.md) · [publication receipt](../docs/weekly-money-1.4-publication-receipt.json).

Version 1.4/code 16, SHA-256 `b4e1ac9625d0576691c1a20605b5375f664ecfe3863b52c7821fce6ae4e4ad23`. CI 44011689271 verified unit/lint, Android 15, long press and undated receipts, plus a genuine public 1.3 in-place upgrade. Publisher 44014964552 promoted exact bytes without rebuilding. The user reports phone functionality works; the installed APK hash was not independently checked. No independent security audit. Schema 7 reads older backups; earlier apps cannot open schema-7 files.

## Previous publication

# Weekly Money 1.3 publication

[Download the signed APK](https://github.com/universalrp2003/RP-Android-Apps/releases/download/weekly-money-v1.3/Weekly-Money-v1.3.apk) · [Tamil guide](https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html) · [release notes](../releases/weekly-money-v1.3.md) · [publication receipt](../docs/weekly-money-1.3-publication-receipt.json).

Version 1.3/code 15, SHA-256 `ec2606b774f916633d502d1b62f4891034423681f946128efac42c7374c3a416`. CI 44001102380 verified Android 15, unlocked-home button and public 1.2 in-place upgrade; publisher 44002563015 promoted the exact bytes without rebuilding after retrying a draft enumeration failure. The exact APK has not been confirmed physically phone-tested or independently audited.

## Previous publication

# Weekly Money 1.2 publication

[Download the signed APK](https://github.com/universalrp2003/RP-Android-Apps/releases/download/weekly-money-v1.2/Weekly-Money-v1.2.apk) · [Tamil guide](https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html) · [release notes](../releases/weekly-money-v1.2.md) · [publication receipt](../docs/weekly-money-1.2-publication-receipt.json).

Version 1.2/code 14, SHA-256 `437b5c742c4164477ba16b64bed6aca7bb2e7ca641c46b9f11e17ef65e30c1e5`. CI 43997105422 verified Android 15 and public 1.1 in-place upgrade; publisher 43999516682 promoted exact bytes without rebuilding. No physical phone acceptance or independent security audit.

## Previous publication

# Weekly Money 1.1 publication

[Download the verified signed APK](https://github.com/universalrp2003/RP-Android-Apps/releases/download/weekly-money-v1.1/Weekly-Money-v1.1.apk) · [Tamil guide](https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html) · [release notes](../releases/weekly-money-v1.1.md) · [publication receipt](../docs/weekly-money-1.1-publication-receipt.json).

Version 1.1/code 13, SHA-256 `1ebae14f47b05458376e2346421d9c68b4d20874b8101114abfc9d6a09f887c0`. Built and verified in CI 43991161598, published without rebuild in 43992133375. Android 15 emulator and public 1.0 upgrade passed; no physical phone acceptance or independent security audit.

## Previous publication

# Weekly Money 1.0 — published

[Download the signed APK](https://github.com/universalrp2003/RP-Android-Apps/releases/download/weekly-money-v1.0/Weekly-Money-v1.0.apk) · [Release notes](../releases/weekly-money-v1.0.md) · [Exact-byte publication receipt](../docs/weekly-money-1.0-publication-receipt.json) · [Guide](https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html).

- Package `com.universalrp.weeklymoney.preview.notebook`, version 1.0/code 12; persistent signing certificate SHA-256 `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- APK 7,401,255 bytes; SHA-256 `b23ea6dd75b1b6aa95798810f1abefa956dff06b8bca9ac95b7387218f687fb8`. Published release is non-debuggable and uses the clean Tamil label.
- [CI run 43873920987](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43873920987) passed unit tests, debug/release lint and builds, Android 15 native regression (1 test, 0 failures), signer/identity checks and the exact release APK upgrade from public 0.11. A synthetic encrypted notebook retained its UID and exact encrypted bytes, then unlocked with the master password and displayed its existing volume. No owner data was used.
- [Publisher run 43875223289](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43875223289) promoted those exact verified bytes, without rebuilding or replacing old releases.

For normal updates do not uninstall or clear data. Keep external backups and the master password. Schema 6 reads schemas 1–5; do not downgrade after saving. Versions signed with a different old key cannot be guaranteed to update in place. CI does not establish universal physical-device, Play Protect, biometric, financial or security certification. Optional Tamil dates use a bounded 2026 table.

Historical release receipts: [0.11](../docs/weekly-money-0.11-publication-receipt.json) and [0.10](../docs/weekly-money-0.10-publication-receipt.json); their descriptions reflect their release dates, not the current scope.
