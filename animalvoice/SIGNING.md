# Animal Sounds experimental signing

The 0.17 experimental APK is signed with the repository's existing owner-backed RP release key, so future CI builds can update it. The private key is stored only in GitHub Actions secrets and is never committed:

- `RP_RELEASE_KEYSTORE_BASE64` — base64 PKCS#12 keystore
- `RP_RELEASE_KEYSTORE_PASSWORD` — keystore and key password
- alias — `cleansweep`
- certificate SHA-256 — `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`

CI uses `.github/actions/prepare-release-signing` to decode and verify the key, builds a non-debuggable release APK, verifies the certificate with `apksigner`, and uploads the APK as an artifact. The workflow fails closed if the secrets or fingerprint are missing or wrong.

## One-time migration from 0.16

0.16 and earlier were debug-signed with a per-run key. Android does not allow an APK signed with a different certificate to update an installed app, even when the application ID and version code match. A 0.17 APK therefore cannot update 0.16 in place.

To migrate:

1. Note that uninstalling deletes the downloaded BirdNET model and app settings. There is no model export, and `allowBackup` is disabled, so the model cannot be restored from a backup.
2. Uninstall 0.16.
3. Install 0.17.
4. Re-download the ~79 MB species model.

From 0.17 onward, same-key updates preserve app data, including the downloaded model and the auto-photo setting, as long as the application ID (`com.universalrp.animalvoice.experimental`) and signing certificate remain unchanged.

## Key custody

The private key must be backed up by the repository owner outside Git. If the Actions secret is lost, a replacement key cannot update existing installations; that would require another uninstall and a new one-time migration. Never paste the keystore or password into chat, an issue, or a committed file.
