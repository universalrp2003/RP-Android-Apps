# Tamilan Store0.11 — explicit catalog refresh and honest saved-list status

Refresh catalog can explicitly retry the public feed. Metadata transport bypasses stale caches with a per-request nonce and no-cache headers, without device identifiers or extra automatic polling. Report success, unavailable online checks and rejected catalogs separately; retain the last good list on failure. Package identities, no rollback, same-version content immutability, APK hash/signer verification and explicit Android installation approval remain enforced.

The owner reported an old catalog despite a newer installed Oor app. This improves recovery and diagnosis; it does not claim the exact cause on that phone was established. Browser downloads stuck at100% are outside Store control. Completed system downloads are copied through Android DownloadManager’s provider-owned file descriptor rather than a separate external-storage path view; exact size, SHA-256, package, version and signer checks remain required before installation. System-owned background downloads, verification and installer handoff must pass their existing native gates before promotion.

Install over the same-key Store0.10; do not uninstall or clear local data. No new permissions, silent installs, warning bypass or Google/OEM/Doze guarantee. Final bytes are not installed on a phone independently.

## தமிழ்

# தமிழன்0.11 — பட்டியலை மீண்டும் பெறும் பொத்தான்

செயலிப் பட்டியலை மீண்டும் பெறு பொத்தான் மற்றும் பழைய cache-ஐத் தவிர்க்கும் metadata கோரிக்கை. வெற்றி / இணையச் சோதனை முடியவில்லை / பட்டியல் சரிபார்ப்பில் ஏற்கப்படவில்லை தனித்தனியாகத் தெரியும்; தோல்வியில் சேமித்த பட்டியல் காக்கப்படும். அடையாளம், பதிப்பு, ஹாஷ், கையொப்பம் மற்றும் Android நிறுவல் ஒப்புதல் சோதனைகள் தளர்த்தப்படவில்லை. புதிய அனுமதி அல்லது மறைமுக நிறுவல் இல்லை. அதே கையொப்பத்தில் புதுப்பிக்கவும்; செயலியை நீக்கவோ தரவை அழிக்கவோ வேண்டாம். Brave பதிவிறக்கத்தை அங்காடி கட்டுப்படுத்தாது; Google அல்லது அனைத்து சாதன உறுதி அல்ல.
