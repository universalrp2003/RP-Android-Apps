# Emergency Alert 1.0 - hears the emergency sound, tells the family, no internet needed

The owner's instruction for this part of the work was: hear the emergency sound, then send a Tamil alert to the family phones. He chose it first, and he was specific that it must work over the local Wi-Fi with no internet. This release is that app, and nothing else.

It listens for the sounds that mean somebody needs help - a smoke alarm, a fire alarm, a siren, a scream, a baby crying, breaking glass, a doorbell - and the moment it hears one it says so in Tamil and tells every other phone on the same Wi-Fi.

## What it listens for

YAMNet names 521 different sounds. This app gates on eighteen of them, and only those eighteen can raise an alert. A ceiling fan, a television, a washing machine and typing are all deliberately absent, because an app that wakes a family at three in the morning for a fan is worse than no app at all.

Each of the eighteen carries a Tamil name and a Tamil sentence, which is what the other phones actually receive:

- **புகை எச்சரிக்கை, தீ எச்சரிக்கை, தீ, சைரன், பேரழிவ்ச்சை சைரன்** - the fire and smoke class.
- **கத்தும் ஒலி, கூச்சல், அலறும் ஒலி, குழந்தைகள் கூச்சல், அழும் ஒலி, குழந்தை அழுகை, உடைந்து விழும் ஒலி, கண்ணாடி உடைவு, துப்பாக்கி ஒலி** - somebody needs help.
- **கதவு மணி, டிங்-டாங், மணி ஒலி** - the door.

A fire alarm has to reach 45% confidence, a shout 55%, a doorbell 40%. Those numbers are not arbitrary: a doorbell is a narrow-band sound YAMNet is good at, and a shout is not. Requiring the same confidence for both would either miss every shout or fire on every raised voice in an argument.

## How the alert travels

The alert is a UDP packet on the home network. There is no server, no account, no cloud, and no pairing screen. Every phone on the same Wi-Fi that has this app open and listening hears it.

- **No internet needed.** The router losing its upstream connection does not stop this working, which is exactly when a family needs it to work.
- **The broadcast address is read from the Wi-Fi interface** rather than assumed, because many home routers drop a global broadcast while passing the directed one.
- **Three copies of each packet**, because UDP on a shared network loses packets and this is not a stream where a retransmit is expected.
- **A ping and a reply**, so the screen can show which phones are actually connected rather than only the ones that happened to have an emergency. Without it there is no way to tell whether the feature is working at all.
- **A repeat of the same sound within fifteen seconds is one event**, not thirty. A smoke alarm that sounds for half a minute produces one alert, one notification and one packet.

## The two limits, stated plainly

- **Android blocks every app from hearing the other side of a phone call.** This app hears the room, not the call. The same is true of every app on the phone; it is not something this app can work around.
- **It is an alert, not a guard.** Sometimes an ordinary sound will be mistaken for an emergency one. The confidence percentage is shown next to every alert precisely so a false one can be judged.

## What is not in it

SMS is deliberately absent. The owner's chosen scope was the local network, and an app that can send a text message from somebody's phone is a different permission and a different risk. If SMS is ever wanted it will be a separate, explicitly granted step.

There is no location, no contacts, no storage, no camera, no phone state and no advertising identifier. The permissions are RECORD_AUDIO, INTERNET (for the local socket only), VIBRATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE and RECEIVE_BOOT_COMPLETED.

## Device test status

**This 1.0 build has not been installed on a phone by anyone, so it has had no device test.** The wire protocol, the duplicate suppression, the emergency vocabulary against the shipped model and the state store are covered by unit tests, and the shared model and label file are the same ones Vasanthi AI already ships. What is not covered: whether the microphone stays open for a whole night, whether the packets actually cross a real home router, and how it behaves on a phone with aggressive battery management. Those need a real phone in a real house.

## தமிழ்

