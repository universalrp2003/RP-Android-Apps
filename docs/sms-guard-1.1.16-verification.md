# SMS Guard1.1.16 / code22 — SMS-only default setup

## Authorization

Publication authorized: yes. The owner explicitly requests fixing the default-SMS button, removing the obsolete banner, building and publishing a new release. In the clarification the owner chose **“Allow SMS-only default mode with an explicit warning”**, rather than implementing full MMS first. This choice supersedes the previous unconditional default-role gate; it does not authorize hiding the MMS limitation, claiming Google approval or erasing records.

The owner supplied a1.1.15 Play Protect “This app looks safe” screenshot and a running-app screenshot, then reports another uninstall/reinstall through Store. The new Store screenshot is an **installation-in-progress** panel, not a completed-install or hash record. The running-app screenshot confirms the old default-button/MMS toast and old static banner. Do not claim that these screenshots verify1.1.16, its installed bytes, natural SMS, MMS, voice or all-device Google approval. Screenshots include personal messages; keep them out of public source/evidence artifacts.

## Candidate requirements and current status

Implementation1.1.16/code22 restores Android's native role chooser after persistent explicit SMS-only acknowledgement; cancellation is respected. Previous default metadata, SMS/provider records and existing preferences remain intact. No package/key/permission expansion is intended. Cloud-consent and speech boundaries from1.1.15 remain unchanged. Radio, held Oor and other public assets are unchanged.

The obsolete Play Protect status card is removed from Messages; no live Google status API or replacement “approved” badge is fabricated. MMS support is **not implemented**. Only content-free unsupported-WAP notice counts/warnings are added, with optional notifications and no PDU/body/sender logging, speech, uploads or download attempt. The warning is only possible when an appropriate MMS notice reaches the app; it does not recover missed content.

Pending: current local/static gates; release unit/lint/sign/package/hash; exact-APK Android35 emulator test; final candidate identity; permission comparison; publication and website/Store integration. These outcomes must be filled from actual successful runs, not inferred from this document. No new1.1.16 APK is claimed phone-tested or Google-cleared. An emulator is not a carrier/OEM/Play Protect test.

See [release notes](../releases/sms-guard-v1.1.16.md), [privacy](../PRIVACY.md) and [earlier warning plus owner scan result](sms-guard-play-protect.md).
