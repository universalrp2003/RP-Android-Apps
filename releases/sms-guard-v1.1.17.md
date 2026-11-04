# SMS Guard 1.1.17 — steadier online voice recovery

- Gives the preferred online voice more time inside the same bounded deadline: three-voice shares are now 55/30/15 instead of equal thirds.
- Keeps distinct-voice fallback, explicit online consent and clean, complete audio requirements. No same-voice retry or longer total deadline.
- Adds bounded upstream timing/failure diagnostics for synthetic CI probes. Online service availability is not guaranteed.

Normal same-key update from 1.1.16: do not uninstall or clear app data. Existing messages, default-SMS role and preferences should remain. No new permissions. SMS-only default mode remains explicit; MMS is not implemented.

This release is not a physical-phone, carrier/MMS, audible-playback or Google Play Protect certification. Historical legacy-key installations cannot update across signing identities; do not erase data as a workaround.

## தமிழ்

இணையக் குரலின் முதல் தேர்வுக்கு ஒரே காலவரம்புக்குள் அதிக நேரம் வழங்கப்படுகிறது; மாற்றுக் குரல்கள் தொடரும். இணையச் சேவை எப்போதும் வெற்றி பெறும் என்ற உறுதி இல்லை.

1.1.16 இலிருந்து சாதாரண புதுப்பிப்பு செய்யவும். செயலியை நீக்கவோ உள்ளூர் தரவு அழியும்படி அழிக்கவோ வேண்டாம். குறுஞ்செய்திகள், அமைப்புகள் மற்றும் இயல்புநிலை SMS தேர்வு பாதுகாக்கப்பட வேண்டும். புதிய அனுமதிகள் இல்லை; MMS ஆதரவு இல்லை. கைப்பேசி ஒலி அல்லது Google ஒப்புதல் சான்று அல்ல.
