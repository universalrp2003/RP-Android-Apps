# விலங்கு ஒலிகள் · Animal Sounds 0.22

Fixes from the owner's phone screenshots of 0.21.

- **Tamil text is the same visual size as English.** At the same nominal size, Tamil glyphs are drawn larger and looser than Latin letters, so the Tamil title "விலங்கு ஒலிகள்" and the heading "ஒலியைக் கேட்டு அறியுங்கள்" broke into two lines. Tamil text is now drawn at 85% of the sp size, so both interfaces read at the same size and the title and heading fit on one line at normal font sizes. Spacing and 48 dp touch targets are unchanged, and the system font size is still honoured.
- **Settings shows every detail directly.** "Advanced settings" and "What can it identify?" are no longer dropdowns: the species model file controls and the full coverage text are always visible.
- **About page.** Links now carry only the official website and the GitHub repository (the Tamilan Store and DailyVault download links and the legacy mirror link are gone). **Our apps** is a plain list of all 18 app names, Tamilan Store and DailyVault first, with one button to the official website where the downloads are.
- **Reference photos are on by default.** After each identification the app sends only the species names to Wikipedia/Wikimedia Commons and shows a free-licence photo with its credit and licence links; no audio is sent, and the photo does not confirm what you heard. The switch in ⚙ Settings turns it off (this also applies to existing installations that never changed the switch).
- Version 0.22 (version code 22). Signed with the same key as 0.17–0.21, so it installs over 0.21 in place and keeps the downloaded model and settings.
- **Device test status:** the exact 0.22 bytes were not installed on a phone before publication. CI ran the unit tests and the Android 15 emulator suite (main screen, Settings, Guide and About at 1.0× and 2.0× font scale in both languages). This is not an independent security audit.

## தமிழ்

0.21-இன் தொலைபேசிச் சிறுபடங்களில் உரிமையாளர் சுட்டிய பிழைகளின் திருத்தங்கள்.

- **தமிழ் எழுத்து ஆங்கிலத்துடன் ஒரே தெரிவளவில்.** ஒரே அளவில் தமிழ் எழுத்துக்கள் ஆங்கில எழுத்துகளைவிடப் பெரிதாகவும் தளர்வாகவும் வரையப்பட்டதால், "விலங்கு ஒலிகள்" தலைப்பும் "ஒலியைக் கேட்டு அறியுங்கள்" தலைப்பும் இரு வரிகளில் பிரிந்தன. இப்போது தமிழ் எழுத்து sp அளவில் 85%-ஆக வரையப்படுவதால் இரு மொழிகளும் ஒரே அளவில் தெரிகின்றன; வழக்கமான எழுத்தளவில் தலைப்புகள் ஒரே வரியில் அமைகின்றன. இடைவெளிகளும் 48 dp தொடுப் பரப்புகளும் மாறவில்லை; அமைப்பின் எழுத்தளவும் பின்பற்றப்படும்.
- **அமைப்புகளில் எல்லா விவரங்களும் நேரடியாக.** "மேலதிக அமைப்புகள்" மற்றும் "எதை அறியலாம்?" கீழ்த்தோன்று பட்டிகள் இல்லை: உயிரின மாடல் கோப்புக் கட்டுப்பாடுகளும் முழு உள்ளடக்க விவரமும் எப்போதும் தெரியும்.
- **இந்தச் செயலி பற்றி பக்கம்.** இணைப்புகளில் இப்போது அதிகாரப்பூர்வ இணையதளமும் GitHub களஞ்சியமும் மட்டுமே உள்ளன (தமிழன் கடை, தினக்கணக்கு பதிவிறக்க இணைப்புகளும் பழைய மாற்றுக் களஞ்சிய இணைப்பும் நீக்கப்பட்டன). **எமது செயலிகள்** என்பது எல்லா 18 செயலிகளின் பெயர்களின் தெளிவான பட்டியல் — தமிழன் கடையும் தினக்கணக்கும் முதலில் — மற்றும் பதிவிறக்கங்கள் இருக்கும் அதிகாரப்பூர்வ இணையதளத்திற்கு ஒரு பொத்தான்.
- **மாதிரிப் படங்கள் இயல்பாக இயக்கப்படும்.** ஒவ்வொரு அடையாளத்திற்கும் பிறகு செயலி இனப் பெயர்களை மட்டும் Wikipedia/Wikimedia Commons-க்கு அனுப்பி, நன்றியும் உரிம இணைப்பும் கொண்ட இலவச உரிமப் படத்தைக் காட்டும்; ஒலி அனுப்பப்படாது; படம் நீங்கள் கேட்டதை உறுதிப்படுத்தாது. ⚙ அமைப்புகளில் உள்ள மாற்றியால் அணைக்கலாம் (மாற்றியை ஒருபோதும் மாற்றாத ஏற்கனவே உள்ள நிறுவல்களுக்கும் இது பொருந்தும்).
- பதிப்பு 0.22 (version code 22). 0.17–0.21 உடன் அதே கையொப்ப விசை; 0.21 மீது நேரடியாக மேலெழுதி நிறுவலாம். பதிவிறக்கிய மாடலும் அமைப்புகளும் அப்படியே இருக்கும்.
- **தொலைபேசி சோதனை நிலை:** இந்த 0.22 கோப்பு வெளியீட்டிற்கு முன் தொலைபேசியில் நிறுவி சோதிக்கப்படவில்லை. செயலி சோதனைகளும் Android 15 emulator தொகுப்பும் (முதன்மைத் திரை, அமைப்புகள், வழிகாட்டி, பற்றி — இரு மொழிகளிலும் 1.0× மற்றும் 2.0× எழுத்தளவிலும்) வெற்றி பெற்றன. இது தனிப்பட்ட பாதுகாப்புத் தணிக்கை அல்ல.
