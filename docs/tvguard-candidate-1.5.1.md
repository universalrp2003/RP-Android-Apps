# Android TV Guard 1.5.1 — copy fixes from the owner's screenshots

**Status: candidate, not published.** The owner installs it, tests it on the TV, and says yes before
anything goes to a public release.

## What changed since 1.5

Only text, plus one new link card in the phone hub. No change to the pill layout, the storage
calculation, the permissions, the signing key, or the feature switches.

| Fix | Before (visible in the 1.5 screenshots) | Now |
| --- | --- | --- |
| Storage row, English and Tamil | `11.88 GiB free of 14.68 GiB(limited to the physical disk…)` — the note ran into the figure | a space is part of the layout string, so the note reads `… 14.68 GiB (limited to …)` |
| Expert switch, status bar | claimed *the individual status pills below need this switch* | says the switch draws the in-app header and clock and **does not** affect the floating pill, which has its own switch |
| Expert switch, floating pill | *needs Display over other apps **and the top status bar***, drew *four* readings at *about 50%% transparency* | five readings listed, needs only "Display over other apps", secure/system screens may hide it, and the stray `%%` is gone |

The `%%` mattered: Android renders `%%` as `%`, so the sentence about transparency was the only
place in the app that still printed a doubled percent sign.

## New in the phone hub

The Wi-Fi hub page now has an **RP App Store** card (between the maintenance actions and the remote
control) that opens <https://universalrp2003.github.io/RP-Android-Apps/>. It is a shortcut to the
official download page from the phone already paired to the TV, so nobody has to remember an
address. It changes no install behaviour: the APK is opened from the hub's file list and Android
asks the user to confirm. The card's wording was deliberately left as built — it names the file
list rather than promising an install — because changing a string now would change the candidate's
hash after the owner had been given it.

The Tamilan Store model is **unchanged** — a new app still means a new Tamilan Store APK. See
[store-model-decision.md](store-model-decision.md).

## Identities

| | |
| --- | --- |
| Package | `com.universalrp.tvguard` (unchanged) |
| versionName / versionCode | `1.5.1` / `8` |
| Signer | the same stable key as 1.2 – 1.5; no new key, no migration |
| Minimum | Android TV 8.0 (API 26) |

**Built candidate**, run `43012300628`, artifact `tvguard-1.5.1-stable-signed-tested-candidate`:

- `Android-TV-Guard-1.5.1-signed-candidate.apk`, 11,254,309 bytes
- SHA-256 `aea1ae03d2bfe87ee0659cd2cba8104e22365c1c18391587d9ae7b70cae3614b`
- versionName 1.5.1 / versionCode 8, same signer, installs over 1.5 in place

## What this build does and does not prove

Verified in CI on a disposable API 34 Android TV emulator:

- 69 unit tests for 1.3 – 1.5, plus 3 for the new store card (72 total), run on both the debug and
  the release variant.
- Five D-pad focus tests, walking the drawn order, with the options opened by the **Menu** key and
  no long-press menu present.
- Same-key upgrade from a synthetic code 1 build, preserving favourites, hidden apps, feature
  choices and a private file.
- Rejection of a different-signer install without deleting the old app's data.

Not verified here, and not claimed: the physical TV, the real remote's key codes, voice input, root
features, the screenshot button's behaviour, or any Play Protect clearance. Those stay the owner's
screen-and-remote check, which is why 1.5.1 is a candidate.

## For the record: how the two string defects got past 1.5

The 1.5 gate checked that English and Tamil had the same keys, the same placeholders, and no
mistranslated counts. It did not check what the rendered sentence looks like. Both defects are
grammar-level: a missing space, and copy that was true in 1.4 and became false when the pill
changed. The lesson is in the workflow now — the strings are read as sentences, and the 1.5
screenshots that exposed them are the ones published with this release.
