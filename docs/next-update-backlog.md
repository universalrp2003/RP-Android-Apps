# Queued Next-Update Items

## Completed in Live Guard `2.61` (`versionCode = 66` — Device & Play Protect Verified on 22 October 2026)

- [x] **Live Guard Home-Screen Widget — 4th `காட்சி` (`View`) Mode (`மழை & புயல்` / `Rain & storm`), Dedicated `CleanSweepWeatherWidget` `2×2` Preset, Urgent Storm Brief Line Across All Views, and Smart `Open` (`திற`) Deep-Link Navigation** (`CompactWidgetPolicy.kt`, `CleanSweepWidget.kt`, `WeatherAlertStore.kt`, `MainActivity.kt`, `CompactWidgetPolicyTest.kt`).
- [x] **Google Search Console Dual Verification (`HTML tag` + `site/google07eb97f28234915c.html`)** deployed inside `site/`.

## Minor Polish Spotted During `2.61` Screenshot Review (Bundle into Next Future Code Update)

- [ ] **Decode numeric HTML/XML character entities (`&#x1f537;`, `&#8217;`, etc.) in RSS news headlines (`NewsPolicy.kt`)** so external news feeds containing hex/decimal HTML entities render real Unicode symbols (`🔷`, `’`) cleanly on the Home news banner.

## Queued for the next Tamilan Store build (owner asked 13 November 2026)

Bundle these into the next Store release; do not start a Store-only build just for them.

- [ ] **The Store's About screen still says "Our 13 apps" while the catalog ships 15.** The banner
      already says "15 apps", so the two disagree on the same screen. The owner explicitly said to
      ride this along with the next Store change rather than publish a build for it alone.
- [ ] **The About screen reports a stale bundled-catalog date.** It reads "Bundled catalog: 30
      October 2026" even though the shipped `catalog.json` is newer (System App Disabler 0.2.0,
      Tamilan Store 0.13 are pinned in it). Either the date constant is not regenerated when the
      catalog is rebuilt, or it is hard-coded; make it derive from the catalog itself so it cannot
      drift again.
- [ ] While in there, check that the About screen's app count and the catalog cannot disagree again:
      derive the number from the parsed catalog instead of a literal in the layout, and cover it with
      a unit test.

## Known gap: update awareness (owner asked 13 November 2026)

- [ ] **System App Disabler and Android TV Guard have no update check.** Twelve of the fourteen apps
      show the once-a-day foreground "Update available" dialog; Secret Message has none in its shipped
      build either. The Disabler and TV Guard therefore cannot tell a user that a newer release exists.
      Add the shared `AppUpdates.Control` / `DailyWebsiteUpdateNotice` path to both (read-only,
      foreground-only, opens the official site, never downloads or installs), and add each to
      `AppUpdates.ids`. This needs a Disabler 0.3.0 and a TV Guard 1.6.0, so build, owner-test, pin,
      then publish — never a rebuild of untested bytes.
- [ ] **A turned-off row was labelled "● running now".** Fixed in source on 13 November 2026 (the
      off branch is now tested before the running branch in `MainActivity`), with a test that asserts
      that order. It needs a Disabler rebuild to reach the phone: disabling leaves the process alive,
      so `ps` still lists the package. Bundle it with the update-check work above in 0.3.0.
- [ ] **The Disabler cannot tell "disabled" from "suspended", and its Enable button trusted a local
      note.** Owner report 13 November 2026: a turned-off app came straight back after force-close,
      "never stopped". Cause: on HyperOS `pm disable-user` is refused for system packages, so the app
      falls back to `pm suspend`, which stops the app from starting but leaves any live process
      alone — force-closing it does nothing. The row then still read its own SharedPreferences note
      rather than asking the phone. Source is now fixed: `isActuallyOff()` checks the framework
      (a disabled package yields no `ApplicationInfo`) and `pm list packages -e`, the Enable/Disable
      decision comes from that, a suspend that reports exit 0 but leaves the package enabled is now
      a reported failure instead of a silent success, and the row has a third state
      "● stopped — no process, starts on next launch" from `FLAG_STOPPED`. Needs 0.3.0 to reach the
      phone. The diagnostic ("Check Shizuku shell") now prints enabled/disabled list membership plus
      `dumpsys package` flags for HyperComm so the next report is evidence, not a guess.
