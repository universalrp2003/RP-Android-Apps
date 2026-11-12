# Vasanthi AI 1.1.1 - speech is the default, the notification is permanent, and English inside Tamil is translated

The owner installed 1.1 on his own phone and sent fourteen screenshots and six points. Every one of them is answered here, and the two that mattered most were about the app not doing the one thing it exists to do.

## Speech is now the only thing on by default

1.1 turned both switches on at once, and that is the reason speech kept missing what was said.

Sound naming and speech want the same microphone. While sound naming holds it, the recogniser is only given the gaps between classifications - a few seconds every few seconds - so a television or a person speaking across a room is heard in fragments or not at all. The owner's words were "its not listening", and he was right.

So **ஒலி is off and பேச்சு is on.** With sound naming off the recogniser has the microphone outright: no sharing, no handover, no gap. It is more accurate and it reaches further, which is the same thing he asked for in the last round and the same thing he is asking for now. Sound naming is still there, one tap away, for when he wants it.

## A permanent notification, so the app never has to be found

The owner asked for a permanent notification he could use to turn listening on without searching for the application. 1.1's notification only existed while the microphone was open, and the moment listening stopped the service, the notification and the bubble all went away - which is exactly the thing he was describing.

Now the service stays alive once it has been started. The notification is ongoing whether or not the microphone is open, and it carries a toggle: **தொடங்கு** when listening is off, **நிறுத்து** when it is on, plus **மூடு** for closing the app properly. Turning both switches off closes the microphone and leaves the notification and the bubble where they are.

The notification's foreground-service type follows the microphone. A microphone-typed service that never touches the microphone is timed out by Android 15 and stopped, and that would have taken the permanent notification with it.

## The interface is much smaller

The owner's words were "still big ui interface big unwanted text", and the screenshots showed exactly what he meant: a full-width green Stop button with a paragraph under it, a block naming a sound with **its English name printed beside the Tamil one**, a room loudness block, and a block with two paragraphs of explanation before either switch. All of it sat above the history.

- **The main screen is now three cards.** One card of controls - status, one button, two switches, one line. One card of speech - the live line, large. The history. That is the whole screen.
- **The English label is gone.** 1.1 printed the recogniser's English name of a sound next to its Tamil name on the main screen, which is an English word on a screen read by someone who reads Tamil. It is not printed anywhere now.
- **The sound and loudness blocks are gone unless sound naming is on.** They used to sit above the speech, and they are four things he did not ask for.
- **The panel leads with speech.** What somebody said is now the first and largest thing on it, and the sound name, its confidence bar, the loudness bar and the loudness word are at the bottom behind the sound switch.
- **The About card is four lines.** It was five feature bullets, a divider, a heading and two limit bullets.

## English inside Tamil is translated

The owner's words were "english to tamil translation posible now its showing thanglish word for english", and this was a real bug, not a matter of taste.

The recogniser is asked for Tamil and gives Tamil - but not *only* Tamil. A name, a word it knows only in English, or a word it heard phonetically and wrote down in Latin letters all arrive *inside* a Tamil sentence. The screenshots show it plainly: ஒலி with a Latin "too" on the end of one word, and "chu" inside another. 1.1 decided whether to translate by testing whether the whole line was mostly Latin. A Tamil sentence with one English word in it is not mostly Latin, so it tested as Tamil and was shown untranslated.

- **The words are found individually now.** Every run of Latin letters in the line is translated and put back in place, and everything Tamil is left exactly as it was.
- **A word that cannot be translated is kept, not dropped.** Losing part of what somebody actually said is worse than leaving one English word in it.
- **The word-finding is unit-tested**, including the exact case that failed: one English word inside a Tamil sentence.

## Device test status

**1.1 was installed on a phone by the owner**, on his own Xiaomi, and the fourteen screenshots he sent are what this release is built from. **This 1.1.1 build has not been installed on a phone by anyone, so it has had no device test.** The word-finding, the store, the cooldowns and the wording are covered by unit tests. Whether speech now keeps up with a television left on, whether the permanent notification survives being left alone for hours, and whether the smaller screen still reads clearly on a small phone are not.

Two things still need her to try: whether the speech line is big enough to read at a glance, and whether the two switches are obvious about which one is doing what.

## தமிழ்

**வசந்தி AI 1.1.1** - உரிமையாளர் தங்கள் தொலைபேசியில் 1.1-ஐ நிறுவி பதினான்கு திரைப்படங்களையும் ஆறு குறிப்புகளையும் அனுப்பினார். அவற்றில் மிக முக்கியமானவை இரண்டு, செயலி செய்ய வேண்டிய ஒரே வேலையை செய்யவில்லை என்பதைப் பற்றியவை.

**பேச்சு மட்டும் இப்போது முன்னிருப்பாக உள்ளது**

