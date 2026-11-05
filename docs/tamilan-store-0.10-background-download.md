# Tamilan Store 0.10/code12 — screen-off transfer recovery candidate

## Report and measured source cause

Owner reports turning the screen off stops an in-progress Store download and requires another download. Current public0.9 has two deterministic source problems: MainActivity.onStop calls StoreModel.hidden, which pauses/cancels a DOWNLOADING/READY batch; transport runs in viewModelScope with a180s timeout; every new model deletes cached verified APKs. Individual transfers are also vulnerable to model/process lifetime. No claim that an OEM trace was captured.

## Fix

Use Android DownloadManager for one explicitly requested canonical release URL, storing its ID and frozen validated13-app catalog snapshot privately. Download into an app-specific external `.transfer` file; never expose that raw file to the installer. Screen-off doesn't cancel, and reopening restores the same ID rather than enqueuing again. On completion, bound-copy to a private temporary APK and verify size/hash/package/version/SDK/signers/downgrade before rename. Before Android handoff, verify again. Persist completed-file recovery before removing the transport request. Explicit cancellation removes the system request and state; cancellation joins the observer before cleanup. Model disposal leaves the request intact.

DownloadManager owns redirect/network retries; it receives only a public release URL, no cookies, authorization or private identifiers. Hash/identity/signature checks, not server-supplied MIME or redirect identity, authorize the final APK. No new permissions, service, wake lock or battery exemption. Android/OEM may defer downloads under network/storage/power/force-stop restrictions; no uninterrupted-transfer guarantee.

A live batch remains in memory and does not open installers while Store is hidden. After process death only the current chosen transfer is recovered; approvals for remaining batch apps are intentionally not persisted or auto-resumed. New review is required. No unattended installation, Play Protect bypass, data clearing or unrelated release.

## Validation status

429 Python checks and localization/source checks pass locally. New native regression downloads a real immutable public DailyVault APK with the emulator screen OFF, checks persisted system ID recovery, verifies the file on model reopening, reopens again without a second request, and checks explicit cancellation removes the ID. This is not actual process-kill/Doze/OEM proof. Separate signed candidate workflow now checks public0.9→0.10 same-key update and full permission continuity. Kotlin/native execution pending CI. No public Store publication authorized or performed in this change.

## First executed results

Source9f67a1e: Android35 screen-off native workflow42058610255 passed both tests (actual public APK download, same system ID, native hash verification, completed-file reopening and explicit cancellation). Signed candidate42058610282 passed public0.9→0.10 same-UID/private-file/language/motion/restart, established signer and complete permission equality. Artifact13430269468 archiveSHA2269ba37ad02139faa3e045f587d2f3a5d466c47e9e5d1bfa8643a8443e70822; candidate19,476,375B/SHA6a14e98925f8a6d72d52c9fbd41a2f36a544a886b4bed9cc79d245418a2af761. These exact bytes are not published. A subsequent single-install foreground guard also prevents a user-tapped verification finishing after screen-off from launching an installer in the background; latest-source CI must pass separately.
