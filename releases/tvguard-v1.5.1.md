## Android TV Guard 1.5.1

Two text defects that were visible in the 1.5 screenshots are fixed. Nothing else about the app
changed: same pill layout, same storage calculation, same permissions, same signing key.

### Fixed

- **The storage note no longer runs into the number.** `11.87 GiB free of 14.68 GiB (limited to the
  physical disk; the filesystem claims more)` — the space before the bracket was missing in 1.5, in
  both English and Tamil.
- **The Expert switches tell the truth about the pill.** They now say the strip draws **five**
  readings, needing only "Display over other apps", and that the top status bar and the floating
  strip are separate switches. 1.5 still said "four readings", claimed the pill needed the top status
  bar, and printed `50%%` instead of `50%`.

### New

- The phone Wi-Fi hub has an **RP App Store** card that opens the official download page from the
  phone already paired with the TV. Installs are unchanged: the APK is opened from the hub's file
  list and Android asks you to confirm.

### Installed and checked on a real TV

The owner installed this exact build on a Sony Bravia and reported it working, with the strip
reading `78% · 996 MB · 11.9 GB · 46.0 °C · good`, the storage audit line reading
`11.87 GiB free of 14.68 GiB (limited to the physical disk…)` with the space present, and the Expert
copy reading "five readings" and "does not affect the floating status pill".

CI on the same bytes: **72 unit tests**, five D-pad focus tests on an Android TV emulator, a same-key
upgrade keeping favourites, hidden apps, feature choices and a private file, and a different-signer
install rejected without deleting the old app's data.

### Install

Same package, same key, higher version code — install over 1.5 without uninstalling. Favourites,
hidden apps and feature choices are preserved. Android TV 8.0 or newer.

- `Android-TV-Guard-v1.5.1.apk`
- SHA-256 `aea1ae03d2bfe87ee0659cd2cba8104e22365c1c18391587d9ae7b70cae3614b`
- 11,254,309 bytes

This is not a Play Protect clearance, and it is not a guarantee for every Android TV model. Stop if
Android warns; do not bypass a security prompt.
