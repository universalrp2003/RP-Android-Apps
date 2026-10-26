# Vasanthi AI 1.1 - the whole app no longer closes, and two switches instead of one

The owner installed 1.0.1 on his own phone and sent seven screenshots. Two of them mattered more than the rest: one where the app had closed by itself after a few minutes, and one where the recogniser had stopped responding. Everything in this release is built from those.

## The crash

The app was writing its history to disk the wrong way round.

`VasanthiStore.add()` re-read the whole file, parsed it, added one line, and wrote the whole file back - for every single line, on the main thread, the same thread that draws the floating window. And `recordSpeech` had no cooldown at all. So a television playing in the background was recognised again every four seconds, and each of those results rewrote the entire history file from scratch. The file grew, the writes took longer, the main thread spent more of its time blocked on disk, and eventually the system killed the process.

That is the "auto closing". The app was crashing, and it took exactly as long as the history took to grow.

Three changes, and all three are about doing less work per line:

- **The history is read once and then held in memory.** The file is read at startup and never re-read again; adding a line appends to a list.
- **One line is one small write.** A single background thread does the writing, one entry at a time, so the main thread never touches the disk at all.
- **A sentence that repeats is dropped for twenty-five seconds.** The recogniser restarts every few seconds and usually returns the same sentence again. Without the cooldown, a television left on fills the history with the same line dozens of times, and a history nobody can read is worse than no history.

## Two switches, not one

The owner asked for two switches - one for sound listening, one for voice listening - so that one function runs at a time. He is right, and the reason is not tidiness: **both functions need the same microphone, and only one of them can have it properly.**

So there are now two independent switches, ஒலி and பேச்சு, on the floating panel and on the main screen. They can be on together - the microphone is shared, four seconds of sound naming and then a turn of up to twelve for the recogniser - or either can be on alone.

**Turning ஒலி off is the better setting for speech.** With sound naming off, the recogniser has the microphone outright: no sharing, no handover, no four-second gap. It is more accurate and it reaches further, which is the owner's complaint about speech from a little way away. That is what the switch is for, and it is why they are two switches rather than one master.

## On-device recognition is gone

The owner reported that on-device recognition never worked and asked for it to be removed. It has been removed. There is one mode now, it is Google's, and it is the default, because it is the one that actually works on his phone.

The screen says plainly that audio is sent over the internet. That is a real cost and it is not hidden.

## English arrives as Tamil

The owner asked for English speech to be translated to Tamil and shown, and for the English labels to be replaced with Tamil. Both are done.

- **English speech is translated on the phone.** The recogniser is asked for Tamil and usually gives Tamil, but not always: a name, a word it knows only in English, or someone who actually spoke English. Those lines are now translated with ML Kit's on-device English-to-Tamil model before they are written down. The model is about thirty megabytes and downloads the first time it is needed, so a household that speaks only Tamil never pays for it.
- **Every English label is gone.** The panel, the main screen and the history rows are Tamil only now. The English name of the recogniser used to be printed beside each saved line, and it is not written at all any more.

## The beeping is stopped

The owner called it "ding ding ding" and asked for it to stop completely. The recogniser's own service plays a beep on every session it starts, and a recogniser that restarts every few seconds therefore beeps every few seconds. There is no setting anywhere that turns it off, so the streams it plays on are silenced for as long as recognition runs and put back exactly where they were the moment it stops. This is stated on the app's own screen, because a phone that has gone quiet deserves an explanation.

## The rest of what the screenshots showed

- **The floating button is 44dp, down from 56.** Still above the 48dp touch-target floor, still draggable, still over every app.
- **The panel is smaller and the speech box is bigger.** Labels dropped to 10sp and grey, the sound name to 19sp, and the speech box is 170dp and scrolls. Speech is the part she reads line by line, so it gets the room.
- **Refreshing the panel can no longer kill the app.** It runs four times a second for as long as the app is open, which is a great many chances for one bad frame to take the process down.
- **The microphone is never released under a live reader.** Every path that closes the classifier's microphone now waits for the reader thread to leave before releasing the object it reads from. This is the most likely native crash in 1.0.1's sharing loop.
- **Quiet speech from across the room is not cut off.** The recogniser now waits six seconds of silence before deciding a sentence is over, instead of the short default, and the watchdog that guards it outlasts that instead of racing it.

