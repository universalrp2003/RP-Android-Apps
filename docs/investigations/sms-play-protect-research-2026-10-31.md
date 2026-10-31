# SMS Guard — Play Protect research and source findings

Date: 31 October 2026. Research only; no app rebuild, submission, publication, permission change, data reset or Google clearance. Oor is unchanged pending the owner's travel test tomorrow morning. Radio1.31 remains the accepted public APK.

## Bottom line

The owner's new20:31–20:33 screenshots identify the **current displayed warning category** as Google's internet-sideload/sensitive-data fraud-protection block. Google's exact backend verdict, per-permission decision and the identity/hash of each blocked APK are not independently established. Do not label it a false positive or promise that changing a version/signature/build will remove it. Separate privacy/redaction defects remain real remediation work, not established causes of this installation block.

Google distinguishes an internet-sideload/sensitive-permission fraud block from a **Harmful app blocked** classification. The historical owner screenshot was reported as the latter for SMS1.1.12/code18. The new screenshots now provide complete wording for the current category, distinct from that earlier historical description. The exact1.1.14 candidate's current phone verdict has not been independently established. [1](https://developers.google.com/android/play-protect/warning-dev-guidance)

## New owner evidence —20:31–20:33,31 October2026

Four screenshots supplied with “Any version its not allowed”:

