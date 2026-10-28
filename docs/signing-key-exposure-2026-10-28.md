# Legacy signing-key exposure — confirmed, 28 October 2026

## Finding and scope

The repository is public. Its tracked main CI signing PKCS12 container contains a private-key bag, and a tracked workflow contains the unlock reference. A local, non-exporting audit verified that the container authenticates with that reference and that its certificate matches the current main/Store fingerprint `9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55`. No private key was exported, printed, copied, replaced or added to Git during this investigation. This document intentionally contains no password or extraction instructions.

Treat this identity as exposed, including after deleting the current file or changing a repository password: copies and history remain. Certificate matching alone cannot establish first-party provenance when the corresponding private signing material is public. This is independent of the unresolved Play Protect installation verdicts; it does **not** prove their cause, malicious code, or a false positive. Successful tests and owner-device acceptance do not repair key custody.

This confirmed finding concerns the main CI identity used by the legacy apps, Store, DailyVault and Voice. Secret Message uses the separate `58e370…bc04f08` identity and its own password secret; that is not evidence of general security clearance, nor a claim that this main-key audit examined its private-key custody.

## Immediate safeguards implemented

- Main-identity publication/mirroring workflows now stop at an explicit known-exposed-identity gate. Build-only/unit/lint/merged-APK checks remain available. The exact old Voice1.3.2 promotion is held too, even if the GitHub403 permission issue is later addressed.
- The hold is tied to the known fingerprint, not whether a keystore still exists at HEAD. Removing the file alone cannot silently clear the hold.
- Store routing requires the expected package/version/code/certificate **and exact size/SHA-256 of the already-public canonical Store0.5 APK** (`16912504` bytes, `d5f41e7b5142507e8b42fb9d2c4e0e2d492eaf03581a53b3dabacf692daf4bc7`). This metadata was rechecked against the canonical public release API. Unknown/debug/modified/future/unreadable Store APKs fall back to the official app-specific website instead of relying on certificate identity alone.
- The local read is only on an explicit update action; it downloads nothing and adds no network/install permission. A future legitimate Store APK requires a separately reviewed published-byte allowlist update. This deliberately conservative mitigation is not a complete platform-signing remediation or security certification of Store0.5.
- Existing published assets, installed apps, signatures, private data, native crypto and keystores remain unchanged. No warning bypass, bulk withdrawal, forced reinstall, repackaging or silent signing-key replacement.

## Required signing-continuity work before new public APKs

1. Establish owner-controlled private production key custody outside the public tree and public workflow literals. Do not generate or transmit production private keys/passwords in chat, and do not commit new private key material.
2. Review a per-package, data-preserving update/rotation strategy, actual APK signing lineage and capability flags. Android9+ rotation support does not by itself prove a safe migration from an exposed key. Android8/8.1 require separate compatibility treatment; do not promise that blind re-signing preserves in-place updates or data on every supported version.
3. Test recovery/backup, installed-signature continuity, app locks and protected data on the supported platforms before publication. Secret's separate identity and chat/crypto protocol must not be swept into a blanket key change.
4. Update actual verified signing fingerprints, Store verification policy and release guards together only after review. Never simply remove the publication guard or treat a different, unreviewed fingerprint as proof of private custody.
5. Build a distinct version/code, verify actual signed bytes and platform behavior, then publish without replacing any existing same-label asset. The old owner-accepted Voice1.3.2 bytes remain historical acceptance evidence, not a newly private-signed release.

GitHub release creation also returned HTTP403 (runs40673435557/40673761435). Resolving connection permissions does not resolve this signing hold. SMS1.1.10's harmful-install warning and the external voice-service failures remain open independently.

## தமிழ்

பொதுவான களஞ்சியத்தில் தற்போதைய பொதுக் கையொப்பத்தின் தனிச்சாவிக் கோப்பும் அதைத் திறக்கும் குறிப்பும் இருப்பது உறுதியாகியுள்ளது. தனிச்சாவியை வெளியே எடுக்கவோ காட்டவோ மாற்றவோ இல்லை. கையொப்பம் பொருந்துவது மட்டும் இனி முதல் தரப்பு மூலத்தை உறுதிப்படுத்தாது. இது பாதுகாப்பு எச்சரிக்கையின் காரணம் என்பதற்கான சான்று அல்ல.

புதிய பொதுச் செயலிக் கோப்பு வெளியீடுகள் நிறுத்தப்பட்டுள்ளன. நிறுவிய செயலிகள் மற்றும் தரவுகள் மாற்றப்படவில்லை. தெரிந்த பொதுவெளி அங்காடிக் கோப்பின் கணிதச் சுருக்கமும் பொருந்தினால் மட்டுமே அதற்கு இணைப்பு திறக்கும்; இல்லையெனில் அதிகாரப்பூர்வ இணையப் பக்கம் திறக்கும். பாதுகாப்பு எச்சரிக்கையைத் தவிர்க்கவோ செயலிகளை நீக்கவோ வேண்டாம். பழைய கைபேசிகளிலும் தரவு இழப்பில்லாமல் தொடரக்கூடிய கையொப்ப மாற்றத் திட்டத்தைச் சரிபார்த்த பின்னரே புதிய வெளியீடு செய்ய வேண்டும்.
