# Live Guard 2.83 private candidate — evidence and boundaries

Public Live Guard2.82, Voice Notes1.3.4, Launcher1.7, Radio1.31 and Store0.9 remain pinned. This source/code88 candidate is not publication authority. A normal same-key Android update must retain data; no uninstall/reset/downgrade or protection bypass.

## Battery

The configurable estimate requires100% + Android Full + bounded documented current/voltage and sustained low battery intake. Defaults <1W, <=100mA,120s; alternate thresholds are visible. Strict near-zero-current evidence is still separate and additionally waits the configured stability interval. Neither battery-side power nor Android Full certifies adapter termination. Unsupported values, stale/gapped samples and a never-proven zero sensor cannot confirm low intake. UI/speech must distinguish percentage-only, estimated low intake and measured near-zero intake. Announcement rank only increases within a connection. Unplug percentage notice is independent of the optional early-removal reminder. Quiet-hour opt-in does not outrank calls, silent places or the master switch.

## News trust and cancellation

Preferences choose a bounded subset from the same validated source pool; empty selection is the existing mix. Category quotas are ceilings;13 selected categories rotate across the eight-story cap, not fabricated fillers. Tamil-only does not enter the English translation path. Configured AI can select existing evidence sentences; local code validates every index/sentence and translation before speech. FREE keyless provider is not used. Humour is spoken with a non-factual label; recruitment reporting is not vacancy/authenticity verification. Old preference fingerprints are rejected after preparation and at playback/clip boundaries. Stop still cancels playback/manual preparation. No private messages, GPS or phone reports are added to news prompts.

## Weather data flow and failure states

Three extra named places + existing GPS/fallback toggle, independent enable/edit/delete, lead15/30/60, bounded40-record per-place check/delivery history. Coordinates/history stay in MODE_PRIVATE storage and app backup is disabled. Enabled checks disclose provider coordinates/IP; search goes through the existing geocoder. Location replacement resets episode state; late deleted/disabled/moved-watch responses cannot recreate or deliver a watch. History records speech completion only after actual completion, not notification posting.

All forecasts are estimates, not a promise of arrival or raw radar. Hourly-only Tomorrow and MET Norway use distinct source labels. Missing precipitation, stale/undated MET Norway model issue (>12h) and stale/undated Open-Meteo current data fail rather than become dry conditions. Coarse trace-rain thresholds suppress noise, not prove rain absence. Android scheduling and network/permission/audio gates can delay or prevent delivery. Severe weather may bypass quiet hours, never calls. A GDACS nearby alert point is supplementary, not an IMD impact forecast.

Small zoom7 Tamil Nadu-region map is native Canvas, explicitly opened; no remote JavaScript or hidden prefetch. OSM viewed256px tiles use a stable app User-Agent, attribution and seven-day cache; bounded cache has no offline-download feature. Optional Tomorrow overlay uses the existing encrypted key in an HTTPS header, never a URL/log; no keyless production fallback. TLS/host/redirect/size/image-dimension checks bound network content. Incomplete tiles never mean dry. Provider issue time, entitlement, pixel-colour calibration and observed ground-radar coverage/local accuracy remain unverified. Official provider legend link, not invented pixel thresholds. No map pixels drive alerts. Weather overlays expire after15minutes; refresh is explicit, with up to nine tile calls. Cancellation/closing stops subsequent tile requests; an in-flight bounded HTTPS request can still finish.

Cyclones: bounded GDACS basin records with freshness checks, latest dated observed fix separately from forecast fixes, derived motion only from two credible observations. Dated longitude/latitude sketch connects published fixes only; it is not a map projection, official cone, landfall or impact prediction. Without dated fixes, the point is expressly a feed reference, not a current verified observation. IMD/RSMC link remains the regional authority. Missing/stale/unavailable is never an all-clear.

## Sources checked

- Android BatteryManager API / AOSP: documented counter units and sentinel/unsupported handling; battery intake is not adapter output.
- https://docs.tomorrow.io/reference/get-map-tile —256px tiles, ISO time, supported zoom; query colour-gradient description is insufficient to invent calibrated mm/h thresholds.
- https://docs.tomorrow.io/reference/data-layers-core —precipitationIntensity mm/hr, official spectrum; worldwide model availability is not TN observed-radar proof.
- https://support.tomorrow.io/hc/en-us/articles/31227543026708-How-to-Use-the-Tomorrow-io-API —keyless access is testing/evaluation, authenticated production access/plan conditions apply.
- https://open-meteo.com/en/docs —India subhourly estimates can be interpolated from hourly forecasts.
- https://operations.osmfoundation.org/policies/tiles/ —visible attribution, identifying native User-Agent, viewed tiles only, caching.
- https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP?eventtype=TC —mixed geometry; iscurrent alone is not adequate freshness evidence.
- IFRCGo/pystac-monty TC event/impact test fixtures (public GitHub) —dated actual/forecast advisory fixes; fixtures are schema evidence, not a current cyclone claim.
- https://rsmcnewdelhi.imd.gov.in/ —official cyclone advisories/track links, not automatically verified current advisories in this app.

## Verified candidate receipt

Targeted [run41378483444](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41378483444) **passed** for source`e58f6beb05b7408333d8943dfbe370bf51eab413`. Artifact`13112670646` contains the exact signed `CleanSweep-v2.83.apk`:14,585,575bytes, SHA-256`2be8a5f2f103323a907d1a83bbd91c112e3da7ac79a827ee79ca01855bf717a2`.

-302 Python tests; all567 required release regression tests across60 suites executed, none skipped; Kotlin compilation and release lint passed.
- Pinned signer and exact public2.82 baseline digest; full merged permissions unchanged (none added/removed).
- Exact signed normal update in disposable Android35 emulator preserved synthetic UID, data and preferences and survived restart.
- Native English settings smoke passed: default-topic restore, battery thresholds, lead-time setting, editing a place at the three-place limit, independent history and cyclone section presence.
- No physical-phone, new charging cycle, actual HD playback, authenticated Tomorrow map, live/current cyclone or local weather accuracy validation. No Play Protect claim or public publication.

[Machine-readable receipt](live-guard-2.83-candidate-verification.json). Download the Actions artifact within30days; extract only`CleanSweep-v2.83.apk` for an owner test, using Android's normal same-key update.

## Validation boundaries / still required

CI must compile, execute all release/JVM/Robolectric regressions, lint, verify signer + full merged permission continuity against exact public2.82, and exercise the exact signed same-key upgrade plus native settings. Source/localization checks alone are not a build. Synthetic emulator fixtures are not physical-phone charging, live weather accuracy, current cyclone verification or Play Protect clearance. Owner's new charging result and authenticated TN map test remain pending. No perfect-accuracy claim.
