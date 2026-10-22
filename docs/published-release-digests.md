# Verified published Android APK digests

The four-app legacy stable batch was built, verified and published by GitHub Actions run **38521705254**, from source commit `838122db61ef7c177977f04595af0f4a1452bb8a`. That workflow checked package, version, minimum SDK, signing certificate, local SHA-256 and release-asset metadata before promoting the four APKs to `universalrp2003/RP-Android-Apps` and `universalrp2003/InternetRadio`; Oor Arivippu was intentionally outside that batch.

Oor Arivippu 1.0.0 / code 2 was published separately by the standalone stable workflow in [run 38748688974](https://github.com/universalrp2003/InternetRadio/actions/runs/38748688974), from source commit `921038e68e1c9d440f52c696816bc24882ac06fc`. The run passed release unit tests and lint, checked the package/version/minimum SDK and pinned signer, then verified identical public stable APK assets, sizes, SHA-256 digests and canonical URLs in both repositories. This standalone publication did not change the four-app batch.

Oor Arivippu 1.0.1 / code 3 was published on 16 October 2026 by the same standalone workflow in [run 38826899278](https://github.com/universalrp2003/InternetRadio/actions/runs/38826899278), from source commit `6f14f65c7cb54ab48c1a3460bab73fb75178951c` (app source identical to the candidate that passed the Oor test workflow in [run 38826564762](https://github.com/universalrp2003/InternetRadio/actions/runs/38826564762)). The run repeated the release tests, lint, package/version/minimum-SDK and pinned-signer checks, then verified identical draft and public assets in both repositories. A separate post-publication API check confirmed both releases are public, stable and canonical with identical size/digest, and that the 1.0.0 releases and assets are unchanged.

Oor Arivippu 1.0.2 / code 4 was published later on 16 October 2026 by the same standalone workflow in [run 38840264469](https://github.com/universalrp2003/InternetRadio/actions/runs/38840264469), from source commit `50bd12b2a2b205d5adfce7bb70b47df23b2ccc16` (app source identical to the candidate that passed the Oor test workflow in [run 38840000191](https://github.com/universalrp2003/InternetRadio/actions/runs/38840000191)). The run repeated the release tests, lint, package/version/minimum-SDK and pinned-signer checks, then verified identical draft and public assets in both repositories. A separate post-publication API check confirmed both releases are public, stable, marked latest and canonical with identical size/digest, and that the 1.0.0 and 1.0.1 releases and assets are unchanged.

Oor Arivippu 1.1.0 / code 5 was published on 18 October 2026 by the same standalone workflow in [run 39105539892](https://github.com/universalrp2003/InternetRadio/actions/runs/39105539892), from source commit `fbdcc1b`, after Oor test [run 39105368624](https://github.com/universalrp2003/InternetRadio/actions/runs/39105368624) passed on the same app source. At the owner's request it was published as soon as the automatic checks passed, without a device test. The run passed the release unit tests, lint, signed build and package/version/minimum-SDK/signature checks, then verified identical draft and public assets in both repositories. The 1.0.2 assets are unchanged.

Secret Message 1.0.0 / code 1 was published on 17 October 2026, at the owner's request and without an independent security review, by `.github/workflows/secret-message-wifi-test.yml` in [run 39052903146](https://github.com/universalrp2003/InternetRadio/actions/runs/39052903146), from source commit `8b7aee99ba975ef79543d97ef32aee49758fbb9d`. The run passed the unit tests, the four-ABI native build and the debug and release APK checks. It signed the release APK with the project's own key (certificate SHA-256 `58e370e417cf6f33234689acd5bf379474f5a8c55f79181d9e892a321bc04f08`), checked the pinned certificate, package, version, debuggable flag and minimum SDK, and passed the emulator smoke on the signed APK through the live relay. It then verified identical draft and public assets in both repositories. No build of the 1.0.0 source has been installed on a phone, so its digest below is not device-tested or hash-matched; the test builds that preceded it were. GitHub now shows it as the latest release of both repositories; the app updaters choose releases by their own asset names, so the other apps are unaffected, and the Oor Arivippu 1.0.2 assets are unchanged.

Secret Message 1.1.0 / code 2 was published on 18 October 2026 by `.github/workflows/secret-message-wifi-test.yml` in [run 39101002517](https://github.com/universalrp2003/InternetRadio/actions/runs/39101002517), from source commit `e928819`, at the owner's request after a two-phone test of the 1.1.0 test builds (Xiaomi 2406ERN9CI and motorola moto g57 power). The run passed 23 unit tests, the four-ABI native build, the signing and package checks and the emulator smoke on the signed APK through the live relay, then verified identical draft and public assets in both repositories. Its SHA-256 equals that of the phone-tested build from run 39098419129, so the published APK is the tested one. Like 1.0.0 it has not been independently security-reviewed. It is the latest release of both repositories; the other apps' updaters choose releases by their own asset names.

On 19 October 2026 that release train was completed: Live Guard 2.49 / code 54, Ramesh Radio 1.12 / code 12, PulseEQ 1.6 / code 7, RP Vocal Remover Studio 0.1.12 / code 13, Oor Arivippu 1.1.1 / code 6 and Secret Message 1.1.1 / code 3 were published from `main`. Every app gained the compact bilingual in-app guide; Live Guard also gained **My plan**, and Secret Message gained the guide plus a narrow-screen layout fix. The app versions that were already public (2.46, 1.11, 1.5, 0.1.11, 1.1.0, 1.1.0) kept their release pages and assets unchanged.

## Current active stable releases — as of 22 October 2026

On 21 October 2026, following the physical-device verification of **Live Guard 2.56 / code 61** ([run 39580876173](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39580876173), commit `cd55415`, `CleanSweep-v2.56.apk`, `13,825,964` bytes, SHA-256 `7bbb11a922860a8019e0a3cfa4fd7cc1538eaa35c4c9722754f2e08049f1dcdb`) and **Live Guard 2.57 / code 62** ([run 39585872882](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39585872882), commit `8605122`, `CleanSweep-v2.57.apk`, `13,898,564` bytes, SHA-256 `203548d3dfe3c7b2a04751c70e6192235d2252c81374f1c093fd327731a2d168`) on **OPPO A3s** and **Redmi 13 5G** (both scanned clean by Google Play Protect — **“This app looks safe”** — with live Airtel 4G/5G cell readings and `Alangudi · Rain expected within ~30 minutes` radar verification), three coordinated stable releases were published at the owner's direction:

- **Live Guard 2.59 / code 64** (`CleanSweep-v2.59.apk`, `13,953,820` bytes, SHA-256 `3668a0b5f1e7dd464a851f3d3224ca87157bbba7c4def38a90ea2935b8ef5b97`) and **Ramesh Radio 1.18 / code 18** (`Ramesh-Radio-v1.18.apk`, `14,641,505` bytes, SHA-256 `8dc0dcca9e19da1943af39119c05511c49b97ce58c98e30b927849c0f288362f`) were built, verified, and published together by Build APK [run 39606864576](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39606864576) from commit `0d87ea2`, and **Oor Arivippu 1.1.4 / code 9** (`Oor-Arivippu-v1.1.4.apk`, `12,342,853` bytes, SHA-256 `11d302800aa0598fc8e3183a29af5bf0e372bae0dc2b33648aab24b55405474f`) was published across both official repositories by `Oor Arivippu stable release` [run 39606864614](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39606864614) from commit `0d87ea2`. **All three APKs (`Live Guard 2.59`, `Ramesh Radio 1.18`, and `Oor Arivippu 1.1.4`) were installed and verified live on physical phone hardware by the owner on 21 October 2026 (`7:58–7:59 PM IST`, Alangudi `622301`)**, confirming accurate real-time `Tomorrow.io` radar agreement across all three apps (`No rain approaching right now — staying quiet` / `Clear / normal — staying silent` / `மழை அறிகுறி இல்லை – அமைதி`).
- **Live Guard 2.60 / code 65** (`CleanSweep-v2.60.apk`, `13,953,756` bytes, SHA-256 `403dbd9bd63b65b7c1117d84315938d597fedc07acb0bb42b8e0dec576f09212`) was then published alone by Build APK [run 39617943653](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39617943653) from commit `119af43` on `arena/12471a95-rp-android-apps` (`Ramesh Radio 1.18`, `PulseEQ 1.8`, `RP Vocal Remover Studio 0.1.14`, `Oor Arivippu 1.1.4`, and `Secret Message 1.1.2` were left untouched) to fix narrow-screen `FlowRow` button wrapping (`Check now` · `Test voice` · `Preview animation`) and full-width `Tomorrow.io` guide link stacking spotted in the Live Guard 2.59 device screenshots. **On 21 October 2026 (`8:16–8:21 PM IST`), the owner upgraded in-app to `CleanSweep-v2.60.apk`, confirmed Google Play Protect scanned clean (“This app looks safe”), and verified the fixed `Rain & cyclone alert` screen layout and launcher widget modes live on phone hardware.**
- **Live Guard 2.61 / code 66** (`CleanSweep-v2.61.apk`, `13,962,740` bytes, SHA-256 `e9321a64b7501aeb79df0943584978a4916f688ab57a6047350f665ccc32cf0e`) was published alone on 22 October 2026 by Build APK [run 39733591941](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39733591941) from commit `97b1cfc` on `arena/377705d8-rp-android-apps` (`Ramesh Radio 1.18`, `PulseEQ 1.8`, `RP Vocal Remover Studio 0.1.14`, `Oor Arivippu 1.1.4`, and `Secret Message 1.1.2` left untouched) to add the 4th **`Rain & storm` (`மழை & புயல்`)** `காட்சி` (`View`) mode across existing home widgets, the dedicated `CleanSweepWeatherWidget` `2×2` launcher preset, automatic urgent storm brief alerts across all widget modes, and per-mode `Open` (`திற`) deep-links. **On 22 October 2026 (`12:04–12:06 PM IST`), the owner upgraded in-app to `CleanSweep-v2.61.apk`, confirmed Google Play Protect scanned clean (“This app looks safe”), and verified the live `Rain & storm` home-screen widget (`30°C · ஆலங்குடி · No rain approaching · Wind 24 km/h · Tomorrow.io · 11:57 AM`) on phone hardware.** All 47 suites / 420 declarations, localization, `0` permission delta, and post-publication GitHub SHA-256 checks also passed.

| App | Version / code | RP-Android-Apps release | APK asset | Bytes | SHA-256 |
| --- | ---: | --- | --- | ---: | --- |
| SMS Guard (குறுஞ்செய்தி காவலன்) | 1.1.0 / 6 | [sms-guard-v1.1.0](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/sms-guard-v1.1.0) | `SMS-Guard-v1.1.0.apk` | 12,330,481 | `67567d1f39571edc918c7bf9a810c06e4729fb1b43c40226422230e0f051996c` |
| Live Guard (CleanSweep) | 2.61 / 66 | [live-guard-v2.61](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/live-guard-v2.61) | `CleanSweep-v2.61.apk` | 13,962,740 | `e9321a64b7501aeb79df0943584978a4916f688ab57a6047350f665ccc32cf0e` |
| Ramesh Radio | 1.18 / 18 | [ramesh-radio-v1.18](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/ramesh-radio-v1.18) | `Ramesh-Radio-v1.18.apk` | 14,641,505 | `8dc0dcca9e19da1943af39119c05511c49b97ce58c98e30b927849c0f288362f` |
| PulseEQ | 1.8 / 9 | [pulseeq-v1.8](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/pulseeq-v1.8) | `PulseEQ-v1.8.apk` | 11,567,368 | `6c20c94dd66d553c2498b3e17178e3821796f82ee2faeaaaa5b8934da9f189ed` |
| RP Vocal Remover Studio | 0.1.14 / 15 | [rp-vocal-remover-studio-v0.1.14](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/rp-vocal-remover-studio-v0.1.14) | `RP-Vocal-Remover-Studio-v0.1.14.apk` | 63,768,368 | `7a2bbd644bbffa09a8e4076f1ab59c5e59cfa3416e8cf88fa59fefc2ce0b8fc4` |
| Oor Arivippu | 1.1.4 / 9 | [oor-arivippu-v1.1.4](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.1.4) | `Oor-Arivippu-v1.1.4.apk` | 12,342,853 | `11d302800aa0598fc8e3183a29af5bf0e372bae0dc2b33648aab24b55405474f` |
| Secret Message | 1.1.2 / 4 | [secret-message-v1.1.2](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/secret-message-v1.1.2) | `Secret-Message-v1.1.2.apk` | 29,460,361 | `9ad6feba519e6746936f483c6f34893bc8da29250d382cb232be3d73315ba27b` |

The sizes and SHA-256 values are the public release assets' values reported by GitHub after publication, not build-only candidate hashes.

## Live Guard 2.54 — zero-data plans, bidirectional validity and opt-in daily watch (21 October 2026)

The 2.54 release notes are [`releases/live-guard-v2.54.md`](../releases/live-guard-v2.54.md). It adds a direct **No data (0 GB)** action and a separate booster-removal action; supports inclusive validity calculation in either direction with calendar/time selection for both fields; and makes the scheduled daily security-risk watch opt-in, including a one-time opt-out migration for existing installs. The one-time **Run the daily check now** action remains available. No permission was added.

## Live Guard 2.51 — published alone, 20 October 2026

Live Guard 2.51 / code 56 was published by Build APK [run 39407712230](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39407712230),
from `arena/1c17a6e2-rp-android-apps` at commit `821aa52`. This was the first release under the owner's
"one app at a time" rule: only Live Guard's version changed, so only its tag was public after the run.
PulseEQ 1.7, Ramesh Radio 1.13 and RP Vocal Remover Studio 0.1.13 kept their existing release pages and
assets, and Oor Arivippu and Secret Message were not touched.

The run passed the build (both variants), the required unit and regression suites — 44 suites / 400
declarations, including the ten new `RechargeSmsParserTest` cases — the Live Guard permission delta
(no permission added), the APK metadata checks and the post-publication asset check.

| Asset | Bytes | SHA-256 |
| --- | --- | --- |
| `CleanSweep-v2.51.apk` | 13,793,884 | `260120452041fcbe9d67aa76bd8c71cc1da78a40b2d57cf235a20a387e650ec5` |

The digest above is GitHub's own digest for the published asset, read back from the release API after
publication.

**What it contains:** the operator-clock expiry rule, the booster-pool counting fix, seven selectable
colour themes, a subject-grouped side menu with a reordered Home, and the "fill My plan from the
operator's SMS" card (copy and paste only — the app still declares no SMS permission). The local
date-format and twelve-hour-clock rules are unchanged.

**What it does not prove:** this build was not installed on a phone before publication, so no
physical-device result is claimed for these bytes. The previous instance of this section (Live Guard
2.50) is the last release with a recorded owner install. CI checks, a compiled APK and a published
asset are not Play Protect approval, not Play Store review and not an all-device compatibility claim.

## Ramesh Radio 1.14 — published alone, 20 October 2026

Ramesh Radio 1.14 / code 14 was published by Build APK [run 39413564341](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39413564341)
from `arena/1c17a6e2-rp-android-apps`. Only Radio's version changed, so only its tag was released;
Live Guard 2.51, PulseEQ 1.7, Vocal Remover 0.1.13, Oor Arivippu 1.1.1 and Secret Message 1.1.1 kept
their existing releases.

| Asset | Bytes | SHA-256 |
| --- | --- | --- |
| `Ramesh-Radio-v1.14.apk` | 14,526,817 | `d3372cbf5c0bcc4a11b260c49d437541fd9a901fd2593dfc61f8da9e4bbe238a` |

Digest read back from the release API after publication.

**What it contains:** the More tab's update section is a readable card with its long explanation
folded away, the first screen's search and sorting text are one-line rows that open on tap, and the
songs tab now reads **பாடல்கள் / என் பாடல்கள்** instead of "உள்ளூர் ஒலி". No new permission.

**Not claimed:** these bytes were not installed on a phone before publication. CI built every
module, ran the radio unit tests and lint, and checked the APK metadata.

**Workflow note:** this publish needed a fix of its own. The Live Guard permission gate ran even
though Live Guard was already public and compared it against its own latest tag, then failed
closed and published nothing (run 39412645425). The gate now applies only when Live Guard is the
app being published.

## Ramesh Radio 1.15 — the first screen shows stations, not paragraphs (20 October 2026)

Published by Build APK [run 39415226287](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39415226287)
from `arena/1c17a6e2-rp-android-apps`. Only Radio's version changed, so only its tag was released;
Live Guard 2.51 and the other apps kept their releases.

| Asset | Bytes | SHA-256 |
| --- | --- | --- |
| `Ramesh-Radio-v1.15.apk` | 14,543,201 | `b7ef2321f12faaf48f0d21b514a586bc6c906821d672a8f9459e55a706903ce5` |

Digest read back from the release API after publication. It supersedes 1.14, published fifteen
minutes earlier on the owner's instruction to take the Home and Tamil UI first; 1.15 contains the
1.14 update-card fix as well, so a phone that updates once receives both.

**What it contains:** six primary categories with the rest behind **More ▾ / Less ▴**, a one-line
ellipsised header, an action-first empty state, a data pill that follows the app language, and
unwrapped, centred bottom-bar labels for Tamil. No new permission.

**Not claimed:** these bytes were not installed on a phone before publication. CI compiled every
module, ran the radio unit tests, lint and the APK checks.

## Live Guard 2.53 — volume, guide, one status icon, three widgets (20 October 2026)

Published by Build APK [run 39428035172](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39428035172)
from `arena/1c17a6e2-rp-android-apps` (workflow run 39426688658 had already compiled and tested the
same source). Only Live Guard's version changed; Ramesh Radio 1.15, PulseEQ 1.7, Vocal Remover
0.1.13, Oor Arivippu 1.1.1 and Secret Message 1.1.1 kept their releases.

| Asset | Bytes | SHA-256 |
| --- | --- | --- |
| `CleanSweep-v2.53.apk` | 13,815,072 | `7d640e044ad62bf5841c38d18a741c8e8ccf06dfe9d0fea2b45e14ddc21104f4` |

Digest read back from the release API after publication. The release asset is signed and re-packed
by the publishing step, so its digest is not required to equal the candidate build's
(`13db2a88…a0e8`, recorded in `docs/candidate-build-digests.md`).

**What it contains:** one announcement-volume control opened over Home by the speaker button and
also present in Settings and the plan card; **Guide** given its own screen instead of opening the
permission checklist; the status-bar icon fixed to a monochrome glyph; the Home update card
reduced to one row with all detail in its dialog; the estimated phone age on Home; the assistant
reduced to a small symbol that opens the assistant window; and three 2×2 widgets — **Battery &
plan**, **Plan & days** (data left, days left, today) and **Security** (saved review only, no
background scan). No new permission.

## Live Guard 2.52 — short first page, readable menu, Tamil typography (20 October 2026)

Published by Build APK [run 39419657235](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39419657235)
from `arena/1c17a6e2-rp-android-apps`. Only Live Guard's version changed; Ramesh Radio 1.15,
PulseEQ 1.7, Vocal Remover 0.1.13, Oor Arivippu 1.1.1 and Secret Message 1.1.1 kept their releases.

| Asset | Bytes | SHA-256 |
| --- | --- | --- |
| `CleanSweep-v2.52.apk` | 13,811,808 | `19ba3d6c5922062add5ae9480e0b433ffa83be2225e317741d862c7a4118171b` |

Digest read back from the release API after publication.

**What it contains:** Home rebuilt around a hero card (free storage, used/total bar, Battery · RAM ·
Signal chips, scan button inside the card) with the plan card beneath and every other card under
three tap-to-open sections; a rebuilt side menu (banner, group headings with rules, icon tiles with
hints, accent rail on the current screen, language/theme footer); language-aware typography that
gives Tamil a 1.62× line box, no extra letter spacing and slightly larger glyphs. No new
permission.

**Not claimed:** these bytes were not installed on a phone before publication. Two CI compile
failures (a missing `SurfaceC` import and a missing `TextUnit.isSpecified` import) were fixed before
the green run; no device result is claimed for any of them.

## Earlier four-app batch — 20 October 2026 (initial Live Guard 2.50 release)

The four-app batch was rebuilt and published on 20 October 2026 by Build APK [run 39355649275](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39355649275),
from `arena/1c17a6e2-rp-android-apps` at commit `74b22a2`. It published Live Guard 2.50 / code 55,
Ramesh Radio 1.13 / code 13, PulseEQ 1.7 / code 8 and Vocal Remover 0.1.13 / code 14. Later solo
releases that day advanced Live Guard through 2.53 and Ramesh Radio to 1.15; the table below records
this batch's original assets, not today's version matrix. The run passed the build, unit/regression
suites, permission-delta gate, APK checks and post-publication asset checks. Live Guard 2.50 added
`ACCESS_COARSE_LOCATION`, which the gate treats as a newly added dangerous permission, so publication
required a recorded physical-device install: [`live-guard-2.50-device-evidence.md`](live-guard-2.50-device-evidence.md)
records the owner installing the exact `CleanSweep-v2.50.apk` (13,740,236 bytes) over 2.49 with Play
Protect on, Play Protect reporting "This app looks safe", the installer completing the update and the
nearby-cell list then showing four cells. **The published asset's SHA-256 is the tested digest**, so
the released APK is the installed one. That is one phone; no second device and no Play Store bundle
was tested. PulseEQ 1.7 and Vocal Remover 0.1.13 contained no functional change — their versions moved
only because the batch publishes all four apps together. Oor Arivippu 1.1.1 and Secret Message 1.1.1
were unchanged from 19 October 2026.

| App | Version / code | RP-Android-Apps release | APK asset | Bytes | SHA-256 |
| --- | ---: | --- | --- | ---: | --- |
| Live Guard (CleanSweep) | 2.50 / 55 | [live-guard-v2.50](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/live-guard-v2.50) | `CleanSweep-v2.50.apk` | 13,740,236 | `b4c183c876a8da8aff4169c975c1a1111a3229658620e99fed87cb3e645d6176` |
| Ramesh Radio | 1.13 / 13 | [ramesh-radio-v1.13](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/ramesh-radio-v1.13) | `Ramesh-Radio-v1.13.apk` | 14,526,813 | `d763d585d46d29319b18dcbe98fd37bb4ad587062210e44556bc6ae7ab1792d1` |
| PulseEQ | 1.7 / 8 | [pulseeq-v1.7](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/pulseeq-v1.7) | `PulseEQ-v1.7.apk` | 11,567,368 | `a75c411d0e854fe17357d8dc3fc5797b5bc9f4ec5e87cdc5d4d7eb7dc6b91c93` |
| RP Vocal Remover Studio | 0.1.13 / 14 | [rp-vocal-remover-studio-v0.1.13](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/rp-vocal-remover-studio-v0.1.13) | `RP-Vocal-Remover-Studio-v0.1.13.apk` | 63,745,096 | `8f597a7f621dcfdbaf26e2c76c2b32d2585fc447dce0e915576dfb819358e8b9` |
| Oor Arivippu | 1.1.1 / 6 | [oor-arivippu-v1.1.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.1.1) | `Oor-Arivippu-v1.1.1.apk` | 12,228,161 | `5fe6899685858cbd6a552aa303d7218b10f72af1190da6c72f9b8c054ca9343d` |
| Secret Message | 1.1.1 / 3 | [secret-message-v1.1.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/secret-message-v1.1.1) | `Secret-Message-v1.1.1.apk` | 29,460,361 | `35351521a72b4b1fd0f510540e15d588e05baaffd1bdd43a1fd63604ec7ddb85` |

The sizes and digests above are the release assets' own values as reported by the GitHub releases API after publication,
not build-only candidate hashes.

## Previous active stable releases — as of 19 October 2026

All six apps were published on 19 October 2026. The four-app batch (Live Guard, Ramesh Radio, PulseEQ
and RP Vocal Remover Studio) was built, verified and published by Build APK [run 39272003076](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39272003076)
from `main` at merge commit `de624572`; Oor Arivippu 1.1.1 by its standalone [run 39268547086](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39268547086);
Secret Message 1.1.1 by `.github/workflows/secret-message-wifi-test.yml` in [run 39268577963](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39268577963).
Each run checked the package, version, minimum SDK, signing certificate and local SHA-256, promoted the
release, and then re-read its own public release to confirm the asset name, byte size and GitHub-reported
SHA-256. None of the six was mirrored to `InternetRadio`: that repository keeps its earlier releases
(2.46, 1.11, 1.5, 0.1.11, Oor 1.1.0 and older, Secret Message 1.1.0) unchanged, and the updaters still
try `RP-Android-Apps` first.

| App | Version / code | RP-Android-Apps release | APK asset | Bytes | SHA-256 |
| --- | ---: | --- | --- | ---: | --- |
| Live Guard (CleanSweep) | 2.49 / 54 | [live-guard-v2.49](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/live-guard-v2.49) | `CleanSweep-v2.49.apk` | 13,734,860 | `ae85fc1b18267a6808f80c99713978d62b5aff33c3707f61d286dc712a2e511b` |
| Ramesh Radio | 1.12 / 12 | [ramesh-radio-v1.12](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/ramesh-radio-v1.12) | `Ramesh-Radio-v1.12.apk` | 14,526,813 | `ec4d0d1ea0986661fd474f8686b7b042b1afe8f360f29adbc68dafd7e8291274` |
| PulseEQ | 1.6 / 7 | [pulseeq-v1.6](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/pulseeq-v1.6) | `PulseEQ-v1.6.apk` | 11,567,364 | `0cbb29f15ef018903a69b82ae2214db35555474d468e95967253ee05630a6e74` |
| RP Vocal Remover Studio | 0.1.12 / 13 | [rp-vocal-remover-studio-v0.1.12](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/rp-vocal-remover-studio-v0.1.12) | `RP-Vocal-Remover-Studio-v0.1.12.apk` | 63,739,000 | `9c8015f43023099a06067051bc3fe0788eb7cb8e85a81bf483d257111ae9e6d4` |
| Oor Arivippu | 1.1.1 / 6 | [oor-arivippu-v1.1.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.1.1) | `Oor-Arivippu-v1.1.1.apk` | 12,228,161 | `5fe6899685858cbd6a552aa303d7218b10f72af1190da6c72f9b8c054ca9343d` |
| Secret Message | 1.1.1 / 3 | [secret-message-v1.1.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/secret-message-v1.1.1) | `Secret-Message-v1.1.1.apk` | 29,460,361 | `35351521a72b4b1fd0f510540e15d588e05baaffd1bdd43a1fd63604ec7ddb85` |

The Live Guard 2.49 permission-delta gate compared the built APK with the published 2.46 APK and found
no newly added dangerous runtime permission (the new `FOREGROUND_SERVICE_DATA_SYNC` is a normal
foreground-service declaration), so no new physical-device evidence record was required. The owner
reports that he downloaded and installed all six updates on his phone from these release pages and that
they work. That is an owner report; it does not hash-match the installed bytes to the digests above,
and GitHub publication is not Play Protect approval or an all-device compatibility claim.

## Previous active stable releases — as of 18 October 2026

| App | Version / code | RP-Android-Apps release | InternetRadio fallback | APK asset | Bytes | SHA-256 |
| --- | ---: | --- | --- | --- | ---: | --- |
| Secret Message | 1.1.0 / 2 | [secret-message-v1.1.0](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/secret-message-v1.1.0) | [secret-message-v1.1.0](https://github.com/universalrp2003/InternetRadio/releases/tag/secret-message-v1.1.0) | `Secret-Message-v1.1.0.apk` | 29,443,977 | `cc9a69d0022e49c1f653291e033bca8de0e1c4e0477731df1741cc3ddfbc5e10` |
| Oor Arivippu | 1.1.0 / 5 | [oor-arivippu-v1.1.0](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.1.0) | [oor-arivippu-v1.1.0](https://github.com/universalrp2003/InternetRadio/releases/tag/oor-arivippu-v1.1.0) | `Oor-Arivippu-v1.1.0.apk` | 12,211,781 | `34858e8c6e4831e725ac7503f6613c96bcb896d659ebbdb588dcd139afc7c8b3` |
| Live Guard | 2.46 / 51 | [live-guard-v2.46](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/live-guard-v2.46) | [live-guard-v2.46](https://github.com/universalrp2003/InternetRadio/releases/tag/live-guard-v2.46) | `CleanSweep-v2.46.apk` | 13,655,296 | `92be908e773d0214c5c8dc0d9bf4254f05541fc791d6494316a0a07cfb6ea80a` |
| Ramesh Radio | 1.11 / 11 | [ramesh-radio-v1.11](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/ramesh-radio-v1.11) | [ramesh-radio-v1.11](https://github.com/universalrp2003/InternetRadio/releases/tag/ramesh-radio-v1.11) | `Ramesh-Radio-v1.11.apk` | 14,494,129 | `e27c408ac7e199fc480b15e7472e4c3bcbf121f79bb6dbddb5d875c0e48594fc` |
| PulseEQ | 1.5 / 6 | [pulseeq-v1.5](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/pulseeq-v1.5) | [pulseeq-v1.5](https://github.com/universalrp2003/InternetRadio/releases/tag/pulseeq-v1.5) | `PulseEQ-v1.5.apk` | 11,534,600 | `1860ad5282e9d2809143e6923678f467b13f55e9bee9c02b1612b7ab0622be5f` |
| RP Vocal Remover Studio | 0.1.11 / 12 | [rp-vocal-remover-studio-v0.1.11](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/rp-vocal-remover-studio-v0.1.11) | [rp-vocal-remover-studio-v0.1.11](https://github.com/universalrp2003/InternetRadio/releases/tag/rp-vocal-remover-studio-v0.1.11) | `RP-Vocal-Remover-Studio-v0.1.11.apk` | 63,671,220 | `aea56a8676ce73339eb8eae4e29b0760676ed8300f47970490223061f876f609` |

The post-publication GitHub API check confirmed that all ten release records are public, stable (not drafts or prereleases), and have canonical release-page and APK-download URLs. For each app, the public assets in both repositories have the same name, byte size and SHA-256 digest; the four legacy destination versions are newer than their bridge versions. The APK updaters check `RP-Android-Apps` first and `InternetRadio` second; when an otherwise-valid version is present in both, the new repository has priority. Owner-provided phone screenshots dated 14 October show update-available notices for all four bridge versions; later screenshots show Live Guard reporting 2.46 installed, Ramesh Radio at Play Protect's safe-to-install screen, and PulseEQ at Android's update confirmation. These confirm device-side discovery and updater hand-off, but do not identify the supplying repository or bind the device state to the published SHA-256 digests; final installation is not shown for every app. On 15 October, the owner supplied Oor Arivippu screenshots showing a local `Oor-Arivippu-v1.0.0.apk` file with app info `com.oorarivippu.voiceannouncer` version 1.0.0 (2), Play Protect's “This app looks safe” screen, and the running app's About/update screen reporting version 1.0.0 and no newer release. The owner reports the app is working and successful testing on two devices. The displayed package/version and rounded file size are consistent with the published release, but the screenshots do not expose the APK SHA-256 or prove that the installed bytes match the published digest above. Keep hash-linked exact-build confirmation distinct from owner-reported tests and screenshots. For Oor Arivippu 1.0.1, the owner tested the debug APK from [run 38776902130](https://github.com/universalrp2003/InternetRadio/actions/runs/38776902130), built from the same app source before the version was raised; screenshots show it running at 1.0.0 with the voice picker and a place announcement. After publication, the owner also installed the debug APK from Oor test [run 38826564762](https://github.com/universalrp2003/InternetRadio/actions/runs/38826564762) (artifact `oorarivippu-test-debug-apk-10`), built from the exact 1.0.1 source; screenshots show package `com.oorarivippu.voiceannouncer` 1.0.1 (3), Play Protect's “This app looks safe” screen, and the updater reporting installed 1.0.1 with no newer release. These debug-signed builds cannot share the stable APK's bytes, and no screenshot shows an APK SHA-256, so the 1.0.1 digest below is not device hash-matched. Oor Arivippu 1.0.2 was published at the owner's request without a device test; no build of the 1.0.2 source has been installed on a phone, so its digest above is not device-tested or hash-matched.

## Previous Oor Arivippu stable releases — 15 and 16 October 2026

The earlier stable Oor releases remain available unchanged on both repositories:

| App | Version / code | RP-Android-Apps release | InternetRadio fallback | APK asset | Bytes | SHA-256 |
| --- | ---: | --- | --- | --- | ---: | --- |
| Oor Arivippu | 1.0.1 / 3 | [oor-arivippu-v1.0.1](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.0.1) | [oor-arivippu-v1.0.1](https://github.com/universalrp2003/InternetRadio/releases/tag/oor-arivippu-v1.0.1) | `Oor-Arivippu-v1.0.1.apk` | 12,195,397 | `ec10c7369807748c9a5608f1a5c6a37eff5f8e1f7433cc16e984c75d8958457d` |
| Oor Arivippu | 1.0.0 / 2 | [oor-arivippu-v1.0.0](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.0.0) | [oor-arivippu-v1.0.0](https://github.com/universalrp2003/InternetRadio/releases/tag/oor-arivippu-v1.0.0) | `Oor-Arivippu-v1.0.0.apk` | 12,179,013 | `b438e0dbe76c0de975a4ce35bf126b8825cd5af0976fe3db93a11ae8b6f6794d` |

**FTTH download observation — 14 October:** the owner reports that direct Brave downloads from both repositories over BSNL FTTH were very slow. Screenshots show `CleanSweep-v2.46.apk` (13.66 MB) at about 0.33 MB and 0.46 MB downloaded, with Brave estimating seven minutes remaining. This demonstrated the slowdown was not limited to the in-app updater. The owner later reported that restarting the BSNL router resolved it. That points to a transient router/session/network-path issue, but the exact cause was not established. The 31.8 Mbps speed test used a different endpoint and does not establish throughput to GitHub.

For the 14 October bridge releases, build-only runs [38514595315](https://github.com/universalrp2003/InternetRadio/actions/runs/38514595315) and [38523346072](https://github.com/universalrp2003/InternetRadio/actions/runs/38523346072) produced candidate APK hashes that differ from the published bytes. Those historical candidate records remain in Git history; [`candidate-build-digests.md`](candidate-build-digests.md) is a rolling record replaced by later candidate builds. Do not substitute candidate hashes for the published digests above.

## Previous stable bridge releases — 14 October 2026

These versions were published to `InternetRadio` so existing installations could update to a bridge client that knows both repositories. Their release records remain available unchanged:

| App | Release | APK asset | Bytes | SHA-256 |
| --- | --- | --- | ---: | --- |
| Live Guard | [live-guard-v2.45](https://github.com/universalrp2003/InternetRadio/releases/tag/live-guard-v2.45) | `CleanSweep-v2.45.apk` | 13,652,452 | `426340c89dcdb7df2dd817070054141ccb9b0da0bac12dad9a82d308b300aaad` |
| Ramesh Radio | [ramesh-radio-v1.10](https://github.com/universalrp2003/InternetRadio/releases/tag/ramesh-radio-v1.10) | `Ramesh-Radio-v1.10.apk` | 14,494,105 | `4bde78049be431e0e6533a9c69e44f13c90c7c823f449b1e385968fafa2dbd06` |
| PulseEQ | [pulseeq-v1.4](https://github.com/universalrp2003/InternetRadio/releases/tag/pulseeq-v1.4) | `PulseEQ-v1.4.apk` | 11,534,600 | `b1164d8899e75d757c1f6a2f88e14fed2a49654747812158ffd38e1bb11cd290` |
| RP Vocal Remover Studio | [rp-vocal-remover-studio-v0.1.10](https://github.com/universalrp2003/InternetRadio/releases/tag/rp-vocal-remover-studio-v0.1.10) | `RP-Vocal-Remover-Studio-v0.1.10.apk` | 63,668,936 | `ccbedc4565c78541efd44cfdf831b465d261b8eaef505b8ba477326755a5c70e` |

## Previous stable releases — 13 October 2026

These historical release pages, tags and APK assets remain on `InternetRadio`:

| App | Release | APK asset | Bytes | SHA-256 |
| --- | --- | --- | ---: | --- |
| Live Guard | [live-guard-v2.44](https://github.com/universalrp2003/InternetRadio/releases/tag/live-guard-v2.44) | `CleanSweep-v2.44.apk` | 13,632,768 | `6ebe62debf4bdf8c680c9ce39c4af4f59e15de9d2f6a57750031092d9c0fd515` |
| Ramesh Radio | [ramesh-radio-v1.9](https://github.com/universalrp2003/InternetRadio/releases/tag/ramesh-radio-v1.9) | `Ramesh-Radio-v1.9.apk` | 14,491,793 | `322245c30e1c16e937cf0333e02f2f3270966b79105114ca012e58647c70948b` |
| PulseEQ | [pulseeq-v1.3](https://github.com/universalrp2003/InternetRadio/releases/tag/pulseeq-v1.3) | `PulseEQ-v1.3.apk` | 11,533,640 | `fa5a96407ffd5439aee677fa1855c05ef5b9c5f9bee3199ea397707580ddc3cc` |
| RP Vocal Remover Studio | [rp-vocal-remover-studio-v0.1.9](https://github.com/universalrp2003/InternetRadio/releases/tag/rp-vocal-remover-studio-v0.1.9) | `RP-Vocal-Remover-Studio-v0.1.9.apk` | 63,659,576 | `e49ca9fbf96aed66351d13ae63c3d2878280b6b09be42b050c00de9d97e6b208` |

For every release, installation remains under Android's per-app **Install unknown apps** permission and installer confirmation. The updater does not silently install or bypass Play Protect.
