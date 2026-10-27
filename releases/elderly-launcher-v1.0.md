# Elderly Launcher 1.0 - a home screen with big buttons, no small text and no menus

The owner asked for a launcher for an elderly person as the last of the three apps. This release is that, and nothing else.

It replaces the phone's home screen with eight very large buttons and nothing else: phone, messages, camera, contacts, torch, volume up, volume down, settings. Below them, a separate red button for an emergency call, and one more for the list of every installed app.

## Why it is built this way

The screen is for somebody who does not read small text and does not navigate menus. So there is no settings screen, no list to scroll, and nothing to configure: the same eight buttons are in the same places every time, and the four most-used ones are nearest the thumb.

- **Every label is at least 18sp and every button is at least 96dp tall.** The clock is 44sp.
- **The torch and the two volume buttons are on the home screen**, because reaching the real volume slider on a modern Android phone is a gesture, and a gesture is exactly what this screen exists to avoid.
- **The emergency button is a different colour and sits alone**, so it cannot be pressed by mistake while reaching for something else. It asks first, then dials.
- **The time is shown large**, because a phone that does not say what time it is on its front screen has failed at the one thing a home screen is for.

## What it does and does not do

Every button either starts an app or acts on the phone directly, and each one fails quietly: a launcher that crashes when the camera app is missing is worse than no launcher at all, because the phone becomes unusable. If the phone has no app that answers a button, the screen says so rather than doing nothing.

The emergency number is 108, India's emergency number, and it is shown before the call is placed.

## Permissions

`QUERY_ALL_PACKAGES` to list the installed apps, which is what a launcher is for and is the one category Google Play explicitly allows it for. `CALL_PHONE` to place the emergency call directly; if it has not been granted, the dialler opens with the number already filled in instead, so the button still does something useful. `FLASHLIGHT` and `VIBRATE` for the torch, `POST_NOTIFICATIONS` for Android 13+.

No location, no contacts, no storage, no camera use of its own, no phone state, no advertising identifier, no accounts.

## Device test status

**This 1.0 build has not been installed on a phone by anyone, so it has had no device test.** The home screen's shape and order are covered by unit tests. What is not covered: whether it actually becomes the home screen when the owner presses Home, whether the torch toggles on a real device, and whether the emergency call goes through. A launcher is the one kind of app that cannot be judged from anything but a phone, because being the home screen is the whole feature.

## தமிழ்

**முதியவர் திரை 1.0** - மூன்று செயலிகளில் கடைசியாக உரிமையாளர் கேட்டது. இந்தப் பதிப்பு அது மட்டுமே.

தொலைபேசியின் முகத் திரையை எட்டு பெரிய பொத்தான்களாக மாற்றுகிறது: தொலைபேசி, செய்திகள், கேமரா, தொடர்புகள், விளக்கு, ஒலி அதிகரிப்பு, ஒலி குறைப்பு, அமைப்பு. அவற்றின் கீழ், அவசர அழைப்புக்கு தனித்த சிவப்புப் பொத்தான், மற்ற எல்லா செயலிகளின் பட்டியலுக்கு ஒன்று.

**ஏன் இவ்வாறு கட்டப்பட்டது**

இந்தத் திரை சிறிய எழுத்தைப் படிக்காத, மெனுவை நடத்தாத ஒருவருக்கு. எனவே அமைப்புத் திரை இல்லை, உருட்டும் பட்டியல் இல்லை, அமைக்க எதுவும் இல்லை: அதே எட்டுப் பொத்தான்கள் எப்போதும் அதே இடத்தில், அதிகம் பயன்படும் நான்கு விரல் அருகில்.

- **ஒவ்வொரு எழுத்தும் குறைந்தது 18sp, ஒவ்வொரு பொத்தானும் குறைந்தது 96dp.** கடிகாரம் 44sp.
- **விளக்கும் இரு ஒலிப் பொத்தான்களும் முகத் திரையில்**, ஏனெனில் நவீன ஆண்ட்ராய்டு தொலைபேசியில் உண்மையான ஒலி ஸ்லைடரை அடைவது ஒரு சைகை, சைகை என்பது இந்தத் திரை தவிர்க்கும் அதே விஷயம்.
- **அவசரப் பொத்தான் வேறு நிறம், தனியாக**, எனவே வேறு ஏதாவது எட்டும் போது தவறாக அழுத்தப்பட முடியாது. அது முதலில் கேட்கிறது, பிறகு அழைக்கிறது.
- **நேரம் பெரியதாகக் காட்டப்படுகிறது**, ஏனெனில் தன் முகத் திரையில் எண்ணை சொல்லாத தொலைபேசி, ஒரு முகத் திரை இருக்கும் ஒரே காரணத்தில் தோல்வி அடைந்தது.

**என்ன செய்யும், என்ன செய்யாது**

ஒவ்வொரு பொத்தானும் ஒரு செயலியைத் தொடங்குவது அல்லது தொலைபேசியை நேரடியாகச் செயல்படுத்துவது, ஒவ்வொன்றும் அமைதியாகத் தோல்வியடைகிறது: கேமரா செயலி இல்லாத போது விழும் லான்ச்சர், லான்ச்சர் இல்லாததை விட மோசம், ஏனெனில் தொலைபேசி பயன்படுத்த முடியாததாகி விடும். ஒரு பொத்தானுக்கு பதில் சொல்லும் செயலி இல்லையென்றால் திரை அதைச் சொல்லும், ஒன்றுமே செய்யாமல் இருப்பதை விட.

அவசர எண் 108, இந்தியாவின் அவசர எண், அழைப்பு முன் அது காட்டப்படுகிறது.

**அனுமதிகள்**

நிறுவப்பட்ட செயலிகளைப் பட்டியலிட `QUERY_ALL_PACKAGES`, இது ஒரு லான்ச்சர் இருக்கும் காரணமே, Google Play வெளிப்படையாக அனுமதிக்கும் ஒரே வகை. அவசர அழைப்பை நேரடியாகச் செய்ய `CALL_PHONE`; அது வழங்கப்படாவிட்டால் எண் ஏற்கனவே நிரம்பிய டயலர் திறக்கும், எனவே பொத்தான் பயனுள்ள ஒன்றை இன்னும் செய்யும். விளக்குக்கு `FLASHLIGHT` மற்றும் `VIBRATE`, ஆண்ட்ராய்டு 13+ க்கு `POST_NOTIFICATIONS`.

இருப்பிடம், தொடர்புகள், சேமிப்பு, சொந்த கேமரா பயன்பாடு, போன் நிலை, விளம்பர அடையாளம், கணக்கு - இவை எதுவும் இல்லை.

**சாதனப் பரிசோதனை நிலை**

**இந்த 1.0 பதிப்பு யாராலும் எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** முகத் திரையின் வடிவம் மற்றும் வரிசை அலகு சோதனையில் உள்ளன. இல்லாதவை: உரிமையாளர் முகப்பு அழுத்திய போது இது உண்மையில் முகத் திரையாகிறதா, உண்மையான சாதனத்தில் விளக்கு மாறுகிறதா, அவசர அழைப்பு செல்கிறதா. லான்ச்சர் என்பது ஒரு தொலைபேசியில் இருந்து மட்டுமே தீர்மானிக்கக்கூடிய ஒரே வகை செயலி, ஏனெனில் முகத் திரை ஆக இருப்பதே முழு வசதி.
