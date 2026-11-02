# Live Guard 2.85 private candidate — investigation and verification

## Owner feedback and scope (2 November 2026)

Owner confirmed Android's actual “not responding / Wait / Close app” dialog briefly appears on Read news in private 2.84. Network changes showed an HD warning and NETWORK_CHANGE engine error. Owner chose **GDACS, IMD, Tomorrow.io**, compact browser plus Expand and selectable/preserved zoom where supported. Actual GDACS event-map rendering on the owner's phone and Tomorrow-labelled weather results are limited evidence, not map/forecast accuracy proof. Raw screenshots/locations are private and not included here.

## Findings and changes

1. `MainViewModel.readTodaysNewsNow` launches on main. Previously, the fetch switched to IO but returned to main for selection/AI evidence/JSON work and history after playback. The whole manual entry is now IO-safe and `NewsBulletinPipeline.prepare` explicitly moves selection to Default for every caller.
2. Announcer posted `onStarted` to main; its news callback performed a synchronous preference commit. Start checkpoints are now dispatched off main. The main/UI story-status reader is read-only and no longer waits for the watcher monitor while a worker commits completed-story history. Completed-story commits still precede next-clip scheduling; interrupted stories remain unread. Delayed start checkpoints cannot overwrite newer audible activity.
3. Main-thread cancellation invoked `MediaPlayer.stop`, which can block behind prepare/decoder/binder work. Cancellation now interrupts the worker and schedules Stop on a dedicated player-control worker using the captured old player. Shared voice-engine code and other accepted apps are unchanged.
4. The HD warning used an OR of last automatic and latest runtime failures. This could keep warning after a later manual success. The warning now uses latest runtime stage; separate last-automatic evidence remains visible. Network-change uncached synthesis waits up to five seconds for a 750ms same-validated-route window; network instability is not charged as a provider error. Relevance checks reject superseded transport announcements. This is resilience work, **not proof of the phone's HD-provider root cause**.
5. The viewer lazily retains at most three WebViews during one open window, detaching/reattaching instead of reloading on tabs/Expand. Page zoom is separately bounded/persisted per provider with display-density-aware restoration. Website-owned geographic zoom/layers remain untouched. No documented cross-provider map-zoom deep-link API was established, so persistence across closing/reopening/rotation or provider layout resets is not promised. The public Tomorrow India radar page is used, not a fabricated `/maps/` path. Website layers/timestamps are provider-owned; “radar” naming is not a claim of observed coverage at the selected place. Home/place label is context only; choose location on website.

**No owner ANR trace or logcat was available.** These are concrete main-thread hazards and a stale-warning logic bug found in source, not a confirmed unique explanation for the owner's ANR. Real-device reproduction/retest is still required.

## Security/data-flow review

- Actual pages, HTTPS top-level scheme/host/userinfo/port allowlist; only known GDACS legacy HTTP links are upgraded before navigation. Lookalikes, arbitrary subdomains, credentials, active schemes and nonstandard ports stay blocked. TLS errors cancel; safe browsing on; no mixed content.
- No native/JS bridge or injected scripts, API key, place coordinates or Android location. File/content access, geolocation, third-party cookies and multiple-window support off. Public site scripts/subresources are provider-owned and may contact their own vendors; not a full network sandbox. Websites receive ordinary browser connection metadata. Provider storage uses WebView's existing app-wide website store; no claim of separate cookie profiles between apps/windows. No raw owner evidence or credentials persisted in diagnostics.
- Only bounded numeric page zoom is newly persisted; live URLs/map state remain session memory. Up to three lazy WebViews, destroyed on close. No new permissions/dependencies, sibling product changes or publication. The shared directory candidate version is aligned to 2.85; its publishedVersion remains 2.82, and accepted public APK bytes remain unchanged.
- SACHET remains a separate explicit official-local-alert browser link, not a fourth tab.

## Verification status

**PASS: private verification run [41395967905](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41395967905), job125117239541, 13m40s.** Exact tested source `d4b49084aec222e7e83dd50dc6f98ee23600c2e3`; later documentation commits do not change the APK.

- 309 Python source tests; **610 executed required JVM regressions /65 suites**, none missing/skipped; release compile/lint/sign PASS.
- APK `CleanSweep-v2.85.apk`,2.85/code90,14,653,079 bytes; SHA-256 `84a36c0cc8973122cca069341a3b0bcb81eff6a2b5d932d8ec95657f85cb2f1f`.
- Signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`; zero added/removed merged permissions versus public2.82. Same UID, data/preferences preserved, restart PASS for normal same-key2.82→2.85 update.
- Native synthetic English UI PASS: opt-in Advanced/thresholds, place-first/home,60-minute lead, saved-place history; compact website-window controls, GDACS/IMD/Tomorrow.io tabs, Expand/Compact, page-zoom menu and Close. **Not** proof of actual provider map geometry, geographic-zoom persistence, chosen page-scale restoration on a phone, live API entitlement, audible HD output or ANR fix.
- [Private artifact13120046127](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41395967905/artifacts/13120046127), `Live-Guard-2.85-signed-tested-candidate`, ZIP14,197,270 bytes, expires2December2026. Hash/bytes are CI evidence; no independent local APK download/hash.
- [Machine-readable receipt](live-guard-2.85-candidate-verification.json). No physical-phone or Play Protect clearance claim. Public2.82/site/catalog unchanged; no publication.

First attempted run41395837407 stopped before compilation: missing literal phone-test-status sentence and directory candidate version mismatch in source guards, both corrected. A later historical2.84 rerun41392195951 failed only collecting its first UI hierarchy (no file); the2.85 native collector now retries hierarchy collection a bounded three times and never reuses a stale dump. No install/data-reset workaround is used.

**Remaining owner checks:** normal update from installed2.84 (do not uninstall); Read news while scrolling/back/Stop; actual Wi-Fi↔mobile HD output and latest diagnostics; provider-owned map controls and state through tabs/Expand. ANR trace/logcat, if the dialog repeats, is needed for a defensible specific diagnosis. No raw owner evidence is published.


## References consulted

- Android ANR diagnosis: https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs — input timeout commonly five seconds; avoid main blocking I/O, binder work, long CPU work and lock contention; traces needed for root-cause diagnosis.
- WebView bridge risks: https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges — no bridge is exposed here; narrow navigation and no privileged app object in provider pages.
- WebView scale API: https://developer.android.com/reference/android/webkit/WebView — setInitialScale does not account for screen density; zoomBy is clamped by WebView limits. Page zoom is not geographic map zoom.
- https://www.gdacs.org/ — actual OpenLayers map and provider attribution; legacy HTTP report links exist. No documented external zoom-control contract established.
- https://rsmcnewdelhi.imd.gov.in/ — actual regional warning website, graphics/PDF links; controls depend on content.
- https://weather.tomorrow.io/IN/radar/ — fetched public India radar page; no private-platform entitlement or selected-home centering inferred. `https://www.tomorrow.io/weather/maps/` redirected to a not-found page and is not used.
