# தமிழன் — Tamilan Android application store (preview)

Owner-approved isolated first-party store for the twelve RP apps. Not Google Play, not an official F-Droid client or repository. No public release, store listing or device compatibility certification is claimed.

## Implemented preview

- Tamil-first with persisted English switch, searchable bundled catalog of twelve public releases (snapshot 28 October 2026), installed-version display, release/help/privacy links.
- Single user-initiated download with progress, cancel and retry. APKs stay in private cache, one at a time. No account, ads, analytics, other-developer uploads or background inventory upload.
- HTTPS-only canonical release URLs; bounded redirect hosts, byte limit, size and SHA-256 checks. Package/version/minimum SDK and pinned single signer checked before handoff; installed signer and downgrade checks fail closed. Android's package installer performs final platform validation and user confirmation.
- Unknown-source settings open only when the user selects Install. The user must return and tap Install again; never disable Play Protect. FileProvider exposes only the verified cache directory, not all storage. Package visibility is scoped to twelve packages; no QUERY_ALL_PACKAGES or storage permission.
- Process termination requires a new download; preview has no background/resumable download or notifications. In-flight work survives configuration changes in a ViewModel, not process death.

## Explicit remaining gates

1. Android CI build, lint and tests; no local SDK/build verification claimed.
2. Exercise redirect, truncated/wrong-size/hash APK, wrong package/version/signer, downgrade, cancellation, low storage, installer rejection, configuration change and process death on devices. Revalidate each public app's signing identity; pinned-signer mismatch blocks installation, it must not be bypassed.
3. Test Android 8 and current Android, large fonts, screen readers and package visibility. A displayed installed version does not prove update compatibility.
4. Current catalog is BUNDLED, not a remotely trusted live update feed. It clearly labels its date; newer releases require a new snapshot/store build. Signed remote catalog, freshness/rollback protection, automatic update discovery, categories, richer descriptions and screenshots remain next-stage work. No private key/token may be bundled for this.
5. Review installer-permission store policies before seeking Play listing. Separate privacy policy, release signing continuity, owner phone sign-off and a standalone publication gate are required before shipping.

The preview is intentionally not in the twelve published-app directory or website release table. Existing apps' update mechanisms and releases are unchanged. `.github/workflows/tamilanstore-preview.yml` builds only a test artifact and has read-only repository permissions.

## Build

`gradle :tamilanstore:testDebugUnitTest :tamilanstore:lintDebug :tamilanstore:assembleDebug`
