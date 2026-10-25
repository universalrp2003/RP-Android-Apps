# SMS Guard 1.1.6 — Monthly spend card

**Version:** 1.1.6 · **version code 12** · **App:** SMS Guard (குறுஞ்செய்தி காவலன்)

Adds one card to the Finance tab: **this month's spend**, with last month beside it.

No new permissions. No behaviour change to bill reminders, OTP silence, spoof blocking, or the
Finance ledger's existing totals.

## What was added

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
