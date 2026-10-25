# SMS Guard 1.1.7 — The monthly-spend card, without the wrap

**Version:** 1.1.7 · **version code 13** · **App:** SMS Guard (குறுஞ்செய்தி காவலன்)

One fix and one correction to the monthly-spend card added in 1.1.6. No new permissions, no
behaviour change to anything else.

## What changed

**The projected month-end figure no longer wraps inside its own digits.** On the owner's phone the
1.1.6 card read "₹12,92" and then "9.48" on the next line, because the long Tamil label beside it
left the amount no room. The number was right; the line break was in the wrong place. Every amount
in the card is now a single non-wrapping line, and the label wraps instead.

**The card and the period summary are no longer claimed to agree.** The 1.1.6 note said the card and
the ledger "can never disagree", and on the owner's phone they did: the card read ₹10,427 while the
October summary read ₹10,302. The reason is that the card is always the whole calendar month across
every account, while the summary honours the account you have selected and any message you have
excluded. Both are correct; they answer different questions. The code comment and the note now say
so, instead of claiming an equality that does not hold.

## What did not change

The figures themselves. The month clamp, the projection rule, the "last month had no spend"
wording, the genuine-bank/UPI-and-not-a-self-transfer filter and OTP silence are all exactly as they
were in 1.1.6, and the card's unit tests are unchanged. This release changes how the card is drawn
and what the note claims, not what it counts.

## Not verified

The exact bytes of this release were not installed on a phone before publication: no device test was
run for 1.1.7. CI unit, lint, regression, signature and release-asset checks pass. What still needs
a physical-device check is precisely what this release is for — that the projected figure now sits
on one line on a narrow screen in Tamil, and that the card and the summary still read sensibly
against a real month of messages.

## தமிழ்

- **மாதாந்திர செலவு அட்டையில் எண்கள் இனி ஒரே வரியில் தெரியும்.** 1.1.6-ல் "மாதம் முடியும்
  பதிவத்தில்" எண் "₹12,92" என்று பிறகு "9.48" என்று இரண்டு வரிகளாகப் பிரிந்து தெரிந்தது —
  தமிழ் விளக்கச் சொல் நீளமாக இருந்ததால் எண்ணுக்கு இடம் மிச்சமில்லை. எண் தவறல்ல, வரி
  பிரிந்தது தவறு. இப்போது ஒவ்வொரு எண்ணும் ஒரே வரியில், விளக்கச் சொல்லே வரி மாறி எழுகிறது.
- **அட்டையும் காலாண்டு சுருக்கமும் ஒன்றாக இருக்கும் என்ற உரை நீக்கப்பட்டது.** 1.1.6 குறிப்பில்
  "இரண்டு ஒருபோதும் மாறற்கூடியதல்ல" என்று எழுதப்பட்டது; ஆனால் ஃபோனில் அட்டை ₹10,427, அக்டோபர்
  சுருக்கம் ₹10,302 என்று தெரிந்தது. காரணம்: அட்டை எப்போதும் முழு மாதம், எல்லா கணக்குகளும் —
  சுருக்கம் நீங்கள் தேர்வு செய்த கணக்கு மற்றும் நீக்கிய செய்திகளை மட்டுமே காட்டுகிறது. இரண்டும்
  சரியே; வேறு வேறு கேள்விக்கு பதில்.
- **எண்கள் மாறவில்லை.** மாத எல்லை, கணிப்பு விதி, "கடந்த மாதம் செலவு இல்லை" வாசகம், வடிகட்டல்,
  OTP ஒலிக்கப்படாதது — எல்லாம் 1.1.6-லேயே அதே. இந்த வெளியீடு எண்ணுவதை மட்டும் அல்ல, தோற்றத்தை
  சரிசெய்கிறது.
- **இந்த வெளியீட்டின் துல்லியமான கோப்புகள் வெளியிடுவதற்கு முன் ஃபோனில் நிறுவப்படவில்லை:**
  1.1.7-க்கு சாதன சோதனை இல்லை. CI சோதனைகள் வெற்றி. இந்த வெளியீட்டின் நோக்கமே அது — குறுகிய
  திரையில் எண் ஒரே வரியில் இருக்கிறதா, என்பதை ஃபோனில் சரிபார்க்க வேண்டும்.
