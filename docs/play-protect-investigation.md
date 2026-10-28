# Play Protect warning investigation — 28 October 2026

Owner supplied screenshots now show “Harmful app blocked” for Tamilan Store debug pilot (0.5/code5, displayed 23.16 MB) and Expense Tracker (installed/offered binary hash not established). This is distinct from an unknown-source permission prompt. Earlier “looks safe” screens do not override this new evidence. A functioning UI, successful CI, source availability and matching signer do not prove malware absence or a false positive.

## Current response

- Do not ask users to disable Play Protect, press Install anyway, uninstall working apps, rename/repackage APKs or otherwise evade the warning.
- Pause Store-first suite rollout/publication pending investigation. Withdraw the flagged Store debug pilot as a recommended install. Existing public Store 0.5 is a different release binary; no conclusion about its current Play Protect verdict is established here.
- Ask owner to expand “More details” on the warning and share only the exact detection/reason text. Do not request sensitive phone/DNS logs or credentials.
- Establish the exact blocked APK source, hash, version, signer and merged-manifest permission delta. Compare with the CI artifact and previously published release. A same versionName/code does not establish identical bytes.
- Review live-catalog routing/download/installer behavior and Expense Tracker SMS access. Do not label these permissions the cause without detection evidence.
- If evidence supports a false positive, use Google's official developer appeal process with the exact package/hash and explanation; do not promise approval or that signing alone clears a verdict.

Owner also supplied NextDNS logs: speech.platform.bing.com appears without the red blocked marker visible on nearby ad-domain rows. Entry details and the exact test time are not expanded, so this suggests no visible DNS block for that entry, not proof of successful TLS/WebSocket/audio, all-profile compatibility or a Play Protect cause. Do not copy raw profile IDs/history into the repository or public galleries.

## Voice Notes feedback

Owner says all working but update check says nothing; existing inline result used default Text color within the custom dark screen. Results now use an explicit AlertDialog for success/no-update/error and immediate checking state. Search box removed at owner's request, microphone moved above the notes and padding reduced. HD-only playback/privacy disclosure and biometric gate preserved. Android preview run 40651513745 passed unit tests/lint/assemble (source 4b7156d); device result visibility is still to be checked.

Source-only review checkpoint: Store declares INTERNET, REQUEST_INSTALL_PACKAGES and POST_NOTIFICATIONS; Expense Tracker declares READ_SMS/RECEIVE_SMS and no INTERNET in its source manifest. No ad SDK signature was found in the reviewed Gradle/source search. This is not a merged-APK/dependency audit, does not establish the blocked APK's identity, and does not explain or clear Play Protect's verdict. Removing permissions solely to evade a verdict is not a fix.

## Latest owner evidence — 28 October, 15:08–15:14

Owner reports SMS Guard1.1.10/code16 works, but its installation screenshot explicitly shows “Harmful app blocked.” Candidate distributed was run40662052275/source60e7931, SHA256 a558254afcaa462f0e8a2ff56633b4242aae034c3b8f6ca779519b904b4ba4c7 (installed bytes not independently hash-matched). Later overall Play Protect scan shows “No harmful apps found”; that is a different scan context and does not resolve the earlier install verdict. Withdraw this candidate as a recommended install and hold its public publication too. Do not ask to bypass warning or uninstall/clear financial/SMS data. More details in the block dialog remains collapsed; exact classification is still unknown. Do not declare false positive or installer permission as cause.

Voice Notes1.3/code4 screenshot shows “looks safe,” distinct upgrade from1.2/code3, and functioning localized no-newer-stable update dialog. Owner reports playback cannot be stopped/switched; no claim of complete functional acceptance. Playback-control fix under test as1.3.1/code5, preserving notes/HD-only/privacy and adding deterministic stop/cancellation/late-result tests.

Owner says expanded SMS install warning provides no classification, only “Install anyway.” No further classification evidence is available from this dialog; do not repeatedly ask for it or encourage bypass. Functional acceptance and later clean aggregate scan remain separate evidence. Any developer appeal/review should use the known candidate hash/package and honestly state that classification was not disclosed.


## Confirmed signing-custody finding (independent of warning classification)

A public main CI private-key container and public unlock reference were verified without exporting the key. Certificate-only identity checking is insufficient for that exposed identity. See [signing-key exposure and data-preserving remediation hold](signing-key-exposure-2026-10-28.md). Main-identity publication is explicitly held; Store handoff additionally checks the exact canonical public Store APK bytes and otherwise uses browser. This does NOT identify or clear the Play Protect classification. Do not silently rotate keys, remove apps/data, bypass warnings, or treat deleting a tracked key file as remediation.

Owner has since accepted Voice1.3.2's new-note placement and Stop/switch behavior. That acceptance remains valid for the distributed candidate's observed behavior, not private key custody or universal security certification. Exact-artifact publication was denied HTTP403 and is now also held for signing review. No new public Voice1.3.2 asset was created.
