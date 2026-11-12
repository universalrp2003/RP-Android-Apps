# Live Guard 2.81 — interrupted news and repeated first stories

- Fix the between-clip media check: Live Guard's own recently released player must not be mistaken for another app's music. Screen/call/quiet/day/Stop gates remain active throughout playback.
- Prepare all HD clips before any playback, but checkpoint each fully completed story durably at its final clip. If a later story fails, the completed prefix is not selected again. A prepared or interrupted story is never marked read.
- Successful bulletins retain completion-relative timing. An audible but incomplete attempt also waits the selected 30/60/120/180-minute interval before another automatic read; it does not pretend the bulletin succeeded. Silent preparation failures still have the ten-minute recovery check.
- Reopening Settings preserves a pending retry instead of pulling it forward repeatedly. Explicit interval changes still reschedule. Diagnostics distinguish completed stories, last full completion and last audio activity from the prepared selection.
- Owner reports these supplied bytes were installed on a phone, tested and accepted. Owner tested the supplied 2.81 update, reported it working perfectly and authorized publication on31 October2026. Screenshots show a completed bulletin at14:22 and the next check at14:52 with30m selected. Installed-phone hash was not independently measured. CI/emulator checks are not universal OEM/all-interval/unplugged proof. Earlier2.79/2.80 assets remain unchanged.
- In-place update intended for the current owner-key install. Do not uninstall, clear data or bypass a signing/security warning; uninstalling may erase local data. No new runtime-dangerous permissions. Optional precise-alarm access from the earlier candidate remains separate from audio/network guarantees.

## Exact release artifact

Source08c339448a3bc2a286b1340c217eb256c6abdac8; signed run41181427941/artifact13035561625; no rebuild.14,500,707 bytes, SHA-256 `c851a09f20a0799a49789047d970f2ca512d71a53d16699cd19c36ef57517805`. Owner-authorized same-key **update**; uninstalling may erase local data. This is not Google Play Protect clearance.

## தமிழ்

- அடுத்த ஒலித் துண்டுக்கு முன் செயலியின் சொந்த முந்தைய ஒலியை வேறு செயலியின் இசையாகத் தவறாகக் கருதும் சோதனை திருத்தப்பட்டுள்ளது. திரை, அழைப்பு, அமைதி நேரம், நாள் மாற்றம் மற்றும் நிறுத்து கட்டுப்பாடுகள் தொடரும்.
- அனைத்து உயர்தர ஒலித் துண்டுகளும் முதலில் தயாரிக்கப்படும். ஒவ்வொரு செய்தியும் முழுமையாக வாசித்த பிறகே தனியாகச் சேமிக்கப்படும். பின்னர் வேறொரு செய்தி தோல்வியடைந்தாலும் முன்பு முடித்த செய்தி மீண்டும் தேர்ந்தெடுக்கப்படாது. தயாரித்த செய்தியையோ பாதியில் நிறுத்திய செய்தியையோ வாசித்ததாகக் குறிக்காது.
- முழுத் தொகுப்பு முடிந்த நேரத்திலிருந்து இடைவெளி தொடரும். ஒலி வந்தபின் வாசிப்பு இடைநிறுத்தப்பட்டாலும் அடுத்த தானியங்கி வாசிப்புக்குத் தேர்ந்தெடுத்த இடைவெளி பின்பற்றப்படும்; முழுத் தொகுப்பும் முடிந்ததாகக் கூறாது. ஒலியே வராத தயாரிப்புத் தோல்விக்கு பத்து நிமிட மீட்புச் சோதனை தொடரும்.
- அமைப்புகளை மீண்டும் திறப்பதால் காத்திருக்கும் சோதனை நேரம் தொடர்ந்து முன்னே கொண்டுவரப்படாது. பயனர் இடைவெளியை மாற்றினால் அட்டவணை மாறும். முழுமையாக வாசித்த செய்திகள், கடைசி முழுத் தொகுப்பு மற்றும் கடைசி ஒலி நேரம் தனித்தனியாகக் காட்டப்படும்.
- உரிமையாளர் இந்தப் புதுப்பிப்பை கைபேசியில் சோதித்து நன்றாகச் செயல்படுவதாக உறுதிப்படுத்தி வெளியிட அனுமதித்துள்ளார். நிறுவப்பட்ட கோப்பின் ஹாஷ் தனியாகச் சரிபார்க்கப்படவில்லை. தானியங்கிச் சோதனையோ குறுகிய நேர முன்மாதிரி விழிப்புச் சோதனையோ உண்மையான கைபேசி ஒலி மற்றும் மின்னேற்றமற்ற சோதனைக்குச் சமம் அல்ல. முந்தைய வெளியீடும் சோதனைப் பதிப்பும் மாற்றப்படவில்லை.
- தற்போதைய உரிமையாளர் கையொப்ப நிறுவலைத் தரவை அழிக்காமல் புதுப்பிக்க வேண்டும். செயலியை நீக்கினால் உள்ளூர் தரவு அழியலாம். கையொப்ப அல்லது பாதுகாப்பு எச்சரிக்கையைத் தவிர்க்க வேண்டாம். புதிய ஆபத்தான இயக்கநேர அனுமதிகள் இல்லை; துல்லிய அலார அனுமதி இணையமோ ஒலியோ வெற்றி பெறும் உத்தரவாதம் அல்ல.
