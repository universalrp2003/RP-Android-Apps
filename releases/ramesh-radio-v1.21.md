## Ramesh Radio (ரமேஷ் ரேடியோ) v1.21 — Live Mobile-Tower Place Card, Automatic Location Prompt & Full Spoken Place Details

**Package:** `com.universalrp.tamilnadufm` · **Version:** `1.21` (`versionCode = 21`) · **Minimum Android:** Android 8.0+ (API 26)
**Verification Note:** Built, linted, unit-tested, and verified via CI static/manifest/signature checks; this exact candidate build was not installed on a physical phone prior to GitHub release publication (`0` new permissions added over `v1.20`).

### What's New in v1.21
- **Live Mobile-Tower Location Permission Prompt & 1-Tap Setup:**
  - Opening **Rain & Cyclone Voice Alert** or tapping **Check now** properly prompts for Android's `ACCESS_COARSE_LOCATION` runtime permission (and shows a clear **`📡 Allow Mobile Tower Location`** button whenever permission is not yet granted) so mobile-tower village/town lookup works right away without manual system-settings hunting.
- **Multi-Source Cell-Tower Reverse Geocoding (Works on Xiaomi HyperOS / MIUI / All OEMs):**
  - Added layered reverse geocoding (`Android Geocoder` -> `OpenStreetMap Nominatim` Tamil/English village+district+PIN -> `BigDataCloud` reverse lookup -> coordinate fallback) so cell-tower weather checks **always resolve a real town/village + district name** even on phones where `Geocoder` returns empty results.
- **Prominent Active Weather Area Card & Checked-Status Place Label:**
  - The Rain & Cyclone dialog, floating rain alert banner, and `"Checked 11:25 AM · 📍 <Town (District)> · ..."` line now prominently display the **exact active village/town, district, and source (`📡 Mobile Tower` vs `📌 Saved PIN/Town`)**.
- **Full Place + District Spoken in Every Rain & Storm Voice Announcement:**
  - Every `30-min Rain Alert`, `Heavy Rain Warning`, `Urgent Cyclone Warning`, and `Test voice` announcement now clearly speaks your **exact town/village AND district details** aloud while smoothly ducking radio playback to 25% and restoring full volume afterwards.
