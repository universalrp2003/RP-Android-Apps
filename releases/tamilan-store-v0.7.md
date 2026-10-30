# Tamilan Store 0.7 — candidate, not published

Fixes the Store's stale legacy certificate pin. Current replacement-key APKs use certificate SHA-256 `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d`; Secret Message retains its separate established certificate. Hash, size, package/version, SDK, installed signer and downgrade checks still fail closed. Never accept both keys to bypass Android update compatibility.

Failures distinguish download interruption, size/checksum, archive metadata, APK signer and installed-signer incompatibility. A verified file explicitly tells the user to tap Install for Android confirmation. No silent installs or Play Protect bypass.

Update compatible Store installations normally. Do not uninstall or clear app data to address a signing mismatch: local data could be lost. A legacy-key Daily Vault 0.3 cannot be updated directly by a replacement-key APK without signing continuity. Preserve that installation and its records while investigating/exporting a backup; do not assume the installer’s initial Update button proves compatibility or successful installation.

Public Store 0.6, Daily Vault 0.5 and all immutable assets/website pointers remain unchanged. This is an unshipped candidate; CI and device validation must be recorded truthfully before any new publication.

## தமிழ்

தமிழன் அங்காடியின் பழைய கையொப்பச் சரிபார்ப்பு மதிப்பு திருத்தப்பட்டது. புதிய வெளியீடுகளின் கையொப்பம் மட்டும் ஏற்கப்படும்; Secret Message தனிக் கையொப்பத்தைத் தொடர்கிறது. கோப்பளவு, சரிபார்ப்பு மதிப்பு, செயலிப் பெயர், பதிப்பு மற்றும் கையொப்பச் சோதனைகள் நீக்கப்படவில்லை.

பதிவிறக்கத் தோல்வி, கோப்புச் சரிபார்ப்புத் தோல்வி, கையொப்ப முரண்பாடு தனித்தனியாக விளக்கப்படும். கோப்பு சரிபார்க்கப்பட்டதும் Android ஒப்புதலைத் திறக்க நிறுவு அழுத்த வேண்டும். தானாக நிறுவாது; பாதுகாப்பு எச்சரிக்கையை மீற வேண்டாம்.

பழைய கையொப்ப நிறுவலை வேறு கையொப்ப APK-யால் நேரடியாகப் புதுப்பிக்க முடியாது. செயலியை நீக்கவோ தரவை அழிக்கவோ வேண்டாம்; உள்ளூர் தரவு அழியலாம். பதிவுகளைப் பாதுகாத்து முதலில் காப்புப் பிரதியைப் பரிசோதிக்கவும். இது இன்னும் வெளியிடப்படாத சோதனைப் பதிப்பு.
