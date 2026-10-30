# Daily Vault 0.5 release verification

Publication authorized: yes — the owner requested the redesign, password-free app opening, passwords only for backup/restore, screenshots enabled, and build/publication. This authorizes **Daily Vault only**, not replacement of any existing asset or publication of another app.

## Exact tested candidate — including storage-failure recovery

- Source: `c41ccdb9d0e6b9b3c3663397f96fadaa0c3e268a`.
- Successful signed candidate run: [41096269759](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41096269759), Daily Vault job `124202732993`.
- Immutable artifact: `12996159002`, `dailyvault-verified-release-apk`.
- Artifact archive SHA-256: `e17d7a8ae72510478bb6dffd3f69c0484e8bba89312be5663b852d84e80eb4b3`.
- The pinned archive contains exactly `DailyVault-v0.5.apk` and its SHA-256 manifest. The archive digest authenticates every byte of both. Promotion independently verifies the contained APK against that manifest, surfaces its digest/size before draft creation, and checks package/version/minimum SDK, non-debuggable status, owner-backed signer and compatibility with public 0.4. It does **not** rebuild. The public receipt below will record the APK digest/size too.
- Package `com.universalrp.dailyvault`, version `0.5`, code `5`, minimum SDK `26`.
- Expected signer SHA-256: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.

## Verified scope — PASS

- 215 Python source-contract tests and localization checks. These are source checks, not device proof.
- 14 release JVM tests, release lint, signed release build, package/version and signing checks.
- Android 15 emulator executes the **same signed release APK**: fresh no-login opening; expense and note saving; process restart persistence; Tamil UI; screenshot capture; font scale 1.3 capture; independent legacy-v1 encrypted fixture recovery; wrong legacy password leaves encrypted file bytes unchanged; recovery keeps the legacy ciphertext unchanged; password-protected export; correct independent backup password restores both records; wrong backup password preserves current encrypted bytes; explicit replacement consent; temporary record no longer visible after successful restore.
- **Additional real emulator fault injection passed:** with an unavailable synthetic device wrapper, a denied local file write leaves both encrypted-record and credential-wrapper hashes unchanged. A successful file commit followed by denied wrapper persistence produces the explicit warning; the committed records are recoverable with the known independent backup password, recovery preserves the committed ciphertext, and subsequent process restart opens automatically again.
- Emulator evidence: artifact `12996558454`, `dailyvault-release-emulator-evidence`, on the same run, retained 14 days. All records/passwords are synthetic, never owner data. Two additional screenshots document the storage-failure/recovery cases.
- Restore never replaces an existing wrapper before the atomic file commit. If device-key persistence fails after commit, the fallback is the known backup password, never an unstored random secret. The warning explains possible one-time completion of recovery.
- Initial attempts exposed emulator launcher startup and an inaccessible floating-action label. Explicit localized action semantics now match the visible button; system bars use API-26-compatible controls. Daily Vault ANRs remain fatal to the test.

## Publication sequencing and history

Earlier run `41091029321` passed the initial record/backup tests but was superseded by the storage-failure fix; it is **not** the promotion source. Its branch promotion attempts `41092998853` and `41093305813` verified that older archive/signatures/permission delta, but GitHub refused draft creation with HTTP 403. No 0.5 draft or public release was created by those attempts.

Publication runs only after green PR CI and merge to **main**, using normal contents-write Actions permission, without adding token scopes or changing credentials. The tag targets that main publication commit; the workflow first proves **all Daily Vault/shared/Gradle compiled inputs are unchanged from the successful candidate source above**. This preserves the exact tested APK while avoiding a release ref targeting a different workflow snapshot. The source APK provenance remains the pinned candidate run, not a rebuild at publication. The workflow checks the draft digest before publication, the tag target afterwards, and re-downloads the public APK and manifest to verify their bytes. Website/catalog promotion follows that public verification in a separate change.

## Limits and data-safety notice

No physical-device install, OEM compatibility, Play Protect clearance, security audit, universal storage-failure protection or large-font/no-clipping certification is claimed. Screenshot/font captures and injected faults prove those bounded emulator scenarios only. Promotion additionally compares built permissions against immutable public 0.4 and requires no newly dangerous permission.

Use **Update**, preserving package/signing compatibility and records; never advise uninstalling or clearing data to bypass a conflict. An incompatible older signer must be investigated first. Existing usable local wrappers are reused. A password-only legacy vault may require its original password **once**. If device-key persistence fails while finishing restore, that backup password may be needed once to finish recovery. Unreadable files are never silently erased. Portable backups retain authenticated TDV-v1 format and require their own chosen password.

Opening is intentionally not authenticated. Anyone using the unlocked phone can view/capture records. Backup encryption does not protect an already-open app. Network access is only the disclosed read-only update-metadata check, not vault-content transmission.

Public site/catalog metadata stays on 0.4 until 0.5's public asset and re-downloaded bytes verify. Store 0.6 and all other public APKs remain unchanged. Existing tags/assets are never replaced.

## Public API receipt — final promotion status pending

Promotion [41100372958](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41100372958) published release `429318029` on `2026-10-30T17:37:40Z`. Public APK SHA-256 `8842606659442a1e2b8c7d28fd2affac56bb40c0bc679acf86785f3167903d55`, size **11,712,552 bytes**. The public API digest matches the published asset receipt; final workflow/re-download completion is still being awaited before metadata merge. Tag target/main publication commit: `a3512abca9add95d3ffb7ff93fc2ff87371ddcf4`; source compiled inputs matched the pinned candidate. This follow-up promotes only website/catalog/public-version metadata. Public0.4 and Store0.6 assets remain unchanged.
