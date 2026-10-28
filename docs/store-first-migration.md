# Store-first update migration

Owner approval: 28 October 2026. Supersedes the earlier instruction to retain app-local APK self-installers. Do not remove working updater paths until the replacement has been tested.

## Ordered gates

1. Live official catalog, strict identity/URL/version validation, last-known-good offline fallback and unchanged APK signer/hash checks. Store refresh must never say live check succeeded when it failed.
2. Store-specific app links with safe browser fallback. No forced Store install. Store itself uses browser release handoff to avoid recursion.
3. One pilot application: optional silent bounded reminders, explicit user action, no app-local APK download/install. Validate Store installed/not installed/offline/malformed feed/cancel/downgrade.
4. Roll out shared updater replacement and app-specific implementations only after pilot acceptance. Keep OTP/voice/privacy behavior unchanged. Remove obsolete installer permissions/providers only after checking all call sites.
5. DailyVault discoverable bilingual About/Guide; SMS Guard network-test diagnosis and distinct release version; Voice Notes distinct release version and lock regression tests.
6. Publish verified signed builds and synchronize website/live catalog after release existence/hash verification. No overwriting public APK assets. Record exact CI/source/hash and owner-device evidence separately.

Status: planning and foundation implementation in progress. No claim of finished suite migration or new publication.

## Foundation source changes

- Added catalog generator using only canonical stable public GitHub APK metadata/digests, paginated with a fail-closed bound; Pages builds regenerate site/catalog.json. No CI artifact enters the live feed.
- Store manual Refresh now fetches HTTPS catalog with no redirects, 256 KiB bound, package/id pinning, numeric no-rollback policy and same-version content immutability. Last-known-good catalog cached; failures retain saved/bundled catalog and show offline status. Existing APK hash/signer checks remain.
- Store accepts bounded tamilan://app/<known-id> links to filter the desired card. Added shared explicit-component Store router with website per-app anchor fallback. Router is foundation only; no app caller has yet migrated.
- DailyVault Guide now appears near the header and covers entries, lock modes, backup/restore replacement and no password recovery. Removed stale preview text.
- Existing twelve-app catalog scope, bundled notification worker and old self-download functions remain until the pilot is verified. No suite-migration publication performed by these changes.

Pending: CI/device verification of foundation, Pages live endpoint confirmation, pilot notification/update caller, remaining app-specific migrations, SMS Guard live integration test diagnosis, Voice Notes/SMS new release versions and publication.

## Pilot and release-test separation

- Voice Notes pilot adds an explicit Check updates action and in-app Update available notice, routing to the Store or per-app website anchor. No APK download/installer added. System/background reminders and suite rollout remain pending; do not describe this pilot as finished notification migration.
- SMS Guard live neural synthesis assertion is unchanged, moved to dedicated testLiveSmsVoice task with real failure reporting. Deterministic release tests exclude that exact external-service method only. Scheduled live monitoring uploads the live results separately and marks probe failure visibly. This fixes release-test categorization, NOT the external speech service outage; owner previously confirmed incoming SMS voice on the device.
- Existing published versions are unchanged during foundation/pilot review. New public versions, app-local installer removal and final release/catalog synchronization remain gated on CI and device review.

## Verified foundation/pilot CI checkpoint

- Store foundation: run 40647956504, source fdb2aa5ef50cbf3cccdbcb2346dd853ac70b4d57, success (debug unit tests/lint/build; includes live-feed validation tests).
- DailyVault visible Guide: run 40647956491, same source, success (debug tests/lint/build).
- Voice Notes Store-first pilot: run 40648114993, source dea25de231f284e196dfe26e9559901577bbd277, success (debug tests/lint/build).
- SMS Guard deterministic release regression gate: run 40648183451, source bad323dcbf02c6c5b523a725cd4831447d79b087, success (release unit tests/lint/build, signer/package checks). APK 12461665 bytes, SHA-256 c08ff58bdfe85a84c9b210f61a206bec5174e17da961067f094e12d5319a9d9b. External voice monitoring remains separate, not newly verified healthy.
- Pages run 40648178396 succeeded, including live catalog generation. Endpoint behavior on the actual device is still pending; deployment status is not direct runtime verification.
- 142 Python tests passed. No new public application release or legacy installer removal at this checkpoint.

