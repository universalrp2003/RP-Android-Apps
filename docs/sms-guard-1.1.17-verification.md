# SMS Guard 1.1.17 / code 23

Publication authorized: yes. On 4 November 2026 the owner requested a new SMS Guard build and publication incorporating the merged issue #49 budget-shaping fix (PR #98).

Scope: SMS Guard only. Preserve other app assets, all previous releases, existing permissions, consent, default-SMS role, messages and preferences. No uninstall or data clearing. New version/code, same package and established replacement signer. Online voices remain upstream-dependent; no phone/audio/Google certification is claimed.

Candidate validation pending. The signed candidate workflow now upgrades from the hash-pinned public 1.1.16, checking installed baseline and candidate bytes, UID, role and synthetic message preservation. Publication must use this exact tested candidate without rebuilding. Website/feed updates follow verified publication only.

## Selected candidate

Source `e194b6d9018e0d6a5704f6db738dc91996adf0bd`; run **41827320776**, job **126565272533**, artifact **13320805354**, archive SHA-256 `f4d7b19783570e14f166b6bf01a4c7f051906036c0f6617f1fafc75f95548119`. APK **12,840,884 bytes**, SHA-256 `b9a7befd1e2c2b3afac8272994f374efdf92fa2151b6d4b624e25168566cd8dd`. Established signer `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`. SMS-only behavior preserved.

Unit tests, release lint/build/signature checks and Android35 exact-byte baseline 1.1.16 → candidate upgrade passed, preserving UID, role and synthetic SMS. Evidence artifact13321100027, archive `6c8ed096d1927e23000edd07a69b376e7275a921b3065241f5e76a7db39fec89`. Actual installed APK hash matched. No physical phone/MMS/Play Protect/audio proof. Live probe41825858972 and HD failover41825859178 passed with unchanged runtime inputs; Surya completed at1,643ms with24,192bytes in the SMS lane and was selected in the shared lane. Release verification41827320660 also passed.

426 Python checks passed locally. Publisher must promote these exact bytes, compare full permissions against public1.1.16 and refuse an existing tag.
