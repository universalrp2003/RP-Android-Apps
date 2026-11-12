# Technical handover for the next developer or agent

Snapshot: 4 November 2026. Start read-only. This document does not authorize a build, signing-key change, deployment, release, external upload, paid resource or account migration.

## Repository and evidence

- Repository: https://github.com/universalrp2003/RP-Android-Apps . Main public Oor integration: PR88, merge commit `ce15e055a54b094e4e32c86175e352ffa187dac6`.
- This continuity task is on `arena/282fce0a-rp-android-apps`. Future helpers must follow the branch assigned by their current environment; never switch to historical Arena branches mentioned in old notes. Do not reset/rename a tracked Arena branch.
- Read `AGENTS.md`, relevant module source/tests and the **newest applicable** receipt. Historical “pending”, branch, candidate-only and permission statements are not necessarily current status. Preserve history rather than silently rewriting old evidence.
- `release-inventory.json` is a dated metadata snapshot of 13 ordinary apps plus the separate Store. It was compared with canonical GitHub Release IDs/assets/digests, not with freshly downloaded binaries or installed phones. Check again before future work; do not treat it as an automatically updating feed.
- Catalogs: `site/catalog.json` (legacy12 entries), `site/catalog-v2.json` (13 entries). The published Store0.9 APK's embedded fallback `tamilanstore/src/main/assets/catalog.json` intentionally lags. Do not rebuild Store merely to refresh the live feed.

### Oor1.1.16 is already published and the website is live

Do **not** rerun a pinned publisher or replace the existing tag/assets.

| Evidence | Reference |
|---|---|
| Exact APK | `Oor-Arivippu-v1.1.16.apk`, code21,12891948 bytes |
| SHA-256 | `3d7119ae7d843ee57c4ca2a69df5ef6bfedf4e90210af20ceb4e74a6f5b42fee` |
| Native verification | run41669768816/job126034701119, artifact13249211486 |
| Exact publication | run41751132736/job126316197572; release433392185/APK707716347 |
| Public receipt | `docs/oor-about-guide-1.1.16-publication-receipt.json` |
| Main Pages verification | run41751753970, merge `ce15e055…`, successful main deployment and live metadata/eight APK+ZIP checks |
| Main website checks | run41751754209, success |

The final main deployment and public release status were independently rechecked via GitHub during this continuity task. Native evidence covers same-key public15→16 selected data/settings/UID/restart, an isolated synthetic encrypted FamilyStore fixture/actual Android Keystore continuity, bilingual About/Guide UI, existing family/HTTPS regressions and exact installed APK bytes. Three native tests and a separate baseline seed phase;30family+4About required JVM tests. Separate source check passed98unit tests with one explicitly named pre-existing external live-speech probe not run. This is not audible-phone, real-family-all-devices, OEM, GPS, security-audit or Play Protect proof.

The approved candidate and public APK are identical. Current public15 assets and the labelled15masked screenshot gallery remain intact. The Oor build defaults its family service URL to empty; **a generic unconfigured CI APK is not interchangeable with the accepted configured release**. Existing configured URL property: `ORG_GRADLE_PROJECT_oorFamilyServiceUrl`; the verified endpoint is recorded in the candidate receipt. Do not redeploy its service for this handover.

## Source and toolchain map

Root Gradle modules: `cleaner` (Live Guard), `radio`, `oorarivippu`, `smsguard`, `equalizer` (PulseEQ), `vocalremover`, `voicenotes`, `expensetracker`, `vasanthi`, `podcast`, `launcher`, `dailyvault`, `tamilanstore`, and `secretmessage` whose project directory is `secretmessage/product`.

Shared Android code: `shared/src/main/java`, including `updates/SisterApps.kt`, `updates/AppGuides.kt` and shared weather/audio logic. A shared change can affect multiple apps; keep unrelated behavior and releases unchanged.

At this snapshot: Java17, Gradle wrapper8.14.3, Android Gradle plugin8.13.0, Kotlin/Compose plugin2.2.21. Inspect the selected module for exact SDK/NDK requirements; Oor compiles/targets SDK36 and supports API26+. Other modules and native components may require additional tools. A clean machine also needs Android SDK access and dependency downloads; public source alone is not an offline reproducible build environment.

