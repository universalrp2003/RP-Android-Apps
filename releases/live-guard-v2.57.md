# Live Guard — v2.57

## English

### 30-Minute Rain Head-Start & 24×7 Cyclone / Heavy Rain Voice Alert

- **Helpful only, zero daily spam:** Live Guard stays completely silent on normal sunny, cloudy, or breezy days — never sending daily routine weather notifications.
- **30-minute rain heads-up (Quiet Hours respected):** When rain is expected within ~30 minutes, Live Guard announces once per rain episode in Tamil or English and stays silent during your configured **Quiet Hours** (sleeping time). Ongoing continuous rain does not repeat announcements until dry weather returns for at least 90 minutes.
- **Very heavy rain & cyclone safety override (24×7, no skip):** If forecast intensity escalates to **very heavy rain**, intense squall gusts, or an **active cyclone advisory**, Live Guard warns immediately at **any hour day or night** without skipping Quiet Hours (while still respecting **CallGuard** so live phone calls are never interrupted).
- **Smart automatic GPS + Oor Arivippu-style PIN / place picker:**
  - When phone **GPS is ON**, Live Guard detects your location automatically with minimal battery use.
  - When phone **GPS is OFF**, it automatically falls back to your saved town/village — searchable by 6-digit PIN code (`622301`), place name, or both (`alangudi 622301`) with a full district + state + PIN selection list so towns sharing the same name never get mixed up.
- **Built-in free weather engine + optional 1-minute Tomorrow.io key:** Works immediately out of the box with zero API key required (Open-Meteo `minutely_15` + official GDACS cyclone advisories), plus an optional Android-Keystore-encrypted personal **Tomorrow.io** API key slot for 1-minute rain radar precision.
- **No new Android permissions:** Uses only permissions already declared by Live Guard (`INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`).

---

## தமிழ்

### 30 நிமிட மழை முன்னறிவிப்பு & 24×7 கனமழை / புயல் குரல் எச்சரிக்கை

- **பயனுள்ள நேரத்தில் மட்டும் — தினசரி தொந்தரவு இல்லை:** சாதாரண வெயில்/மேகமூட்ட நாட்களில் எந்த அறிவிப்பும் குரல் தொந்தரவும் இருக்காது.
- **30 நிமிட மழை முன்னறிவிப்பு (அமைதி நேர மதிப்புடன்):** மழை தொடங்க சுமார் 30 நிமிடத்திற்கு முன் ஒருமுறை மட்டும் தமிழ்/ஆங்கிலத்தில் குரலில் அறிவிக்கும். தூங்கும் **அமைதி நேரத்தில் (Quiet Hours)** சாதாரண மழை அறிவிப்பு ஒலிக்காது; தொடர்ந்து மழை பெய்தாலும் மீண்டும் மீண்டும் தொந்தரவு செய்யாது.
- **மிகக் கனமழை & புயல் அவசர எச்சரிக்கை (இரவு-பகல் 24×7):** மிகக் கனமழை, பலத்த சூறாவளிக் காற்று அல்லது புயல் எச்சரிக்கை ஏற்பட்டால் அமைதி நேரத்திலும் **தவிர்க்காமல் உடனடியாக** குரலில் எச்சரிக்கும் (தொலைபேசி அழைப்பில் இருக்கும்போது மட்டும் CallGuard காத்திருக்கும்).
- **புத்திசாலி இடத் தேர்வு (GPS தானியங்கி + ஊர் அறிவிப்பு வகை PIN/ஊர் தேடல்):**
  - போன் **GPS இயக்கத்தில்** இருந்தால் இடம் தானாகவே கண்டறியப்படும்.
  - **GPS அணைந்திருந்தால்**, நீங்கள் சேமித்த ஊரைத் தானாகப் பயன்படுத்தும் — `622301` அல்லது `alangudi 622301` போன்று PIN எண் / ஊர்ப் பெயரைத் தேடி, மாவட்டம் + மாநிலம் + PIN விவரத்துடன் உங்கள் சரியான ஊரைத் தேர்ந்தெடுக்கலாம்.
- **இலவச உள்ளமைந்த ரேடார் + விருப்ப Tomorrow.io 1-நிமிட விசை:** எந்த விசையும் இன்றி உடனடியாக வேலை செய்யும்; கூடுதலாக 1-நிமிட துல்லியத்திற்கு தனிப்பட்ட Tomorrow.io API விசையை (Android Keystore மறையாக்கத்துடன்) சேர்க்கலாம்.
- **புதிய Android அனுமதி எதுவும் சேர்க்கப்படவில்லை.**

---

## Verification and device testing

The exact APK was **not installed on a phone before publication**. Build, unit/regression (46 suites / 413 tests), localization, APK and release-asset checks are recorded by the publishing workflow. Passing CI is not a Play Protect approval or an all-device compatibility claim.

Version 2.57 / code 62.
