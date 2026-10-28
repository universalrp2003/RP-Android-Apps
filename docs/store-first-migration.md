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
