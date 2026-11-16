# Weekly Money permanent update identity

Owner approved public TEST0.10 after phone testing. Its exact tested bytes were published as release 446842477 at 2026-11-16T09:54:42Z.

- Package: `com.universalrp.weeklymoney.preview.notebook` — retain this identity for in-place updates, despite the historical word `preview`.
- Current version/code: 0.10 / 10. Every subsequent update must have a higher versionCode.
- Persistent signing certificate SHA-256: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- Reuse existing owner-backed signing secrets; do not generate a new release key, use ephemeral debug signing, switch package, or resign an approved APK during promotion. Missing/mismatched key must fail the release gate.
- Keep private key/backup outside Git and chat. Certificate fingerprints are public, not recovery credentials.
- Normal update uses Android installer in place; no uninstall, clear-data, or routine backup/restore migration. Backups remain a safety precaution. Different historic signers and Android/OEM policy restrictions cannot be bypassed.

Public APK: https://github.com/universalrp2003/RP-Android-Apps/releases/download/weekly-money-v0.10/Weekly-Money-v0.10.apk
Bytes 10,120,630; SHA-256 `5f05a9bd070d451b0db51560f3e1304903db2e64cc2c402843f64c6924730494`.
Source/verified artifact provenance: docs/weekly-money-0.10-verification.md.

Publication initially stopped after creating an unpublished draft because GitHub's by-tag endpoint did not resolve drafts. The verified draft was recovered by release ID and published after checking target commit, APK digest/size and prerelease flag. Redundant identical unpublished drafts were removed; no published asset was overwritten. Promotion now enumerates drafts before creating and publishes by numeric release ID.

This remains a publicly available **TEST preview**, not a claim that unfinished collector exchange/independent recovery-secret features are complete.
