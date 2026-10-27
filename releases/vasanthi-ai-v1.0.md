# Vasanthi AI 1.0 - the first build, and it has never been on a phone

A new app, built for one person. The owner's sister is deaf from birth: there is no neural connection between her ear and her brain, so she has never heard a sound and she cannot speak. She reads Tamil. Vasanthi AI listens to the room and shows her in Tamil what it heard - what sound is happening, how loud it is, and what someone just said. It is named after her.

## What is in this first build

- **A floating window that is always there.** A draggable circle sits over every other app, so she can be reading a message and still see that someone is at the door. Tap it and the panel opens.
- **It names the sound, in Tamil.** A YAMNet classifier runs entirely on the phone and recognises 521 real-world classes. 194 of them, the ones that actually turn up in a home, carry Tamil names: doorbell, telephone, ringtone, a crying child, a barking dog, breaking glass, a smoke alarm, running water. A class with no Tamil name is dropped rather than shown as English.
- **Every sound shows how sure it is.** The classifier is sometimes wrong. A wrong sound presented as certain is worse than no sound at all, so a confidence bar sits under every name.
- **A loudness bar.** The room in dBFS, drawn as a bar and named in five Tamil words, because someone who has never heard a sound has nothing to compare a decibel number against.
- **It writes down what is said.** Tamil speech recognition through the platform recogniser, shown as it arrives.
- **It vibrates on the sounds that matter.** Doorbell, smoke alarm, crying child, breaking glass. This is the part that works when the phone is in her pocket and she is not looking at it.
- **It saves everything.** Every line of speech and every named sound, timestamped, filterable, deletable, re-readable.

## Two things this app does not do, said plainly

- **It cannot hear a phone call.** Android does not allow any app to capture the other side of a call, at the system level. There is no permission to request and no library that works around it. Speech spoken to her in person is transcribed; a phone call is not. This is stated on the app's own screen, because finding that out after the app is built is worse than being told first.
- **Speech recognition is not perfect.** Good for clear Tamil, weaker in noise and with fast or dialect-heavy speech.

## Privacy, which was the owner's call to make

The app holds `INTERNET` for one reason: the cloud Tamil recogniser is more accurate than the on-device one. **The default is on-device, so audio does not leave the phone unless the owner changes it**, and the floating window always says which mode is live. Sound classification is on-device either way - the YAMNet model runs locally and makes no network call at all.

No location, no contacts, no storage, no camera, no phone state, no accounts, no advertising id.

## Device test status

**Not installed on a phone.** No build of this app has been run on any device, by anyone.

The sound vocabulary, the loudness meter and the history store are covered by unit tests. Everything that actually matters is not: the floating window, the microphone, the classifier on real audio in a real room, the recogniser on real Tamil speech, the permission flow, and whether the Tamil wording is readable on a real screen at a glance. This app exists to be used by one particular person, so it needs her phone before it can be called anything at all.

Two things in particular need her to try: whether the Tamil sound names read clearly at the size the panel uses, and whether the confidence bar tells her anything useful or is just decoration.

## தமிழ்

**வசந்தி AI 1.0**

உரிமையாளரின் சகோதரி பிறந்தாளிலிருந்தே கேட்க மாட்டார். காதுக்கும் மூளையுக்கும் இடையே நரம்பு இணைப்பு இல்லை. அவர் எந்த ஒலியையும் ஒருபோதும் கேட்டதில்லை, பேசவும் மாட்டார். தமிழ் படிக்கத் தெரிந்தவர். இந்தச் செயலி அறையில் நடப்பதைக் கேட்டு, தமிழில் எழுதிக் காட்டுகிறது - எந்த ஒலி, எவ்வளவு சத்தம், யாரோ சொன்னது என்ன.

**இந்தப் பதிப்பில் உள்ளது**