NEXT: owner-device pilot verification (Store app-specific routing and website fallback), then background notification/skip-version policy and suite-wide migration. Existing Store 0.5/DailyVault 0.3 public releases remain available. These intermediate APKs retain the same version labels as the public base and are test artifacts only; final public rollout needs distinct version/code. Voice Notes and SMS Guard stable releases are not finished/published by this checkpoint.

## Safety hold and latest UI feedback

Owner's next screenshots show Play Protect “Harmful app blocked” for Store debug pilot and Expense Tracker. Do not infer false positive, DNS cause or resolution from “all working.” Suite rollout is paused pending exact detection text/hash/permission review; see play-protect-investigation.md. Do not recommend bypassing Play Protect. Voice Notes search field removed per owner, spacing reduced and update-check results moved into an explicit dialog. Source tests/localization passed; Android build pending. No claim of verified Store/browser fallback based solely on installation screenshots.

Voice Notes UI follow-up verified: run 40651513745, source 4b7156d, succeeded debug unit tests/lint/assemble; artifact 12800026324 (voice-notes-preview), reports 12800156198. 143 Python source tests and localization checks passed. Test-only intermediate build, existing version label unchanged; not a new public release or device validation. Play Protect investigation remains open.

## Update-check deployment follow-up

Owner's new screenshot confirms result dialog renders but check is unavailable. GitHub Contents API returns 404 for main/site/catalog.json; Pages deployment is intentionally main-only. A successful branch Pages build must NOT be called a deployed endpoint. Added read-only canonical GitHub Releases metadata fallback for transport/HTTP failures only, with canonical tag/asset validation, size/digest checks, bounded body/pagination and cancellation. Missing/malformed metadata remains an error, not “no updates.” Invalid website schema/identity is not bypassed by fallback. No APK downloaded or installed. Preview lane now triggers on shared changes. Tamilan About's Google Play/F-Droid reference removed in both languages per owner. New preview CI pending; suite migration, live external synthesis, signed public versions and Play Protect investigation remain open.

Follow-up verification: Voice Notes run 40659196019 (source 0bdf3cb) succeeded unit/lint/build; artifact 12803007871. 144 Python tests and localization passed. Regenerated the 12-app feed against actual public release API metadata: unchanged; public SMS remains 1.1.9 and Voice Notes 1.2. No release published. Manual live workflow dispatch still returns integration HTTP403; a marker-gated, branch-only SMS live probe preserves real failing assertions and reports independently (not a release/publish workflow).

## Latest execution result

- Store run 40659196161 passed unit/lint/build; artifact 12803581184 includes requested About wording removal.
- Fresh actual SMS probe 40659911399, source c593f80: failure. Check annotations confirm LIVE tests=1/failures=1/errors=0/skipped=0; `ta-IN-ValluvarNeural` returned null MP3. Deterministic suites shown in that run: 5+6+25 tests, all zero failures/errors/skips. Result artifact 12803318391. No DNS/NextDNS cause established. This is a real executed assertion, not NO-SOURCE or a successful live-service recovery.
- Initial fresh probe 40659611059 also failed; full log retrieval from results-receiver host was unavailable in sandbox. Added bounded JUnit check annotations so the next real probe exposed the precise assertion via GitHub API without publishing raw logs/voice text.
- No suite-wide installer removal, notification rollout, PR merge, website deployment or new signed public app release performed. Current pilot/deployment/security gates remain explicit; do not substitute unit/build success for device routing or Play Protect clearance.

## Distinct signed candidate preparation

Prepared Voice Notes 1.3/code4 and SMS Guard 1.1.10/code16 (not public). SisterApps publishedVersion still points to the actual public 1.2/1.1.9 releases; website/feed unchanged. Voice signed candidate can now run via explicit candidate marker with publication false; release lint is a mandatory gate instead of an ignored report. Removed misleading SMS artifact name v1.1.0. New bilingual notes explicitly state no exact-byte physical-device test. Safe branch implementation continues while publication/device/security gates remain open.