## Device test status

**1.0.1 was installed on a phone by the owner**, on his own Xiaomi, and the seven screenshots he sent are what this release is built from. **This 1.1 build has not been installed on a phone by anyone, so it has had no device test.** The store, the translator wiring, the cooldowns and the wording are covered by unit tests. Whether the app now survives a television left on all afternoon, whether the English-to-Tamil model downloads and works on his phone, and whether two switches read clearly on a small screen are not.

Two things still need her to try: whether the panel is comfortable to read at the new sizes, and whether the two switches make it obvious which one is doing what.

## தமிழ்

**வசந்தி AI 1.1** - உரிமையாளர் தங்கள் தொலைபேசியில் 1.0.1-ஐ நிறுவி ஏழு திரைப்படங்களை அனுப்பினார். அந்த ஏழில் இரண்டு மட்டும் மிக முக்கியம்: ஒன்று, சில நிமிடங்களுக்குப் பிறகு செயலி தானே மூடியிருந்தது; மற்றொன்று, பேச்சு அறிதல் நின்றிருந்தது. இந்தப் பதிப்பு முழுவதும் அதிலிருந்தே வந்தது.

**செயலி மூடியது**

வரலாற்றை வட்டில் எழுதுவது தவறான வழியில் நடந்தது.

`VasanthiStore.add()` ஒவ்வொரு வரியிலும் முழு கோப்பையும் மீண்டும் படித்து, பிரித்து, ஒரு வரி சேர்த்து, மீண்டும் முழுவதையும் எழுதியது. அது முதன்மை நூலில் நடந்தது - மிதக்கும் திரையை வரையும் அதே நூலில். `recordSpeech`-க்கு எந்தத் தாமதமும் இல்லை. அதனால் பின்னணியில் ஓடும் டெலிவி நான்கு நொடிக்கு ஒரு முறை மீண்டும் அறியப்பட்டு, ஒவ்வொரு முறையும் முழு வரலாறுக் கோப்பையும் முதலிலிருந்து எழுதியது. கோப்பு பெரிதாகிறது, எழுத நேரம் அதிகரிக்கிறது, முதன்மை நூல் வட்டில் அதிக நேரம் தடைபடுகிறது, கடைசியில் தொலைபேசி செயலியைக் கொன்றது.

அதுவே "தானே மூடுகிறது" என்று தோன்றியது: செயலி விழுந்து நின்றது, வரலாறு எவ்வளவு நேரத்தில் பெரிதாகின்றதோ அத்தனை நேரம் பிறகு அது நடந்தது.

மூன்று மாற்றங்கள், மூன்றும் ஒரு வரிக்கு செய்யும் வேலையைக் குறைப்பதைப் பற்றியவை:

- **வரலாறு ஒரு முறை படிக்கப்படுகிறது, பிறகு நினைவில் வைக்கப்படுகிறது.** செயலி தொடங்கும்போது ஒரு முறை படிக்கப்படும், மீண்டும் படிக்கப்படாது. ஒரு வரி சேர்ப்பது ஒரு பட்டியலில் சேர்க்கிறது.
- **ஒரு வரிக்கு ஒரு சிறிய எழுத்து.** ஒரே பின்னணி நூல் ஒவ்வொரு வரியையும் வரிசையாக எழுதுகிறது, முதன்மை நூல் வட்டைத் தொடாது.
- **திரும்பத் திரும்ப வரும் வாக்கியம் இருபத்தைந்து நொடி வரை போடப்படாது.** அறிதல் சில நொடிக்கும் மீண்டும் தொடங்கி, அதே வாக்கியத்தையே திரும்பத் தரும். தாமதம் இல்லாவிட்டால், ஒரு டெலிவி அதே வரியைப் பல முறை வரலாற்றில் நிரப்பும். படிக்க முடியாத வரலாறு, வரலாறு இல்லாததை விட கெட்டது.

**இரண்டு சுவிட்சுகள், ஒன்று அல்ல**

