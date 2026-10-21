# Ramesh Radio 1.18 (`versionCode = 18`) — Built-in Default `Tomorrow.io` 1-Minute Radar for Every User (Zero API Key Needed)

## English Summary

Following real-world physical-device verification of **Ramesh Radio 1.17** in Alangudi (`622301`), where coarse default Open-Meteo NWP grids falsely reported `"Ālangudi · Rain starting now"` while `Tomorrow.io` accurately reported `0 mm/hr` dry cloudy skies, **Ramesh Radio 1.18** makes **`Tomorrow.io` 1-minute radar the automatic built-in default for every listener — with ZERO API key setup and ZERO Location permissions**:

1. **Automatic Built-in `Tomorrow.io` 1-Minute Radar (`No Key Required`):**
   - Queries `Tomorrow.io` `1m` + `1h` timelines directly from each listener's phone with smart 10-minute tile caching — accurate down to the minute right out of the box without pasting an API key!
2. **Instant Stale False-Alert Cleanup + Calibrated `MET Norway` / `Open-Meteo` Fallback:**
   - Immediately refreshes any old `Open-Meteo` cached alert as soon as Ramesh Radio opens, and suppresses trace NWP drizzle noise in fallback models so dry cloudy evenings stay 100% quiet.
3. **Full 2-Line Mini Alert Banner Headline + Smooth `25%` Audio Ducking:**
   - Full non-truncated village + weather alert headline at the top bar when real rain or storm approaches, keeping smooth `25%` FM/music volume ducking and **zero Android Location permissions**.

---

## தமிழ் சுருக்கம் (Tamil Summary)

ஆலங்குடி (`622301`) நேரடி சோதனைக்குப் பிறகு, எந்த இட (Location) அனுமதியும் இல்லாமல் **Ramesh Radio 1.18**-இல் இப்போது **Tomorrow.io 1-நிமிட ரேடார்** எந்த API விசையும் இன்றி அனைவருக்கும் தானாகவே (Default) இயங்கும்:

1. **API விசை தேவையில்லை — நேரடி `Tomorrow.io` 1-நிமிட துல்லியம்:**
   - விசை எதுவும் ஒட்டாமலேயே துல்லியமான மழை & புயல் கண்காணிப்பு தானாகச் செயல்படும்.
2. **தவறான மழை எச்சரிக்கை நீக்கம்:**
   - மழை இல்லாத மேகமூட்ட நேரங்களில் வந்த தவறான `"Rain starting now"` எச்சரிக்கை முழுமையாகச் சரிசெய்யப்பட்டுள்ளது.

---

## Verification Status

- The published APK (`Ramesh-Radio-v1.18.apk`, `14,641,505` bytes, GitHub SHA-256 `8dc0dcca9e19da1943af39119c05511c49b97ce58c98e30b927849c0f288362f`), built by Build APK [run 39606864576](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39606864576) from commit `0d87ea2`, was **installed and verified live on physical phone hardware by the owner on 21 October 2026 (`7:59 PM IST`, Alangudi `622301`)**, confirming keyless `Tomorrow.io` radar accuracy (`Clear / normal — staying silent · 7:59 pm · 28°C · Tomorrow.io (1-min radar)` with `Personal API key not saved — free built-in radar active`). Automated shared bilingual catalog checks, unit tests, merged manifest permission checks (`0` added permissions, `0` Location permissions), release APK signing, and SHA-256 verification executed in GitHub Actions CI.
