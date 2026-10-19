# Live Guard 2.49

**My plan** — one screen for the whole recharge — plus a guide in the app, and two display fixes.

## Highlights

- **New — Mobile → My plan.** The old pack dialog, the daily chip row and the start/expiry screen are now one screen that asks the questions in the order a person thinks: what does this recharge give (**No data / Daily / Total**), how much, how long, is there a booster, and is 5G free on this plan.
- **Every amount accepts a typed value in GB or MB.** Presets are shortcuts, not a closed list: 0.75 GB, 500 MB and 1.2 GB all work, and a typed "1.5 GB" is understood even when MB is selected.
- **Start date and number of days**, with the last day shown and a plain "N days left" or "This pack has ended".
- **Booster** — on/off, any amount, and whether your operator uses it **before** or **after** the daily data. The phone cannot see the carrier's billing, so the app follows the order you state and says so on the screen.
- **Fixed — the 4G / 5G Auto / On / Off buttons.** All three looked identical on the dark theme; the chosen one is now filled, outlined and marked with a tick.
- **Fixed — the split counters no longer look wrong.** The card now says when counting started ("Counted since 4:28 PM — earlier usage today is in the mobile total above"), so a small 4G figure next to a large daily total is explained instead of confusing.
- **New — a guide in the app**: About → Guide, in Tamil and English, with short questions that open when tapped.
- Nothing on the My plan screen resets a counter, and it never claims to read your carrier's balance. There is no new Android permission.

## Installation and safety

Download `CleanSweep-v2.49.apk` and install it through Android. If Android asks, enable **Allow from this source** for the browser or file manager you used, then confirm in Android's installer. No silent installation and no Play Protect bypass is used.

**Device testing:** these exact CI-built bytes were **not installed on a phone** during this release run; no physical-device test is claimed for this APK. The release passed the configured CI, unit-test and permission-delta checks. That is not Play Protect approval and not an all-device compatibility claim.

## தமிழ்

# லைவ் கார்டு 2.49

**என் திட்டம்** — முழு ரீசார்ஜ்க்கும் ஒரே திரை — மற்றும் செயலிக்குள் ஒரு வழிகாட்டி, இரண்டு காட்சிச் சரிசெய்தல்கள்.

- **புதியது — Mobile → என் திட்டம்.** பழைய பேக் சாளரம், தினசரி chip வரிசை, தொடக்க/காலாவதித் திரை ஆகியவை இப்போது ஒரே திரை. தரவு இல்லை / தினசரி / மொத்தம், எவ்வளவு, எத்தனை நாட்கள், பூஸ்டர் இருக்கிறதா, 5G இலவசமா — மனிதர் நினைக்கும் வரிசையில் கேட்கிறது.
- **ஒவ்வொரு அளவையும் GB அல்லது MB-இல் தட்டச்சு செய்யலாம்.** 0.75 GB, 500 MB, 1.2 GB எல்லாம் வேலை செய்யும்; MB தேர்ந்திருந்தாலும் "1.5 GB" புரியும்.
- **தொடக்க தேதி + நாட்கள்**, கடைசி நாள் மற்றும் "N நாட்கள் மீதம்" அல்லது "இந்தப் பேக் முடிந்தது".
- **பூஸ்டர்** — on/off, எந்த அளவும், தினசரி தரவுக்கு **முன்** அல்லது **பின்** என்பதை நீங்கள் சொல்வதைப் பின்பற்றும்; நிறுவனத்தின் பில்லிங்கைப் போனால் பார்க்க முடியாது என்பதைத் திரையிலேயே கூறும்.
- **சரிசெய்யப்பட்டது** — 4G / 5G Auto / On / Off பொத்தான்கள்: தேர்ந்தது இப்போது நிரப்பப்பட்டு, விளிம்புடன், ✓ குறியுடன் தெரியும்.
- **சரிசெய்யப்பட்டது** — பிரிப்புக் கணக்கு எப்போது தொடங்கியது என்பதைக் காட்டும், எனவே சிறிய 4G எண் குழப்பமாகத் தெரியாது.
- **புதியது — வழிகாட்டி**: About → Guide, தமிழ் + ஆங்கிலம், தொட்டால் திறக்கும் குறுகிய கேள்விகள்.
- எந்தக் கணக்கும் மீட்டமைக்கப்படுவதில்லை; புதிய Android அனுமதி இல்லை.

`CleanSweep-v2.49.apk`-ஐ Android வழியாக நிறுவவும். கேட்டால் **இந்த மூலத்திலிருந்து நிறுவு** அனுமதியை இயக்கி உறுதிப்படுத்தவும். அமைதியான நிறுவல் இல்லை; Play Protect தவிர்க்கப்படவில்லை.

**சாதனச் சோதனை:** இந்த CI கோப்பு இந்த வெளியீட்டில் போனில் நிறுவப்படவில்லை; இதற்கு சாதனச் சோதனை கூறப்படவில்லை. CI மற்றும் அனுமதி சோதனைகள் மட்டுமே வெற்றி பெற்றன. இது Play Protect ஒப்புதல் அல்ல.
