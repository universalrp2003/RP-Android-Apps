# Live Guard 2.50 — device evidence (one build, one phone)

**Recorded 20 October 2026 from the owner's own phone and words.** The owner installed this exact
APK over 2.49 with **Play Protect ON**, confirmed the nearby-cell list works, and reported
"See all fine". This is one build on one phone. It is not Google's approval, not app-store
approval and not all-device certification, and it says nothing about any other build.

## The tested bytes

```
TESTED-APK-SHA256: b4c183c876a8da8aff4169c975c1a1111a3229658620e99fed87cb3e645d6176
TESTED-RUN-ID: 39353706282
```

- APK: `CleanSweep-v2.50.apk`, **13,740,236 bytes** (the installer shows 13.74 MB),
  package `com.universalrp.cleansweep`, version **2.50 (55)**, minimum Android 8.0 (API 26).
- Built by Build APK [run 39353706282](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39353706282)
  from source commit `3bef93c`, artifact `cleansweep-apk` → `release/`.
- The digest above is this repository's own recorded candidate digest for that artifact
  (`docs/candidate-build-digests.md`), and the download folder held the matching sidecar
  `CleanSweep-v2.50.apk.sha256` (121 bytes) beside the APK.
- The owner downloaded the APK artifact from that run and installed it directly; it was not
  installed from a release page, because no 2.50 release exists yet.

## Phone

- Owner's Xiaomi phone running HyperOS — the same device that produced the 2.40–2.42 evidence
  records. The owner did not restate the exact model and build in this report.
- Play Protect state during the install: **ON**. It was not disabled or bypassed.

## Observed Play Protect result

`Screenshot_2026-10-20-07-16-07-972_com.android.vending.jpg` — Google Play Protect, Live Guard app
icon, heading **"This app looks safe"**, subtitle **"You can continue to install it"**, buttons
Install / Don't install. No warning, no block, no "app scan recommended" prompt this time.

## Observed installer outcome

1. `Screenshot_2026-10-20-07-15-24-514_com.cxinventor.file.explorer.jpg` and
   `…-07-15-27-856_…jpg` — Downloads → `cleansweep-apk` → `release` holds
   `CleanSweep-v2.50.apk.sha256` (121 B) and `CleanSweep-v2.50.apk` (13.74 MB).
2. Tapping the APK opened Android's package installer card: Name **Live Guard**, Size
   13.74 MB, Modified 20 Oct 2026 1:35 am, App info `com.universalrp.cleansweep 2.50 (55)`,
   **Installed version 2.49 (54)**, button **UPDATE**. This is an in-place update, not a
   fresh install.
3. The owner confirmed the update; Play Protect gave the result above; the installer completed.
4. `Screenshot_2026-10-20-07-16-37-328_com.universalrp.cleansweep.jpg` — the app opens and
   About reads **CleanSweep v2.50**. Settings, plan and language carried over (the owner made
   no re-setup and reported "all fine").

## What the build was expected to fix, as observed on the phone

- `Screenshot_2026-10-20-07-16-52-865_com.universalrp.cleansweep.jpg` — Mobile & data, **before**
  granting location: the new amber card **"அருகிலுள்ள செல் டவர்கள்" / "Nearby cell towers"**
  with the explanation and the **"இருப்பிடத்தை அனுமதித்து மீண்டும் படி"** (Allow location and
  rescan) button. The old dead end ("Android only shares them with Location allowed" with no
  action) is gone.
- `Screenshot_2026-10-20-07-17-45-052_com.universalrp.cleansweep.jpg` — **after** allowing
  location: the tower card lists **4 cells** with real identifiers and levels —
  `cell 191225394 · PCI 462 · -104 dBm · TAC 55383` marked as the serving cell, then
  `cell 2147483647 · PCI 463 · -105 dBm`, `PCI 272 · -109 dBm`, `PCI 356 · -137 dBm` — sorted
  the way the release notes describe, with the honest note about what the modem chooses to
  report. This is the exact capability that could never work before 2.50.
- `Screenshot_2026-10-20-07-18-31-604_com.brave.browser.jpg` — the Live Guard notification
  shows the split **4G 39.14 MB used · 5G 12.51 MB used**, and the pack line
  **"4.14 GB / 9.0 GB · 4.86 GB left"**. The split and total agree with each other, which is
  what the 2.50 split fix was for. *Open item, not a publication blocker:* the allowance reads
  9.0 GB, so either the saved plan is 9 GB (for example 6 GB + a 3 GB booster) or a booster is
  switched on in My plan; the owner was asked to confirm. Nothing in these screens shows two
  different totals for the same pack.

## Limits of this record

- One phone, one build. No second device, no Android 8/9 or Android 13 check.
- The Play Store bundle (`CleanSweep-v2.50.aab`) was **not installed on any phone**; no
  physical-device test covers the bundle bytes.
- The screenshots show the package/version and the working features; they do not print a
  SHA-256 on screen. The digest above is bound to the installation through the artifact the
  owner downloaded, its sidecar file and this repository's recorded candidate digest — not
  through a hash shown on the phone.
- CI success, this record and GitHub publication are not Play Protect approval and not an
  all-device compatibility claim.
