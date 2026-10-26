# Vasanthi AI 1.0.1 - speech recognition now actually gets the microphone

The owner installed 1.0 on his own phone and sent screenshots, and those screenshots are what this release is built from. Sound naming worked exactly as intended from the very first build: the doorbell, a cough, speech, music and silence were all named in Tamil with a confidence figure, and the saved history filled up. The speech line never did.

## What was wrong

The app was asking the phone to do two things with one microphone.

The classifier needs the raw microphone open continuously, because YAMNet has to be fed about a second of audio at a time to name a sound. The speech recogniser needs the microphone too. But a recognition service runs inside another application's process, usually the Google app, and Android will not let two applications capture audio at once. So the recogniser was asked to start while the classifier still held the microphone. Every attempt was refused, five refusals in a row is the point at which the engine gives up, and the app reported that the phone had no recogniser at all. There was one. Sound naming kept working the whole time, which is why the two halves of the app disagreed about whether speech recognition existed.

## What changed

- **The microphone is handed over, not fought over.** The classifier runs for four seconds, then the recogniser gets a turn of up to six, then the classifier takes it back. Sound naming is the app's primary job and it is never stopped for long.
- **A recogniser that stays silent cannot stop sound naming.** The platform is not obliged to report anything back, so a watchdog takes the microphone back whether or not the recogniser says it has finished. This is the one failure the schedule must not have.
- **Failures are asked about less often.** A recogniser that keeps failing for the same reason now backs off instead of being retried every few seconds, so a phone with no Tamil pack does not spend its day retrying.
- **The reason is named instead of guessed at.** The recogniser can distinguish a microphone another app is holding, no internet, and a Tamil pack that was never downloaded. Those need three different actions from the owner, and "No speech recogniser" sent him looking for a setting that does not exist. The floating window and the main screen both show the reason now, from one shared piece of wording so the two cannot disagree.
- **Nobody having spoken is not a fault.** It is the normal outcome most of the time, and it proves the recogniser is working, so it now clears the warning instead of leaving it on screen.

## Device test status

**The 1.0 build was installed on a phone by the owner**, on his own Xiaomi, and his screenshots are what found the defect above. **This 1.0.1 build has not been installed on a phone by anyone.** The microphone sharing, the error mapping and the wording are covered by unit tests. The microphone handover on a real phone, the recogniser on real Tamil speech, and whether the Tamil wording is readable on a real screen at a glance are not.

Two things still need her to try: whether the Tamil sound names read clearly at the size the panel uses, and whether the confidence bar tells her anything useful or is just decoration.

## தமிழ்

**வசந்தி AI 1.0.1**

உரிமையாளர் தங்கள் சொந்த தொலைபேசியில் 1.0-ஐ நிறுவி, திரைப்படங்களை அனுப்பினார். அதுவே இந்தப் பதிப்பின் ஒரே குறையைக் கண்டுபிடிக்க உதவியது. ஒலி அறிதல் முதல் பதிப்பிலிருந்தே சரியாக வேலை செய்தது: கதவு மணி, இருமல், பேச்சு, இசை, அமைதி - அனைத்தும் தமிழில் நம்பகத்தன்மையுடன் காட்டப்பட்டன, சேமித்த விவரமும் நிரம்பியது. பேச்சு மட்டும் ஒருபோதும் வரவில்லை.

**என்ன தவறு**

தொலைபேசியின் ஒரு மைக்கில் இரண்டு வேலைகளைச் செய்யச் சொன்னோம்.

ஒலிக்குப் பெயர் சொல்ல YAMNet-க்கு தொடர்ந்து மூல ஒலி தேவை. ஒவ்வொரு முறையும் சுமார் ஒரு நொடி ஒலி தர வேண்டும். பேச்சு அறிதலுக்கும் அதே மைக் தேவை. ஆனால் அறிதல் சேவை வேறு ஒரு செயலியில், பொதுவாக Google செயலியில், ஓடும். ஆண்ட்ராய்டு இரண்டு செயலிகளுக்கும் ஒரே நேரத்தில் ஒலி பிடிக்க அனுமதிக்காது. அதனால் ஒலி அறிதல் மைக்கைப் பிடித்திருக்கும்போதே பேச்சு அறிதலைத் தொடங்கச் சொன்னோம். ஒவ்வொரு முறையும் மறுக்கப்பட்டது. ஐந்து முறை மறுக்கப்பட்டால் இயந்திரம் நிற்கும். அப்போது செயலி "இந்தத் தொலைபேசியில் பேச்சு அறிதல் சேவை இல்லை" என்று காட்டியது. இருந்தது. ஒலி அறிதல் அந்த முழு நேரமும் வேலை செய்தது. அதனால் ஒரே செயலியின் இரு பகுதிகள் வேறுவேறு சொன்னன.

