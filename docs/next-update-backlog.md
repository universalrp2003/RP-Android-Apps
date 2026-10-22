# Queued Next-Update Items

## Completed in Live Guard `2.61` (`versionCode = 66` — Device & Play Protect Verified on 22 October 2026)

- [x] **Live Guard Home-Screen Widget — 4th `காட்சி` (`View`) Mode (`மழை & புயல்` / `Rain & storm`), Dedicated `CleanSweepWeatherWidget` `2×2` Preset, Urgent Storm Brief Line Across All Views, and Smart `Open` (`திற`) Deep-Link Navigation** (`CompactWidgetPolicy.kt`, `CleanSweepWidget.kt`, `WeatherAlertStore.kt`, `MainActivity.kt`, `CompactWidgetPolicyTest.kt`).
- [x] **Google Search Console Dual Verification (`HTML tag` + `site/google07eb97f28234915c.html`)** deployed inside `site/`.

## Minor Polish Spotted During `2.61` Screenshot Review (Bundle into Next Future Code Update)

- [ ] **Decode numeric HTML/XML character entities (`&#x1f537;`, `&#8217;`, etc.) in RSS news headlines (`NewsPolicy.kt`)** so external news feeds containing hex/decimal HTML entities render real Unicode symbols (`🔷`, `’`) cleanly on the Home news banner.
