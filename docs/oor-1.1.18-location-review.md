# Oor1.1.18 — historical GPS/locality release

Superseded by1.1.19, which adds synchronous lifecycle cleanup without relying on Compose frames. The1.1.18 APK/receipts remain immutable; use the latest1.1.19 download.

Private owner screenshots are reference-only; no new private screenshots, names or places are published. The owner explicitly authorized necessary fixes, a manual30-second GPS check, verification and publication, without requiring another phone test.

## Final changes

- Conservative locality selection rejects administrative/district fallbacks; family reports remain locality-only, not finer local subarea disclosure. Providers can still mislabel or omit a locality; no ground-truth boundary guarantee.
- Optional **Check GPS now ·30seconds** works without a trip or vehicle. It requires already-granted precise permission and enabled GPS, never requests permission, and stops on explicit stop, close, background, permission loss or disposal. Android31+ additionally enforces a30-second request duration. No diagnostic persistence/upload. Passive callback is visible/resumed-only; stale/not-reported counts are not zero and do not prove fused-fix attribution.
- Shared truthful speed text; stale last-known straight-line distance has its original fix age; ETA requires fresh reliable moving speed. Invalid fixes retain the original last-good coordinates/time without manufacturing freshness. Walking/drift distance is not proof of vehicle detection.
- Language-keyed UI status, shorter app title, compact spacing/insets, dynamic About app count, and clear incoming-family versus outgoing-sharing wording. Proper place/person names are not artificially translated.
- Bounded geocoder, bounded speech queue/executor shutdown and destination-quality gate retained. Exit10 is user-requested app termination, not proof of a crash or whole-phone reboot. No reboot-cure claim.

## Exact final evidence

Candidate **42163141442**, source `02e98aad51e4f72ee8e44c622243d47c61934abd`, job127713690474, artifact13477479675: **SUCCESS**. Release unit/JVM/lint/assembly, signer and complete permission equality, exact public1.1.17→1.1.18 install-over, UID/private/preferences/original encrypted family ciphertext, decryption/restart/consent and exact installed bytes passed. English/Tamil home/history/diagnostics controls passed at100% and200% font. Three actual native tests include real Android GPS request deadline and idempotent cleanup; UI starts/stops the sample through dialog controls. Background/disposal binding has source regression guards; physical satellite reception is not tested.

Candidate source435 Python tests passed; publisher source436 passed with localization, signer and relay policy/HTTP gates. Additional current-directory regressions run independently after publication. Historical candidate42159402800 / SHA `cf280091200f33442a5b81cd94e6146e64cd0247de7c5d3fee8d4536754865e1` predates these final fixes and is **not** the published APK. Earlier failed/cancelled UI runs remain failed/cancelled evidence.

Publisher **42164167011 SUCCESS** promoted tested bytes unchanged: release436107627, APK asset715932949, published2026-11-06T04:43:46Z. Final APK **12,924,720B**, SHA-256 `e224ed2a9db1494c480c14e616821b24fbc73bd2ec48fea05390f401281ef0da`. Stable signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`. All prior release IDs/asset IDs/sizes/digests checked unchanged, including Store0.10. No rebuild or overwrite.

See `oor-1.1.18-candidate-verification.json`, `oor-location-1.1.18-publication-pins.json` and `oor-location-1.1.18-publication-receipt.json`. The candidate receipt deliberately retains publication_authorized=false at verification time; the separately authorized publisher receipt records publication.

## Limits and installation

Final bytes are not independently hash-matched on a physical phone; no physical GNSS/road/arrival/audio, reboot cause/cure or Play Protect clearance. Keep Play Protect enabled. Install this final file over the same-key app, even if an earlier candidate already says1.1.18; no uninstall, data clear, downgrade or warning bypass.
