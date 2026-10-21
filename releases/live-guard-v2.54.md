# Live Guard 2.54

This release addresses the zero-data entry and validity-date requests reported after 2.53, and makes
the scheduled daily security-risk watch opt-in.

## Highlights

### Daily security-risk watch is off until enabled

The scheduled daily security check is now **off by default**. This applies to fresh installs, and the
first 2.54 run after updating 2.53 also turns it off on existing devices once. If the owner later
switches it on, that choice survives restarts and future updates. **Run the daily check now** remains
available as a one-time, manual check.

### Choose no data without typing zero

The GB/MB amount dialog has a direct **No data (0 GB)** button. Typing `0` is accepted as well. It
saves a voice-only plan and mirrors a real 0 GB allowance so an older quota does not continue to
appear on the other data screens. The booster dialog has a separate **Remove booster (0 MB)** action.

### Set plan dates from either end

The validity section now has calendar and time selection fields for both the start and end:

- Choose a **start date/time** and the number of days to calculate the end date.
- Choose an **end date/time** and the number of days to calculate the start date.
- The day count remains inclusive, so a 68-day validity ending 15 December calculates a start of
  9 October.
- A manually chosen end time overrides the carrier's usual expiry rule. Clear that override to
  return to the operator setting (for example, Jio's recharge time or Airtel/Vi's end-of-day rule).

Existing plan dates and operator settings from 2.53 remain readable. The new date and time are
stored locally with the rest of the plan.

## Under the hood

- Added reverse inclusive date arithmetic and plan-specific end date/time policies, covered by
  `PlanPolicyTest`.
- No new Android permissions.
- Version 2.54 / code 59.

## Verification status

Published on 21 October 2026 by [Build APK run 39526559599](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/39526559599).
The build, required unit/regression suites, APK verification and post-publication asset checks passed.
The public APK is 13,818,416 bytes; its SHA-256 is recorded in
[`../docs/published-release-digests.md`](../docs/published-release-digests.md).

This build was **not installed on a phone**; no device compatibility is claimed for these bytes.
Automated builds, unit tests and static checks are separate evidence from a physical-phone test.
Do not treat CI as Play Protect approval.

## தமிழ்

# லைவ் கார்டு 2.54

2.53 புதுப்பிப்புக்குப் பிறகு தெரிவிக்கப்பட்ட பூஜ்யத் தரவு மற்றும் செல்லுபடித் தேதி சிக்கல்களையும்,
தினசரி பாதுகாப்பு அபாயக் கண்காணிப்பின் இயல்புநிலையையும் இந்தப் பதிப்பு சரிசெய்கிறது.

- **தினசரி பாதுகாப்புச் சோதனை இயல்பாக நிறுத்தம்:** புதிய நிறுவல்களில் இது நிறுத்தப்பட்டிருக்கும்.
  2.53-இலிருந்து 2.54-க்கு முதல் முறை புதுப்பிக்கும்போதும், எல்லா சாதனங்களிலும் ஒருமுறை
  நிறுத்தப்படும். பின்னர் உரிமையாளர் இயக்கினால், மறுதொடக்கம் மற்றும் அடுத்த புதுப்பிப்புகளிலும்
  அந்த விருப்பம் பாதுகாக்கப்படும். **தினசரி சோதனையை இப்போதே இயக்கு** என்பதைத் தட்டி ஒருமுறை
  மட்டும் கைமுறையாகச் சோதிக்கலாம்.

- **தரவு இல்லை:** GB/MB அளவு சாளரத்தில் **தரவு இல்லை (0 GB)** பொத்தான் உள்ளது. `0` என்று
  தட்டச்சு செய்தாலும் ஏற்கப்படும். இது குரல் மட்டும் உள்ள திட்டமாகச் சேமித்து, பழைய தரவு அளவு
  வேறு திரைகளில் தொடர்ந்து காட்டப்படாமல் உண்மையான 0 GB ஆக மாற்றும். பூஸ்டர் சாளரத்தில்
  **பூஸ்டரை நீக்கு (0 MB)** என்ற தனி பொத்தான் உள்ளது.
- **தொடக்கத் தேதியிலிருந்து:** தொடக்க தேதி/நேரத்தையும் நாட்களையும் தேர்ந்தெடுத்தால் முடிவு தேதி
  கணக்கிடப்படும்.
- **முடிவு தேதியிலிருந்து:** முடிவு தேதி/நேரத்தையும் நாட்களையும் தேர்ந்தெடுத்தால் தொடக்க தேதி
  கணக்கிடப்படும். இரு தேதிப் புலங்களும் காலண்டரையும் நேரத் தேர்வையும் திறக்கும். நாட்கள் இரு
  முனைகளையும் சேர்த்துக் கணக்கிடப்படும்; 15 டிசம்பர் முடியும் 68 நாள் திட்டத்தின் தொடக்கம்
  9 அக்டோபர்.
- கைமுறையாகத் தேர்ந்தெடுத்த முடிவு நேரம் ஆபரேட்டரின் வழக்கமான முடிவு விதியை மாற்றும். அதை
  நீக்கினால் Jio, Airtel/Vi போன்ற ஆபரேட்டர் விதி மீண்டும் பயன்படுத்தப்படும்.
- 2.53-இல் சேமித்த திட்டத் தகவல் தொடர்ந்து படிக்கப்படும். புதிய அனுமதி எதுவும் சேர்க்கப்படவில்லை.

2.54 APK எந்த ஃபோனிலும் நிறுவிச் சோதிக்கப்படவில்லை; சாதன இணக்கத்தன்மை உறுதியளிக்கப்படவில்லை.
தானியங்கி கட்டமைப்பு அல்லது சோதனைகள் நேரடி ஃபோன் சோதனைக்கோ Play Protect அங்கீகாரத்திற்கோ
சமமல்ல.
