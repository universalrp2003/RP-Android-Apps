# Live Guard 2.62 (`versionCode = 67`) — 10 Studio Ultra-HD Online Voices + Daily Auto Male ↔ Female Rotation (Turned ON by Default)

## English Summary

Requested after real-world physical phone verification of **SMS Guard 1.1.0**, **Live Guard 2.62** brings **10 Studio Ultra-HD Online Neural Voices (`5 Male HD + 5 Female HD`)** with **Automatic Daily Male ↔ Female Voice Rotation turned ON by default** across every spoken Live Guard feature — with **zero new Android permissions**:

1. **10 Studio Ultra-HD Neural Voices (`5 Male HD + 5 Female HD`, Tamil & English):**
   - Covers all four world Tamil studio neural suites (`ta-IN-ValluvarNeural`, `ta-LK-KumarNeural`, `ta-SG-AnbuNeural`, `ta-MY-SuryaNeural`, `ta-IN-PallaviNeural`, `ta-MY-KaniNeural`, `ta-SG-VenbaNeural`, `ta-LK-SaranyaNeural`) plus custom FM-bass / bright-room studio profiles and 10 flagship Indian, Multilingual & Expressive English neural voices (`Prabhat`, `Andrew Multilingual`, `Brian Multilingual`, `Ryan`, `William`, `Neerja Expressive`, `Emma Multilingual`, `Ava Multilingual`, `Sonia`, `Natasha`).
2. **Turned ON by Default Out-of-the-Box & on Update (`2.61 -> 2.62`):**
   - Voice output, Online Studio Ultra-HD Neural synthesis, and Daily Auto Male ↔ Female Rotation are automatically enabled out of the box (and migrated ON once when updating from `<= 2.61`), while preserving any manual toggle made afterwards.
   - AI Assistant answers also speak in alternating Male ↔ Female Studio Ultra-HD voices automatically without manual slot setup.
3. **Instant Offline System Voice Fallback + Local LRU MP3 Cache:**
   - Caches frequent battery, charging, thermal, and weather voice clips locally for instant `0 ms` replay, works over NextDNS, silences immediately during phone calls (`CallGuard`), honors Quiet Hours, and automatically falls back in `0 ms` to installed Android `TextToSpeech` voices whenever offline.
4. **Full Studio Voice Card in `Settings → Voice & daily watch`:**
   - Displays **Today's** and **Tomorrow's** scheduled voices, one-tap **`👨 Preview Male HD`** & **`👩 Preview Female HD`** preview buttons, and all **10 Studio Ultra-HD Voices** for instant tap-to-preview or manual locking.

---

## தமிழ் சுருக்கம் (Tamil Summary)

புதிய Android அனுமதிகள் எதுவும் சேர்க்காமல், **Live Guard 2.62**-இல் **10 ஸ்டுடியோ Ultra-HD ஆன்லைன் குரல்கள் (`5 ஆண் + 5 பெண்`)** மற்றும் **தினசரி ஆண் ↔ பெண் தானியங்கி குரல் மாற்றம்** இயல்பாகவே (**Default ON**) இயக்கத்தில் சேர்க்கப்பட்டுள்ளது:

1. **10 ஸ்டுடியோ Ultra-HD குரல்கள் (தமிழ் & ஆங்கிலம் — 5 ஆண் + 5 பெண்):**
   - வள்ளுவர், குமார், அன்பு, சூர்யா, வள்ளுவர் பேஸ் மற்றும் பல்லவி, கனி, வெண்பா, சரண்யா, பல்லவி தெளிவு ஆகிய 10 துல்லியமான Ultra-HD குரல்கள் சேர்க்கப்பட்டுள்ளன.
2. **இயல்பாகவே (Default ON) இயக்கம் + தினசரி ஆண் ↔ பெண் மாற்றம்:**
   - செயலி நிறுவும்போதும் புதுப்பிக்கும்போதும் தானாகவே இயங்கும்; தினமும் ஆண் குரல் ↔ பெண் குரல் என மாறி ஒலிக்கும்.
3. **இணையம் இல்லாதபோது உடனடி சிஸ்டம் குரல் (Offline System TTS Fallback):**
   - இணையம் இல்லாத நேரங்களில் எந்தத் தடையுமின்றி போனில் உள்ள Android சிஸ்டம் குரலுக்குத் தானாக மாறும்; அழைப்பின்போது அமைதியாகக் காத்திருக்கும்.

---

## Verification and device testing

Published APK: **`CleanSweep-v2.62.apk`** (`14,280,379` bytes, GitHub Release SHA-256 `c13a7c256c8752889f86aadea71e958c4bc057f8186a56407638b71193bd177d`), built and verified by Build APK [run 39794121797](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39794121797) from commit `d25dbb3` (`arena/05a52d45-rp-android-apps`). Validated through automated localization audits (`1266` EN/TA keys), manifest permission delta checks (`0` added permissions), release lint, unit & live 20-voice neural synthesis tests (`48` suites / `426` required regression tests), stable signing certificate verification (`9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55`), and GitHub Release SHA-256 asset verification in GitHub Actions; these exact candidate bytes were **not installed on a phone** prior to publication. Passing CI is not Play Protect certification or an all-device compatibility claim.

Version 2.62 / code 67.
