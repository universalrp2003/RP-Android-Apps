# Oor Arivippu 1.1.17/code22 — unpublished UI/trip candidate

Owner approved implementation, not publication. Compact Tamil controls and family cards; repeated safety caveats moved into family details. No change to consent, pairing, sharing switches, expiry or transport. Recent local announcement history moved intact from settings to home; these are announcement records, not a continuous route log.

Progress uses existing accepted travel callbacks only. Coordinates/speed/fix age are volatile session state, not persisted or uploaded. Require reported position accuracy ≤65m; require valid reported speed with speed uncertainty ≤2m/s. No guessed speed from displacement. Use monotonic fix time; hide readings after30s, on GPS off or trip stopped. Trip start resets readings. Rough minutes use straight-line distance/current speed, minimum3km/h. Never road/traffic ETA. UI timer updates age only while resumed and does not acquire location.

No manifest/polling/API changes. No screenshot originals or family/travel details added. Public1.1.16 and site/catalog pointers stay unchanged. Normal same-key update only; no reset/uninstall. New three JVM policy tests and one Python source contract;427 Python checks pass locally. Kotlin compile/lint, actual emulator rendering/font scaling and physical travel accuracy are separate gates, not proven by source checks. Candidate build pending CI; no release authorized.

## CI result

Source2dd46a7: signed release verification41987517269 **SUCCESS** (unit tests, release lint, signed assembly, package/version/signer verification). Signed candidate artifact13396967013 `oorarivippu-stable-candidate-679`, archive SHA-25672bdacb7034e5d66bc82212949a163cf54d41377c65e90cbd436223dbcc50683. Debug test build41987517564 also **SUCCESS** (unit tests/lint/assembly). These are build proofs, not actual emulator UI/font-scale, same-key installed-byte upgrade or physical travel/ETA proofs. Those runtime gates remain pending; do not describe the candidate as phone-tested or promote it yet. Public1.1.16 remains unchanged.
