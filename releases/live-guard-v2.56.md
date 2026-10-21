# Live Guard 2.56

Fixes the 4G ⇄ 5G voice announcement switch and removes raw `2147483647` Android placeholder values from nearby cell-tower rows. Published for **Live Guard only**; no other RP Ideas app version was bumped.

## What changed

- **4G ⇄ 5G voice announcements actually speak when turned on:**
  - Wired the **Announce 4G ⇄ 5G changes** switch directly to `Announcer.Event.RADIO_CHANGE` (`voice_radio_change`) and enabled master speech output when switched on (including a one-time upgrade sync if the switch was already enabled on 2.55 or earlier).
  - Updated `RadioWatchService` to track both **5G SA** (`NETWORK_TYPE_NR` where `overrideNetworkType` stays `NONE`) and **5G NSA / 5G+** (`OVERRIDE_NETWORK_TYPE_NR_NSA` / `NR_ADVANCED`) on the active mobile-data SIM, re-arming the watch on app launch as well as reboot.
  - Added matching **4G ⇄ 5G switches** control in **Voice & daily watch** and Tamil labels on the **4G / 5G split** card.
- **Hidden tower fields no longer display `2147483647`:**
  - Filtered Android's `CellInfo.UNAVAILABLE` (`2147483647`) and `CellInfo.UNAVAILABLE_LONG` (`9223372036854775807`) sentinels across cell IDs, PCI, TAC/LAC and dBm values on 5G, 4G, 3G and 2G cells.
  - Unknown signal strength now displays `—` instead of `2147483647 dBm` and sorts below real measured neighbour dBm readings.
- Added unit tests in `CellTowerAndRadioPolicyTest`. No new Android permission.

## Verification and device testing

Published on 21 October 2026 by [Build APK run 39580876173](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39580876173) from commit `cd55415`: `CleanSweep-v2.56.apk`, `13,825,964` bytes, SHA-256 `7bbb11a922860a8019e0a3cfa4fd7cc1538eaa35c4c9722754f2e08049f1dcdb`.

The exact APK was **not installed on a phone before publication**. Build, unit/regression, localization, APK and release-asset checks are recorded by the publishing workflow. Passing CI is not a Play Protect approval or an all-device compatibility claim.

Version 2.56 / code 61.

## தமிழ்

# லைவ் கார்டு 2.56

**4G ⇄ 5G மாற்ற அறிவிப்பு** குரலில் ஒலிக்காத சிக்கலும், அருகிலுள்ள செல் டவர் பட்டியலில் காட்டப்பட்ட `2147483647` என்ற Android மறைப்பு எண்ணும் இதில் சரிசெய்யப்பட்டுள்ளன. **Live Guard மட்டுமே** புதுப்பிக்கப்பட்டுள்ளது; மற்ற செயலிகளுக்குத் தேவையற்ற புதுப்பிப்பு அறிவிப்பு அனுப்பப்படவில்லை.

- **4G ⇄ 5G குரல் அறிவிப்பு இப்போது வேலை செய்யும்:**
  - **4G ⇄ 5G மாற்றத்தை அறிவி** சுவிட்ச் குரல் இயந்திரத்தின் `RADIO_CHANGE` அனுமதியுடனும் முதன்மை ஒலி அமைப்புடனும் இணைக்கப்பட்டுள்ளது (முன்பே ஆன் செய்திருந்தாலும் தானாக இணைக்கப்படும்).
  - தரவு SIM-இல் **5G SA** மற்றும் **5G NSA / 5G+** ஆகிய இரு வகை மாற்றங்களும் கண்டறியப்பட்டு 20 விநாடி இடைவெளியில் அறிவிக்கப்படும்.
- **செல் டவர் பட்டியலில் `2147483647` நீக்கம்:**
  - Android மறைக்கும் டவர் எண், PCI, TAC மற்றும் dBm மதிப்புகள் (`2147483647`) வடிகட்டப்பட்டு, உண்மையான அளவுகள் மட்டுமே காட்டப்படும்; தெரியாத dBm மதிப்பு `—` எனக் காட்டப்படும்.
- புதிய Android அனுமதி எதுவும் சேர்க்கப்படவில்லை.

### சோதனை நிலை

வெளியீட்டுக்கு முன் இந்தத் துல்லியமான APK-ஐ போனில் நிறுவிச் சோதிக்கவில்லை. Build, unit/regression, மொழிபெயர்ப்பு, APK மற்றும் வெளியீட்டுக் கோப்பு சரிபார்ப்புகள் வெளியீட்டு workflow-இல் பதிவாகும். CI வெற்றி என்பது Play Protect ஒப்புதல் அல்லது எல்லா சாதனங்களிலும் இயங்கும் உத்தரவாதம் அல்ல.

பதிப்பு 2.56 / code 61.
