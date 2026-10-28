# Owner follow-up checklist — 28 October 2026

- Tamilan Store: compact Search button/dialog; owner galleries for all twelve apps; tap to enlarge; Live Guard Tamil name கைபேசி காவலன்; Store stable-release discovery/browser handoff; request-app and Contact actions.
- Keep existing apps' self-download/update behavior unchanged. Store/DailyVault previews are not authorized public releases.
- Preserve Secret Message/DailyVault screenshot blocking. Gallery copies redact Secret Message invite/token and device identity; partially mask personal names in Expense Tracker and Launcher.
- DailyVault photos and Store photos are saved in docs/images; DailyVault remains a standalone unpublished preview, not a public catalog download.
- Voice Notes next build: About, bilingual Guide, sister-app/website links and optional biometric/device-credential unlock; see voicenotes-next-build.md. These features remain pending implementation/testing.
- Verify redactions visually, gallery asset integrity, localization, Python tests, Android compile/lint/tests and device behavior before delivery. Source checks alone are not APK verification.
- Investigate DailyVault 0.2 failing AAR metadata build and verify latest Store Android CI; do not claim builds succeeded without checking.
- User has supplied all twelve public-app galleries. Expense Tracker/Voice Notes have three images each; do not claim four-to-five for every app.

Do not promise flawless behavior. Report actual validation results and remaining limitations.

## Execution update

- Store 0.4 before latest gallery/search edits: CI run 40630856873 succeeded. Latest edits need a new run.
- DailyVault latest failure root cause verified via CI annotations: InvalidFragmentVersionForActivityResult (lintDebug), not an assumed AAR problem. Explicit Fragment 1.8.6 added; new build pending.
- Voice Notes compact scrollable bilingual Help/About with website/sister-app directory link and optional local biometric/device-credential access gate implemented. Read-only debug preview workflow added; no stable version/release publication performed. Android compile/lint and device behavior remain pending.

## Owner-device acceptance — 28 October 2026, 11:20 IST

Owner reports “All fine working perfectly” after installing/testing the three preview builds. Supplied screenshots show Store galleries and compact toolbar, Voice Notes Help/About with lock OFF, installer version details for Store 0.4-preview/code4 and DailyVault 0.2-preview/code2, and Voice Notes 1.2/code3 candidate replacing the same stable version label. Play Protect screenshots show “This app looks safe” for these device installation attempts; this is not a universal safety/security certification or APK hash verification. Owner acceptance is device-specific, not exhaustive biometric/cancellation/backup-restore/all-device proof. No public publication or PR merge was requested by this acceptance. Future Voice Notes releases should use a distinct version/code to avoid same-version candidate confusion.

## Publication authorization and security hold

Owner now authorizes publishing all, but reports SMS Guard Finance lock toggle bypass. Publication preparation is authorized; no new release has yet been published for this request. SMS Guard must be fixed/tested first. Source patch routes disabling through device authentication, guards the ViewModel against unauthenticated disable, and blocks unauthenticated creation/replacement/removal of Finance PIN while locked (another potential bypass). Android compilation and cancellation/device testing pending. Locked users with no enrolled phone credential cannot create a new bypass PIN; recovery/enrollment UX needs review before release. Existing PIN verification remains available for Finance viewing. New Store/DailyVault public release workflows/catalog assets and distinct Voice Notes versioning still need preparation; do not claim publication from authorization alone.

## SMS Guard security candidate owner acceptance — 28 October 2026, 11:56 IST

Owner reports the installed security candidate asks for biometric/PIN when changing Finance settings and that newly arrived SMS voice announcements continue working normally. Screenshots show the Finance lock enabled and installation UI for SMS Guard 1.1.9/code15. Candidate supplied: workflow 40643001380, source 89fe93cf6ca72ce4e688fa59e9c88eb153f27eca, APK SHA-256 6ad54ff23eca21908b4959f148ca3244cd71b2b66a9e2213ae7aad9e0af34dbf. Screenshot/version labels alone do not independently verify installed APK hash. This is owner-device acceptance, not exhaustive cancellation/all-device testing or proof that live network CI passed. Full CI live neural voice test remains unresolved; compile-only candidate did not run unit/live tests. Publication authorization remains recorded; no new public release was performed by recording this acceptance. Public SMS Guard release must use a new version/code rather than replacing the existing 1.1.9 asset.

## First public releases verified — 28 October 2026

Owner explicitly requested “publish all new applications.” Published the two previously unpublished standalone applications (not unrelated legacy app rebuilds):

- Tamilan Store 0.5/code5: https://github.com/universalrp2003/RP-Android-Apps/releases/tag/tamilan-store-v0.5 — Tamilan-Store-v0.5.apk, 16912504 bytes, SHA-256 d5f41e7b5142507e8b42fb9d2c4e0e2d492eaf03581a53b3dabacf692daf4bc7.
- DailyVault 0.3/code3: https://github.com/universalrp2003/RP-Android-Apps/releases/tag/daily-vault-v0.3 — DailyVault-v0.3.apk, 12417577 bytes, SHA-256 aa35dca15be265146618c5e53748abc03e6022e4469ab7b6c523f655412043dd.

Workflow 40644533898 succeeded: 139 Python tests/localization, both release unit-test tasks, lintRelease and signed assembleRelease, package/version/non-debuggable/pinned-signer verification. GitHub API confirms both releases draft=false/prerelease=false and matching APK digests, plus SHA256 files. Original preview device acceptance is not a claim of device-testing these rebuilt release binaries. Existing preview package names/signers preserved, version codes incremented. No old asset overwritten. SMS Guard and Voice Notes updates remain unpublished; original twelve-app Store catalog/website is unchanged by these first releases and still needs separate directory integration. Store self-release checker can now discover the actual public 0.5 release. No PR merge performed.
