# Live Guard 2.85 — private candidate only

**Not published.** Normal same-signing-key update, version code 90. Do not uninstall or reset: uninstalling may erase local data. Public Live Guard remains 2.82 until separately approved. Other accepted apps are unchanged.

- Moves the whole manual-news path and CPU-heavy bulletin selection off main. Audio-start checkpoints are scheduled off main; UI story status no longer takes the durable-history monitor. Completed stories still use durable checkpoints, never marking unfinished stories read.
- Moves player cancellation off the UI thread and snapshots the old player, rather than letting a late Stop affect a replacement.
- Network-change speech waits briefly for the same validated default route before uncached synthesis. Superseded transport announcements cancel. A later successful request no longer leaves a warning based on an older automatic error; the historical diagnostic remains available. HD only, no phone-TTS fallback.
- Compact actual-provider viewer: GDACS, IMD, Tomorrow.io tabs, Expand/Compact, Back/current-page browser action, page-zoom selection. Live tab instances preserve website-owned map state during tab switches/Expand. Page zoom is stored per provider; persistent geographic map zoom/deep links across closing/reopening are **not** promised. Use each website's own map controls and place selector. No API key/GPS injection or app-drawn tracks.
- Preserves simple 100%-reached battery defaults, opt-in Advanced settings, home/GPS/saved places, strict fresh Tomorrow.io minute-data policy and silent regional disaster reports.

## Verification boundary

Source work and regression additions are not proof that the reported phone ANR or live HD/network behavior is fixed. No ANR trace has been supplied. CI compilation/JVM/lint/signer/permission/normal-upgrade/native UI evidence must be recorded separately; physical-phone Read news, Stop and Wi-Fi/mobile recovery remain acceptance checks. No public publication or Play Protect clearance is implied.

## தமிழ்

இது தனிப்பட்ட சோதனைப் பதிப்பு மட்டும்; பொதுவில் வெளியிடப்படவில்லை. அதே கையொப்பத்துடன் சாதாரண புதுப்பிப்பு செய்ய வேண்டும். செயலியை நீக்க வேண்டாம்; நீக்கினால் உள்ளூர் தரவு அழியலாம்.

செய்தித் தேர்வு மற்றும் குரலை நிறுத்தும் கனமான பணிகள் திரை இயக்கத்திலிருந்து பிரிக்கப்பட்டுள்ளன. புதிய குரல் முயற்சி வெற்றி பெற்றபின் பழைய தானியங்கி தோல்வியை தற்போதைய HD எச்சரிக்கையாகக் காட்டாது. இணையம் மாறும்போது நிலையான சரிபார்க்கப்பட்ட இணைப்புக்குச் சிறிது நேரம் காத்திருக்கும்; HD குரல் மட்டுமே தொடரும்.

சிறிய இணையச் சாளரத்தில் GDACS, IMD, Tomorrow.io தாவல்கள், பெரிதாக்கு/சிறிதாக்கு மற்றும் பக்க அளவு உள்ளன. தாவல்கள் மாறும்போது திறந்த தளத்தின் வரைபட நிலை தொடரும். வரைபட இடம்/அளவை அந்தத் தளத்திலேயே தேர்ந்தெடுக்கவும்; சாளரத்தை மூடியபின் புவியியல் வரைபட அளவு சேமிக்கப்படும் என உறுதி இல்லை. உண்மைக் கைபேசியில் செய்தி, நிறுத்தல், இணைய மாற்றம் இன்னும் சோதிக்கப்பட வேண்டும்.
