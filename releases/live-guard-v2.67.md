## Live Guard (CleanSweep) v2.67 — Readable News Headlines (HTML/XML Entity Decoding)

**Package:** `com.universalrp.cleansweep` · **Version:** `2.67` (`versionCode = 72`) · **Minimum Android:** Android 8.0+ (API 26)
**Published Signed Asset:** `CleanSweep-v2.67.apk` · **`14,330,071` bytes** · **SHA-256:** `ea8eedd2ddb13d5200347da2db580a45ee8fb628634b798183aaf5765650adf4` ([Build APK run 39990781045](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39990781045))
**Verification Note:** Built, unit-tested, and verified via CI static/manifest/signature/localization checks; this exact build was **not installed on a phone** prior to GitHub release publication (**adds zero new permissions** over `v2.66` — no dangerous-permission delta, so no device-install record is required). GitHub publication is not Play Protect approval or all-device certification.

### What's New in v2.67
- **Readable RSS / news headlines:** HTML and XML character references that survived into a headline — `&amp;`, `&#39;`, `&quot;`, `&rsquo;`, `&lsquo;`, `&ldquo;`/`&rdquo;`, `&mdash;`/`&ndash;`, `&hellip;`, `&nbsp;`, `&eacute;` and similar — are now decoded to their real characters in the News Strip and the spoken bulletin, in both **Tamil and English**. No more literal "R&amp;D" or "It&#39;s" in a headline.
  - Decoding is a **single pass**, so `&amp;lt;` correctly stays `&lt;` instead of collapsing to `<` (no over-decoding).
  - Entities are decoded **after** tag-stripping, so a decoded `<`/`>` is never re-treated as a tag — comparison headlines like "5 < 10" are preserved intact.
  - Non-breaking and other Unicode spaces collapse to a normal space; unknown or out-of-range references (e.g. surrogates, `> U+10FFFF`) are left untouched — fail-safe.
- **Zero new permissions** over `v2.66`; the package name, verified updater and signing key are unchanged. Adds **8 unit tests** covering named entities, numeric (decimal + hex), `&nbsp;`, unknown/out-of-range, single-pass ampersand sequences, preserved angle brackets, end-to-end RSS double-decode, and mixed references.
