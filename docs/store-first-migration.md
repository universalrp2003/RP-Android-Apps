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
