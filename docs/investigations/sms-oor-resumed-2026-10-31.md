# Resumed SMS Guard / Oor Arivippu investigation — 31 October 2026

Owner asks to resume fixes after accepting/publishing LG2.81. Scope: new test candidates, not a public release or main merge. Work is on arena/9e83fc74-rp-android-apps. Held c96dd9e9 was fetched and only the two app modules, their owner regression contracts and Oor About gate were reapplied; current LG/site/Store published pointers are retained.

## Confirmed source fixes

### Oor1.1.13 / code18

- Restore district/town separation, explicit district suffix in speech/history, dynamic published About directory and invalid destination coordinate rejection from the held investigation.
- Also reject explicitly district-labelled locality/feature values as town candidates, while permitting a real district capital with the same unlabelled name.
- Preserve existing movement gates, recent-place suppression and legitimate circular-route revisits. No new journey/settings/history format or migration.
- District labels still depend on the phone geocoder; first resolution is not a proven district boundary crossing. A physical journey with these new bytes is still untested.

### SMS1.1.14 / code20

- Restore bill due-date versus causal “due to” regressions and suppress known prior false reminders across daily/immediate/manual delivery without deleting records.
- Restore fail-closed HTTP/redirect/provider handling, HEAD-only probes, versioned positive-cache expiry, no raw-IP DNS-filter bypass and honest URL/request-metadata disclosures.
- Confirmed additional source defect: goAsync SMS processing ran all online URL probes before finish; multiple slow links can exceed Android's broadcast budget. New receiver path runs local heuristics only, stores the SMS first, commits extracted pending URLs and schedules a WorkManager chain. There are no complete SMS bodies, URLs, sender details or keys in WorkManager input/tags.
- Each background pass makes at most three remote URL attempts. Completed URLs leave only their exact message/timestamp queue entry; unresolved URLs/messages rotate fairly, and network-constrained exponential retries retain incomplete checks. The existing bounded local queue/cache are retained. Process death/reboot scheduling is delegated to WorkManager; startup also recovers legacy prefs queues. This is not a universal wall-clock or OEM guarantee, and local provider/prefs work still has platform latency.
- Blocked senders are not probed, completion speech respects updated voice/block/mute/quiet/internet settings, and per-URL completion avoids attributing a mixed-batch threat to the wrong domain. Quiet-time link results remain in the inbox cache; no audible success is claimed for suppressed speech.
- Binary inspection of the initial1.1.14 candidate confirmed WorkManager merged an unused normal FOREGROUND_SERVICE permission/component. The final revision explicitly removes both: only regular, non-expedited jobs are used. This is capability minimization, not a Play Protect diagnosis or concealment. Permission-button wording also discloses the existing send/contact/SIM requests rather than claiming read/receive only; existing defaults now get an amber MMS-unavailable label.
- **MMS remains unimplemented.** New default-SMS role requests are refused with an explanation. Existing default users keep the same package/manifest/signing identity and SMS receipt; use the existing native switch-back action to select their complete messaging app. Do not recommend this as a full default messaging replacement. No automatic role switch, uninstall or deletion occurs.

## Play Protect: separate unresolved investigation, not a solved claim

The owner supplied a “Harmful app blocked” screenshot for SMS1.1.12/code18. A later About screen does not prove clearance. Local reference hash from the earlier investigation is aa074c13acd403fe747e8dd492db969a9b4f1efb700ab8723edac90ea4c8e5a0; the installed-phone hash was not independently measured. The cause/category is unknown. Source findings above are genuine defects but are **not evidence of why Google classified that APK**.

Evidence to retain for a legitimate review:
- Exact blocked APK/package/version/SHA, download origin, expanded warning wording, phone/Android/Play Protect details and scan date (never SMS/OTP/bank content).
- Candidate build source/run/artifact/hash, established signer, merged manifest permissions/components, non-debuggable state and no permission expansion or APK self-installer.
- Product scope and privacy truth: optional link checks contact extracted URL hosts and a DNS threat provider; URLs may contain personal tokens; optional owner-key Google checks transmit URLs. Voice services are separate. No blanket “everything stays on phone” claim.
- Explicit default-MMS limitation and no restricted-settings/security-warning bypass instructions.

Only Google can determine/review its own verdict. No review/appeal has been submitted by this agent, no clearance is claimed, and installing a same-key/new-version candidate does not itself establish a fix. If a new candidate is blocked, stop and retain its exact warning/hash rather than overriding protection. Do not rotate package IDs or signing keys to evade detection. Full MMS support and robust isolation from private-network/DNS-rebinding endpoints remain separate engineering work; HEAD probes are not a sandbox and can expose tokenized URLs or trigger badly designed endpoints.

## Verification status

Local Python, localization and source checks and remote Android unit/lint/signing results must be recorded after execution. No local Android SDK/JDK is available in this sandbox, so no local JVM or device-test pass is implied. No APK is downloaded under /home/user. Public SMS1.1.11/Oor1.1.10 and immutable LG2.81 remain unchanged.
