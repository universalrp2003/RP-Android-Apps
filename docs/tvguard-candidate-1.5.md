# TV Guard 1.5 — candidate for the Sony Bravia

**Status: built, CI-verified, not published. Waiting for the physical test.**

## The file

| | |
|---|---|
| Artifact | `tvguard-1.5-stable-signed-tested-candidate` (run 43006502886) |
| APK inside | `Android-TV-Guard-1.5-signed-candidate.apk` |
| Size | 11,253,454 bytes |
| SHA-256 | `60040a431bacfb1e866ae401470541af276bcf8ff74ec3cf75468e3fa0a674b9` |
| Package | `com.universalrp.tvguard` |
| Version | `versionName 1.5`, `versionCode 7` |
| Signing | same key as 1.4, installs straight over it |

## The storage figure, and why 1.4 did not fix it

You were right that it still showed 25.9 GB, and your own audit screen explains exactly why.
It reports:

- `/data` filesystem: **32.00 GiB total, 25.90 GiB available**
- Candidate block device: **15.76 GB (14.68 GiB), 30777344 sectors**

So the *filesystem itself* is overstating its size: it claims 32 GiB on a chip that holds
14.68 GiB. 1.4's rule was "free space must fit inside the volume's own capacity" — and 25.9
fits inside a fake 32, so the check passed and the impossible number was printed anyway.

Only the hardware size cannot be inflated, and the app already reads it (`/sys/block/mmcblk0/size`)
for that audit screen. 1.5 caps the visible figure with it:

- when a filesystem claims more capacity than the disk has, its free space is **scaled to the
  physical disk**, which preserves the proportion (81% free still reads as 81%) while making
  the absolute number one the device can physically hold;
- the audit screen now has a row, **"Free space shown in the pill"**, printing the exact figure
  the pill uses and saying plainly when the filesystem overstated itself. The two can no
  longer disagree;
- the rule lives in one function, `StorageTruth.cap()`, and is tested against your TV's real
  numbers — 25.90 of 32.00 GiB capped by 15.76 GB — plus a truthful filesystem and a device
  with no readable size.

Expected on your TV: about **12.7 GB free** instead of 25.9 GB, which is roughly what a 14.68 GiB
chip with 81% free actually holds.

## Everything else

Nothing else changed in 1.5. The seven fixes from 1.4 (Down reaching Hardware Truth, the
storage card appearing only when needed, the camera in the top bar, the recently-used apps
list, no hearts, the guide, Tamil) are carried over unchanged, and you have already confirmed
them working.

## What CI proved, and what it did not

69 unit tests, five D-pad/focus tests, a different-signature APK rejected, a same-key APK
updating in place with favourites, hidden apps, feature choices and private files preserved,
and the installed APK's hash matching the artifact byte for byte — all on a disposable
Android TV emulator with this exact APK.

Not proved: the new storage number on your set. That is the one thing to check.

## A note on the build itself

This round the sandbox checkout picked up the wrong base commit mid-work and briefly mixed the
TV Guard changes with the rest of the repository. All of it was recovered from a backup and
the branch was rebuilt on the correct commit; `git log` shows only TV Guard commits and the CI
digest commits, and the APK above is built from that clean branch. Mentioning it because a
"verified" claim should say what happened, not just the happy ending.
