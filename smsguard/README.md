# SMS Guard (குறுஞ்செய்தி காவலன்)

**Package:** `com.universalrp.smsguard` · **Current Stable Release:** [`v1.1.4` (`versionCode = 10`)](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/sms-guard-v1.1.4) (`12,379,633` bytes · SHA-256 `37b45394ce776c223b7f089bb1388c450829301ea21fe2b86ed18d29c616203d`) · **Minimum Android:** Android 8.0+ (API 26)

Full-featured open-source **Tamil (`தமிழ்`) & English** Android Default SMS app, cyber phishing shield, smart voice announcer (instant screen-ON auto-stop, global `⏹ Stop / நிறுத்து` button across every window, newest-messages-at-top thread/history sorting, and default-ON full readout for Government, `TNEPDS` Ration Shop, `TNSEDU` School Education Exam Results, Utilities, and non-OTP senders), and spoof-proof Bank / Card / UPI / FASTag finance tracker.

## Core Capabilities

1. **Green-Tick Safe vs Red Don't-Touch Link Shield (NextDNS Compatible):**
   - Inspects every link inside incoming SMS messages without uploading personal conversations.
   - Blocks raw `.apk`/`.xapk` malware links, brand-impersonation lookalikes, and scam WhatsApp/Telegram job traps with a full-screen protective warning.
   - Resolves domains cleanly via Android's native system/Private DNS (fully compatible with NextDNS, AdGuard, and router firewalls) plus Cloudflare `1.1.1.2` threat checks and an optional Keystore-encrypted Google Safe Browsing v4 API key (with built-in step-by-step setup guide).
   - **Honest Offline Queue:** Never guesses when internet is off — marks links Pending, warns not to tap, and automatically verifies upon reconnection.

2. **6 Studio Online Neural Voices (3 Male + 3 Female) & Automatic Daily Rotation:**
   - Speaks verified bank transactions, link warnings, and wake-up summaries using crisp studio-quality **Tamil (`Valluvar`, `Anbu`, `Surya` Male · `Pallavi`, `Venba`, `Saranya` Female)** and **English (`Prabhat`, `Guy`, `Ryan` Male · `Neerja`, `Aria`, `Sonia` Female)** neural voices.
   - Automatically alternates **Male one day → Female the next day** by default, playing **Online Neural HD first** and falling back automatically in `0 ms` to your phone's installed **Android System TTS** voice whenever offline.
   - **100% OTP Silence & Zero Account Digits:** Never reads OTP codes or bank account digits aloud; never speaks outgoing SMS sent by you.

3. **Sleep Time Quiet Schedule & Sequential Wake Readout:**
   - Stays completely quiet during user-selected sleep hours (12-hour `h:mm a` clock) and reads queued bank/money alerts clearly one by one when sleep hours end.

4. **Spoof-Proof Bank, Card, UPI & FASTag Finance Ledger + Biometric Lock:**
   - Rejects fake payment SMS messages sent from personal `+91` numbers (`₹0` added + spoof warning).
   - Separates Bank/UPI/Card spending from FASTag toll balance deductions with `O(N)` UTR deduplication across 50,000+ SMS inboxes, protected by Fingerprint / Screen Lock or custom PIN.

## Build & Verify

```sh
./gradlew :smsguard:testReleaseUnitTest :smsguard:lintRelease :smsguard:assembleRelease
```
