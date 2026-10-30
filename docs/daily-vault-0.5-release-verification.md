# Daily Vault 0.5 release verification

Publication authorized: yes — the owner requested the redesign, password-free app opening, passwords only for backup/restore, screenshots enabled, and build/publication. This authorizes **Daily Vault only**, not replacement of any existing asset or publication of another app.

## Exact tested candidate

- Source: `bb400961341fc51d10fa191f79ffa052e3a66d91`.
- Successful signed candidate run: [41091029321](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41091029321), Daily Vault job `124184635743`.
- Immutable artifact: `12994921382`, `dailyvault-verified-release-apk`.
- Artifact archive SHA-256: `5ffa58fb2717118d86548be5afbcd18b0073779f0e6043bfe002f05ff8f28b09`.
- The pinned archive contains exactly `DailyVault-v0.5.apk` and its SHA-256 manifest. Promotion checks the archive hash, the contained APK hash, package/version/minimum SDK, non-debuggable status, owner-backed signer and compatibility with public 0.4 before publication. It does **not** rebuild.
- APK SHA-256, read from the authenticated pinned archive: `caae81328c5577dbc333dd34b0341f8154a9f23d98af648d9e121dc91d1fcb5e`; size **11,712,552 bytes**.
- Package `com.universalrp.dailyvault`, version `0.5`, code `5`, minimum SDK `26`.
- Expected signer SHA-256: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.

## Verified scope

- 215 Python source-contract tests and localization checks pass. These are source checks, not device proof.
- 14 release JVM tests, release lint, signed release build, package/version and signing checks pass.
- Android 15 emulator executes the **same signed release APK**: fresh no-login opening; expense and note saving; process restart persistence; Tamil UI; screenshot capture; font scale 1.3 capture; independent legacy-v1 encrypted fixture recovery; wrong legacy password leaves encrypted file bytes unchanged; recovery keeps the legacy ciphertext unchanged; password-protected export; correct independent backup password restores both records; wrong backup password preserves current encrypted bytes; explicit replacement consent; removal of the temporary record after successful restore.
- Emulator evidence: artifact `12993404207`, `dailyvault-release-emulator-evidence`, on the same run, retained 14 days. All data/passwords are synthetic test fixtures, never owner records.
- Initial attempts failed on emulator launcher startup or an inaccessible floating-action label. The final run passed without hiding Daily Vault ANRs. Explicit localized action semantics now match the visible button; light system bars use API-26-compatible controls.

## Limits and data-safety notice

No physical-device install, OEM compatibility, Play Protect clearance, security audit or universal large-font/no-clipping certification is claimed. Screenshot and font-scale captures prove those bounded scenarios only. The promotion additionally compares the built permissions against immutable public 0.4 and requires no newly dangerous permission.

Use **Update**, keeping package/signing compatibility and records; never advise uninstalling or clearing app data to bypass an install conflict. An incompatible older signer must be investigated before installation. Existing usable local wrappers are reused. A password-only legacy vault may require its original password **once** for recovery; unreadable files are never silently erased. Portable backups retain the existing authenticated TDV-v1 format and require their own chosen password.

Opening is intentionally not authenticated. Anyone with access to the unlocked phone can view/capture records. Backup encryption does not protect an already-open app. Network access is only the disclosed read-only update-metadata check, not vault-content transmission.

Public site/catalog metadata stays on 0.4 until 0.5's public asset digest and re-downloaded bytes verify. The post-publication receipt will record the APK digest/size and promotion run. Store 0.6 and all other public APKs remain unchanged.