- Installer identity prompts show `SMS-Guard-v1.1.8.apk`, package com.universalrp.smsguard, version1.1.8/code14 (12.45MB), and `SMS-Guard-v1.1.9.apk`, version1.1.9/code15 (12.46MB).
- Two Google Play Protect dialogs say **“App blocked to protect your device”**, followed by **“This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud.”** The visible action is OK.
- Owner reports older versions are also not allowed. These screenshots do not show completed installations; a block dialog does not itself display version/hash, so do not independently assign either block to a particular APK hash or generalize to every version ever released.
- This wording matches Google's documented sensitive-permission/internet-sideload block. This app's manifest actually declares READ_SMS and RECEIVE_SMS, making that documented mechanism consistent with the observed result. It is not the generic “scan recommended” prompt and not the separately titled “Harmful app blocked” prompt. [1](https://developers.google.com/android/play-protect/warning-dev-guidance)
- The Downloads/file-manager origin is visible. Do not change installation routes, disguise the package/permissions or disable protection to get around the block. Stop testing successively older versions; the evidence is sufficient to focus the investigation.

### Consequences for the plan

The installation is blocked before the app can display its own permission/consent screens. Batching fewer runtime permission prompts or fixing bill/voice logic is useful engineering, but does not remove core READ_SMS/RECEIVE_SMS declarations. Merely renaming/rebuilding/signing/changing version numbers is not an appropriate remedy. Removing only SEND_SMS likewise leaves the two documented core permissions.

For the full automatic SMS feature set, the next legitimate route is a permission/data-flow compliance audit and Google's review of the exact affected APK. A future Google Play release would need its own SMS-permission eligibility/review and is not guaranteed to clear Play Protect. If those requirements cannot be met, an explicitly agreed reduced manual share/paste design is a product alternative, not a hidden fallback that preserves automatic inbox/finance functionality. Keep existing records and full default messaging support safe.

The screenshot identifies a restriction category; it neither establishes that the APK is malware nor certifies it safe. The earlier cloud-speech disclosure and sent-message replay defects still need remediation before asserting policy compliance in an appeal.

## Evidence examined

Public main contains SMS1.1.11. The owner's selected, unpublished SMS1.1.14/code20 is from immutable source `1fb1ba413e4d00cdf78f48e5e1ab6b4ae55a8714`; it was inspected with `git show`/`git grep`, not restored over main. Current research branch: `arena/45c89326-rp-android-apps`.

The exact retained candidate was downloaded from Git blob `de2dbd2c50ed66b357413bfd41a970936e59a5a9` into `/tmp`, independently rehashed and its actual binary manifest parsed:

- File: SMS-Guard-v1.1.14-selected.apk; bytes **12,808,116**.
- SHA256: **946077ea5733da40a4266c5135c39846a05a3708e2571c02d7d6de43722111e6**.
- Package `com.universalrp.smsguard`; version1.1.14/code20; nondebuggable.
- Binary manifest includes READ_SMS, RECEIVE_SMS, SEND_SMS, WRITE_SMS, RECEIVE_MMS, RECEIVE_WAP_PUSH, READ_CONTACTS and READ_PHONE_STATE; also ordinary internet/network/notification/wake/boot/biometric permissions and an AndroidX signature receiver permission.
- No REQUEST_INSTALL_PACKAGES, SYSTEM_ALERT_WINDOW or FOREGROUND_SERVICE permission in this selected APK. No accessibility/notification-listener service or WorkManager SystemForegroundService was found in its parsed application components.
- Prior CI receipt41191133001/artifact13039140616 verified the established signing identity. This research independently verified hash/manifest, not a fresh local apksigner run or Google scan.

The earlier1.1.12 reference hash `aa074c13acd403fe747e8dd492db969a9b4f1efb700ab8723edac90ea4c8e5a0` is historical provenance, not an independently matched installed-phone hash. It must not be confused with the selected1.1.14 hash above.

## Confirmed source findings — not a diagnosis of Google's classifier

All paths below refer to source1fb1ba41, not to an assumed newly fixed APK.

### 1. Sensitive SMS permissions are genuinely declared

Google explicitly documents fraud-protection blocks for internet-sideloaded apps declaring READ_SMS/RECEIVE_SMS and certain other sensitive access. These two permissions are present in the actual APK. This is a plausible explanation **only if the warning matches that category**, not proof that it caused the historical harmful-app classification. [1](https://developers.google.com/android/play-protect/warning-dev-guidance)

Removing both permissions would also remove general automatic inbox/arrival access; it is not a no-feature-loss fix. SMS Retriever/User Consent APIs are for scoped verification use cases, not replacements for this app's entire inbox/finance/readout function. Switching to notification-listener access to evade SMS restrictions is not an appropriate fix; Google lists that access too. [1](https://developers.google.com/android/play-protect/warning-dev-guidance)

### 2. Permission requests are broader than the immediate readout task

`MainActivity.kt`, `requestCompanionSmsPermissions()` (around lines311–323), batches READ_SMS, RECEIVE_SMS, SEND_SMS, READ_PHONE_STATE, READ_CONTACTS and notifications. Send, contacts and SIM access should instead be justified/requested only when their individual features are used, with denied-permission fallbacks. That minimizes exposure but retaining core READ_SMS/RECEIVE_SMS can still encounter Google's block.

### 3. Cloud voice and the written privacy promise do not match

Confirmed data flow:

1. `SmsAnnouncementPlanner.kt` builds bank-name/transaction-amount announcements and, when full-normal-message reading is enabled, includes cleaned ordinary SMS text plus sender identity.
2. `SmsVoiceAnnouncer.kt:speakSingleBlocking()` (around274–291) passes that text to the neural engine when online voice is used.
3. `NeuralCloudVoiceEngine.kt` sends the text as SSML to `speech.platform.bing.com` over a TLS WebSocket. Escaping SSML is not personal-data redaction. The engine may cache the returned audio locally.
4. `GuardSettingsStore.kt` defaults internet, voice and full-normal-message reading to true. A search of the inspected SMS UI did not find a distinct affirmative cloud-speech data-transmission consent gate; this is a bounded source finding, not a device walkthrough.

`PRIVACY.md` says SMS bodies, sender names and bank/UPI amounts are never uploaded to any AI/cloud backend, then describes online speech using bank-name/amount phrases. The first promise is inconsistent with the actual speech data flow; describing cloud speech only as short scrubbed phrases also misses ordinary-message readout.

This does **not** establish that the entire inbox is automatically uploaded, that the developer receives it, or that OTP redaction fails on every incoming path. It does establish that selected spoken SMS-derived text can leave the device and must be described accurately, with meaningful consent and a real decline path. Google's unwanted-software principles require transparency and agreement before unexpected sensitive-data transmission. [1](https://developers.google.com/android/play-protect/mobile-unwanted-software)

### 4. Manual sent-message replay bypasses the normal planner/redaction route

Static trace:

- `SmsRepository.kt:loadEnrichedMessages()` classifies sent rows with `OtpResult(false, null)` rather than running incoming OTP analysis (around482).
- `ui/MainViewModel.kt:replayMessageVoice()` checks that flag, then for a sent row uses `msg.body.take(180)` and calls `SmsVoiceAnnouncer.speak()` directly (around678–685).
- `SmsVoiceAnnouncer.speak()` trims whitespace, not secrets. Online synthesis can then receive that text.

Consequently a synthetic sent message containing an OTP or account digits can bypass the incoming planner's protection on manual replay. This is a source-established unsafe path, **not evidence that the owner actually replayed or transmitted such a message**. All read/replay/preview routes need a shared privacy boundary before either local speech or cloud synthesis; do not rely on an incoming-message classification flag to protect sent messages.

Until corrected, avoid manually replaying sent messages containing secrets. Keeping Play Protect enabled is essential; do not force a blocked install. A temporary choice to disable SMS Guard's own Internet Access prevents this app's neural-speech request, but it also suspends online link checks. An Android-selected system TTS provider can itself be network-backed; do not call every system voice offline. No settings have been changed by this research.

### 5. Default SMS eligibility and MMS remain incomplete

The candidate refuses new default-SMS setup requests because incoming MMS is unimplemented, yet keeps legacy role components/permissions for existing users. `SmsReceivers.kt:MmsDeliverReceiver` remains a placeholder. Do not acquire default status just to qualify for permissions or remove components blindly while an existing install may still be the default handler.

Google Play distribution is a separate policy project, not automatic Play Protect clearance. Play's SMS policy restricts non-default SMS/voice/notification uses and requires eligibility/declarations for exceptions. Financial management and anti-smishing exceptions are not blanket authorization for unrelated personal-message handling; anti-smishing eligibility has additional evidence requirements. A complete default handler would require real messaging/MMS support and user choice. [1](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en)

## Recommended remediation order

1. **Privacy boundary first:** centralize OTP/account-sensitive redaction for every speech path, including manual sent replay; add synthetic regression cases. Correct the website/in-app privacy statements and add a separate bilingual, affirmative cloud-voice disclosure/consent/revocation gate before transmission. Preserve useful HD voice as an informed choice, not a silent feature removal. Review personal audio-cache retention and link-token/URL disclosures too.
2. **Least privilege:** separate send/contact/SIM permission prompts from reading; inventory legacy default-role capabilities and design any migration without deleting SMS/settings/bills or breaking an existing default handler. Never hide capabilities merely to evade detection.
3. **Choose a truthful distribution/product scope:** reviewed companion use with justified permissions/eligible exceptions, or a genuinely complete default messaging app. A manual share/paste-only design can avoid broad SMS access, but loses automatic SMS/finance features and requires the owner's decision. Do not implement that trade-off silently.
4. **New distinct test version if changes are approved:** keep package, signer and all records; preserve the exact1.1.14 artifact. Verify merged manifest, dependency/network inventory, all speech paths with synthetic secrets, offline/privacy toggles, and real device installation with Play Protect ON. A lint/test/antivirus pass is not Google clearance.
5. **Appeal the exact affected APK only after factual review:** retain expanded warning, version/code, APK SHA, certificate, download source and Android/Play Protect details. Submit an evidence-based classification appeal, not a request for advice on an unbuilt future implementation or a claim of compliance before remediation.

## Official appeal form — checked, NOT submitted

Official endpoint: https://support.google.com/googleplay/android-developer/contact/protectappeals (linked by Google's warning guide).

The public form currently asks for email, application package name, **SHA256 of the APK uploaded to VirusTotal**, and additional supporting information limited to1000 characters; developer name is also present. It has reCAPTCHA and states that decisions are final and there will be no response/guidance on future implementations. Reading a form that contains generic “submitted” UI text does not mean a submission occurred. No form was filled, no CAPTCHA completed, no email sent, and no APK uploaded to VirusTotal by this research.

Do not substitute an artifact-ZIP checksum for the APK SHA. Do not upload SMS databases, private messages, OTPs, backups, signing keys or private service keys. Any third-party sample upload should be explicitly approved, and the exact affected sample must be identified first. A scan score neither proves safety nor overturns Google's verdict.

## What is still needed from the owner

The current warning wording has now been supplied: do not ask for it again or request another blocked-install attempt. Before any exact-APK appeal, associate the already tested file with its version/source/SHA if available; block dialogs alone do not establish the hash. Do not substitute the1.1.14 hash for the pictured older files. No SMS/OTP/bank content is needed. Radio's successful installation and Oor's upcoming trip do not resolve this separate SMS finding.

## Validation boundaries

Actual APK SHA/size/binary manifest were checked. Source was read at the selected immutable commit. No emulator/phone/network interception, new Kotlin tests, full dependency/CVE audit, VirusTotal submission, Play Protect verdict lookup or Google appeal was performed. Only this research note and session guidance are changed; production and selected app APKs remain unchanged.

## Primary references

- [1](https://developers.google.com/android/play-protect/warning-dev-guidance) Google's warning categories, remediation guidance and appeal link (page dated18 August2026).
- [2](https://support.google.com/android/answer/2812853?hl=en) Google Play Protect user/developer guidance.
- [1](https://developers.google.com/android/play-protect/mobile-unwanted-software) Disclosure/consent and sensitive-data principles.
- [1](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en) Current Play SMS/Call Log policy; do not substitute its separately labelled preview policy for current requirements.
- Official appeal form above, directly fetched31 October2026; no submission.

## Subsequent owner authorization (31 October 2026)

The owner subsequently explicitly authorized building and publishing a new bug-fix SMS release for family/friends despite the unresolved warning, with clear documentation and source audit access. This supersedes this investigation's earlier “research only / not authorized” status for SMS, not for Oor. Source findings above describe the pre-fix baseline, not a Google diagnosis. SMS-only held fixes were selectively restored, and the distinct1.1.15/code21 adds explicit default-off cloud consent, speech boundaries and permission minimization. See [current public disclosure](../sms-guard-play-protect.md) and [release notes](../../releases/sms-guard-v1.1.15.md) for the implementation and candid limitations. Automated/build/publication results belong in a separate exact-artifact receipt; no phone acceptance or Google clearance follows from this authorization. No appeal/sample upload is authorized or submitted.
