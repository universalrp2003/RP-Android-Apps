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
- [ ] **A suspended app can still have a running process.** The 0.3.0 candidate shows
      `suspended — may still run` rather than claiming that it is fully off. The running list and
      suspension list can overlap, and the `turned off` count includes suspensions; make that count
      clear without implying the process has stopped. Verify state on each actual device.
- [ ] **Suspension differs from disable and the local record may become stale.** The app attempts
      disable first, then suspension if disable fails. A reported successful suspend is not evidence
      that a process is stopped or cannot restart. The app stores its own successful-action record,
      which may become stale if state changes elsewhere; don't treat it as a live OS check. On the
      owner's HyperOS device, the enabled/disabled package lists did not reveal suspension.
- [ ] **Suspension cannot be inferred from enabled/disabled lists.** On the owner's HyperOS
      device a reported suspension stayed in the `-e` list, and the inspected `flags=` line did not
      expose it. The newer candidate queries the public suspension API on Android 10+ and shows
      unknown when unavailable; keep checking real-device compatibility and external state changes.
- [ ] **Persistent flag and suspension need cautious wording.** HyperComm on the owner's phone
      has `FLAG_PERSISTENT` and was observed running after suspension and force-close. The flag alone
      does not prove it is always running, cannot be disabled or uninstalled, or that disabling is
      safe. The app now distinguishes reported suspension from disable and avoids those claims;
      the saved suspension record can become stale. Verify actual behaviour on the device.
- [ ] **Joyose description overclaims that it cannot be stopped.** Owner reported it was not
      running after suspension; Settings showed Force stop greyed out on 14 November 2026. The
      UAD-derived row description says "can't be stopped". Review and replace that categorical
      claim with device-dependent wording, without assuming it stays stopped after a reboot.

### App Disabler: suspension research and state correctness

- Source now verifies requested suspension results, queries `PackageManager.isPackageSuspended(packageName)` on API 29+, shows unknown on failures/older versions, offers recovery, and separates counts. Unit and device testing remains necessary, especially external/multiple suspenders and user profiles. Don't publish newly rebuilt bytes as the previous owner-tested candidate. See [suspension research](appdisabler-suspension-research.md).
- Prototype synchronous restriction writes only with disposable fixtures and privileged access; compare immediate-reboot baselines, restore and OEM behavior before claiming a persistence fix. Do not equate suspension with disable or automatically uninstall protected apps.

- [ ] **Unlisted user apps must not automatically read “Safe to disable.”** Owner screenshots of
      the hash-matched corrected candidate show Aadhaar, AadhaarFaceRd and an IDBI app with that
      green label; they are absent from the bundled UAD list and the loader defaults non-system
      apps to SAFE. Design a neutral, understandable classification and test it before rebuilding;
      functionality and importance depend on the user's needs. Do not publish rebuilt bytes as
      though they were the current owner-tested APK.
- [ ] **Overlapping states need clear row text.** In the owner's suspended filter Earphones appears
      with a “disabled” row. The counts and filters intentionally permit disabled and suspended to
      overlap, but the row only displays one status. Consider displaying both where both are known.

### Agreed follow-up: unlisted apps and persistence lab

- Source now treats every package missing from UAD as Unlisted, offers an explicit Google AI link
  on the row and detail/confirmation, and warns that AI answers may be wrong and Google receives
  the app name and package. The user decides; no answer is fetched or applied automatically.
  Verify on stock emulators and the owner's phone before release.
- Source now displays both disabled and suspended when both are observed. Verify the overlap with
  a disposable fixture and on-device before release.
- `appdisabler-persistence-lab.yml` tests flags-0 baseline, synchronous disable, suspend baseline,
  and suspend plus hidden flush, with an inert fixture on stock Android 15/16 only. It is research,
  not shipped behavior or a guarantee; no OEM or physical reboot is covered.

- [ ] **AI answers are speculative.** The owner opened Google AI for Live Guard and IDBI in the
      hash-matched 0.3.0 candidate. Google speculated about OEM/carrier/enterprise origins for this
      project's own `com.universalrp.cleansweep` and gave categorical banking safety claims. Keep
      AI opt-in and warn users that answers can be wrong; do not use generated text as a rating or
      auto-disable recommendation.
- [ ] **Compact and unclipped status rows.** In the owner's 0.3.0 screenshots the unlisted cards
      are tall from stacked actions, and the “disabled and suspended — tap Enable to restore”
      status is clipped. Shorten or wrap the status and reduce vertical waste in a future candidate;
      preserve rating colors and both state labels. Any rebuild needs fresh owner hash-matched tests.

### Current continuation

- Compact unlisted rows now move the AI link into the content column beside the existing action
  column, and the dual status is shortened to `disabled and suspended`. Candidate `d09ab49e…`
  passed stock smoke and fixture cycles (with infrastructure retries). The owner identified the
  matching hash and checked the UI and a delayed reboot/restore cycle with an inert user-app fixture
  on a Redmi 13 5G. This does not establish immediate-reboot or system-package persistence.
- The Shizuku persistence experiment is isolated to an emulator-only debug activity and a fixed
  disposable fixture. The candidate pipeline checks that its activity is absent from the release
  manifest and DEX. Experimental results must not be described as a shipped persistence fix.
