# Live Guard 2.97 — manage your rain-alert home

- Remove saved home with confirmation. Its cached forecast, episode and notification are cleared; other saved places, GPS-follow choice, voice settings, lead time and your personal weather key are kept. Choose a home again to resume the home-based weather checks.
- Save current GPS place as home, even when no home is saved. This is a user-started one-shot lookup, not automatic tracking. Location permission and a fresh, named GPS fix are required. Review the locality before confirming replacement. A denied permission, disabled Location, expired fix or failed lookup never copies the old home as current GPS.
- Cancel leaves home unchanged. Following GPS remains a separate opt-in. Tamil and English controls wrap at large text sizes.

No new permissions, provider endpoint, background poller, automatic IMD import or change to voice eligibility. Rain data remains a forecast, not a local measurement or official warning. Existing app features and website decluttering are retained.

Update over same-key public 2.96 without uninstalling or clearing local data. Android decides installation. Stop if Play Protect warns; do not bypass it. Validation covers signed-byte identity, full permission continuity, preserved-state upgrade/restart, required JVM and Android 35 native regressions. The exact candidate is not installed on a phone independently. Synthetic test GPS is not proof of physical-device location accuracy, audible delivery, all-OEM reliability or Google clearance.

## தமிழ்

சேமித்த வழக்கமான இடத்தை உறுதிப்படுத்திய பிறகு அகற்றலாம். அந்த இடத்தின் பழைய முன்னறிவிப்பும் அறிவிப்பும் நீக்கப்படும்; மற்ற இடங்கள், GPS பின்தொடர்தல், குரல் அமைப்புகள், முன் அறிவிப்பு நேரம், தனிப்பட்ட விசை பாதுகாக்கப்படும். மீண்டும் வழக்கமான இடத்தைத் தேர்வு செய்து கண்காணிப்பைத் தொடரலாம்.

தற்போதைய GPS இடத்தை வழக்கமான இடமாகச் சேமிக்கலாம்; பழைய வழக்கமான இடம் இல்லாவிட்டாலும் பயன்படுத்தலாம். இருப்பிட அனுமதியும் புதிய GPS ஊர் விவரமும் தேவை. காட்டப்படும் ஊரைச் சரிபார்த்து உறுதிப்படுத்தவும். ரத்து செய்தால் சேமித்த இடம் மாறாது. பழைய இடம் GPS எனச் சேமிக்கப்படாது; தானியங்கிப் பின்தொடர்தல் இயக்கப்படாது.

புதிய அனுமதி அல்லது கூடுதல் பின்னணி கண்காணிப்பு இல்லை. அதே கையொப்ப 2.96 செயலியை நீக்காமல் புதுப்பிக்கவும். பாதுகாப்பு எச்சரிக்கை வந்தால் நிறுத்துங்கள்; அதை மீற வேண்டாம். செயற்கை GPS சோதனை உண்மைக் கைபேசி இருப்பிடத் துல்லியத்திற்கோ Google அனுமதிக்கோ சான்றல்ல.
