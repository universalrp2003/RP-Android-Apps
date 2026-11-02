# Live Guard 2.87 — scoped authorized follow-up

Owner accepts private 2.86 and authorizes building/publishing Live Guard only. Clarifications: next three successful news bulletins per stable event; fresh permitted optional phone fix else explicitly saved home; reported wind/movement only with verified units/report time. No repeat accepted physical checks. Automated build/signing/permission/update/regression gates still required.

## Data/safety boundary

Only nonempty existing news reads call the bounded IO-only GDACS adapter. No worker/standalone speech, location upload, AI interpretation, new permission or dependence on weather API entitlement. Existing news, master, call, quiet/silent-place and HD-only controls apply. Separate default-off news-disaster opt-in; turning off immediately withdraws prepared news, including off/on cycles. All three report kinds use active, recent, bounded Point reports with finite coordinates. No empty-feed all-clear. Partial source failures omit missing types, not invented reports. Up to three regional events in a read; existing regional filters are TC 2000km, FL 500km, EQ 300km, not warning/impact radii.

Phone sample requires optional GPS enabled, permission, location service on, <=5min wall/monotonic age, finite accuracy <=1km and valid coordinates. Otherwise use saved home, explicitly labelled; no home means omit. Phone sample and source times are spoken. Draft <=10min and source staleness limits, home/GPS-mode change and consent generation are checked throughout preparation/playback without GPS/network/disk writes on main.

Observed cyclone position/wind only from matching storm, actual=true/current=true, latest <=18h dated GDACS timeline. Forecast rows and severitydata whole-track/page 'Current Max' (next72h) never supply current wind. Verified normalized wind_speed schema: maximum sustained wind in m/s, converted x3.6 to km/h. Units supported by IFRC schema and cross-checked with official GDACS dated bulletin table (28.292m/s = 102km/h rounded in public report). Unitless track_speed=0 is not verified movement: announce unavailable. No derived track velocity relabelled as reported movement.

Sources checked 2 November 2026 (public only; not stored raw/private):
- https://ifrcgo.org/monty-stac-extension/model/sources/GDACS/ — Tropical cyclone advisory timeline: wind_speed is maximum sustained wind in m/s; actual/current/date semantics.
- https://www.gdacs.org/Cyclones/report.aspx?eventtype=TC&eventid=1001346&system=JTWC — dated bulletin table vs overall maximum/next72h forecast 'Current' distinction.
- https://www.gdacs.org/gdacsapi/api/export/gettimeline?id=818121 — structured actual/current rows; movement field lacks a verified unit/missing-value contract.

Counters store only stable kind/id + 0..3 in private prefs, not locations or text. Reservations exclude concurrent manual/auto reads; full completion writes durably off-main before releasing reservations. IDs ignore episode/date/language/distance; no daily resets or expiry. Bounded 256-event memory: no tombstone eviction, new events fail closed at capacity. Corrupt memory fails closed. Process death immediately after audible completion but before durable receipt cannot be made atomic with a media player; exceptional storage failure/process kill may lose that last receipt. No absolute exactly-once claim.

## Verification

Verified CI41407403574/job125152452757 SUCCESS14m29s:669requiredJVM68suites, release compile/lint/sign/full permissions, normal2.82 preserved-data upgrade/native UI PASS. Exact artifact13124038781/source`52b525888631b12ba281e0df328d466013de8f7c` promoted without rebuild by41409032348; published2.87 release431191311/asset700956127. Prior2.86 results were not used as proof of the new feature. Added parser/speech and durable opt-in/counter/withdrawal/concurrency tests. Signed candidate workflow ran complete required suites, release lint/build, signer/full permission comparison and normal preserved-data 2.82 update/native UI smoke. No physical/audible new-feature or provider-accuracy proof claimed. Public2.87 now published; website/live Store catalog updated to the exact new APK. See publication receipt and release ledger.
