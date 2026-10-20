# Changelog — RP Ideas Android apps

This file indexes the six actively maintained apps. App-specific details, installation notes and verified APK digests are linked below. Historical GitHub Releases, tags and assets remain available even when no longer represented in the active source tree.

## Unreleased

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

## Latest published releases — as of 19 October 2026

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
