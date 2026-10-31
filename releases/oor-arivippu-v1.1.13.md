# Oor Arivippu 1.1.13 / code18 — investigation candidate, NOT published

## English

These new candidate bytes are not installed on a phone. Build/test/signing results will be recorded separately; no physical-device acceptance or publication is claimed.

- Keep district and town roles separate in journey geocoding and destination search. Never promote subAdminArea or an explicitly district-labelled locality/feature into a town.
- Say “Pudukkottai district” / “புதுக்கோட்டை மாவட்டம்” explicitly when the geocoder supplies that district; retain district, town and landmark labels together in announcement history. Do not invent a district when unavailable or claim a verified boundary crossing from reverse-geocoder output alone.
- Preserve legitimate circular-route returns, existing movement/recent-place controls, journey settings and history. Reject invalid destination coordinate ranges and retain district labels even when a capital shares the name.
- About uses the current shared published-app directory and removes the retired InternetRadio repository link, not the active Ramesh Radio app.
- Phone route/speech acceptance of these new bytes is still required. CI and SPOKEN callbacks are not independent microphone evidence.
- Keep package and established signing identity for an in-place update. Do not uninstall or clear app storage: uninstalling may erase local data and journey history. No fresh-install or signing-key migration is authorized.

## தமிழ்

இது 1.1.13 / code18 சோதனை வேட்பாளர் மட்டுமே; பொதுவெளியில் வெளியிடப்படவில்லை.

- ஊர், மாவட்டம் தனித்தனியாகக் கையாளப்படும். மாவட்டத்தை ஊராக அறிவிக்காது; மாவட்டம் எனக் குறிக்கப்பட்ட முகவரியும் ஊராக மாறாது.
- முகவரித் தகவலில் கிடைக்கும்போது “புதுக்கோட்டை மாவட்டம்” என்று தெளிவாகச் சொல்லும். ஊர், மாவட்டம், முக்கிய இடம் ஆகியவை வரலாற்றில் சேர்ந்து இருக்கும். இல்லாத மாவட்டத்தை உருவாக்காது; மாவட்ட எல்லை உண்மையாகக் கடந்ததாக உறுதி கூறாது.
- வட்டப் பயணத்தில் உண்மையான மறுவருகையைத் தடுக்காது. பயண அமைப்புகளும் வரலாறும் பாதுகாக்கப்படும்; தவறான அட்ச/தீர்க்க ரேகைகள் ஏற்கப்படாது.
- பழைய InternetRadio இணைப்பு நீக்கப்பட்டது; தற்போதைய Ramesh Radio செயலி பட்டியலில் இருக்கும்.
- புதிய APK-இன் உண்மைப் பயணச் சோதனை இன்னும் தேவை. அதே கையொப்பத்துடன் புதுப்பிக்கவும்; நிறுவல் நீக்கம்/தரவு அழித்தல் வேண்டாம். உள்ளூர் தரவு அழியலாம்.
