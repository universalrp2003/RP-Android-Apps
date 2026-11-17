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
