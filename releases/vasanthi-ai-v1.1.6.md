# Vasanthi AI 1.1.6

## Sound recognition: more names, clearer at a glance

- The floating panel and the history list now show a picture (emoji cue) next to every recognised sound, so a sound can be identified at a glance without reading. The cue is derived from the recognised class, never from the microphone, and stays the same picture for the same sound every time.
- 54 more everyday sounds now have a Tamil name (248 of 521 classes in total), including dog, car, thunder, rain, blender, hair dryer, electric shaver, toothbrush, zipper, keys, scissors, chewing, running, crowd, cheering, cowbell, bee, jingle bell, vehicle horn, truck, air horn, ambulance siren, train whistle, ice-cream truck, lawn mower, jackhammer, aircraft and jet engines, engine starting, boat motor, fire alarm, buzzer, telephone dial tone, busy signal, whistle, steam whistle, water pump, television, radio, computer keyboard, Carnatic music and cinema songs. Fire alarm joins the urgent list, which is now 27 sounds.
- An unlabelled quiet window is no longer classified: when the microphone level is in the "மிக அமைதி" (very quiet) band the capture thread skips the model and reports Silence directly. This saves battery during silent stretches and stops the model from inventing a sound out of noise.
- Silence is no longer written to history. A stretch of quiet no longer pushes real sounds out of the list, and the panel shows a 0% bar with the "மிக அமைதி" note while it is quiet.
- The history header has an **Export** button. It shares the whole list as plain text (oldest first, sound or speech, time and confidence) through Android's share sheet, so a record can be kept or sent without copying by hand. Nothing is sent anywhere unless you choose a share target.

## Installing

- Same package `com.universalrp.vasanthi` and the same signing key as the public 1.1.5, so this is an in-place update: install over 1.1.5 and keep going. Do not uninstall first; uninstalling may erase local data (history and settings).
- Phones still on 1.1.4 or earlier (the original key) cannot update in place and need a fresh install; see the 1.1.5 note for that one-time step.

## Testing limits

- **The candidate was installed on a phone.** The owner installed the candidate APK from CI run 45331600666 (source 97d252e) on his own phone on 24 November 2026 and confirmed it works. His screenshots show the picture in front of every sound line in the history, speech lines with no percentage, the urgent cards, the பேச்சு / ஒலி filters and a recognised speech line; Google Play Protect showed "This app looks safe" on that one phone. Not shown on a phone: the history **Export** share sheet, the quiet-window gate over a long silent stretch and its battery effect.
- **The published APK is a rebuild, not the file he installed.** The publish run builds from the same source with the same key and verifies its own bytes; no second phone has been tested. The new gate, the picture map and the history rule are covered by JVM unit tests, but CI is not proof of microphone behaviour, battery life or the floating panel on your specific phone.

## தமிழ்

