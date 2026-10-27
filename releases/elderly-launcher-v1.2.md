# Elderly Launcher 1.2

## Three things the owner found

**The volume buttons did nothing.** They were wired to the media stream only. Media volume is inaudible when nothing is playing, so the button moved a number nobody could hear and read as "not working". Both buttons now act on whichever stream is relevant at the moment - the ring volume when the phone is idle, the media volume while something plays - which is exactly what the phone's own hardware buttons do. They also bring up the system's own volume slider, so a press has a visible result even when the volume is already at its limit and cannot move.

**The launcher was invisible everywhere except the home screen.** It answered the home button perfectly and appeared in no app list at all: not in its own list of installed apps, and not in the Settings screen where an app is uninstalled. The cause was a missing `CATEGORY_LAUNCHER` in the manifest - `CATEGORY_HOME` makes an app the home screen, but it is `CATEGORY_LAUNCHER` that makes it an app. Both are now declared. The launcher also used to hide itself from its own list; it no longer does.

**The app list had no icons.** Every other launcher on the phone shows each app's own artwork beside its name, and this one showed a column of names, which is what the owner saw and called "not showing". Each row now carries the app's real icon, loaded from the phone.

## What is unchanged

The eight big buttons in their fixed order, the favourites row, the emergency button, the first-run home prompt and the smaller text are all as they were in 1.1. Nothing was moved, because the owner did not ask for anything to move.

## Honest limits

- **This 1.2 build has not been installed on a phone by anyone**, so it has had no device test.
- Icons come from each app's own published artwork. An app that ships no usable icon gets a tile with its first letter instead.
- Being the home screen is the whole feature of this app, and only a phone can show that it works.

## தமிழ்

**முதியவர் திரை 1.2** - ஒலிப் பொத்தான்கள் வேலை செய்யும், செயலியின் சின்னம் எல்லா இடத்திலும் தெரியும், செயலிப் பட்டியலில் சின்னங்கள் இருக்கும்.

**ஒலி அதிகரிப்பு, ஒலி குறைப்பு**

இரண்டு பொத்தான்களும் ஊடக ஒலியை மட்டுமே மாற்றின. ஊடக ஒலி எதுவும் இயங்காத பொழுது கேட்க முடியாதது - எனவே பொத்தான் ஒரு எண்ணை மாற்றியது, யாரும் அதைக் கேட்க முடியவில்லை, "வேலை செய்யவில்லை" என்று தோன்றியது. உரிமையாளர் அதே சொன்னார்.

இப்போது இரண்டு பொத்தான்களும் அந்த நேரத்திற்குரிய ஓட்டையின் ஒலியை மாற்றுகின்றன - தொலைபேசி செயலற்று இருக்கும்போது ரிங் ஒலி, ஏதாவது இயங்கும்போது ஊடக ஒலி. தொலைபேசியின் சொந்த பொத்தான்கள் செய்வது அதே. மேலும், அழுத்தினால் தொலைபேசியின் சொந்த ஒலிப் பட்டை திரையில் வரும், எனவே ஒலி மாற்ற முடியாத நிலையில் இருந்தாலும், அழுத்துவதற்கு ஒரு தெரியும் முடிவு உண்டு.

**செயலி எங்கும் தெரியாதது**

முகத் திரை பொத்தானுக்கு நன்றாகப் பதிலளித்தது, ஆனால் எந்தச் செயலிப் பட்டியலிலும் தெரியவில்லை: தன் சொந்த செயலிப் பட்டியலிலும் இல்லை, செயலியை நீக்கும் அமைப்புத் திரையிலும் இல்லை. காரணம் - கோப்பில் `CATEGORY_LAUNCHER` இல்லை. `CATEGORY_HOME` ஒரு செயலியை முகத் திரையாக்கும்; `CATEGORY_LAUNCHER` அதை ஒரு செயலியாக்கும். இப்போது இரண்டும் உள்ளது.

தன் சொந்த பட்டியலில் தன்னை மறைத்தது நின்றது - இப்போது அது அந்தப் பட்டியலிலும் உள்ளது.

**சின்னங்கள் இல்லாமை**

தொலைபேசியில் உள்ள மற்ற எல்லா திரைகளும் ஒவ்வொரு செயலியின் சொந்த சித்திரத்தைப் பெயருக்குப் பக்கத்தில் காட்டுகின்றன. இதில் பெயர்களின் ஒரு நீள் பட்டியல் மட்டும் இருந்தது - உரிமையாளர் அதைப் பார்த்து "தெரியவில்லை" என்று சொன்னார். இப்போது ஒவ்வொரு வரிசையிலும் அந்தச் செயலியின் உண்மையான சின்னம் உள்ளது.

**மாறாதவை**

நிச்சயமான வரிசையில் எட்டுப் பெரிய பொத்தான்கள், விருப்ப நபர்கள், அவசரப் பொத்தான், முதல் முறை முகத் திரைக் கேள்வி, சிறிய எழுத்துகள் - எல்லாம் 1.1-ல் இருந்தபடியே. உரிமையாளர் எதையும் நகர்த்தச் சொல்லவில்லை.

**சாதனப் பரிசோதனை நிலை**

**இந்த 1.2 பதிப்பு யாராலும் எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** சின்னங்கள் ஒவ்வொரு செயலியும் வெளியிடும் சித்திரத்திலிருந்து வருகின்றன. சின்னம் இல்லாத செயலிக்கு அதன் முதல் எழுத்து ஒரு தட்டில் காட்டப்படும். முகத் திரையாக இருப்பதே இந்தச் செயலியின் முழு வசதி, அதை ஒரு தொலைபேசி மட்டுமே காட்ட முடியும்.
