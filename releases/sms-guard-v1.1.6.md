# SMS Guard 1.1.6 — Monthly spend card

**Version:** 1.1.6 · **version code 12** · **App:** SMS Guard (குறுஞ்செய்தி காவலன்)

Adds one card to the Finance tab: **this month's spend**, with last month beside it.

No new permissions. No behaviour change to bill reminders, OTP silence, spoof blocking, or the
Finance ledger's existing totals.

## தமிழ் சுருக்கம்

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

It does not guess. Every figure comes from `MonthlySpendPolicy`, which applies exactly the same
genuine-bank/UPI-and-not-a-self-transfer filter the ledger already uses, so the card and the ledger
can never disagree. A spoofed "₹500 debited" from a random personal number can never inflate a spend
total, and paying your own credit-card bill is still a transfer rather than spending.

The month is clamped to its real length — 28 in February, 30 in September — and a month that has
already finished is not projected past its own last day.

## Not verified

The exact bytes in this release were not installed on a phone before publication: no device test was
run for 1.1.6. CI unit, lint, regression, signature and release-asset checks pass. The card's layout
on a narrow screen and the numbers against a real month of messages still need a physical-device
check.