Research: compared rany2/edge-tts upstream constants, DRM and communicate implementations via GitHub API; endpoint and client version agree with our implementation. Upstream issue 512 reports intermittent no-audio failures, but this does NOT diagnose our CI. Diagnostic probe 40661025735 actually recorded DEADLINE_EXCEEDED / COMPLETE=false / BYTES=0 at 8s, with no HTTP failure callback. Added handshake/close phase categories, proper peer close response and a 20s LIVE-TEST-only deadline; production default timeout unchanged. Do not infer 403, TLS failure, NextDNS block or service recovery without new evidence.

Voice pilot reminder implementation: opt-in daily app-opening metadata checks, disabled by default; attempts bounded to 24 hours, including offline failures. Automatic checks never open a result/error dialog; explicit manual check always reports. Added Later/Skip version with manual override; settings live inside the result dialog to avoid adding main-screen clutter. No background/system notification claims: this is in-app only. Added JVM tests for default/first/24-hour/clock rollback/skip/manual semantics. Suite-wide rollout still follows pilot validation.

## Verified signed candidates and transport diagnosis

- Voice Notes 1.3/code4: run 40661909175, source 875f12f, SUCCESS: unit tests (including daily/skip policy), mandatory release lint, signed release assemble, package/version/non-debuggable/pinned signer/size/SHA verification. Artifact 12804657034, `voicenotes-stable-candidate-156`. PUBLISH_REQUESTED=false; publish steps skipped. Exact bytes not device-tested.
- SMS Guard 1.1.10/code16: run 40662052275, source 60e7931, SUCCESS: deterministic unit tests, release lint/assemble, ROLE_SMS/package/version/pinned signer/SHA checks. Artifact 12804632569, `SMS-Guard-signed-candidate`. APK 12478049 bytes; SHA256 a558254afcaa462f0e8a2ff56633b4242aae034c3b8f6ca779519b904b4ba4c7. Test only, no public release. Exact bytes not device-tested.
- Separate real live probe 40662034689, source 60e7931, FAILED. Valluvar request established HTTP101, peer closed1011 with zero audio; one bounded transient retry also established101 then closed1011 without audio. Thus DNS/TLS/WebSocket upgrade succeeded for these CI attempts; this is not evidence of a NextDNS block or HTTP403. It does not establish service-wide outage or all-user failure. Artifact 12803927921. Live assertion remains intact; no guaranteed recovery claimed.
- Fixed actual client discrepancies found during research: canonical server voice name in SMS (matching upstream and shared engine), peer close response and remaining-deadline accounting for clock/transient retries. One transient1011 retry only, never auth/policy bypass; shared HD engine uses same policy. No robotic fallback added to Voice Notes. Runtime timeout defaults unchanged; live-test timeout is20s.
- Read-only upstream research sources: https://github.com/rany2/edge-tts/blob/master/src/edge_tts/data_classes.py (canonical voice normalization), constants.py/drm.py/communicate.py (endpoint/client/request comparison), https://github.com/rany2/edge-tts/issues/512 (other users report no-audio failures; not proof of our cause).
- 144 Python tests, localization and diff checks passed. No PR merge, public release, Pages deployment or suite-wide migration claimed. Remaining: exact-byte device check/routing approval, website main integration/deployment, remaining app-local installer removal, optional system/background notifications if required, Store/Expense Play Protect detection/byte investigation. In-app daily pilot reminders are implemented, opt-in and disabled by default, with Later/Skip; they are NOT background notifications.

## Owner feedback and playback-control fix

Latest owner screenshots confirm Voice1.3/code4 installed over1.2/code3 and update check reports no newer stable version; daily app-opening check initially off as intended. Store-installed/browser routing to an actual newer release remains untested. Owner reports SMS1.1.10 functioning but Play Protect blocked its installation; later clean aggregate scan does not clear this verdict. SMS publication now held alongside Store/Expense investigation.

Voice playback bug confirmed in source: active playback returned early for every subsequent Play tap; active row had only a spinner. Preparing1.3.1/code5: same button becomes Stop during synthesis/playback; another note cancels/replaces current request; request-token guard prevents stale finally/status updates; generation invalidates late audio; cancellable blocking work and mutex prevent overlap; unique temporary clips cleaned up; deleting active note, ON_STOP and disposal stop playback. Added three JVM fake-backend tests (stop between chunks, cancellation/release before replacement, late synthesis after stop) and source UI guard. This is not real MediaPlayer/device proof; CI pending. No app release or Play Protect clearance claimed.

