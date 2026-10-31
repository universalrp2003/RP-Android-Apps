# SMS Guard 1.1.13 — cautious link verification (unpublished candidate)

- Failed HTTP/redirect checks and an unavailable configured Safe Browsing check remain unverified, never green. A completed check means “no threat detected”, not guaranteed safe, in Tamil/English UI and speech.
- Link probes use HEAD only; no Range GET, response-body read or GET fallback. Sites rejecting HEAD remain unverified. Every redirect is locally screened before visiting it; exhausted/unsupported redirects are incomplete.
- Remove raw-IP DNS-filter avoidance. Use the normal security-provider hostname and return unverified when threat intelligence is unavailable, malformed or cooling down. Users on filtering networks may see more pending checks, not a fabricated clean result.
- Online probes still disclose the target URL (which may contain personal tokens), IP/request metadata to its server and hostnames to DNS/security providers; optional Google checks send the URL to Google. HEAD is not a privacy guarantee and can still trigger poorly implemented endpoints. No complete SMS-body upload is added.
- Remove blanket restricted-settings unlock instructions; disclose the MMS limitation in setup/Controls and keep native App Info for permission inspection only.
- Existing false-bill review/alerts, saved settings and records retained. Compatible owner-key **update** intended; do not uninstall or clear data. New candidate/code19, not a replacement asset for1.1.12 or public1.1.11. No new permissions or signing identity.
- **Play Protect “Harmful app blocked” on the owner's1.1.12 remains unresolved. These source changes are not evidence of Google approval, a false positive or a cleared warning. Do not bypass protection or use this candidate as the default SMS handler for acceptance yet.** Incoming MMS receiver is currently a placeholder; complete MMS delivery and broadcast network-time limits remain open audit findings.

These exact new bytes are **not installed on a phone**. Uninstalling may erase local data; preserve the existing installation and backups.

## தமிழ்

இணையச் சோதனை தோல்வியடைந்தால் இணைப்பு பாதுகாப்பானது என்று காட்டாது. முடிந்த சோதனைகளில் அச்சுறுத்தல் தெரியவில்லை என்பது பாதுகாப்பு உத்தரவாதம் அல்ல. இணைப்பின் உள்ளடக்கத்தைப் பதிவிறக்கும் GET முறைக்குப் பதிலாக HEAD மட்டும் பயன்படுத்தப்படும்; முடியாத சோதனை காத்திருப்பில் இருக்கும். DNS வடிகட்டலைத் தவிர்க்க நேரடி IP முகவரி பயன்படுத்தப்படாது. இணையச் சோதனையில் இணைப்பு, IP மற்றும் வழக்கமான கோரிக்கை விவரங்கள் வெளியே செல்லலாம். இது வெளியிடப்படாத சோதனைப் பதிப்பு. பழைய தரவை அழிக்கவோ செயலியை நீக்கவோ வேண்டாம். Google பாதுகாப்பு எச்சரிக்கை இன்னும் தீர்க்கப்படவில்லை; அதைப் புறக்கணித்து நிறுவ வேண்டாம். MMS பெறுதல் இன்னும் முழுமையாகச் செயல்படுத்தப்படவில்லை.
