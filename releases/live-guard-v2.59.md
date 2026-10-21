# Live Guard 2.59 (`versionCode = 64`) — Built-in Default `Tomorrow.io` 1-Minute Radar for Every User (Zero API Key Needed)

## English Summary

Following real-world physical-device verification of **Live Guard 2.58**, **Ramesh Radio 1.17**, and **Oor Arivippu 1.1.3** across Alangudi (`622301`), **Live Guard 2.59** upgrades every user to **Tomorrow.io 1-minute radar precision by default with ZERO API key setup**:

1. **Built-in Default `Tomorrow.io` 1-Minute Radar (`Zero API Key Required`):**
   - Queries `Tomorrow.io`'s official keyless `1m` + `1h` timelines endpoint directly from each user's device IP (with smart 10-minute ~2 km tile caching), so **every user gets real 1-minute radar accuracy out of the box** without creating an account or pasting a key.
   - Optional personal `Tomorrow.io` API key remains supported (encrypted with Android Keystore `AES-GCM`) for dedicated high-volume personal quota.
2. **Multi-Model Anti-False-Positive Fallback (`MET Norway ECMWF` + Calibrated `Open-Meteo`):**
   - If a shared mobile carrier CGNAT IP temporarily hits HTTP `429`, automatically falls back to Norwegian Meteorological Institute (`MET Norway Locationforecast 2.0` `ECMWF`) and calibrated `Open-Meteo` with coarse NWP trace-drizzle noise filtering (`< 0.35 mm / 15m` at low probability ignored), eliminating false `"Rain starting now"` alerts on humid cloudy evenings.
   - Automatically refreshes any stale `Open-Meteo` cached alert immediately upon launching the updated app.
3. **Full 2-Line Alert Banner Headline Layout:**
   - Expands `WeatherMiniAlertBanner` headlines to up to 2 lines so Tamil and English village + alert titles read completely without mid-word clipping.

---

## தமிழ் சுருக்கம் (Tamil Summary)

ஆலங்குடி (`622301`) நேரடி சோதனைக்குப் பிறகு, எந்தப் புதிய Android அனுமதியும் சேர்க்காமல் **Live Guard 2.59** வெளியிடப்படுகிறது:

1. **எந்த API விசையும் இன்றி `Tomorrow.io` 1-நிமிட ரேடார் இப்போது அனைவருக்கும் தானியங்கி (Default):**
   - கணக்கு தொடங்கவோ API விசையை ஒட்டவோ தேவையில்லாமல், ஒவ்வொரு பயனருக்கும் **Tomorrow.io 1-நிமிட துல்லிய ரேடார்** தானாகவே வேலை செய்யும்.
2. **தவறான மழை எச்சரிக்கை நீக்கம் (`MET Norway ECMWF` + மேம்படுத்தப்பட்ட Fallback):**
   - மேகமூட்டமான மாலை நேரங்களில் தவறாக வந்த `"Rain starting now"` எச்சரிக்கைகள் முழுமையாகத் தடுக்கப்பட்டுள்ளன; புதுப்பித்தவுடன் பழைய பதிவு தானாகவே புதுப்பிக்கப்படும்.
3. **முழுமையான 2-வரி மினி அனிமேஷன் தலைப்பு:**
   - சிறிய மழை/புயல் அனிமேஷன் பட்டையில் ஊர்ப் பெயரும் எச்சரிக்கையும் பாதியில் வெட்டப்படாமல் முழுமையாகத் தெரியும்.

---

## Verification and device testing

The exact APK (`CleanSweep-v2.59.apk`, `13,953,820` bytes, GitHub SHA-256 `3668a0b5f1e7dd464a851f3d3224ca87157bbba7c4def38a90ea2935b8ef5b97`), published by Build APK [run 39606864576](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39606864576) from commit `0d87ea2`, was **subsequently installed and verified live on physical phone hardware by the owner on 21 October 2026 (`7:59 PM IST`, Alangudi `622301`)**, confirming accurate real-time radar status (`No rain approaching right now — staying quiet · Checked at 7:59 PM · 28°C · 7 km/h`). Build, unit/regression (47 suites / 417 tests), localization, manifest permission delta (`0` added permissions), APK and release-asset checks are recorded by the publishing workflow. Passing CI and a single-owner test are not an all-device compatibility claim.

Version 2.59 / code 64.
