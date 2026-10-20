# Live Guard 2.51

The pack now ends when **your operator** says it ends, and a booster can no longer inflate your
balance after it has expired.

## Highlights

### The pack ends at the operator's clock

Indian operators do not agree on when validity ends, so the app now asks and follows the answer:

| Operator | When the pack really ends |
|---|---|
| **Jio** | Counts from the recharge **moment**: a 63-day pack recharged at 5:00 pm ends at 5:00 pm on the last day; a "1 day" data pack is 24 hours, not "until midnight". |
| **Airtel** | **Midnight** — Airtel's terms say every validity expires at midnight, so a 1-day pack bought at 11:50 pm lasts nine minutes. |
| **Vi / BSNL** | Same as Airtel. |

- **New — Recharged at.** One button, type `5:00`, choose am/pm. Times are shown in twelve-hour
  form everywhere.
- **New — Operator chips** (Jio / Airtel / Vi / BSNL) set the rule automatically, and the rule can
  be overridden: *Same time as recharge* or *End of the last day*.
- **New — Valid till.** The field every operator app prints ("Valid Till 10 Dec 2026"). Type that
  date and the day count is **derived** for you, so a wrong day count cannot move the ending.
  Airtel's ₹489 pack is 77 days from 25 Sep — the app now fills that in from `2026-12-10`.
- The summary reads exactly: **"Ends 2026-12-10, 11:59 pm · 58 days left"**, and the countdown
  works to the minute, so a pack that dies at 5:00 pm today says so that afternoon.

### The booster is a second pool — the "1.05 GB left, but Airtel says 4.3 GB" fix

The owner's own numbers on 20 October 2026:

| | Live Guard (before) | Airtel's app |
|---|---|---|
| Measured | 4.95 GB used of 6 GB → **1.05 GB left** | **4.3 GB left** (1.7 GB used) |
| Booster | 3 GB, 17–19 Oct, expired | already spent |

Both were right, and they were counting **different pools**. Android measures every byte the radio
carried: 1.70 GB against the pack **plus** 3.25 GB during the booster's three days = 4.95 GB.
Airtel's app shows only the base pack once the booster is over.

- **Fixed — bytes measured during the booster's days are charged to the booster, not the pack.**
  With the owner's numbers Live Guard now reads **1.70 GB used · 4.3 GB left of 6 GB**, the same as
  the carrier, and says so: *"3.25 GB of the measured data was used during the booster's days, so
  it is not counted against this pack."*
- While a booster is still running, its bytes stay inside the combined allowance, because that is
  what the operator shows then too.
- **Fixed — one rounding rule.** My plan said "1.1 GB left" while the notification said
  "1.05 GB" for the same pack. Both now use the same two-decimal formatter.

### Publishing behaves itself

Only the apps whose version actually changed are published. A Live Guard fix no longer pushes a
pointless "update available" onto PulseEQ and Vocal Remover.

## Installation and safety

Download `CleanSweep-v2.51.apk` and install it through Android. If Android asks, enable **Allow from
this source** for the browser or file manager you used, then confirm in Android's installer. No
silent installation and no Play Protect bypass is used. The update installs over 2.50 with the same
signing key, so the plan, settings and language are kept.

No new Android permission is added: Location was introduced in 2.50 and this release asks for
nothing else.

**Device testing:** this build was **not installed on a phone** at the time of writing, and no
physical-device test is claimed for these exact bytes. The release passed the configured CI build,
the unit and regression suites (including the new plan-arithmetic tests), the APK checks and the
permission-delta gate. That is not Play Protect approval and not an all-device compatibility claim.

## தமிழ்

# லைவ் கார்டு 2.51

பேக் எப்போது முடிகிறது என்பதை இப்போது **உங்கள் ஆபரேட்டரே** தீர்மானிக்கிறார்; காலாவதியான பூஸ்டர்
இனி உங்கள் இருப்பை ஊதிப் பெருக்காது.

- **புதியது — ரீசார்ஜ் செய்த நேரம்.** ஒரு பொத்தான்; `5:00` என்று தட்டச்சு செய்து am/pm தேர்ந்தெடுக்கவும்.
- **புதியது — ஆபரேட்டர்** (Jio / Airtel / Vi / BSNL). விதி தானாக அமையும்; தேவைப்பட்டால் நீங்கள் மாற்றலாம்:
  *ரீசார்ஜ் நேரத்திலேயே* அல்லது *கடைசி நாள் முடிவில்*.
- **புதியது — இது வரை செல்லும்.** ஆபரேட்டர் ஆப் காட்டும் தேதியை (உ.ம். 2026-12-10) உள்ளிடுங்கள்;
  நாட்கள் தானாகக் கணக்கிடப்படும். ஏர்டெல் ₹489 = 25 செப் முதல் 77 நாட்கள்.
- சுருக்கம்: **"முடிவு 2026-12-10, 11:59 pm · 58 நாட்கள் மீதம்"** — நிமிடம் வரை சரியாக.

- **சரிசெய்யப்பட்டது — பூஸ்டர் தனிக் கணக்கு.** 20 அக் 2026 அன்று லைவ் கார்டு "6 GB-இல் 1.05 GB மீதம்"
  என்றது, ஏர்டெல் ஆப் "4.3 GB மீதம்" என்றது. இரண்டும் சரி: Android 4.95 GB அளந்தது — அதில் 3.25 GB
  பூஸ்டரின் மூன்று நாட்களில் சென்றது. இப்போது லைவ் கார்டு **1.70 GB பயன்பாடு · 4.3 GB மீதம்** எனக்
  காட்டி, பூஸ்டரில் சென்ற அளவையும் தனியாக விளக்குகிறது.
- **சரிசெய்யப்பட்டது — ஒரே வட்டமிடல்.** "1.1 GB" versus "1.05 GB" வேறுபாடு இனி இல்லை.
- **வெளியீடு:** மாறிய செயலிகள் மட்டுமே பதிப்பிடப்படும்; மற்ற செயலிகளுக்கு தேவையற்ற புதுப்பிப்பு வராது.

`CleanSweep-v2.51.apk`-ஐ Android வழியாக நிறுவவும். கேட்டால் **இந்த மூலத்திலிருந்து நிறுவு** அனுமதியை
இயக்கி உறுதிப்படுத்தவும். புதிய Android அனுமதி இல்லை (Location 2.50-இல் வந்தது).
