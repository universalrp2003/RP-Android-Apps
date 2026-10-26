# Expense Tracker — செலவு கண்காணிப்பு / Tamil expense tracker

**Current stable release:** 1.0 / code 1

**Package:** `com.universalrp.expensetracker`

**Minimum Android:** 8.0 / API 26

Expense Tracker reads the owner's bank and UPI SMS and shows the month as categories. It answers the question SMS Guard deliberately does not: not "how much left my account" but "what did I spend it on". It is built on the anti-spoof sender verification and transaction parsing that SMS Guard already proves, now shared from `shared/` so both apps run identical code.

## What it does

- Reads bank and UPI SMS, keeps only what survived the anti-spoof parser, and shows the month as categories with each one's share of the total.
- **No internet permission at all.** There is no `android.permission.INTERNET` in the manifest, so the ledger cannot leave the phone even by accident, and Android's own permission screen says "No internet access". That is also why there is no voice here: the HD neural voice is a network call and this app makes no network calls.
- The parser refuses a personal mobile number claiming "Rs.5000 credited", a promotional loan offer, an OTP-shaped message, and an outgoing NEFT settlement confirmation. Refusals are counted and shown, so you can see how much was thrown away rather than trusting a tracker that silently drops rows.
- Money is formatted the way a bank statement reads it: Indian digit grouping, `₹1,23,456`, paise rounded once at the boundary.
- Month-over-month navigation, income/spend/net totals, and a delete for a row you disagree with.

## Permissions

`READ_SMS` and `RECEIVE_SMS`. **No internet, no location, no contacts, no storage, no camera, no phone state, no account, no advertising id.**

## Download the stable release

- [Official Website Hub](https://universalrp2003.github.io/RP-Android-Apps/) · [Download Expense-Tracker-v1.0.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/expense-tracker-v1.0/Expense-Tracker-v1.0.apk) · [release notes](../releases/expense-tracker-v1.0.md) · [all 9 RP Ideas apps](../README.md)

> The APK is signed with the same CI keystore as the other RP Ideas apps, so it installs without an "unknown developer" warning. The exact byte size and SHA-256 are recorded in [../README.md](../README.md) once the published asset has been read back, because a release build is not byte-reproducible and the candidate digest is not the published one.

## Device test status

No build of this app has been installed on a phone. The categoriser, the month totals and the ledger's encode/decode round trip are covered by unit tests. Reading a real inbox, the live SMS receiver, the permission flow and the Tamil wording on a real screen are not verified.

## Build and test

```
./gradlew :expensetracker:testDebugUnitTest :expensetracker:assembleRelease
```