### Safe source checks

From the repository root, before requesting a candidate:

```bash
python3 -m unittest discover -s tools/tests -q
python3 tools/verify_apps_localization.py
python3 tools/verify_release_signing_policy.py --fresh-install
python3 tools/update_website_release_dates.py
git diff --check
```

`--fresh-install` here is an existing **policy checker option**, NOT an instruction to uninstall or reset a phone.

With the appropriate SDK/JDK, targeted Oor source checks are:

```bash
./gradlew :oorarivippu:testReleaseUnitTest :oorarivippu:lintRelease
```

These commands are not APK assembly, phone installation or publication proof. Account for explicitly opt-in external tests honestly; do not disable required tests or call skips passes. Use hosted CI if the current sandbox cannot obtain Android tooling. Do not use historical branch-triggered native/publisher workflows without reviewing their pins and obtaining current authorization.

## Release discipline

1. Confirm the new request and scope. Keep deferred requests deferred. Inspect working-tree changes without overwriting user work.
2. Change only the requested app/shared behavior. Preserve privacy, saved settings, OFF choices and supported-platform behavior. Add focused tests.
3. For an authorized new APK, use a distinct higher version/code and the **existing correct per-package signer**. Materialize secrets privately, never from public literals or chat. No automatic key rotation.
4. Verify package/version/minSDK/nondebuggable/signature/full permission delta, required JVM/backend/native tests and same-key upgrade from the actual current public baseline. Emulator root helpers are disposable-emulator-only, never phone-root/protection-bypass instructions.
5. Pin the exact configured artifact, archive/APK hashes and test evidence. Candidate artifacts expire; preserve the selected authorized bytes privately. A rebuilt APK is not the tested artifact, even when source looks identical.
6. Ask for authorization to publish that exact artifact unless the owner already explicitly authorized this exact scope. Old “release everything” or prior-version acceptance is not blanket permission. Never overwrite public assets or rerun completed historical publishers.
7. Update only the requested app's actual public metadata, version/date/size/hash links and public feed after publication. Keep older mirrors. Run relevant CI before merge. Deploy Pages from main only and verify actual live HTML/feed/guide and exact APK+ZIP bytes.

Publishing history is in `PUBLISHING.md`, `docs/published-release-digests.md`, per-version candidate/publication receipts and `.github/workflows`. Some are historical and deliberately hardcoded; they are evidence/examples, not a reusable “run this now” cookbook. No generic publish markers on routine commits.

## Owner constraints and next work

- **No new feature is authorized by this handover.** No app rebuild, tag, public APK, worker redeployment or sibling update.
- Next *requested* Live Guard update only: reduce unused home gap above Free storage and make “Scan & clean junk”, including Tamil, compact without losing accessibility/font scaling. Do not implement it now.
- Same-key updates; no uninstall/data-clear/downgrade loops, no Play Protect bypass/clearance promises, no repeat already-accepted phone tests without a specific new need.
- Oor family: optional/defaultOFF, mutual verification, per-person persistent ON until OFF, upload only during a user-started trip. Trip stop pauses uploads/revokes its lease while preserving intent. Latest encrypted town/district/time expires within15minutes; no precise coordinates, map or route history. Device-only bounded family speech, not cloud HD or travel/speech history; receive only within the resumed window or existing trip service. Not emergency/continuous tracking.
- Preserve Oor district/history/About/labelled legacy mirror. Legacy removal was Live Guard-only. Other Apps links use published metadata; current self link is the live official website app section.
- Weather/disaster claims must reflect actual provider freshness/coverage/quota and Android lifecycle/audio limits; omit unknown data, preserve saved OFF. Do not infer location precision or rewrite saved home coordinates.
- Never publish private screenshots/names/towns/messages/voice notes. Only already approved opaque-masked15gallery derivatives are authorized. Preserve their paths/labels and originals' privacy.
- APK downloads only `/tmp`, never `/home/user` or the repository. No production secrets or private user data in an AI prompt/ZIP/log/PR. See the private checklist for separate signer and account custody.

If evidence conflicts or an account/backup is missing, report what is known and ask the owner before doing anything irreversible. Do not replace an unknown with a guarantee.
