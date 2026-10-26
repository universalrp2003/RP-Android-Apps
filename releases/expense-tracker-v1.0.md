# Expense Tracker 1.0

## What this app is

A bank and UPI expense tracker that reads your SMS and shows the month as categories. It answers the
question SMS Guard deliberately does not: not "how much left my account" but "what did I spend it
on". A month that reads ₹18,400 across 31 transactions is a number; the same month read as ₹6,100
food, ₹4,800 fuel, ₹3,200 bills is something you can act on.

It is built on the anti-spoof sender verification and transaction parser that SMS Guard already
proves, moved into `shared/` so both apps run the identical code.

## What changed

- New app: read bank and UPI SMS, keep only what survived the anti-spoof parser, show the month as
  categories with each one's share of the total.
- **No internet permission at all.** There is no `android.permission.INTERNET` in the manifest, so
  the ledger cannot leave the phone even by accident, and Android's own permission screen says "No
  internet access" the moment you look. That is also why there is no voice here: the HD neural voice
  is a network call and this app makes no network calls.
- Permissions: `READ_SMS` and `RECEIVE_SMS`. No location, no contacts, no storage, no camera, no
  phone state, no account, no analytics, no advertising id.
- The parser refuses a personal mobile number claiming "Rs.5000 credited", a promotional loan offer,
  an OTP-shaped message, and an outgoing NEFT settlement confirmation. Each refusal is **counted and
  shown** at the bottom of the screen, so you can see how much it threw away rather than trusting a
  tracker that silently drops rows.
- Money is formatted the way a bank statement reads it: Indian digit grouping, `₹1,23,456`, and
  paise rounded once at the boundary so a month total never disagrees with the sum of its own rows
  by a paisa.

## Why the parser moved rather than being copied

SMS Guard's ledger already parses amounts, detects spoofs, separates FASTag tolls and recognises
self-transfers. Copying that into a second app would have created two parsers that drift apart, and
the one that drifts is the one that starts counting a spoofed credit as income. The three engine
files are now in `shared/src/main/java`, which SMS Guard already compiled from, so SMS Guard's
behaviour is unchanged and this app gets the same tested code.

## Device test

Not phone-installed for this release. This is a new app and no build of it has been installed on a
phone. The categoriser, the month totals and the ledger's encode/decode round trip are covered by
unit tests. Reading a real inbox, the live SMS receiver, the permission flow and the Tamil wording
on a real screen are not verified.

## தமிழ்

உங்கள் வங்கி & UPI SMS-ஐப் படித்து, அந்த மாதத்தின் செலவை வகைகளாகக் காட்டும் செயலி. SMS காவலன் எவ்வளவு பணம் வெளியேறியது என்று காட்டுகிறது; இச் செயலி அது எதற்கு என்று காட்டுகிறது. இணைய அனுமதி இல்லாதது இதன் முக்கிய சிறப்பு: பணம் தொலைபேசியை விட்டு வெளியேற முடியாது, ஆண்ட்ராய்டின் அனுமதித் திரையிலேயே "இணையம் இல்லை" என்று காட்டப்படும். எனவே இங்கே குரல் இல்லை. அனுமதிகள் SMS படிக்க மட்டும். தனி எண்களிலிருந்து வரும் "Rs.5000 credited" செய்திகள், கடன் விளம்பரங்கள், OTP செய்திகள் எல்லாம் நிராகரிக்கப்படும், மற்றல் எத்தனை நிராகரிக்கப்பட்டது என்று கீழே காட்டப்படும்.
