# Tamilan Store 0.17 — owner phone test of the candidate

**What was verified, by whom, and when.** Owner report, 24 November 2026, 11:50–11:51 (phone clock), on the
owner's own phone. APK: `Tamilan-Store-v0.17.apk`, 20.71 MB as the phone shows it, from the CI artifact
`Tamilan-Store-0.17-release-key-upgrade-candidate` of run
[45331600676](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/45331600676) (source `97d252e`),
SHA-256 `c38bed526fc53e0276f64c853665b60f29ad9cc687b623b3e1c7bf6013be66c7`, 20,713,347 B. The owner's words:
**"All working you can publish both"**, with four screenshots. The publication promotes these exact bytes, so the
published file is the file that was on this phone.

## What the screenshots show

| Screenshot | Shows |
| --- | --- |
| `owner-0-17-play-protect-scanning.jpg` | Google Play Protect "Scanning…" for `தமிழன்`. |
| `owner-0-17-play-protect.jpg` | Google Play Protect: **"This app looks safe — You can continue to install it"**, Install / Don't install. One phone; not a Google clearance. |
| `owner-0-17-installer-update.jpg` | Package installer: `Tamilan-Store-v0.17.apk`, 20.71 MB, `com.universalrp.tamilanstore` **0.17 (19)**, installed version **0.16 (18)**, button **UPDATE** — the in-place, same-signer upgrade this release is meant to be. The Downloads folder shows the artifact's `verification.json`, `permissions.json` and `.sha256` beside the APK. |
| `owner-0-17-about.jpg` | About after the update: **"தமிழன் · 0.17. நமது 17 செயலிகள்"**, and the bundled-list line **"Store 0.17-உடன் வந்த பட்டியல்: 17 செயலிகள், 13 நவம்பர் 2026"** — the date now read from `catalog.json` (`updated: 2026-11-13`) and rendered in Tamil, not typed into the app. "அனைத்தும் பட்டியல்" filter, Updates (0), SMS Guard 1.1.17 listed below. |

## What this does **not** show

- The **download restored after relaunch** fix (a download begun from a shorter live feed coming back after a
  relaunch): covered by the JVM and emulator tests in run 45331600676, not demonstrated on this phone.
- The English rendering of the catalog date; only the Tamil About screen was captured.
- A catalog refresh over the mobile network after the update.

## Not claimed

No second phone, no all-OEM or Doze behaviour, no Google clearance for the release. Play Protect "looks safe" is
the owner's observation on one phone at one moment.
