# Voice Notes 1.1

## What changed

- **The title and the footer line no longer sit under the system bars.** On Android 15 and later an app
  that targets API 36 cannot opt out of edge-to-edge: the window always extends behind the status and
  navigation bars. Voice Notes 1.0 applied no insets, so the app title was drawn under the clock and
  the "HD neural voice · no cloud · no account" line under the navigation bar. Both are now inside
  the bars.
- Nothing else changed. The notes, the dictation, the neural playback, the Tamil wording and the
  permissions are the same code as 1.0.

## What the owner found

The 1.0 build was installed on a phone, and its screenshots showed exactly the two things this
release fixes: the title clipped by the status bar, and the footer clipped by the navigation bar.
They share one root cause — missing window insets — so they are one fix, not two.

## Device test

The exact 1.1 bytes are not phone-installed. The 1.0 build before them was, and its screenshots are
what found the defect this release repairs. Everything in 1.1 is a layout-inset change with no effect
on the note store, the dictation or the neural playback, all of which the owner has already exercised
on 1.0.

## தமிழ்

முதல் வெளியீட்டில் செயலியின் தலைப்பு நிலைப்பட்டையின் அடியில், கீழே உள்ள வரி வழிச்செலுத்து பட்டையின் அடியில் ஒட்டிக்காட்டப்பட்டது. ஆண்ட்ராய்டு 15 மற்றல் அதற்குப் பிறகு திரை முழுவதும் பயன்படுத்த வேண்டும், எனவே இந்த இரு வரிகளும் இப்போது சரியாகத் தெரிகும். மற்ற எல்லாமும் மாறவில்லை — குறிப்புகள், பேச்சு, நரம்பியல் குரள், அனுமதிகள் எல்லாமும் 1.0 அதே code.
