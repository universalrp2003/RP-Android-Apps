# Tamil Podcast Player 1.0 - listens to Tamil shows, online or offline

The owner asked for a Tamil podcast player as the next app after Emergency Alert was withdrawn. This release is that, and nothing else.

It plays Tamil podcast shows. With the internet on it streams them live; without it, it plays what has already been saved. The screen stays simple because this app is meant to be used while doing something else.

## What it does

- **Four Tamil shows are built in**, so the app is useful the moment it opens without anybody having to find a feed URL. They are public RSS feeds chosen because they publish in Tamil and because they are long-running. Nothing about them is bundled: no audio, no cached episode, so the app stays small and the content stays the publisher's.
- **Any RSS feed can be added** by pasting its URL. A feed that stops publishing simply shows an empty list.
- **Playback keeps going with the screen off.** A partial wake lock is held while an episode plays, and the notification carries pause and stop.
- **Speed and skip** are on the player card, and the episode's real length is shown, read from the feed's `itunes:duration` in any of the three forms that are in the wild.

## What it is built from

No media library and no new dependency. `android.media.MediaPlayer` is the platform's own player and streams an HTTP URL directly, which is the whole of what this app needs from playback. Adding ExoPlayer would be several megabytes of download and one more thing that can fail to resolve, for features nobody asked for.

The RSS reader is hand-rolled for one reason: this app's parsing has to be testable on the JVM. Android's `XmlPullParser` lives in the platform stub, so a test that parses anything fails with "not mocked". RSS 2.0 is a small, regular subset of XML, so extracting the title, the enclosure URL, the duration and the date with string operations is both testable and honest about how much of the format is used. It deliberately does not handle CDATA beyond unwrapping it, namespaces, or Atom; a feed that needs any of those shows as empty rather than as wrong.

Dates are parsed by hand from RFC 822 rather than with `SimpleDateFormat`, because that class is not safe to call from a test and from a service at once.

## Permissions

`INTERNET` to fetch and stream, `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` to keep playing with the screen off, `POST_NOTIFICATIONS` for the player notification, and `WAKE_LOCK` for the same reason the service exists.

No location, no contacts, no storage, no camera, no phone state, no advertising identifier, no accounts.

## Device test status

**This 1.0 build has not been installed on a phone by anyone, so it has had no device test.** The RSS reader, the duration and date parsers, the episode cap and the store's rules are covered by unit tests. What is not covered: whether a real Tamil feed parses, whether streaming survives a network change, and whether playback continues through a full night with the screen off. Those need a phone.

## தமிழ்

**தமிழ் போட்காஸ்ட் பிளேயர் 1.0** - அவசர எச்சரிக்கை நீக்கப்பட்ட பிறகு உரிமையாளர் கேட்ட அடுத்த செயலி. இந்தப் பதிப்பு அது மட்டுமே.

தமிழ் நாட்காட்டிகளைக் கேட்கலாம். இணையம் இருந்தால் நேரலையில், இல்லையென்றால் சேமித்ததைக் கேட்கலாம். வேறு ஏதாவது செய்யும் போது பயன்படும் செயலி என்பதால் திரை எளிமையாக இருக்கிறது.

**என்ன செய்யும்**

- **நான்கு தமிழ் நாட்காட்டிகள் உள்ளேயே உள்ளன**, எனவே யாரும் இணைப்பைத் தேடாமலே செயலி திறந்த உடனே பயன்படும். அவை தமிழில் வெளியிடும் பொது RSS நாட்காட்டிகள்; நீண்ட காலம் வெளியிடுவதைக் கருத்தில் கொண்டு தேர்ந்தெடுக்கப்பட்டவை. அவற்றில் எந்த ஒலியும் சேமிக்கப்படவில்லை, எனவே செயலி சிறியது, உள்ளடக்கம் வெளியிடுபவரிடமே இருக்கிறது.
- **எந்த RSS நாட்காட்டியையும்** இணைப்பை ஒட்டி சேர்க்கலாம். வெளியிடல் நின்ற நாட்காட்டி காலியாகத் தெரியும்.
- **திரை அணைந்தாலும் இயக்கம் தொடரும்.** இயக்கத்தில் ஒரு பகுதி வேக்-லாக் வைக்கப்படும், அறிவிப்பில் இடைநிறுத்து மற்றும் நிறுத்து உள்ளன.
- **வேகமும் தாண்டலும்** இயக்க அட்டையில் உள்ளன, பகுதியின் உண்மையான நீளம் நாட்காட்டியிலிருந்து படிக்கப்படும் - பூமியில் உள்ள மூன்று வடிவங்களிலும்.

