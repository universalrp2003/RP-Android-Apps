# Secret Message 1.1.1

A guide inside the app, and small fixes. No change to the encryption, the relay or the call behaviour.

**Not independently security-reviewed.** No outside security expert has reviewed this app, and the mls-rs library has not had a full third-party audit. Do not rely on it where your safety depends on secrecy.

## What's new

- **Guide** (Settings → Guide) in Tamil and English: short questions that open when tapped — how invites work, what the safety code means, what the relay can see, and what the app does not protect against.
- **Fixed — the guide screen's layout** on narrow phones (a width constraint was missing, so the cards could overflow).
- Everything else is unchanged from 1.1.0: remembered phones, calls, names and status, ready for chats, Tamil and English, and the verified updater.

## What the relay can see

Unchanged: both phones' IP addresses, when they connect and how much data they send, and the start of each session before the safety-code check; it cannot read messages, drawings or pictures.

## Install

- `Secret-Message-v1.1.1.apk`: Android 8.0 (API 26) or newer. It installs over 1.1.0 and keeps your remembered phones.
- Package `com.rpideas.secretmessage`, signed with the project's own release key; the signing certificate is checked by the updater before Android's installer asks you.

## தமிழ்

# ரகசிய செய்தி 1.1.1

செயலிக்குள் வழிகாட்டி, சிறிய சரிசெய்தல்கள். குறியாக்கம், relay, அழைப்பு நடத்தையில் மாற்றம் இல்லை.

**சுயாதீனப் பாதுகாப்பு மதிப்பாய்வு செய்யப்படவில்லை.** வெளிப் பாதுகாப்பு நிபுணர் யாரும் இதை மதிப்பாய்வு செய்யவில்லை; mls-rs முழுமையான தணிக்கை செய்யப்படவில்லை.

- **வழிகாட்டி** (அமைப்புகள் → வழிகாட்டி) தமிழ் + ஆங்கிலம்: அழைப்பிதழ், safety code, relay எதைப் பார்க்கும், செயலி எதைப் பாதுகாக்காது.
- **சரிசெய்யப்பட்டது** — குறுகிய போன்களில் வழிகாட்டித் திரையின் அகலம்.
- மற்றவை 1.1.0 போலவே: நினைவில் உள்ள தொலைபேசிகள், அழைப்புகள், பெயர்கள், தமிழ்/ஆங்கிலம், சரிபார்க்கப்பட்ட புதுப்பிப்பு.

`Secret-Message-v1.1.1.apk`: Android 8.0 (API 26) அல்லது புதியது. 1.1.0 மேல் நிறுவலாம்; நினைவில் உள்ள தொலைபேசிகள் பாதுகாக்கப்படும்.
