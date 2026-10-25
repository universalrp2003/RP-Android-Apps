# Ramesh Radio 1.25 — More than one saved place

**Version:** 1.25 · **version code 25** · **App:** Ramesh Radio

Rain and cyclone voice alerts could only ever point at one place. You now keep your own list of
places and switch between them with one tap.

No new permissions. No change to how alerts are worded, when they fire, or how the radio ducks and
restores its volume.

## What changed

- A row of chips under the weather banner, one per saved place. The current one is highlighted.
- Tapping a chip makes that place active immediately: the alert re-checks and speaks for the new
  town. No searching, no re-typing the PIN.
- The ✕ on a chip forgets that place. If you remove the active place, the next one in your list
  takes over, so alerts never keep pointing at a place you deleted.
- Every place you pick from the search box is remembered automatically, so the list fills itself as
  you use it.
- The chips only appear once you have more than one place, so a first-time owner sees nothing extra.

Up to 12 places. Re-picking a place you already have moves it to the front instead of adding a
duplicate, and two picks that resolve to the same coordinates count as one place.

## How it is built

The shared weather runtime keeps exactly one active place, and it is shared with Live Guard and Oor
Arivippu, so it is deliberately unchanged: this list lives in Ramesh Radio's own storage and only
hands a place to the shared runtime when you pick one. The other two apps see the new place exactly
as they always did.

Storage is one line per place with control-character separators — no JSON dependency, and a
half-written or hand-edited line costs you one place rather than the whole list. The
encode/decode round-trip, the dedupe key and the cap are unit-tested.

## Device test

Installed on a phone on 25 October 2026 and updated in-app from 1.24. The saved place, the rain
dialog and the zero-location-permission message all render, and a live check returned "Checked 4:37
PM - Inunganur (Karur district, Tamil Nadu 639205) - Rain expected within ~30 min". Play Protect
reported "This app looks safe".

Still to check on a phone: the whole point of this release. The phone held exactly one saved place,
and the chips only appear once there is more than one, so the chip strip, one-tap switching, the
highlight following the active place, and the X forgetting a place have not been exercised on a
device yet.

## தமிழ் சுருக்கம்

- **ஒன்றுக்கு மேற்பட்ட சேமித்த ஊர்கள்:** மழை & புயல் குரல் எச்சரிக்கைகள் இதுவரை ஒரு ஊரை மட்டும்
  காட்டிவந்தன. இப்போது உங்கள் சொந்த ஊர் பட்டியலை வைத்துக்கொள்ளலாம் — வானிலை பட்டியின் கீழ்
  ஒவ்வொரு ஊருக்கும் ஒரு chip-ஆகத் தெரியும். இயக்கப்படும் ஊர் highlights செய்யப்படும்.
- **ஒரு தட்டுதலால் ஊர் மாறும்:** தேடுவது தேவையில்லை, PIN-ஐ மீண்டும் type செய்ய தேவையில்லை.
  எச்சரிக்கை புதிய ஊருக்கு மீண்டும் சோதித்து பேசும்.
- **× அழித்தால் ஊர் அழிகிறது:** இயக்க ஊரை அழித்தால் அடுத்த ஊர் தானாக எடுக்கப்படும், அதனால்
  நீங்கள் அழித்த ஊருக்கு எச்சரிக்கை பேசாமல் இருக்கும்.
- **தேடலில் தேர்வு செய்த ஒவ்வொரு ஊரும் தானாகச் சேமிக்கப்படும்.** அதனால் நீங்கள் பயன்படுத்தும்போதே
  பட்டியல் நிரம்பும். முதல் முறை பயன்படுத்துபவருக்கு ஒரு ஊர் மட்டுமே இருந்தால் chip-கள்
  தெரியாது.
- **12 ஊர்கள் வரை.** ஏற்கனவே உள்ள ஊரை மீண்டும் தேர்வு செய்தால் நகல் சேராமல் முன்னால் நகரும்;
  ஒரே coordinates-க்கு இரண்டு தேர்வுகள் ஒரே ஊராகவே கணக்கிடப்படும்.
- **புதிய அனுமதி இல்லை.** எச்சரிக்கை சொற்கள், எப்போது பேசும், radio எந்த ஆரத்தில் குறைகிறது
  மற்றும் திரும்ப பெறுகிறது — இவை எல்லாம் மாறவில்லை.
- **ஃபோனில் நிறுவப்பட்டு சோதிக்கப்பட்டது (25 அக்டோபர் 2026).** 1.24-ல் இருந்து செயலியில்
  மேம்படுத்தப்பட்டது. சேமித்த ஊர், மழை உரையாடல் மற்றும் இருப்பிட அனுமதி இல்லாத நிலை
  செய்தி - இவை மூன்றும் சரியாகத் தெரியும். Play Protect "இந்த செயலி பாதுகாப்பானது" என்று
  தெரிவித்தது.
- **இன்னும் சோதிக்கப்படவில்லை:** இந்த வெளியீட்டின் முக்கிய நோக்கம். ஃபோனில் ஒரே ஒரு
  சேமித்த ஊர் மட்டுமே இருந்தது, chip-கள் ஒன்றுக்கு மேற்பட்ட ஊர் இருந்தால் மட்டுமே
  தெரியும். அதனால் chip-ஐத் தட்டி ஊர் மாறுவது, இயக்க ஊர் highlight ஆவது, x அழிப்பது -
  இவை சாதனத்தில் இன்னும் சோதிக்கப்படவில்லை.
