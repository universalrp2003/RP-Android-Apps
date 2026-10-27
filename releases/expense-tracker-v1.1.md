# Expense Tracker 1.1

## What this release is

1.0 was installed on the owner's phone and showed a month of zeroes. The owner read that correctly: the app was not seeing the SMS already in the inbox. This release is the repair, and it is worth being precise about why, because the cause was not a permission and not a signing problem.

## What was wrong

- **The inbox was never scanned on launch.** 1.0 scanned only from a content observer, which fires when a *new* message arrives. On a phone whose inbox already held months of bank SMS nothing ever fired, so the ledger stayed empty for as long as the app was open. A first scan now runs when the screen opens, and again the moment the permission is granted.
- **The sender engine was called without the message body.** `SenderIdentityEngine.identify` uses the body to tell a promotional header from a transactional one, and SMS Guard's own repository has always passed it. Without it a real bank alert can be classified as something that is not financial.
- **The empty state could not be read.** "No transactions recorded for this month" looks identical whether the app read two thousand messages and found nothing, or never opened the inbox at all. The owner had no way to tell those apart and reasonably assumed the worse one. The screen now says how many messages it checked, and says plainly when it has not checked any yet.

## What is unchanged

- No new permissions. Still `READ_SMS` and `RECEIVE_SMS`, and still **no `INTERNET` permission at all** — the ledger cannot leave the phone, and that is the app's whole reason for existing separately.
- The anti-spoof rules, the categoriser, the month totals and the ledger format are untouched, so 1.0's stored ledger is read by 1.1 without a migration.

## Device test

The owner installed the published 1.0 APK and returned screenshots: the app opens, both languages render, the month card and the empty state draw correctly, and the file explorer's checksum dialog confirms the installed bytes are exactly the published `6b7f1248…` asset. That install is what found the defect above.

The exact 1.1 bytes have **not** been installed on a phone. What is unit-tested is the categoriser, the month totals, the ledger round trip and the message-identity rule. What is not verified is 1.1 reading a real inbox — which is the one thing this release exists to fix, so it needs the owner's phone before it can be called working.

## தமிழ்

1.0-ஐ உரிமையாளரின் தொலைபேசியில் நிறுவியபோது மாதம் முழுவதும் பூஜ்யமாகக் காட்டியது. உரிமையாளர் சரியாகவே புரிந்துகொண்டார்: பழைய SMS-களை இச் செயலி பார்க்கவில்லை. காரணம் அனுமதியும் இல்லை, கையொப்பமும் இல்லை — செயலி தொடங்கும்போதே SMS-ஐப் படிக்கவே இல்லை. புதிய செய்தி வரும்போது மட்டுமே படித்தது, எனவே பழைய செய்திகளுக்கு எதுவும் நடக்கவில்லை. இப்போது திரை திறக்கும்போதே படிக்கும், மற்றல் அனுமதி கொடுக்கப்பட்ட உடனேயே படிக்கும். அனுமதிகள் அதேபோலத்தான் — SMS மட்டும், இணையம் இல்லை. எத்தனை செய்திகள் படிக்கப்பட்டது என்று இப்போது திரையிலேயே காட்டப்படும்.
