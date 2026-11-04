# Owner Drive backup status — 4 November 2026

**Partial, not a complete recovery backup.** The original continuity ZIP remains documents-only. A separate owner-private Drive folder named **RP Android Apps — Recovery Backups** now has a dated first snapshot. Its private `BACKUP-STATUS.json`/`README-FIRST.md` are the authority for copied files. Do not put private folder URLs, upload-session URLs or secret values into this repository.

## Actually copied and checked

- Source archive at commit `81f090373735342b90791d1a86efbd1cfc69ceb4`: 1,758 tracked files, 38,425,215 bytes, SHA256 `ee9fcbd5138621ff34a09cb7aeefe2c7b0ae4528e9351629e05fcc95567f4d7c`. Drive byte size/MD5 matched the local archive; ZIP contents passed integrity checks. Known signing/credential containers and the legacy signing directory were excluded. Its public certificate fingerprint was supplied separately for restoration to `secretmessage/signing/certificate-sha256.txt`. This is a source working snapshot, **not Git history**.
- Documents/release-notes archive: 381 files, 1,571,630 bytes, SHA256 `d11a95bd534786999d5128eb6b94de241e9c462ad9b8ff0c6d016b37d83f605b`. Drive byte size/MD5 matched; ZIP integrity checked.
- Existing continuity pack, explicit archive checksums/status, public certificate fingerprint, weekly checklist, private signing-recovery reference, and the dated 14-app public release inventory.
- Root folder permissions checked: owner only. Existing separate signing-recovery folder also showed owner only. Its **contents were not read/copied/decrypted/restoration-tested**. Finding filenames is not signing recovery verification.

## Not done

- **Actual APK binaries: 0 of 14 backed up.** An exact approved Oor APK download hit the sandbox's release-asset network restriction (EOF). A proposed one-time hosted transfer could not create its narrow upload secret because the GitHub Actions secrets public-key endpoint returned HTTP 403, `Resource not accessible by integration`. No secret was set, no backup workflow was created/run, no APK was rebuilt or published, and no upload-session URL was committed or put into public logs. The unused narrow session expires automatically; initiation is not a stored APK.
- APK folder is explicitly labelled **PENDING**. It contains instructions, inventory, and `tools/download_continuity_apks.py` for a trusted computer with working release-asset network access. It downloads exact public assets below `/tmp`, checks pinned metadata and bytes/SHA256, then produces a ZIP for owner upload. It does not itself upload to Drive. Its actual network/download path has **not** completed in this sandbox.
- Raw signing keys/passwords, Git history, phone data/Android Keystore contents, account access and private backend state are not included. Separate Secret Message signer recovery still needs owner-private review.
- **No automatic weekly job or reminder is configured.** The checklist is a manual process, not a scheduler. Unattended updates require a separately agreed secure authentication/storage/scheduling setup; do not expose Drive upload capabilities via public workflow files, dispatch inputs or logs to work around a permission refusal.

## Weekly continuation

1. Locate the existing private parent folder by name; do not recreate duplicates or change sharing.
2. Make a new dated snapshot, record its source commit and canonical public release metadata, and copy actual source/documents with explicit credential exclusions. Keep public signing fingerprints.
3. Copy only new/changed exact APKs; reuse prior verified files for unchanged versions. Preserve previous good snapshots; no unapproved deletion, key changes, rebuilds or publishing.
4. Verify uploaded byte sizes/checksums and archive contents. Mark unresolved parts pending; links alone do not count as APK backups. Keep another independent copy and test restore privately.
5. Owner must choose/authorize any unattended weekly setup. Do not claim background work continues when no scheduler exists.
