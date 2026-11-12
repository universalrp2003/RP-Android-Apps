# Private backup checklist — owner actions still required

Prepared 4 November 2026. **The boxes below are deliberately unchecked.** Creating a handover ZIP does not complete these backups. Do not paste account recovery codes, passwords, API tokens or private keys into chat/issues/PRs.

## A. Source and project history

- [ ] Download a source ZIP from GitHub onto your own device, then copy it to another independent location.
- [ ] Also save the continuity pack from this task; a source ZIP of an older main commit will not contain it.
- [ ] On a trusted computer, optionally preserve Git history using `git clone --mirror https://github.com/universalrp2003/RP-Android-Apps.git RP-Android-Apps.git`. Do this **outside an existing working repository**, and keep it private. If that destination already exists, stop rather than replacing it.
- [ ] Confirm the backup opens and contains the expected modules, shared source, Gradle configuration, workflows, docs and website. Record the commit/date.

A source ZIP does not preserve Git history; a mirror does not download GitHub release assets, Actions artifacts, GitHub issues, repository settings or secrets. History may retain old exposed signing material: see `docs/signing-key-exposure-2026-10-28.md`. Do not upload a historical mirror as a supposedly secret-free archive.

## B. Public APK files

- [ ] Download the current public APK for each app listed in `release-inventory.json` and any older versions you specifically need to retain.
- [ ] Follow the standing rule: download APKs to **`/tmp`**, never into this repository or `/home/user`. Then transfer the selected files to your own durable storage outside the sandbox. `/tmp` is only staging, not a backup.
- [ ] Compare each file's byte count and SHA-256 with the inventory before archiving it. A size/hash mismatch means stop; do not install or publish that file.
- [ ] Keep exact public APKs, release notes and verification receipts. An APK is not a copy of source or private app data, and a GitHub Release is separate from the expiring Actions candidate artifact.

Example on a trusted Linux computer or authorized sandbox, using GitHub CLI:

```bash
(
set -eu
# Stop if the destination already contains files; do not overwrite an old copy.
mkdir /tmp/rp-oor-1.1.16-backup

gh release download oor-arivippu-v1.1.16 \
  --repo universalrp2003/RP-Android-Apps \
  --pattern Oor-Arivippu-v1.1.16.apk \
  --dir /tmp/rp-oor-1.1.16-backup

sha256sum /tmp/rp-oor-1.1.16-backup/Oor-Arivippu-v1.1.16.apk
wc -c /tmp/rp-oor-1.1.16-backup/Oor-Arivippu-v1.1.16.apk
)
```

Expected: `12891948` bytes and SHA-256
`3d7119ae7d843ee57c4ca2a69df5ef6bfedf4e90210af20ceb4e74a6f5b42fee`.

Only after a match should the owner transfer it into independent backup storage. Never use an APK/hash match as malware, audible-speech or all-device reliability clearance. No APKs were downloaded as part of this handover task.

## C. Signing keys — most important for future updates

- [ ] Find the existing **encrypted owner-controlled recovery backup** for the current main signer. The repository records that one was placed in private Google Drive during the October29 migration; this task has not opened or verified it.
- [ ] Confirm you can unlock that backup privately on a trusted computer. Use a tool that displays only its **public certificate fingerprint**, not private-key contents. Do not send the password or unlocked file to an agent.
- [ ] Compare the public certificate SHA-256 with the current common signer:
  `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.
- [ ] Make a second encrypted key backup in a different secure location; keep recovery instructions/password recovery separately and test that the owner can access them.
- [ ] Review **Secret Message's separate signing identity and private recovery backup** with the owner. Do not assume the common key above applies to that package.

Common-signer CI input names are `RP_RELEASE_KEYSTORE_BASE64` and `RP_RELEASE_KEYSTORE_PASSWORD`. These are **names, not secret values**. GitHub Actions secrets are not a downloadable recovery archive. Do not create a workflow that prints/exfiltrates them to recover a missing backup.

The source tree also contains a legacy Secret Message signing container at `secretmessage/signing/secretmessage-release.p12`. This pack does **not** copy or examine that file. Its presence is not proof of secure key custody. Do not copy it into a new public handover, change it, rotate any signer, or promise same-key recovery without owner-led review. The common-signer historical exposure and remediation are recorded separately in `docs/signing-key-exposure-2026-10-28.md`.

**Do not generate a new production key as a quick workaround.** A different signer can break normal in-place updates. APKs, public certificate hashes, source code and cloud-secret names cannot reconstruct a lost private signing key. Any loss requires a per-package recovery assessment, not forced uninstall/data clearing or blind re-signing.

## D. Account and service continuity

- [ ] Confirm owner access to GitHub and its repository/Actions/Pages settings; maintain private 2FA recovery methods.
- [ ] Confirm owner access to the cloud account hosting the Oor family relay and the separate Secret Message service. Preserve configuration and recovery instructions privately, without exporting real messages, pairing secrets or locations here.
- [ ] Record which weather, speech and other provider accounts/configuration the app actually uses; review their terms and quotas before migrating anything.
- [ ] Keep an owner-controlled inventory of required secret **names**, what each is for and where its secure recovery copy lives—never store values in this public repository.
- [ ] Use each app's approved export/backup feature for private data where available. DailyVault uses password-protected backup/restore. Source or APK copies do not include phone-held Android Keystore keys, family pairings or private app data; do not assume generic Android backups restore them.

Moving development to another agent does not automatically migrate backend accounts or make their hosting free forever. Do not move/deploy a worker, enable paid resources, rotate credentials or request new provider access solely because an AI tool changes.
