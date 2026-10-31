# Tamilan Store 0.7 candidate verification — 31 October 2026

**Publication authorized: yes — 31 October 2026.** Owner reports the signed Store0.7 candidate working and provides installer/inventory screenshots, then explicitly authorizes publishing Store and Vault and grouping Vault as the normal thirteenth non-Store app. The installed APK hash is not independently matched. Publication is pending this main-only exact-artifact promotion; public Store remains0.6 until verified. Daily Vault remains immutable public 0.5. No website/public feed was advanced, no existing asset replaced and no owner app was uninstalled or cleared.

## Report and confirmed defect

The owner reports Daily Vault working and supplies phone UI screenshots. Store reports download/verification failure after progress; a file-manager screenshot offers an initial 0.3 → 0.5 Update action. Those screens do not establish completed in-place updating, the installed certificate, APK hash or preservation of previous records.

Store 0.6 source still pinned retired certificate `9e30f4eab8afce5e4ab444ff9578fec354d6d1414d245253590a9703d5985d55` for non-Secret-Message APKs. Current replacement releases/CI use `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`. The mismatch rejects valid current downloads before installer handoff. Secret Message's separate established certificate is unchanged.

Candidate 0.7/code7 corrects that pin, refreshes the bundled fallback to already-published 13-app receipts, distinguishes failure stages, explains the Install action, and supplies read-only URI ClipData for installer forwarding. Installed signer, minimum SDK, package/version, size/hash and downgrade gates remain mandatory. Accepting both certificates would not create Android signing continuity and is not a fix.

## Current code / signed candidate

- Source: `710cf71e3bfa2aaea8f841db944db04ad46d9043` (not the earlier f2ee504 fallback snapshot).
- All **11** workflows for that exact source are authoritative `completed/success`; independent REST recheck found no non-success run.
- Local **219 Python tests**, localization and diff checks passed.
- [Signed candidate workflow 41147678511](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41147678511), Store job `124376508008`, SUCCESS: release JVM tests, lint, signed release build, package/version/code, non-debuggable status and pinned release signer.
- Artifact `13021219269`, `tamilanstore-verified-release-apk`, archive size `15,880,624` bytes; GitHub API archive digest `sha256:0915caff33ffe1847dcd086635ea957af6006b1084e0e7bbd31b94fae025625e`.
- Contains the candidate `Tamilan-Store-v0.7.apk` and its checksum manifest. Archive download in this sandbox was blocked at GitHub's external artifact-storage redirect, so **no local archive/APK-byte re-download verification or inner-APK digest is claimed here**. A future publisher must download and hash the pinned archive, verify its inner manifest/identity/signer/permissions and compatibility with public Store0.6, then verify public bytes after authorized publication. Do not use a debug APK or the superseded f2ee504 artifact for promotion.
- Subsequent bot `72e899a` changed only candidate-build-digests.md and was inspected/fast-forwarded. Its generic receipt describes the earlier f2ee504 build; the current Store artifact above is authoritative for this fix.

## Android 15 verification — PASS

[Preview workflow 41147678510](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41147678510), source710cf71, SUCCESS. The instrumentation target is a **debug Store build** from the same code, not the signed release Store APK. It uses real immutable signed Daily Vault APK fixtures:

| Fixture | Bytes | SHA-256 |
| --- | ---: | --- |
| 0.3 | 12,417,577 | `aa35dca15be265146618c5e53748abc03e6022e4469ab7b6c523f655412043dd` |
| 0.4 | 12,532,289 | `39ff7b7bcabb9f8a53b329d9f244b0d6e754d9a49159f5f3afac00469d5a0dc1` |
| 0.5 | 11,712,552 | `8842606659442a1e2b8c7d28fd2affac56bb40c0bc679acf86785f3167903d55` |

- Fresh fixture passes the real Android archive parser, current certificate pin and immediate pre-handoff revalidation.
- FileProvider URI is content-only, read-only and included in both intent data and ClipData. Android's installer reads it and offers Install confirmation.
- With public0.4 installed, verification accepts the same-key 0.5 update and Android offers confirmation.
- Confirmations are **cancelled** in these probes; assertions check that fresh remains uninstalled and 0.4 remains 0.4. This does not claim completed installation/update or an automatic installer.
- With original public0.3 installed, its certificate does not match the current pin: status is SIGNER_MISMATCH, 0.5 archive verification rejects the cross-key update and installed0.3 remains present. A synthetic private-file marker hash is unchanged. This is not a test of the owner's records or a general data migration guarantee.
- Wrong size, checksum, package and version are rejected with their specific failure stages.
- Only CI emulator fixture transitions remove synthetic test installs. The script refuses non-GitHub/non-emulator devices. No user-data uninstall workaround is introduced.
- Emulator evidence artifact `13021313973`, API archive digest `sha256:9a87ce8f57a0bf8950f5c87d75370c011d5b9557fd0218d3e9da8c2aa9ebc063`, includes phase results and confirmation screenshots; retention14 days. Test reports artifact `13021698652`.

## Limits / next decision

No physical-phone/OEM validation of Store0.7, exact-signed-Store release emulator test, full Store UI/network download-to-completed-install proof, Play Protect clearance, security audit or successful owner 0.3 data migration is claimed. The phone's actual installed0.3 signer and manual-update outcome still need clarification.

Do not uninstall or clear Daily Vault to resolve a signing conflict. Existing usable records must stay accessible. Investigate the installed identity and a safe backup/recovery plan before any migration; never ask the owner to upload private records or passwords. Publication of a new Store release requires owner approval and its own immutable promotion/public-download verification. Keep public Store0.6 and DailyVault0.5 pointers unchanged until then.

## Owner phone report / authorization

Owner reports Store working correctly after obtaining the signed0.7 candidate. Screens show the file manager offering0.6→0.7, Android installing Elderly Launcher1.5, then Store detecting Launcher1.5 and DailyVault0.5 installed with Open actions. This supports installer handoff and status refresh on that phone, not all-app/OEM/PlayProtect certification or a hash-matched DailyVault migration. Owner says no one has an existing Daily Vault installation requiring migration; retain all data/signing safety gates anyway. DailyVault0.5 is already public and must not be republished/replaced. Only Store0.7 is a new APK publication. Website grouping changes to13 regular non-Store apps plus separate Store follow verified publication.
