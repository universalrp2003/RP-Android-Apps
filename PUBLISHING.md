# Build and release guide — RP Ideas

This guide covers the thirteen **non-Store** RP Ideas products plus the separate Tamilan Store hosted at [`universalrp2003/RP-Android-Apps`](https://github.com/universalrp2003/RP-Android-Apps) and the official website [`universalrp2003.github.io/RP-Android-Apps/`](https://universalrp2003.github.io/RP-Android-Apps/). The four legacy modules are `cleaner` / Live Guard, `radio` / Ramesh Radio, `equalizer` / PulseEQ, and `vocalremover` / RP Vocal Remover Studio. Other Android apps keep their own release workflows. Secret Message retains its separate signing identity. GitHub Releases are the verified direct-download channel. A successful CI build is required before publication; CI success is not Play Protect approval or a device-compatibility certification.

## Fresh-install signing migration — owner decision, 29 October 2026

The exposed legacy signing key is retired. New eligible main-key APKs must use the pinned owner-backed replacement through the secret-backed CI helper; never restore or publish with the old key. The owner selected a fresh-install migration: Android will not update old-key installations with replacement-key APKs, and uninstalling may erase local data. Keep the bilingual official-site warning visible before new APK links go live, and put the uninstall/data-loss warning in each release note. Tell users to download the official replacement first, export/back up important data, uninstall the old app, then install using Android; stop if Play Protect warns. Do not claim Play Protect clearance.

PR #57 merged and the main signed build/candidate checks passed. SMS Guard 1.1.11 (run [40843672129](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40843672129)), Vasanthi AI 1.1.5 (run [40846720080](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40846720080)), Oor Arivippu 1.1.10 (run [40854800838](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40854800838)), Tamil Podcast Player 1.6 (run [40856359350](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40856359350)), DailyVault 0.4 (run [40858134703](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40858134703)) Tamilan Store 0.6 (run [40859576371](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40859576371)) Expense Tracker 1.2 (run [40861057750](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40861057750)) Elderly Launcher 1.5 (run [40862085273](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40862085273)) Voice Notes 1.3.3 (run [40863878094](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40863878094)) Live Guard 2.74 (run [40865365438](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40865365438)) Ramesh Radio 1.29 (run [40866988270](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40866988270)) and PulseEQ 1.11 (run [40868591421](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40868591421)) are published with verified SHA-256 digests in RP-Android-Apps. None of the exact APKs was phone-tested; the Pages mirror remains unchanged pending the site inventory update. Other replacement-key releases remain pending their per-app gates. Never overwrite a public same-version asset. Keep Tamilan Store updater code unchanged; Secret Message is outside this signer rotation. Operating details and fingerprint are in [`docs/fresh-install-signing-migration.md`](docs/fresh-install-signing-migration.md).

## Publishing one app at a time (current procedure, 29 October 2026)

### The Live Guard permission gate applies only to Live Guard publishes

The strict permission/device-evidence gate runs **only when Live Guard itself is being published**.
`gh release view "$LIVE_GUARD_TAG"` decides that, not `releases/latest-tag.txt`: on 20 October 2026
a publish of Ramesh Radio 1.14 compared Live Guard against its own latest tag, could not read a
previous manifest, failed closed and published nothing. Do not answer a failure like that by
removing the gate or weakening the fail-closed comparison — scope it to the app being published.

Each release dispatch selects exactly one app. The four-app `Build APK` workflow still runs its
full candidate build and regression suite, but its publication step filters to the selected module;
Live Guard's permission/device-evidence gate runs only when Live Guard is selected. The Tamilan
Store/DailyVault workflow also accepts one app per dispatch, and every other app has its own
standalone workflow. For all workflows, `publish` defaults to **false**; first run candidate-only,
then dispatch the same app with publication enabled only after its own checks pass.

- Bump `versionName` / `versionCode` only for the app that changed and add that app's
  `releases/<tag>.md` note.
- Check that app's release tag does not exist before publication; never replace an existing
  same-version release or asset.
- `releases/latest-tag.txt` advances only when Live Guard itself is published; it is the baseline
  for the next permission delta.

## What triggers the Build APK and website workflows

Both `Build APK` (`.github/workflows/build-apk.yml`) and `Deploy Official Website to GitHub Pages` (`.github/workflows/pages.yml`) ignore the same documentation-only paths, so a commit that touches only `README.md`, `CHANGELOG.md`, `PUBLISHING.md`, `docs/published-release-digests.md` or a `releases/*.md` release note produces **no build run and no site redeploy** — there is nothing to build, nothing to publish and nothing to redeploy. Editing one of those files and seeing no run is the expected behaviour, not a broken pipeline.

Publication is controlled per app, never as a cross-app batch. Manual workflow dispatch requires a single app choice and defaults to candidate-only; the established one-app workflows also accept their app-specific `[publish-<app>]` commit marker. The two new-app releases use separate `[publish-tamilanstore]` or `[publish-dailyvault]` markers. Candidate builds must pass before a release marker is used, and every release workflow repeats its own build, signer, package/version and asset gates. The official Pages mirror is refreshed only after the public release asset's tag, size and SHA-256 have been verified and the site change is on `main`.

`releases/latest-tag.txt` is deliberately **not** ignored: the Live Guard permission-delta gate reads it as the previous-release baseline, so changing it still triggers a run. `site/**` is of course never ignored — it is the site's own content.

## Historical publication — SMS Guard 1.1.7 / code 13 (25 October 2026)

| App | Gradle module | Version / code | Tag | Asset |
| --- | --- | --- | --- | --- |
| SMS Guard | `smsguard` | 1.1.7 / 13 | `sms-guard-v1.1.7` | `SMS-Guard-v1.1.7.apk` |

Published by [SMS Guard run 40202187916](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40202187916): `SMS-Guard-v1.1.7.apk`, `12,428,897` bytes, SHA-256 `b1f01bf25127f1e0e4bd69cea3fc2fda1f9f2f9a59b0ac429c9387e0bf4c1371`. Two changes, both prompted by the owner's second
batch of screenshots: the projected month-end figure in the monthly-spend card no longer wraps
inside its own digits (every amount is now a single non-wrapping line, and the label wraps instead),
and the claim that the card and the period summary can never disagree is withdrawn - the card is
always the whole calendar month across every account, while the summary honours the selected
account and any excluded message, so the two legitimately differ (Rs.10,427 against Rs.10,302 on
the owner's phone). The figures, the month clamp, the projection rule and OTP silence are unchanged.
`0` new permissions over `1.1.6`. The exact candidate bytes were **not installed on a phone**
before publication, so no device test is claimed for them.

## Historical publication — Voice overhaul follow-ups: Live Guard 2.72, Ramesh Radio 1.25, SMS Guard 1.1.6 and Oor Arivippu 1.1.9 (25 October 2026)

| App | Gradle module | Version / code | Release tag | APK |
|---|---|---:|---|---|
| Live Guard / CleanSweep | `cleaner` | 2.72 / 77 | `live-guard-v2.72` | `CleanSweep-v2.72.apk` |
| Ramesh Radio | `radio` | 1.26 / 26 | `ramesh-radio-v1.26` | `Ramesh-Radio-v1.26.apk` |
| SMS Guard | `smsguard` | 1.1.6 / 12 | `sms-guard-v1.1.6` | `SMS-Guard-v1.1.6.apk` |
| Oor Arivippu | `oorarivippu` | 1.1.9 / 14 | `oor-arivippu-v1.1.9` | `Oor-Arivippu-v1.1.9.apk` |

Four apps were published on 25 October 2026 from the `arena` branch, each by its own workflow: **Live Guard 2.72 / code 77** and **Ramesh Radio 1.25 / code 25** by [Build APK run 40183935239](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935239) (`CleanSweep-v2.72.apk` `14,413,927` bytes SHA-256 `e6664f5d14f7fe31a8a754b5d35bde62ba14d1b6ee894ba2b454f4ab7952fc0b`, `Ramesh-Radio-v1.25.apk` `15,092,968` bytes SHA-256 `49f7a4321e2e6facc20c2fccf0bec696a3cf9c7eab833afc3056cfafd3cec634`); **SMS Guard 1.1.6 / code 12** by [SMS Guard run 40183935196](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935196) (`SMS-Guard-v1.1.6.apk` `12,428,897` bytes SHA-256 `3e129c31dd1556fc4cedfdfcd36f50147fda2edb3963ee10575bb244b064b8b8`); and **Oor Arivippu 1.1.9 / code 14** by [Oor Arivippu run 40183935183](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935183) (`Oor-Arivippu-v1.1.9.apk` `12,794,316` bytes SHA-256 `6915d597faa0ca9c0665bab695863d02fa04f56675796babcd9ce6446e286679`). `PulseEQ 1.9`, `RP Vocal Remover Studio 0.1.15` and `Secret Message 1.1.3` were already public at these versions, so the batch step skipped them and their releases are untouched.

- **Live Guard 2.72** limits each silent-place slot to chosen weekdays, so "Mon–Fri 9–6, Sat 10–2" is two slots instead of five repeated ones; makes a sustained HD-voice outage visible on the voice screen (wait, then announce — never a quieter fallback voice and never a silent drop); and removes the system text-to-speech code that 2.71 had already made unreachable.
- **Ramesh Radio 1.25** keeps a list of saved places for rain and cyclone voice alerts instead of one: a row of chips under the weather banner, one tap to switch, the ✕ forgets a place and promotes the next one. Up to 12 places, deduped by coordinates. `SharedWeatherRuntime` is deliberately unchanged, so Live Guard and Oor Arivippu see the new active place exactly as before.
- **SMS Guard 1.1.6** adds a monthly-spend card to the Finance tab: this month against last, the change in rupees and percent (suppressed when last month had no spending), the running daily average, the projected month end, the days remaining and a plain-language trend. The month ends at the last instant of the calendar month, not midnight on the last day.
- **Oor Arivippu 1.1.9** records what actually happened to each announcement (spoken / held for a call / no internet / GPS off / no voice / not spoken) with a summary line, persists the list across the process being killed, and makes the empty state say that an empty list means the voice was not delivered.

Live Guard added `0` dangerous permissions over `v2.71` — its manifest is byte-identical — so the permission-delta gate required no device-install record. Every one of the four release notes states plainly that the exact published bytes were **not installed on a phone** before publication, so publication itself carries no device test for them; three of the four were installed and run on the owner's phone later the same day (see below). `releases/latest-tag.txt` advanced to `live-guard-v2.72` for the next delta. The complete asset ledger is in [`docs/published-release-digests.md`](docs/published-release-digests.md).

**Device test, 25 October 2026 (after publication).** The owner installed the published APKs on their own phone and returned screenshots. Play Protect reported "This app looks safe" on Live Guard 2.72 and Oor Arivippu 1.1.9; Ramesh Radio updated in-app from 1.24 to 1.25. Confirmed on screen: Live Guard's silent-place day chips (seven chips, Monday first, all seven selected, "9:00 AM - 6:00 PM" and "+ another slot"), Oor Arivippu's `Version: 1.1.9` About line and the new A5 empty state, and Ramesh Radio's saved place, rain dialog and zero-location-permission message with a live check at Inunganur (Karur district, Tamil Nadu 639205). SMS Guard 1.1.6 was not among the reported screenshots, so no device result is claimed for it. The screenshots show no APK SHA-256, so these are owner-reported device results, not hash-matched ones. Still unverified: Ramesh Radio's chip strip, because the phone held exactly one saved place and the strip only appears with more than one.

That test found two release-note defects, both now fixed in the notes and in the live release bodies: the 2.72 note claimed a Monday-to-Friday default slot the code does not have (the default is `SilentPlaceSlot(ALL_DAYS, 9..18)`), and none of the four notes carried a `## ` Tamil section, so a Tamil-language owner was shown the English text inside a Tamil app. `tools/verify_apps_localization.py` now requires exactly one Tamil section, requires it to be the last section, and requires the bare `## ` marker with no words after it - `localizedReleaseNotes` returns `substringAfter(marker)`, so `## Tamil summary` used to leave the word "summary" as the first line of the Tamil release notes in the app. `releases/pulseeq-v1.9.md` and `releases/rp-vocal-remover-studio-v0.1.15.md` really did have a mid-note Tamil section that hid their checksum blocks from English readers, and both are fixed.

**CI status of the note/documentation commits (`3b1d901`, `4e2a728`).** Build APK (`40190656084`, `40191842460`), the website deploy and the Secret Message workflow all passed on both commits. The Oor Arivippu and SMS Guard *test* workflows (`40190656107`/`40191842472` and `40190656097`/`40191842454`) failed on both, and the failure is the same named test in each: `NeuralCloudVoiceEngineTest.livePlaceAnnouncementSynthesisWorksForTamilAndEnglishStudioVoices` and `SmsGuardEngineTest.liveOnlineNeuralStreamingSynthesizesRealMp3WhenNetworkAvailable`. Both are **live-network tests** that call `speech.platform.bing.com` and skip only when DNS resolution fails, so a resolved-but-unreachable endpoint fails them. They passed on `fb30665` at 11:19 UTC and failed at 11:52 and 12:08 UTC on two different commits, with no Kotlin source changed in either, so this is the Azure speech endpoint, not a regression. Re-running them is the only way to confirm, and `gh run rerun` and `workflow_dispatch` are both unavailable here, so a re-run needs another push to the `arena` branch. Do not "fix" these tests by weakening them: the HD neural voice is the only voice these apps have, so a live check that it really returns MP3 bytes is worth keeping.

### Previous publication — Weather Accuracy & Privacy Update: Live Guard 2.70 + Ramesh Radio 1.24 + Oor Arivippu 1.1.8 (24 October 2026)

| App | Gradle module | Version / code | Release tag | APK |
|---|---|---:|---|---|
| Live Guard / CleanSweep | `cleaner` | 2.70 / 75 | `live-guard-v2.70` | `CleanSweep-v2.70.apk` |
| Ramesh Radio | `radio` | 1.24 / 24 | `ramesh-radio-v1.24` | `Ramesh-Radio-v1.24.apk` |
| Oor Arivippu | `oorarivippu` | 1.1.8 / 13 | `oor-arivippu-v1.1.8` | `Oor-Arivippu-v1.1.8.apk` |

Live Guard 2.70 / code 75, Ramesh Radio 1.24 / code 24 and Oor Arivippu 1.1.8 / code 13 were published on 24 October 2026 from the `arena` branch by [Build APK run 40101594815](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40101594815) (Live Guard + Ramesh Radio, `CleanSweep-v2.70.apk` `14,419,847` bytes SHA-256 `8fc9f0a348a958f03c365148c4a8e012e63b85f939cf8eb71933040217beb52c`, `Ramesh-Radio-v1.24.apk` `15,076,580` bytes SHA-256 `d3454873fc179241f37acd6e7acf5060418091c1ede6b640b14c32e5988b4749`) and [Oor Arivippu run 40098498419](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40098498419) (`Oor-Arivippu-v1.1.8.apk` `12,777,932` bytes SHA-256 `1e66733e85f0fbbe43e3f34c1f47fa0b1e90a92bdf1a55da0a3a28f10b1b4f77`), leaving `PulseEQ 1.9`, `RP Vocal Remover Studio 0.1.15` and `Secret Message 1.1.3` untouched (the \"one app at a time\" rule: only the three apps' versions changed, so only their tags became public). Live Guard's home-screen widget rain card now shows the real observed/peak 60-minute precipitation rate instead of a generic quiet-hours note, and the shared rain \"starting now\" classification now requires a *noticeable* rate (`NOTICEABLE_RAIN_MM_HR = 0.40` mm/hr) across Live Guard, Ramesh Radio and Oor Arivippu. Ramesh Radio **removed the `ACCESS_COARSE_LOCATION` (mobile-tower) permission** and every location prompt, so rain and cyclone voice alerts use the saved PIN/Village with zero location access — a net `-1` permission. Oor Arivippu only bumped its About-screen sister-app links. Live Guard added `0` dangerous permissions over `v2.69`, so the permission-delta gate required no device-install record and the exact candidate bytes were published directly after full CI localization / manifest / regression / signature / release-asset checks without a pre-publication phone install. `releases/latest-tag.txt` advanced to `live-guard-v2.70` for the next delta. After publication the owner installed all three APKs on their phone and confirmed they work. The complete asset ledger is in [`docs/published-release-digests.md`](docs/published-release-digests.md).

## Also on 24 October 2026 — SMS Guard 1.1.5 solo

| App | Gradle module | Version / code | Release tag | APK |
|---|---|---:|---|---|
| SMS Guard | `smsguard` | 1.1.5 / 11 | `sms-guard-v1.1.5` | `SMS-Guard-v1.1.5.apk` |

SMS Guard 1.1.5 / code 11 was published **alone** on 24 October 2026 by [SMS Guard run 40048301709](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40048301709) (`SMS-Guard-v1.1.5.apk`, `12,412,513` bytes, SHA-256 `021d86dd573668aaf4e4776327458ad99fef436fc0d5af3cdb8dcbc48705445d`), leaving the six other apps untouched. It adds a dedicated **Bills (பில்கள்)** tab that captures bill SMS (credit card, TNEB, broadband, LPG, postpaid, water, insurance, loan EMI) automatically with per-reminder voice/notification/mark-paid/delete controls, a \"Read Aloud Now\" button, a daily voice cap (default 2) that never suppresses a near-due or overdue bill, and anti-spoof rules so only verified official senders can create a reminder. OTP silence is unchanged. This release **adds `1` permission** — `RECEIVE_BOOT_COMPLETED`, so the daily bill check survives a reboot; no new data collection, and bills stay in the app's private storage. Because `RECEIVE_BOOT_COMPLETED` is a normal (non-dangerous) permission, the Live Guard permission-delta gate was not triggered and no device-install record was required; the exact candidate bytes were published directly after full CI unit / lint / regression / signature / release-asset checks. **Post-publication device confirmation (24 October 2026):** the owner installed `SMS-Guard-v1.1.5.apk` on their phone and reports it works, with the new Bills (பில்கள்) tab present and the About screen showing version `1.1.5`; one phone only, and the screenshots show no APK SHA-256, so the installed bytes are not hash-matched to the published asset. The complete asset ledger is in [`docs/published-release-digests.md`](docs/published-release-digests.md).

## Previous publication — Live Guard 2.67 solo (23 October 2026)

| App | Gradle module | Version / code | Release tag | APK |
|---|---|---:|---|---|
| Live Guard / CleanSweep | `cleaner` | 2.67 / 72 | `live-guard-v2.67` | `CleanSweep-v2.67.apk` |

Live Guard 2.67 / code 72 was published **alone** on 23 October 2026 by [Build APK run 39990781045](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39990781045) (`CleanSweep-v2.67.apk`, `14,330,071` bytes, SHA-256 `ea8eedd2ddb13d5200347da2db580a45ee8fb628634b798183aaf5765650adf4`), leaving Ramesh Radio 1.22, PulseEQ 1.9, RP Vocal Remover Studio 0.1.15 and Secret Message 1.1.3 untouched (the "one app at a time" rule: only Live Guard's version changed, so only its tag became public). It decodes HTML/XML character references in RSS/Atom news headlines (`&amp;`, `&#39;`, `&rsquo;`, `&mdash;`, `&hellip;`, `&nbsp;`, `&eacute` …) to their real characters in the News Strip and spoken bulletin (Tamil + English), with a single-pass decoder (`&amp;lt;` stays `&lt;`, never over-decodes), entity decoding after tag-stripping (literal `<`/`>` preserved), NBSP/Unicode-space collapse, and unknown/out-of-range references left untouched; adds `8` unit tests. The run added `0` new permissions over `v2.66`; the permission-delta gate compared the freshly built APK against the previously published `live-guard-v2.66` asset with `aapt2`, found no dangerous-permission addition, and therefore required no device-install record — the exact candidate bytes were published directly after full CI localization / manifest / regression / signature / release-asset checks without a pre-publication phone install. `releases/latest-tag.txt` advanced to `live-guard-v2.67` for the next permission delta. The complete asset ledger is in [`docs/published-release-digests.md`](docs/published-release-digests.md).

## Latest publication — six app updates (21 October 2026)

| App | Gradle module | Version / code | Release tag | APK |
|---|---|---:|---|---|
| Live Guard / CleanSweep | `cleaner` | 2.57 / 62 | `live-guard-v2.57` | `CleanSweep-v2.57.apk` |
| Ramesh Radio | `radio` | 1.16 / 16 | `ramesh-radio-v1.16` | `Ramesh-Radio-v1.16.apk` |
| PulseEQ | `equalizer` | 1.8 / 9 | `pulseeq-v1.8` | `PulseEQ-v1.8.apk` |
| RP Vocal Remover Studio | `vocalremover` | 0.1.14 / 15 | `rp-vocal-remover-studio-v0.1.14` | `RP-Vocal-Remover-Studio-v0.1.14.apk` |
| Oor Arivippu | `oorarivippu` | 1.1.2 / 7 | `oor-arivippu-v1.1.2` | `Oor-Arivippu-v1.1.2.apk` |
| Secret Message | `secretmessage/wifi-test` | 1.1.2 / 4 | `secret-message-v1.1.2` | `Secret-Message-v1.1.2.apk` |

Live Guard 2.57 / code 62 was published alone on 21 October 2026 by [Build APK run 39585872882](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39585872882) (`CleanSweep-v2.57.apk`, `13,898,564` bytes, SHA-256 `203548d3dfe3c7b2a04751c70e6192235d2252c81374f1c093fd327731a2d168`), following 2.56 / code 61 earlier in the same session, leaving all other five apps untouched. Earlier on 21 October 2026, the four batch apps (Live Guard 2.55 / code 60, Ramesh Radio 1.16 / code 16, PulseEQ 1.8 / code 9, RP Vocal Remover Studio 0.1.14 / 15) were published by [Build APK run 39544037445](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39544037445); Oor Arivippu 1.1.2 by its standalone [run 39547449350](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39547449350) and Secret Message 1.1.2 by its standalone [run 39547449434](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39547449434), both triggered by the publish markers in the PR #12 merge commit on `main` (workflow dispatch is not available to the Arena integration, which returns 403). Each run passed its build, unit/regression, APK and post-publication asset checks; no app added an Android permission, and the exact APKs were not installed on a phone before publication. All current versions are published on `RP-Android-Apps` only; the older `InternetRadio` releases remain unchanged. The complete asset ledger is in [`docs/published-release-digests.md`](docs/published-release-digests.md).

Live Guard 2.54 / code 59 was published earlier the same day, alone, by [run 39526559599](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39526559599); its owner device report (OPPO A3s and Redmi 13 5G, Play Protect “This app looks safe”, not hash-matched to the public asset) is recorded in the ledger.

## Previous four-app batch — 20 October 2026

| App | Gradle module | Version / code at end of 20 October | Release tag | APK |
|---|---|---:|---|---|
| Live Guard / CleanSweep | `cleaner` | 2.53 / 58 | `live-guard-v2.53` | `CleanSweep-v2.53.apk` |
| Ramesh Radio | `radio` | 1.15 / 15 | `ramesh-radio-v1.15` | `Ramesh-Radio-v1.15.apk` |
| PulseEQ | `equalizer` | 1.7 / 8 | `pulseeq-v1.7` | `PulseEQ-v1.7.apk` |
| RP Vocal Remover Studio | `vocalremover` | 0.1.13 / 14 | `rp-vocal-remover-studio-v0.1.13` | `RP-Vocal-Remover-Studio-v0.1.13.apk` |

The 20 October four-app batch itself (Build APK [run 39355649275](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39355649275)) published Live Guard 2.50 / code 55, Ramesh Radio 1.13 / code 13, PulseEQ 1.7 / code 8 and RP Vocal Remover Studio 0.1.13 / code 14. Later solo releases that day advanced Live Guard to 2.53 and Ramesh Radio to 1.15; the table above records the end-of-day versions. Live Guard 2.50 was the first release that asked for Location, so its permission gate required a recorded physical-device install; [`docs/live-guard-2.50-device-evidence.md`](docs/live-guard-2.50-device-evidence.md) records the owner's install of the exact APK (`b4c183c8…6176`), and the published asset carries that same digest. PulseEQ 1.7 and Vocal Remover 0.1.13 contained no functional change; their versions moved because the batch publishes all four apps together and refuses to reuse a tag. Published asset sizes and digests are recorded in [`docs/published-release-digests.md`](docs/published-release-digests.md).

## Previous published release batch — 14 October 2026

| App | Gradle module | Version / code | Release tag | APK |
|---|---|---:|---|---|
| Live Guard / CleanSweep | `cleaner` | 2.46 / 51 | `live-guard-v2.46` | `CleanSweep-v2.46.apk` |
| Ramesh Radio | `radio` | 1.11 / 11 | `ramesh-radio-v1.11` | `Ramesh-Radio-v1.11.apk` |
| PulseEQ | `equalizer` | 1.5 / 6 | `pulseeq-v1.5` | `PulseEQ-v1.5.apk` |
| RP Vocal Remover Studio | `vocalremover` | 0.1.11 / 12 | `rp-vocal-remover-studio-v0.1.11` | `RP-Vocal-Remover-Studio-v0.1.11.apk` |

[Publication run 38521705254](https://github.com/universalrp2003/InternetRadio/actions/runs/38521705254) passed the builds, unit/regression suites, APK checks and dual-repository release verification. All four stable releases are published on both [`RP-Android-Apps`](https://github.com/universalrp2003/RP-Android-Apps/releases) and [`InternetRadio`](https://github.com/universalrp2003/InternetRadio/releases). The published asset digests—not build-only candidate hashes—are recorded in [`docs/published-release-digests.md`](docs/published-release-digests.md). The associated non-public build was [run 38523346072](https://github.com/universalrp2003/InternetRadio/actions/runs/38523346072). Its candidate digest record and earlier records remain in Git history; [`docs/candidate-build-digests.md`](docs/candidate-build-digests.md) is a rolling record replaced by each later candidate build.

The previously published releases, tags and assets for retired modules are retained as history. They are not built or included in new release batches.

## Oor Arivippu standalone stable release

Oor Arivippu is a fifth active product but remains intentionally excluded from the existing four-app batch. Its owner-authorized first stable release, version 1.0.0 / code 2 (`Oor-Arivippu-v1.0.0.apk`), was published separately through `.github/workflows/oorarivippu-release.yml` in [run 38748688974](https://github.com/universalrp2003/InternetRadio/actions/runs/38748688974). The [RP-Android-Apps release](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.0.0) and [InternetRadio fallback](https://github.com/universalrp2003/InternetRadio/releases/tag/oor-arivippu-v1.0.0) are public and unchanged.

Version 1.0.1 / code 3 (`Oor-Arivippu-v1.0.1.apk`, 12,195,397 bytes, SHA-256 `ec10c7369807748c9a5608f1a5c6a37eff5f8e1f7433cc16e984c75d8958457d`) was published the same way on 16 October 2026 in [run 38826899278](https://github.com/universalrp2003/InternetRadio/actions/runs/38826899278), after the Oor test workflow passed on the identical app source in [run 38826564762](https://github.com/universalrp2003/InternetRadio/actions/runs/38826564762). The current stable release, 1.0.2 / code 4 (`Oor-Arivippu-v1.0.2.apk`, 12,195,401 bytes, SHA-256 `bffdf37f525becc3a631c83988e56ca9881e620b76fd2e60bd7ce5b31efdd78b`), followed later that day in [run 38840264469](https://github.com/universalrp2003/InternetRadio/actions/runs/38840264469), after the Oor test workflow passed on the identical app source in [run 38840000191](https://github.com/universalrp2003/InternetRadio/actions/runs/38840000191); at the owner's request it was published without a device test. The [RP-Android-Apps release](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.0.2) and [InternetRadio fallback](https://github.com/universalrp2003/InternetRadio/releases/tag/oor-arivippu-v1.0.2) serve identical bytes, and the 1.0.1 releases are unchanged. Each new Oor release raises `versionCode` by one and the last `versionName` component, and updates the workflow's expected version/code, the About page's own release link with its verifier expectation, and `releases/oor-arivippu-v<version>.md` together.

The standalone workflow runs the Oor Arivippu release unit tests, release lint and signed release build; checks package/version, APK size, SHA-256 and the pinned release certificate; then stages identical APK bytes as drafts in both repositories. It verifies both draft assets before publishing the destination release and then the `InternetRadio` source fallback, and checks their canonical URLs and matching size/digest again. Publication requires the source repository's `RP_ANDROID_APPS_RELEASE_TOKEN` secret with destination-release write access.

Version 1.1.0 / code 5 (`Oor-Arivippu-v1.1.0.apk`, 12,211,781 bytes, SHA-256 `34858e8c6e4831e725ac7503f6613c96bcb896d659ebbdb588dcd139afc7c8b3`) was published the same way on 18 October 2026 in [run 39105539892](https://github.com/universalrp2003/InternetRadio/actions/runs/39105539892) from commit `fbdcc1b`, after Oor test [run 39105368624](https://github.com/universalrp2003/InternetRadio/actions/runs/39105368624) passed; at the owner's request it was published as soon as the automatic checks passed, without a device test.

The workflow is restricted to the authorized Arena branch (1.0.1, 1.0.2 and 1.1.0: `arena/d4fb2344-internetradio`). A commit containing `[publish-oorarivippu]` is the explicit publication trigger; do not use that marker until the Oor test workflow and required hosted checks have passed and publication is authorized. Ordinary commits do not publish. Never overwrite either repository's existing tag or reuse a version for changed APK bytes.

The push trigger ignores workflow-only edits, and GitHub does not start a path-filtered workflow for a push that changes no files, so an empty marker commit will not start a release. The 1.0.1 marker commit changed only `oorarivippu/README.md`, which is not packaged in the APK and does not start the legacy batch workflow. Oor commit messages must never contain the legacy batch's bare publish marker.

Earlier Oor Arivippu test APKs were debug-signed. They are not production updater builds and may not be updatable to the stable-signed APK; Android may require uninstalling a test APK first, which removes that installation's local settings and history. The owner reports successful two-device testing and that the stable app is working; screenshots show the v1.0.0 (code 2) package and running app, but do not show the APK SHA-256. For 1.0.1, the owner tested the debug APK from [run 38776902130](https://github.com/universalrp2003/InternetRadio/actions/runs/38776902130), built from the same app source before the version was raised, and after publication the debug APK of the exact 1.0.1 source from [run 38826564762](https://github.com/universalrp2003/InternetRadio/actions/runs/38826564762), which reported 1.0.1 (3); a debug-signed build cannot share the stable APK's bytes. Do not describe the exact published APK bytes as device-tested unless a hash is matched.

## Secret Message standalone release

Secret Message (`com.rpideas.secretmessage`) is a separate app, outside the four-app batch and the Oor workflow. At the owner's explicit request, version 1.0.0 / code 1 (`Secret-Message-v1.0.0.apk`, 29,345,617 bytes, SHA-256 `17ecabe0af808a48c790f631c6a069d39d107ff948fe99ed5e2ee32f164038f7`) was published on 17 October 2026 without an independent security review, which its release notes and About page say plainly. It was published by `.github/workflows/secret-message-wifi-test.yml` in [run 39052903146](https://github.com/universalrp2003/InternetRadio/actions/runs/39052903146) from source commit `8b7aee99ba975ef79543d97ef32aee49758fbb9d`. The [RP-Android-Apps release](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/secret-message-v1.0.0) and [InternetRadio fallback](https://github.com/universalrp2003/InternetRadio/releases/tag/secret-message-v1.0.0) serve identical bytes.

Version 1.1.0 / code 2 (`Secret-Message-v1.1.0.apk`, 29,443,977 bytes, SHA-256 `cc9a69d0022e49c1f653291e033bca8de0e1c4e0477731df1741cc3ddfbc5e10`) was published on 18 October 2026 by the same workflow in [run 39101002517](https://github.com/universalrp2003/InternetRadio/actions/runs/39101002517) from source commit `e928819`, after the owner tested the 1.1.0 test builds on two phones and asked to publish. The published bytes are identical to the phone-tested build of run 39098419129. Its release notes, like 1.0.0's, say plainly that the app has not been independently security-reviewed.

That workflow builds the four-ABI JNI library and the debug and unsigned release APKs and runs the unit tests. It signs the release APK with the project's own key (`secretmessage/signing/secretmessage-release.p12`, created once in CI and protected by the owner's `SECRETMESSAGE_SIGNING_PASSWORD` secret; certificate SHA-256 `58e370e417cf6f33234689acd5bf379474f5a8c55f79181d9e892a321bc04f08`). It checks the pinned certificate, package, version, debuggable flag and minimum SDK, and runs the emulator smoke on the signed APK through the live relay. Only then does a separate job stage drafts in both repositories, verify their size and SHA-256 (`secretmessage/wifi-test/verify-release-asset.py`), publish both and verify them again. Publication needs a push to `arena/d4fb2344-internetradio` whose commit message contains the Secret Message publish marker; that marker must not appear in any other commit message. Each new version raises `versionCode` by one and the last `versionName` part, updates `EXPECTED_CODE`/`EXPECTED_VERSION` in the workflow, and adds `secretmessage/releases/secret-message-v<version>.md`. Never overwrite a tag or replace the signing key.

## CI build

The GitHub Actions workflow `.github/workflows/build-apk.yml` builds the four legacy-batch debug and signed release APKs, runs unit and required CleanSweep regression tests, checks localization and source contracts, validates package/version/minimum SDK/permissions/icons/signatures, and verifies release asset size and SHA-256 against the local APK bytes. Oor Arivippu has its own test workflow and the standalone stable workflow described above.

The legacy batch workflow also produces Android App Bundles for `cleaner`, `radio` and `equalizer`. RP Vocal Remover Studio and Oor Arivippu are standalone direct-download APKs and have no AAB in that workflow.

Local commands (JDK 17, Android SDK and NDK required):

```sh
./gradlew :cleaner:testDebugUnitTest :cleaner:testReleaseUnitTest \
  :radio:testDebugUnitTest :radio:testReleaseUnitTest \
  :equalizer:testDebugUnitTest :equalizer:testReleaseUnitTest \
  :vocalremover:testDebugUnitTest :vocalremover:testReleaseUnitTest

./gradlew :cleaner:assembleRelease :radio:assembleRelease \
  :equalizer:assembleRelease :vocalremover:assembleRelease

./gradlew :oorarivippu:testDebugUnitTest :oorarivippu:testReleaseUnitTest \
  :oorarivippu:lintDebug :oorarivippu:lintRelease :oorarivippu:assembleRelease
```

## Verified GitHub Releases

1. Update the relevant module's `versionName` and `versionCode`; do not reuse a release version for changed APK bytes.
2. Update that app's notes in `releases/` and `CHANGELOG.md`, and refresh its current-version link in the root `README.md` table. State plainly whether those exact APK bytes were installed on a phone; never turn a CI pass into a device-test claim.
3. Push the verified source to the authorized branch. Candidate builds run on ordinary app-source pushes and do not publish. For publication, dispatch one selected app with `publish=true`, or use exactly one app-specific marker (`[publish-liveguard]`, `[publish-radio]`, `[publish-pulseeq]`, `[publish-vocalremover]`, `[publish-tamilanstore]`, `[publish-dailyvault]`, or the matching standalone app marker). Never combine app markers in one commit. Do not use the old generic `[publish]` batch marker.
4. The same-repository workflows publish with their `contents: write` workflow permission; no personal token is needed. If the workflow lacks permission or cannot reach the public repository, it must fail before making the release public. Never paste credentials into chat.
5. One-time bridge seeding is complete. Do not rerun migration mirroring for tags that already exist; the workflows intentionally refuse to overwrite a target tag.
6. Wait for all source, test, lint, permission, package, version, signer, note and asset checks for the selected app. Promotion occurs only after that app's draft/candidate checks pass; the workflow then verifies the public URL and SHA-256 digest.
7. Confirm each release is stable (not a draft or prerelease) and its canonical page contains the expected APK. Record the public asset's API-reported size and SHA-256 in [`docs/published-release-digests.md`](docs/published-release-digests.md), and update the root README's current-version table and latest-publication summary. The release workflow does not automatically rewrite the README; until this documentation change is merged into `main`, the GitHub repository homepage will continue to show its previous README.
8. Submit the release documentation to `main` through a pull request after its checks pass, then verify the repository homepage displays the updated version table. A README update on a development branch alone does not complete this step.

The four legacy batch releases (Live Guard 2.46, Ramesh Radio 1.11, PulseEQ 1.5 and RP Vocal Remover Studio 0.1.11) are published on both `universalrp2003/RP-Android-Apps` and `universalrp2003/InternetRadio`. Oor Arivippu 1.1.1 (current), 1.1.0, 1.0.2, 1.0.1 and 1.0.0 are also public on both repositories from separate standalone release runs; their matching sizes, digests and canonical URLs are recorded in [`docs/published-release-digests.md`](docs/published-release-digests.md). The four legacy apps remain in the original batch, and their one-to-two-month migration grace period began on 14 October 2026. Keep the old repository, historical release pages, tags and assets intact; do not rename or archive it before grace-period/device validation. See [`docs/repository-migration.md`](docs/repository-migration.md) for the current gate and sequence.

### Live Guard permission gate

The release workflow compares the merged CleanSweep APK manifest with the most recently published Live Guard release. A newly added dangerous permission requires a truthful physical-device evidence record that identifies the exact tested APK bytes. If an evidence record exists, only its matching bytes may be released. Missing or unreadable prior-release data fails closed. App-op/signature permissions are not treated as Android runtime-dangerous permissions.

No release process disables Play Protect or bypasses Android's installer. Users may need to grant the per-app **Install unknown apps** permission in Settings, and Android shows its own confirmation for each installation.

## Store metadata

The active store-art pipeline covers CleanSweep, Ramesh Radio and PulseEQ. F-Droid metadata is present for those three modules; it is preparation, not a claim that F-Droid has accepted or published them. RP Vocal Remover Studio and Oor Arivippu ship through their GitHub Releases only. See [`store/README.md`](store/README.md).


## Owner-approved three-app follow-up — 28 October 2026

SMS Guard 1.1.9/code 15 and Voice Notes 1.2/code 3 are already public; their assets are not replaced. Public metadata size/digests and workflow links are recorded in `docs/published-release-digests.md`. The owner reports both work on their phone, with screenshots; the friend's Redmi/MIUI issue remains unconfirmed. Website mirrors pin the new digests while keeping older downloads.

The historical owner review applied to Ramesh Radio 1.28, not the replacement-key 1.29 bytes. For the current 1.29 migration release, use the one-app `radio` dispatch; the candidate note explicitly says the new-key APK was not installed on a phone. The public 1.28 asset remains untouched until a distinct 1.29 release passes its own checks. SMS Guard and Voice Notes likewise require their own one-app release workflows; never use legacy multi-app commit markers.

[Three-app bilingual help, website and sister-app links](docs/three-app-help.md).
