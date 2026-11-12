# தினக்கணக்கு / Daily Vault 0.5

Manual spending and notes/history, separate from the SMS-based Expense Tracker. Tamil/English, system light/dark appearance, Overview / Records / Backup navigation, exact integer-paise totals, monthly categories, recent entries, search, clearly selected spending/notes filters, native date/time selection, edit and confirmed deletion. No fabricated financial data. Failed saves keep the draft open.

## Opening and local records

**No app-opening password or biometric gate. Screenshots/recording/recents previews are allowed**, per the owner's 30 October 2026 decision. Anyone using the unlocked phone can view/capture records; use the phone's screen lock. Private local files remain encrypted and atomically written. A randomly generated local credential is wrapped by Android Keystore; it is not a backup password or an authentication-bound/hardware-guaranteed key. Existing convenience-mode wrappers retain their original namespace and alias.

An older password-only vault may require the **previous password once** to authenticate its existing file and save a local wrapper. This does not overwrite the authenticated file or establish a recurring login. Unreadable existing files are never silently initialized or erased. If the device key or app data is lost, a separate exported backup and its password are needed; uninstalling or clearing app data may erase local records. Do not blindly uninstall to update. Same-key public0.4 →0.5 is an in-place update; older key-mismatched builds require the documented signing-migration precautions.

## Portable backups

Each export asks for a new 10–256-character backup password and confirmation, then re-encrypts an immutable snapshot independently of the local credential. Backups retain the TDV-v1 AES-256-GCM envelope, random16-byte salt/12-byte nonce, authenticated header and PBKDF2-HMAC-SHA256/210,000 iterations. Restore requires the exported file's password and explicit replacement acknowledgement. It fully authenticates and validates before atomic replacement. Wrong/corrupt/oversized files must not replace current records. This is **replacement, not merging**. Old v1 backups remain readable; no device-only key is needed on the receiving phone.

Use Android's file picker; no broad storage permission. An export provider failure can leave an incomplete new file: keep older backups, retry and test restore before relying on a copy. A successful file write is not an independently verified off-device backup. Automatic Android backup is disabled. No forgotten-password recovery. Limits:16MiB encrypted files,50,000 records and bounded field lengths. Not an independently security-audited password manager.

## Network and privacy

No advertising SDK, SMS, contacts, microphone, location, biometric or installer permission. INTERNET is only for the existing once-daily foreground public release-metadata check. No ledger, note, backup or password data is uploaded. The site/GitHub receives normal connection metadata such as IP. Updates open the official website in the external browser.

## Design research and validation

Material guidance informed selected navigation/filters, readable hierarchy and native date selection rather than a typed timestamp:
- https://developer.android.com/develop/ui/compose/accessibility/api-defaults (48dp interactive targets)
- https://m3.material.io/components/date-pickers/accessibility

Run `gradle :dailyvault:testDebugUnitTest :dailyvault:testReleaseUnitTest :dailyvault:lintRelease :dailyvault:assembleRelease` plus the Python source contracts. `legacy-v1.tdv` is an independently generated Python AES-GCM/PBKDF2 synthetic fixture, not owner data; its test-only password is `LegacyBackupTest123`. JVM tests cover money/date/schema bounds, Unicode and note totals, legacy compatibility, independent backup/local credentials and tampering/wrong passwords. The publication candidate workflow also exercises the **exact signed release APK** with synthetic data in an Android15 emulator and retains UI/screenshot evidence. This is not physical-device/OEM or Play Protect certification; see the run result before claiming a pass.

Publication is explicitly authorized for0.5, but current-public links/catalogs remain0.4 until actual promotion. Previous0.4 release assets are immutable.
