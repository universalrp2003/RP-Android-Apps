## Oor Arivippu (ஊர் அறிவிப்பு) v1.1.8 — More Accurate Rain Alerts

**Package:** `com.universalrp.voiceannouncer` · **Version:** `1.1.8` (`versionCode = 13`) · **Minimum Android:** Android 8.0+ (API 26)
**Published Signed Asset:** `Oor-Arivippu-v1.1.8.apk` (signed and published by CI).
**Verification Note:** Built, linted, unit-tested, and verified via CI static / manifest / signature checks. Permission delta over `v1.1.7`: `0` new permissions added. **Post-publication device confirmation (24 October 2026):** the owner installed `Oor-Arivippu-v1.1.8.apk` on their phone and confirmed it works.

### Why this release
Oor Arivippu shares its rain & storm alert engine with Live Guard and Ramesh Radio. This release ships the shared accuracy improvement so the town rain alerts stop firing on dark-but-dry days.

### What's New in v1.1.8
- **More accurate rain alerts (துல்லியம்):** the 30-minute rain heads-up now only announces when the forecast rain reaches a *noticeable* rate (≥ 0.4 mm/hr). A dark, chilly, merely-drizzling sky no longer triggers a spoken *"rain is starting now"*.
- **Everything else unchanged:** announcements, GPS + saved-PIN location, and voice delivery are exactly as in `v1.1.7`; this is the shared rain-accuracy update only.
- **No new permissions** — `0` added over `v1.1.7`.
