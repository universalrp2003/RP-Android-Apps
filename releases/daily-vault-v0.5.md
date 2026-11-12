# Daily Vault 0.5 / code 5

## தமிழ்

புதிய முகப்பு, தெளிவான செலவு/குறிப்பு தேர்வு, தேடல், வகை வாரியான மாதச் செலவு, தேதி/நேரத் தேர்வுகள் மற்றும் தனி காப்புப் பகுதி. தமிழ்/ஆங்கிலம் மற்றும் போனின் ஒளி/இருள் தோற்றம் ஆதரவு. பழைய "preview 0.2" வாசகம் இல்லை; உண்மையான நிறுவிய பதிப்பு காட்டப்படும்.

செயலியைத் திறக்கக் கடவுச்சொல் அல்லது உயிரளவியல் பூட்டு வேண்டாம். காப்பெடுக்கும்போது கடவுச்சொல்லைத் தேர்வு செய்யவும்; அந்தக் கோப்பை மீட்டெடுக்க அதே கடவுச்சொல் தேவை. பழைய கடவுச்சொல் மட்டும் கொண்ட கோப்பை மீட்க அதன் கடவுச்சொல் ஒருமுறை தேவைப்படலாம். திறக்க முடியாத பழைய பதிவுகள் அழிக்கப்படாது. திரைப்பிடிப்பு/திரைப்பதிவு அனுமதிக்கப்படும்; திறந்த போனைப் பயன்படுத்துபவர் பதிவுகளைப் பார்க்கவும் பிடிக்கவும் முடியும்.

0.4 போன்ற ஒரே கையொப்பப் பதிப்பிலிருந்து **Update** செய்யவும்; செயலியை நீக்கவோ தரவை அழிக்கவோ வேண்டாம். பழைய வேறு கையொப்பப் பதிப்பில் நிறுவல் முரண்பாடு வந்தால் நிறுத்தி, முதலில் காப்பெடுத்து மீட்டெடுத்துச் சோதிக்கவும். Uninstalling may erase local data. காப்புக் கடவுச்சொல் மறந்தால் மீட்பு இல்லை.

## English

- Redesigned Overview / Records / Backup navigation, real today/month/all-time spending totals, monthly category breakdown and recent activity. No fake balance or sample records.
- Clearly selected Spending / Notes tabs, localized category defaults, searchable/editable records, confirmed deletion and scrollable editors with native date/time pickers. Failed saves keep the draft open.
- No recurring app-opening password, biometric prompt or lock settings. Existing convenience-mode credentials remain readable. Old password-only vaults may need their original password once for recovery; unreadable records are never silently reset.
- Each export explicitly asks for a 10–256 character password and confirmation. Restore asks for that backup's password and replacement acknowledgement; it authenticates and validates completely before replacing local records. Wrong/corrupt files must not replace current data. Legacy TDV v1 backups stay compatible.
- Local records remain encrypted in private atomic storage using a device-local Keystore-wrapped credential. Portable backup encryption is independent of that device credential. No automatic backup and no forgotten-password recovery.
- Screenshots, recordings and recent-app previews are allowed by owner request. Anyone using the unlocked phone can view/capture records. Use the phone's screen lock; this is not an independently audited password manager.
- No ads/advertising SDK. INTERNET remains only for the existing daily foreground public release-metadata check; no ledger, note, backup or password upload. Biometric permission removed; no new dangerous permissions.

## Installation and evidence limits

Package `com.universalrp.dailyvault`, version 0.5 / code 5, minimum Android 8 / API26, target36. Same private signing identity as public0.4: certificate SHA256 `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`.

Use an **in-place update** from the same-key release. Do not uninstall or clear data. If an older key-mismatched build refuses the update, stop and export/test a backup before considering the documented signing migration. Uninstalling may erase local data. Old0.4 release assets remain unchanged.

The owner supplied earlier UI photos and authorized this redesign/build/publication, not a phone test of0.5. CI/JVM/emulator evidence is recorded separately; no physical-device, installed-hash, OEM behavior or Play Protect clearance is inferred from it. No new dangerous permission is requested.

### Restore storage-failure safety

A failed local file write must leave both the previous encrypted records and their credential wrapper unchanged. When a previously unavailable wrapper must be recreated, it is saved only after the atomic file commit. If credential storage then fails, the committed restored records remain recoverable using the known backup password, and the app explicitly warns that one-time recovery may be needed. It never commits data under an unstored random key in that recovery path.
