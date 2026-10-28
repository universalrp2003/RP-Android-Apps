# Owner follow-up checklist — 28 October 2026

## Active owner-authorized follow-up — 28 October 2026

- PR53 source/site integration IS complete (main54d5955a; production Pages40682415356 SUCCESS); the older checkpoint below predates that integration.
- Owner now authorizes distinct same-language HD alternatives instead of repeating failed Valluvar. Both actual transports recovered to Pallavi23328bytes in live run40686360024; ten deterministic failover tests passed. New suite About/project links and Store0.6/Daily0.4/SMS1.1.11/Voice1.3.3 source are tracked on arena/5b3f10e0-rp-android-apps, PR54, NOT new public APKs or accepted phone builds.
- Website14-product directory and backwards-compatible Store catalogs are prepared, not yet a production deployment of this follow-up. See [implementation, direct evidence and remaining gates](hd-voice-recovery-and-about-2026-10-28.md).
- The confirmed exposed-signing-identity hold remains. GitHub secrets metadata403 prevents confirming private production custody through this integration; do not infer absence or request keys/passwords in chat. No silent rotation/uninstall/data reset. Per-package older-Android continuity, security-warning classification and actual product galleries remain open; this is not an all-work-finished claim.

## Current status (supersedes all dated historical instructions below)

- **Signing hold:** main CI private signing material/unlock reference is public, independently confirmed without exporting the key. Main-identity publication/mirroring now explicitly stops; removing a tracked file cannot clear the known-fingerprint hold. No key rotation, app/data deletion or old asset replacement. Data-preserving Android-version-specific signing review required: [finding and plan](signing-key-exposure-2026-10-28.md).
- **Voice1.3.2/code6 owner acceptance remains complete** for Stop/switch/new-note visibility on the owner's device: run40668667451/artifact12808020972/source41f29c3. No independent installed-hash/universal/security certification. Exact old-artifact publication was denied GitHub403 and is now additionally held for signing review. Do not relabel later same-version rebuilds as that accepted candidate.
- **Store-first source migration and deterministic APK gates complete:** nine-module actual unit/lint/build/merged-APK run40680202509(source463faf5) PASSED. Shared, Cleaner and Secret app-local installers removed; related permissions/providers/resources retired narrowly. Daily in-app checks opt-in/defaultOFF, no background/system-notification completion claim; offline PulseEQ remains without INTERNET.
- **Additional actual gates PASS:** broad signed/debug/release/minSDK/DEX/regression run40679527615(sourcee7ea5c1); five candidate jobs40678487296; isolated Secret23unit tests/four-ABI JNI/reduced merged-permission/signed-package/API35 real relay/local-host/identity/settings/metadata-browser-choice/defaultOFF smoke40678487225(sourcedd3302f). Later changes to the native-app inputs are absent: only live-probe/tests/evidence changed. Not two-phone/physical-device/security certification.
- Installed Store must declare the app deep-link handler AND match reviewed canonical published APK metadata/hash, not certificate alone. Public0.5 lacks the handler, so it correctly uses app-specific website fallback rather than misleadingly open a generic Store home screen. A capable privately-signing-reviewed Store update is required for end-to-end Store-side app-selection acceptance; never force installation.
- Live Guard's Tamil runtime/launcher/widget/guide name is now **கைபேசி காவலன்**, not only the website card label. No new public APK/version overwrite.
- **SMS live diagnosis narrowed, not declared fixed:** same-runner40680202248 app and upstream clients fail only Valluvar among four tested profiles; app Pallavi/Prabhat/Neerja return complete audio. Provider metadata still advertises Valluvar. Strict all-four live job remains failed; no DNS skip-as-success or owner-profile inference. Production4200ms/HD-only/OTP/privacy/voice preference behavior unchanged. [Actual results and limits](sms-live-voice-diagnosis-2026-10-28.md).
- SMS1.1.10 harmful-install warning remains unresolved despite owner runtime acceptance; Store-debug/Expense holds remain. No warning classification was disclosed even after More details; do not ask again, infer false positive, bypass protection or delete working data. Signing finding is not asserted to be the warning's cause.
- Store0.5/DailyVault0.3 public assets remain unchanged. DailyVault's discoverable bilingual Guide and Store description cleanup are implemented/tested in SOURCE, not claimed installed in those old public bytes; newer APK rollout awaits signing/security gates.
- Official12-app stable-public catalog/website build passes; main integration/deployment is the remaining source/website delivery step, not a new APK publication. New app-directory expansion and new privately-signed release synchronization remain gated, not silently complete.

## Historical checklist and evidence

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

## Latest owner report and playback-control candidate

Owner installed Voice1.3/code4; update-check dialog now succeeds and shows no newer stable version, opt-in daily check off. Owner reports one functional bug: active voice cannot be stopped/switched. Fixed as1.3.1/code5 (sourcef1de08c), verified signed candidate run40667074846/artifact12806208921 and debug run40667074766; device validation remains pending. Tap same active square Stop to cancel during loading/playback; tap another note to replace. 145 Python tests passed; three JVM fake-backend cancellation cases also pass CI.

Owner says SMS1.1.10 works; installation screenshot still explicitly shows harmful block. Later aggregate clean scan is not clearance of that install verdict. Withdraw recommended SMS candidate installation/publication pending detection classification. Do not bypass protection or recommend deleting SMS/finance data. Store/Expense holds remain. Exact warning details not expanded in supplied screenshot.

## Voice1.3.2 owner acceptance — 28 October 2026, 15:37 IST

Owner reports “Its working fine now all perfect.” Mark the reported Voice playback-control and new-note visibility bugs resolved on the owner's device with1.3.2/code6 candidate160 (run40668667451/source41f29c3/artifact12808020972). Screenshots show the distinct upgrade from1.3.1/code5 and “looks safe” installation verdict. Not independent installed-hash proof, all-device/security certification, publication or merge. Keep remaining SMS/Store/Expense security and suite-rollout gates open.
