# Voice Notes 1.3.1 — playback-control candidate

This exact version has no physical-device test and is not publicly released. Owner installed the earlier 1.3/code4 candidate and reported working update check, but could not stop playback or switch notes. That report is not exact-byte acceptance of this rebuilt candidate. Keep installed notes; do not uninstall to update.

- Active Play now becomes Stop, including while synthesis is loading. Tap it again to cancel; tap another note to stop the old request and start the new one.
- Cancelled/late requests cannot clear a newer note's playback state or start stale audio. Playback is serialized; temporary clip files are unique and cleaned up.
- Coroutine cancellation interrupts blocking synthesis/playback waits; deletion of an active note and disposal of the screen stop its request.
- HD neural-only playback, privacy consent, optional biometric gate, bilingual UI and daily opt-in in-app update checks remain. No robotic fallback added. External speech service availability is not guaranteed.
- Version code5. Deterministic cancellation tests and signed APK/package/signer/hash/release-lint gates must pass. Physical MediaPlayer/device control validation remains pending.

## தமிழ்

இந்த 1.3.1 பதிப்பின் அதே APK இன்னும் கைபேசியில் சோதிக்கப்படவில்லை; பொதுவெளியில் வெளியிடப்படவில்லை. முந்தைய 1.3 சோதனையில் புதுப்பிப்புச் சரிபார்ப்பு இயங்கியது; ஆனால் வாசிப்பை நிறுத்தவோ வேறு குறிப்பை வாசிக்கவோ முடியவில்லை என்று பயனர் தெரிவித்தார். அது இந்த APK-க்கான சான்று அல்ல. குறிப்புகளைப் பாதுகாக்க செயலியை நீக்க வேண்டாம்.

வாசிப்பு தொடங்கியதும் Play பொத்தான் Stop ஆக மாறும். குரல் தயாராகும்போதும் அதே பொத்தானால் நிறுத்தலாம். வேறு குறிப்பைத் தட்டினால் பழைய வாசிப்பு நிறுத்தப்பட்டுப் புதியது தொடங்கும். தாமதமாக வரும் பழைய பதில் புதிய வாசிப்பைக் கலைக்காது. ஒரே நேரத்தில் இரண்டு வாசிப்புகள் இயங்காது. வாசிக்கப்படும் குறிப்பை நீக்கினாலும் திரையை விட்டு வெளியேறினாலும் அந்த வாசிப்பு நிறுத்தப்படும். HD குரல், தனியுரிமை ஒப்புதல், விருப்பமான சாதனப் பூட்டு மற்றும் புதுப்பிப்பு வசதிகள் தொடர்கின்றன; இயந்திரக் குரல் மாற்று சேர்க்கப்படவில்லை. கைபேசியில் வாசிப்பு நிறுத்தம் மற்றும் மாற்றத்தைச் சரிபார்க்க வேண்டும்.
