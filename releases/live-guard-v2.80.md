# Live Guard 2.80 — candidate, not published

## English

- News uses a persisted wake-up deadline for the selected 30-minute / 1-hour / 2-hour / 3-hour interval instead of depending solely on periodic WorkManager ticks or an Activity-owned screen-off coroutine. Only completed playback advances the interval and consumes headlines.
- A delayed/failed/blocked automatic read keeps unread news and schedules a bounded retry. Foreground execution is visible while preparing/reading, with a time-limited wake lock; nothing is held between checks.
- Android 12+ precise wake-ups require **Alarms & reminders** special access. The app explains this and offers an explicit settings button, never silently grants it. Without it, inexact alarms and persisted work are best-effort. Battery restrictions, Doze network access, OEM rules and force-stop can still prevent delivery. A wake-up check is not a promise that remote news/HD preparation and playback start at that exact instant.
- Settings distinguish the last actual check/result and next check from the prepared category selection. No fresh eligible news, screen-on, calls, other media, 10 PM–6 AM, user quiet hours and silent places remain silent.
- Network validation changes resume pending HD requests. Brief Wi-Fi/mobile handoffs get a bounded grace period; fully prepared current-day news remains local audio. Unrelated old failures no longer immediately trigger an HD-unavailable notice on a new connection.
- Full up-to-eight source-backed stories, selected HD voice, prepare-all-before-playback, cancellable Stop, freshness and completed-playback-only deduplication are preserved. No old stories/tips are invented to fill unavailable categories.

**Installation:** candidate only, no release asset yet. No physical-device test; not installed on a phone. Same package and pinned owner-backed signer as 2.79; compatible-key installs should use Android's normal update flow without uninstall/clear-data. Old legacy-signer installations cannot update across the historical key migration. Back up/export before considering any migration: uninstalling may erase local data. Do not bypass Android protection warnings. No independent physical-phone timing, installed-hash, Play Protect or OEM-delivery claim.

## தமிழ்

- தேர்ந்தெடுத்த 30 நிமிடம் / 1 மணி / 2 மணி / 3 மணி இடைவெளிக்குச் செய்திச் சரிபார்ப்பு நேரம் சேமிக்கப்படும். செயலித் திரை திறந்திருக்க வேண்டியதில்லை. முழுமையாகப் படித்து முடித்தால் மட்டுமே இடைவெளியும் படித்த செய்திகளும் புதுப்பிக்கப்படும்.
- முடியாத அல்லது தடுக்கப்பட்ட வாசிப்பு படித்ததாகக் குறிக்கப்படாமல் மீண்டும் முயற்சிக்கப்படும். தயாரிப்பு/வாசிப்பின்போது மட்டும் பின்னணி அறிவிப்பும் நேர வரம்புள்ள விழிப்பு பூட்டும் இருக்கும்.
- ஆண்ட்ராய்டு 12 முதல் துல்லியமான விழிப்பிற்கு **அலாரங்கள் மற்றும் நினைவூட்டல்கள்** சிறப்பு அனுமதி தேவை. அமைப்புகளைத் திறக்கும் பொத்தான் உள்ளது; தானாக அனுமதி வழங்கப்படாது. அனுமதி இல்லாமல் தாமதம் ஏற்படலாம். மின்கலக் கட்டுப்பாடுகள், உறக்கநிலை இணையக் கட்டுப்பாடு, தயாரிப்பாளர் விதிகள் மற்றும் கட்டாய நிறுத்தம் செயல்பாட்டைத் தடுக்கலாம். திட்டமிட்ட சரிபார்ப்பு நேரமே அந்நேரத்தில் உடனடியாக ஒலி தொடங்கும் உறுதி அல்ல.
- கடைசியாகச் சரிபார்த்த நேரம்/முடிவும் அடுத்த சரிபார்ப்பு நேரமும், தயாரித்த செய்தித் தேர்விலிருந்து தனியாகக் காட்டப்படும். புதிய தகுதியான செய்தி இல்லை, திரை இயக்கம், அழைப்பு, மற்ற ஒலி, இரவு 10 முதல் காலை 6 வரை, அமைதி நேரம்/இடம் ஆகியவற்றில் ஒலிக்காது.
- வைஃபை/மொபைல் இணைய மாற்றத்திற்கு குறுகிய அவகாசம் உள்ளது. ஏற்கனவே முழுமையாகத் தயாரித்த இன்றைய செய்தி ஒலி இணையம் இல்லாமலும் தொடரும். பழைய தோல்விகள் புதிய இணைப்பில் உடனடியாக HD எச்சரிக்கையை ஏற்படுத்தாது.
- எட்டு வரை முழுச் செய்திகள், தேர்ந்தெடுத்த HD குரல், அனைத்தையும் தயாரித்த பின் வாசித்தல், நிறுத்துதல், இன்றைய செய்திகள் மற்றும் முழுமையாக வாசித்த பின் மட்டும் படித்ததாகக் குறித்தல் பாதுகாக்கப்பட்டுள்ளன.

**நிறுவல்:** இது இன்னும் வெளியிடப்படாத சோதனைப் பதிப்பு. 2.79-இன் அதே தொகுப்பு மற்றும் உரிமையாளரின் தற்போதைய கையொப்பம் பயன்படுத்தப்படும். பொருந்தும் கையொப்பமுள்ள செயலியை தரவு நீக்காமல் புதுப்பிக்க வேண்டும். பழைய கையொப்பப் பதிப்பிற்கு நேரடிப் புதுப்பிப்பு முடியாது; எந்த மாற்றத்திற்கு முன்பும் காப்பெடுக்கவும். நிறுவல் நீக்கினால் உள்ளூர்த் தரவு அழியலாம். ஆண்ட்ராய்டு பாதுகாப்பு எச்சரிக்கையை மீற வேண்டாம். உண்மைக் கைபேசி நேரச் சோதனை அல்லது Play Protect ஒப்புதல் உறுதி கூறப்படவில்லை.
