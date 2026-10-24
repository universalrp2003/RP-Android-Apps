## Ramesh Radio (ரமேஷ் ரேடியோ) v1.23 — Keep Playing With The Screen Off (Background-Playback Guide)

**Package:** `com.universalrp.tamilnadufm` · **Version:** `1.23` (`versionCode = 23`) · **Minimum Android:** Android 8.0+ (API 26)
**Verification Note:** Built, linted, unit-tested, and verified via CI static/manifest/signature checks; this exact candidate build was **not installed on a physical phone** prior to GitHub release publication (`0` new permissions added over `v1.22`). The published asset's size and SHA-256 are recorded in [`docs/published-release-digests.md`](../docs/published-release-digests.md).

### Why this release
On many phones the radio stopped when the screen turned off. Ramesh Radio already plays through a `mediaPlayback` foreground service with a network wake lock, so on stock Android it never stops — but Xiaomi/MIUI/HyperOS, realme/Oppo/Vivo and Samsung One UI kill background apps with their own battery optimisation, which cut the stream out. This release adds an in-app, bilingual reminder that walks the user through the fix.

### What's New in v1.23
- **In-app "keep playing in the background" guide (தமிழ் & English):**
  - When the phone's battery optimisation is stopping Ramesh Radio, the app now shows a short bilingual reminder with a one-tap **Open app settings / அமைப்புகளைத் திற** shortcut.
  - The reminder lists the three steps that fix it on every phone: turn **Autostart ON**, set **Battery** to **No restrictions / Unrestricted**, and **lock** the app in the Recents menu (🔒).
  - It appears only while the app is actually under battery optimisation, disappears the moment the app is exempt, and stays quiet for seven days after **Not now / பிறகு**. It never blocks playback.
- **No playback code-path change and `0` new permissions:** the player, wake lock and foreground service are unchanged; this release only adds the on-device reminder above (no new Android permission is requested).
