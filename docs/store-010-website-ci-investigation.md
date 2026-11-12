# Store0.10 website integration gate investigation

The Store APK is already public as the exact owner-accepted candidate; do not rerun its publisher or rebuild/replace that asset. PR101 website integration was held rather than merging around a failed check.

## Actual blocker

SMS Guard Test APK run42063720168 / check127372348436 passed compile, unit tests, lint, signing, package/permission verification and immutable baseline retrieval. It failed during the Android35 role smoke's initial setup of the public1.1.16 baseline, before the role/receive checks. Its annotation records a timeout waiting for `வேண்டாம் — உள்ளூர் குரல் மட்டும்`, while the visible app-owned dialog was `புதுப்பிப்பு உள்ளது`, the complete website-only1.1.17 update message, `அதிகாரப்பூர்வ இணையதளத்தைத் திற` and `பிறகு`.

This is a real asynchronous UI obstruction, not evidence that the role/receive assertions passed and not a Store transport failure. The shared daily foreground website notice legitimately advertises the newer public SMS release to the old baseline. Test timing determines whether it covers the initial consent choice.

## Narrow test-harness correction

Only the smoke driver and a pure UI recognizer change; no SMS application source, permissions, update behavior, public APK or release metadata change. Recognize the exact four Tamil or English labels of the ordinary1.1.17 website notice, require every labelled node to belong to the SMS app, require an enabled Later button, save the pre-dismissal synthetic emulator XML and tap Later. Re-read the UI and continue the original assertions. Do not open the website or download anything through the UI.

Unknown notices, partial matches, extra security/consent text, foreign packages (including Play Protect/permission controller), disabled controls, crashes and ANRs are not dismissed. The helper has positive Tamil/English and negative regression cases. All original exact-installed-byte, UID, consent decline, role chooser/cancel/restart, system-granted role, synthetic SMS and same-key update/data assertions remain mandatory. No app data is cleared and no permissions are auto-granted as a workaround. The result records the actual count of ordinary notices deferred; a runtime pass must come from a fresh completed CI check, not source tests or the previous failed run.

Local repository suite after this correction: **431 Python tests**, plus localization/source-link checks. Historical430-test receipts remain historical. This change authorizes no SMS release or Store repromotion. After green integration gates, merge PR101, deploy main Pages and require the live metadata plus exact APK/ZIP hash verifier. Website-only0.9 mirror retirement remains solely for the existing800MB budget; its GitHub release assets remain public and unchanged.
