# Live Guard 2.50

Nearby cell towers that actually appear, and one pack number on every screen.

## Highlights

- **Fixed — the nearby-cell list was impossible to unlock on Android 13 and newer.** Live Guard asked Android for Precise location, but the permission was declared with a maximum SDK of 32, so on a modern phone the grant could never succeed and the list stayed empty behind the note *"Android only shares them with Location allowed"*. Location is now declared for every Android version, and `ACCESS_COARSE_LOCATION` is declared beside `ACCESS_FINE_LOCATION` because Android 12+ ignores a request for Precise on its own.
- **New — a "Nearby cell towers" card on the Mobile screen.** It explains the one reason the list needs location, has its own **Allow location and rescan** button, and a link to the app settings when Android has already stopped asking. Choose **Precise** — *Approximate* is not enough for tower ids, and the card says so instead of leaving an empty list.
- **Said plainly: location is read only while the Mobile screen builds a reading.** It is not used in the background, not stored, and not uploaded. No location history is kept.
- **The list is ordered the way "nearby" reads** — the serving cell first, then the strongest neighbours by dBm, up to 16 read and 10 shown, with a short note saying so. If the phone or the carrier simply reports nothing, the screen now says that instead of blaming a missing permission.
- **Fixed — one pack number, everywhere.** My plan is the answer: base total plus a booster, exactly as the plan screen shows it. The notification card, the status pill, the widgets and the 4G/5G split all divide by that same effective total now. A value that the first 2.49 build had written from the base only is repaired on the next reading.
- **Fixed — "Reset recharge" is not silently undone.** Moving the counter by hand, then editing an unrelated plan field, no longer throws the new start day away.
- **Fixed — the split card shows what was counted.** It reports the 4G/5G amounts actually measured plus the time counting started, instead of a "left" figure that ignored usage from before the screen was opened.
- **New guidance** in About → Guide: what the nearby-cell list is, why location is needed for it, and what the pill colours mean.
- **One new Android permission is requested: Location** (Coarse and Precise, in one dialog, only when you tap the new card). Nothing else was added. The Phone button asks for Phone only; on Android 13+ the Wi-Fi tools ask for Nearby devices only, so no prompt carries a hidden second reason.

## Upgrading

The upgrade installs over 2.49 with the same signing key, so plans, settings, language and history are kept. Location starts **off**: Live Guard only asks when you tap **Allow location and rescan**. If you later turn location off in Android settings, the tower card simply comes back with the button.

## Installation and safety

Download `CleanSweep-v2.50.apk` and install it through Android. If Android asks, enable **Allow from this source** for the browser or file manager you used, then confirm in Android's installer. No silent installation and no Play Protect bypass is used.

**Device testing:** this build has **not been installed on a phone** at the time these notes were written, and no physical-device test is claimed for these exact bytes yet. The release passed the configured CI build, unit and regression suites, the APK checks and the permission-delta gate. That is not Play Protect approval and not an all-device compatibility claim.

## தமிழ்

# லைவ் கார்டு 2.50

அருகிலுள்ள செல் டவர்கள் உண்மையில் தெரியும்; எல்லாத் திரைகளிலும் ஒரே பேக் அளவு.

- **சரிசெய்யப்பட்டது — Android 13 மற்றும் அதற்கு மேல் அருகிலுள்ள செல் பட்டியல் வரவே வழியில்லை.** லைவ் கார்டு Precise location கேட்டது, ஆனால் அனுமதி `maxSdkVersion="32"` உடன் இருந்தது; புதிய போனில் அந்த அனுமதி ஒருபோதும் வெற்றி பெறாது. இப்போது எல்லா Android பதிப்புகளுக்கும் Location சேர்க்கப்பட்டுள்ளது, Android 12+ தனியாக Precise கேட்டால் புறக்கணிப்பதால் `ACCESS_COARSE_LOCATION`-உம் சேர்க்கப்பட்டுள்ளது.
- **புதியது — Mobile திரையில் "அருகிலுள்ள செல் டவர்கள்" அட்டை.** பட்டியலுக்கு ஏன் location தேவை என்று விளக்குகிறது; **இருப்பிடத்தை அனுமதித்து மீண்டும் படி** பொத்தான் அதற்கே. Android மீண்டும் கேட்காதபோது **செயலி அமைப்புகள்** வழி கொடுக்கிறது. **Precise** தேர்ந்தெடுக்கவும் — *தோராயமானது* டவர் ஐடிக்குப் போதாது, அதை அட்டையே சொல்கிறது.
- **தெளிவாக: Mobile திரை படிக்கும்போது மட்டுமே location படிக்கப்படுகிறது.** பின்னணியில் படிக்கப்படுவதில்லை, சேமிக்கப்படுவதில்லை, எங்கும் அனுப்பப்படுவதில்லை.
- **பட்டியல் வரிசை அர்த்தமுள்ளது** — சேவை செய்யும் செல் முதலில், பின்னர் dBm அடிப்படையில் வலுவான அண்டை செல்கள்; 16 வரை படித்து 10 காட்டப்படும். போனோ நெட்வொர்க்கோ எதுவும் தரவில்லை என்றால், அதை நேர்மையாகச் சொல்கிறது.
- **சரிசெய்யப்பட்டது — எல்லா இடமும் ஒரே பேக் அளவு.** என் திட்டம் = அடிப்படை + பூஸ்டர். அறிவிப்பு, pill, widget, 4G/5G பிரிவு அனைத்தும் அதே அளவால் வகுக்கின்றன. 2.49-இன் முதல் பதிப்பு எழுதிய தவறான மதிப்பு தானாகச் சரிசெய்யப்படும்.
- **சரிசெய்யப்பட்டது — "ரீசார்ஜ் கவுன்டரை மீட்டமை" அமைதியாக ரத்தாகாது.** கைமுறை மீட்டமைப்புக்குப் பிறகு திட்டத்தின் வேறு ஒரு புலத்தைச் சேமித்தாலும் புதிய தொடக்க நாள் போகாது.
- **சரிசெய்யப்பட்டது — பிரிவு அட்டை எண்ணியதைக் காட்டுகிறது.** திரை திறக்கும் முன்னர் பயன்படுத்திய தரவை விட்டுவிட்ட "மீதம்" எண்ணுக்குப் பதிலாக, அளந்த 4G/5G அளவும் எண்ணத் தொடங்கிய நேரமும் காட்டப்படும்.
- **புதிய வழிகாட்டி** — About → வழிகாட்டி: செல் டவர் பட்டியல் என்றால் என்ன, ஏன் location தேவை, pill நிறங்களின் பொருள்.
- **புதிய Android அனுமதி ஒன்றே: Location** (Coarse + Precise, ஒரே சாளரத்தில், நீங்கள் அட்டையைத் தொட்டால் மட்டும்). வேறு எதுவும் சேர்க்கப்படவில்லை.

## நிறுவுதல்

`CleanSweep-v2.50.apk`-ஐ Android வழியாக நிறுவவும். கேட்டால் **இந்த மூலத்திலிருந்து நிறுவு** அனுமதியை இயக்கி உறுதிப்படுத்தவும். அமைதியான நிறுவல் இல்லை; Play Protect தவிர்க்கப்படவில்லை. 2.49-இன் மேல் அதே கையொப்பத்துடன் ஏறும் — திட்டம், அமைப்புகள், மொழி, வரலாறு அப்படியே இருக்கும். Location இயல்பாக **அணைந்தே** இருக்கும்.
