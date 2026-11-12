# Oor Arivippu 1.1.14 / code 19 — configured test candidate

**Not a public release. No physical-device test.** Secret Message, Live Guard and public APK/catalog versions are unchanged.

[Verified test download (GitHub artifact ZIP)](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41630998303/artifacts/13228173881) — may require GitHub sign-in; expires 3 December 2026. Extract `Oor-Arivippu-v1.1.14-family-candidate.apk`. No installation bypass, uninstall or data clear. Install only as a same-key update; stop if Android reports a signature mismatch. Other unconfigured or failed-build artifacts are not this candidate.

- APK SHA-256: `59fb5d218f11bebe39fa15d0e907d8cf7a43157928742c7d9ccf4c70d6ec1329`; 12,875,568 bytes.
- Stable signer/full merged permissions match accepted public Oor 1.1.10. No added permissions.
- CI41630998303/job125900229419 passed: 19 backend policy tests, real deployed HTTPS flow, 359 repository Python checks, 17 family JVM tests, release lint/build, actual Android35 encrypted-keystore and two-logical-phone tests, default OFF/independent consent/stop/revocation, bilingual consent UI, same-UID update/selected settings/private file/restart, and exact installed APK hash.
- Separate service provisioning41625797930 passed unused-worker preflight and real HTTPS smoke; no existing worker/subscription/Secret Message changed. Deployment authorization is not public APK release authorization.
- Native tests use two isolated logical phone stores on **one Android emulator**, not two physical phones, actual GPS, audible speech or all OEM/background behavior. No independent security audit, emergency guarantee, Play Protect clearance or unlimited free-hosting promise.

## Use on two consenting phones

1. Open **Family · optional town sharing**. Give the other person a local private label and create/share a private invitation. The invitation expires after ten minutes; send only to the intended person over a trusted channel. An expired/incomplete invitation can be removed and recreated.
2. The recipient pastes the invitation and labels the inviter locally. Compare the displayed connection code on a trusted call or in person; **both** confirm. Matching a code and connecting do not start sharing.
3. Each person independently starts their normal Oor trip tracking and chooses **1, 4 or 8 hours** of family town sharing. Viewing somebody else's report does not require starting a trip or sharing your own town.
4. Use **Stop all my family sharing**, individual Stop, or Remove connection. Local OFF is saved first. Offline/network-limited remote removal can be delayed; an older report can remain until its observation-based fifteen-minute expiry.

Only town/district/time are end-to-end encrypted for this service; no coordinates, subarea, address, PIN, speed, contacts or route history. Names stay locally encrypted. Updates are normally attempted at most every five minutes during the explicitly started trip; GPS/geocoding/internet/Android restrictions can delay or prevent updates. Absence of a fresh report is **not** evidence that somebody is safe or stationary. Existing Android/Play-services positioning and platform geocoding remain; the Google Maps app/API is not used by this feature.

See the JSON verification receipt for exact provenance. Public Oor remains 1.1.10; no release/site promotion has been performed.
