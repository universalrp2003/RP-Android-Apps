# Live Guard 2.84/code89 — simpler private candidate

**Not published. Build/upgrade/native checks pending for this revision.** Public Live Guard 2.82 and all other accepted apps stay unchanged. The 2.83 receipt applies only to its exact old source/APK, not this revision.

## Owner decisions, 2 November 2026

- Normal “Battery reached 100 percent” speech; Advanced battery checks OFF by default. Only opt-in reveals and enables the sustained current/power estimate announcements. Missing sensors are never zero. Call/silent-place/master voice protections remain.
- Default town/PIN confirmed and saved first. Existing places preserved; a separate optional current-GPS place changes at weather checks, not continuous tracking. Never overwrite home. Reject old GPS fixes; moving more than 1.2 km resets only that place's speech episode.
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

## Validation to complete

591 required JVM regressions/63 suites, lint/release compile, same-key normal upgrade, full permission continuity, and native home-first/Advanced/lead/history smoke. Physical audio, real Tomorrow.io access and plan, local forecast accuracy and actual website rendering are not emulator proof. Never uninstall or clear owner data for testing.
