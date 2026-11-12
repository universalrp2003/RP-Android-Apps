# Elderly Launcher 1.1 - it offers to become the home screen, calls your people in one tap, and the letters are smaller

The owner installed 1.0 and reported three things. All three were real, and all three are fixed here.

## What the owner reported

- **"Not asking for first time permission to set the fault as a launcher."** Correct. 1.0 registered itself as a home-screen option and then said nothing, so the only way to use it was to find Settings > Default Home screen by hand - which is what the owner had to do, and which nobody who needs this app should have to do.
- **"The letters are showing little bit big."** Correct. Labels were 20sp and the clock 44sp.
- **"Maybe need the important persons one click call button like son daughter brother any other."** A request, and the most useful one of the three.

## What 1.1 does

**It offers to become the home screen on first launch.** One screen appears, and pressing the button opens Android's own home-app chooser with "முதியவர் திரை" in it. There is nothing to configure and nothing to explain in a settings screen, because the system picker is the explanation. It asks once and never again.

**One-tap calling for the people who matter.** A favourites card sits between the button grid and the emergency button. Pressing a name dials that person directly - no contact list, no confirmation, no hunting. Up to eight of them, laid out to wrap so a long name does not push a button off the edge. Adding one opens the phone's own contact picker filtered to numbers, so a contact with no number cannot be chosen. Contacts are read from the phone's own provider and stored on the device; there is no second copy of the address book and nothing is sent anywhere.

**The letters are smaller.** Labels 20sp to 17sp, the clock 44sp to 36sp, dialog text 18-22sp down to 14-17sp. Still large by normal standards - this is a screen for someone who does not read small text - but no longer so large that a two-word label has to wrap.

## What has not changed

The eight buttons, their order, the red emergency button on its own, and the absence of a settings screen. The owner's report did not ask for any of that to move, and the fixed order is the point.

## Permissions

`READ_CONTACTS` is new, and only for the favourites the owner picks. `CALL_PHONE` now serves both the emergency button and the favourites, and still falls back to the dialler with the number filled in when it has not been granted. Everything else is as it was.

## Device test status

**This 1.1 build has not been installed on a phone by anyone, so it has had no device test.** The home screen's shape, the button order and the favourites rules are covered by unit tests. What is not covered, and what 1.0 proved matters: whether the home-app chooser actually appears on first launch, whether a favourite dials, and whether the smaller text still reads at arm's length. A launcher is the one kind of app that cannot be judged from anything but a phone, because being the home screen is the whole feature.

## தமிழ்

**முதியவர் திரை 1.1** - முகத் திரையாக மாற்றுவதை முதலிலேயே கேட்கிறது, நீங்கள் வைத்தவர்களை ஒரே தட்டில் அழைக்கிறது, எழுத்துகள் சிறியவை.

உரிமையாளர் 1.0-ஐ நிறுவி மூன்று விஷயங்களைத் தெரிவித்தார்கள். மூன்றும் உண்மை, மூன்றும் இங்கே சரிசெய்யப்பட்டுள்ளன.

**உரிமையாளர் தெரிவித்தது**

- **"முதல் முறை லான்ச்சராக அமைக்க அனுமதி கேட்கவில்லை."** சரி. 1.0 தன்னை முகத் திரை விருப்பமாக பதிவு செய்து, பிறகு ஒன்றும் சொல்லவில்லை, எனவே பயன்படுத்தும் ஒரே வழி அமைப்பு > இயல்புநிலை முகத் திரை என்பதை கையால் தேடுவது - உரிமையாளர் அதே செய்ய நேர்ந்தது, இந்தச் செயலி தேவைப்படும் யாரும் செய்யக்கூடாதது.
- **"எழுத்துகள் கொஞ்சம் பெரியதாக இருப்பது போல."** சரி. எழுத்துகள் 20sp, கடிகாரம் 44sp.
- **"முக்கிய நபர்களை ஒரே தட்டில் அழைக்கும் பொத்தான் தேவை - மகன், மகள், சகோதரர்."** ஒரு கோரிக்கை, மூன்றில் மிகப் பயனுள்ளது.

**1.1 என்ன செய்கிறது**