உரிமையாளர் இரு சுவிட்சுகள் கேட்டார் - ஒலி கேட்க ஒன்று, குரல் கேட்க ஒன்று - ஒரு நேரத்தில் ஒரு வேலை மட்டும் நடக்க. அவர் சரி. காரணம் அழகல்ல: **இரண்டு வேலைக்கும் ஒரே மைக் தேவை, ஒன்று மட்டுமே அதை முழுமையாக வைத்திருக்க முடியும்.**

அதனால் இப்போது இரு சுதந்திர சுவிட்சுகள் உள்ளன - ஒலி மற்றும் பேச்சு - மிதக்கும் திரையிலும் முதல் திரையிலும். இரண்டையும் ஒன்றாக வைக்கலாம் - மைப் பிரிக்கப்படும், நான்கு நொடி ஒலி அறிதல், பின்னர் அறிதலுக்கு பன்னிரண்டு நொடி வரை - அல்லது ஒன்றை மட்டும் வைக்கலாம்.

**ஒலி-ஐ ஆஃப் செய்வது பேச்சுக்கு சிறந்த அமைப்பு.** ஒலி அறிதல் நின்றால், அறிதலுக்கு மைக் முழுவதுமாகக் கிடைக்கும்: பிரிவு இல்லை, கைமாற்றம் இல்லை, நான்கு நொடி இடைவேலை இல்லை. அது துல்லியமாகவும் தொலைவிலிருந்து வரும் பேச்சையும் பிடிக்கும். உரிமையாளர் சொன்ன "கொஞ்சம் தொலைவிலிருந்து பேசினால் காட்டவில்லை" என்பது இதற்கான காரணம். அதனால் இவை இரண்டு சுவிட்சுகள், ஒரே ஒரு சுவிட்சு அல்ல.

**தொலைபேசியிலேயே நடக்கும் அறிதல் நீக்கப்பட்டது**

உரிமையாளர், தொலைபேசியிலேயே அறிதல் ஒருபோதும் வேலை செய்யவில்லை என்று சொல்லி, அதை நீக்கச் சொன்னார். நீக்கப்பட்டது. இப்போது ஒரு வழி மட்டும், அது Google-இன், அது தான் முன்னிருப்பு. அவரது தொலைபேசியில் வேலை செய்வது அது.

திரை தெளிவாகச் சொல்கிறது: ஒலி இணையம் வழியாகச் செல்கிறது. அது உண்மையான விலை, அது மறைக்கப்படவில்லை.

**ஆங்கிலம் தமிழாக வருகிறது**

உரிமையாளர் கேட்டார்: ஆங்கிலப் பேச்சு தமிழாக மாற்றிக் காட்டப்பட வேண்டும், ஆங்கில எழுத்துகளும் தமிழாக மாற்றப்பட வேண்டும். இரண்டும் முடிந்தது.

- **ஆங்கிலப் பேச்சு தொலைபேசியிலேயே மாற்றப்படுகிறது.** அறிதலிடம் தமிழ் கேட்கப்படுகிறது, பொதுவாகத் தமிழ் கிடைக்கும். ஆனால் எப்போதும் அல்ல - ஒரு பெயர், ஆங்கிலத்தில் மட்டும் அறிந்த ஒரு வார்த்தை, அல்லது உண்மையில் ஆங்கிலம் பேசிய ஒருவர். அந்த வரிகள் இப்போது எழுதப்படுவதற்கு முன் ML Kit-இன் தொலைபேசியிலேயே உள்ள ஆங்கிலம்-தமிழ் மாதிரியால் மாற்றப்படுகின்றன. மாதிரி முப்பது மெகாபைட், தேவைப்படும்போது பதிவிறக்கம் ஆகும். தமிழ் மட்டும் பேசும் வீடு அதற்காக எதையும் செலுத்தாது.
- **ஆங்கில எழுத்துகள் அனைத்தும் போய்விட்டன.** திரை, முதல் திரை, வரலாற்று வரிகள் ஆகியவற்றில் இப்போது தமிழ் மட்டும். ஒவ்வொரு சேமித்த வரியின் பக்கத்தில் அறிதல் சேவையின் ஆங்கிலப் பெயர் எழுதப்பட்டுவந்தது; இப்போது அது ஒருபோதும் எழுதப்படாது.

**டிங் டிங் டிங் நிறுத்தப்பட்டது**

