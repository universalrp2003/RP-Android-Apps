# Voice Notes 1.2

## Changes

- Search saved notes and edit existing notes while retaining their identity and date.
- Explicit Continue / Not now disclosure before HD neural playback sends the selected note text to the online voice service.
- HD neural voice only: unavailable service reports an error, never a robotic system-voice fallback.
- Synced temporary-file writes and atomic replacement; save failures are visible.
- Prevent overlapping voice requests and clear busy state after errors. Correct title language selection.
- Dictation uses the installed device speech-recognition service; local-only processing is not promised.

## Device test

No physical-device test of this exact build. CI build, tests, signature and asset checks must pass before publication. This is not a device-compatibility guarantee. No new permissions are requested by these changes.

## தமிழ்

சேமித்த குறிப்புகளைத் தேடவும் திருத்தவும் முடியும். தேர்ந்தெடுத்த குறிப்பை இணையக் குரல் சேவைக்கு அனுப்பும் முன் தெளிவான ஒப்புதல் கேட்கப்படும்.

உயர்தர நரம்பியல் குரல் மட்டுமே பயன்படுத்தப்படும். அது கிடைக்காவிட்டால் பிழை காட்டப்படும்; இயந்திரக் குரலுக்கு மாறாது. குறிப்பைச் சேமிக்க முடியாதபோதும் பிழை தெரியும்.

குரல்வழி உள்ளீடு சாதனத்தின் பேச்சறிதல் சேவையைப் பயன்படுத்துகிறது; அது முழுவதும் சாதனத்திலேயே நடைபெறும் என்று உறுதியில்லை. இந்தப் பதிப்பு நேரடியாகச் சாதனத்தில் சோதிக்கப்படவில்லை. புதிய அனுமதிகள் சேர்க்கப்படவில்லை.
