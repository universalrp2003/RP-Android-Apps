# Secret Message 1.1.2

A small Help button on the connection screen opens a more complete guide in Tamil and English.

**Not independently security-reviewed.** No outside security expert has reviewed this app, and the mls-rs library has not had a full third-party audit. Do not rely on it where your safety depends on secrecy.

## What's new

- **Guide on the main screen:** tap the help icon beside the Secret Message header to open the guide directly. Topics expand when tapped and cover Internet-relay versus same-trusted-Wi-Fi connections, sending invites, comparing the 20-digit safety code, remembered phones, message timers, incoming tone, optional app lock, staying ready for chats, updates, privacy and limits.
- The guide now explains the relay's visibility: it can see both phones' IP addresses, connection times, traffic sizes and the initial invite/device IDs/public keys before the safety-code comparison. It forwards encrypted chat data but cannot read messages, drawings or pictures. Relay rooms are transient and the service is configured not to retain message content; this does not hide connection metadata.
- No change to the MLS protocol, encryption, message expiry, relay, or call behavior. No new Android permission.

## What the relay can see

Both phones' IP addresses, connection times and traffic sizes, and the start of each session before the safety-code check (invite token, device IDs and public keys). It cannot read messages, drawings or pictures. The relay forwards bytes and does not keep a chat history; connection metadata remains visible to the relay operator and Cloudflare's account-level service statistics may exist.

## Install and verification

`Secret-Message-v1.1.2.apk` supports Android 8.0 (API 26) or newer and installs over 1.1.1 with the same project signing key. Build, unit tests, signing checks and the hosted emulator smoke are run by the standalone workflow. The exact APK was not installed on a phone before publication; an emulator check is not a physical-device test.

Version 1.1.2 / code 4.

## தமிழ்

# ரகசிய செய்தி 1.1.2

இணைப்புத் திரையிலுள்ள சிறிய உதவிப் பொத்தான் விரிவான தமிழ்/ஆங்கில வழிகாட்டியைத் திறக்கும்.

**சுயாதீனப் பாதுகாப்பு மதிப்பாய்வு செய்யப்படவில்லை.** வெளிப்புறப் பாதுகாப்பு நிபுணர் யாரும் இந்தச் செயலியை மதிப்பாய்வு செய்யவில்லை; mls-rs முழுமையான மூன்றாம் தரப்புத் தணிக்கையும் செய்யப்படவில்லை. உங்கள் பாதுகாப்பு ரகசியத்தன்மையைச் சார்ந்த சூழலில் இதை நம்ப வேண்டாம்.

## புதியது

- **முக்கியத் திரையில் வழிகாட்டி:** Secret Message தலைப்பருகிலுள்ள உதவிப் பொத்தானைத் தட்டுங்கள். தலைப்புகளைத் தொட்டால் உரை விரியும். இணைய relay மற்றும் ஒரே நம்பகமான Wi-Fi இணைப்பு, அழைப்பிதழ் அனுப்புதல், 20 இலக்கப் பாதுகாப்புக் குறியீட்டை ஒப்பிடுதல், நினைவில் உள்ள தொலைபேசிகள், செய்தி நேரம், வரும் செய்தி ஒலி, விருப்ப செயலிப் பூட்டு, உரையாடலுக்குத் தயாராக வைத்தல், புதுப்பிப்பு, தனியுரிமை, வரம்புகள் ஆகியவை விளக்கப்படும்.
- relay பார்க்கக்கூடியதை வழிகாட்டி தெளிவாகச் சொல்கிறது: இரு போன்களின் IP முகவரி, இணையும் நேரம், தரவு அளவு, குறியீடு ஒப்பிடுவதற்கு முன் அழைப்பிதழ்/சாதன ID/பொது விசைகள். அது குறியாக்கப்பட்ட அரட்டைத் தரவை அனுப்பும்; செய்தி, வரைபடம், படம் ஆகியவற்றைப் படிக்க முடியாது. relay அறை தற்காலிகமானது; செய்தி உள்ளடக்கத்தைச் சேமிக்காதபடி சேவை அமைக்கப்பட்டுள்ளது. ஆனால் இணைப்பு பற்றிய metadata மறைக்கப்படாது.
- MLS நெறிமுறை, குறியாக்கம், செய்தி காலாவதி, relay அல்லது அழைப்பு நடத்தையில் மாற்றமில்லை. புதிய Android அனுமதி இல்லை.

## relay எதைப் பார்க்க முடியும்

இரு போன்களின் IP முகவரிகள், இணைப்பு நேரங்கள், தரவு அளவுகள், பாதுகாப்புக் குறியீடு ஒப்பிடுவதற்கு முன் அமர்வின் தொடக்கத் தகவல்கள் (அழைப்பிதழ் token, சாதன ID-கள், பொது விசைகள்) relay-க்கு தெரியும். செய்தி, வரைபடம், படம் ஆகியவற்றைப் படிக்க முடியாது. relay செய்திகளை அனுப்பி, அரட்டை வரலாற்றை வைத்திருக்காது; இணைப்பு metadata relay இயக்குநருக்குத் தெரியும், Cloudflare கணக்கு நிலைச் சேவை புள்ளிவிவரங்களும் இருக்கலாம்.

## நிறுவுதல் மற்றும் சோதனை

`Secret-Message-v1.1.2.apk` Android 8.0 (API 26) அல்லது புதிய பதிப்பை ஆதரிக்கிறது; அதே திட்டக் கையொப்பக் குறியுடன் 1.1.1 மேல் நிறுவலாம். தனி workflow build, unit tests, கையொப்பச் சரிபார்ப்பு, hosted emulator சோதனை ஆகியவற்றைச் செய்கிறது. வெளியீட்டுக்கு முன் இந்தத் துல்லியமான APK-ஐ போனில் நிறுவிச் சோதிக்கவில்லை; emulator சோதனை என்பது நேரடி போன் சோதனை அல்ல.

பதிப்பு 1.1.2 / code 4.
