# Vasanthi AI 1.1.2 - an app for everyone, with a guide and links to every other RP Ideas app

The owner installed 1.1.1 and his sister installed it too, and both work. His instruction was that Vasanthi AI is now an app for everyone rather than one built for a single person, and that it needs an About page with a guide and links to all the other applications. This release does both, and nothing else.

## It is no longer described as one person's app

The main screen used to end with a line saying the app was built for the owner's sister. That line is gone. In its place is who the app is actually for, in the same plain terms the guide uses:

- **Anyone who wants to see what is being said, or what is happening in the room, in Tamil.** People who are deaf or hard of hearing, people who cannot hear a television or a call clearly, and anyone who would rather read than listen.
- **The subtitle says what it does** rather than what it was made for: "shows the sound and speech around you in Tamil".
- **The module README is rewritten the same way**, and still records honestly that it began as an app for one person. The history is not being hidden, it is just no longer the app's description of itself.

## A guide, in the same place as every other app

There is now a **வழிகாட்டி** button on the main screen. It opens a tap-to-open Tamil and English guide with nine topics: what the app does, the floating circle, the two switches, English becoming Tamil, the permanent notification, the saved history, who it is for, privacy, and what it cannot do.

The guide lives in the shared guide file beside the other six apps' guides, so a step that changes on a phone is corrected once, for every app, in both languages. It also means the wording cannot drift apart from the app it explains.

The owner asked for this by name: "you can add about page guide". The other six apps already had one, and the three newest did not.

## Links to every other RP Ideas application

The About section now lists **every other app in the suite** plus the project links - the official website, the GitHub repository and the legacy mirror - each opening in the browser. That list comes from the same shared directory every other app's About screen renders, so adding an app to it adds it to all ten at once.

This is the "all other applications link etc" he asked for, and it was already half-built: `SisterApps.kt` has carried all ten apps since Expense Tracker shipped, and the other apps' About screens already render it. Vasanthi AI was the one app not rendering it.

## Also corrected

- **Two stale comments said "seven RP Ideas apps".** There are ten, and the guide file now says so too.
- **The guide file's header said the guide covered six apps.** It says ten, with the three newest named.

## Device test status

**1.1.1 was installed on a phone by the owner**, on his own Xiaomi, and **his sister installed it too**. Both work, and that is what closed the last of the reported defects. **This 1.1.2 build has not been installed on a phone by anyone, so it has had no device test.** The guide text, the word-finding, the store and the cooldowns are covered by unit tests, and the shared guide and sister-app list are the same code the other apps already ship. Whether the guide reads clearly on a small screen and whether the other-app links open the right pages are not.

## தமிழ்

**வசந்தி AI 1.1.2** - உரிமையாளர் 1.1.1-ஐ நிறுவினார், அவரது சகோதரியும் நிறுவினார். இரண்டும் வேலை செய்கிறது. அவர் சொன்ன கட்டளை: வசந்தி AI இனி ஒரே ஒரு நபருக்கு உருவான செயலி அல்ல, அனைவருக்கும் ஒரு செயலி; அதற்கு ஒரு வழிகாட்டியும் மற்ற எல்லா செயலிகளின் இணைப்புகளும் தேவை. இந்தப் பதிவு அந்த இரண்டையும் மட்டுமே செய்கிறது.

**இது இனி ஒரே ஒரு நபரின் செயலி அல்ல**

முதல் திரையின் இறுதியில் "இந்தச் செயலி உரிமையாளரின் சகோதரிக்காக உருவாக்கப்பட்டது" என்று ஒரு வரி இருந்தது. அது போய்விட்டது. அதற்குப் பதிலாக, வழிகாட்டி பயன்படுத்தும் அதே எளிய வார்த்தைகளில், யாருக்கு இது என்பது வந்துள்ளது:

- **எவருக்கும் என்ன சொல்கிறார்கள், அல்லது அறையில் என்ன நடக்கிறது என்பதைத் தமிழில் பார்க்க வேண்டுமானாலும் இது.** செவிடு அல்லது கேள்வி மெதுவானவர்கள், டெலிவி அல்லது அழைப்பைத் தெளிவாகக் கேட்க முடியாதவர்கள், கேட்பதை விட படிக்க விரும்புவோர்.
- **துணைத் தலைப்பு எதற்காக உருவானது என்று அல்ல, என்ன செய்கிறது என்று சொல்கிறது:** "சுற்றியுள்ள ஒலியையும் பேச்சையும் தமிழில் காட்டும் செயலி".
- **தொகுதி README அதே வழியில் மீண்டும் எழுதப்பட்டது**, அது ஒரு நபருக்காகத் தொடங்கியது என்பதை இன்னும் நேர்மையாகப் பதிவு செய்கிறது. வரலாறு மறைக்கப்படவில்லை, அது இனி செயலியின் சொந்த விளக்கம் அல்ல.

