# Live Guard 3.00 — simpler My plan, honest data counts

**The expired-booster bug is fixed.** A booster that had already ended kept inflating the data
limit, so the floating card could disagree with My plan by gigabytes. Reproduced on the owner's
own phone: My plan said **2.24 GB left** while the card said **5.24 GB**. The cause was that a
stored limit written while the booster was running was never corrected after it lapsed. An ended
booster now adds exactly zero, and both surfaces read the same number.

**My plan is much simpler.** Set one amount and choose what resets it:

- **Daily reset** — 1, 1.5, 2, 2.5 or 3 GB, or type your own. Resets itself at midnight.
- **Total pack** — one amount, or unlimited, that only resets when *you* reset it.
- **Booster** — you enter the amount and the last day yourself. After that day it counts nothing.

Start date, day count, end date and the operator's end-of-day rule are gone. If you entered those
on 2.99 they are simply ignored; your numbers and packs are not touched.

**Both "count from now" buttons now live in one place**, and each explains what it will do before
you press it:

- *Start fresh from 0* — re-bases today and the pack together.
- *Keep today's usage* — re-bases the pack window only.

They used to promise a difference they did not deliver; now they really differ.

**The card and the widget agree with the screen.** The home-screen widget no longer prints
"3 days left" from the removed date fields — it describes the plan the same way My plan does.

**No alarm colour on the readings.** The pill and the plan screen show counts only. 4G and 5G are
listed separately and are never added together.

**The SIM-signal option has moved to Settings**, where it also says plainly that the percentage is
a rough estimate of signal strength, not reception reliability.

**To update:** Update over **2.99** without uninstalling and without clearing app storage — local data is kept:
your saved plan, history and settings are preserved. Uninstalling may erase your data instead, so
update in place. **No new permissions.**

This candidate is release-signed with the same key as 2.99 and is proven to install over the
exact published 2.99 APK before anything is published. **Not installed on a phone:** the upgrade
and native checks run on a disposable Android 15 emulator, so no physical-device test backs
this build.

## தமிழ்

**காலாவதியான பூஸ்டர் பிழை சரிசெய்யப்பட்டது.** முடிந்துவிட்ட பூஸ்டர் தொடர்ந்து தரவு வரம்பை
உயர்த்திக்கொண்டே இருந்தது, அதனால் மிதக்கும் அட்டையும் "எனது திட்டம்" திரையும் வெவ்வேறு
எண்களைக் காட்டின. உரிமையாளரின் ஃபோனிலேயே இது உறுதி செய்யப்பட்டது: எனது திட்டம் **2.24 GB
மீதம்** என்றது, அட்டை **5.24 GB** என்றது. இனி முடிந்த பூஸ்டர் எதையும் கூட்டாது; இரண்டும் ஒரே
எண்ணைக் காட்டும்.

**எனது திட்டம் மிகவும் எளிமையாகிவிட்டது.** ஒரு அளவை மட்டும் அமைத்து, எப்போது புதுப்பிக்க வேண்டும்
என்பதைத் தேர்ந்தெடுக்கவும்:

- **தினசரி புதுப்பிப்பு** — 1, 1.5, 2, 2.5 அல்லது 3 GB, அல்லது உங்கள் சொந்த அளவு. நள்ளிரவில்
  தானாகவே புதுப்பிக்கும்.
- **மொத்த பேக்** — ஒரு அளவு அல்லது வரம்பற்றது; நீங்கள் reset செய்யும் வரை மட்டுமே இருக்கும்.
- **பூஸ்டர்** — அளவையும் கடைசி நாளையும் நீங்களே உள்ளிடுவீர்கள். அந்த நாள் முடிந்தபின் எதுவும்
  கணக்கில் சேராது.

தொடக்கத் தேதி, நாட்கள் எண்ணிக்கை, முடிவுத் தேதி, ஆபரேட்டர் விதி ஆகியவை நீக்கப்பட்டன. 2.99 இல்
அவற்றை உள்ளிட்டிருந்தால் அவை கணக்கில் எடுக்கப்படாது; உங்கள் எண்களும் பேக்குகளும் மாறாது.

**"இப்போதிலிருந்து எண்ணு" இரண்டு பொத்தான்களும் இப்போது ஒரே இடத்தில் உள்ளன**, ஒவ்வொன்றும்
அழுத்துவதற்கு முன்பே தான் என்ன செய்யும் என்பதை விளக்குகிறது:

- *பூஜ்ஜியத்திலிருந்து புதிதாக* — இன்றைய பயன்பாடு மற்றும் பேக் இரண்டையும் புதிதாக அடிப்படையாக்கும்.
- *இன்றைய பயன்பாட்டை வைத்துக்கொள்* — பேக் கால அளவை மட்டும் புதுப்பிக்கும்.

**அட்டையும் விட்ஜெட்டும் திரையுடன் ஒத்துப்போகின்றன.** முகப்பு விட்ஜெட் இனி "3 நாட்கள் மீதம்"
என்று பழைய தேதிக் கணக்கைக் காட்டாது; எனது திட்டம் காட்டுவது போலவே விவரிக்கும்.

**எண்களுக்கு எச்சரிக்கை நிறம் இல்லை.** மிதக்கும் குறியும் திட்டத் திரையும் எண்ணிக்கைகளை மட்டுமே
காட்டும். 4G, 5G தனித்தனியாகக் காட்டப்படும், ஒருபோதும் கூட்டப்படாது.

**சிம் சிக்னல் விருப்பம் இப்போது Settings இல் உள்ளது**; அந்தச் சதவீதம் சிக்னல் வலிமையின்
தோராயமான அளவீடு தான், சிக்னல் நம்பகத்தன்மை அல்ல என்றும் அங்கே தெளிவாகக் கூறப்பட்டுள்ளது.

**புதுப்பிப்பு:** **2.99** மேல் செயலியை நீக்காமல், சேமிப்பை அழிக்காமல் புதுப்பிக்கவும் — உள்ளூர் தரவு பாதுகாக்கப்படும்:
உங்கள் திட்டம், வரலாறு, அமைப்புகள் அப்படியே இருக்கும். செயலியை நீக்கினால் தரவு அழிய வாய்ப்புள்ளது,
எனவே நேரடியாகப் புதுப்பிக்கவும். **புதிய அனுமதிகள் இல்லை.**

இந்த வேட்பாளர் 2.99 போன்ற அதே விசையால் கையொப்பமிடப்பட்டது; வெளியிடும் முன் வெளியிடப்பட்ட
2.99 APK மேல் நிறுவப்படுவது சரிபார்க்கப்படும். **உண்மையான ஃபோனில் நிறுவப்படவில்லை:** புதுப்பிப்பு
மற்றும் native சோதனைகள் ஒருமுறை பயன்படுத்தும் Android 15 எமுலேட்டரில் மட்டுமே இயக்கப்படுகின்றன.
