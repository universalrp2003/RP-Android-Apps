# Live Guard 2.77 — prepare the full news bulletin before speaking

**Version:** 2.77 · **versionCode:** 82 · **Package:** unchanged.

This follow-up repairs the news-voice failure still seen after 2.76. Live Guard now prepares **every** HD audio part of the bulletin before the first sound, then plays the prepared local files back-to-back. A slow synthesis retry can no longer leave the owner with two headlines, silence, and a stuck red Stop chip. The red Stop chip is shown only while audio is actually playing; preparation shows “Preparing news audio…”. Stop cancels the whole manual read, including preparation.

The news path no longer sends headlines through the assistant AI provider for a rewrite. The cleaned eight-headline bulletin is spoken directly, so a configured Groq/other helper cannot shorten or drop items. News synthesis gets a bounded patient budget per part and per HD voice, while short alerts keep their existing fast timeout. Failed or stopped reads still do not mark headlines as announced.

No new Android permissions are declared. The exact 2.77 candidate APK was not installed on a phone; CI compilation and regression tests are not a device test, Play Protect clearance, or all-device certification.

**Install warning:** Version 2.77 uses the same replacement signing identity as 2.76, 2.75 and 2.74, so it can update those installations normally without clearing app data. Android cannot update installations signed with the exposed pre-2.74 key. If you are on an older version, download this official APK first, export or back up anything important, then uninstall the old app and install this APK through Android. Uninstalling may erase local data. Do not uninstall before obtaining the replacement APK. Stop if Play Protect warns; do not bypass it.

## தமிழ்

**பதிப்பு:** 2.77 · **versionCode:** 82 · **செயலி அடையாளம்:** மாற்றமில்லை.

2.76-க்குப் பிறகும் செய்தி குரல் நின்றுபோன சிக்கலை இந்தத் தொடர் திருத்துகிறது. லைவ் கார்ட் இப்போது முதல் ஒலி எழுவதற்கு முன்பே செய்தித் தொகுப்பின் **எல்லா** HD ஒலிப் பகுதிகளையும் தயாரித்து, பின்னர் தயாரான உள்ளூர் கோப்புகளை ஒன்றன்பின் ஒன்றாக இயக்குகிறது. மெதுவான மறுமுயற்சி இனி இரண்டு தலைப்புகள், அமைதி, சிக்கிய சிவப்பு Stop பொத்தான் என்ற நிலையை ஏற்படுத்தாது. ஒலி உண்மையில் இயங்கும்போது மட்டுமே சிவப்பு Stop chip காட்டப்படும்; தயாரிப்பு நிலையில் “செய்தி ஒலி தயாராகிறது…” காட்டப்படும். Stop அழுத்தினால் தயாரிப்பு உட்பட முழு கைமுறை வாசிப்பும் ரத்து செய்யப்படும்.

செய்தி பாதை இனி தலைப்புகளை உதவி AI வழங்குநரிடம் மறுஎழுத அனுப்பாது. சுத்திகரிக்கப்பட்ட எட்டு தலைப்புத் தொகுப்பு நேரடியாகப் படிக்கப்படும்; Groq/பிற உதவி அமைப்பு செய்திகளைச் சுருக்கவோ விடவோ முடியாது. செய்தி தொகுப்புக்கு ஒவ்வொரு பகுதிக்கும் ஒவ்வொரு HD குரலுக்கும் கட்டுப்படுத்தப்பட்ட பொறுமையான நேரம் வழங்கப்படுகிறது; சிறிய அறிவிப்புகளின் வேகமான நேரம் மாறவில்லை. தோல்வியடைந்த அல்லது நிறுத்தப்பட்ட வாசிப்பு செய்திகளை படித்ததாகக் குறிக்காது.

புதிய Android அனுமதிகள் எதுவும் சேர்க்கப்படவில்லை. இந்த 2.77 வேட்பாளர் APK கைபேசியில் நிறுவிச் சோதிக்கப்படவில்லை; CI தொகுப்பும் மீள்தன்மை சோதனைகளும் கைபேசி சோதனையோ, Play Protect ஒப்புதலோ, எல்லா சாதனங்களுக்குமான சான்றிதழோ அல்ல.

**நிறுவும் எச்சரிக்கை:** பதிப்பு 2.77, பதிப்பு 2.76, 2.75 மற்றும் 2.74-இன் அதே மாற்றுக் கையொப்ப அடையாளத்தைப் பயன்படுத்துகிறது; ஆகவே செயலி தரவை அழிக்காமல் அவற்றில் நேரடியாகப் புதுப்பிக்கலாம். 2.74-க்கு முந்தைய வெளிப்பட்ட பழைய கையொப்பத்தால் நிறுவிய செயலியை Android நேரடியாகப் புதுப்பிக்க அனுமதிக்காது. பழைய பதிப்பில் இருந்தால், முதலில் இந்த அதிகாரப்பூர்வ APK-ஐப் பதிவிறக்கி, முக்கியத் தகவல்களை ஏற்றுமதி அல்லது காப்புப்பிரதி செய்து, பிறகு பழைய செயலியை நீக்கி Android வழியாக இதை நிறுவவும். நீக்கினால் உள்ளூர் தரவு அழியலாம். புதிய APK கிடைக்கும் முன் நீக்க வேண்டாம். Play Protect எச்சரித்தால் நிறுத்துங்கள்; அதைத் தவிர்க்க வேண்டாம்.