**ஒரு வழிகாட்டி, மற்ற எல்லா செயலியும் உள்ள இடத்தில்**

இப்போது முதல் திரையில் **வழிகாட்டி** ஒரு பொத்தான் உள்ளது. அது ஒன்பது தலைப்புகளுடன் தொட்டு விரிவாக்கும் தமிழ்-ஆங்கில வழிகாட்டியைத் திறக்கிறது: செயலி என்ன செய்கிறது, மிதக்கும் வட்டு, இரு சுவிட்சுகள், ஆங்கிலம் தமிழாக வருவது, நிரந்தர அறிவிப்பு, சேமித்த விவரம், யாருக்கு இது பயன்படும், தனியுரிமை, இது செய்யாதது.

வழிகாட்டி மற்ற ஆறு செயலிகளின் வழிகாட்டிகளுக்கு பக்கத்தில் உள்ள பகிரப்பட்ட கோப்பில் வாழ்கிறது. அதனால் போனில் மாறும் ஒரு படி ஒரே இடத்தில், எல்லா செயலிக்கும், இரு மொழியிலும் சரி செய்யப்படும். அது விளக்கும் செயலியிலிருந்து வார்த்தைகள் விலகிப் போவதும் இல்லை.

உரிமையாளர் இதைப் பெயரிட்டுக் கேட்டார்: "you can add about page guide". மற்ற ஆறு செயலிகளுக்கு ஏற்கனவே ஒன்று இருந்தது, புதிய மூன்றுக்கு இல்லை.

**மற்ற ஒவ்வொரு RP Ideas செயலியின் இணைப்புகளும்**

About பகுதி இப்போது **தொகுத்தில் உள்ள மற்ற எல்லா செயலியையும்** மற்றும் திட்ட இணைப்புகளையும் பட்டியலிடுகிறது - அதிகாரப்பூர்வ இணையதளம், GitHub களஞ்சியம், பழைய பிரதிபலிப்பு - ஒவ்வொன்றும் உலாவியில் திறக்கும். அந்தப் பட்டியல் மற்ற எல்லா செயலியின் About திரையும் வரைபடமாக்கும் அதே பகிரப்பட்ட கடைவாசியிலிருந்து வருகிறது, அதனால் ஒரு செயலியைச் சேர்ப்பது பத்துக்கும் சேர்க்கிறது.

இது அவர் கேட்ட "all other applications link etc", அது ஏற்கனவே பாதி கட்டப்பட்டிருந்தது: Expense Tracker வெளியானது முதல் `SisterApps.kt` பத்து செயலிகளையும் சுமந்து வந்தது, மற்ற செயலிகளின் About திரைகள் ஏற்கனவே அதை வரைபடமாக்குகின்றன. வசந்தி AI மட்டுமே அதை வரைபடமாக்காத ஒரே செயலி.

**மேலும் சரி செய்யப்பட்டவை**

- **"ஏழு RP Ideas செயலிகள்" என்று இரண்டு பழைய குறிப்புகள் இருந்தன.** பத்து உள்ளன, வழிகாட்டிக் கோப்பும் இப்போது அதையே சொல்கிறது.
- **வழிகாட்டிக் கோப்பின் தலைப்பி ஆறு செயலிகளைக் குறித்தது.** இப்போது பத்து, புதிய மூன்று பெயரிடப்பட்டுள்ளன.

**சாதனப் பரிசோதனை நிலை**

**1.1.1 உரிமையாளரின் தொலைபேசியில் நிறுவப்பட்டது**, தங்கள் சொந்த Xiaomi-யில், **அவரது சகோதரியும் அதை நிறுவினார்**. இரண்டும் வேலை செய்கிறது, அதுவே குறைக்கப்பட்ட கடைசி விவரத்தை முடித்தது. **இந்த 1.1.2 பதிப்பு எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** வழிகாட்டி உரை, வார்த்தை கண்டறிவு, கிடங்கு, தாமதங்கள் ஆகியவை அலகு சோதனையில் உள்ளன, பகிரப்பட்ட வழிகாட்டியும் சகோதரி செயலிப் பட்டியலும் மற்ற செயலிகள் ஏற்கனவே அனுப்பும் அதே குறியீடு. சிறிய திரையில் வழிகாட்டி தெளிவாகப் படிக்கிறதா, மற்ற செயலி இணைப்புகள் சரியான பக்கங்களைத் திறக்கிறதா என்பது இல்லை.
