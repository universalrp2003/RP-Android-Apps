# SMS Guard (குறுஞ்செய்தி காவலன்)

**Package:** `com.universalrp.smsguard` · Android 8.0+ · Development version **1.1.15 / code 21**. See the [public releases](https://github.com/universalrp2003/RP-Android-Apps/releases?q=sms-guard&expanded=true) and [verification receipt](../docs/sms-guard-1.1.15-verification.md) for actual publication/build status rather than treating this source version as an installed-phone test.

## Read before installing or sharing

**Play Protect's sensitive-data installation block remains unresolved.** The owner reports blocked older APKs and explicitly authorized publishing bug fixes for family/friends with this disclosure. The app still declares SMS read/receive permissions. Installation may remain blocked; keep Play Protect enabled and respect warnings. No Google clearance, malware-free guarantee or exact new-build phone test is claimed. Update same-key installations normally; do not uninstall/reset to force installation and risk local data loss.

[Full warning, privacy choices and audit access](../docs/sms-guard-play-protect.md) · [Privacy policy](../PRIVACY.md) · [1.1.15 notes](../releases/sms-guard-v1.1.15.md). Public source enables review, not a safety certification.

## Features and limitations

- Tamil/English SMS inbox companion, on-device bill/finance analysis, optional voice announcements, sleep schedule and manual/global Stop. Automatic outgoing-message echoes are suppressed; manual sent-message replay is explicitly user-started and privacy-screened.
- **Incoming MMS is incomplete**. New default-SMS setup is unavailable; keep a complete messaging app as default. This is not a full-featured replacement for all messaging.
- Local suspicious-link rules plus Internet-controlled HEAD-only redirect checks, system DNS and Cloudflare security DNS-over-HTTPS; optional Google Safe Browsing with the user's API key. URLs/hostnames/request metadata leave the phone for online checks. No threat detected is not a guarantee; failed/offline checks stay pending. Durable bounded background work resumes deferred checks; failed rescans invalidate old green results.
- **HD cloud speech is separately opt-in, OFF by default even on upgrades.** Enabling it sends spoken text—including sender names, ordinary SMS content and financial amounts—to Microsoft. Gender/language profiles can rotate daily. Internet ON alone does not permit cloud speech. Review/withdraw in Controls; withdrawal cannot recall text already sent.
- Without consent/connectivity, use only an installed system voice reported as non-network. If none exists, no speech—not an automatic network fallback. Availability, gender and provider behavior depend on installed engines. There is no zero-latency promise.
- Full-body and common final speech screening suppress detected OTP/PIN/password-bearing text and mask account/masked/other long numbers while preserving bounded labelled amounts. These are **heuristics, not perfect secret detection**; do not share messages you cannot share. Nearby people can hear speech. Optional sender-only private-message announcements contain no message body/code.
- Finance rejects ordinary-number payment claims and separates bank/card/UPI/FASTag records using sender/transaction heuristics, not bank verification or a “spoof-proof” guarantee. Questionable bills stay reviewable but are excluded from reminders. Biometrics/custom credential protect UI access; the complete ledger is not separately encrypted by this lock.
- Companion setup explains read/receive/notifications. Contacts and Phone/SIM labels are separate optional permission actions. Send permission is requested only on actual Send; a grant keeps the draft and requires another Send tap. Existing messages, accounts and records are preserved on same-key updates.

## Build and audit

```sh
./gradlew :smsguard:testReleaseUnitTest :smsguard:lintRelease :smsguard:assembleRelease
```

Deterministic regressions, native compilation/lint, signature/hash checks, optional external voice probes, phone feature tests and Google installation verdicts are separate evidence. See `SpeechPrivacyTest`, the queue/link/bill regressions, and the immutable release receipt. No Google/VirusTotal upload or appeal was performed for this work.

## தமிழ்

Google Play Protect நிறுவல் எச்சரிக்கை இன்னும் தீர்க்கப்படவில்லை. பாதுகாப்பை அணைக்கவோ எச்சரிக்கையை மீறவோ வேண்டாம். HD குரலுக்கு தனி ஒப்புதல் தேவை; அனுமதித்தால் வாசிக்கும் உரை Microsoft-க்கு அனுப்பப்படலாம். மறுத்தால் உள்ளூர் குரல் மட்டும்; அது இல்லையெனில் குரல் இல்லை. ரகசியம் கண்டறிதல் முழுமையானதல்ல. MMS பெறுதல் முழுமையாக இல்லை; முழுமையான செய்திச் செயலியை இயல்புநிலையாக வைத்திருங்கள். அதே கையொப்பப் பதிப்பை புதுப்பிக்கவும்; நிறுவுவதற்காக நீக்கி உள்ளூர் தரவை இழக்க வேண்டாம். மேலுள்ள இணைப்புகளில் மூலநிரலையும் வரம்புகளையும் ஆய்வு செய்யலாம்.

Historical 28 October owner feedback concerned 1.1.9/code15, not 1.1.15; the supplied later blocked-install screenshots supersede any assumption that an older install proves current installability. No screenshots are APK-hash-matched.

[தமிழ் / English help and other applications](../docs/three-app-help.md) · [Official website](https://universalrp2003.github.io/RP-Android-Apps/)
