# Live Guard 2.75 — permission center, clear first-run defaults, and fuller news briefs

**Version:** 2.75 · **versionCode:** 80 · **Package:** unchanged.

Live Guard now has a dedicated Permissions screen that shows the current Android grant state, explains what each permission is used for, and opens the matching Android request or settings page. The settings and first-run descriptions now match what the app actually scans: hidden scanning and installed-app filtering start enabled, while broader duplicate, download, and large-file categories remain opt-in. Spoken news briefs can include up to eight headlines, with a two-hour interval by default. The free assistant remains usable without an API key.

This source change does not declare any additional Android permissions. CI release checks compare the built APK with the existing public release before publication; they are not a device-install test.

**Install warning:** Version 2.75 uses the same replacement signing identity as 2.74, so it can update a 2.74 installation normally. Android cannot update installations signed with the exposed pre-2.74 key. If you are on an older version, download this official APK first, export or back up anything important, then uninstall the old app and install this APK through Android. Uninstalling may erase local data. Do not uninstall before obtaining the replacement APK. Stop if Play Protect warns; do not bypass it.

The exact 2.75 APK was not installed on a phone. CI verification is not a device test, Play Protect clearance, or all-device certification.

## தமிழ்

**பதிப்பு:** 2.75 · **versionCode:** 80 · **செயலி அடையாளம்:** மாற்றமில்லை.

Android வழங்கியுள்ள அனுமதிகளின் தற்போதைய நிலையைப் பார்க்கவும், ஒவ்வொரு அனுமதியும் எதற்காகப் பயன்படுத்தப்படுகிறது என்பதை அறியவும், அதற்கான Android கோரிக்கை அல்லது அமைப்புப் பக்கத்தைத் திறக்கவும் Live Guard-இல் தனி **அனுமதிகள்** திரை சேர்க்கப்பட்டுள்ளது. அமைப்புகளும் முதல் நிறுவல் விளக்கங்களும் செயலி உண்மையில் தேடும் வகைகளுடன் பொருந்துகின்றன: மறைக்கப்பட்ட உருப்படிகளைத் தேடுதலும் நிறுவப்பட்ட செயலிகளை மட்டும் வடிகட்டுதலும் இயல்பாக இயக்கப்படும்; நகல்கள், பழைய பதிவிறக்கங்கள், பெரிய கோப்புகள் போன்ற விரிவான வகைகளைப் பயனர் விருப்பமாகத் தேர்ந்தெடுக்கலாம். குரல் செய்தி சுருக்கத்தில் அதிகபட்சம் எட்டு தலைப்புகள் இடம்பெறும்; இயல்புநிலை இடைவெளி இரண்டு மணி நேரம். இலவச உதவியாளரை API திறவுகோல் இல்லாமலும் பயன்படுத்தலாம்.

இந்த மூலக் குறியீட்டு மாற்றம் புதிய Android அனுமதிகளை அறிவிக்கவில்லை. வெளியீட்டுக்கு முன் CI உருவாக்கிய APK-ஐ ஏற்கெனவே வெளியிடப்பட்ட பதிப்புடன் ஒப்பிடும்; அது கைபேசியில் நிறுவிச் சோதிப்பதற்கு மாற்றாகாது.

**நிறுவும் எச்சரிக்கை:** பதிப்பு 2.75, பதிப்பு 2.74-இன் அதே மாற்றுக் கையொப்ப அடையாளத்தைப் பயன்படுத்துகிறது; ஆகவே 2.74-இல் நேரடியாகப் புதுப்பிக்கலாம். 2.74-க்கு முந்தைய வெளிப்பட்ட பழைய கையொப்பத்தால் நிறுவிய செயலியை Android நேரடியாகப் புதுப்பிக்க அனுமதிக்காது. பழைய பதிப்பில் இருந்தால், முதலில் இந்த அதிகாரப்பூர்வ APK-ஐப் பதிவிறக்கி, முக்கியத் தகவல்களை ஏற்றுமதி அல்லது காப்புப்பிரதி செய்து, பிறகு பழைய செயலியை நீக்கி Android வழியாக இதை நிறுவவும். நீக்கினால் உள்ளூர் தரவு அழியலாம். புதிய APK கிடைக்கும் முன் நீக்க வேண்டாம். Play Protect எச்சரித்தால் நிறுத்துங்கள்; அதைத் தவிர்க்க வேண்டாம்.

இந்த 2.75 APK கைபேசியில் நிறுவிச் சோதிக்கப்படவில்லை. CI சரிபார்ப்பு கைபேசி சோதனையோ Play Protect ஒப்புதலோ அல்லது எல்லா சாதனங்களுக்குமான சான்றிதழோ அல்ல.