உரிமையாளர் அதை "டிங் டிங் டிங்" என்று சொல்லி, முழுவதுமாக நிறுத்தச் சொன்னார். அறிதல் சேவை ஒவ்வொரு முறையும் தொடங்கும்போது ஒலி எழுப்புகிறது; சில நொடிக்கும் மீண்டும் தொடங்கும் அறிதல் எனவே சில நொடிக்கும் ஒலி எழுப்புகிறது. அதை நிறுத்தும் அமைப்பு எதுவும் இல்லை. அதனால் அது ஒலி எழுப்பும் ஓடைகள் அறிதல் நடக்கும் நேரத்தில் அமைதியாக்கப்படுகின்றன, நிற்கும்போது அதே இடத்திற்குத் திரும்பப்படுகின்றன. இது செயலியின் சொந்த திரையிலேயே சொல்லப்பட்டுள்ளது, ஏனெனில் அமைதியாகிய தொலைபேசிக்கு ஒரு விளக்கம் உரியது.

**திரைப்படங்களில் காட்டிய மற்றவை**

- **மிதக்கும் பொத்தான் 44dp, முன்பு 56dp.** 48dp தொடும் அளவுக்கு மேலே, இழுத்துச் செல்லக்கூடியது, எந்தச் செயலியின் மேலும் தெரியும்.
- **திரை சிறிதாக, பேச்சுப் பெட்டி பெரிதாக.** எழுத்துகள் 10sp ஆகவும் சாம்பல் நிறத்திலும் மாறின, ஒலிப் பெயர் 19sp ஆக மாறியது, பேச்சுப் பெட்டி 170dp உருட்டக்கூடியது. பேச்சு அவர் வரிசையாகப் படிக்கும் பகுதி, அதனால் அதற்கே அதிக இடம்.
- **திரை புதுப்பிப்பு இனி செயலியை நிற்க வைக்காது.** வினாடிக்கு நான்கு முறை ஓடும், முழு நேரமும் திறந்திருக்கும். ஒரு தவறான காட்சி செயலியை நிற்க வைக்கும் ஆபத்து மிக அதிகம்.
- **வாசிக்கும் நூல் உயிரோடு இருக்கும்போது மைக் ஒருபோதும் விடுவிக்கப்படாது.** ஒலி அறிதலின் மைக்கை மூடும் ஒவ்வொரு வழியும், அதைப் படிக்கும் நூல் வெளியேறும் வரை காத்திருக்கிறது. 1.0.1-இன் பிரிவில் இது தான் மிக சாத்தியமான இயல்பு விழுகை.
- **தொலைவிலிருந்து மெதுவாக வரும் பேச்சு வெட்டப்படாது.** வாக்கியம் முடிந்தது என்று அறிதல் ஆறு நொடி அமைதியைக் காத்திருக்கிறது, சிறிய முன்னிருப்பு அல்ல. அதைக் காக்கும் காவலாளி அதற்கு மேலாக நேரம் காத்திருக்கிறது.

**சாதனப் பரிசோதனை நிலை**

**1.0.1 உரிமையாளரின் தொலைபேசியில் நிறுவப்பட்டது**, தங்கள் சொந்த Xiaomi-யில். அவர் அனுப்பிய ஏழு திரைப்படங்களே இந்தப் பதிப்பின் ஆதாரம். **இந்த 1.1 பதிப்பு எந்தச் சாதனத்திலும் நிறுவப்படவில்லை.** கிடங்கு, மாற்றி இணைப்பு, தாமதங்கள், வார்த்தைகள் ஆகியவை அலகு சோதனையில் உள்ளன. டெலிவி மதியம் முழுவதும் இயங்கியாலும் செயலி நிற்கிறதா, அவரது தொலைபேசியில் ஆங்கிலம்-தமிழ் மாதிரி பதிவிறக்கம் ஆகி வேலை செய்கிறதா, சிறிய திரையில் இரு சுவிட்சுகள் தெளிவாகத் தெரிகிறதவா என்பது இன்னும் சோதிக்கப்படவில்லை.

இன்னும் இரண்டு அவரைச் சோதிக்க வேண்டும்: புதிய அளவில் திரை படிக்க அமைதியாக உள்ளதா, இரு சுவிட்சுகள் எது என்ன செய்கிறது என்று தெளிவாகச் சொல்கிறதவா.
