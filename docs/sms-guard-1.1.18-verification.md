# SMS Guard 1.1.18 / code 24

Publication authorized: yes. On 24 November 2026 the owner asked for SMS delivery reports and an SMS archive option, chose a per-message archive with a separate Archived view and an inline status under each sent bubble (no failure notification), installed the CI candidate on his own phone twice, and wrote verbatim: "Yes its all ok you can publish now".

Scope: SMS Guard only. Preserve other app assets, all previous releases, existing permissions, consent, default-SMS role, messages and preferences. No uninstall or data clearing. New version/code, same package and established replacement signer. No new permissions; one new private receiver (`.service.SmsStatusReceiver`, `exported=false`, no intent-filter). No MMS, Play Protect, audio or Google certification is claimed beyond the facts listed below.

## Candidate

Source `0d0f3e3` (feature `85c1f7a`, compile/test fixes `2343e4f`, status-label fix `0d0f3e3`). Candidate runs **45372490739** (from `2343e4f`) and **45376638202** (label-fixed, from `0d0f3e3`), artifact `SMS-Guard-signed-candidate`. Both passed `compileReleaseKotlin`, the JVM unit tests (`SmsDeliveryStatusPolicyTest`, `SmsDeliveryProgressTest`), release lint, signing and the Android 35 default-role emulator smoke (artifact `SMS-Guard-default-role-emulator-evidence`).

Owner phone test, 24 November 2026: both candidates installed in place over 1.1.17 (App info shows 1.1.18 (24)); Play Protect "This app looks safe" on that one phone — one phone's observation, not a Google clearance; a message sent on the owner's Vodafone Idea SIM showed "Delivered ✓✓" from the network's delivery report (the first candidate showed a duplicated "✓✓ Delivered ✓✓" wording, fixed in `0d0f3e3` and confirmed on the second install); the Archive button and the 📦 Archived chip were present in Tamil and English. Not exercised on the phone: archiving a message end-to-end, and a failed-send reason.

568 Python checks passed locally and `tools/verify_apps_localization.py` was clean. The verifier's stale Oor pin (`version = "1.1.17"`, which had been satisfied accidentally by SMS Guard's old line) was corrected to Oor's real `1.1.26`.

## Publication complete

Publisher **45380209608** (marker commit `e63cf69`, which changed only the release note and AGENTS.md) rebuilt from the same source tree and signing key as the second candidate, verified package `com.universalrp.smsguard`, versionCode 24, non-debuggable, signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`, and published release **456643246**, asset **786837941**, `SMS-Guard-v1.1.18.apk`, **12,873,680 bytes**, SHA-256 `22c3f7ed4dee0533679022a9099cfe0a55da2a1bc60099ebd3bd5a28f6997222`, at 2026-11-24T11:03:55Z. GitHub's server-side asset digest equals that SHA-256. The published file is not the owner-installed file (same source and key, rebuilt). Website and Store-catalog pins (pages.yml, site/index.html newest-first, catalogs, release-dates, SisterApps `publishedVersion`) follow in PR #210; Pages deployment verification pending.
