# SMS Guard 1.1.10 — release candidate

This exact version has no physical-device test and is not yet publicly released. Owner previously confirmed Finance authentication and incoming voice on an earlier 1.1.9 candidate; that is not exact-byte acceptance of this rebuilt version. Preserve SMS history and settings; do not uninstall to update.

- Finance lock disabling and protected PIN changes require authentication; ViewModel guards remain in place.
- Sender-only/OTP safeguards preserved. No OTP code speech added.
- Deterministic release regression tests are separate from the real external synthesis assertion. Live monitoring currently fails; do not describe external availability as fixed or blame NextDNS without evidence.
- Added optional content-free transport diagnostic categories and proper peer WebSocket closing; text/tokens/URLs/headers are not logged by these diagnostics. Runtime speech timeout unchanged.
- Version code 16; pinned signer/package/hash/non-debuggable checks remain mandatory. Distinct public publication, device validation and Store-first suite migration remain pending.

## தமிழ்

இந்த 1.1.10 பதிப்பின் அதே APK இன்னும் கைபேசியில் சோதிக்கப்படவில்லை; பொதுவெளியில் வெளியிடப்படவில்லை. முந்தைய 1.1.9 சோதனையில் நிதிப் பூட்டும் புதிய குறுஞ்செய்திக் குரலும் இயங்கியதாக பயனர் தெரிவித்தார்; அது இந்த APK-க்கான சான்று அல்ல. செய்திகள் மற்றும் அமைப்புகளைப் பாதுகாக்க செயலியை நீக்க வேண்டாம்.

நிதிப் பூட்டை அணைப்பதற்கும் பாதுகாக்கப்பட்ட PIN மாற்றங்களுக்கும் அங்கீகாரம் தேவை. OTP எண்களைப் பேசாத பாதுகாப்பு தொடர்கிறது. நிலையான சோதனைகளும் இணையக் குரல் சேவையின் நேரடிச் சோதனையும் தனித்தனியாக உள்ளன. நேரடி இணையச் சோதனை இன்னும் தோல்வியடைகிறது; சேவை சரியாகிவிட்டதாகக் கூறவில்லை. குரல் உரை அல்லது ரகசிய விவரங்களைப் பதிவு செய்யாமல் இணைப்புத் தோல்வியின் வகையை மட்டும் கண்டறிய வசதி சேர்க்கப்பட்டுள்ளது. செயலியில் குரல் காத்திருப்பு நேரம் மாற்றப்படவில்லை. கைபேசி சோதனையும் வெளியீட்டுச் சரிபார்ப்புகளும் இன்னும் தேவை.
