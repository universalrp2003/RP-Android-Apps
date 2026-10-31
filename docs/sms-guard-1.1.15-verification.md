# SMS Guard 1.1.15 / code 21 — verification and publication receipt

## Authorization and limits

The owner explicitly authorized building and publishing the latest bug fixes for trusted family/friends despite the unresolved Play Protect installation block, with candid documentation and public source access. This is not device acceptance or Google clearance. See [warning and audit document](sms-guard-play-protect.md), [release notes](../releases/sms-guard-v1.1.15.md) and [privacy policy](../PRIVACY.md).

No Play Protect bypass, package/signing change, data reset, Google appeal or VirusTotal/sample upload is part of this work. Same-key installed records/update path must be preserved. Oor's held travel-test candidate and accepted Radio1.31 are unchanged.

## Verification in progress

- Local repository suite: **250 Python tests passed**; bilingual/source localization, signing policy and whitespace checks passed.
- First implementation source `1081d183525ef613cfd755fb58e0b9a1c3ba0a70`: [SMS release gate 41214167012](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41214167012) passed native release JVM tests, release lint, signed assembly and verification, **without publication**. This is an intermediate result, not the final selected artifact: subsequent disclosure/heuristic hardening needs its own candidate gate.
- Final candidate, exact APK digest, manifest/signature/permission comparison and public release: **not yet selected/published**. Do not infer those results from the intermediate run. The final receipt will replace this pending section after the actual gates succeed.
- No exact new-build phone installation/natural-SMS/cloud-consent UI test, real-phone offline voice behavior, or Google installation clearance has been established. Heuristic tests use synthetic data, not the owner's private SMS.
