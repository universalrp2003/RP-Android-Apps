## Live Guard (லைவ் கார்டு) v2.70 — Rain Alert Now Shows the Real Rain Rate (+ fewer false "rain now")

**Package:** `com.universalrp.cleansweep` · **Version:** `2.70` (`versionCode = 75`) · **Minimum Android:** Android 8.0+ (API 26)
**Published Signed Asset:** `Live-Guard-v2.70.apk` (signed and published by CI).
**Verification Note:** Built, linted, unit-tested, and verified via CI static / manifest / signature checks; this exact candidate build was **not installed on a physical phone** prior to GitHub release publication. Permission delta over `v2.69`: `0` new permissions added.

### Why this release
Two things about the rain & storm alert. First, the status line under a rain alert carried a confusing phrase. Second, on dark, chilly-but-dry days the app sometimes announced "rain is starting now" when no noticeable rain actually fell.

### What's New in v2.70
- **The rain alert now shows the real rain rate (மழை விகிதம்):** the widget brief now reads the actual forecast intensity instead of the old line — e.g. *"Falling now · 2.4 mm/hr"* / *"இப்போது வருகிறது · 2.4 மிமீ/மணி"* while it rains, and *"Up to 6.0 mm/hr in ~30 min"* / *"~30 நிமிடத்தில் 6.0 மிமீ/மணி வரை"* when rain is approaching — so you can see how heavy it will be.
- **Fewer false "rain starting now" alerts (துல்லியம்):** the alert now only fires when the forecast rain reaches a *noticeable* rate (≥ 0.4 mm/hr). A merely dark, chilly, trace-drizzle sky no longer triggers a spoken *"rain is starting now"*.
- **No new permissions** — `0` added over `v2.69`; this is a display + accuracy change only.
