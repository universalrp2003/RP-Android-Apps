# SMS Guard 1.1.6 — Monthly spend card

**Version:** 1.1.6 · **version code 12** · **App:** SMS Guard (குறுஞ்செய்தி காவலன்)

Adds one card to the Finance tab: **this month's spend**, with last month beside it.

No new permissions. No behaviour change to bill reminders, OTP silence, spoof blocking, or the
Finance ledger's existing totals.

## How it is built

The Finance tab already totals whatever period the owner picks, and the yearly view already draws a
bar per month. What was missing is the number people open the tab for, so the new card shows:

- **This month's spend** and how many verified debits it is made of.
- **vs last month**, as a signed rupee change plus a percentage — or the words "Last month had no
  spend" when there is nothing to compare against. "Up 100% from ₹0" is arithmetic, not information,
  so it is never shown.
- **Daily average**, and **projected month-end**, labelled an estimate everywhere it appears: it is
  a rate multiplied by the days left in the month, not money that has moved.
- **Days left** in the month, so a partial month is never read as a finished one.

## What it deliberately does not do

It does not guess. Every figure comes from `MonthlySpendPolicy`, which applies the same
genuine-bank/UPI-and-not-a-self-transfer filter the ledger uses, so a spoofed "₹500 debited"
from a random personal number can never inflate a spend total, and paying your own credit-card
bill is still a transfer rather than spending.

One deliberate difference from the period summary below it: the card is always the whole
calendar month across every account, while the summary honours the account you have selected
and any message you have excluded. On the owner's test phone the card read ₹10,427 and the
October summary read ₹10,302 for exactly that reason, so the two are allowed to differ and
neither is wrong.

The month is clamped to its real length — 28 in February, 30 in September — and a month that has
already finished is not projected past its own last day.

## Device test

Installed on a phone on 25 October 2026. The app opens and the Finance tab reaches the new monthly
spend card. The owner confirmed the install after this note was first written, so the exact bytes
of 1.1.6 are phone-installed rather than untested.

CI unit, lint, regression, signature and release-asset checks all pass, and the month clamp, the
projection rule and the "last month had no spend" wording are unit-tested. Still to check on a
phone: the card's figures against a real month of messages.

The layout part of that check is now done, and it found one defect: on the owner's phone the
projected month-end figure wrapped inside its own digits, reading "₹12,92" and then "9.48" on the
next line, because the long Tamil label beside it left the amount no room. The figure itself was
right; this is a layout fault, not a numbers fault. Every amount in the card is now a single
non-wrapping line, and the label wraps instead. That fix is in the source for the next build, not
in the 1.1.6 bytes described here.

## தமிழ்

- **நிதித் (Finance) தாவலில் புதிய மாதாந்திர செலவு அட்டை:** இந்த மாதம் எத்தனை, கடந்த மாதம்
  எத்தனை, இரண்டுக்கும் இடையேயான மாற்றம் ரூபாயிலும் சதவீதத்திலும்.
- **நாள் வரியான சராசரி** (இதுவரை நடந்த நாட்களால் வகுத்தல்), **மாத இறுதியில் எதிர்பார்க்கும் செலவு**,
  **மீதமுள்ள நாட்கள்**, மற்றும் எளிய மொழியில் ஒரு **பிரவணம்** (கடந்த மாதத்தை விட அதிகம் /
  குறைவு / சமம்).
- **மாதம் கடந்த நாள் நள்ளிரவில் அல்ல, நாள் இறுதியில் முடுகிறது.** அதனால் கணிப்பு ஒரு நாள்
  குறையாக இருக்காது.
- **கடந்த மாதம் செலவு இல்லாவிட்டால் சதவீத மாற்றம் காட்டப்படாது** — அர்த்தமில்ல எண்ணைக் காட்டுவதை விட
  காட்டாதது நேர்மையானது.
- வடிகட்டல் உண்மையான செலவுகளை மட்டும் எண்ணும்: சொந்த கணக்குகளுக்கு இடையே நகர்வு அல்ல, பற்று
  (DEBIT) மட்டும். பில் SMS வரவு/செலவாக கணக்கிடப்படாது.
- **OTP ஒலிக்கப்படாதது உறுதி.** ஒரு முறை குறியீடுகள் எப்போதும் பில் நினைவூட்டலின் பகுதியாகவும்
  ஒலிக்கப்படுவதில்லை.
- **ஃபோனில் நிறுவப்பட்டது (25 அக்டோபர் 2026).** இந்த குறிப்பு எழுதப்பட்ட பிறகு 1.1.6-ன்
  துல்லியமான கோப்புகள் நிறுவப்பட்டதாக உறுதி செய்யப்பட்டன. செயலி திறந்து நிதித் தாவலில்
  புதிய மாதாந்திர செலவு அட்டை எட்டுகிறது.
- **இன்னும் சோதிக்க வேண்டியது:** ஒரு உண்மையான மாதத்தின் செய்திகளுக்கு அட்டையின் எண்கள்,
  மற்றும் குறுகிய திரையில் அட்டையின் தோற்றம். CI சோதனைகள் எல்லாம் வெற்றி; மாதம் கடந்த
  நள்ளிரவில் அல்லாமல் நாள் இறுதியில் முடுவது unit சோதனையில் சரிபார்க்கப்பட்டுள்ளது.
