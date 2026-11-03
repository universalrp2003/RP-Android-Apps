# Oor About/Guide correction — source only

The owner correctly identified that the built-in About/Guide had been missed. Publishing website text and masked screenshots did not change the installed 1.1.15 APK.

## Corrections
- Replace the second hardcoded, stale seven-release list with `SisterApps` verified public metadata. Link Oor's latest public release to its live website app section (not a build-time predecessor tag), separately; exclude self from Other Apps. Preserve installed-version lookup, credits, official repositories and the clearly labelled legacy mirror.
- Add English/Tamil optional-family summary and a direct link to the live bilingual web guide and reviewed masked screenshots.
- Update only Oor's shared built-in guide: mutual confirmation/default OFF, six connections including pending, saved per-person ON/OFF, trip-only upload/pause, old timed-grant migration OFF, freshness/expiry, receiver window, private device-only speech and audio limits, provider boundaries and no emergency/all-clear guarantee.
- Replace misleading unconditional 24×7/free/minute-precise weather guidance with actual active-host, saved-OFF and provider/coverage/quota/Android limitations. No weather runtime change.
- Add guards against stale hardcoded Oor tags and missing built-in family guidance, plus four Kotlin guide/catalog tests. The unrelated isolated Secret Message source/guard is not changed.

## Delivery boundary
Published **1.1.15/code20** remains the exact accepted configured APK, **12,891,952 bytes**, SHA-256 `ec625cbfb6faa1bab680b71e4184d945913908662e56f898e695dea932c1abe5`.

These edits do **not** update a phone, authorize another publication, rebuild the existing release, bump a version, alter the relay or change family consent/location/speech behavior. A later APK must have a new version/code, the existing verified relay configuration, same-key upgrade and native UI verification, and its own exact-byte promotion authorization. Generic unconfigured CI outputs are not deliverables.

The dedicated workflow compiles Oor, runs its actual unit tests and release lint, uploads reports only and never assembles/publishes an APK. Source checks alone are not native layout, phone, audio or installed-version proof.
