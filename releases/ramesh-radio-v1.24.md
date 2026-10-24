## Ramesh Radio (ரமேஷ் ரேடியோ) v1.24 — Rain Alerts Without Any Location Permission

**Package:** `com.universalrp.tamilnadufm` · **Version:** `1.24` (`versionCode = 24`) · **Minimum Android:** Android 8.0+ (API 26)
**Published Signed Asset:** `Ramesh-Radio-v1.24.apk` (signed and published by CI).
**Verification Note:** Built, linted, unit-tested, and verified via CI static / manifest / signature checks; this exact candidate build was **not installed on a physical phone** prior to GitHub release publication. Permission delta over `v1.23`: `ACCESS_COARSE_LOCATION` **removed** — `0` new permissions added.

### Why this release
Ramesh Radio asked for approximate (mobile-tower) location only to power local rain & storm alerts. This release removes that permission entirely: rain & cyclone alerts now work from your saved PIN code / town alone, so the app no longer needs any location access.

### What's New in v1.24
- **Location permission removed (இருப்பிட அனுமதி நீக்கப்பட்டது):** the `ACCESS_COARSE_LOCATION` permission and its "enable mobile tower" prompt are gone. Ramesh Radio now requests **no location permission at all**.
- **Rain & storm alerts still work — from your saved PIN/town (மழை எச்சரிக்கை):** set your PIN code or town once inside the rain settings (e.g. Alangudi / `622301`) and the 30-minute rain heads-up, very-heavy-rain and cyclone voice alerts keep working exactly as before, using your saved place instead of the phone's location.
- **Accuracy improvement shared with the other apps (துல்லியம்):** a merely dark, chilly, trace-drizzle sky no longer triggers a false *"rain is starting now"* — the alert now waits for a noticeable rain rate.
- **Playback unchanged:** the FM stream, foreground service and background-playback behaviour are untouched; only the location permission and the rain-alert location source changed.
