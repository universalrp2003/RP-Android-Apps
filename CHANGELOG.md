# Changelog — RP Ideas Android apps

This file indexes all seven published **RP Ideas** Android applications at [`universalrp2003.github.io/RP-Android-Apps/`](https://universalrp2003.github.io/RP-Android-Apps/): six actively maintained utilities (**SMS Guard / குறுஞ்செய்தி காவலன்**, **Live Guard / CleanSweep**, **Ramesh Radio / ரமேஷ் ரேடியோ**, **PulseEQ**, **RP Vocal Remover Studio**, and **Oor Arivippu / ஊர் அறிவிப்பு**) plus the separately published **Secret Message / இரகசிய செய்தி** app, which has not been independently security-reviewed. App-specific details, installation notes and verified APK digests are linked below. Historical GitHub Releases, tags and assets remain available even when no longer represented in the active source tree.

## Published — Voice overhaul follow-ups: Live Guard 2.72, Ramesh Radio 1.25, SMS Guard 1.1.6 & Oor Arivippu 1.1.9 (25 October 2026)

Four apps published on 25 October 2026 from the `arena` branch, each by its own workflow: Live Guard
2.72 and Ramesh Radio 1.25 by [Build APK run 40183935239](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935239),
SMS Guard 1.1.6 by [SMS Guard run 40183935196](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935196),
and Oor Arivippu 1.1.9 by [Oor Arivippu run 40183935183](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935183).
`PulseEQ 1.9`, `RP Vocal Remover Studio 0.1.15` and `Secret Message 1.1.3` were already public at these
versions, so the batch step skipped them and their releases are unchanged.

- **Live Guard 2.72 / code 77** ([`releases/live-guard-v2.72.md`](releases/live-guard-v2.72.md), `CleanSweep-v2.72.apk`, `14,413,927` bytes, SHA-256 `e6664f5d14f7fe31a8a754b5d35bde62ba14d1b6ee894ba2b454f4ab7952fc0b`, [Build APK run 40183935239](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935239)): each silent-place slot can now be limited to chosen weekdays, so "Mon-Fri 9 AM-6 PM, Sat 10 AM-2 PM" is two slots instead of five repeated ones; a slot that names no days is impossible and the card no longer claims voice is paused when no slot covers today; a sustained HD-voice outage is visible on the voice screen (wait, then announce - never a quieter fallback voice and never a silent drop); and the dead system text-to-speech code that 2.71 made unreachable is removed. `0` new dangerous permissions over `v2.71` - the manifest is byte-identical, so the permission-delta gate required no device-install record.
- **Ramesh Radio 1.25 / code 25** ([`releases/ramesh-radio-v1.25.md`](releases/ramesh-radio-v1.25.md), `Ramesh-Radio-v1.25.apk`, `15,092,968` bytes, SHA-256 `49f7a4321e2e6facc20c2fccf0bec696a3cf9c7eab833afc3056cfafd3cec634`, [Build APK run 40183935239](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935239)): rain and cyclone voice alerts can point at more than one place - a row of chips under the weather banner, one tap to switch, the X forgets a place and promotes the next one, and every place picked from the search box is remembered. Up to 12 places, deduped by coordinates. `SharedWeatherRuntime` is deliberately unchanged, so Live Guard and Oor Arivippu see the new active place exactly as before. `0` new permissions over `v1.24`.
- **SMS Guard 1.1.6 / code 12** ([`releases/sms-guard-v1.1.6.md`](releases/sms-guard-v1.1.6.md), `SMS-Guard-v1.1.6.apk`, `12,428,897` bytes, SHA-256 `3e129c31dd1556fc4cedfdfcd36f50147fda2edb3963ee10575bb244b064b8b8`, [SMS Guard run 40183935196](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935196)): a monthly-spend card on the Finance tab - this month against last month, the change in rupees and percent (suppressed when last month had no spending), the running daily average, the projected month end, the days remaining and a plain-language trend. The month ends at the last instant of the calendar month, not midnight on the last day. OTP silence unchanged. `0` new permissions over `v1.1.5`.
- **Oor Arivippu 1.1.9 / code 14** ([`releases/oor-arivippu-v1.1.9.md`](releases/oor-arivippu-v1.1.9.md), `Oor-Arivippu-v1.1.9.apk`, `12,794,316` bytes, SHA-256 `6915d597faa0ca9c0665bab695863d02fa04f56675796babcd9ce6446e286679`, [Oor Arivippu run 40183935183](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935183)): the announcement list now records what actually happened to each announcement (spoken / held for a call / no internet / GPS off / no voice / not spoken) with a summary line, survives the process being killed, and its empty state says plainly that an empty list means the voice was not delivered. Voice previews are excluded. `0` new permissions over `v1.1.8`.

**No device test for any of the four (25 October 2026):** the exact published bytes of Live Guard 2.72, Ramesh Radio 1.25, SMS Guard 1.1.6 and Oor Arivippu 1.1.9 were **not installed on a phone** prior to GitHub release publication, and every release note says so. All four passed the full CI unit / lint / regression / signature / release-asset checks before publication.

## Unreleased

_No unreleased changes._

- **SMS Guard 1.1.6 / code 12** ([`releases/sms-guard-v1.1.6.md`](releases/sms-guard-v1.1.6.md)): a monthly-spend card on the Finance tab — this month versus last month, the change in rupees and percent (suppressed when last month had no spending), the running daily average, the projected month end, the days remaining, and a plain-language trend. The month ends at the last instant of the calendar month, not at midnight of the last day. `0` new permissions over `1.1.5`.
- **Ramesh Radio 1.25 / code 25** ([`releases/ramesh-radio-v1.25.md`](releases/ramesh-radio-v1.25.md)): more than one saved place for rain and cyclone voice alerts — a row of chips under the weather banner, one tap to switch, the active chip removes with the next place promoted. Up to 12 places, with dedupe by coordinates. `0` new permissions over `1.24`.
- **Oor Arivippu 1.1.9 / code 14** ([`releases/oor-arivippu-v1.1.9.md`](releases/oor-arivippu-v1.1.9.md)): the announcement list now records what actually happened to each announcement (spoken / held for a call / no internet / GPS off / no voice / not spoken) with a summary line, survives the process being killed, and its empty state explains that an empty list means the voice was not delivered. `0` new permissions over `1.1.8`.

## Published — Weather Accuracy & Privacy Update (`Live Guard 2.70`, `Ramesh Radio 1.24`, `Oor Arivippu 1.1.8`) (24 October 2026)

Coordinated weather-accuracy + privacy update across three apps, published from the `arena` branch by [Build APK run 40101594815](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40101594815) (Live Guard + Ramesh Radio) and [Oor Arivippu run 40098498419](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40098498419) (`PulseEQ 1.9`, `RP Vocal Remover Studio 0.1.15`, and `Secret Message 1.1.3` unchanged; exact candidate bytes were **not installed on a phone** prior to GitHub release publication — no dangerous-permission delta was introduced, so no device-install record is required):

- **Live Guard 2.70 / code 75** ([`releases/live-guard-v2.70.md`](releases/live-guard-v2.70.md), `CleanSweep-v2.70.apk`, `14,419,847` bytes, SHA-256 `8fc9f0a348a958f03c365148c4a8e012e63b85f939cf8eb71933040217beb52c`, [Build APK run 40101594815](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40101594815)): (1) the compact home-screen **widget's rain card now shows the real rain detail** — a "Falling now · X mm/hr" / "Up to X mm/hr in ~N min" brief (Tamil "இப்போது வருகிறது ·" / "~N நிமிடத்தில் X மிமீ/மணி வரை") driven by the observed current and peak 60-minute precipitation rate, instead of a generic quiet-hours note; (2) the shared **rain "starting now" classification now requires a *noticeable* rate** (`NOTICEABLE_RAIN_MM_HR = 0.40` mm/hr on current/peak 60-minute precipitation) across Live Guard, Ramesh Radio and Oor Arivippu, so a faint drizzle no longer announces "rain now". `0` new permissions over `v2.69`.
- **Ramesh Radio 1.24 / code 24** ([`releases/ramesh-radio-v1.24.md`](releases/ramesh-radio-v1.24.md), `Ramesh-Radio-v1.24.apk`, `15,076,580` bytes, SHA-256 `d3454873fc179241f37acd6e7acf5060418091c1ede6b640b14c32e5988b4749`, [Build APK run 40101594815](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40101594815)): **removed the `ACCESS_COARSE_LOCATION` (mobile-tower) location permission** and every location prompt/dialog; rain & cyclone voice alerts now use the saved PIN/Village with zero location access (also benefits from the shared noticeable-rain gate). Net `-1` permission over `v1.23`.
- **Oor Arivippu 1.1.8 / code 13** ([`releases/oor-arivippu-v1.1.8.md`](releases/oor-arivippu-v1.1.8.md), `Oor-Arivippu-v1.1.8.apk`, `12,777,932` bytes, SHA-256 `1e66733e85f0fbbe43e3f34c1f47fa0b1e90a92bdf1a55da0a3a28f10b1b4f77`, [Oor Arivippu run 40098498419](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40098498419)): About-screen sister-app links bumped to Live Guard `2.70`, Ramesh Radio `1.24` and Oor Arivippu `1.1.8` (also benefits from the shared noticeable-rain gate). `0` new permissions over `v1.1.7`.

**Post-publication device confirmation (24 October 2026):** the owner installed the published `CleanSweep-v2.70.apk`, `Ramesh-Radio-v1.24.apk`, `Oor-Arivippu-v1.1.8.apk` and `SMS-Guard-v1.1.5.apk` on their phone and confirmed all four work. The SMS Guard screenshots show version `1.1.5` but no APK SHA-256, so those installed bytes are not hash-matched to the published asset.

## Published — SMS Guard `1.1.5` Bill & Payment Due Reminders — New Bills Tab (24 October 2026)

- **SMS Guard 1.1.5 / code 11** ([`releases/sms-guard-v1.1.5.md`](releases/sms-guard-v1.1.5.md), `SMS-Guard-v1.1.5.apk`, `12,412,513` bytes, SHA-256 `021d86dd573668aaf4e4776327458ad99fef436fc0d5af3cdb8dcbc48705445d`, [SMS Guard run 40048301709](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40048301709)): credit-card, electricity (TNEB), internet/broadband, LPG gas, postpaid mobile, water, insurance and loan-EMI **bill SMS are captured automatically** into a dedicated **Bills (பில்கள்)** tab and are never counted as income/spend in the Finance ledger. You can add your own reminders (biller name, optional amount, due date via a date picker, category) with per-reminder 🔊 voice / 🔔 notification / ✅ mark-paid / 🗑 delete controls, a **\"Read Aloud Now\"** button, a master ON/OFF plus separate voice and notification switches, an adjustable daily maximum (1–5, default 2) and a configurable daily check time (default 9:00 AM). A near-due bill announces and notifies immediately on SMS arrival and again on the daily check; a bill **due within ~2 days or overdue always speaks aloud and posts a notification**, never suppressed by the daily cap. Anti-spoof by design: only verified official (non-promotional, non-personal) senders can create a reminder, payment receipts (\"your bill has been paid\") are ignored, and a random personal number can never manufacture a fake \"electricity due\" reminder. Voice respects the existing Sleep / Quiet Hours, master Voice switch and per-bill 🔊 toggle; the daily check re-arms across reboot and time/timezone changes. **OTP silence is unchanged** — one-time codes are never part of a bill reminder and are never spoken. Adds `1` permission (`RECEIVE_BOOT_COMPLETED`, so the daily bill check survives a reboot); bills are stored only in the app's private storage with no new data collection. The exact `1.1.5` build was published directly after full CI unit / lint / regression / signature / release-asset checks. **Post-publication device confirmation (24 October 2026):** the owner installed `SMS-Guard-v1.1.5.apk` on their phone and reports it works, with the new Bills (பில்கள்) tab present and the About screen showing version `1.1.5`; one phone only, and the screenshots show no APK SHA-256, so the installed bytes are not hash-matched to the published asset.

## Published — Ramesh Radio `1.23` Keep Playing With The Screen Off — In-App Background-Playback Guide (24 October 2026)

- **Ramesh Radio 1.23 / code 23** ([`releases/ramesh-radio-v1.23.md`](releases/ramesh-radio-v1.23.md), `Ramesh-Radio-v1.23.apk`, `15,076,612` bytes, SHA-256 `187d2479c952141d7ee1d2b3278fc3b5106fcf41b7a1011d0c6d04a864f4009b`, [Build APK run 40087097219](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40087097219)): adds an in-app bilingual (தமிழ் & English) **"keep playing in the background" guide** that appears when the phone's battery optimisation is stopping background playback (Xiaomi/MIUI/HyperOS, realme/Oppo/Vivo, Samsung One UI), with a one-tap **Open app settings / அமைப்புகளைத் திற** shortcut into Autostart / Battery / Recents-lock. The player already runs as a `mediaPlayback` foreground service with a network wake lock, so this adds `0` new permissions over `v1.22`; candidate bytes published directly after full CI localization / manifest / regression / signature / release-asset checks without a pre-publication phone install.

## Published — Live Guard `2.67` Readable News Headlines — HTML/XML Entity Decoding (23 October 2026)

- **Live Guard 2.67 / code 72** ([`releases/live-guard-v2.67.md`](releases/live-guard-v2.67.md), `CleanSweep-v2.67.apk`, `14,330,071` bytes, SHA-256 `ea8eedd2ddb13d5200347da2db580a45ee8fb628634b798183aaf5765650adf4`, [Build APK run 39990781045](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39990781045)): RSS/Atom news headlines that carry HTML/XML character references (`&amp;`, `&#39;`, `&quot;`, `&rsquo;`, `&ldquo;`/`&rdquo;`, `&mdash;`/`&ndash;`, `&hellip;`, `&nbsp;`, `&eacute;` …) now decode to their real characters in the News Strip and the spoken bulletin (Tamil + English) instead of showing literal "R&amp;D" / "It&#39;s". Decoding is a **single pass** (`&amp;lt;` stays `&lt;`, never over-decodes), runs **after** tag-stripping so literal `<`/`>` in comparison headlines like "5 < 10" survive intact, collapses NBSP/Unicode spaces to a normal space, and leaves unknown or out-of-range references untouched (fail-safe). Adds `8` unit tests. `0` new permissions over `v2.66` (no dangerous-permission delta, so no device-install record is required); the exact candidate bytes were published directly after full CI localization / manifest / regression / signature / release-asset checks without a pre-publication phone install.

## Published — Live Guard `2.66` Rich Rain Window, Cyclone/Heavy-Rain News Safety Lead & Cyber-Fraud Headlines (23 October 2026)

- **Live Guard 2.66 / code 71** ([`releases/live-guard-v2.66.md`](releases/live-guard-v2.66.md), `CleanSweep-v2.66.apk`, `14,328,023` bytes, SHA-256 `212fae456b37e8b4aaad9022f3b2d369ecbd8daf0cb1eabe45c4030834ace8da`, [Build APK run 39965508009](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39965508009)): (1) a data-driven **"Rain details" window** on the Rain & Storm screen (opened by tapping the rain notification) showing **current conditions** (temperature / cloud cover / wind gust / rain now), a **"Next precipitation"** 2-hour per-10-minute intensity chart with the peak rate, a plain **"rain will start in ~X min" / "Raining now" / "No rain in the next 2 hours"** headline, and an **active cyclone / storm banner** (name, Red/Orange level, distance, max wind) — with no radar-map dependency and no extra battery drain; (2) the spoken **news bulletin now opens with a bilingual cyclone / very-heavy-rain safety lead** for the user's location (identical wording to the standalone weather alert) on both the automatic screen-OFF bulletin and the manual **Read today's news**; (3) **latest Indian / Tamil-Nadu cyber-fraud headlines** (UPI & OTP scams, fake job offers, digital-arrest calls, etc.) added to the news strip and bulletin in Tamil and English. Adds `0` new permissions over `v2.65` (no dangerous-permission delta, so no device-install record is required); the exact candidate bytes were published directly after full CI localization / manifest / regression / signature / release-asset checks without a pre-publication phone install.

## Published — SMS Guard `1.1.4` Inbox-Sync Fix, Service-SMS Filter, Per-Sender Controls, Compose Contacts & Bank-Amount-Only Voice (23 October 2026)

- **SMS Guard 1.1.4 / code 10** (`releases/sms-guard-v1.1.4.md`, `SMS-Guard-v1.1.4.apk`, `12,379,633` bytes, SHA-256 `37b45394ce776c223b7f089bb1388c450829301ea21fe2b86ed18d29c616203d`, [release run 39949105537](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39949105537)): Real-phone-driven reliability + security update addressing six owner requests and a security audit — (1) **inbox-sync / phantom-notification fix** (notify only when the SMS actually persisted; auto-clear a conversation's notification on read/delete including read/deleted in another app; one retry on the provider insert for OEM flakiness); (2) **service-SMS filter** that silences routine operator data/validity/remaining-SMS dumps in voice + notifications while still storing them in the inbox, with real bank credit/debit always still announced; (3) **per-sender quick actions** (turn notifications off / don't announce aloud / block) from each conversation's ⋮ menu plus a Controls "Blocked & Muted Senders" restore card; (4) the **"all links verified" banner no longer appears on every app open** (only on a manual scan or a real threat); (5) **New SMS compose shows recent contacts inline and resolves the contact name** under the recipient field; (6) **bank voice announces the received/spent amount only — the available/closing balance is never spoken**. Security hardening: closed an OTP-detection gap (bare "your code is 482910" is now kept silent) and restricted the body-bank-name spoof surface to `-T` transactional headers. New unit tests cover the detector, RULE 7.5 silencing, balance suppression, the bare-code OTP silence, and the `-T`-trusted / `-S`-rejected spoof case. Full security catalogue: `smsguard/SECURITY.md`. `0` new permissions over `v1.1.3`.

## Published — Ramesh Radio `1.22` Opt-In Location Trust Polish (23 October 2026)

- **Ramesh Radio 1.22 / code 22** ([`releases/ramesh-radio-v1.22.md`](releases/ramesh-radio-v1.22.md), `Ramesh-Radio-v1.22.apk`, `15,076,612` bytes, SHA-256 `1b1a5d633141d3f278f54419cad3e120be8c103ec0a259661e9a34e649bcca71`, [Build APK run 39933357994](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39933357994)): Removed all automatic `ACCESS_COARSE_LOCATION` runtime prompts on app launch, dialog open, switch toggle, and **Check now** so users never see an unexpected Location dialog; made `📡 Enable Mobile Tower (Optional)` strictly opt-in when explicitly clicked inside **Rain & Cyclone Voice Alert**, with a transparent Tamil & English privacy card explaining Android's Mobile Signal Tower permission grouping and full 100% zero-permission support via saved PIN/Village (`0` permissions added; candidate bytes published directly after CI verification without pre-publication phone install).

## Published — Real-Phone Feedback & Polish Update (`SMS Guard 1.1.3`, `Live Guard 2.65`, `Ramesh Radio 1.21`) (23 October 2026)

Follow-up release addressing physical phone feedback across **SMS Guard**, **Live Guard**, and **Ramesh Radio** (`Oor Arivippu 1.1.7`, `PulseEQ 1.9`, `RP Vocal Remover Studio 0.1.15`, and `Secret Message 1.1.3` unchanged; exact candidate bytes were not installed on a physical phone prior to GitHub release publication):

- **SMS Guard 1.1.3 / code 9** ([`releases/sms-guard-v1.1.3.md`](releases/sms-guard-v1.1.3.md), `SMS-Guard-v1.1.3.apk`, `12,363,249` bytes, SHA-256 `536bafa0665db72783d09205c86ed44cad2dedfd60fd29580122170f6cdb7b3a`, [run 39916519051](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39916519051)): Instant voice auto-stop when turning ON the phone screen (`Intent.ACTION_SCREEN_ON`), compact global `⏹ Stop / நிறுத்து` button across every window whenever voice is playing, strictly newest-messages-at-top (`DESC`) ordering inside opened multi-message threads as well as Inbox/Finance/Shield histories, and default-ON full voice readout + Tamil/English department recognition for Government, Ration Shop (`TNEPDS`), School Education Exam Results (`TNSEDU`), Electricity/Gas/Water utilities, and non-OTP messages (`0` permissions added).
- **Live Guard 2.65 / code 70** ([`releases/live-guard-v2.65.md`](releases/live-guard-v2.65.md), `CleanSweep-v2.65.apk`, `14,312,367` bytes, SHA-256 `7d495ee3208631b930f38f48c5b82f7054f29dcc424a51a90019578543343634`, [Build APK run 39917424951](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39917424951)): News Headlines Voice Announcer enabled **ON by default** at **`3h` (`180` min)** interval (including one-time upgrade activation from `2.64`), `FlowRow` non-wrapping Tamil button layout fixes across `SettingsScreen`, `VoiceScreen`, `HomeScreen`, and `ReadingMovePad` (`← → ↑ ↓ · Auto · Drag`), concise non-duplicated Tamil descriptions, and full village + district voice announcements (`0` permissions added).
- **Ramesh Radio 1.21 / code 21** ([`releases/ramesh-radio-v1.21.md`](releases/ramesh-radio-v1.21.md), `Ramesh-Radio-v1.21.apk`, `15,076,612` bytes, SHA-256 `b5cd68048a05fb52de998401194eabf23c2da8695238bf737e452de520ab033d`, [Build APK run 39917424951](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39917424951)): Automatic runtime `ACCESS_COARSE_LOCATION` mobile-tower permission prompt + 1-tap button, multi-source cell-tower reverse geocoding (`Geocoder` + `Nominatim` + `BigDataCloud`), prominent active weather area card + checked-status place label, and full town/village + district details spoken in every rain/cyclone alert (`0` permissions added).

## Published — Four-App Feature & Reliability Update (`SMS Guard 1.1.2`, `Live Guard 2.64`, `Ramesh Radio 1.20`, `Oor Arivippu 1.1.7`) (23 October 2026)

Published across [SMS Guard run 39905821109](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39905821109), [Build APK run 39905821149](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39905821149), and [Oor Arivippu run 39905170667](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39905170667) (`PulseEQ 1.9`, `RP Vocal Remover Studio 0.1.15`, and `Secret Message 1.1.3` unchanged; exact candidate bytes were not installed on a physical phone prior to GitHub release publication):

- **SMS Guard 1.1.2 / code 8** ([`releases/sms-guard-v1.1.2.md`](releases/sms-guard-v1.1.2.md)): Edge-to-edge reply keyboard visibility (`imePadding` + `navigationBarsPadding` + auto-scroll), NEFT outgoing beneficiary credit classification + recipient transfer voice announcement, clean top `தமிழ் | ENG` language/voice sync button with auto HD Male/Female online & offline system voices, and new `Sender Name Only` + `Read Full Normal SMS` toggles.
- **Oor Arivippu 1.1.7 / code 12** ([`releases/oor-arivippu-v1.1.7.md`](releases/oor-arivippu-v1.1.7.md)): Redesigned ultra-clean Home screen with top `தமிழ் | ENG` + `⚙️ Settings` bar, big Start/Stop button, conditional active-only Rain Alert card, Current City card, live session Traveled Distance (`km`) box, Destination box, and automatic HD Male/Female voices.
- **Ramesh Radio 1.20 / code 20** ([`releases/ramesh-radio-v1.20.md`](releases/ramesh-radio-v1.20.md)): Mobile cell-tower approximate location (`ACCESS_COARSE_LOCATION`) + saved PIN fallback, strictly active-window/playing-only rain & storm alerts, and 20-minute forecast freshness validation across all three weather-enabled apps.
- **Live Guard 2.64 / code 69** ([`releases/live-guard-v2.64.md`](releases/live-guard-v2.64.md)): Smart Screen-OFF News Headlines Voice Announcer (`OFF/ON`, `30m/1h/2h/3h`, silent `10 PM–6 AM`, `CallGuard` + active-media guards, clean prefix/suffix/emoji sanitizer, zero duplicate headlines, plus manual `Read today's news` + `Stop reading`), full Tamil ↔ English news strip & voice synchronization, and 24×7 background rain alert freshness + notification auto-clear (`0` permissions added over `v2.63`).

## Published — Unified 7-App Super About Update (22 October 2026)

Upgrades all seven RP Ideas Android apps (`SMS Guard 1.1.1 / code 7`, `Live Guard 2.63 / code 68`,
`Ramesh Radio 1.19 / code 19`, `PulseEQ 1.9 / code 10`, `RP Vocal Remover Studio 0.1.15 / code 16`,
`Oor Arivippu 1.1.6 / code 11`, and `Secret Message 1.1.3 / code 5`) with unified bilingual Tamil
and English **About** pages containing direct one-tap links to the **RP-Android-Apps Official
Website (`universalrp2003.github.io/RP-Android-Apps/`)**, **RP-Android-Apps GitHub Repository**,
**Legacy Mirror Repository**, and **all 7 RP Ideas apps** (`0` permissions added across all 7 apps).
Verified public APK sizes and SHA-256 digests are recorded in [`docs/published-release-digests.md`](docs/published-release-digests.md).

## Published — Live Guard 2.57 (21 October 2026)

Published alone by Build APK
[run 39585872882](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39585872882)
from commit `8605122` on `arena/12471a95-rp-android-apps`. `CleanSweep-v2.57.apk` is `13,898,564`
bytes, SHA-256 `203548d3dfe3c7b2a04751c70e6192235d2252c81374f1c093fd327731a2d168`. No other app
version was bumped and no Android permission was added. The exact APK was not installed on a phone
before publication.

- **30-minute rain heads-up & zero daily spam:** built-in Open-Meteo `minutely_15` + official GDACS
  cyclone watch (plus optional Android-Keystore-encrypted personal Tomorrow.io 1-minute key) stays
  silent on normal days, warns once ~30 minutes before rain begins, respects Quiet Hours for normal
  rain, and locks until 90 continuous dry minutes pass.
- **24×7 severe storm & cyclone override:** escalations to very heavy rain or cyclonic gusts bypass
  Quiet Hours so dangerous storms announce day or night without skip (still respecting CallGuard).
- **Smart location + Oor Arivippu-style PIN/village picker:** automatic cached/quick GPS when phone
  GPS is ON; saved PIN/village fallback when GPS is OFF (`622301`, `alangudi 622301`) with district
  + state + PIN candidate selection. Shared engine lives in `shared/src/main/java/com/universalrp/weather/`
  ready for Oor Arivippu integration.
- Release notes: [`releases/live-guard-v2.57.md`](releases/live-guard-v2.57.md).

## Published — Live Guard 2.56 (21 October 2026)

Published alone by Build APK
[run 39580876173](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39580876173)
from commit `cd55415` on `arena/12471a95-rp-android-apps`. `CleanSweep-v2.56.apk` is `13,825,964`
bytes, SHA-256 `7bbb11a922860a8019e0a3cfa4fd7cc1538eaa35c4c9722754f2e08049f1dcdb`. No other app
version was bumped and no Android permission was added. The exact APK was not installed on a phone
before publication.

- **4G ⇄ 5G voice announcements:** wired **Announce 4G ⇄ 5G changes** directly to
  `Announcer.Event.RADIO_CHANGE` and master speech output (with a one-time upgrade sync for devices
  where the switch was already enabled), bound `RadioWatchService` to the active mobile-data SIM,
  and tracked both 5G SA and 5G NSA transitions.
- **Nearby cell towers:** filtered Android `CellInfo.UNAVAILABLE` (`2147483647`) and
  `CellInfo.UNAVAILABLE_LONG` (`9223372036854775807`) sentinels out of cell IDs, PCI, TAC/LAC and
  dBm values so hidden fields display cleanly instead of `2147483647`.
- Release notes: [`releases/live-guard-v2.56.md`](releases/live-guard-v2.56.md).

## Published — six app updates (21 October 2026)

Live Guard 2.55 / code 60, Ramesh Radio 1.16 / code 16, PulseEQ 1.8 / code 9 and
RP Vocal Remover Studio 0.1.14 / code 15 were published by Build APK
[run 39544037445](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39544037445);
Oor Arivippu 1.1.2 / code 7 by its standalone workflow in
[run 39547449350](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39547449350) and
Secret Message 1.1.2 / code 4 by its standalone workflow in
[run 39547449434](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39547449434).
Verified public asset sizes and SHA-256 values are in
[`docs/published-release-digests.md`](docs/published-release-digests.md). All six are published on
`RP-Android-Apps` only; no new Android permission was added, and the exact APKs were not installed
on a phone before publication.

- **All six apps:** a direct Guide button on the main screen opens the bilingual, expandable
  in-app guide.
- **Ramesh Radio 1.16:** local audio files can be seeked with a position bar while playing.
- **Oor Arivippu 1.1.2:** compact tap-to-open voice settings and a 0–100% speech-volume slider.
- Release notes: [`releases/live-guard-v2.55.md`](releases/live-guard-v2.55.md),
  [`releases/ramesh-radio-v1.16.md`](releases/ramesh-radio-v1.16.md),
  [`releases/pulseeq-v1.8.md`](releases/pulseeq-v1.8.md),
  [`releases/rp-vocal-remover-studio-v0.1.14.md`](releases/rp-vocal-remover-studio-v0.1.14.md),
  [`releases/oor-arivippu-v1.1.2.md`](releases/oor-arivippu-v1.1.2.md),
  [`secretmessage/releases/secret-message-v1.1.2.md`](secretmessage/releases/secret-message-v1.1.2.md).

## Published — Live Guard 2.54 (21 October 2026)

Published alone by Build APK [run 39526559599](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39526559599).
`CleanSweep-v2.54.apk` is 13,818,416 bytes, SHA-256
`b7de1d35845caa9f7e2a640c0868e7d32512a168a9aff021c4e4053c011af1b1`. The run passed the build,
required unit/regression suites, APK verification and post-publication asset checks. No new Android
permission was added. The owner reports 2.53 worked on an OPPO A3s (3 GB RAM) except for the
zero-entry issue, and that 2.54 now works on an OPPO A3s and Redmi 13 5G; Play Protect reported
“This app looks safe.” The supplied screenshots do not show the installed version or APK SHA-256,
so this post-publication report is not hash-matched to the public asset.

### Live Guard 2.54 — zero-data plans, bidirectional validity and opt-in daily watch

- **Zero data is an explicit choice.** The GB/MB amount dialog offers a one-tap **No data
  (0 GB)** action (typed zero is accepted too). This avoids relying on older numeric keyboards to
  enter zero. Choosing it records a voice-only plan and mirrors a genuine 0 GB quota, instead of
  leaving a stale allowance from an older plan. The booster dialog has its own **Remove booster
  (0 MB)** action.
- **Daily security-risk watch is opt-in.** New installs default to off. On the first 2.54 start,
  existing devices are also switched off once; later manual choices are preserved. The one-time
  **Run the daily check now** action remains available.
- **My plan dates work in either direction.** Pick a start date/time plus the number of days to
  calculate the end, or pick an end date/time plus the number of days to calculate the start.
  Both fields open the Android calendar followed by its time picker; the other date updates from
  the inclusive validity. An explicitly selected end time overrides the carrier default and can be
  cleared to restore the Jio/Airtel/Vi rule. Existing 2.53 plans load unchanged.
- Added policy tests for zero, inclusive reverse date calculation, and end-date/time overrides.
  Version 2.54 / code 59; no new permission.
- Release notes: [`releases/live-guard-v2.54.md`](releases/live-guard-v2.54.md).

### Published — Live Guard 2.53 (20 October 2026)

Released alone by Build APK run 39428035172. `CleanSweep-v2.53.apk` is 13,815,072 bytes, SHA-256
`7d640e044ad62bf5841c38d18a741c8e8ccf06dfe9d0fea2b45e14ddc21104f4`. No device install is claimed
for these bytes.

### Live Guard 2.53 — the volume, the guide, the white box, and three widgets

Owner's list after 2.52, 20 October 2026: the volume slider, *Guide* opening the wrong page, a
status-bar icon that flickers with "a white box", update text taking too much of the first page,
phone age on Home, the assistant as a small symbol, and the widget redesign.

- **One announcement-volume control.** New `AnnouncementVolumePanel` / `AnnouncementVolumeDialog`:
  the Home speaker opens the slider over the page (Done returns without navigating), Settings has
  the same card, and the plan card's private copy of the slider was replaced by it. Saved on
  release; Test speaks a sample sentence at the chosen level.
- **Guide is the guide.** `Screen.GUIDE` added; the menu's Guide row used to call
  `Screen.FEATURE_SETUP` (the permission checklist). Reviewed feature setup keeps its own row.
- **One status-bar icon.** The data watch notification used `R.mipmap.ic_launcher`, which Android
  renders as a white square because only the alpha channel is used; it now uses
  `R.drawable.ic_stat_data`.
- **Update card on Home is one row** (`LiveGuardUpdateCard(compact = true)`), with the full text,
  switch, notes and install steps in its dialog. The About screen keeps the full card.
- **Phone age on Home**, under the storage bar; **assistant is a small symbol** that opens the
  assistant window. The large AssistantCard and its inline question box left Home.
- **Three widgets, three jobs**, still 2×2 with the same View cycle: **Battery & plan**, **Plan &
  days** (data left, days left, today), **Security** (the saved permission review only — no scan).
  Class names, presets and manifest receivers unchanged so existing widgets update in place.
- 12 new Tamil strings. Version 2.53 / code 58. No new permission.
- See [`releases/live-guard-v2.53.md`](releases/live-guard-v2.53.md).

### Published — Live Guard 2.52 (20 October 2026)

Released alone by Build APK run 39419657235. `CleanSweep-v2.52.apk` is 13,811,808 bytes, SHA-256
`19ba3d6c5922062add5ae9480e0b433ffa83be2225e317741d862c7a4118171b`. No device install is claimed
for these bytes.

### Live Guard 2.52 — a short first page, a readable menu, Tamil typography

Owner verdict on 2.51, 20 October 2026: *"i am not satisfied the new design"* — and, asked which
part, *"all above"*: the Home page, the side menu, the themes and the Tamil text.

- **Home is three things now.** The hero card carries free storage, a used/total bar and a
  Battery · RAM · Signal chip row, with **Scan & clean junk** inside the same card. The plan card
  follows. Everything else lives under three tap-to-open section headers — **Cleaning tools**,
  **Phone, network & security**, **Assistant & setup** — each of which remembers whether it was
  open. The readings matrix, the per-kind tiles, the battery/heat line, the status-pill controls,
  the history shortcut, the shortcut grid, the assistant and the feature-setup card are all still
  there, one tap away instead of in the scroll path.
- **Side menu rebuilt.** Banner header, group headings with a rule, icon tile + one-line hint on
  every row, an accent rail on the current screen, and language/theme chips in the footer.
- **Tamil typography.** `CleanSweepTheme` now supplies a language-aware `Typography`: in Tamil every
  style gets a 1.62× line box, zero extra letter spacing and slightly larger glyphs; English keeps
  Material's defaults. `UiMode` is the Compose-observable mirror of `Lang.current`, updated at
  launch and by `setLanguage`, so the switch re-renders immediately.
- **Themes carry further.** `PanelCard` gained an optional colour (an open section is one step
  lighter), and the menu banner and hero use `HeroGradient`, the palette's two signature colours.
- Version 2.52 / code 57. No new permission.

### Published — Ramesh Radio 1.15 (20 October 2026)

Released alone by Build APK run 39415226287. `Ramesh-Radio-v1.15.apk` is 14,543,201 bytes, SHA-256
`b7ef2321f12faaf48f0d21b514a586bc6c906821d672a8f9459e55a706903ce5`. It supersedes 1.14 and carries
its update-card fix as well. No device install is claimed.

### Ramesh Radio 1.15 — the first screen shows stations, not paragraphs

Owner priority for the next Radio pass: *"home page the unnecessary text occupying the space
redesign it … put in drop down text … mostly english ui is looking good only tamil ui the letters
and size placement making the look bad."*

- **Categories** open with the six that matter (தமிழ், Tamil FM, Tamil News, Towns, Favourites,
  Recent); the genre/archive chips live behind **More ▾ / Less ▴**, expanded in place.
- **Header** subtitle is one line and ellipsised, so a long word cannot push the data pill away.
- **Empty state** leads with the action; the explanation is one short line under it.
- **Tamil:** the data pill follows the chosen language ("2.4 GB இன்று" / "2.4 GB Today") instead of
  always printing Tamil; bottom-bar labels are one line, centred and unwrapped so **செய்திகள்** and
  **பாடல்கள்** are not clipped.
- Version 1.15 / code 15. Widget chooser and full restyle remain for 1.16.

### Published — Ramesh Radio 1.14 (20 October 2026)

Released alone by Build APK run 39413564341. `Ramesh-Radio-v1.14.apk` is 14,526,817 bytes, SHA-256
`d3372cbf5c0bcc4a11b260c49d437541fd9a901fd2593dfc61f8da9e4bbe238a` (GitHub's digest of the published
asset). The first attempt (run 39412645425) failed in the Live Guard permission gate, which ran even
though Live Guard was already public; the gate is now scoped to Live Guard publishes. No device
install is claimed for these bytes.

### Ramesh Radio 1.14 — the update card, a quieter first screen and Tamil wording

Owner report, 20 October 2026, with phone screenshots of the More tab: the update block was a wall
of near-black text between two cards, with an empty band under it. Same message: the first screen
carried too much text, "Local" was translated in a way that read like a local radio station, and
English looks fine while Tamil placement does not.

- **Fixed — the update section is a card.** `AppUpdates.Control` now has a title, one short line,
  explicit Material colours and a collapsed **How updates are verified** row. The long explanation
  no longer fills the tab; the switch, the check button and the download/install flow are unchanged.
- **First screen quieter.** The search box is one line until tapped (the live-directory button sits
  at its end and still works from the collapsed row); the sorting explanation is folded into
  **How this list is sorted**.
- **Tamil wording.** `Local` → **பாடல்கள்**, `Local audio` → **என் பாடல்கள்**; the tab no longer
  reads as a local radio station. Tamil added for the search hint, the sorting row and the update
  explanations (shared catalog, 489 entries).
- Version 1.14 / code 14. The shared sister-app list and the Oor Arivippu About links now point at
  `ramesh-radio-v1.14`.

### Published — Live Guard 2.51 (20 October 2026)

Released alone, under the owner's one-app-at-a-time rule, by Build APK [run 39407712230](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39407712230)
from commit `821aa52`. `CleanSweep-v2.51.apk` is 13,793,884 bytes, SHA-256
`260120452041fcbe9d67aa76bd8c71cc1da78a40b2d57cf235a20a387e650ec5` (GitHub's digest of the published
asset). The other five apps kept their existing releases. No device install is claimed for these
bytes; see [`docs/published-release-digests.md`](docs/published-release-digests.md).

### Live Guard 2.51 — themes, a subject menu and "fill my plan from the SMS"

Owner request, 20 October 2026: *"we need themes to choose same color are boring"*, *"change the UI
for side sweep to go other menu organise intelligently important items in first page not very long
scrolling page"*, and (earlier) *"add option to paste recharge sms to fill automatically"*.

- **New — seven colour themes.** Midnight (the old blue, still the default), Saffron, Evergreen,
  Violet, Slate, Black (OLED) and Rose, each with a Tamil name. The choice is saved and applied
  before the first frame. Every screen already read its colours through `Theme.kt`, so the same
  eleven names now resolve against the selected palette — no screen was touched to add a theme,
  and the Material scheme (switches, fields, dialogs) is rebuilt from the same palette.
- **New — side menu, grouped by subject.** Swipe from the left edge anywhere, or tap the menu
  button where the logo used to be. Cleaning · Phone · Network & data (signal, towers, Wi-Fi, My
  plan) · Security · Assistant · Live Guard. Nothing was removed from the app; the screens are
  grouped and reachable in one tap.
- **Home order is by importance.** Storage, then **Scan & clean junk**, then the plan and alerts,
  then help and analysis. About moved into the menu. Three small section titles separate the
  groups, so the long list reads as sections instead of one pile.
- **New — fill My plan from the operator's SMS, without the SMS permission.** The user copies the
  message and pastes it; `RechargeSmsParser` reads the operator, the data allowance, daily vs
  total, the validity days or date and the price. Nothing is saved until **Use these values** is
  tapped, and a message that states no plan (an OTP, a balance reminder) is refused rather than
  half-filled. Ten unit tests pin the shapes the owner receives and the figures that must not be
  guessed; "unlimited 5G" is shown but never turned into an unlimited total.
- **Fixed — a theme switch repaints immediately.** The palette is Compose snapshot state, and
  AppRoot keys its tree on the saved theme, so screens that observe no other state still rebuild.

### Live Guard 2.51 — the booster is a second pool (the 1.05 GB / 4.3 GB disagreement)

Owner question, 20 October 2026: *"Now new problem its counting correctly? I will share
screenshot see"* — My plan said 1.05 GB left of 6 GB while the Airtel app said 4.3 GB left.

Both numbers were right about different pools. Android measured 4.95 GB in the pack window; of
that, 3.25 GB fell inside the 3 GB booster's three days (17–19 Oct). Airtel's app shows only the
base pack once the booster is spent, so it reported 1.7 GB used / 4.3 GB left.

- **Fixed — bytes measured inside the booster's own date range are charged to the booster.** With
  the owner's numbers Live Guard now shows 1.70 GB used and 4.3 GB left of 6 GB, and says
  *"3.25 GB of the measured data was used during the booster's days, so it is not counted against
  this pack."* While a booster is running its bytes stay inside the combined allowance, which is
  also what the operator shows at that time.
- `PlanPolicy.usageAgainstAllowance` is the single rule; two tests pin the owner's exact numbers.
- **Fixed — one rounding rule.** My plan printed "1.1 GB left" while the notification printed
  "1.05 GB" for the same pack; both now use `formatBytesSafe`.
- **New — Valid till.** The date the operator app prints ("Valid Till 10 Dec 2026"). Typing it
  derives the day count, which is how the 84-vs-77 slip on the ₹489 pack cannot happen again:
  25 Sep → 10 Dec is 77 days, not 84.
- **Workflow — only changed apps are published.** The four-app batch used to bump every version
  together, so a Live Guard fix pushed a meaningless update onto PulseEQ and Vocal Remover. The
  publish step now filters to versions whose release does not exist yet, skips the rest, and
  exits cleanly when there is nothing new.

### Live Guard 2.51 — the pack ends when the operator says, and a booster has dates

Owner request, 20 October 2026: *"some provider plan expire exact time of recharge see jio, if we
recharge today 5 pm the plan will expire same after expiry date 5pm, some not like airtel so get
time input also do research and implement best one"* — and, from the same message, the booster
that was "expired yesterday but i forget to turn off".

**What the research says** (kept as comments in `PlanPolicy` so it cannot drift):

| Operator | When the pack really ends |
|---|---|
| Jio | Counts from the recharge moment: 63 days recharged at 5:00 pm ends at 5:00 pm on the last day; a "1 day" data pack is 24 hours, not "until midnight". |
| Airtel | At midnight — its published terms say *"All validities expire at midnight unless otherwise specified"*, so a 1-day pack bought at 11:50 pm dies nine minutes later. |
| Vi / BSNL | Same as Airtel. |

- **New — a recharge time in My plan.** One button opens a small dialog: type `5:00`, pick am/pm,
  done. The time is shown in twelve-hour form everywhere (the app never displays a 24-hour clock).
- **New — the operator, and how it ends the pack.** Four chips (Jio, Airtel, Vi, BSNL) set the
  rule automatically, and the rule can be overridden: *Same time as recharge* or *End of the last
  day*. The screen says which one is in force and why.
- **The summary is exact.** "Ends 2026-12-10, 5:00 pm · Ends today" appears in My plan, and the
  Mobile screen's My plan card now carries the same ending instead of a bare date. The countdown
  is minute-accurate: a pack that dies at 5:00 pm today no longer pretends the day is intact.
- **Unknown operator → the earlier ending.** If no operator is chosen the app assumes the
  recharge time. Warning a few hours early is harmless; warning after the SIM has stopped working
  is not. With no time at all the app keeps the whole last day rather than inventing an hour.
- **New — booster first and last day.** Set both (or use the *Started today* / *Ends today* chips).
  A booster whose last day has passed **stops adding to the allowance even when its switch is
  still on** — the owner's "9.0 GB" was a 6 GB pack plus a forgotten 3 GB booster, so the plan now
  reports 6 GB again and the card says the booster ended on that date. A booster that has not
  started yet does not count either.
- The mirrored pack limit is now repaired in both directions (up when a booster is added, down
  when it expires), but only while the mirror still holds a value this store wrote — a quota set
  by hand is never overwritten.
- Seven new unit tests cover the Jio/Airtel arithmetic, the unknown-operator default, the typed
  time formats, and the expired/not-yet-started booster.

### Release batch 2.50 / 1.13 / 1.7 / 0.1.13 — published 20 October 2026

The four-app batch publishes Live Guard, Ramesh Radio, PulseEQ and RP Vocal Remover Studio
together and refuses to reuse an existing tag, so all four versions move even though only two
apps changed:

| App | Version / code | Change |
|---|---:|---|
| Live Guard (CleanSweep) | 2.50 / 55 | nearby cell towers, one pack number everywhere |
| Ramesh Radio | 1.13 / 13 | last station on launch, compact search row |
| PulseEQ | 1.7 / 8 | no functional change; version raised for the batch |
| RP Vocal Remover Studio | 0.1.13 / 14 | no functional change; version raised for the batch |

Publication: [Build APK run 39355649275](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39355649275).
2.50 adds `ACCESS_COARSE_LOCATION`, so the owner's 12 October 2026 rule required a recorded
physical-device install first: he installed `CleanSweep-v2.50.apk`
(`b4c183c8…6176`) over 2.49 with Play Protect on, Play Protect reported "This app looks safe",
the installer completed the update, and the nearby-cell list then showed four cells with
PCI/TAC/dBm. The published APK carries that same digest, so the released bytes are the installed
ones ([`docs/live-guard-2.50-device-evidence.md`](docs/live-guard-2.50-device-evidence.md)). One
phone only: no second device was tested and the Play Store bundle was not installed anywhere.
Published sizes and digests: Live Guard 13,740,236 / `b4c183c8…6176`; Ramesh Radio 14,526,813 /
`d763d585…92d1`; PulseEQ 11,567,368 / `a75c411d…1c93`; Vocal Remover 63,745,096 / `8f597a7f…e8b9`.

### Live Guard 2.50 — nearby cell towers, and one pack number everywhere (published)

Owner request, 20 October 2026: the Mobile screen said *"No cell tower details: Android only
shares them with Location allowed"* and offered no way to fix it.

- **New — the nearby-cell list actually works.** Android refuses `CellInfo` (the serving cell
  and its neighbours, with PCI/TAC and dBm) to any app without **Precise** location, on every
  API level. `ACCESS_FINE_LOCATION` was declared with `maxSdkVersion="32"`, so from Android 13
  the permission could never be granted at all — that is why the list stayed empty on the
  owner's phone. It is now declared for every version, `ACCESS_COARSE_LOCATION` is declared
  beside it (Android 12+ ignores a request for Precise on its own), and the Mobile screen has a
  **Nearby cell towers** card with its own *Allow location and rescan* button.
- The card says what is and is not happening: location is read only while the Mobile screen
  builds a reading, never in the background, and nothing is uploaded. If the user chose
  *Approximate*, the card says that is not enough for tower ids and points at the app settings.
- The list is sorted the way "nearby" reads: the serving cell first, then the strongest
  neighbours, up to 16. An empty list with location allowed is reported as the phone's or
  carrier's own limit, not as a missing permission.
- The Wi-Fi screen no longer asks for location on Android 13+ (Nearby-devices is enough
  there); the Phone permission button asks for Phone only, so each prompt has one reason.
- **Published only after a recorded device install.** `ACCESS_COARSE_LOCATION` is a dangerous
  permission 2.49 did not ask for, so the release gate required a physical-device record:
  `docs/live-guard-2.50-device-evidence.md` holds the owner's install of the exact APK
  (`b4c183c8…6176`, run 39353706282), including what it does not prove.

### Live Guard — one pack number everywhere (shipped in 2.50)

Owner-reported from the phone on 19 October 2026: My plan said **3.5 GB left of 7.5 GB**
while the Live Guard notification and pill said **488 MB left of 4.5 GB** for the same
recharge, and the 4G/5G card said **3.53 GB left** next to a **488 MB** total remainder.

- **Fixed — the booster is now part of the quota every screen uses.** `PlanStore.save`
  mirrored only `plan.totalMb` to the older screens, while My plan's Today card used
  `PlanPolicy.effectiveTotalMb` (base + booster on a total-only plan). The mirror now stores
  the effective total, so My plan, the ongoing card, the floating pill, the widget and the
  insights screen divide by the same amount. `PlanStore.ensureMirror` repairs the value the
  first 2.49 build wrote, and only while that mirror still equals the old base value, so a
  pack limit the owner re-synced by hand is never overwritten.
- **Fixed — the 4G/5G card and watch no longer show two "left" figures.** The split counters
  start when sampling starts, so subtracting them from the whole pack ignored everything
  used before that and contradicted the measured pack line. The card and the ongoing watch
  now show `4G … used · 5G … used` (plus `5G Unlimited` when that policy is on); the pack
  remainder stays in Live Guard / My plan, where it is measured.
- **Fixed — an unrelated My plan edit can no longer undo a manual reset.** `PlanStore.save`
  compared the start date with the tracker's window, so after a reset any later booster or
  mode change moved the window back and silently re-counted the old usage. It now compares
  with the start date the plan itself last saved.
- The older Home **Set Pack Size** dialog writes through `PlanStore` when a plan exists, so
  it cannot leave My plan on a different total.
- The Live Guard guide now explains My plan and the pill colours in Tamil and English: below
  75% used is normal, 75–90% is amber, 90% and above is red and flashing, and that warning
  follows the number the user entered, not a carrier message.
- Version name/code are unchanged in this source tree (`2.49` / `54`); a release that
  changes the APK bytes still needs its own version bump and device test.

### Ramesh Radio 1.13 — last station on launch, compact search row (published)

Owner request, 20 October 2026.

- **New — opening the app starts the station you played last.** The resume happens once per
  app process, only when nothing is already queued or playing, so a widget or notification
  session is never restarted (and never paused by a duplicate play). **More** has a switch,
  *Start the last station when the app opens*, saved in the app's own preferences and ON by
  default. The "Continue listening" card and `resumeLast()` behave as before.
- **Fixed — the search row is one line tall again.** The station-list action
  ("நிலையப்பட்டியல்") was a labelled chip inside the text field, so it took roughly half the
  width and the Tamil hint wrapped onto three lines, doubling the field's height. It is now a
  compact icon square beside a full-width field, with the hint pinned to a single ellipsised
  line; the same action still opens the live directory, and the section row keeps its
  "Live directory" chip.
- The Radio guide explains both in Tamil and English.

### Guide in every app (Tamil + English)

- **New — the same compact guide in each app.** Before this, only Live Guard and Secret Message
  had help text, and Oor Arivippu only had folded "Details" notes; Ramesh Radio, PulseEQ and
  RP Vocal Remover Studio had none at all. Every app's About screen now opens a guide written
  for that app, in Tamil and English, as short titles that open when tapped — the owner's
  "click and expand" shape. The text lives in one file,
  `shared/src/main/java/com/universalrp/updates/AppGuides.kt`, so a step that changes on a phone
  is corrected once for all five legacy-batch apps; the screen itself is
  `GuideScreen.kt` in the same package. Secret Message keeps its own longer 1.1.0 text in its
  own build (it is a separate Gradle project) and now uses the same tap-to-open list.
- The guide never promises what the app does not do: PulseEQ states plainly that only some
  phones let one app tune other apps' sound, the radio says a live stream cannot be dragged
  forward, Vocal Remover says the seek bar is not there yet, and Secret Message repeats that
  it is not independently security-reviewed.

### Release train v2026.10.19 — guide in every app, published

The guide reached all six apps, but four of them were still carrying the version number that was
already on the release page, so nothing could be published: the batch workflow refuses to reuse a
version for changed APK bytes. This train gives every changed app a new number and its own release
notes.

- **PulseEQ 1.6 (code 7)** — the guide on About; no audio-processing change.
- **RP Vocal Remover Studio 0.1.12 (code 13)** — the guide on About; no processing change.
- **Oor Arivippu 1.1.1 (code 6)** — the guide in its About dialog; voice behaviour unchanged.
- **Secret Message 1.1.1 (code 3)** — the guide, and the narrow-screen layout fix it needed.
- **Ramesh Radio 1.12 (code 12)** and **Live Guard 2.49 (code 54)** were already new versions.
- Release notes for all six live in `releases/` (and `secretmessage/releases/`), in English and
  Tamil, and the Live Guard note states plainly that its exact bytes were not installed on a
  phone during this run.

### Live Guard 2.49 — My plan

- **New — one screen for the whole recharge.** "What is my plan?" used to be answered in three
  places (a pack-size dialog, a daily chip row, a start/expiry screen), and none of them could
  express what the operators actually sell. **Mobile → My plan** now asks the questions in the
  order a person thinks:
  1. *What does this recharge give?* — **No data** (voice only), **Daily** (GB every day) or
     **Total** (whole pack).
  2. *How much?* — preset chips plus **Custom …** everywhere, and the custom box accepts **GB or
     MB** (0.75 GB, 500 MB, 1.2 GB). A typed "1.5 GB" is understood even when MB is selected.
  3. *How long?* — start date and a number of days (28 / 84 / 365 as shortcuts), with the last
     day shown and a warning when the pack has ended.
  4. *Is there a booster?* — on/off, any amount in GB or MB, and **first / after** the daily
     data. The phone cannot see which order the carrier applied, so the app follows the order
     the user states, and says so.
  5. *Is 5G free?* — Auto / On / Off, with the reason in plain words.
- **The arithmetic is a pure policy with tests.** `PlanPolicy` (new) does the amounts, the
  inclusive expiry, the days left and the booster share; `PlanPolicyTest` covers 13 cases
  (1536 MB from "1.5 GB", rejection of 0 / negative / typo'd / absurd values, a booster that is
  spent first versus after the daily allowance, an unlimited total, a voice-only plan).
  `PlanStore` is the only storage, and it mirrors the older keys so the summary card, the widget
  and the insights screen cannot disagree.
- **Fixed — the 4G / 5G Auto / On / Off buttons were unreadable.** All three looked identical on
  the dark theme because only the font weight changed. The chosen one is now filled, outlined and
  marked with a tick.
- **Fixed — the split counters no longer look wrong.** "4G 0.89 MB used" next to "Mobile data
  today 395 MB" was confusing, because the split can only begin when sampling begins. The card
  now says **"Counted since 4:28 PM — earlier usage today is in the mobile total above."**
- Nothing on the new screen resets a counter, and the footer still says the carrier's billing is
  invisible to the phone: this follows what the user enters, it is not a carrier balance.

### Live Guard 2.48

- **New — a daily 4G allowance that looks after itself.** Indian prepaid plans sell data by
  the day (1.5 GB, 2 GB, 3 GB), but the app could only track a whole recharge. Mobile → *Daily 4G
  limit* now offers those three sizes plus **Off**; the card shows how much is left **today**
  with a progress bar that turns red near the end. Nothing is stored per day: "today" is
  already measured from local midnight, so the count returns to zero every morning by
  construction — no reset button, no midnight job that can be missed, no drift after a
  reboot or a time-zone change.
- **New — nothing is spoken while you are on a call.** A call — ringing, dialling, talking or
  a screened call — now outranks every other voice gate, including a manual test. The check
  is made at the instant of speaking (no listener, so no stale flag can either talk over a
  call or silence the app afterwards): `AudioManager` mode covers in-call, VoIP and ringing,
  and `TelephonyManager` call state covers phones that keep the audio mode at normal until
  pickup. The voice screen reports "Blocked: phone call in progress" so the reason is visible.
  Oor Arivippu already queued its announcements until the call ended; Ramesh Radio, PulseEQ,
  Vocal Remover Studio and Secret Message have no speech path at all, so Live Guard and Oor
  Arivippu are now the complete set.
- Release tooling: `verify_cleaner_next_update_source.py` pins 2.48 / code 53, and the
  prepared-test count is 368 across 42 suites in `verify_cleaner_regressions.py`.

### Ramesh Radio 1.12

- **Fix — crash when opening the live station list (நேரடி நிலையப் பட்டியல்).** The directory
  can return the same stream URL twice (relays, the same station under two names). The list
  used the URL as its Compose key, and a duplicate key throws
  `IllegalArgumentException: Key <url> was already used`, which closed the app. That is why
  it only happened *sometimes*: it depended on the answer the directory happened to send.
  `DirectoryClient` now de-duplicates by URL, the visible list is de-duplicated too, and the
  directory list key is `url + name` so a future duplicate cannot take the app down again.
- **New — draggable seek bar in Now Playing.** Recorded audio (local files) seeks anywhere in
  the file; live radio shows a LIVE pill and moves only inside the buffer, because a live
  stream has no "forward".
- About now lists **all six apps** from one shared list (`SisterApps`) — the old hard-coded
  four pointed at the `InternetRadio` repository and left out Oor Arivippu and Secret Message.

### Live Guard 2.47

- **New — 4G and 5G counted separately.** Android keeps one mobile byte counter, so the app
  samples it every 3 seconds and files each delta under the radio that was active. A new
  `RadioWatchService` (foreground, `dataSync`, started only when the feature is on) does this
  and sends the ongoing notification "4G x left · 5G Unlimited".
- **New — 4G ⇄ 5G announcements**, Tamil and English, with their own switch and a 20-second
  minimum gap so a flapping modem cannot narrate every hop. Wi-Fi ⇄ mobile stays a separate
  switch.
- **New — in-app volume slider** for everything the app speaks (0–100 %), applied per
  utterance with `KEY_PARAM_VOLUME`; the phone's own volume is never changed. The old "Louder
  voice" switch still works and is used as the default when the slider has never been moved.
- **New — 5G-unlimited switch (Auto / On / Off)** with the operator's real terms: Jio needs
  ₹349+ **and** 2 GB/day; Airtel needs an eligible ₹349+ pack, the one-time claim in the
  Airtel Thanks app, and is capped at 300 GB per 28-day cycle. The card shows the reason.
- New permission: `FOREGROUND_SERVICE_DATA_SYNC` (Android 14+ requires the service type to be
  declared). `READ_PHONE_STATE` was already granted.
- About now lists all six apps from the shared `SisterApps` list.

### All apps

- One shared list of the six apps (`shared/.../SisterApps.kt`). Oor Arivippu and Secret
  Message already showed six; Live Guard, Ramesh Radio, PulseEQ and Vocal Remover showed
  four with stale `InternetRadio` links.

## Oor Arivippu 1.0.2 — 16 October 2026

- Place and district announcements speak only the place names. The repeated filler is gone: English no longer says “Entering”, and Tamil no longer says “தற்போது … ஊருக்குள் வந்துள்ளீர்கள்” or “… பகுதிக்கு வந்துள்ளீர்கள்” (for example “Alangudi.” / “ஆலங்குடி.”, and “Pudukkottai district.” / “புதுக்கோட்டை மாவட்டம்.”). The Tamil next-area form with “அடுத்த பகுதி” is unchanged, as are announcement timing, offline suppression and reminders, Plus Code filtering and the voice choices. No permissions changed. See [`releases/oor-arivippu-v1.0.2.md`](releases/oor-arivippu-v1.0.2.md).
- Published by standalone [GitHub Actions run 38840264469](https://github.com/universalrp2003/InternetRadio/actions/runs/38840264469) from commit `50bd12b2a2b205d5adfce7bb70b47df23b2ccc16`, after the Oor test workflow passed on the identical app source in [run 38840000191](https://github.com/universalrp2003/InternetRadio/actions/runs/38840000191). The workflow and a separate public-API check confirmed that both repositories serve the same stable APK (`Oor-Arivippu-v1.0.2.apk`, 12,195,401 bytes, SHA-256 `bffdf37f525becc3a631c83988e56ca9881e620b76fd2e60bd7ce5b31efdd78b`) at canonical release and download URLs. The 1.0.0 and 1.0.1 releases and assets are unchanged.
- At the owner's request, 1.0.2 was published without a device test; no build of the 1.0.2 source has been installed on a phone.

## Oor Arivippu 1.0.1 — 16 October 2026

- Oor Arivippu filters Plus Codes and coordinate-only labels, compares normalized place names before speaking, prefers named landmarks such as hospitals/bus stands, and offers up to four Android TTS voices per language where available. Network voices are identified; offline reminders continue to use a local voice or beep. No permissions changed. See [`releases/oor-arivippu-v1.0.1.md`](releases/oor-arivippu-v1.0.1.md).
- Published by standalone [GitHub Actions run 38826899278](https://github.com/universalrp2003/InternetRadio/actions/runs/38826899278) from commit `6f14f65c7cb54ab48c1a3460bab73fb75178951c`, after the Oor test workflow passed on the identical app source in [run 38826564762](https://github.com/universalrp2003/InternetRadio/actions/runs/38826564762). The workflow and a separate public-API check confirmed that both repositories serve the same stable APK (`Oor-Arivippu-v1.0.1.apk`, 12,195,397 bytes, SHA-256 `ec10c7369807748c9a5608f1a5c6a37eff5f8e1f7433cc16e984c75d8958457d`) at canonical release and download URLs. The 1.0.0 releases and assets are unchanged.
- The owner device-tested the debug APK from [run 38776902130](https://github.com/universalrp2003/InternetRadio/actions/runs/38776902130), built from the same app source before the version was raised, and authorized this release. That build reported 1.0.0. After publication, the owner also installed the debug APK of the exact 1.0.1 source from Oor test [run 38826564762](https://github.com/universalrp2003/InternetRadio/actions/runs/38826564762); it reported 1.0.1 (3), passed Play Protect's “This app looks safe” check and showed no newer release, and the owner reports it works. Both test builds were debug-signed; the exact 1.0.1 stable bytes were not device-tested.

## Release-train snapshot — 19 October 2026

All six apps were published on 19 October 2026 from the release-train source. The four-app batch
was built and published by Build APK [run 39272003076](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39272003076),
Oor Arivippu 1.1.1 by its standalone [run 39268547086](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39268547086),
and Secret Message 1.1.1 by [run 39268577963](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39268577963).
Each publisher re-read its own public release and verified the asset name, size and SHA-256 after promotion.

| App | Version | Release notes | RP-Android-Apps | InternetRadio fallback |
|---|---:|---|---|---|
| Live Guard (CleanSweep) | 2.49 / code 54 | [`releases/live-guard-v2.49.md`](releases/live-guard-v2.49.md) | [live-guard-v2.49](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/live-guard-v2.49) | — (not mirrored; 2.46 remains) |
| Ramesh Radio | 1.12 / code 12 | [`releases/ramesh-radio-v1.12.md`](releases/ramesh-radio-v1.12.md) | [ramesh-radio-v1.12](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/ramesh-radio-v1.12) | — (not mirrored; 1.11 remains) |
| PulseEQ | 1.6 / code 7 | [`releases/pulseeq-v1.6.md`](releases/pulseeq-v1.6.md) | [pulseeq-v1.6](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/pulseeq-v1.6) | — (not mirrored; 1.5 remains) |
| RP Vocal Remover Studio | 0.1.12 / code 13 | [`releases/rp-vocal-remover-studio-v0.1.12.md`](releases/rp-vocal-remover-studio-v0.1.12.md) | [rp-vocal-remover-studio-v0.1.12](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/rp-vocal-remover-studio-v0.1.12) | — (not mirrored; 0.1.11 remains) |
| Oor Arivippu | 1.1.1 / code 6 | [`releases/oor-arivippu-v1.1.1.md`](releases/oor-arivippu-v1.1.1.md) | [oor-arivippu-v1.1.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.1.1) | — (not mirrored; 1.1.0 and older remain) |
| Secret Message | 1.1.1 / code 3 | [`secretmessage/releases/secret-message-v1.1.1.md`](secretmessage/releases/secret-message-v1.1.1.md) | [secret-message-v1.1.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/secret-message-v1.1.1) | — (not mirrored; 1.1.0 and 1.0.0 remain) |

The owner reports that he downloaded and installed all six updates on his phone and that they are
working. That is an owner report from the published release pages: it does not hash-match the
installed bytes to the published digests, and it is not Play Protect approval. The Live Guard 2.49
permission-delta gate found no newly added dangerous runtime permission, so no new physical-device
evidence record was required for publication.

## Published releases — 16 October 2026 snapshot

| App | Version | Release notes | RP-Android-Apps | InternetRadio fallback |
|---|---:|---|---|---|
| Oor Arivippu | 1.0.2 / code 4 | [`releases/oor-arivippu-v1.0.2.md`](releases/oor-arivippu-v1.0.2.md) | [oor-arivippu-v1.0.2](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.0.2) | [oor-arivippu-v1.0.2](https://github.com/universalrp2003/InternetRadio/releases/tag/oor-arivippu-v1.0.2) |
| Live Guard (CleanSweep) | 2.46 / code 51 | [`releases/live-guard-v2.46.md`](releases/live-guard-v2.46.md) | [live-guard-v2.46](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/live-guard-v2.46) | [live-guard-v2.46](https://github.com/universalrp2003/InternetRadio/releases/tag/live-guard-v2.46) |
| Ramesh Radio | 1.11 / code 11 | [`releases/ramesh-radio-v1.11.md`](releases/ramesh-radio-v1.11.md) | [ramesh-radio-v1.11](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/ramesh-radio-v1.11) | [ramesh-radio-v1.11](https://github.com/universalrp2003/InternetRadio/releases/tag/ramesh-radio-v1.11) |
| PulseEQ | 1.5 / code 6 | [`releases/pulseeq-v1.5.md`](releases/pulseeq-v1.5.md) | [pulseeq-v1.5](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/pulseeq-v1.5) | [pulseeq-v1.5](https://github.com/universalrp2003/InternetRadio/releases/tag/pulseeq-v1.5) |
| RP Vocal Remover Studio | 0.1.11 / code 12 | [`releases/rp-vocal-remover-studio-v0.1.11.md`](releases/rp-vocal-remover-studio-v0.1.11.md) | [rp-vocal-remover-studio-v0.1.11](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/rp-vocal-remover-studio-v0.1.11) | [rp-vocal-remover-studio-v0.1.11](https://github.com/universalrp2003/InternetRadio/releases/tag/rp-vocal-remover-studio-v0.1.11) |

GitHub Actions [run 38521705254](https://github.com/universalrp2003/InternetRadio/actions/runs/38521705254) passed the four-app build, unit/regression tests, APK and signature checks, and dual-repository publication. Both repositories now serve the four stable APKs. A live public-API check confirmed the destination releases are non-draft/non-prerelease, use canonical release and download URLs, are newer than the bridge versions, and have exactly matching asset sizes and SHA-256 values on both repositories. Owner-provided phone screenshots show update-available notices for all four bridge versions, confirming device-side metadata discovery. They do not show a completed download/install of these exact bytes or identify the supplying repository. The updater source order and selection logic prefer `RP-Android-Apps`. See [`docs/published-release-digests.md`](docs/published-release-digests.md). The build-only hashes in [`docs/candidate-build-digests.md`](docs/candidate-build-digests.md) are from a different run and must not be treated as the published asset digests.

Oor Arivippu's first stable release, 1.0.0 / code 2 ([`releases/oor-arivippu-v1.0.0.md`](releases/oor-arivippu-v1.0.0.md)), was then published separately, outside that four-app batch, by standalone [GitHub Actions run 38748688974](https://github.com/universalrp2003/InternetRadio/actions/runs/38748688974). The public API confirmed that both repositories serve the same stable APK (`Oor-Arivippu-v1.0.0.apk`, 12,179,013 bytes, SHA-256 `b438e0dbe76c0de975a4ce35bf126b8825cd5af0976fe3db93a11ae8b6f6794d`) at canonical release and download URLs. The owner reports successful two-device testing and that the app is working. Supplied screenshots show the v1.0.0 (code 2) package and the app running at v1.0.0, but do not provide the APK SHA-256 or link the installed bytes to the published digest.

## Previous stable bridge releases — 14 October 2026

| App | Version | Release notes | GitHub Release |
|---|---:|---|---|
| Live Guard (CleanSweep) | 2.45 / code 50 | [`releases/live-guard-v2.45.md`](releases/live-guard-v2.45.md) | [live-guard-v2.45](https://github.com/universalrp2003/InternetRadio/releases/tag/live-guard-v2.45) |
| Ramesh Radio | 1.10 / code 10 | [`releases/ramesh-radio-v1.10.md`](releases/ramesh-radio-v1.10.md) | [ramesh-radio-v1.10](https://github.com/universalrp2003/InternetRadio/releases/tag/ramesh-radio-v1.10) |
| PulseEQ | 1.4 / code 5 | [`releases/pulseeq-v1.4.md`](releases/pulseeq-v1.4.md) | [pulseeq-v1.4](https://github.com/universalrp2003/InternetRadio/releases/tag/pulseeq-v1.4) |
| RP Vocal Remover Studio | 0.1.10 / code 11 | [`releases/rp-vocal-remover-studio-v0.1.10.md`](releases/rp-vocal-remover-studio-v0.1.10.md) | [rp-vocal-remover-studio-v0.1.10](https://github.com/universalrp2003/InternetRadio/releases/tag/rp-vocal-remover-studio-v0.1.10) |

Publication run **38501139771** built and verified the four bridge APKs and published them on the old `InternetRadio` endpoint so existing installations could discover an increasing-version update. Those earlier release pages, tags and assets remain available and were not overwritten by the new batch.

The Live Guard permission-delta check for 2.45 found no newly added dangerous permission, so no physical-device evidence record was required. The exact CI-generated bridge APK bytes were not installed on a phone during that publication; no exact-build device test is claimed. CI and release validation are not Play Protect approval or an all-device compatibility claim. On 14 October, the owner reported the bridge apps were working and manual update checks completed without errors. The subsequent 2.46 / 1.11 / 1.5 / 0.1.11 batch has now seeded `RP-Android-Apps`; API/source-selection verification is recorded above and in the migration ledger. On-device retrieval/installation of those latest exact bytes remains a separate grace-period check.

Full APK filenames, byte sizes and SHA-256 digests for the current and previous releases are in [`docs/published-release-digests.md`](docs/published-release-digests.md).

## Previous stable releases — 13 October 2026

| App | Version | Release notes | GitHub Release |
|---|---:|---|---|
| Live Guard (CleanSweep) | 2.44 / code 49 | [`releases/live-guard-v2.44.md`](releases/live-guard-v2.44.md) | [live-guard-v2.44](https://github.com/universalrp2003/InternetRadio/releases/tag/live-guard-v2.44) |
| Ramesh Radio | 1.9 / code 9 | [`releases/ramesh-radio-v1.9.md`](releases/ramesh-radio-v1.9.md) | [ramesh-radio-v1.9](https://github.com/universalrp2003/InternetRadio/releases/tag/ramesh-radio-v1.9) |
| PulseEQ | 1.3 / code 4 | [`releases/pulseeq-v1.3.md`](releases/pulseeq-v1.3.md) | [pulseeq-v1.3](https://github.com/universalrp2003/InternetRadio/releases/tag/pulseeq-v1.3) |
| RP Vocal Remover Studio | 0.1.9 / code 10 | [`releases/rp-vocal-remover-studio-v0.1.9.md`](releases/rp-vocal-remover-studio-v0.1.9.md) | [rp-vocal-remover-studio-v0.1.9](https://github.com/universalrp2003/InternetRadio/releases/tag/rp-vocal-remover-studio-v0.1.9) |

CI publication run **38380663566** built and verified those releases, including CleanSweep regression tests, package/version/signature checks and public-asset size/SHA-256 verification. The Live Guard permission-delta check found no newly added dangerous permission relative to 2.43. The exact Live Guard CI release APK was not installed during that run; CI is not a phone test or Play Protect approval. The owner separately reported installing all four then-current apps and that they worked on their phone on 13 October 2026; that report is not an all-device compatibility claim. Their release pages, tags, APK assets and original SHA-256 records remain unchanged as historical releases.

## Recent Live Guard history

- **2.44 / code 49:** added update verification, safer APK hand-off and contributor/release links. The permission-delta gate found no new dangerous permission relative to 2.43. See [`releases/live-guard-v2.44.md`](releases/live-guard-v2.44.md).
- **2.43 / code 48:** corrected charging measurements so unsupported zero current is unavailable rather than a false zero-watt reading. See [`releases/v2026.10.12-3.md`](releases/v2026.10.12-3.md).
- **2.42 / code 47:** device-health, voice-selection and monitoring refinements; associated device evidence and scope are linked from the release notes. See [`releases/v2026.10.12-2.md`](releases/v2026.10.12-2.md).
- **2.41 / code 46:** honest charging readings, a local 44 °C warning, and bounded AI output/provider checks. The temperature warning is not an automatic pause threshold. See [`releases/v2026.10.12-1.md`](releases/v2026.10.12-1.md).
- **2.40 / code 45:** removed notification/message-reading capability from the app. See [`releases/v2026.10.11-4.md`](releases/v2026.10.11-4.md) and the [Play Protect installation record](docs/live-guard-play-protect-install-block.md).

Older release notes and device-test records remain in `releases/` and `docs/` as historical records. They describe the scope and test process at the time of those versions and are not current feature or build instructions.

## Project scope

The current source, Gradle build and CI cover five active apps: Live Guard, Ramesh Radio, PulseEQ, RP Vocal Remover Studio and Oor Arivippu. The existing four-app batch workflow remains separate from Oor Arivippu's standalone release workflow; store-art preparation currently covers the three apps with store metadata. Releases for retired modules are preserved on GitHub as history and are not part of current builds or future release batches. The repository slug stays unchanged until the staged updater migration is complete; see [`docs/repository-migration.md`](docs/repository-migration.md).
