# SMS Guard (குறுஞ்செய்தி காவலன்) 1.1.0 (`versionCode = 6`) — 22 October 2026

- **GitHub Release Tag:** [`sms-guard-v1.1.0`](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/sms-guard-v1.1.0)
- **Signed APK Asset:** [`SMS-Guard-v1.1.0.apk`](https://github.com/universalrp2003/RP-Android-Apps/releases/download/sms-guard-v1.1.0/SMS-Guard-v1.1.0.apk)
- **Package ID:** `com.universalrp.smsguard`
- **Minimum Android:** Android 8.0+ (API 26), `targetSdk = 36`
- **Asset Size:** `12,330,481` bytes
- **SHA-256 Digest:** `67567d1f39571edc918c7bf9a810c06e4729fb1b43c40226422230e0f051996c`
- **Signing Certificate SHA-256:** `9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55` (`RP Ideas`)
- **Published by Workflow Run:** [`39787065119`](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39787065119) (commit `7a2f123`)

## Highlights

1. **6 Studio Online Neural Voices (3 Male + 3 Female) & Automatic Daily Alternation:**
   - Added 6 high-quality Neural HD voices covering both **Tamil** (`Valluvar`, `Anbu`, `Surya` Male · `Pallavi`, `Venba`, `Saranya` Female) and **English** (`Prabhat`, `Guy`, `Ryan` Male · `Neerja`, `Aria`, `Sonia` Female).
   - Enabled **automatic daily Male ↔ Female voice alternation** by default, speaking **Online Neural HD first** when connected and automatically falling back in `0 ms` to installed **Android System TTS** voices when offline.
2. **Zero-Gap Home Header & Collapsible Dropdown Search Bar (`🔍`):**
   - Eliminated nested duplicate top status-bar inset spacing on the Messages home screen and replaced the permanent large search box with a compact dropdown search bar triggered on tapping **`🔍`**.
3. **Built-in Step-by-Step Google Safe Browsing API Key Guide:**
   - Added expandable Tamil & English step-by-step setup instructions, direct Google Cloud Console links, voice readout, and one-tap clipboard paste inside **Shield**.
4. **Multi-Device Verified Foundation:**
   - Retains fast `400`-row paged loading + `O(N)` UTR deduplication for 50,000+ Dual-SIM SMS inboxes, outgoing SMS voice suppression, NextDNS compatibility, 100% OTP silence, Sleep Time sequential wake bank readout, and biometric-locked Bank / Card / UPI / FASTag finance tracking (tested prior to 1.1.0 release on Samsung Galaxy M32, Xiaomi Redmi 13 5G, and OPPO A3s).
