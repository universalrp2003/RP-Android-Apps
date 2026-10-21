# Queued Next-Update Items (Do NOT Ship Alone — Include on Next Bug Fix or Feature Build)

Per owner direction on **21 October 2026 (`8:21 PM IST`)** after verifying **Live Guard `2.60` (`code 65`)** live on phone hardware, bundle the item below automatically into the **next Live Guard bug-fix or new-feature release** (no standalone rebuild required right now):

## 1. Live Guard Home-Screen Widget — 4th `காட்சி` (`View`) Mode: `மழை & புயல்` (`Rain & storm`) + Active Alert Line

- **Files to touch when building the next Live Guard update:**
  1. `cleaner/src/main/java/com/universalrp/cleansweep/widget/CompactWidgetPolicy.kt` (and `CompactWidgetPolicyTest.kt`):
     - Extend `enum class Profile { BATTERY, DATA, PHONE, WEATHER }` so tapping **`காட்சி` (`View`)** cycles cleanly across all 4 cards (`பேட்டரி & திட்டம்` -> `திட்டம் & நாட்கள்` -> `பாதுகாப்பு` -> **`மழை & புயல்` / `Rain & storm`**).
  2. `cleaner/src/main/java/com/universalrp/cleansweep/widget/CleanSweepWidget.kt`:
     - Render cached `WeatherAlertManager` snapshot in `Profile.WEATHER` (zero extra network calls on widget redraw unless user taps `புதுப்பி` / `Refresh`, keeping battery drain at zero):
       - `widget_title`: `label("Rain & storm", "மழை & புயல்")`
       - `widget_battery`: temperature (`28°C`) or severity badge (`~30 நிமி` / `புயல்` when active)
       - `widget_state`: saved village/town + status (`மழை அறிகுறி இல்லை` / `~30 நிமிடத்தில் மழை` / `மிக பலத்த மழை` / `புயல் எச்சரிக்கை`)
       - `widget_data`: wind speed + last checked 12-hour clock (`DisplayTime.clock(...)`) + radar source (`Tomorrow.io`)
       - `widget_brief`: `Quiet hours` / active storm priority summary, AND surface an urgent 1-line warning in `widget_brief` across the other modes whenever an active `RAIN_SOON` / `VERY_HEAVY_RAIN` / `CYCLONE` alert is live!
     - Tapping `திற` (`Open`) while on `Profile.WEATHER` deep-links straight into `Rain & cyclone alert` (`WeatherAlertScreen`).
  3. `cleaner/src/main/java/com/universalrp/cleansweep/weather/WeatherAlertManager.kt`:
     - Call `CleanSweepWidget.refresh(context)` whenever a background worker or manual `Check now` updates the weather state so the home-screen widget stays automatically synced.
