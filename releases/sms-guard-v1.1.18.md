# SMS Guard 1.1.18 — delivery reports and message archive

- Every SMS you send now shows the radio's own answer under the message: **Sending…** while it is handed to the network, **Sent ✓** once the tower accepts it, and **Delivered ✓✓** only when the recipient's network sends a delivery report. A failed send or a negative report shows **Not sent ✗** / **Not delivered ✗** with the carrier's reason (radio off, no signal, sending limit, FDN, encoding). A carrier that sends no delivery report leaves the message at "Sent"; the app never invents "Delivered". Messages sent with earlier versions are shown as plain "Sent".
- A message in any conversation now has an **Archive** action. Archived messages leave the conversation and are listed under the new **📦 Archived** chip; **Unarchive** puts a message back. Archive is a list of message ids kept inside the app: the message itself is never moved or deleted, so unarchiving is instant and nothing can be lost. A conversation whose every message is archived disappears from the main list until you unarchive one.
- Works whether or not SMS Guard is the default SMS app (archive always; delivery status needs the default-SMS role to write the status into the message store).

Normal same-key update from 1.1.17: do not uninstall or clear app data. Existing messages, default-SMS role and preferences should remain. No new permissions; the new status receiver is reachable only by the system radio's own callbacks. SMS-only default mode remains explicit; MMS is not implemented.

This release is not a physical-phone, carrier/MMS, audible-playback or Google Play Protect certification. Historical legacy-key installations cannot update across signing identities; do not erase data as a workaround.

This candidate is not installed on a phone; delivery-report behaviour depends on the carrier and must be checked on a real SIM. Exact release bytes are not installed on a phone as part of this CI verification. Emulator and unit tests are separate evidence, not phone acceptance.

## தமிழ்

நீங்கள் அனுப்பும் ஒவ்வொரு குறுஞ்செய்திக்கும் கீழே நெட்வொர்க்கின் பதில் காட்டப்படும்: அனுப்பும் போது **அனுப்புகிறது…**, கோபுரம் ஏற்றுக்கொண்டதும் **அனுப்பப்பட்டது ✓**, பெறுநரின் நெட்வொர்க் உறுதியளித்தால் மட்டும் **வழங்கப்பட்டது ✓✓**. தோல்வியானால் **அனுப்ப முடியவில்லை ✗** / **வழங்கப்படவில்லை ✗** என்று நெட்வொர்க்கின் காரணத்துடன் (ரேடியோ அணைப்பு, சிக்னல் இல்லை, அனுப்பும் வரம்பு, FDN, குறியீட்டுப் பிழை) காட்டப்படும். டெலிவரி அறிக்கை அனுப்பாத நெட்வொர்க்கில் செய்தி "அனுப்பப்பட்டது" என்றே இருக்கும்; செயலி "வழங்கப்பட்டது" என்று பொய் சொல்லாது. பழைய பதிப்புகளில் அனுப்பிய செய்திகள் வெறும் "அனுப்பப்பட்டது" எனக் காட்டப்படும்.

எந்த உரையாடலிலும் ஒவ்வொரு செய்திக்கும் **காப்பகப்படுத்து** செயல் உள்ளது. காப்பகப்படுத்தப்பட்ட செய்திகள் உரையாடலிலிருந்து மறைந்து புதிய **📦 காப்பகம்** சிப்பில் பட்டியலிடப்படும்; **காப்பகத்தை நீக்கு** என்பது செய்தியை மீண்டும் காட்டும். காப்பகம் என்பது செயலிக்குள் சேமிக்கப்பட்ட செய்தி எண்களின் பட்டியல் மட்டுமே; செய்தி நகர்த்தப்படவோ நீக்கப்பவோ இல்லை, எனவே எதுவும் இழக்க முடியாது. ஒரு உரையாடலின் அனைத்துச் செய்திகளும் காப்பகப்படுத்தப்பட்டால் அது முதன்மைப் பட்டியலிலிருந்து மறையும்.

1.1.17 இலிருந்து சாதாரண புதுப்பிப்பு செய்யவும். செயலியை நீக்கவோ உள்ளூர் தரவு அழியும்படி அழிக்கவோ வேண்டாம். குறுஞ்செய்திகள், அமைப்புகள் மற்றும் இயல்புநிலை SMS தேர்வு பாதுகாக்கப்பட வேண்டும். புதிய அனுமதிகள் இல்லை; MMS ஆதரவு இல்லை. இந்த வேட்பாளர் கைப்பேசியில் நிறுவப்படவில்லை; டெலிவரி அறிக்கை நெட்வொர்க்கைப் பொறுந்தது, உண்மையான SIM-இல் சரிபார்க்க வேண்டும். கைப்பேசி ஒலி அல்லது Google ஒப்புதல் சான்று அல்ல.
