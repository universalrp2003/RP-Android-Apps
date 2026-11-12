# SMS Guard1.1.16 · versionCode22 · 31 October 2026

Owner requested a fix for the non-working default-SMS button, removal of the obsolete Play Protect banner, and a new build/publication. After the MMS risk was explained, the owner explicitly selected **SMS-only default mode with an explicit warning**, not full MMS implementation.

## Default SMS now uses Android's chooser

- “Set as default SMS app” first explains in Tamil/English that picture/video and MMS-based group messages are **not supported**. As default, Android can route them only to SMS Guard; content may be missed and switching apps later may not recover it. The sender may need to resend. Cancel leaves the current default unchanged.
- After one-time explicit acknowledgement, Android10+ opens the real `ROLE_SMS` request; Android8/9 uses the system legacy chooser. If a modern OEM does not offer the role, the explicit request opens default-app settings instead. Android still decides availability and the user must confirm; acknowledging the app's warning is not a role grant.
- Cancelling Android's dialog returns to the app, without automatically forcing Settings open. The previous default package is preserved where available, and Controls can switch back. Companion mode remains available for users who need a full MMS messaging app as default.
- Incoming MMS WAP notices now create a content-free local warning/count and an optional system notification rather than being silently ignored. **This does not download, display or recover MMS content**, and cannot guarantee a warning for MMS the app never receives. Dismissing the notice does not recover a message. Notification denial leaves the in-app notice available.

## Obsolete banner removed—not replaced with a fake Google status

The large “Play Protect status: unresolved” card is removed from Messages. It was static release-time text, not a live Google result. The owner supplied a1.1.15 scan saying **“This app looks safe”** and screenshots of the running app, then reports a Store uninstall/reinstall test. That is device-specific owner evidence; the new1.1.16 bytes are **not installed on a phone** as part of release validation, and no universal Google clearance or cause for changed scans is claimed. Keep Play Protect enabled and respect any warning. No bypass, signing/package change, appeal or external sample upload is involved.

MMS limitations and source/privacy details remain available in Controls/About and documentation. Cloud speech consent/sanitization, optional contacts/SIM/send permissions, existing link/bill fixes and all installed records are preserved. This is the same signing identity: **update normally, without uninstalling or clearing app data**. Uninstalling may erase local data. No signing migration is required for same-key installations.

## Verification

Publication requires release JVM tests/lint, package/code/minSDK/nondebuggable/signature/hash checks, actual permission comparison against1.1.15, and the successful exact-APK Android35 emulator role-flow test (disclosure decline, Android chooser/cancellation, acknowledgement persistence, role grant, synthetic incoming SMS and normal-update persistence). The final [verification receipt](../docs/sms-guard-1.1.16-verification.md) records actual outcomes; requirements here are not advance claims of success. Emulator testing is not physical-phone, OEM, carrier MMS or Google Play Protect verification. The release must promote the successful candidate bytes without substituting a later metadata build.

[Warning/history/privacy/source audit](../docs/sms-guard-play-protect.md) · [Public source](https://github.com/universalrp2003/RP-Android-Apps/tree/main/smsguard)

Radio and the held Oor travel-test candidate remain unchanged.

## தமிழ்

இயல்புநிலை SMS பொத்தான் Android-ன் உண்மையான தேர்வைத் திறக்கும் வகையில் திருத்தப்படுகிறது. உரிமையாளர் **MMS வரம்பை அறிந்து SMS-only இயல்புநிலை முறையை** தேர்ந்தெடுத்துள்ளார். முதலில் ஒரு முறை தமிழ்/ஆங்கில ஒப்புதல் கேட்கும். படங்கள்/வீடியோ மற்றும் MMS குழுச் செய்திகளை அனுப்பவோ பெறவோ முடியாது; இயல்புநிலையாக இருந்தால் அவை தவறலாம். பின்னர் செயலியை மாற்றினாலும் கிடைப்பது உறுதியில்லை; அனுப்புநர் மீண்டும் அனுப்ப வேண்டியிருக்கலாம். MMS தேவைப்பட்டால் ரத்து செய்து முழுமையான செய்திச் செயலியை இயல்புநிலையாக வைத்திருங்கள்.

ஒப்புக்கொண்ட பிறகே Android இயல்புநிலைத் தேர்வு திறக்கும்; ரத்து செய்தால் மாற்றமில்லை, Settings தானாக மீண்டும் திறக்காது. MMS அறிவிப்பு வந்தால் உள்ளடக்கமில்லா எச்சரிக்கை காட்டும்; இது MMS பெறுதல் அல்லது மீட்பு வசதி அல்ல. பெரிய பழைய Play Protect பேனர் நீக்கப்படுகிறது. முந்தைய பதிப்புக்கு உரிமையாளரின் போனில் “This app looks safe” காட்டப்பட்டது; அது எல்லாப் போன்களுக்குமான ஒப்புதல் அல்ல. பாதுகாப்பை அணைக்க வேண்டாம்.

அதே கையொப்பத்தில் தரவை அழிக்காமல் வழக்கம்போல் புதுப்பிக்கவும்; மீண்டும் நீக்கி நிறுவ வேண்டாம். இந்தப் புதிய APK-க்கான உண்மையான கைபேசி/கேரியர் MMS சோதனை எனக் கூறவில்லை. CI/எமுலேட்டர் முடிவுகள் மேலுள்ள சரிபார்ப்புக் குறிப்பில் பதிவு செய்யப்படும். Radio மற்றும் பயணச் சோதனைக்கான Oor மாற்றப்படவில்லை.