**என்ன மாறியது**

- **மைக் மாறி மாறி கொடுக்கப்படும்.** ஒலி அறிதல் நான்கு நொடி ஓடும், பின்னர் பேச்சு அறிதலுக்கு ஆறு நொடி வரை கிடைக்கும், பின்னர் ஒலி அறிதல் மீண்டும் எடுத்துக் கொள்ளும். ஒலி அறிதல் இந்தச் செயலியின் முதன்மை வேலை, அது நீண்ட நேரம் நிற்காது.
- **பதில் சொல்லாத அறிதல் ஒலி அறிதலை நிற்க வைக்காது.** தொலைபேசி எதையும் திரும்பச் சொல்ல வேண்டிய அவசியம் இல்லை. அதனால் அறிதல் முடிந்தது என்று சொன்னாலும் சொல்லாவிட்டாலும், ஒரு காவலாளி மைக்கைத் திரும்ப எடுத்துக் கொள்கிறது. இதுவே இத் திட்டத்தில் நடக்கக் கூடாத ஒரே தவறு.
- **தோல்வியடைந்தால் குறைவாகக் கேட்கும்.** ஒரே காரணத்திற்காகத் தோல்வியடைக்கும் அறிதலை இப்போது தள்ளித் தள்ளிக் கேட்கும். தமிழ்க் கட்டி இல்லாத தொலைபேசி நாள் முழுவதும் மீண்டும் மீண்டும் கேட்காது.
- **காரணம் சொல்லப்படும்.** மைக் வேறு செயலியில் இருக்கிறதா, இணையம் இல்லையா, தமிழ்க் கட்டி பதிவிறக்கப்படவில்லையா என அறிதல் மாற்றிச் சொல்ல முடியும். இவை மூன்றுக்கும் வேறு வேறு தீர்வு உள்ளது. "பேச்சு அறிதல் இல்லை" என்று மட்டும் சொல்வது, இல்லாத ஒரு அமைப்பைத் தேட உரிமையாளரை அனுப்பியது. இப்போது காரணம் மிதக்கும் திரையிலும் முதல் திரையிலும் காட்டப்படும். இரண்டும் ஒரே வார்த்தையிலிருந்து வருகின்றன, வேறுபடாது.
- **யாரும் பேசவில்லை என்பது தவறு அல்ல.** அது பெரும்பாலான நேரம் நடப்பது, அறிதல் வேலை செய்கிறது என்பதற்கு அது தான் ஆதாரம். அதனால் எச்சரிக்கையை அழிக்கும்.

**சாதனப் பரிசோதனை நிலை**

**1.0 பதிப்பு உரிமையாளரின் தொலைபேசியில் நிறுவப்பட்டது**, தங்கள் சொந்த Xiaomi-யில். மேலே சொன்ன குறை அதிலிருந்தே கண்டுபிடிக்கப்பட்டது. **இந்த 1.0.1 பதிப்பு எந்தச் சாதனத்திலும் நிறுவப்படவில்லை.** மைப் பிரிவு, பிழை வரைபடம், வார்த்தைகள் ஆகியவை அலகு சோதனையில் உள்ளன. உண்மையான தொலைபேசியில் மைக் கொடுப்பது, உண்மையான தமிழ் பேச்சை எழுத்தாக்குதல், சிறிய எழுத்தில் தமிழ் படிக்கத் தெரிகிறதா என்பது இன்னும் சோதிக்கப்படவில்லை.

இன்னும் இரண்டு அவரைச் சோதிக்க வேண்டும்: திரையில் பயன்படுத்தப்படும் அளவில் தமிழ் ஒலிப் பெயர்கள் தெளிவாகப் படிக்கிறதவா, நம்பகத்தன்மை கோடு பயனுள்ளதா அல்லது அலங்காரமா.
