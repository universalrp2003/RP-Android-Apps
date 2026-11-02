# Live Guard2.89 — clear free weather and new cyclone advisories

Private candidate, not yet published. Public2.88 and all sibling APKs remain unchanged.

- Free weather without a personal API key is normal, not an error: explain the working fallback in neutral text. Label values as forecast estimates rather than local rain measurements. A missing Tomorrow.io key does not mean all weather is unavailable.
- Precise15/30/60-minute rain alerts still require fresh authorized Tomorrow.io minute data. A key alone does not prove entitlement; general forecast remains available when providers respond. Denied/quota/network failures stay labelled; no false precision or all-clear.
- Owner chose **once per genuinely new cyclone advisory**, in the next successful nonempty news bulletin, not three repetitions per cyclone. Only a newer actual/current JTWC advisory timestamp can unlock another announcement. Refresh time, feed metadata, distance, home/language or changed wind alone do not reset it. Skip older cached advisories; newest pending details supersede older ones. No standalone/background disaster alarm or new polling schedule.
- Legacy versions did not record which cyclone advisory was spoken. The latest valid advisory may therefore be heard once after this policy upgrade, even if that cyclone was mentioned earlier. Existing legacy counters are retained, not reset. Flood/earthquake three-successful-bulletin rules remain unchanged.
- All existing disaster OFF choices, master/news/HD/call/quiet/silent-place/location gates and topic/language/home/battery choices remain. Stop/failed playback does not mark an advisory delivered. Bounded durable per-storm high-water memory survives ordinary updates/restarts; process death between completed audio and a successful disk write can still cause a repeat. No absolute exactly-once claim.

Automated verification pending. No physical-device test of2.89; no new audible/provider-accuracy/Play Protect claim. Normal same-key update; do not uninstall or clear local data. Uninstalling may erase local data.

## தமிழ்

API விசை இல்லாமலும் கிடைக்கும் இலவச வானிலை பிழை அல்ல என்று தெளிவாகக் காட்டும் மாற்றம். மதிப்புகள் முன்னறிவிப்பு; உங்கள் இடத்தில் அளந்த மழை அல்ல. துல்லியமான மழை நேர அறிவிப்புகளுக்குப் புதிய அனுமதித்த நிமிடத் தரவு தேவை. புதிய சரிபார்த்த புயல் அறிக்கை அடுத்த வெற்றிகரமான செய்தி வாசிப்பில் ஒருமுறை மட்டுமே; அதே அல்லது பழைய அறிக்கையை மீண்டும் சொல்லாது. புதிய காத்திருக்கும் அறிக்கை பழைய விவரங்களை மாற்றும். வெள்ளம்/நிலநடுக்கத்திற்கான மூன்று வாசிப்பு விதி தொடரும். முந்தைய பதிப்பு எந்தப் புயல் அறிக்கையை வாசித்தது என்பதைச் சேமிக்காததால், புதுப்பித்தபின் தற்போதைய அறிக்கை ஒருமுறை வரலாம். ஏற்கெனவே அணைத்த தேர்வுகளும் குரல் பாதுகாப்பு அமைப்புகளும் மாறாது. இன்னும் தனிப்பட்ட சோதனைப் பதிப்பு; பொது வெளியீடு அல்ல. நீக்கவோ தரவை அழிக்கவோ வேண்டாம்.
