# தமிழன் · Tamilan Store 0.14

Adds **வார வசூல் · Weekly Money TEST preview** as a clearly labelled test entry. It is not described as a production-ready financial ledger. The existing fifteen identities remain in the catalog; package, version, size, SHA-256 and pinned signer are still verified before offering Android's installer. Installation remains user-confirmed.

The new Store uses `catalog-v3.json` (sixteen entries), with a bounded v2 fallback during website rollout. Older Store builds retain their known 12/15-entry endpoints; Weekly Money is never injected into a catalog an old Store does not recognise. Only Weekly Money may use a public prerelease in the generator; unrelated draft/prerelease metadata remains rejected.

Same package `com.universalrp.tamilanstore`, version code 16, existing pinned release signer. No new permission. Update in place without uninstalling or clearing app data. The signed candidate is tested over public Store 0.13 for same UID, language/motion preferences, a synthetic private marker, restart and the newly listed Weekly Money entry before promotion of exact bytes. Phone/OEM/Play Protect acceptance is not claimed by emulator evidence.

## தமிழ்

வார வசூல் சோதனைப் பதிப்பு புதிதாக இணைக்கப்பட்டது. இது பணக் கணக்கின் ஒரே பாதுகாப்பான நகலாகப் பயன்படுத்துவதற்கான production உறுதி அல்ல. முந்தைய பதினைந்து செயலிகளும் தொடரும்; பழைய அங்காடிகளின் 12/15 பதிவு endpoints மாறாது. அளவு, SHA-256, package, version மற்றும் கையொப்பச் சரிபார்ப்பு தொடரும். அதே கையொப்பத்துடன் நீக்காமல் புதுப்பிக்கவும். பழைய/வேறு கையொப்பம் இருந்தால் நிறுவல் மறுக்கப்படும்; பாதுகாப்பை மீற வேண்டாம். Uninstall செய்தால் local data / உள்ளூர் தரவு அழியலாம்; backup இல்லாமல் நீக்க வேண்டாம்.
