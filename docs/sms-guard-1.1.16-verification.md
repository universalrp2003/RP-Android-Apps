# SMS Guard1.1.16 / code22 — SMS-only default setup

## Authorization

Publication authorized: yes. The owner explicitly requests fixing the default-SMS button, removing the obsolete banner, building and publishing a new release. In the clarification the owner chose **“Allow SMS-only default mode with an explicit warning”**, rather than implementing full MMS first. This choice supersedes the previous unconditional default-role gate; it does not authorize hiding the MMS limitation, claiming Google approval or erasing records.

The owner supplied a1.1.15 Play Protect “This app looks safe” screenshot and a running-app screenshot, then reports another uninstall/reinstall through Store. The new Store screenshot is an **installation-in-progress** panel, not a completed-install or hash record. The running-app screenshot confirms the old default-button/MMS toast and old static banner. Do not claim that these screenshots verify1.1.16, its installed bytes, natural SMS, MMS, voice or all-device Google approval. Screenshots include personal messages; keep them out of public source/evidence artifacts.

## Candidate requirements and current status

Implementation1.1.16/code22 restores Android's native role chooser after persistent explicit SMS-only acknowledgement; cancellation is respected. Previous default metadata, SMS/provider records and existing preferences remain intact. No package/key/permission expansion is intended. Cloud-consent and speech boundaries from1.1.15 remain unchanged. Radio, held Oor and other public assets are unchanged.

The obsolete Play Protect status card is removed from Messages; no live Google status API or replacement “approved” badge is fabricated. MMS support is **not implemented**. Only content-free unsupported-WAP notice counts/warnings are added, with optional notifications and no PDU/body/sender logging, speech, uploads or download attempt. The warning is only possible when an appropriate MMS notice reaches the app; it does not recover missed content.

Selected source: `8bc7f5e620c7f3656e17017dd263c6f059a9b544`. Successful signed [candidate41219146110](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41219146110), job124590221381, artifact13049163137 (`SMS-Guard-signed-candidate`), archive SHA-256 `469b25dd879e0e459e5a7adcb2afbe343d05eb4bc867f16da3968caafbdd5eca` (12,342,174 bytes). Selected APK `SMS-Guard-v1.1.16.apk`: **12,824,500 bytes**, SHA-256 `db279d30f2ff32606cf808172c91686b3641cdf2f07eb113813a03ddd74ba2df`; retained Git blob `87f52eb14ac70be77c631bb181d9ee788caa0fe7`. Independently downloaded only to `/tmp` and size/hash matched. Similar size to1.1.15 does not mean identical bytes; the version/code/hash differ.

Native release JVM tests, lint, signed assembly, package/code22/nondebuggable/signature/hash checks and the exact unchanged release APK emulator flow passed. Emulator evidence artifact13049407362, archive SHA-256 `8cc84213c19e3c7bd8a74ba22002b718cbc9e7876039d6f69ef3c4b438462654`: Android35; disclosure decline preserves default, Android chooser opens, cancelling preserves previous default without forced Settings, acknowledgement survives process restart, real system UI grants ROLE_SMS, synthetic incoming SMS is visible, and a normal same-key `adb install -r` preserves role/message. This result is not phone/Google/MMS testing. Evidence contains synthetic emulator data only.

Selected-source SMS stable verification41219130630 and HD/shared voice41219130682 also succeeded. Local253 Python tests, bilingual/source checks, script syntax and whitespace checks passed.

[Exact-byte publisher41219792154](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41219792154), job124591780045 **succeeded**. It independently verified pinned run/source/artifact archive, signed APK/hash and pinned synthetic emulator evidence; built manifest code22/min26/nondebuggable and established signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`; actual permission comparison against hash-pinned1.1.15 found **no new dangerous permissions**. Manifest/permission source is unchanged. No rebuild or overwrite. Draft assets were re-downloaded and SHA-checked, then final canonical public metadata matched.

Public release **430049093**, APK **695875803**, checksum **695875811**, published **2026-10-31T16:28:10Z**. [Download1.1.16](https://github.com/universalrp2003/RP-Android-Apps/releases/download/sms-guard-v1.1.16/SMS-Guard-v1.1.16.apk). Public API digest/size independently matched after publication.1.1.15 assets remain unchanged.

Website, both feeds, Store embedded catalog and Pages mirrors now select this exact version/hash; historical mirror retained. Other app records remain byte-for-byte unchanged. CI-before-merge and main Pages/live verification remain the final integration gates. No new1.1.16 APK is claimed phone-tested or Google-cleared. An emulator is not a carrier/OEM/Play Protect test.

See [release notes](../releases/sms-guard-v1.1.16.md), [privacy](../PRIVACY.md) and [earlier warning plus owner scan result](sms-guard-play-protect.md).