**எதிலிருந்து கட்டப்பட்டது**

ஊடக நிரல் நூலகமும் புதிய சார்பும் இல்லை. `android.media.MediaPlayer` தளத்தின் சொந்த இயக்கி, HTTP இணைப்பை நேரலையில் இயக்குகிறது - இது இந்தச் செயலி இயக்கத்திலிருந்து தேவைப்படும் அனைத்தும். ExoPlayer சேர்ப்பது பல மெகாபைட் பதிவிறக்கமும், தீர்மானிக்கப்படாத ஒரு சார்பும், யாரும் கேட்காத வசதிகளுக்காக.

RSS படிப்பி கையால் எழுதப்பட்டது, ஒரு காரணத்தால்: இந்தச் செயலியின் பகுப்பாய்வு JVM-யில் சோதிக்கக்கூடியதாக இருக்க வேண்டும். ஆண்ட்ராய்டின் `XmlPullParser` தள stub-ல் இருக்கிறது, எனவே எதையும் பகுப்பாய்வு செய்யும் சோதனை "not mocked" என தோல்வியடையும். RSS 2.0 XML-ன் ஒரு சிறிய, ஒழுங்கான துணை, எனவே தலைப்பு, enclosure URL, காலம், தேதி ஆகியவற்றை எழுத்து செயல்பாட்டால் எடுப்பது சோதிக்கக்கூடியது, பயன்படுத்தப்படும் வடிவத்தின் அளவைப் பற்றி நேர்மையானது. CDATA-ஐ அவிழ்ப்பதற்கு அப்பால், பெயர்வெளி, Atom ஆகியவை விவேகமாக கையாளப்படவில்லை; அவை தேவைப்படும் நாட்காட்டி தவறாக அல்ல, காலியாகத் தெரியும்.

தேதிகள் RFC 822-ல் இருந்து கையால் படிக்கப்படுகிறது, `SimpleDateFormat`-ல் அல்ல, ஏனெனில் அந்த வகுப்பு ஒரு சோதனையிலிருந்தும் ஒரு சேவையிலிருந்தும் ஒரே நேரத்தில் அழைப்பது பாதுகாப்பானது அல்ல.

**அனுமதிகள்**

பதிவிறக்க மற்றும் ஓட்ட `INTERNET`, திரை அணைந்தாலும் இயக்க `FOREGROUND_SERVICE` மற்றும் `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, இயக்க அறிவிப்பு `POST_NOTIFICATIONS`, சேவை இருக்கும் அதே காரணத்தால் `WAKE_LOCK`.

இருப்பிடம், தொடர்புகள், சேமிப்பு, கேமரா, போன் நிலை, விளம்பர அடையாளம், கணக்கு - இவை எதுவும் இல்லை.

**சாதனப் பரிசோதனை நிலை**

**இந்த 1.0 பதிப்பு யாராலும் எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** RSS படிப்பி, காலம் மற்றும் தேதி பகுப்பாய்வு, பகுதி வரம்பு, கிடங்கின் விதிகள் ஆகியவை அலகு சோதனையில் உள்ளன. இல்லாதவை: ஒரு உண்மையான தமிழ் நாட்காட்டி பகுப்பாய்வு செய்யப்படுகிறதா, வலையம் மாறினாலும் ஓட்டம் நிற்காது, திரை அணைந்து ஒரு முழு இரவு இயக்கம் தொடர்கிறதா. இவை ஒரு தொலைபேசி தேவை.