- **எப்போதும் தெரிகும் மிதக்கும் திரை.** மற்ற எந்தச் செயலியின் மேலும் ஒரு சுற்று வட்டம் இருக்கும். செய்தி படிக்கும்போதே கதவுக்கு யாரோ வந்திருக்கிறார்கள் என்று தெரியும். தட்டினால் திரை திறக்கும்.
- **ஒலியைத் தமிழில் பெயர் சொல்லும்.** YAMNet என்ற அறிதல் இயந்திரம் தொலைபேசியிலேயே 521 வகை ஒலிகளை அறிகிறது. வீட்டில் நடக்கும் 194 ஒலிகளுக்குத் தமிழ் பெயர்கள் உள்ளன: கதவு மணி, தொலைபேசி, ரிங்டோன், குழந்தை அழுகை, நாய் ஓட்டம், கீறல், புகை எச்சரிக்கை, ஓடும் தண்ணீர். தமிழ் பெயர் இல்லாத ஒலி ஆங்கிலத்தாகக் காட்டப்படாது, நீக்கப்படும்.
- **ஒவ்வொரு ஒலிக்கும் நம்பகத்தன்மை.** அறிதல் இயந்திரம் சில நேரம் தவறாகச் சொல்லும். தவறான ஒலியை உறுதியாகச் சொல்வது, ஒன்றுமே சொல்லாததை விட மோசம். அதனால் ஒவ்வொரு பெயருக்கும் கீழே ஒரு கோடு இருக்கும்.
- **அறை சத்தக் கோடு.** அறை எவ்வளவு சத்தம் என்று ஒரு கோடிலும், ஐந்துத் தமிழ் வார்த்தைகளிலும் காட்டப்படும். ஒலியை ஒருபோதும் கேட்டதில்லாதவருக்கு டெசிபல் எண் ஒப்பிட எதுவும் இல்லை.
- **பேசுவதை எழுதும்.** யாரோ பேசினால் அதைத் தமிழில் எழுதிக் காட்டும்.
- **முக்கியமான ஒலிக்கு தொலைபேசி அதை நடுத்தும்.** கதவு மணி, புகை எச்சரிக்கை, குழந்தை அழுகை, கீறல். தொலைபேசி பையில் இருந்து, அவர் பார்க்காதபோதும் இது வேலை செய்யும்.
- **கேட்டதை எல்லாம் சேமிக்கும்.** ஒவ்வொரு பேச்சும் ஒலியும் நேரத்துடன் சேமிக்கப்படும். பின்னால் படித்துப் பார்க்கலாம், நீக்கலாம்.

**இது செய்யாதது**

- **தொலைபேசி அழைப்பின் ஒலியைக் கேட்க முடியாது.** ஆண்ட்ராய்டு எந்தச் செயலிக்கும் அழைப்பின் மறுபக்க ஒலியைப் பிடிக்க அனுமதிக்காது. கேட்பதற்கு எந்த அனுமதியும் இல்லை, தவிர்க்க எந்த வழியும் இல்லை. முகமுகமாகப் பேசுபவரின் பேச்சு மட்டுமே எழுத்தாகும். இது செயலியிலேயே எழுத்தாகக் காட்டப்படுகிறது.
- **பேச்சு அறிதல் முழுமையாக இருக்காது.** தெளிவான தமிழுக்கு நன்றாக இருக்கும். சத்தமான இடத்திலும், வேகமாகப் பேசும் போதிலும் குறைவாக இருக்கும்.

**தனியுரிமை**

இணைய அனுமதி இருப்பதற்கு ஒரே காரணம்: இணையத்தில் நடக்கும் தமிழ் அறிதல், தொலைபேசியிலேயே நடக்கும் அறிதலை விட துல்லியமாக இருக்கும். **இயல்பாகத் தொலைபேசியிலேயே அறிதல். உரிமையாளர் மாற்றாவிட்டால் ஒலி வெளியே செல்லாது.** எந்த அறிதல் நடக்கிறது என்று மிதக்கும் திரையில் எப்போதும் காட்டப்படும். ஒலி அறிதல் எப்போதும் தொலைபேசியிலேயே நடக்கும், இணையத் தொடர்பு தேவை இல்லை.

இருப்பிடம், தொடர்புகள், சேமிப்பு, கேமரா, தொலைபேசி நிலை, கணக்குகள், விளம்பர அடையாளம் - இவை எதுவும் கேட்கப்படவில்லை.

**சாதனப் பரிசோதனை நிலை**

**எந்தத் தொலைபேசியிலும் நிறுவப்படவில்லை.** இந்தச் செயலியின் எந்தப் பதிப்பும் எந்தச் சாதனத்திலும் ஓட்டப்பட்டதில்லை.

ஒலிப் பெயர்கள், சத்தக் கோடு, சேமிக்கப்பட்ட விவரம் ஆகியவை அலகு சோதனைகளில் சோதிக்கப்பட்டுள்ளன. முக்கியமானவை அனைத்தும் சோதிக்கப்படவில்லை: மிதக்கும் திரை, ஒலி எடுத்தல், உண்மையான அறையில் உண்மையான ஒலியை அறிதல், உண்மையான தமிழ் பேச்சை எழுத்தாக்குதல், அனுமதி பெறுதல், சிறிய எழுத்தில் தமிழ் படிக்கத் தெரிகிறதா என்பது.

இந்தச் செயலி ஒரே ஒரு நபரைப் பயன்படுத்துவதற்காக உருவாக்கப்பட்டது. அதனால் அவரின் தொலைபேசியில் ஓட்டினால் மட்டுமே இது வேலை செய்யும் என்று சொல்ல முடியும்.
