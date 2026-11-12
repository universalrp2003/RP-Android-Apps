# SMS Guard 1.1.15 · versionCode 21 · 31 October 2026

Owner-authorized bug-fix release for family/friends despite the **unresolved Google Play Protect sensitive-data installation block**. Same package and established signing certificate; this is not a signing migration or bypass. Same-key users should update without uninstalling. Uninstalling may erase local data; do not reset/uninstall to force a blocked install.

## Warning before downloading

The owner supplied “App blocked to protect your device” / “This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud.” SMS Guard still needs `READ_SMS`/`RECEIVE_SMS` for its automatic inbox/announcement functionality. These declarations remain visible to installation-time checks. **1.1.15 may still be blocked. Keep Play Protect enabled and respect the warning.** Google does not universally block all SMS applications. No Google clearance, malware-free/false-positive determination or exact-build phone/natural-SMS verification is claimed. These exact new bytes are not installed on a phone as part of release validation. Source availability and trusted recipients are not safety guarantees.

Full disclosure, limitations and audit links: [SMS Guard Play Protect document](../docs/sms-guard-play-protect.md). [Privacy policy](../PRIVACY.md). [Open source](https://github.com/universalrp2003/RP-Android-Apps/tree/main/smsguard). Release tag and verification receipt pin the actual source and APK; automated checks are not a device security verdict.

## Fixes and privacy choices

- Durable background pending-link work, bounded retries, no receiver-held network scan; offline/unavailable stays pending rather than becoming green. Fresh failed rescans invalidate older green cache entries.
- HEAD-only redirects, local checks at each redirect hop and system DNS plus hostname-based Cloudflare security DNS-over-HTTPS. No raw-IP DNS bypass. “No threat detected” is not a guarantee.
- Excludes questionable bill captures from every reminder delivery path; records remain reviewable. Refuses new default-SMS role setup while incoming MMS is incomplete: retain your complete default messaging app.
- Separate Tamil/English **cloud speech consent defaults OFF on upgrades too**. Enabling HD voice shares spoken text with Microsoft, potentially including sender names, ordinary message content and financial amounts, including automatic background speech. Internet ON alone cannot enable it.
- Shared final speech screen plus full-body screening before shortening. Sent messages now receive OTP classification; detected secret-bearing speech is suppressed and account/masked/other long numbers are redacted. Detection remains heuristic and incomplete; explicit labelled amounts can remain.
- Declining consent uses an installed non-network system voice only; if unavailable, no speech rather than silently selecting a network voice. Controls can review/withdraw consent. Revocation/Internet OFF stop playback, block later cloud sends/retries, and revocation clears recent new-format audio cache. They cannot recall already transmitted text. Legacy audio cache is removed without deleting SMS or records.
- Initial companion permissions exclude sending/contacts/phone. Optional contact-name/SIM actions are separate; sending asks on actual Send, retains draft and requires another explicit Send after grant.

## Verification limits

The signed candidate must pass JVM regressions, release lint, manifest/package/code/nondebuggable/signature/hash checks and a dangerous-permission comparison before exact-byte publication. The publication receipt records outcomes rather than assuming success here. No Google/VirusTotal sample upload or appeal was submitted. Radio 1.31 and the held Oor travel-test candidate are unchanged.

## தமிழ்

உரிமையாளர் அனுமதித்த பிழைதிருத்த வெளியீடு. Google Play Protect முக்கியத் தகவல் அனுமதி எச்சரிக்கை இன்னும் தீர்க்கப்படவில்லை; நிறுவல் தடுக்கப்படலாம். பாதுகாப்பை அணைக்கவோ எச்சரிக்கையை மீறவோ வேண்டாம். Google ஒப்புதல் அல்லது புதிய APK தொலைபேசிச் சோதனை எனக் கூறவில்லை. மூலநிரலை யாரும் ஆய்வு செய்யலாம்; அது பாதுகாப்பு உத்தரவாதம் அல்ல.

HD குரலுக்கு தனி தமிழ்/ஆங்கில ஒப்புதல் சேர்க்கப்பட்டுள்ளது; புதுப்பிப்பிலும் முதலில் OFF. அனுமதித்தால் வாசிக்கும் உரை, அனுப்புநர் பெயர், தொகை Microsoft-க்கு அனுப்பப்படலாம். மறுத்தால் இணையமில்லா உள்ளூர் குரல் மட்டும்; அது இல்லையெனில் குரல் இல்லை. Controls பகுதியில் ஒப்புதலைத் திரும்பப் பெறலாம்; ஏற்கெனவே அனுப்பிய உரையை மீட்க முடியாது. ரகசியம் கண்டறிதல்/எண் மறைத்தல் முழுமையானதல்ல.

அனுப்பிய செய்திகளைச் சுருக்கும் முன் முழுவதும் பரிசோதிக்கிறது. பின்னணி இணைப்புச் சோதனை, தவறான பில் நினைவூட்டல் தடுப்பு மற்றும் தெளிவான Pending நிலை திருத்தப்பட்டுள்ளன. MMS பெறுதல் இன்னும் முழுமையில்லை; முழுமையான செய்திச் செயலியை இயல்புநிலையாக வைத்திருங்கள். அதே கையொப்ப நிறுவலைப் புதுப்பிக்கவும்; நிறுவலுக்காக நீக்க வேண்டாம் — உள்ளூர் தரவு அழியலாம். Radio மற்றும் பயணச் சோதனைக்கான Oor மாற்றப்படவில்லை.
