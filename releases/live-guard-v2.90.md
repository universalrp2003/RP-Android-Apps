# Live Guard 2.90 — shorter disaster announcements

Private follow-up candidate; public2.89 remains unchanged.

- Say the reference locality once for the entire disaster-news add-on, without its PIN code or district/state address tail. Distinguish saved reference from enabled fresh phone location without reading a second GPS-check timestamp.
- Speak only available verified wind, gust, movement and explicitly labelled landfall forecast details. Do not read lists of missing data; missing values remain unknown, never fabricated zero or all-clear. Unsupported Tamil landfall translations are omitted from speech rather than announced as unavailable.
- Keep one dated observation/report timestamp and source, plus a short regional-not-local-warning qualification. Do not repeat the provider-update/issue timestamp. Full stored place labels, coordinates, source-issued versus observation fields and on-screen details are not changed or deleted.
- Preserve once-per-new-cyclone advisory rules, flood/earthquake counters, saved OFF/language/topics/GPS, HD/call/quiet protections, compact shared notification and restored three-segment D/U meter.

795 required JVM declarations/74suites and4 Android35 tests require a fresh CI run. No physical-device test, audible pronunciation or live-provider accuracy proof for2.90 yet. Normal same-key update only; do not uninstall or clear local data. Uninstalling may erase local data. No public2.90 publication authorized or performed by this implementation step.

## தமிழ்

பேரிடர் செய்தி அறிவிப்பில் ஊர்ப் பெயர் ஒருமுறை மட்டும்; அஞ்சல் குறியீடும் மாவட்டம் மற்றும் மாநில விவரங்களும் வாசிக்கப்படாது. உறுதிப்படுத்திய கிடைக்கும் தகவல்கள் மட்டுமே சுருக்கமாக வாசிக்கப்படும். கிடைக்காத தகவல்களின் பட்டியல் சொல்லப்படாது; தெரியாத மதிப்பு பூஜ்யமாகவோ பாதுகாப்பு உறுதியாகவோ மாற்றப்படாது. அறிக்கையின் நேரமும் ஆதாரமும் சுருக்கமான பிராந்திய விளக்கமும் தொடரும். சேமித்த இடம், இருப்பிட எண்கள், மொழி, அணைத்த தேர்வுகள், செய்தி விதிகள், குரல் பாதுகாப்பு, மூன்று ஒளிப் பகுதிகள் மாறாது. இன்னும் தனிப்பட்ட சோதனைப் பதிப்பு; பொது வெளியீடு அல்ல. புதிய கைப்பேசி மற்றும் ஒலிச் சோதனைச் சான்று இல்லை. நீக்கவோ தரவை அழிக்கவோ வேண்டாம்.