Playback-control candidate CI verified: Voice Notes1.3.1/code5, source f1de08c, signed run40667074846 SUCCESS (unit tests including fake-backend stop/switch/late-synthesis cases; mandatory release lint; assemble; package/version/non-debuggable/pinned signer/size/SHA checks). Artifact12806208921, voicenotes-stable-candidate-159. Debug unit/lint/build run40667074766 also SUCCESS. 145 Python source tests/localization/diff passed. No public release; exact-byte device Stop/switch behavior pending. SMS candidate remains under the newly observed installation-warning hold, despite owner-reported functionality and later clean overall scan.

Owner next reports Voice1.3.1/code5 “Everything ok” except newly dictated note hidden above list/microphone area. Treat Stop/switch as owner-device acceptance, not universal compatibility or exact-byte hash proof. Source cause: keyed LazyColumn retains previous visible-item anchor when prepending. Preparing1.3.2/code6: explicit remembered list state and scrollToItem(0) keyed only by newly dictated note ID, not edits/deletes. No microphone overlay/layout rework. CI/device layout verification pending. Owner expanded SMS warning: no extra classification, only Install anyway. Do not repeatedly request same details, infer false positive or encourage bypass. Existing warning/publication hold remains.

New-note visibility candidate verified: Voice1.3.2/code6, source41f29c3. Signed run40668667451 SUCCESS (unit tests, mandatory release lint, release build/package/version/non-debuggable/pinned signer/size/SHA gates); artifact12808020972, voicenotes-stable-candidate-160. Debug unit/lint/build run40668667405 SUCCESS. 146 Python source tests, localization and diff checks passed. No public publication. Owner-device insertion/scroll validation still required; long notes can exceed viewport and remain normally scrollable.

## Owner-device acceptance — 28 October 2026, 15:37 IST

Owner reports Voice Notes1.3.2/code6 “Its working fine now all perfect” after the new-note visibility candidate. Treat the reported playback Stop/switch and new-note visibility issues as resolved on the owner's device. Screenshots show candidate160 / Voice-Notes-v1.3.2.apk, installer upgrade1.3.1/code5 →1.3.2/code6 and Play Protect “This app looks safe” for this attempt. Candidate supplied: signed run40668667451/source41f29c3/artifact12808020972. Installed hash is not independently matched; device acceptance/one scan is not universal compatibility or security certification. No new code, public release or PR merge requested/performed by this acceptance. SMS/Store/Expense warning investigation, remote live-service failure and remaining suite/deployment gates are unchanged.

## Suite migration implementation — next gate

After owner accepts Voice1.3.2, replace shared AppUpdates installer with metadata-only Store-first notice compatibility entry; migrate Live Guard's separate updater/card/setup wiring and Secret Message's separate updater without touching MLS/chat/native/signing identities. Remove only dedicated updater permissions/providers/path resources; preserve media/attachment providers and own alerts. Existing browser-only app wrapper now offers explicit Store-first and direct browser handoff without requiring app network permission. Router validates Store's pinned signing certificate and always exposes a browser alternative; package visibility queries are explicit.

Secret Message compiles exactly eight allowlisted handoff source files via generated build sources, not the shared voice/other-app code. Secret app uses its own Tr language and signing identity; Store signer pin is the Store's RP identity. No cryptographic/chat source changed. Existing twelve-app Store APK verification gates are unchanged. Updated retired-reader tests to reject, rather than require, the now-retired installer; introduced suite manifest/source and actual merged-APK migration gates.

150 Python tests and existing localization/cleaner static guards pass. Android multi-module and Secret native/merged-manifest builds pending. These are source migration changes, not public APK releases or evidence that Play Protect warnings clear. Existing published versions/assets/data remain unchanged; no same-label public overwrite allowed. Optional daily checks are in-app only and off by default; no background/system-notification completion claimed. Fresh source invalidates earlier candidate hashes for affected modules; do not distribute these rebuilds as previously accepted APK bytes.
