# Android TV Guard 1.1 — verified signed candidate

Status: **built and verified, NOT published.** It is waiting for you to install it on the real
TV and confirm. Nothing has been uploaded to a release yet.

## Exact build

| Field | Value |
| --- | --- |
| Package | `com.universalrp.tvguard` (unchanged) |
| Version name / code | `1.1` / `3` |
| APK SHA-256 | `510cf0bf305342a61bfe382034376636e114c41123260d1714a2d14533c2f919` |
| APK size | 11,337,740 bytes |
| Signing | the existing stable release key (same signer as the copy on your TV) |
| CI run | [42962761364](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/42962761364) |
| Download | the run's artifact `tvguard-1.1-stable-signed-tested-candidate` |

`verification.json` and a matching `.sha256` ship inside that artifact. Check the SHA-256 before
installing if you want to be certain it is the tested file.

## What CI actually proved on this exact APK

- 70 release unit tests and lint passed.
- Four TV focus tests and four real key-down/hold/key-up long-press tests passed on a
  disposable API 34 Android TV (1080p) emulator.
- A different-signature update was rejected without deleting existing data.
- A same-signer version-code 1 → 3 update preserved favorites, hidden apps, feature choices and
  a private file.

## What CI cannot prove — please check these on the TV

1. **Long press on your remote.** Hold OK on a tile: options open, releasing OK must not launch
   the app, and the D-pad must move between the options. Then Back to dismiss.
2. **Microphone shortcut.** Open Expert → the mic row and press your remote's candidates
   (Menu hold, voice/info keys). Tell me which one your TV actually delivers; none is claimed
   to work globally.
3. **AI answers.** Pick "Free" (no key) or your own provider key, ask a question by voice and
   confirm the spoken answer. Your questions go to that provider, and paid providers may charge.
4. **Status overlay.** Grant "display over other apps", confirm the pill is ~50 % transparent,
   readable, and that the remote still navigates underneath it.
5. **Storage pill.** Confirm the usable free figure and the diagnostic warning are shown as two
   separate, honestly labelled numbers.

## If this is your first install of this key

The earlier test copy may need one uninstall before this APK installs (that deletes its private
data). I have not assumed that happened.
