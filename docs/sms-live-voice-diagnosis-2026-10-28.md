# SMS voice: matched-runner profile differential — 28 October 2026

## New direct evidence

Monitoring run [40680202248](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40680202248), source463faf5, attempted **all four required profiles** with the app's actual Kotlin client, then independently used pinned upstream `edge-tts7.3.1` on the same CI runner. Requests used synthetic test text, not owner SMS, credentials or OTPs. The reference wheel's published PyPI digest was checked during source inspection; no upstream code was patched.

| Profile | Actual app Kotlin result | Independent upstream result |
| --- | --- | --- |
| Tamil male `ta-IN-ValluvarNeural` | **FAIL:** HTTP101 then peer1011,0audio bytes, including bounded retry | **FAIL:** `NoAudioReceived`,0bytes |
| Tamil female `ta-IN-PallaviNeural` | **PASS:**25920bytes; clean completion | **PASS:**25776bytes |
| English male `en-IN-PrabhatNeural` | **PASS:**30672bytes; clean completion | **PASS:**23328bytes |
| English female `en-IN-NeerjaNeural` | **PASS:**24624bytes; clean completion | **PASS:**24624bytes |

The service's voice-list metadata still advertises **both** Tamil India profiles: Pallavi(Female) and Valluvar(Male). Being advertised is not evidence that synthesis succeeds. Some profile fixtures differ between clients; this is a functional availability differential, not an expected byte-for-byte audio comparison. The first Valluvar Tamil fixture is the same. Audio bytes are service responses, not a physical-device playback/accent/quality assessment.

Earlier paired run40679527830 gave the same upstream distinction, but the older app probe stopped at the first failed profile. The updated app probe records each required result and **still fails if any one fails**. DNS failure now explicitly fails the dedicated live task instead of returning a false successful no-audio test. Ordinary deterministic tests continue to exclude this real external-service method.

## Interpretation and limits

- The evidence narrows the repeated CI failure to the **Valluvar synthesis path in this environment/time**. It is not evidence that the whole app voice engine is broken, that every provider voice is unavailable, or that the owner's working runtime is failing.
- Successful audio from three other profiles through the actual app client also argues against a blanket Kotlin transport failure. It does not prove the exact internal provider failure or worldwide availability; clients made sequential requests and may use different provider backend routes.
- Valluvar remains an unresolved external/profile-specific availability issue. Do not relabel the failing required-voice job green, silently retire a still-advertised voice, replace the user's selected voice, introduce robotic TTS, guess new endpoints or disable DNS/security protections.
- Production4200ms deadline, HD-only policy, sender-only OTP safeguards, working voice preferences and owner-accepted Voice Notes playback/new-note code are unchanged by this diagnostic work.
- No owner profile is inferred from the report that runtime works. Existing controls can select other HD profiles; no automatic preference change or physical-device acceptance of another profile is claimed here.
- This does not explain or clear Play Protect's harmful-install verdict. Main-key publication is separately held for [confirmed signing-key exposure](signing-key-exposure-2026-10-28.md); GitHub release creation also returned403. No new SMS/Voice public asset was published.

## தமிழ்

அதே சோதனைக் கணினியில் செயலியின் உண்மையான குரல் இயந்திரத்தையும் தனியான ஒப்பீட்டு நிரலையும் பயன்படுத்தினோம். தமிழ் ஆண் வள்ளுவர் குரலில் இரண்டிலும் ஒலிக் கோப்பு கிடைக்கவில்லை. தமிழ் பெண் குரலும் இரண்டு ஆங்கிலக் குரல்களும் செயலியின் இயந்திரத்திலேயே முழுமையான ஒலித் தரவை வழங்கின. சேவையின் பட்டியலில் ஆண் குரல் இருந்தாலும் ஒலி உருவாக்கம் வெற்றி பெறவில்லை.

எனவே செயலியின் எல்லாக் குரல்களும் செயலிழந்தன என்றோ உரிமையாளரின் இயங்கும் குரல் பாதிக்கப்பட்டது என்றோ கூற முடியாது. இந்தக் குரலுக்கான சேவைப் பாதையின் தோல்வி இன்னும் தீரவில்லை. எந்தத் தோல்வியையும் வெற்றியாகக் காட்டவில்லை; தேர்ந்தெடுத்த குரலையோ உயர்தரக் குரல் கொள்கையையோ மாற்றவில்லை. நிறுவல் பாதுகாப்பு எச்சரிக்கைக்கும் கையொப்பத் தனிச்சாவிப் பிரச்சினைக்கும் தனித்தனி ஆய்வு தேவை.