- **நிறுவல்:** அதே தொகுப்பு `com.universalrp.vasanthi`, பொது 1.1.5-இன் அதே கையொப்பம்; எனவே இது நேரடிப் புதுப்பிப்பு ஆகும்: 1.1.5 மீது நிறுவவும். முதலில் நீக்க வேண்டாம்; நீக்கினால் உள்ளூர்த் தரவு (வரலாறு, அமைப்புகள்) அழியலாம்.
- 1.1.4 அல்லது அதற்கு முந்தைய பதிப்பில் (பழைய கையொப்பம்) இருக்கும் தொலைபேசிகள் நேரடியாகப் புதுப்பிக்க முடியாது; அந்த ஒருமுறைப் படிக்கு 1.1.5 குறிப்புகளைப் பார்க்கவும்.
- மிதக்கும் சாளரத்திலும் வரலாற்றுப் பட்டியலிலும் ஒவ்வொரு அறியப்பட்ட ஒலிக்கும் அருகில் ஒரு படம் (emoji) காட்டப்படும்; சத்தமாக வாசிக்காமலேயே ஒலியை அடையாளம் காணலாம். இந்தப் படம் அறியப்பட்ட ஒலி வகையிலிருந்து மட்டும் தீர்மானிக்கப்படும், ஒரே ஒலிக்கு எப்போதும் ஒரே படம்.
- இன்னும் 54 அன்றாட ஒலிகளுக்குத் தமிழ்ப் பெயர் சேர்க்கப்பட்டது (மொத்தம் 521 வகைகளில் 248). நாய், கார், இடி, மழை, மிக்சி, ஹேர் டிரயர், மின் ஷேவர், பல் துலக்கம், ஜிப்பர், சாவிகள், கத்தரிக்கோல், மெல்லும் ஒலி, ஓட்டம், கூட்டம், வாழ்த்தொலி, மாட்டு மணி, தேனீ, ஜிங்கில் மணி, வாகன ஹாரன், லாரி, ஏர் ஹாரன், ஆம்புலன்ஸ் சிரன், ரயில் விசில், ஐஸ் வண்டி பாட்டு, புல் வெட்டி, சாலை இடிப்பு, விமான இயந்திரம், ஜெட் இயந்திரம், வாகனம் ஸ்டார்ட், படகு மோட்டார், தீ எச்சரிக்கை அலாரம், பசர், டயல் டோன், லைன் பிசி டோன், விசில், நீராவி விசில், நீர் பம்ப், தொலைக்காட்சி, வானொலி, கணினி விசைப்பலகை, கர்நாடக இசை, சினிமா பாட்டு ஆகியவை அடங்கும். தீ எச்சரிக்கை அலாரம் அவசர ஒலிகளில் சேர்க்கப்பட்டு அந்தப் பட்டியல் 27 ஆகியது.
- ஒலியின்றி இருக்கும் நேரத்தில் மாதிரியாக்கப்படாத அமைதி ஜன்னல் இனி வகைப்படுத்தப்படாது: "மிக அமைதி" நிலையில் காப்டர் த்ரெட் மாடலைத் தவிர்த்து நேரடியாக அமைதி எனக் காட்டும். இது அமைதியான நேரத்தில் பேட்டரியை மிச்சப்படுத்தும், சத்தலில் இல்லாத ஒலியை மாடல் கற்பனை செய்யாமல் தடுக்கும்.
- அமைதி இனி வரலாற்றில் சேமிக்கப்படாது. அமைதியான நேரம் உண்மையான ஒலிகளைப் பட்டியலிலிருந்து தள்ளாது; அமைதியாக இருக்கும்போது சாளரம் 0% பட்டையும் "மிக அமைதி" குறிப்பும் காட்டும்.
- வரலாற்றுத் தலைப்பில் **ஏற்றுமதி** பொத்தான் சேர்க்கப்பட்டது. முழுப் பட்டியலையும் சாதாரண உரையாக (பழையது முதலில், ஒலி அல்லது பேச்சு, நேரம், நம்பிக்கை விழுக்காடு) Android பகிர்வு முறை வழியாகப் பகிரலாம். நீங்கள் பகிர்வு இலக்கைத் தேர்ந்தெடுக்காத வரை எதுவும் எங்கும் அனுப்பப்படாது.
- **வேட்பாளர் தொலைபேசியில் நிறுவப்பட்டது.** உரிமையாளர் CI இயக்கம் 45331600666-இன் (மூலம் 97d252e) வேட்பாளர் APK-ஐ 24 நவம்பர் 2026 அன்று தன் தொலைபேசியில் நிறுவி சரியாக இயங்குவதாக உறுதிப்படுத்தினார். அவரது திரைப்பிடிப்புகளில் வரலாற்றில் ஒவ்வொரு ஒலி வரியின் முன்னும் படம், சதவீதம் இல்லாத பேச்சு வரிகள், அவசர அட்டைகள், பேச்சு / ஒலி வடிகட்டிகள் மற்றும் அறியப்பட்ட ஒரு பேச்சு வரி தெரிகின்றன; அந்த ஒரு தொலைபேசியில் Google Play Protect "This app looks safe" எனக் காட்டியது. தொலைபேசியில் காட்டப்படவில்லை: வரலாற்று **ஏற்றுமதி** பகிர்வு, நீண்ட அமைதியின் போது அமைதி-வாயிலும் அதன் பேட்டரி விளைவும்.
- **வெளியிடப்படும் APK அவர் நிறுவிய கோப்பு அல்ல; அதே மூலத்திலிருந்து அதே கையொப்பத்துடன் மீண்டும் உருவாக்கப்படும்.** இரண்டாவது தொலைபேசி சோதிக்கப்படவில்லை. புதிய அமைதி-வாயில், பட அட்டவணை, வரலாற்று விதி ஆகியவை JVM அலகுத் தேர்வுகளில் உள்ளன; CI உங்கள் தொலைபேசியின் மைக்ரோஃபோன், பேட்டரி அல்லது மிதக்கும் சாளர நடத்தைக்கு ஆதாரம் அல்ல.
