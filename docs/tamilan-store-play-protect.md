# Tamilan Store 0.8 — owner installation block and focused review

Updated 1 November 2026. **Unresolved on the owner's phone.** The owner supplied an installation dialog for `com.universalrp.tamilanstore` 0.8 (8), installed version 0.7 (7), followed by Google Play Protect **“Harmful app blocked” / “This app may be harmful.”** A later screen shows the new Store interface running; it does not establish that Google withdrew the warning or independently match the installed APK hash. Do not infer the installation path or recommend its “Install anyway” control. Keep Play Protect enabled; stop at a warning. Do not uninstall/reset a working installation, change package/key, or repackage old bytes to try to escape classification.

Raw screenshots remain outside public source: they also contain private travel history. No originals are republished here.

## What actually happened with SMS Guard

[SMS Guard's investigation](sms-guard-play-protect.md) and [1.1.15 receipt](sms-guard-1.1.15-verification.md) record legitimate privacy changes: separate off-by-default cloud speech consent, local voice fallback, secret suppression/redaction, contextual runtime permission requests and accurate disclosures. The owner subsequently supplied a “This app looks safe” result and reported no warning. There was **no developer action that directly cleared Google's verdict**, no Google/VirusTotal submission, and no demonstrated causal link between a particular fix and that result.

The subsequent 1.1.16 removal of a stale *in-app* warning card did not remove or control a *Google* warning. It followed the owner's successful scan. Removing warning text is not a remediation for the current Store block.

## Focused Store audit and comparison

Reviewed source at the Store0.8/publication baseline, without modifying or rebuilding any released APK:

- Store declares `INTERNET`, `REQUEST_INSTALL_PACKAGES` and `POST_NOTIFICATIONS`. Package visibility is explicitly scoped to the 13 suite apps; no blanket `QUERY_ALL_PACKAGES` declaration. No Store SMS/contacts/phone/location permission, accessibility service or notification-listener service. WorkManager/AndroidX may contribute normal merged-manifest permissions: the release publisher's actual old/new manifest comparison is the separate authoritative no-added-permissions check.
- `POST_NOTIFICATIONS` is **used**, not surplus: `UpdateNotifications.kt` implements optional, initially off, silent daily update notices. It is not notification-listener access. Removing it would remove an existing feature, not establish a Google fix.
- `MainActivity.kt` starts downloads only after a Download/Update action. A verified download needs a separate Install action. Unknown-source settings are opened only from that action if needed; Android still performs installation. `InstallerHandoff.kt` supplies a read-only content URI. No silent installer, automatic acceptance or security-warning override found in these inspected paths.
- `StoreModel.kt` checks HTTPS/allowed redirect hosts, byte limit/size/SHA, APK package/version/signer and installed signer/no-downgrade eligibility; `verifiedFile` rechecks immediately before installer handoff. These are integrity/consent protections, **not malware-free certification**.
- `LiveCatalog.kt` pins package identities, disallows same-version content replacement and bounds fetched metadata. The Store has no SMS cloud-speech consent path to transplant from SMS Guard; no Store speech backend or message-content upload flow was found in this focused source review.
- Independently retrieved the already-published APK via retained Git blob **only into `/tmp`**. Size/SHA below matched; standard `PK\x03\x04` ZIP local-file header and all ZIP entry CRCs passed. No nested APK/JAR payload was present; packaged native libraries were the two ABI copies of AndroidX graphics path. This structural inspection is not a full SDK/binary malware audit or a Google classifier explanation.

**Result:** no evidence-backed equivalent SMS-specific code fix has been identified. The exact Google classification reason remains unknown. Do not label this a proven false positive, blame notifications/the installer permission/the signing key without evidence, or publish a cosmetic rebuild as a promised cure. No app feature, permission, package, signer or version is changed by this investigation.

## Exact artifact for further review

- Package: `com.universalrp.tamilanstore`; versionName **0.8**, versionCode **8**.
- Canonical [APK](https://github.com/universalrp2003/RP-Android-Apps/releases/download/tamilan-store-v0.8/Tamilan-Store-v0.8.apk): **19,276,334 bytes**.
- SHA-256: `3fae24373a6ead0bab0c0b2ad56a80a3b191de7a44ca69b50561deac3eea8474`.
- Release430268186 / APK696852937 / checksum696852940; selected source `478a6fa77ec988575b3df3e4e13c70c830325e72`; retained blob `5fe4d5f903e11a0788a2847cea02d88fec37773d`.
- Established signer SHA-256: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`. Earlier exact release/publisher checks and their limitations: [verification receipt](tamilan-store-0.8-verification.md).

## Legitimate next step — review, not warning evasion

Google's [warning-specific developer guidance](https://developers.google.com/android/play-protect/warning-dev-guidance#harmful-app-blocked) distinguishes “Harmful App Blocked” from the sensitive-permission/fraud wording previously seen with SMS Guard. It recommends reviewing behavior, APIs, minimum permissions, privacy/security, SDKs and APK headers, then an appeal if the developer concludes the classification is erroneous. This audit is only a focused part of that review, not a claim that every dependency has undergone independent analysis.

The current [official appeals form](https://support.google.com/googleplay/android-developer/contact/protectappeals) asks for email, package name, **the SHA-256 of an APK uploaded to VirusTotal**, and additional information (1000-character limit). Merely reading this form does not submit anything. **No APK was uploaded to Google, VirusTotal or another scanner; no appeal was sent.** Owner authorization is required before any such third-party sample upload or submission; the owner handles sign-in/reCAPTCHA. No credentials are requested in chat. Approval, timing and a response are not guaranteed.

Prepared draft (not submitted; confirm facts and prerequisite sample authorization first):

> Please review Play Protect's “Harmful app blocked” classification for Tamilan Store, package com.universalrp.tamilanstore, version 0.8 (8), SHA256 3fae24373a6ead0bab0c0b2ad56a80a3b191de7a44ca69b50561deac3eea8474. The owner observed the block on 1 November 2026 while updating 0.7. This first-party app catalog downloads only after user action, verifies APK size/hash/package/version/signer, and requires a separate action to open Android's installer. Optional silent update alerts default off; no SMS or cloud-speech feature is present. Source and verification: https://github.com/universalrp2003/RP-Android-Apps/blob/main/docs/tamilan-store-play-protect.md . We do not know the classification reason and request review, not a guarantee of future compliance.

Until a genuine new result exists, the public website/release notes must disclose this owner-reported block and must not call the later running screen Google clearance.
