# Tamilan Store 0.13 — Android TV Guard and System App Disabler

The Store now lists fifteen apps in English and Tamil: the thirteen you already had, plus **Android TV Guard 1.5.1** for Android TV boxes and phones, and **System App Disabler 0.1.0**, which disables preinstalled apps through Shizuku and never uninstalls anything or asks for root.

What changed for you:

- Both new entries use the same signed download, hash and signer checks as every other app.
- Refresh catalog keeps working after a failed download; an error message no longer blocks the next check.
- The bundled fallback list now matches the published release list, so a fresh install with no network still shows all fifteen apps and their real versions instead of an older thirteen.
- Older Store builds (0.5 through 0.12) keep the frozen twelve-app endpoint they expect, so installing this update never breaks a Store you have not updated yet.
- No new permissions. Package, checksum, signer and downgrade checks are unchanged.

Update over the current Store without uninstalling and without clearing local data; that keeps your language choice, motion preference and downloads. Uninstalling may erase local data, so back up first if you must remove it.

Release gates: unit tests, lint, a release-key build, and a same-key `adb install -r` update over the exact public Store0.12 on an Android 15 emulator with preferences and a synthetic private file preserved. The final bytes are not installed on a physical phone, and no Play Protect clearance is claimed. Stop if Play Protect warns; never bypass it.

## தமிழ்

இப்போது அங்காடியில் பதினைந்து செயலிகள் உள்ளன: ஏற்கனவே இருந்த பதின்மூன்று, மேலும் **Android TV Guard 1.5.1** (டிவி காவலன்) மற்றும் **System App Disabler 0.1.0** (சிஸ்டம் செயலி முடக்கி — Shizuku வழியாக மட்டும் முடக்கும், நீக்காது, root தேவையில்லை).

- புதிய இரண்டு செயலிகளுக்கும் மற்ற செயலிகளைப் போலவே கையொப்பம், கோப்பு மற்றும் கையொப்பச் சரிபார்ப்பு உள்ளது.
- பதிவிறக்கம் தோல்வியடைந்த பிறகும் பட்டியலைப் புதுப்பிக்கலாம்.
- இணையம் இல்லாமல் புதிதாக நிறுவினாலும் பதினைந்து செயலிகளும் அவற்றின் உண்மையான பதிப்புகளுடன் தெரியும்.
- பழைய அங்காடி பதிப்புகள் (0.5–0.12) தங்கள் பன்னிரண்டு-செயலி முகவரியை இழக்காது.
- புதிய அனுமதி இல்லை. கையொப்பம், கோப்பு மற்றும் பதிப்புச் சரிபார்ப்பில் மாற்றமில்லை.

செயலியை நீக்காமல், உள்ளூர் தரவை அழிக்காமல் புதுப்பிக்கவும். நீக்கினால் உள்ளூர் தரவு அழியலாம். இறுதிக் கோப்பு நேரடிக் கைப்பேசியில் நிறுவப்படவில்லை; Play Protect ஒப்புதல் எதுவும் கூறப்படவில்லை. பாதுகாப்பு எச்சரிக்கை வந்தால் நிறுத்துங்கள்; அதைத் தவிர்க்க வேண்டாம்.
