# System App Disabler 0.5.1

Safety guidance update for System App Disabler (Android 8.0+).

- Clearer English and Tamil guidance before disabling: change one app at a time, test functions you depend on, and keep important data backed up separately.
- Known camera and gallery package names marked Recommended in the existing UAD database are treated as Caution, including Google Photos. The list is incomplete; Caution is not a guarantee of safety and unlisted packages remain unlisted.
- Recovery wording now explains that Enable can fail, Shizuku may need restarting, and this app cannot help if the device cannot start. No app is disabled automatically and this app does not back up app data.

The exact signed release candidate passed unit tests and a same-key 0.5.0-to-0.5.1 upgrade on a disposable Android TV emulator with synthetic saved data. TV navigation was tested without disabling a real package. This exact APK has not been tested on a physical phone or TV. Android and manufacturer behavior varies, and neither disabling nor recovery is guaranteed.

Update in place without uninstalling if Android accepts the signature. Before changing system apps, keep your own external backups. Do not bypass device security warnings.

## தமிழ்

- செயலியை முடக்கும் முன் ஒரு செயலியை மட்டும் மாற்றி, கேமரா, படங்கள், அழைப்புகள் போன்ற தேவையான அம்சங்களைச் சோதிக்க வழிகாட்டுதல் சேர்க்கப்பட்டுள்ளது.
- சில கேமரா மற்றும் படத்தொகுப்புச் செயலிகள் கவனத்துடன் ஆய்வு செய்யப்படுகின்றன. பட்டியல் முழுமையானது அல்ல; பாதுகாப்புக்கு உறுதி இல்லை.
- Enable மூலம் மீட்டெடுக்க முடியாமல் போகலாம். போன் தொடங்காவிட்டால் இந்த ஆப்பால் உதவ முடியாது. முக்கியத் தரவைத் தனியாகக் காப்புப் பிரதி எடுக்கவும்.
- இந்த APK எமுலேட்டரில் சோதிக்கப்பட்டது; உண்மையான போன் அல்லது டிவியில் இன்னும் சோதிக்கப்படவில்லை.