**முதல் முறை திறக்கும்போது முகத் திரையாக மாற்றுவதை கேட்கிறது.** ஒரு திரை வருகிறது, பொத்தானை அழுத்தினால் ஆண்ட்ராய்டின் சொந்த முகத் திரைத் தேர்வு "முதியவர் திரை" என்ற பெயருடன் திறக்கிறது. அமைக்க எதுவும் இல்லை, அமைப்புத் திரையில் விளக்க எதுவும் இல்லை, ஏனெனில் அமைப்புத் திரையே விளக்கம். ஒரு முறை கேட்கிறது, மீண்டும் கேட்காது.

**முக்கிய நபர்களை ஒரே தட்டில் அழைப்பது.** பொத்தான் வரிசைகளுக்கும் அவசரப் பொத்தானுக்கும் இடையில் ஒரு விருப்பப் பொத்தான் அட்டை இருக்கிறது. ஒரு பெயரை அழுத்தினால் அந்த நபரை நேரடியாக அழைக்கிறது - தொடர்புப் பட்டியல் இல்லை, உறுதிப்படுத்தல் இல்லை, தேடல் இல்லை. எட்டு வரை, நீளமான பெயர் ஒரு பொத்தானை விளிம்பில் நகர்க்காதபடி சுற்றி அமர்கிறது. சேர்க்க ஒரு பொத்தானை அழுத்தினால் தொலைபேசியின் சொந்த தொடர்புத் தேர்வு எண்களுக்கே வடிகட்டி திறக்கிறது, எனவே எண் இல்லாத தொடர்பைத் தேர்ந்தெடுக்க முடியாது. தொடர்புகள் தொலைபேசியின் சொந்த வழங்கியிடத்திலிருந்து படிக்கப்படுகின்றன, சாதனத்திலேயே சேமிக்கப்படுகின்றன; முகவரிப் புத்தகத்தின் இரண்டாம் நகல் இல்லை, எங்கும் அனுப்பப்படுவதும் இல்லை.

**எழுத்துகள் சிறியவை.** எழுத்துகள் 20sp-ல் இருந்து 17sp-க்கு, கடிகாரம் 44sp-ல் இருந்து 36sp-க்கு, உரையாடல் எழுத்துகள் 18-22sp-ல் இருந்து 14-17sp-க்கு. இயல்பான தரத்தில் இன்னும் பெரியவை - இது சிறிய எழுத்தைப் படிக்காத ஒருவரின் திரை - ஆனால் இரு வார்த்தை எழுத்து சுற்ற வேண்டிய அளவு பெரியவை இல்லை.

**மாறாதவை**

எட்டுப் பொத்தான்கள், அவற்றின் வரிசை, தனியாக உள்ள சிவப்பு அவசரப் பொத்தான், அமைப்புத் திரை இல்லாமை. உரிமையாளர் அதில் எதையும் நகர்த்தச் சொல்லவில்லை, நிலையான வரிசையே இந்தத் திரையின் நோக்கம்.

**அனுமதிகள்**

`READ_CONTACTS` புதிது, உரிமையாளர் தேர்ந்தெடுத்த விருப்பங்களுக்கு மட்டுமே. `CALL_PHONE` இப்போது அவசரப் பொத்தானையும் விருப்பங்களையும் சேவிக்கிறது, வழங்கப்படாவிட்டால் எண் நிரம்பிய டயலரைத் திறக்கிறது. மற்ற அனைத்தும் அப்படியே.

**சாதனப் பரிசோதனை நிலை**

**இந்த 1.1 பதிப்பு யாராலும் எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** முகத் திரையின் வடிவம், பொத்தான் வரிசை, விருப்பங்களின் விதிகள் ஆகியவை அலகு சோதனையில் உள்ளன. இல்லாதவை, 1.0 முக்கியத்துவத்தை நிரூபித்தவை: முதல் முறை திறக்கும்போது முகத் திரைத் தேர்வு உண்மையில் வருகிறதா, ஒரு விருப்பம் அழைக்கிறதா, சிறிய எழுத்து கை நீளத்தில் இன்னும் படிக்கிறதா. லான்ச்சர் என்பது ஒரு தொலைபேசியில் இருந்து மட்டுமே தீர்மானிக்கக்கூடிய ஒரே வகை செயலி, ஏனெனில் முகத் திரை ஆக இருப்பதே முழு வசதி.
