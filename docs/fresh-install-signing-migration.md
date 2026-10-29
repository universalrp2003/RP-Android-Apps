# Fresh-install signing migration

**Status (29 October 2026): configured, not yet validated by Actions and not yet published.** No APK signed by the replacement key has been released.

The legacy Android signing key was exposed. The owner selected a fresh-install migration rather than reusing that key. New eligible release APKs use the owner-managed PKCS#12 signer with certificate SHA-256:

`7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`

The private keystore and its password are kept outside this repository: an encrypted recovery backup is in the owner's private Google Drive, and CI reads the corresponding repository Actions secrets `RP_RELEASE_KEYSTORE_BASE64` and `RP_RELEASE_KEYSTORE_PASSWORD`. The workflow helper at `tools/prepare_release_signing.py` materializes the file only in the job workspace and rejects any certificate other than the pinned fingerprint. Do not add the keystore, its password, or their encoded values to Git, logs, artifacts, or public documentation.

## User impact and publication gates

- Android will not install a differently signed APK as an update to an existing installation. Before installing a replacement-key APK, users must uninstall the old app and then install the new APK. Uninstalling can erase local data; advise users to export or otherwise back up anything important first.
- Keep existing tags and public assets intact. Every changed APK requires a new version and release tag; never replace a same-version APK.
- The first replacement-key release must pass the repository's ordinary source, test, build, APK identity, signature and release-asset checks. A green build is not a device test, Play Protect clearance, or Play approval.
- Do not publish until the secret-backed signer has been exercised successfully by CI. Report whether a candidate was installed on a device separately from whether CI built and verified it.
- The user-facing daily update notice remains a read-only release check that opens the official website in a browser; Android handles installation with its normal confirmation flow.
- Tamilan Store's updater logic remains unchanged. This signing migration does not authorize bypassing any Store release gate.
- Secret Message has its own separately managed signer and is not part of this key rotation.
