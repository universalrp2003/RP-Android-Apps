# Live Guard 2.61 (`versionCode = 66`) — Home-Screen Weather Widget (`மழை & புயல்` / `Rain & storm`) & Urgent Storm Line

## English Summary

Completing the home-screen widget update queued after the **21 October 2026** device verification of **Live Guard 2.60**, **Live Guard 2.61** integrates real-time **Rain & Storm (`மழை & புயல்`)** radar awareness directly into Live Guard's launcher widgets with **zero added Android permissions and zero passive battery drain**:

1. **4th `View` (`காட்சி`) Card on All Existing Home Widgets — `Rain & storm` (`மழை & புயல்`):**
   - Tapping **`View` (`காட்சி`)** on any existing Live Guard home-screen widget now cycles through **Battery & plan** → **Plan & days** → **Security** → **`Rain & storm` (`மழை & புயல்`)** without needing to remove or re-add your widget.
   - Displays your saved/detected village or town (`ஆலங்குடி` / `Alangudi`), live temperature or active storm badge (`28°C` / `~24m` / `Heavy!` / `Storm!`), wind speed, `Tomorrow.io` radar source, and 12-hour check timestamp.
   - Tapping **`Refresh` (`புதுப்பி`)** while viewing **Rain & storm** triggers an on-demand radar fetch off the main thread; passive launcher updates read cached state only so idle battery drain remains zero.
2. **Dedicated 4th Launcher Widget Preset (`Live Guard • Rain & storm` / `லைவ் கார்டு • மழை & புயல்`):**
   - Adds `CleanSweepWeatherWidget` (`2×2` compact home-screen preset, `exported="false"`) so you can also drag a dedicated **Rain & storm** widget straight onto your home screen beside your Battery, Plan, and Security widgets.
3. **Automatic Urgent Weather Line Across All Widget Views + Smart `Open` (`திற`) Deep-Links:**
   - Whenever an active rain or storm warning (`RAIN_SOON`, `VERY_HEAVY_RAIN`, or `CYCLONE_SEVERE`) is fresh, the bottom brief row on **Battery & plan**, **Plan & days**, and **Security** automatically highlights the live storm alert (`⛈ ...`) and restores its normal summary as soon as skies clear.
   - Tapping **`Open` (`திற`)** on any widget card now jumps directly to its matching screen (**Rain & storm** → `Rain & cyclone alert`, **Plan & days** → `My plan`, **Security** → `Security`, **Battery & plan** → `Health`), including when Live Guard is already open in Recents (`onNewIntent`).

---

## தமிழ் சுருக்கம் (Tamil Summary)

நேற்றைய நேரடி போன் சோதனையில் விடுபட்டிருந்த **முகப்புத் திரை விட்ஜெட் (Home-Screen Widget)** வானிலை வசதிகளுடன், எந்தப் புதிய Android அனுமதியும் சேர்க்காமல் **Live Guard 2.61** வெளியிடப்படுகிறது:

1. **4-வது `காட்சி` (`View`) அட்டை — `மழை & புயல்` (`Rain & storm`):**
   - உங்கள் முகப்புத் திரையில் ஏற்கனவே உள்ள எந்த விட்ஜெட்டிலும் **`காட்சி`** பொத்தானை அழுத்தினால் இப்போது **பேட்டரி & திட்டம் → திட்டம் & நாட்கள் → பாதுகாப்பு → `மழை & புயல்`** என 4 காட்சிகளும் வரிசையாக மாறும்.
   - உங்கள் ஊர் பெயர் (`ஆலங்குடி`), தற்போதைய வெப்பநிலை அல்லது மழை/புயல் நிலை (`28°C` / `~24 நிமிடத்தில் மழை` / `புயல்!`), காற்று வேகம் மற்றும் 12-மணி நேர அளவைத் தெளிவாகக் காட்டும்; **`புதுப்பி`** அழுத்தினால் உடனடியாகப் புதிய ரேடார் தகவலைப் புதுப்பிக்கும்.
2. **தனி `லைவ் கார்டு • மழை & புயல்` விட்ஜெட் தேர்வு:**
   - விட்ஜெட் பட்டியலில் (Widgets Gallery) இப்போது 4-வது தனி விட்ஜெட்டாக **`லைவ் கார்டு • மழை & புயல்`** சேர்க்கப்பட்டுள்ளது.
3. **எல்லா விட்ஜெட்களிலும் தானியங்கி மழை/புயல் எச்சரிக்கை வரி & நேரடி `திற` இணைப்பு:**
   - மழை அல்லது புயல் நெருங்கும்போது மற்ற விட்ஜெட் காட்சிகளின் கீழ் வரியிலும் தானாகவே `⛈` எச்சரிக்கை தோன்றும்; **`திற`** அழுத்தினால் அந்தந்தப் பக்கத்திற்கே நேரடியாகச் செல்லும்.

---

## Verification and device testing

The exact APK was **not installed on a phone before publication** (the immediately preceding **Live Guard 2.60**, **Ramesh Radio 1.18**, and **Oor Arivippu 1.1.4** builds were installed and verified live on physical devices by the owner on 21 October 2026, with Google Play Protect confirming “This app looks safe”). Build, unit/regression (47 suites / 420 tests), localization, manifest permission delta (`0` added permissions), APK and release-asset checks are recorded by the publishing workflow. Passing CI is not a Play Protect approval or an all-device compatibility claim.

Version 2.61 / code 66.
