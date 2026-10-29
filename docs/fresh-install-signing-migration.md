# Fresh-install signing migration

**Status (29 October 2026): owner-selected migration; the secret-backed signer has passed main-branch CI. No replacement-key production APK has been released yet.** Main Build APK run [40838819041](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40838819041) built and verified the four legacy-batch APK signatures; the SMS Guard signed-candidate run [40838818972](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40838818972) and launcher/podcast/Vasanthi/PulseEQ candidate run [40838819054](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40838819054) also passed. The signer integration was merged in [PR #57](https://github.com/universalrp2003/RP-Android-Apps/pull/57). These are CI results, not physical-device tests or Play Protect clearance.

The legacy Android signing key was exposed. The owner selected a fresh-install migration rather than reusing that key. New eligible release APKs use the owner-managed PKCS#12 signer with certificate SHA-256:

`7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`

The private keystore and its password are kept outside this repository: an encrypted recovery backup is in the owner's private Google Drive, and CI reads the corresponding repository Actions secrets `RP_RELEASE_KEYSTORE_BASE64` and `RP_RELEASE_KEYSTORE_PASSWORD`. The workflow helper at `tools/prepare_release_signing.py` materializes the file only in the job workspace and rejects any certificate other than the pinned fingerprint. Do not add the keystore, its password, or their encoded values to Git, logs, artifacts, or public documentation.

## User impact and publication gates

- Android will not install a differently signed APK as an update to an existing installation. Before installing a replacement-key APK, users must download the official replacement, back up/export anything important, uninstall the old app, and then install the new APK through Android. Uninstalling can erase local data; advise users to export or otherwise back up anything important first.
- Do not tell users to uninstall before the replacement APK is available. Keep the official website's bilingual warning visible and the APK links accurate.
- Keep existing tags and public assets intact. Every changed APK requires a new version and release tag; never replace a same-version APK.
- Each app's normal source, unit, lint, package/version/minimum-SDK, permission, build, signature and release-asset checks must pass before its own publication. Do not combine separate app workflows into an unverified bulk release.
- Do not publish until the secret-backed signer has been exercised successfully by CI. Report whether a candidate was installed on a device separately from whether CI built and verified it.
- The user-facing daily update notice remains a read-only release check that opens the official website in a browser; Android handles installation with its normal confirmation flow.
- Tamilan Store's updater logic remains unchanged. Its package signature may change; give the same uninstall/data-loss warning before its replacement release.
- Secret Message has its own separately managed signer and is not part of this key rotation. Do not publish or modify its release as part of this migration.
- This signer migration does not clear, explain or appeal any Play Protect classification. Never claim Play Protect clearance; if protection shows a warning, tell users to stop and do not bypass it.
