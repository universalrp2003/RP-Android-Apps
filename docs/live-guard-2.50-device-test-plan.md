# Live Guard 2.50 — device test plan (needed before publication)

Why this file exists: 2.50 is the first Live Guard release that asks for **Location**. The release
workflow compares the built APK with the last published one (2.49) and treats
`ACCESS_COARSE_LOCATION` as a newly added dangerous permission. Under the owner's 12 October 2026
decision that needs a **recorded physical-device install** before anything is published
(`tools/verify_release_permission_delta.py`, `PUBLISHING.md`). This page is the checklist; the
evidence file is `docs/live-guard-2.50-device-evidence.md` and it is written only from real
answers.

## What to install

- Build: Live Guard / CleanSweep **2.50, versionCode 55**, package `com.universalrp.cleansweep`.
- File: `CleanSweep-v2.50.apk` from the `cleansweep-apk` artifact of the Build APK run on
  `arena/1c17a6e2-rp-android-apps` (the run whose summary lists 2.50 / code 55).
- Install over 2.49 with Play Protect **left ON**. Do not disable Play Protect for the test.
- The signing key is unchanged, so Android should offer **Update**, and the plan, settings,
  language and history must survive.

## What to check (and what "correct" looks like)

1. **Install** — Android's installer shows `2.50 (55)` over `2.49 (54)`, Play Protect gives its
   normal result, and the app opens.
2. **Mobile tab → nearby cell towers.** The amber **Nearby cell towers** card appears with
   *Allow location and rescan*. Before this build the screen only said
   *"No cell tower details: Android only shares them with Location allowed"*.
3. **Tap Allow location and rescan** → Android's location dialog appears with **Precise** and
   **Approximate**. Choose **Precise**, then press allow.
4. **The tower list appears** under *Nearby cell towers*, serving cell first, then the strongest
   neighbours (up to 10 rows), with the line *"Serving cell first, then the strongest neighbours…"*.
   If your phone or network reports nothing, the note must say so honestly instead of blaming a
   permission — record that text if it happens.
5. **Approximate path (optional).** In Android Settings → Apps → Live Guard → Permissions →
   Location, choose *Approximate only*, reopen Mobile: the card must warn that Approximate is not
   enough for tower ids.
6. **One pack number.** My plan set to **6 GB total, start 9 October 2026, 63 days, booster Off,
   5G Off**. The My plan card, the notification card, the status pill and the 4G/5G split must all
   divide by the same 6.0 GB, and the pill should read about **67 %** in cyan (red is 90 % and
   above, amber 75 % and above). Nothing may still say 4.5 GB or 7.5 GB.
7. **Ramesh Radio 1.13** (same batch): the search row is one line tall with the station-list icon
   square beside it, and opening the app starts the last station by itself. The switch is in
   **More → Start the last station when the app opens**.

## What to send back

Screenshots or plain answers for items 1–7, plus the phone model and Android version. Two details
matter most:

- the Android location dialog with **Precise** visible (item 3);
- the tower list itself (item 4), and the pack numbers on two different screens (item 6).

## What happens next

With those answers this repository records the real outcome — Play Protect result and installer
outcome, in the owner's own words — plus the machine-readable lines:

```
TESTED-APK-SHA256: <the digest of the installed CleanSweep-v2.50.apk>
TESTED-RUN-ID: <the Build APK run that produced that APK>
```

The workflow then publishes **those exact bytes** (promoting the run's artifact if a rebuild does
not reproduce the digest), together with Ramesh Radio 1.13, PulseEQ 1.7 and Vocal Remover 0.1.13
in one batch. Until then nothing in the batch is published, and no release note claims a device
test.
