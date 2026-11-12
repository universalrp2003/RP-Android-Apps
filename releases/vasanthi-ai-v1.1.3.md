# Vasanthi AI 1.1.3

## The volume did not come back

The owner reported that the sound was muted on many phones, that after closing the app the volume did not return, that the mute seemed to have no end, and that the notification sound had gone quiet.

The cause is the recognition beep. Google's recognition service plays a beep on every `startListening`, and a recogniser restarted on each no-speech timeout beeps continuously while idle. The only reliable suppression is to silence the streams the beep plays on, which this app has done since 1.1.2 - and that is where the defect was.

**The saved volumes lived only in memory.** They were recorded when the streams were silenced and put back when the engine stopped. But if the process died while they were silenced - the owner swiping the app away, or the system reclaiming it - nothing ever ran the code that put them back. The phone stayed muted, with the notification stream at zero, and the owner had to fix it by hand in Settings. Closing the app did not restore it, which is exactly what was reported.

**The fix:** the saved volumes are now written to disk as they are saved, and read back on the next launch. So even when a previous process was killed mid-silence, the next launch repairs it. The restore also runs once when the engine starts, before it silences anything again, so the volume the owner set is never lost.

The notification stream being muted is why the notification sound had gone quiet; it is put back with the rest.

## Honest limits

- **This 1.1.3 build has not been installed on a phone by anyone**, so it has had no device test.
- Muting the two streams is still the only reliable way to suppress the beep. There is no RecognizerIntent extra that disables it, and Google has moved the sound between `STREAM_MUSIC` and `STREAM_NOTIFICATION` across versions, so both have to be silenced. If a future Android version moves it to a third stream, that stream will need adding.
- If the owner changes the volume *while* the app is silenced, the app will restore the value it saved rather than the one just set. This is accepted: the alternative is to leave the phone muted.

## தமிழ்

**வசந்தி AI 1.1.3** - ஒலி அமைதியாக இருந்தது, செயலியை மூடிய பிறகும் திரும்பவில்லை. இப்போது திரும்பும்.

**என்ன நடந்தது**

பேச்சைக் கேட்கும் சேவை ஒவ்வொரு முறை தொடங்கும்போது ஒரு சத்தம் போடுகிறது. அந்தச் சத்தத்தை அடக்கும் ஒரே வழி, அது இயங்கும் ஓட்டைகளை அமைதியாக்குவது. இந்தச் செயலி 1.1.2 முதல் அதைச் செய்து வந்தது - குறை அங்கே இருந்தது.

**சேமித்த ஒலிகள் நினைவகத்தில் மட்டும் இருந்தன.** ஓட்டைகள் அமைதியாக்கப்படும்போது அவை பதிவு செய்யப்பட்டன, பொறி நிற்கும்போது திரும்ப வைக்கப்பட்டன. ஆனால் அமைதியாக்கிய நிலையிலேயே செயலி இறந்தால் - உரிமையாளர் அதைத் தேய்த்து நீக்குவது, அல்லது தொலைபேசியின் அமைப்பு அதை எடுத்துவிடுவது - திரும்ப வைக்கும் நிரல் ஓடவே இல்லை. தொலைபேசி அமைதியாகவே இருந்தது, அறிவிப்பு ஓட்டை பூஜ்யத்தில். உரிமையாளர் அமைப்புச் சென்று கையால் சரிசெய்ய நேர்ந்தது. செயலியை மூடினாலும் ஒலி திரும்பவில்லை - உரிமையாளர் சொன்னது அதே.

**இப்போது என்ன செய்யப்பட்டது**

சேமித்த ஒலிகள் இப்போது சேமிக்கும்போதே வட்டில் எழுதப்படுகின்றன, அடுத்த முறை திறக்கும்போது படிக்கப்படுகின்றன. எனவே முந்தைய செயலி நடுவில் இறந்தாலும், அடுத்தத் தொடக்கம் அதை சரிசெய்யும். மீட்பு ஒரு முறை பொறி தொடங்கும்போதும் ஓடும், அமைதியாக்குவதற்கு முன்பே - எனவே உரிமையாளர் வைத்த ஒலி ஒருபோதும் இழக்கப்படாது.

அறிவிப்புச் சத்தம் குறைவாக இருந்ததற்குக் காரணம் அறிவிப்பு ஓட்டை அமைதியாக்கியது; அது மற்றவற்றுடன் திரும்ப வைக்கப்படுகிறது.

**வரம்புகள்**

**இந்த 1.1.3 பதிப்பு யாராலும் எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** இரண்டு ஓட்டைகளை அமைதியாக்குவதே சத்தத்தை அடக்கும் ஒரே வழி. அந்தச் சத்தத்தை அணைக்கும் எந்தக் கட்டளையும் இல்லை, மேலும் அந்தச் சேவை அதை `STREAM_MUSIC` மற்றும் `STREAM_NOTIFICATION` இடையே நகர்த்தி வருகிறது, எனவே இரண்டையும் அமைதியாக்க வேண்டும்.

உரிமையாளர் அமைதியாக்கிய நிலையிலேயே ஒலியை மாற்றினால், செயலி சேமித்த மதிப்பைத் திரும்ப வைக்கும், புதிதாக வைத்ததை அல்ல. இது ஏற்கப்பட்டது: மற்ற வழி தொலைபேசியை அமைதியாகவே விடுவது.
