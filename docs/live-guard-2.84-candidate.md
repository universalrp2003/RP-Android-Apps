# Live Guard 2.84/code89 — simpler private candidate

**Not published. Signed build/upgrade/native checks PASSED for exact source `80757effea6d971c860249f0bff9d041761acb40`.** Public Live Guard 2.82 and all other accepted apps stay unchanged. The 2.83 receipt applies only to its exact old source/APK, not this revision.

## Owner decisions, 2 November 2026

- Normal “Battery reached 100 percent” speech; Advanced battery checks OFF by default. Only opt-in reveals and enables the sustained current/power estimate announcements. Missing sensors are never zero. Call/silent-place/master voice protections remain.
- Default town/PIN confirmed and saved first. Existing places preserved; a separate optional current-GPS place changes at weather checks, not continuous tracking. Never overwrite home. Co-located watches share a request and do not duplicate speech in the same check; a GPS result is hidden when the latest available fix no longer matches. Reject old GPS fixes; moving more than 1.2 km resets only that place's speech episode.
- Tomorrow.io minute data required for precise rain announcements. If unavailable, pause these alerts and display a clearly labelled general outlook only. User selected STRICT, not automatic substitute alerts.
- Disaster reports SCREEN ONLY; no automatic GDACS disaster speech. Actual GDACS report/map opens in an isolated app WebView, no local track recreation or fabricated personal-location marker.
- Compact per-place results: name, provider estimate now, rain time within validated minute horizon, later available outlook, source and check timestamp. No invented rain-probability labels. No-rain claims are limited to the actual available horizon.

## Research and limits

- Tomorrow.io forecast documentation: https://docs.tomorrow.io/reference/weather-forecast — minute data for premium users; website accuracy observed by owner does not establish this API key's entitlement. Diagnostic codes distinguish denied access, quota, hourly-only and general unavailability. No keys in logs or URLs. Rechecked official Access Keys documentation (api-authentication.md): the `apikey` header is supported. Use the documented POST Timelines body (post-timelines.md) instead of relying on GET parameter serialization. Rate-limiting.md identifies HTTP429; hourly/daily request budgets depend on plan. Rain checks no longer fetch GDACS on each place request.
- GDACS actual report example: https://www.gdacs.org/report.aspx?eventid=1001346&episodeid=5&eventtype=TC — regional impact classification, map layers/track/cones, dated summaries. Green is not a local all-clear. Display full provider attribution and disclaimers.
- https://sachet.ndma.gov.in/ and /CapFeed — official area-specific CAP warnings; agency integration guide could not be retrieved in research. No unverified CAP endpoint or fabricated local-warning ingestion. Link the actual official portal in the app window instead.
- IMD/RSMC remains regional cyclone warning authority: https://rsmcnewdelhi.imd.gov.in/.
- Supplementary summaries use bounded recent GDACS cyclone/flood/earthquake records and reference-point distances, NOT warning boundaries or predicted local impacts. Failed feeds and empty matching lists differ. Do not average incompatible wind/track predictions.
- WebView HTTPS top-navigation allowlist, no app JavaScript interface, no file/content access, no geolocation grant, no TLS-error bypass, no third-party cookies. Full GDACS WebView rendering still requires a real device/network check.
- Android background checks are best effort, typically WorkManager's minimum 15-minute interval, not a guaranteed 15/30/60-minute warning service. No perfect local forecast/alert-delivery guarantee.

## Verified automated validation; remaining physical boundaries

PASS: 302 source Python tests; 591 required JVM regressions/63 suites; lint/release compile; same-key normal update from public2.82 with UID/data/preferences/restart preserved; full permission continuity; native home-first/Advanced/lead/history and actual website-window open/close smoke. Physical audio, real Tomorrow.io access and plan, local forecast accuracy and actual website rendering are not emulator proof. Never uninstall or clear owner data for testing.

## Exact private download

[Actions run 41390875538](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41390875538), job125101989915, SUCCESS in12m9s.

[Download artifact13117224512](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41390875538/artifacts/13117224512) and extract **CleanSweep-v2.84.apk** only. Normal Android update; do not uninstall or clear data.

CI receipt:14,640,383bytes, SHA-256 `c39acc068f0960ad0a19bc18bf049000d3c8321808df1ee7fa23ff5b23842998`, unchanged signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`; no added/removed permissions. Artifact ZIP14,184,554bytes, expires2December2026. Blob download is unavailable from this sandbox, so these are CI-inspected facts, not an independent local APK download/hash check.

[Machine-readable receipt](live-guard-2.84-candidate-verification.json). Public2.82 and all other accepted public assets are unchanged. The old workflow filename still contains283; actual APK version/code, signer and permissions were inspected as2.84/code89.