1.1-ல் இரு சுவிட்சுகளும் ஒரே நேரத்தில் ஆனாக இருந்தன. பேச்சு தவறவிடுவதற்கு அதுவே காரணம்.

ஒலி அறிதலுக்கும் பேச்சு அறிதலுக்கும் ஒரே மைக் தேவை. ஒலி அறிதல் மைக்கைப் பிடித்திருக்கும்போது, அறிதலுக்கு ஒலி பெயரிடும் நேரங்களுக்கு நடுவே உள்ள இடைவெளிகள் மட்டுமே கிடைக்கும். வினாடிக்கு சில வினாடிகள். எனவே தொலைக்காட்சி அல்லது தொலைவில் நிற்பவரின் பேச்சு துண்டாகவோ, முற்றிலும் அல்லாமலோ கேட்கப்படும். உரிமையாளர் சொன்னார் "கேட்கவில்லை". அவர் சரி.

அதனால் **ஒலி ஆஃப், பேச்சு ஆன்.** ஒலி அறிதல் நின்றால், அறிதலுக்கு மைக் முழுவதுமாகக் கிடைக்கும்: பிரிவு இல்லை, கைமாற்றம் இல்லை, இடைவேலை இல்லை. அது துல்லியமாகவும் தொலைவிலிருந்து வரும் பேச்சையும் பிடிக்கும். ஒலி அறிதல் இன்னும் உள்ளது, ஒரே தட்டில், அவர் விரும்பும்போது.

**நிரந்தர அறிவிப்பு, செயலியைத் தேட வேண்டிய அவசியமில்லை**

உரிமையாளர் கேட்டார்: செயலியைத் தேடாமல் கேட்பதைத் தொடங்க ஒரு நிரந்தர அறிவிப்பு வேண்டும். 1.1-இன் அறிவிப்பு மைக் திறந்திருக்கும்போது மட்டுமே இருந்தது. கேட்பது நின்றாலும் சேவை, அறிவிப்பு, மிதக்கும் வட்டு ஆகியன அனைத்தும் போய்விட்டன. அதுவே அவர் சொன்ன குறை.

இப்போது சேவை தொடங்கிய பிறகு நிற்காது. மைக் திறந்திருக்கிறதா இல்லையா என்பதைப் பொருட்படுத்தாமல் அறிவிப்பு நிரந்தரமாக இருக்கும், அதில் ஒரு மாற்றி உள்ளது: கேட்பது நின்றால் **தொடங்கு**, ஓடும்போது **நிறுத்து**, மற்றும் செயலியை முழுவதுமாக மூட **மூடு**. இரு சுவிட்சுகளையும் ஆஃப் செய்தால் மைக் மூடும், அறிவிப்பும் வட்டும் அப்படியே இருக்கும்.

அறிவிப்பின் முன்புற சேவை வகை மைக்கைப் பின்பற்றுகிறது. மைக்கைத் தொடாத ஒலி வகை சேவையை ஆண்ட்ராய்டு 15 நேரம் முடிந்தது என்று நிறுத்தும், அது நிரந்தர அறிவிப்பையும் கூடக் கொண்டு போய்விடும்.

**இடைமுகம் மிகச் சிறிதாகியது**

உரிமையாளர் சொன்னார் "இடைமுகம் பெரியது, தேவையில்லாத எழுத்து அதிகம்". திரைப்படங்கள் அதைத் தெளிவாகக் காட்டின: நிறைய பச்சை நிற நிறுத்து பொத்தான், அதற்கு கீழே ஒரு பத்தி, ஒலியைப் பெயரிடும் ஒரு பகுதி - அதன் பக்கத்தில் **ஆங்கிலப் பெயரும்** எழுதப்பட்டிருந்தது -, அறை சத்தம் பகுதி, மற்றும் இரு சிவிட்சுகளுக்கு முன் இரு பத்திகள். இவை அனைத்தும் வரலாற்றுக்கு மேலே இருந்தன.

