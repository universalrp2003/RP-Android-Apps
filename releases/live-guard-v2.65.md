## Live Guard (CleanSweep) v2.65 — Default 3-Hour Screen-OFF News Voice Bulletin & Clean Tamil UI Polish

**Package:** `com.universalrp.cleansweep` · **Version:** `2.65` (`versionCode = 70`) · **Minimum Android:** Android 8.0+ (API 26)
**Verification Note:** Built, linted, unit-tested, and verified via CI static/manifest/signature checks; this exact build was not installed on a phone prior to GitHub release publication (adds zero new permissions over `v2.64`).

### What's New in v2.65
- **News Headlines Voice Announcer Enabled by Default (`ON` at `3h` Interval):**
  - News Headlines Voice Announcer is now **ON by default** with **`3h` (`180` minutes)** selected automatically out of the box (including a one-time automatic upgrade migration from `v2.64` while keeping any later user preference changes saved permanently).
  - Retains all 4 selectable cadence chips (`30m / 1h / 2h / 3h`), strict **Screen-OFF + No-Call + No-Media + `10 PM – 6 AM` Night Quiet Window**, max **5** clean unannounced headlines per bulletin, zero duplicate repeats on the same day, and instant **`Read today's news`** + **`⏹ Stop`** controls.
- **Tamil UI Text & Button Layout Fixes (Zero Vertical Squishing, Zero Duplicate Sentences):**
  - Fixed horizontal button wrapping in **Settings**, **Voice & daily watch**, **Today's headlines**, and **ReadingMovePad** (`← → ↑ ↓ · Auto · Drag`) using `FlowRow` + `maxLines = 1, softWrap = false` so Tamil button labels never stack character-by-character vertically.
  - Trimmed duplicate Tamil helper paragraphs under **Status-bar data and watts** and tightened Tamil subtitle phrasing under **News headlines voice announcer**.
- **Shared Rain & Storm Voice Alert Upgrade:**
  - Every rain/storm voice alert now announces both the **town/village name and district details** clearly.
- **Updated Bilingual Guide:** Full Tamil & English in-app guide updated for all `v2.65` defaults and controls.
