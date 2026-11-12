# TV Guard 1.4 — candidate for the Sony Bravia

**Status: built, CI-verified, not published. Waiting for the physical test.**

1.3 was accepted as "otherwise all good", so this round is seven specific fixes plus the
guide and the Tamil/English question.

## The file

| | |
|---|---|
| Artifact | `tvguard-1.4-stable-signed-tested-candidate` (run 42994048708) |
| APK inside | `Android-TV-Guard-1.4-signed-candidate.apk` |
| Size | 11,250,433 bytes |
| SHA-256 | `a51e1a47c953750b2d38f2da04506874b2b34cef1b98dee29b8b09625204bfae` |
| Package | `com.universalrp.tvguard` |
| Version | `versionName 1.4`, `versionCode 6` |
| Signing | same key as 1.3, installs straight over it |

## What each report turned into

**1. The fake "26.0 GB free" on the pill.** This was the serious one and you were right to
call it fake. The pill read `StatFs` on `/data`, which is one small partition, not the drive:
on a 16 GB TV that produced a bigger number than the TV can hold. A new `StorageTruth` measures
every volume the app can see — internal storage plus any USB drive or SD card — prefers the
roomiest real one, and **rejects any volume whose free space exceeds its own capacity**, so an
impossible number is never printed. The audit screen compares partitions against each other;
the pill shows the real figure. `StorageTruthTest` pins the rule down.

**2. The remote's Down button not reaching Hardware Truth.** The status-pill controls were four
big corner cards plus four nudge buttons, which made the screen taller than the TV and left
Down with no focus target — so it appeared stuck. Those controls are now **one compact row**
(`Pill: top-left   ◀  ▶  ▲  ▼`), and the last button explicitly hands focus to the next
section. A focusable section anchor bridges into the cards, so Down always reaches the hardware
section.

**3. The separate big "Storage Access" request.** Gone as a permanent fixture. It now appears
**only when access is genuinely missing**, and tapping it goes straight to the system page in
one press. The extra explanation dialog is removed.

**4. The camera not showing in the top right.** Restored: the screenshot button is back in the
home bar whenever screenshots are enabled, next to refresh.

**5. Running applications list empty.** Not a rendering bug — on Android 8+,
`ActivityManager.runningAppProcesses` only returns the calling app's own processes, so a
third-party app sees nothing. It now reads recently used apps from `UsageStatsManager`. The
section is labelled **"Recently used applications"** because that is honestly what it is; the
memory column is a coarse shared figure, not per-app truth, and it says so.

**6. The heart on every app tile.** Removed. It was decoration and made every app look
favourited. A small star now appears **only** on real favourites.

**7. Guide page.** Rewritten: `docs/tvguard-overlay.md`. It matches the five-reading strip, the
separate switch, how to move it, the corrected storage source, and the removed AI feature.

## Tamil and English

The app already had a Tamil translation file, but only the ADB guide could actually switch
language. Now:

- Every remaining hardcoded UI string was moved into string resources, and **sixteen new
  strings** were added in both English and Tamil (headings, buttons, empty states, counters).
- `android:localeConfig` declares `en` and `ta`, so on Android 13+ the whole app can be
  switched from **Settings → System → Languages → App languages → Android TV Guard**.
- On older Android the app follows the TV's system language.

If any Tamil text reads awkwardly on screen, tell me the screen and the wording — an
unreviewed translation of a technical term is exactly the kind of thing to fix from the TV
rather than guess at again.

## What CI proved, and what it did not

Proved on a disposable Android TV emulator, on this exact APK: 66 unit tests (including five
for the storage rule and five that measure the pill layout), five D-pad/focus tests, a
different-signature APK rejected, a same-key APK updating in place with favourites, hidden
apps, feature choices and private files preserved, and the installed APK's hash matching the
artifact byte for byte.

Not proved: anything visual on your Bravia, and whether the corrected storage figure now reads
sensibly against what you know the TV holds. Those need your eyes.

## Please check

1. Pill storage figure now looks plausible for the set.
2. Down from the pill controls reaches Hardware Truth.
3. Storage Access card appears only if access is missing.
4. Camera button in the top right.
5. Running applications list has entries.
6. No hearts on tiles; a star only on favourites.
7. Tools → Expert, then switch the app language to Tamil and check a few screens.