- **முதன்மைத் திரை இப்போது மூன்று அட்டைகள்.** ஒரு அட்டை கட்டுப்பாடு - நிலை, ஒரு பொத்தான், இரு சுவிட்சு, ஒரு வரி. ஒரு அட்டை பேச்சு - நேரலை வரி, பெரியது. வரலாறு. திரை முழுவதும் இதுவே.
- **ஆங்கில எழுத்து போய்விட்டது.** 1.1 ஒலியின் தமிழ்ப் பெயரின் பக்கத்தில் அறிதல் சேவையின் ஆங்கிலப் பெயரை எழுதியது. தமிழ் படிக்கும் ஒருவருக்கான திரையில் அது ஒரு ஆங்கில வார்த்தை. இப்போது எங்கும் எழுதப்படவில்லை.
- **ஒலி மற்றும் சத்தப் பகுதிகள் ஒலி அறிதல் ஆனாக இருக்கும்போது மட்டுமே தெரியும்.** அவை பேச்சுக்கு மேலே இருந்தன, அவை அவர் கேட்காத நான்கு வேலைகள்.
- **திரை பேச்சிலேயே தொடங்குகிறது.** யார் பேசினார் என்பது இப்போது அதில் முதலிலேயே மிகப் பெரியது. ஒலிப் பெயர், நம்பகத்தன்மை கோடு, சத்தக் கோடு, சத்த வார்த்தை ஆகியன ஒலி சுவிட்சுக்கு பின்னால் கீழே உள்ளன.
- **இதைப் பற்றிய அட்டை நான்கு வரிகள்.** ஐந்து அம்சப் புள்ளிகள், ஒரு பிரிப்பான், ஒரு தலைப்பு, இரு வரம்புப் புள்ளிகள் ஆகியன இருந்தன.

**தமிழுக்கு உள்ளே இருக்கும் ஆங்கிலம் மாற்றப்படுகிறது**

உரிமையாளர் சொன்னார் "ஆங்கிலத்தைத் தமிழ் மாற்ற முடியும், இப்போது ஆங்கிலத்தைத் தங்க்லிஷ் வார்த்தையாகக் காட்டுகிறது". இது ஒரு உண்மையான பிழை, விருப்பம் அல்ல.

அறிதலிடம் தமிழ் கேட்கப்படுகிறது, தமிழ் கிடைக்கும் - ஆனால் *மட்டும்* தமிழ் அல்ல. ஒரு பெயர், ஆங்கிலத்தில் மட்டும் அறிந்த ஒரு வார்த்தை, அல்லது ஒலியைப் பொறுத்து லத்தின் எழுத்தில் எழுதப்பட்ட ஒரு வார்த்தை - இவை அனைத்தும் தமிழ் வாக்கியத்துக்கு *உள்ளே* வருகின்றன. திரைப்படங்கள் அதைத் தெளிவாகக் காட்டின: ஒரு வார்த்தையின் முடிவில் லத்தின் "too", மற்றொன்றுக்கு உள்ளே "chu". 1.1 மாற்ற வேண்டுமா என்பதை முழு வரியும் லத்தின் எழுத்தில் இருந்ததா என்று பார்த்து முடிவு செய்தது. ஒரு ஆங்கில வார்த்தையுள்ள தமிழ் வாக்கியம் லத்தின் எழுத்தில் மிகுந்த அளவு இல்லை, எனவே அது தமிழாகவே கருதப்பட்டு மாற்றப்படாமல் காட்டப்பட்டது.

- **வார்த்தைகள் இப்போது ஒன்றுக்கொன்றாகக் கண்டறியப்படுகின்றன.** வரியில் உள்ள ஒவ்வொரு லத்தின் எழுத்துத் தொடர்பும் மாற்றப்பட்டு, அதே இடத்திலேயே வைக்கப்படும். தமிழ் எழுத்துகள் அப்படியே இருக்கும்.
- **மாற்ற முடியாத ஒரு வார்த்தை வைக்கப்படும், விடப்படாது.** யாரோ சொன்னதன் ஒரு பகுதியை இழப்பது, ஒரு ஆங்கில வார்த்தையை விட கெட்டது.
- **வார்த்தை கண்டறிவது அலகு சோதனையில் உள்ளது**, தோல்வியடைந்த கேஸ் உட்பட: தமிழ் வாக்கியத்துக்கு உள்ளே ஒரு ஆங்கில வார்த்தை.

**சாதனப் பரிசோதனை நிலை**

**1.1 உரிமையாளரின் தொலைபேசியில் நிறுவப்பட்டது**, தங்கள் சொந்த Xiaomi-யில். அவர் அனுப்பிய பதினான்கு திரைப்படங்களே இந்தப் பதிப்பின் ஆதாரம். **இந்த 1.1.1 பதிப்பு எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** வார்த்தை கண்டறிவு, கிடங்கு, தாமதங்கள், வார்த்தைகள் ஆகியவை அலகு சோதனையில் உள்ளன. தொலைக்காட்சி நேர்முகமாக ஓடும்போது பேச்சு தங்குவதா, நிரந்தர அறிவிப்பு மணி நேரம் அமைதியாக இருந்தாலும் நிற்கிறதா, சிறிய திரையில் சிறிய எழுத்துகள் தெளிவாகப் படிக்கிறதவா என்பது இன்னும் சோதிக்கப்படவில்லை.

இன்னும் இரண்டு அவரைச் சோதிக்க வேண்டும்: பேச்சு வரி ஒரே பார்வையில் படிக்கப் போதுமான அளவில் பெரியதா, இரு சுவிட்சுகள் எது என்ன செய்கிறது என்று தெளிவாகச் சொல்கிறதவா.
