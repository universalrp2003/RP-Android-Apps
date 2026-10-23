## Live Guard (CleanSweep) v2.64 — Smart News Voice Announcer, Full Tamil/English News Sync & Fresh Rain Alerts

**Package:** `com.universalrp.cleansweep` · **Version:** `2.64` (`versionCode = 69`) · **Minimum Android:** Android 8.0+ (API 26)
**Verification Note:** Built, linted, unit-tested, and verified via CI static/manifest/signature checks; this exact build was not installed on a phone prior to GitHub release publication (adds zero new permissions over `v2.63`).

### What's New in v2.64
- **Smart Non-Irritating News Headlines Voice Announcer (`OFF / ON` + `30m / 1h / 2h / 3h`):**
  - Automatically announces up to **5 new clean headlines** ONLY when your **Phone Screen is OFF**, **not in a call**, **no music/video is playing**, and outside **`10 PM – 6 AM`** night quiet hours.
  - **Zero Repeat Guarantee:** Tracks every headline spoken today per language and stays 100% silent once caught up.
  - **Manual Instant Readout + Stop:** Added **`🔊 Read today's news`** on the Home news strip, inside **Today's headlines**, in **Settings**, and in **Voice & daily watch**, alongside an instant **`Stop reading`** button.
  - **Clean Speech Sanitizer:** Strips `[Video]`, `BREAKING:`, publisher suffixes, emojis, `Read more...`, and filters out truncated or stub headlines before speaking.
- **100% Tamil ↔ English News Strip & Voice Synchronization:**
  - Switching between **தமிழ்** and **English** now refreshes both displayed Home headlines and spoken announcements in the matching language immediately, backed by separate Tamil (`Google News TA`, `OneIndia Tamil`, `News18 Tamil`) and English (`The Hindu`, `Google News India`) RSS caches.
- **24×7 Background Rain Alert Freshness & Auto-Clear:**
  - Enforces strict 20-minute TTL on short-term rain alerts (`45m` for severe alerts), resets stale alerts whenever your live GPS or saved fallback village/city changes, and automatically cancels old weather notifications as soon as skies clear.
- **Updated Bilingual Guide:** Full Tamil & English in-app guide updated for all v2.64 controls.
