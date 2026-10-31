# SMS Guard: installation warning, privacy and source audit

Updated 31 October 2026. The owner has authorized a new bug-fix release for family/friends **despite the unresolved installation block**. This is not Google approval or proof that the app is safe.

## What was observed

The supplied Play Protect dialogs say:

> App blocked to protect your device
>
> This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud.

Separate installer screenshots show older versions 1.1.8/code14 and 1.1.9/code15. The warning panels do not show the APK hash/version, so they cannot establish a separate verdict for every file. The owner reports that older versions are also blocked. Stop downgrade/uninstall experiments; uninstalling may erase local data.

[Google's warning guide](https://developers.google.com/android/play-protect/warning-dev-guidance) describes this wording as its internet-sideload/sensitive-permission fraud protection. SMS Guard declares `READ_SMS` and `RECEIVE_SMS`, which support inbox reading and incoming announcements and are among the relevant permissions. This is distinct from a separate “Harmful app blocked” classification. The precise backend decision is not known. Google does **not** universally block every SMS application, and reading aloud is not an established universal cause.

**Installation may remain blocked in 1.1.15. Keep Play Protect enabled and respect the block.** No override, alternate-source evasion, package disguise or signing-key change is recommended. No Google clearance, malware-free guarantee, successful exact-APK phone installation or successful natural-SMS test is claimed. Automated tests cannot establish these. No APK has been submitted to Google or VirusTotal as part of this release work.

## What 1.1.15 changes—and what it cannot fix

- Keeps the package and established signing certificate, so same-key installations retain their normal update path. It does not erase SMS, finance records, settings or accounts. Do not uninstall/reset to try to force installation. An older, different-key installation cannot be updated across Android's signature boundary; stop rather than erase data.
- Includes durable deferred link checks, failed-rescan cache invalidation, HEAD-only redirect probes respecting system DNS, bill-review exclusion and honest link/MMS limitations from the held fixes. Pending/offline is not a safe verdict. Link detection and bill classification remain heuristic.
- Adds a **separate cloud-speech choice, off by default for everyone, including upgrades**. Internet access alone is no longer consent. Tamil/English disclosure explains that enabling HD voice sends spoken text—including ordinary message content, sender names and financial amounts—to Microsoft `speech.platform.bing.com`, including background announcements. Provider handling is outside the developer's control; see [Microsoft's privacy statement](https://privacy.microsoft.com/privacystatement). No retention period or deletion of provider-held text is guaranteed.
- Declining cloud speech uses only an installed system voice reported by Android as non-network. If none exists, the app stays silent and messages remain visible. A selected network system voice also requires cloud consent and Internet access; a third-party system engine has its own provider/privacy behavior. The app cannot audit the engine's implementation merely from its reported voice flags.
- Controls permits review/withdrawal. Withdrawal and Internet OFF stop playback and prevent subsequent cloud requests/retries at the send boundary. They cannot recall text already transmitted or remove provider-held copies. Legacy audio cache is discarded; withdrawal removes the app's new audio cache, not messages or records. Recent synthesized audio is cached app-privately (bounded by clip count); it is not a complete encrypted archive or guaranteed time-based deletion.
- Screens complete sent/ordinary message bodies before truncating; classifies sent OTPs; applies a common speech boundary to automatic, manual, queued, bill and preview speech. Detected secret-bearing text is suppressed, labelled account/masked numbers and other 4+ digit numbers are redacted, and bounded explicitly labelled amounts may remain. These are **conservative heuristics, not a universal secret detector**. Novel secret formats, words and misleading labels can evade detection. Do not enable cloud sharing for messages you cannot share; people nearby may hear local or cloud speech.
- Initial companion setup requests only SMS read/receive and notifications. Contacts/name lookup and phone/SIM labels are optional separate actions. Sending permission is requested only after tapping Send; granting it does not auto-send and the draft stays for another explicit tap. Declarations still include these capabilities; runtime minimization does not remove Google's installation-time sensitive-permission check.
- Incoming MMS is incomplete. New default-SMS setup is unavailable; retain a full messaging application as default. Automatic SMS access cannot be replaced transparently with SMS Retriever, one-time consent or notification access without changing the feature/security model.

## Audit the source and exact release

Anyone can inspect [the public repository](https://github.com/universalrp2003/RP-Android-Apps), [SMS source](https://github.com/universalrp2003/RP-Android-Apps/tree/main/smsguard), [shared source](https://github.com/universalrp2003/RP-Android-Apps/tree/main/shared), [privacy policy](https://github.com/universalrp2003/RP-Android-Apps/blob/main/PRIVACY.md) and [1.1.15 release notes](https://github.com/universalrp2003/RP-Android-Apps/blob/main/releases/sms-guard-v1.1.15.md). The public release/tag and verification receipt link immutable source, signed APK, SHA-256, CI run and permission comparison. Open source makes review possible; it is **not** an independent audit, reproducibility proof or safety certification.

Important audit points: `SpeechPrivacy`, `SmsAnnouncementPlanner`, `SmsVoiceAnnouncer`, `NeuralCloudVoiceEngine`, `GuardSettingsStore`, `SmsRepository`, `IncomingSmsProcessor`, the merged Android manifest, and `.github/workflows/smsguard-test-apk.yml`. The release must use the successful candidate's exact bytes, not an unrecorded rebuild. Historical research and its pre-fix findings are preserved in [the investigation](investigations/sms-play-protect-research-2026-10-31.md).

## தமிழ்

உரிமையாளர் அனுமதியுடன் பிழைதிருத்தப் பதிப்பு வெளியிடப்படுகிறது. “App blocked to protect your device” என்ற Google எச்சரிக்கை இன்னும் தீர்க்கப்படவில்லை. SMS படிக்கும்/பெறும் முக்கிய அனுமதிகளால் நிறுவல் தடுக்கப்படலாம். Google எல்லா SMS செயலிகளையும் எப்போதும் தடுக்கும் என்று கூற முடியாது. பாதுகாப்பை அணைக்கவோ எச்சரிக்கையை மீறவோ வேண்டாம். புதிய பதிப்பின் நிறுவல் அல்லது Google ஒப்புதல் உறுதி செய்யப்படவில்லை.

HD குரலுக்கு தனி ஒப்புதல் தேவை; புதிய நிறுவல் மற்றும் புதுப்பிப்பிலும் அது முதலில் OFF. அனுமதித்தால் பின்னணி அறிவிப்புகள் உட்பட வாசிக்கப்படும் சாதாரண செய்தி உரை, அனுப்புநர் பெயர் மற்றும் தொகை Microsoft சேவைக்கு அனுப்பப்படலாம். ரகசியங்களைக் கண்டறிதல்/எண் மறைத்தல் முழுமையான உத்தரவாதம் அல்ல. மறுத்தால் இணையமில்லா உள்ளூர் குரல் மட்டும்; அது இல்லையெனில் குரல் இல்லை. Controls-ல் ஒப்புதலைத் திரும்பப் பெறலாம்; ஏற்கெனவே அனுப்பிய தகவலை மீட்க முடியாது. இணையத்தை OFF செய்தாலும் அடுத்த மேகக் குரல் கோரிக்கைகள் தடுக்கப்படும்.

அதே கையொப்ப நிறுவலைப் புதுப்பிக்கவும்; நிறுவுவதற்காக நீக்கவோ தரவை அழிக்கவோ வேண்டாம். MMS பெறுதல் முழுமையாக இல்லை; முழுமையான செய்திச் செயலியை இயல்புநிலையாக வைத்திருங்கள். மேலே மூலநிரல், தனியுரிமை மற்றும் சரிபார்ப்பு இணைப்புகள் உள்ளன. யாரும் ஆய்வு செய்யலாம்; திறந்த மூலநிரல் என்பது பாதுகாப்புச் சான்றிதழ் அல்ல.
