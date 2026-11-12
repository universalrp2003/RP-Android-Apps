# Tamilan Store 0.6 — fresh-install signing migration

**Version:** 0.6 · **versionCode:** 6 · **Package:** `com.universalrp.tamilanstore`.

This release changes the Android signing identity because the former key was exposed. **Tamilan Store's updater implementation and behavior are unchanged.** The package identity remains the same, but Android will not update the old-key installation with this APK.

**Install warning:** Download this official APK first and export/back up anything important. Then uninstall the old Tamilan Store and install this version through Android. Uninstalling may erase local data and may require installing the apps you need again. Do not uninstall before obtaining this APK. Stop if Play Protect warns; do not bypass it.

The exact replacement-key APK was not installed on a phone. CI verification is not a device test or Play Protect clearance. The previous 0.5 tag and asset remain unchanged.

## தமிழ்

**பதிப்பு:** 0.6 · **versionCode:** 6 · **செயலி:** `com.universalrp.tamilanstore`.

பழைய கையொப்பத் திறவுகோல் வெளிப்பட்டதால் Android கையொப்பம் மாற்றப்பட்டுள்ளது. **தமிழன் அங்காடியின் புதுப்பிப்பு வசதியும் நடத்தையும் மாற்றப்படவில்லை.** செயலி அடையாளம் அதேபடி இருக்கும்; ஆனால் பழைய கையொப்ப APK-இலிருந்து இந்த APK-ஐ நேரடியாகப் புதுப்பிக்க Android அனுமதிக்காது.

**நிறுவும் எச்சரிக்கை:** முதலில் இந்த அதிகாரப்பூர்வ APK-ஐப் பதிவிறக்கி, முக்கியத் தகவலை ஏற்றுமதி அல்லது காப்புப்பிரதி செய்யவும். பின்னர் பழைய தமிழன் அங்காடியை நீக்கி Android வழியாகப் புதியதை நிறுவவும். நீக்கினால் உள்ளூர் தரவு அழியலாம்; தேவையான செயலிகளை மீண்டும் நிறுவ வேண்டியிருக்கலாம். APK கிடைக்கும் முன் நீக்க வேண்டாம். Play Protect எச்சரித்தால் நிறுத்துங்கள்; அதைத் தவிர்க்க வேண்டாம்.

இந்தப் புதிய கையொப்ப APK கைபேசியில் நிறுவிச் சோதிக்கப்படவில்லை. CI சரிபார்ப்பு கைபேசி சோதனையோ Play Protect ஒப்புதலோ அல்ல. பழைய 0.5 வெளியீடும் கோப்பும் மாற்றப்படவில்லை.
