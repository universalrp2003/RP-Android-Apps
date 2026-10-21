# Live Guard 2.60 (`versionCode = 65`) — Weather Screen Button Flow Layout Fix

## English Summary

Following real-world physical-device verification screenshots of **Live Guard 2.59**, **Ramesh Radio 1.18**, and **Oor Arivippu 1.1.4** (`7:58–7:59 PM`, Alangudi `622301`), **Live Guard 2.60** fixes two narrow-width button layout issues spotted in the **Rain & cyclone alert** screen:

1. **Crisp `FlowRow` Button Wrapping (`Check now` · `Test voice` · `Preview animation`):**
   - Replaces the single-row `Row` with a wrapping `FlowRow` so the **Preview animation** button never wraps vertically character-by-character on phone screens.
2. **Clean Full-Width Stacking for `Tomorrow.io` Guide Action Links:**
   - Stacks **Tomorrow.io API key help & docs** and **Step-by-step setup & voice guide** cleanly on separate lines so neither label splits mid-word.

---

## தமிழ் சுருக்கம் (Tamil Summary)

நேரடி போன் திரைப்படங்களில் கண்டறியப்பட்ட இரண்டு வடிவமைப்புத் திருத்தங்களுடன் **Live Guard 2.60** வெளியிடப்படுகிறது:

1. **பொத்தான்கள் மடிப்பு சீரமைப்பு (`FlowRow`):** **Check now**, **Test voice**, மற்றும் **Preview animation** பொத்தான்கள் சிறிய திரைகளிலும் எழுத்துக்கள் செங்குத்தாக உடையாமல் அழகாக அடுக்கப்படும்.
2. **Tomorrow.io வழிகாட்டி இணைப்புகள்:** இரண்டு வழிகாட்டி பொத்தான்களும் தனித்தனி வரிகளில் தெளிவாகக் காட்டப்படும்.

---

## Verification and device testing

The exact APK was **not installed on a phone before publication** (the immediately preceding **Live Guard 2.59**, **Ramesh Radio 1.18**, and **Oor Arivippu 1.1.4** builds were verified live on physical devices by the owner on 21 October 2026). Build, unit/regression (47 suites / 417 tests), localization, manifest permission delta (`0` added permissions), APK and release-asset checks are recorded by the publishing workflow. Passing CI is not a Play Protect approval or an all-device compatibility claim.

Version 2.60 / code 65.
