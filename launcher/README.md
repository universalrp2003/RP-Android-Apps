# Elderly Launcher (முதியவர் திரை)

**Current stable release:** 1.4 / code 5

Package `com.universalrp.launcher`. Android 8.0 / API 26 or later.

## Features

- **Readable controls:** Explicit high-contrast foregrounds for app rows, large buttons, favourites and emergency confirmation, including disabled states; palette regression checks require at least 4.5:1.
- **Default home and battery:** A dedicated default-home chooser, live battery percentage/charging information and Back navigation that returns to home without closing it.
- **Contextual permissions:** Contacts when adding a favourite, phone access when calling, and camera access for the torch. No unused notification permission. Never call an emergency number for testing.
- **Browser updates:** No package-install permission or update APK provider. About opens official release pages in the browser; the launcher does not download or install APK updates.

## Permissions and privacy

No account, ads or app analytics. Contacts/favourites stay on the phone. The launcher posts no notifications. Calls, contacts and torch use contextual Android permissions. Project and release links open in the browser; there is no package-install permission.

## Release and testing

[Official release](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/elderly-launcher-v1.4) - [APK](https://github.com/universalrp2003/RP-Android-Apps/releases/download/elderly-launcher-v1.4/Elderly-Launcher-v1.4.apk) - [release notes](../releases/elderly-launcher-v1.4.md).

Published APK: 11,514,811 bytes; SHA-256 `7cfcde5ec9500281d50704cac17c94e9915c07c0e53dc7bdf3daab5c48899232`.

The owner installed the version 1.4 candidates and supplied Play Protect screens saying This app looks safe, then confirmed All good. These releases promote the same immutable candidate APKs from run 40485186570, not new binaries. This is owner phone evidence, not an independent security audit or a guarantee about future scans. Stop if any security warning appears.

Owner screenshots show readable controls and the default-home chooser. The chooser alone does not establish role completion. All OEM permission flows, torch behavior, larger font settings and every screen are not individually verified by the supplied screenshots.

[Publication workflow](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40487465425) passed package/version, signer, size/hash and promotion provenance checks. [Detailed evidence and limitations](../docs/2026-10-27-device-follow-up.md). The earlier warning’s precise cause and the separate signing-trust concern are not declared resolved.

## தமிழ்

பெரிய பொத்தான்கள், முக்கிய நபர்கள், அவசர அழைப்பு உறுதிப்படுத்தல் ஆகியவற்றில் தெளிவான எழுத்துகள் உள்ளன. முகப்புத் திரைத் தேர்வு, மின்கல விவரம், பின்செல்லல், தேவைக்கேற்ற அனுமதிகள் உள்ளன. புதுப்பிப்புகள் உலாவியில் திறக்கும். சோதனைக்காக அவசர எண்ணை அழைக்க வேண்டாம்.

புதிய பதிப்புகளை உரிமையாளர் தொலைபேசியில் நிறுவினார். பாதுகாப்புச் சோதனை ஏற்றுக்கொண்ட படங்களை அளித்து அனைத்தும் நன்றாக இருப்பதாக உறுதிப்படுத்தினார். சோதித்த அதே கோப்புகளே வெளியிடப்பட்டுள்ளன. இது தனிப்பட்ட சாதனச் சோதனை; அனைத்துச் சாதனங்களுக்கும் எதிர்காலச் சோதனைகளுக்கும் உத்தரவாதம் அல்ல. பாதுகாப்பு எச்சரிக்கை வந்தால் நிறுத்துங்கள்.
