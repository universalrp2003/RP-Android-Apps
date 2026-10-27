# Elderly Launcher (முதியவர் திரை)

**Current stable release:** 1.3 / code 4

Package `com.universalrp.launcher`. Android 8.0 / API 26 or later.

Large, predictable home-screen buttons, favourite calls and a live battery reading.

## Unreleased candidate — not in the APK linked below

- Explicit high-contrast text for app rows, large controls, favourites and emergency confirmation, including disabled states. The palette/source regression gate checks six color pairs at 4.5:1 or better and rejects the previous inherited foreground bug.
- Package-install permission and update APK provider removed. About opens official releases in the browser instead of in-app APK download/installation.
- **Play Protect report unresolved. Do not bypass its warning.** No new stable APK has been published. [Investigation, limitations and device checklist](../docs/2026-10-27-device-follow-up.md).

## Published 1.3 features

- **Set default home:** A dedicated button opens Android’s home-role chooser or Home settings. Cancelled selection can be retried.
- **Contextual permissions:** Phone permission when calling, contacts permission when adding a favourite, camera permission for the torch. Denied phone access falls back to the dialler.
- **Live battery status:** Real percentage and charging/full state. Missing readings are labelled unknown rather than shown as zero.
- **Back behaves as home:** Returns from app lists and help pages without closing the home screen. Installed apps retain their real icons.
- **Help and updates:** Built-in Tamil/English guide, About, sister-app links and a verified updater requiring Android installation confirmation.

## Permissions and privacy

No ads, account or analytics. Contacts/favourites stay on the phone. The launcher posts no notifications, so it does not ask for notification access. Internet is used for the optional verified updater; project links open in the browser.

## Release and testing

[Official release](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/elderly-launcher-v1.3) - [APK](https://github.com/universalrp2003/RP-Android-Apps/releases/download/elderly-launcher-v1.3/Elderly-Launcher-v1.3.apk) - [release notes](../releases/elderly-launcher-v1.3.md).

Published APK: 11,514,983 bytes; SHA-256 `33070c5dbd50dc3a4be0d3239ac43eff5ab1e0313e52da8f87d5986816e86745`.

At publication, 1.3 had no physical-device test. The owner subsequently reported working basic behavior and a Play Protect block; not every scenario has been tested. CAMERA is newly declared for the torch. Default-home selection, permission prompts, torch and battery updates still need phone testing. Never call 108 for testing.

## தமிழ்

பெரிய பொத்தான்கள் கொண்ட முகப்புத் திரை. விருப்ப நபர்களை அழைக்கலாம்; மின்கல அளவும் மின்னேற்ற நிலையும் தெரியும்.

முகப்புத் திரையாக அமை பொத்தானில் இதை இயல்புநிலை முகப்புத் திரையாகத் தேர்ந்தெடுக்கலாம். அழைக்கும்போதும் தொடர்பைச் சேர்க்கும்போதும் விளக்கை இயக்கும்போதும் தேவையான அனுமதி கேட்கப்படும். பின்செல் பட்டியல்களிலிருந்து முகப்புத் திரைக்குத் திரும்பும்; முகப்புத் திரையை மூடாது.

இந்தப் பதிப்பு தொலைபேசியில் நிறுவிச் சோதிக்கப்படவில்லை. விளக்கிற்காக கேமரா அனுமதி புதிதாகச் சேர்க்கப்பட்டுள்ளது. முகப்புத் திரைத் தேர்வு, அனுமதிகள், விளக்கு, மின்கல நிலை சாதனத்தில் சோதிக்கப்பட வேண்டும். சோதனைக்காக அவசர எண்ணை அழைக்காதீர்கள்.
