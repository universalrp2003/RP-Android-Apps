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

## Preview 0.2 — installed-state follow-up

Owner screenshots show preview 0.1 running in Tamil/English, download progress and an Android installer prompt. They do not independently establish completed installation of every app. The unknown-source warning is a sensitive-permission disclosure, not an APK malware verdict; do not bypass Play Protect or recommend uninstalling a newer candidate just to test the store.

0.2/code 2 refreshes local installed versions on return to the foreground and package added/replaced/removed broadcasts. Installed apps get Open (or Android app info if no launch activity). Current/newer versions are not offered another Install/Download action. Older numeric versions get Update with installed/offered version details; unrecognized preview labels are not guessed into update prompts. Signer mismatches block store updates and warn against uninstall/data loss. The model also blocks downloads of same/newer/unknown versions, and archive versionCode/signer checks remain mandatory. All/Installed/Updates filters compare only the bundled catalog snapshot, NOT a live Google Play-like remote feed.

The screenshot's Vasanthi 1.1.5 and Podcast 1.6 are newer than the public catalog entries 1.1.4/1.4, so this preview intentionally offers Open, not a downgrade. Existing applications' self-update mechanisms remain unchanged. Android/device validation of this new revision is pending the preview workflow and owner review; cancellation is not treated as an install success.

## Preview 0.3 — compact home and quiet opt-in reminders

Main screen keeps header, search, filters and app actions; long privacy/catalog/verification text is behind Help/About (collapsed by default). A lightweight local vector illustration inspired by Tamil Nadu temple/palm scenery decorates the header, with no image download or tracking. Contact uses ACTION_SENDTO for `universalrp2003@gmail.com`; no email is sent automatically and a mail app is required.

Daily reminders are OFF by default. Enabling them requests notification permission where required, schedules a non-exact 24-hour WorkManager check, and compares installed versions only with the bundled snapshot. No live release feed is implied. No notification for zero updates, permission denial, or failures. One aggregate silent low-importance notification at most per rolling 24 hours; no vibration/sound requested. Android/OEM settings and background limits may affect delivery; users can switch it off. WorkManager may add normal wake/boot/network-state permissions through manifest merge; it does not receive permission to read private user data. Old app self-updaters remain unchanged.

The reported Secret Message signer mismatch exposed a real catalog pin assumption: Secret Message uses a separate established release key. Preview 0.3 pins its exact certificate from `secretmessage/signing/certificate-sha256.txt` / its updater (58e370e4…bc04f08) and retains the legacy pin for the other apps. Unknown/mismatched keys still fail closed; this is not trust-on-first-use or bypassing a signature check. Device confirmation is required before claiming the reported mismatch resolved.
