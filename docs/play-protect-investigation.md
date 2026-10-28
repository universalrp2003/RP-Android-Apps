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
