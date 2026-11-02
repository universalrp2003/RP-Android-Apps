# Live Guard2.89 — clear free weather and new cyclone advisories

Private candidate, not yet published. Public2.88 and all sibling APKs remain unchanged.

- Free weather without a personal API key is normal, not an error: explain the working fallback in neutral text. Label values as forecast estimates rather than local rain measurements. A missing Tomorrow.io key does not mean all weather is unavailable.
- Precise15/30/60-minute rain alerts still require fresh authorized Tomorrow.io minute data. A key alone does not prove entitlement; general forecast remains available when providers respond. Denied/quota/network failures stay labelled; no false precision or all-clear.
- Owner chose **once per genuinely new cyclone advisory**, in the next successful nonempty news bulletin, not three repetitions per cyclone. Only a newer actual/current JTWC advisory timestamp can unlock another announcement. Refresh time, feed metadata, distance, home/language or changed wind alone do not reset it. Skip older cached advisories; newest pending details supersede older ones. No standalone/background disaster alarm or new polling schedule.
- Legacy versions did not record which cyclone advisory was spoken. The latest valid advisory may therefore be heard once after this policy upgrade, even if that cyclone was mentioned earlier. Existing legacy counters are retained, not reset. Flood/earthquake three-successful-bulletin rules remain unchanged.
- All existing disaster OFF choices, master/news/HD/call/quiet/silent-place/location gates and topic/language/home/battery choices remain. Stop/failed playback does not mark an advisory delivered. Bounded durable per-storm high-water memory survives ordinary updates/restarts; process death between completed audio and a successful disk write can still cause a repeat. No absolute exactly-once claim.

Automated verification pending. No physical-device test of2.89; no new audible/provider-accuracy/Play Protect claim. Normal same-key update; do not uninstall or clear local data. Uninstalling may erase local data.

## Additional held-build improvements

- Speak the exact named reference town/district, not merely “home”; confirm a saved place before rain/disaster checks without moving existing coordinates. Fresh named GPS is separate and optional. Clear cyclone/event names, dated observation versus GDACS update timestamps, sustained wind and GDACS/JTWC attribution.
- Movement/landfall remain explicitly unavailable until a reliable, validated source contract exists. No invented speed, landfall, live safety prediction or Windy trial-data use. Official IMD links remain available.
- Security → App identity details shows Android-reported signer hashes, signing history, package and reported installer. Opted-in local history distinguishes signing rotation from an unexplained signer difference and records comparable installer changes; not a malware/safety verdict. Powerful-access combinations receive reasons/manual review. Familiar labels/package prefixes are not authenticated trust.
- A one-time Enable / Not now introduction explains optional local security history; no new permission, background service, APK upload or automatic removal. Existing opt-in/OFF choices are preserved. APK/link review stays deferred.
- Extended verification pending; earlier2.89 test results do not cover these additions. Publication remains on hold.

## தமிழ்

API விசை இல்லாமலும் கிடைக்கும் இலவச வானிலை பிழை அல்ல என்று தெளிவாகக் காட்டும் மாற்றம். மதிப்புகள் முன்னறிவிப்பு; உங்கள் இடத்தில் அளந்த மழை அல்ல. துல்லியமான மழை நேர அறிவிப்புகளுக்குப் புதிய அனுமதித்த நிமிடத் தரவு தேவை. புதிய சரிபார்த்த புயல் அறிக்கை அடுத்த வெற்றிகரமான செய்தி வாசிப்பில் ஒருமுறை மட்டுமே; அதே அல்லது பழைய அறிக்கையை மீண்டும் சொல்லாது. புதிய காத்திருக்கும் அறிக்கை பழைய விவரங்களை மாற்றும். வெள்ளம்/நிலநடுக்கத்திற்கான மூன்று வாசிப்பு விதி தொடரும். முந்தைய பதிப்பு எந்தப் புயல் அறிக்கையை வாசித்தது என்பதைச் சேமிக்காததால், புதுப்பித்தபின் தற்போதைய அறிக்கை ஒருமுறை வரலாம். ஏற்கெனவே அணைத்த தேர்வுகளும் குரல் பாதுகாப்பு அமைப்புகளும் மாறாது. இன்னும் தனிப்பட்ட சோதனைப் பதிப்பு; பொது வெளியீடு அல்ல. நீக்கவோ தரவை அழிக்கவோ வேண்டாம்.
