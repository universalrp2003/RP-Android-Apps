# Oor1.1.19 — final foreground-only GPS release

The owner explicitly authorized necessary fixes, the optional30-second GPS check, verification and publication without another phone test. No private screenshots, family names or owner locations are published.

## What changed

Retains1.1.18 conservative locality selection, family locality-only privacy, bounded geocoder/speech cleanup, compact bilingual UI/dynamic app count, shared truthful speed, original-age last-known distance and fresh-moving-speed-only ETA. Exit10 means user-requested app termination; it is not a whole-device reboot diagnosis.

Adds direct lifecycle observers to stop GPS and unregister passive GNSS on pause/stop/destroy without waiting for Compose recomposition or another frame. A sample cannot start when the lifecycle is not resumed. It still needs an explicit click, existing precise permission and enabled GPS, and stops at30seconds or on stop/close/disposal. Android31+ also enforces the request duration. No diagnostic storage/upload, new permission prompts, trip/family/destination writes or silent continuous GPS.

## Exact verification and publication

Final source **10f7d339b5451020bfb5fcb9fbba352ea38bdec1**, candidate **42169463061 SUCCESS**, job127733605804, artifact13480129182. Release JVM/unit/lint/assembly, established signer/full permissions, exact public1.1.18→1.1.19 install-over, original UID/preferences/private file/encrypted family ciphertext/decryption/restart and opt-in consent passed. Four actual native tests passed: bilingual100%/200% home/history/diagnostics/About/Guide controls; actual30-second GPS timeout; idempotent stop/restart cleanup; synchronous lifecycle pause/destroy stop and refusal to start while paused without any Compose frame. The same exact installed APK hash is verified before and inside instrumentation.

All441 Python tests and localization passed at final candidate/publisher source. Earlier42164750724 failed a baseline-version assertion;42165376484,42166420708,42167518339 and42168538626 failed retained UI navigation assertions. These are not passing evidence. Native scrolling now uses the actual exposed scroll owner, label-centred real touch, stable viewport handling and both-direction search; no history, bilingual/font, About/Guide, preservation or exact-byte assertions were removed. All four tests passed together in the final run, not pieced together from failed runs.

Publisher **42170377449 SUCCESS** promoted the tested APK unchanged. **1.1.19 / code24**,12,924,720B, SHA-256 `f00fefa61f8b0c3282746749a32cf9c17d2b9a62b38477a783573b6de9b3bb02`. Release436142996 / asset716068902, published2026-11-06T05:46:46Z. Stable signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`. No rebuild or overwrite; all prior public release/asset IDs/sizes/digests unchanged, including Oor1.1.18 and Store0.10.

Checked-in candidate/pins/publication JSON receipts contain the exact source/run/artifact/hash identities. Candidate publication_authorized=false is historical verification-time state; publication is separately authorized and recorded by the publisher receipt.

## Installation and boundaries

Install the final1.1.19 APK over the existing same-key app, including an earlier1.1.18 candidate. Do not uninstall, clear data, downgrade or bypass a warning. Keep Play Protect enabled.

No independently hash-matched physical-phone test, real satellite acquisition, real-road/arrival/audio proof, reboot cause/cure, all-OEM battery guarantee or Google clearance is claimed. Counts are receiver observations, not proof that particular satellites produced a fused distance. Provider locality can still be coarse, wrong or suppressed; stationary/walking drift is not proof of vehicle detection.

The website keeps all previously live13 verified APK/ZIP pairs and adds final1.1.19 as pair14. Intermediate1.1.18 remains unchanged on GitHub; its proposed CDN pair was never deployed. Both compatible12/13-app feeds advance only Oor; Store's immutable bundled fallback remains untouched. Mirror budget remains795,815,998B of800,000,000B.
