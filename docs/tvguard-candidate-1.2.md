# Android TV Guard 1.2 — verified signed candidate

Status: **built and verified, NOT published.** Waiting for you to install it on the TV and
confirm. Nothing has been uploaded to a release.

## Exact build

| Field | Value |
| --- | --- |
| Package | `com.universalrp.tvguard` (unchanged) |
| Version name / code | `1.2` / `4` |
| APK SHA-256 | `6d2825978d6fb8cdb14e6e38f27dfb5fdaef0942f8c16007cd358eb58cf346a3` |
| APK size | 11,283,753 bytes |
| Signing | the existing stable release key (same signer as the 1.1 copy on your TV) |
| CI run | [42972541128](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/42972541128) |
| Download | the run's artifact `tvguard-1.2-stable-signed-tested-candidate` |

Because the code went 3 → 4 under the same signer, this **installs over the 1.1 test copy
without an uninstall**, so its saved settings are kept.

## What changed, and why

### Long press — the actual TV bug
The policy only noticed a long press from an *auto-repeating* key-down. A Sony BRAVIA remote
sends one DOWN, no repeats, then UP, so the menu never opened. Worse, the tile's key-up branch
only handled "launch", so that new "open options" result was swallowed — the press did nothing.

Now: a hold is measured when the key is released, and the tile opens the menu on that event.
The emulator test injects a single DOWN, waits, then UP (no repeats), which is what the TV
really sends; a second test still covers a remote that does repeat.

### Status pill
- Only the five readings you asked for: processor load, free RAM, free storage, temperature,
  internet quality. **Wi-Fi strength is gone.**
- **No background colour** — the text sits directly over the picture with a soft shadow.
- Values carry a plain word: `CPU 56% free good`, `... medium`, `... bad`.
- **Movable without a pointer:** Tools → pick one of four corners, then nudge with the
  ← → ↑ ↓ buttons. The position is saved and applied to a running pill immediately.

### Storage
The pill was showing the true `StatFs` figure but wrapped in "(reported) · check note", which
made it look invented. It now reads plainly `26.0 GiB free`. The honest mismatch warning
remains a separate line on the Hardware screen, unchanged.

### Hidden apps
Tools → Show or hide apps now lists **every** installed app with a tick: tick hides, untick
shows. This also removes any dependence on long press working on a given remote.

### Removed
- **AI entirely**: provider client, key storage, dialogs, settings screen, tests, strings and
  the microphone shortcut (it never fired on your remote anyway). Spoken questions are
  answered on-device again.
- **Tools & Settings tile** from the home grid; the tab remains.
- The separate settings-icon shortcut for storage; the app now asks in-app first and only
  opens the system screen if you agree.

## What CI proved on this exact APK
- 64 release unit tests and lint pass.
- Five TV focus tests and five real key-down/hold/key-up long-press tests pass on a
  disposable API 34 Android TV emulator, including the no-auto-repeat remote.
- Different-signature update rejected without data loss; same-signer update preserved
  favorites, hidden apps, feature choices and a private file.

## Still yours to check (I cannot do these without your TV)
1. **Long press**: hold OK on a tile → menu opens, release does not launch, D-pad moves
   through the options, Back restores focus.
2. **Tools page scroll**: the arrows should now reach every row, including the pill position
   controls under the tabs.
3. **Pill**: no background, five items only, and it moves when you press the corner/nudge
   buttons.
4. **Show or hide apps**: tick an app, confirm it leaves the home grid; untick to restore.
5. Storage access now asks first — confirm the wording is what you want.
