# SMS Guard 1.1.12 — bill reminder safeguards

- A transaction declined “due to” an invalid card, failed payment, refund or reversal must not become a bill. Automatic reminders now require a date directly attached to a payment deadline; unrelated transaction, statement and expiry dates are not borrowed. Genuine outstanding bills with a deadline remain eligible even after a failed payment.
- Previously stored transaction-status false reminders remain available for review, with a warning instead of an overdue badge. Their bill voice/notifications are suppressed in daily, immediate and manual bill reads. Nothing is silently deleted or marked paid; manual reminders and the original SMS are preserved.
- Bill source labels have a full-width row separate from the scrollable actions. About no longer shows the legacy repository link or a frozen seven-app count.
- Candidate only; not published. The exact candidate APK is not installed on a phone. Unit tests, lint, signing and APK compatibility checks are separate CI evidence, not phone acceptance or Play Protect clearance.
- This is intended as an in-place update from owner-key 1.1.11, preserving data and preferences. Old-key installs cannot be updated across signing identities; do not uninstall or clear data as a troubleshooting step. No new permissions are requested.

Uninstalling can erase local data; it is not required to upgrade the current owner-key version. Stop on a signature mismatch rather than resetting data.

## தமிழ்

- அட்டை செல்லாததால் நிராகரிக்கப்பட்ட பரிவர்த்தனை, தோல்வியடைந்த கட்டணம் அல்லது திருப்பிய பணம் பில் நினைவூட்டலாக மாறாது. பணம் செலுத்தும் காலக்கெடுவுடன் நேரடியாக இணைந்த தேதியே ஏற்கப்படும். பரிவர்த்தனைத் தேதியோ அறிக்கைத் தேதியோ காலக்கெடுவாக எடுத்துக்கொள்ளப்படாது. உண்மையான நிலுவைக் கட்டணமும் அதன் காலக்கெடுவும் இருந்தால் நினைவூட்டல் தொடரும்.
- முன்பு தவறாகச் சேமித்த பரிவர்த்தனை நினைவூட்டல்கள் சரிபார்ப்புக்காக இருக்கும். தாமத எச்சரிக்கைக்குப் பதிலாக விளக்கம் காட்டப்படும்; பில் குரலும் அறிவிப்பும் நிறுத்தப்படும். எந்தப் பதிவும் தானாக நீக்கப்படாது; செலுத்தியதாகவும் மாற்றப்படாது. கைமுறை நினைவூட்டல்களும் அசல் குறுஞ்செய்தியும் பாதுகாக்கப்படும்.
- பில் மூல விவரம் தனி முழு அகல வரியில் காட்டப்படும். செயலி விவரத்திலுள்ள பழைய களஞ்சிய இணைப்பும் ஏழு செயலிகள் என்ற பழைய எண்ணிக்கையும் நீக்கப்பட்டுள்ளன.
- இது சோதனைப் பதிப்பு; வெளியிடப்படவில்லை. இந்தப் புதிய கோப்பு தொலைபேசியில் நிறுவிச் சோதிக்கப்படவில்லை. தானியங்கிச் சோதனை கைபேசி ஏற்புச் சோதனையோ பாதுகாப்பு ஒப்புதலோ அல்ல.
- தற்போதைய உரிமையாளர் கையொப்பப் பதிப்பிலிருந்து தரவும் விருப்பங்களும் மாறாமல் புதுப்பிக்க வேண்டும். பழைய வேறு கையொப்ப நிறுவலுக்கு நேரடிப் புதுப்பிப்பு சாத்தியமில்லை. சிக்கலைச் சரிசெய்ய செயலியை நீக்கவோ தரவை அழிக்கவோ வேண்டாம். புதிய அனுமதிகள் இல்லை.
