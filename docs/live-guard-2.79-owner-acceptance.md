# Live Guard 2.79 owner acceptance and release authorization

Date: 30 October 2026. Publication authorized: yes

After receiving the candidate linked below, the owner said: “Now its very good shape we can publish and finish all unfinished task”. The owner subsequently chose “Include the Store release too”, expressly authorizing the current branch merge and release completion. The earlier website-only PR restriction is superseded by that explicit branch-merge approval. No Store-module delta exists against main and Store 0.6 is already public; do not create an unnecessary Store release or overwrite 0.6.

## Immutable candidate supplied

- Source: `8bd7cf95393cd4f33e21aa005ed650a4c32a921d`
- Successful Build APK run: 41049481796
- Artifact: 12974397087 (`cleansweep-apk`)
- Release APK: `CleanSweep-v2.79.apk`; version 2.79 / code 84
- Bytes: 14,479,951
- SHA-256: `95a050510786f1e4a34746cb0b3dcf5d3a3a032ec61052a94745d8f7719941ac`
- Pinned owner-backed signer: `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`

Promote only these verified candidate bytes, not a new rebuild. Do not use the debug APK. Keep all existing release assets unchanged. Same-key installed versions update in place; do not instruct the owner to uninstall or clear data.

## Evidence and its limits

The three supplied screenshots are named:

- Screenshot_2026-10-30-17-16-40-127_com.universalrp.cleansweep.jpg
- Screenshot_2026-10-30-17-16-48-550_com.universalrp.cleansweep.jpg
- Screenshot_2026-10-30-17-16-50-863_com.universalrp.cleansweep.jpg

They show the voice/settings screen, the last-prepared category report (science 1, cyber safety 1; other categories empty in that selection), checked-translation labels, original English titles and source links. They do not prove that eight stories were available, every item was spoken, background/interval delivery passed, translated meaning is flawless, or Play Protect cleared this APK. The installed file hash is not independently matched. The owner’s earlier positive playback feedback applies to 2.78; do not transfer it into an independently verified exact-byte 2.79 playback claim.

CI passed the candidate's required 510 JVM tests in each Debug/Release variant and APK verification. Promotion must recheck run/artifact identity, unchanged compiled inputs, APK size/hash/package/version/non-debuggable state, minimum SDK, pinned signing certificate and built-permission delta against public 2.75. If a newly added dangerous permission requires device evidence, stop rather than weakening that gate.

Screenshots used on the public website are cropped only to remove the phone status/navigation bars. No UI text or results are changed. This record is owner-reported acceptance, not independent security or universal-device certification.
