# SMS Guard 1.1.8 - The monthly-spend card, in Tamil that reads as Tamil

**Version:** 1.1.8 · **version code 14** · **App:** SMS Guard (குறுஞ்செய்தி காவலன்)

1.1.7 put every rupee amount in the monthly-spend card on one line, and the owner's phone
confirmed it worked. The same screenshots then showed what 1.1.7 had broken beside those
amounts, and what 1.1.6 had been quietly showing in Tamil ever since the card was new. No new
permissions, and no behaviour change to anything else.

## What changed

**The "vs last month" line is no longer cut off.** 1.1.7 pinned the trend text to a single
non-wrapping line on the assumption that it was a number. When last month had no spend it is a
sentence, and on the owner's phone it ran off the edge of the card: it read "கடந்த மாதம் செலவு
இ" and nothing more. The amount now has its own place beside the label, and the sentence wraps
underneath in full.

**Five Tamil strings that were not Tamil now are.** The card read "6 மீதம் நாடகள்", where
நாடகள் means dramas rather than days. It read "உறியாக்க செலவுகள்" and "மாஸம் முடிசெய்யும்
பதவிடத்தில் (நிறுமணம்)", neither of which is a Tamil phrase. It read "பின்னும் மாஸத்தில்",
where பின்னும் means more rather than last. All five are plain Tamil now, written out and read
back.

**The income and spend counters are no longer in English.** On a Tamil screen the two summary
cards read "9 verified credits" and "11 verified debits". They are catalog fields now, like
every other string in the app.

**The two app subtitles were the wrong way round.** In Tamil mode the app bar named the app "SMS
Guard"; in English mode it named the app in Tamil. Both are the right way round now.

## What did not change

The figures. The month clamp, the projection rule, the genuine-bank/UPI-and-not-a-self-transfer
filter and OTP silence are exactly as they were in 1.1.6.

## Device test

These exact bytes were not installed on a phone before publication, and no device result is
claimed for them. What is claimed is narrower: the 1.1.7 build before them was installed on a
phone on 25 October 2026, and its screenshots are what found every defect this release repairs -
the amounts on one line, the clipped sentence, and the Tamil. Every CI unit, lint, regression,
signature and release-asset check passed.

## தமிழ்

- **"கடந்த மாதம்" வரி இனி வெட்டப்படுவதில்லை.** 1.1.7-ல் நிலை வாக்கியம் ஒரே வரியில் அமைக்கப்பட்டது, ஏனெனில் அது எண் என்று எண்ணப்பட்டது. கடந்த மாதம் செலவு இல்லாதபோது அது ஒரு வாக்கியம்; ஃபோனில் அது அட்டை விளிம்பைத் தாண்டி ஓடி, "கடந்த மாதம் செலவு இ" என்று மட்டும் தெரிந்தது. இப்போது எண் தனி வரியில் இருக்கிறது, வாக்கியம் அதன் கீழே முழுமையாக எழுகிறது.
- **தமிழ் அல்லாத ஐந்து சொற்கள் இப்போது தமிழாகும்.** அட்டை "6 மீதம் நாடகள்" என்று எழுதியது; நாட்கள் அல்ல, நாடகள் என்பது நாடகம். "உறியாக்க செலவுகள்" மற்றும் "மாஸம் முடிசெய்யும் பதவிடத்தில் (நிறுமணம்)" இரண்டும் தமிழ் வாக்கியமல்ல. "பின்னும் மாஸத்தில்" என்பதில் "பின்னும்" என்பது கடந்த மாதம் அல்ல, அதிகம். ஐந்தும் இப்போது சரியான தமிழ்.
- **வரவு மற்றும் செலவு எண்ணிக்கை இனி ஆங்கிலத்தில் இல்லை.** தமிழ் திரையில் "9 verified credits" மற்றும் "11 verified debits" என்று ஆங்கிலத்தில் எழுதியது. இப்போது இந்த இரண்டும் கேட்டலாகில் சேர்ந்துவிட்டன.
- **இரண்டு துணைத் தலைப்புகள் இடமாறின.** தமிழ் பயன்முறையில் செயலியின் பெயர் "SMS Guard" என்று, ஆங்கில பயன்முறையில் தமிழில் எழுதியது. இப்போது இரண்டும் சரியாக இருக்கிறது.
