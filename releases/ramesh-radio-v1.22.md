## Ramesh Radio (ரமேஷ் ரேடியோ) v1.22 — Zero Surprise Location Prompts & Explicit Opt-In Mobile Tower Privacy Card

**Package:** `com.universalrp.tamilnadufm` · **Version:** `1.22` (`versionCode = 22`) · **Minimum Android:** Android 8.0+ (API 26)
**Verification Note:** Built, linted, unit-tested, and verified via CI static/manifest/signature checks; this exact candidate build was not installed on a physical phone prior to GitHub release publication (`0` new permissions added over `v1.21`).

### What's New in v1.22
- **Never Auto-Prompts for Location Permission Anywhere:**
  - Removed automatic `ACCESS_COARSE_LOCATION` runtime prompts when launching **Ramesh Radio**, opening **Rain & Cyclone Voice Alert**, flipping the enable switch, or tapping **Check now** — so users listening to radio never see an unexpected Android system Location permission dialog.
- **Zero-Permission Saved PIN / Village Recommended Flow + Explicit Opt-In Only Button:**
  - Works 100% with **Location permission OFF (`Don't allow`)** when you save your **6-digit PIN code or Village/Town name** once (e.g., `622301` or `ஆலங்குடி` / `Madurai`).
  - Android only requests `ACCESS_COARSE_LOCATION` if you **explicitly tap** the optional **`📡 Enable Mobile Tower (Optional)` (`📡 மொபைல் டவர் இயக்கு (விருப்பம்)`)** button inside **Rain & Cyclone Voice Alert**.
- **Clear In-App Bilingual Privacy Note (`தமிழ்` & `English`):**
  - Explains clearly right inside **Rain & Cyclone Voice Alert** and the **In-App Guide** why Android classifies Mobile Signal Tower town lookup inside the system `Location` permission group, confirms that **Ramesh Radio never tracks or stores user movements** (`0% GPS battery drain`), and reminds users they can revoke Location permission in Android App Settings anytime while continuing to receive exact village + district rain alerts via their saved PIN/Town.
