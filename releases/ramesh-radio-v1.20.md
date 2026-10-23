## Ramesh Radio (ரமேஷ் ரேடியோ) v1.20 — Cell-Tower Local Weather, Scoped Rain Alerts & Updated Guide

**Package:** `com.universalrp.tamilnadufm` · **Version:** `1.20` (`versionCode = 20`) · **Minimum Android:** Android 8.0+ (API 26)

### What's New in v1.20
- **Zero-GPS-Drain Mobile Tower Approximate Location (`ACCESS_COARSE_LOCATION`) + Saved PIN Fallback:**
  - Resolves your current area automatically from your connected mobile cell tower / network provider when Location is ON (zero GPS battery drain!), and seamlessly falls back to your saved PIN code / town when Location is OFF or unavailable.
- **Strictly Scoped Rain & Cyclone Alerts (No Unwanted Background Polling):**
  - Checks and announces local rain/storm alerts **ONLY** while the **Ramesh Radio window is open** OR while **radio audio is actively playing**. As soon as radio playback stops and the app closes, weather polling halts completely (leaving 24×7 background alerts to **Live Guard**).
- **Zero Stale Rain Messages:**
  - Added strict 20-minute forecast freshness validation and automatic place-change reset so old rain alerts never linger after skies clear or after moving to a new town.
- **Updated Bilingual Guide & Sister-App Links:** Updated Tamil & English in-app guide and sister-app catalog.
