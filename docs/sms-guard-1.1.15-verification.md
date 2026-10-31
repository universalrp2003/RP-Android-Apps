# SMS Guard 1.1.15 / code 21 — verification and publication receipt

## Authorization and limits

The owner explicitly authorized building and publishing the latest bug fixes for trusted family/friends despite the unresolved Play Protect installation block, with candid documentation and public source access. This is not device acceptance or Google clearance. See [warning and audit document](sms-guard-play-protect.md), [release notes](../releases/sms-guard-v1.1.15.md) and [privacy policy](../PRIVACY.md).

No Play Protect bypass, package/signing change, data reset, Google appeal or VirusTotal/sample upload is part of this work. Same-key installed records/update path must be preserved. Oor's held travel-test candidate and accepted Radio1.31 are unchanged.

## Selected candidate (publication authorized, not yet published)

**Publication authorized: yes.** This authorization explicitly accepts the unresolved installation warning, not an assertion of app safety.

- Source: `71c2cd915a78c45092ae2a7091dfe57e1b603ded`. The PR merge tree `6cf11f4cbe0d155cae5375d7cc973f7bf900e703` is byte-for-byte tree-identical (`4faa2827b026fa51df307f3e7bddf3b209d06238`); no app/runtime input changed after this candidate.
- Successful signed candidate [run41214460439](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41214460439), job124576009488, artifact13047597952 (`SMS-Guard-signed-candidate`). Release JVM tests, lint, assembly, package/nondebuggable/manifest/signature/hash and upload steps passed.
- Archive SHA-256: `c439fd13ed1e0a2b4c516e176375f65ab9efbb5199b6b17ef381e89e1b13aad0` (12,335,752 bytes).
- Selected APK `SMS-Guard-v1.1.15.apk`: **12,824,500 bytes**, SHA-256 `ca6f4e80a7e417c2f86d25ec7f496f120f6f9f0d44525a2b8af04b5bbf4ee01e`; retained Git blob `41b1ba8f235a1ee4f8723c6be3be73260f45cc2b`. Independently downloaded only to `/tmp` and size/hash/actual manifest checked locally.
- Package `com.universalrp.smsguard`, version1.1.15/code21, minimum API26, target36, nondebuggable, backups disabled. Actual manifest keeps SMS read/receive/send and optional contacts/phone capabilities; no foreground-service, installer, overlay, accessibility or notification-listener capability was found. Core SMS declarations mean installation can remain blocked.
- Candidate workflow verified established signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`. Exact-byte promotion must independently check it against the public1.1.11 APK and compare built dangerous permissions before publication.
- One-shot publication workflow pins run, artifact, archive/APK digests and immutable source. It checks successful source/shared gates, unchanged compiled inputs, signer/permission delta, exact draft re-download and final public asset metadata. It never rebuilds the selected APK, replaces old assets, or treats static checks as Google clearance.

## Validation history

- Local repository suite: **250 Python tests passed**; bilingual/source localization, signing policy and whitespace checks passed.
- First implementation source `1081d183525ef613cfd755fb58e0b9a1c3ba0a70`: [SMS release gate 41214167012](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41214167012) passed native release JVM tests, release lint, signed assembly and verification, **without publication**. This is an intermediate result, not the final selected artifact: subsequent disclosure/heuristic hardening needs its own candidate gate.
- Final selected candidate identity is above. Public release and independent promotion-time permission/signature verification are **still pending**; update this receipt only from actual publication results.
- No exact new-build phone installation/natural-SMS/cloud-consent UI test, real-phone offline voice behavior, or Google installation clearance has been established. Heuristic tests use synthetic data, not the owner's private SMS.

### Promotion gate correction

The first promotion attempt [41214868800](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41214868800) stopped **before downloading/uploading or creating a release**: the four-app aggregate41214456050 was a pending run automatically superseded by the next push in its global concurrency queue. This was a cancelled aggregate, not an SMS source/test failure. The corrected gate pins the already successful selected-source SMS stable verification41214456132 and shared-neural regression/actual synthetic HD failover41214456065 alongside the full signed candidate41214460439. The four-app aggregate does not build SMS; all relevant current-head CI must still finish green before merging. Artifact/signature/permission/hash gates and the selected bytes are unchanged.