**அவசர எச்சரிக்கை 1.0** - உரிமையாளரின் கட்டளை: அவசர ஒலியைக் கேட்டு, குடும்பத் தொலைபேசிகளுக்குத் தமிழ் எச்சரிக்கை அனுப்ப வேண்டும். அவர் அதை முதலில் தேர்ந்தெடுத்தார், உள்ளூர் வைஃபையில் இணையம் இல்லாமல் வேலை செய்ய வேண்டும் என்று குறிப்பிட்டார். இந்தப் பதிப்பு அந்த செயலி மட்டுமே.

கேட்கும் ஒலிகள்: புகை எச்சரிக்கை, தீ எச்சரிக்கை, சைரன், கத்தல், குழந்தை அழுகை, கண்ணாடி உடைவு, கதவு மணி. ஒன்றைக் கேட்ட உடனே தமிழில் சொல்லி, அதே வைஃபையில் உள்ள மற்ற எல்லா தொலைபேசிகளுக்கும் தெரிவிக்கிறது.

**என்ன கேட்கிறது**

YAMNet 521 ஒலிகளுக்குப் பெயர் சொல்கிறது. இந்தச் செயலி அவற்றில் பதினெட்டை மட்டும் கவனிக்கிறது, அந்தப் பதினெட்டே மட்டுமே எச்சரிக்கை தரும். நிலைக்கதிர், தொலைக்காட்சி, சலவை இயந்திரம், தட்டச்சு - இவை விவேகமாக விலக்கப்பட்டுள்ளன. இரவு மூன்று மணிக்கு ஒரு fan-க்காகக் குடும்பத்தை எழுப்பும் செயலி, செயலி இல்லாததை விட மோசம்.

அந்தப் பதினெட்டிலும் ஒரு தமிழ்ப் பெயரும் ஒரு தமிழ் வாக்கியமும் உள்ளன, அதுவே மற்ற தொலைபேசிகள் பெறுவது:

- **புகை எச்சரிக்கை, தீ எச்சரிக்கை, தீ, சைரன், பேரழிவ்ச்சை சைரன்** - தீயும் புகையும் வகை.
- **கத்தும் ஒலி, கூச்சல், அலறும் ஒலி, குழந்தைகள் கூச்சல், அழும் ஒலி, குழந்தை அழுகை, உடைந்து விழும் ஒலி, கண்ணாடி உடைவு, துப்பாக்கி ஒலி** - யாரோ உதவி தேவை.
- **கதவு மணி, டிங்-டாங், மணி ஒலி** - கதவு.

தீ எச்சரிக்கை 45% நம்பகத்தன்மை, கூச்சல் 55%, கதவு மணி 40%. இவை நியாயமான எண்கள்: கதவு மணி ஒரு குறுகிய ஒலி, அதை YAMNet நன்றாக அறிகிறது; கூச்சலை அறியாது. இரண்டுக்கும் ஒரே எண்ணைக் கேட்டால், ஒன்றில் எல்லா கூச்சலும் தவறிப்போம், இல்லையென்றால் விவாதத்தில் எழும் ஒவ்வொரு குரலும் எச்சரிக்கை தரும்.

**எச்சரிக்கை எப்படிப் பயணிக்கிறது**

எச்சரிக்கை உள்ளூர் வலையத்தில் ஒரு UDP packet. சேவையகம் இல்லை, கணக்கு இல்லை, மேகக்கணி இல்லை, இணைப்புத் திரை இல்லை. அதே வைஃபையில் இந்தச் செயலியைத் திறந்து கேட்க வைத்துள்ள ஒவ்வொரு தொலைபேசியும் கேட்கும்.

