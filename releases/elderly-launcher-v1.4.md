# Elderly Launcher 1.4

## Readable controls

- Explicit white foregrounds replace inherited purple text on app rows, large controls, favourites and emergency confirmation. Disabled states have explicit readable colors too.
- The theme defines foreground roles consistently. Regression checks require six palette pairs to meet at least 4.5:1 contrast and reject restoration of the inherited-foreground bug.
- Existing default-home selection, contacts, favourite calls, battery readings, torch, Back navigation and guides remain. Never call an emergency number for testing.

## Update capability and device evidence

- Removed package-install permission and the update APK provider. Official release pages open in the browser; the launcher no longer downloads or installs APK updates itself.
- On 27 October 2026 the owner supplied a Play Protect screen stating This app looks safe for the preceding candidate, confirmed normal operation, and supplied screenshots showing readable launcher labels and the Android default-home chooser. The chooser screenshot alone does not prove the home role was selected.
- The owner then installed version 1.4 / code 5 on a phone, supplied its version information, Play Protect’s This app looks safe screen and readable launcher screenshots, and confirmed All good. This version has been installed on a phone by the owner. Publication promotes the immutable APK from CI run 40485186570 rather than substituting a newly rebuilt APK.
- A Play Protect result is not an independent security audit or a guarantee about future scans. The earlier warning's precise cause was not established. Stop if any security warning appears; do not bypass protection.
- The package and signing identity remain unchanged for upgrades. Version code increases to 5. Signing trust requires a separate reviewed production plan; no identity change is used to evade a classification.

- Promotion provenance: [phone-tested CI run 40485186570](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40485186570), immutable artifact `12724268538`. The release gate checks its original APK receipt and preserves its SHA-256.

## தமிழ்

- செயலிப் பட்டியல், பெரிய பொத்தான்கள், முக்கிய நபர்கள் மற்றும் அவசர அழைப்பு உறுதிப்படுத்தலில் தெளிவான வெள்ளை எழுத்துகள் உள்ளன. முடக்கப்பட்ட பொத்தான்களுக்கும் தனியான தெளிவான நிறங்கள் உள்ளன.
- நிற வேறுபாடுகள் தானாகச் சரிபார்க்கப்படுகின்றன. பழைய படிக்க முடியாத எழுத்து நிறம் மீண்டும் வந்தால் சோதனை தோல்வியடையும்.
- இயல்புநிலை முகப்புத் திரைத் தேர்வு, முக்கிய நபர்கள், தொடர்புகள், மின்கல விவரம், விளக்கு, பின்செல்லல், வழிகாட்டி ஆகியவை தொடர்கின்றன. சோதனைக்காக அவசர எண்ணை அழைக்க வேண்டாம்.
- தொகுப்புகளை நிறுவும் அனுமதி நீக்கப்பட்டுள்ளது. அதிகாரப்பூர்வ வெளியீடுகள் உலாவியில் திறக்கும். முகப்புச் செயலி புதுப்பிப்புக் கோப்புகளைப் பதிவிறக்கவோ நிறுவவோ செய்யாது.
- முந்தைய சோதனைப் பதிப்பை நிறுவியபோது பாதுகாப்புச் சோதனை ஏற்றுக்கொண்டதாக உரிமையாளர் படங்களை அளித்தார். எழுத்துகள் தெளிவாக இருப்பதையும் வழக்கமான இயக்கத்தையும் உறுதிப்படுத்தினார். முகப்புத் திரைத் தேர்வு திறந்த படமும் கிடைத்தது; தேர்வு முடிந்ததாக அந்தப் படம் மட்டும் நிரூபிக்காது.
- புதிய பதிப்பையும் உரிமையாளர் தொலைபேசியில் நிறுவினார். பாதுகாப்புச் சோதனை ஏற்றுக்கொண்ட படங்களையும் தெளிவான முகப்புத் திரைப் படத்தையும் அளித்து அனைத்தும் நன்றாக இருப்பதாக உறுதிப்படுத்தினார். சோதித்த அதே கோப்பே வெளியிடப்படுகிறது; புதிதாகக் கட்டிய வேறு கோப்பு அல்ல. எதிர்காலத்தில் பாதுகாப்பு எச்சரிக்கை வந்தால் நிறுத்துங்கள்; எச்சரிக்கையைத் தவிர்க்க வேண்டாம்.
