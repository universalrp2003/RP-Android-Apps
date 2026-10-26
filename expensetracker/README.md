# Expense Tracker — செலவு கண்காணிப்பு / Tamil expense tracker

**Current stable release:** 1.1 / code 2

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

- [Official Website Hub](https://universalrp2003.github.io/RP-Android-Apps/) · [Download Expense-Tracker-v1.1.apk from RP-Android-Apps](https://github.com/universalrp2003/RP-Android-Apps/releases/download/expense-tracker-v1.1/Expense-Tracker-v1.1.apk) · [release notes](../releases/expense-tracker-v1.1.md) · [all 9 RP Ideas apps](../README.md)

> The APK is signed with the same CI keystore as the other RP Ideas apps, so it installs without an "unknown developer" warning. The published asset for 1.0 was 11,547,831 bytes, SHA-256 6b7f1248eb22c59aba4a12aa43d213a7133f98afe1ed248c2242b03e0e7025d7. The 1.1 asset is 11,564,215 bytes, SHA-256 dd39091be62fba2d725300ced0132ae04a4da5ff528ca0be73638845272ee3f9.

## Device test status

1.0 was installed on the owner's phone and showed a month of zeroes, because it only scanned the inbox when a **new** message arrived — a phone whose inbox already held months of bank SMS never triggered a scan. 1.1 scans when the screen opens and when the permission is granted, and reports how many messages it checked.

The exact 1.1 bytes have **not** been installed on a phone, and reading a real inbox is the one thing 1.1 exists to fix, so it needs the owner's phone before it can be called working. The categoriser, the month totals, the ledger round trip and the message-identity rule are unit-tested.

## Build and test

```
./gradlew :expensetracker:testDebugUnitTest :expensetracker:assembleRelease
```