- **இணையம் தேவை இல்லை.** வழங்கியின் இணைப்பு போனாலும் இது வேலை செய்யும், அதுவே குடும்பத்திற்கு தேவைப்படும் நேரம்.
- **ஒலிபரப்பு முகவரி வைஃபை இடைமுகத்திலிருந்து படிக்கப்படுகிறது**, ஊகிக்கப்படவில்லை. பல வீட்டு வழங்கிகள் உலகளாவிய ஒலிபரப்பைத் தள்ளி, நேரடியை அனுப்புகின்றன.
- **ஒவ்வொரு packet-ம் மூன்று நகல்களில்**, UDP பகிரப்பட்ட வலையத்தில் packet-களை இழக்கிறது, இது மீண்டும் அனுப்ப எதிர்பார்க்கும் ஒரு ஓட்டம் அல்ல.
- **ஒரு ping மற்றும் ஒரு பதில்**, எந்தத் தொலைபேசிகள் உண்மையில் இணைந்துள்ளன என்பதைத் திரை காட்டுவதற்கு. அது இல்லாமல் அவசரம் இருந்த தொலைபேசிகளை மட்டுமே காட்ட முடியும், அது வேலை செய்கிறதா என்பதை அறிய வழி இல்லை.
- **பதினைந்து வினாடிக்குள் அதே ஒலி மீண்டும் வந்தால் அது ஒரு நிகழ்வு**, முப்பதை அல்ல. அரை நிமிடம் ஒலிக்கும் புகை எச்சரிக்கை ஒரு எச்சரிக்கை, ஒரு அறிவிப்பு, ஒரு packet.

**இரண்டு வரம்புகள், நேர்மையாகச் சொல்லப்பட்டவை**

- **ஆண்ட்ராய்டு எந்தச் செயலிக்கும் தொலைபேசி அழைப்பின் மறுபக்க ஒலியைக் கேட்க அனுமதிக்காது.** இந்தச் செயலி அறையைக் கேட்கிறது, அழைப்பை அல்ல. தொலைபேசியில் உள்ள எந்தச் செயலிக்கும் இது உண்மை; இது ஒரு செயலி சுற்றிப் பார்க்கக்கூடியது அல்ல.
- **இது ஒரு எச்சரிக்கை, காவலர் அல்ல.** சில நேரம் பொதுவான ஒலியை அவசர ஒலி என நினைக்கும். ஒவ்வொரு எச்சரிக்கைக்குப் பக்கத்தில் நம்பகத்தன்மை சதவீதம் காட்டப்படுவது தவறானதைத் தீர்ப்பதற்கே.

**இதில் இல்லாதவை**

SMS விவேகமாக இல்லை. உரிமையாளர் தேர்ந்தெடுத்த நோக்கம் உள்ளூர் வலையம், மற்றும் ஒரு தொலைபேசியிலிருந்து குறுஞ்செய்தி அனுப்பக்கூடிய செயலி வேறு அனுமதியும் வேறு அபாயமும். SMS ஒருபோதும் வேண்டுமானால் அது தனி, வெளிப்படையாக அனுமதிக்கப்படும் படி.

இருப்பிடம் இல்லை, தொடர்புகள் இல்லை, சேமிப்பு இல்லை, கேமரா இல்லை, தொலைபேசி நிலை இல்லை, விளம்பர அடையாளம் இல்லை. அனுமதிகள்: RECORD_AUDIO, INTERNET (உள்ளூர் socket-க்கு மட்டும்), VIBRATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, RECEIVE_BOOT_COMPLETED.

**சாதனப் பரிசோதனை நிலை**

**இந்த 1.0 பதிப்பு யாராலும் எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** கம்பி நெறிமுறை, நகல் தடுப்பு, அனுப்பப்பட்ட மாதிரிக்கு எதிரான அவசர சொற்கள், நிலை கிடங்கு ஆகியவை அலகு சோதனையில் உள்ளன, பகிரப்பட்ட மாதிரியும் அடையாளக் கோப்பும் வசந்தி AI ஏற்கனவே அனுப்பும் அதேவை. உள்ளதல்லாதவை: ஒரு முழு இரவு மைக் திறந்து இருக்கிறதா, packet-கள் உண்மையில் ஒரு வீட்டு வழங்கியைத் தாண்டிச் செல்கிறதா, கடுமையான மின் மேலாண்மை உள்ள தொலைபேசியில் எப்படி நடந்துகொள்கிறது. இவை ஒரு உண்மையான தொலைபேசி, ஒரு உண்மையான வீடு தேவை.
