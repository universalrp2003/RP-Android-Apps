# Live Guard 2.52

A shorter first page, a side menu you can read, and Tamil that finally has room to breathe.

## Highlights

### The first page is short now

The owner's words: *"important items in first page, not very long scrolling page"*. Home used to
open with a four-tile readings matrix, a separate scan button, a plan card and four more cards
below it. Now it opens with three things:

1. **Free storage** in large type, with one bar showing used of total, and a summary chip row for
   **Battery · RAM free · Signal**.
2. **Scan & clean junk** — the action sits inside that same card, so it is one tap from the top.
3. **Your plan** — data left, validity and booster, exactly as before.

Everything else is grouped under three section headers that open when tapped:

| Section | What is inside |
|---|---|
| **Cleaning tools** | Readings matrix (storage, RAM, permission review, network probe) and the per-kind scan tiles |
| **Phone, network & security** | Live battery/heat line, status-pill controls, history shortcut, the shortcut grid |
| **Assistant & setup** | The assistant card and the feature-setup review |

A section remembers whether it was open, so coming back from another screen does not close it.

### The side menu is a real menu

- A tinted **banner** with the app name, the tagline and a close button.
- Each subject has a **heading with a rule** beside it, so *Cleaning*, *Phone*, *Network & data*,
  *Security*, *Assistant* and *Live Guard* are visible as groups, not a flat list.
- Every row has an **icon tile and a one-line hint** ("Recharge, data left, validity"), and the
  screen you are on gets an **accent rail** and a tinted row — so "where am I" needs no reading.
- The footer holds **language** and **theme** switches, the two things people look for here.

### Tamil typography

Tamil glyphs are taller than Latin ones, and at Material's Latin line height they crowd each other
and look cramped — the owner's *"letters and size placement making the look bad"*. When the app is
in Tamil, every text style now gets a taller line box (1.62×), no extra letter spacing (Latin
tracking pulls Tamil clusters apart) and a slightly larger glyph size. English keeps Material's
defaults, untouched. This applies to every screen at once, because it is the theme's typography,
not a per-screen patch.

### Themes

The seven palettes from 2.51 are unchanged in name and meaning; the menu banner and the hero card
now use the theme's own two signature colours, so a theme change is visible immediately instead of
only on buttons.

## Installation and safety

Download `CleanSweep-v2.52.apk` and install it through Android. It installs over 2.51 with the same
signing key, so the plan, theme, language and settings are kept. **No new permission is added** — the
permission list is identical to 2.51.

**Verified in CI:** run 39418752154 compiled both variants, ran every required unit and regression
suite, verified the APK metadata and the permission delta (no permission added), and uploaded the
APK. The published digest is recorded in `docs/published-release-digests.md`.

**Device testing:** this build was **not installed on a phone** at the time of writing. It passed the
CI build, the required unit and regression suites, the APK checks and the permission delta. That is
not Play Protect approval and not an all-device compatibility claim.

## தமிழ்

# லைவ் கார்டு 2.52

முதல் பக்கம் சுருக்கம், பக்கப் பட்டி தெளிவு, தமிழுக்கு இப்போது போதுமான இடைவெளி.

- **முதல் பக்கம் இப்போது மூன்று விஷயங்கள் மட்டுமே:** **காலி சேமிப்பு** (பயன்படுத்திய/மொத்தம்
  பட்டையுடன்), அதே அட்டையில் **ஸ்கேன் செய்து சுத்தம் செய்** பொத்தான், மற்றும் **உங்கள் பேக்கேஜ்**.
  மற்றவை தட்டினால் திறக்கும் மூன்று பிரிவுகளுக்குள்: **சுத்தம் செய்யும் கருவிகள்**,
  **ஃபோன், நெட்வொர்க் மற்றும் பாதுகாப்பு**, **உதவியாளர் மற்றும் அமைப்பு**. எந்தப் பிரிவைத்
  திறந்தீர்கள் என்பது நினைவில் இருக்கும்.
- **பக்கப் பட்டி:** மேலே நிறமான தலைப்புப் பட்டை; ஒவ்வொரு பிரிவுக்கும் கோடுடன் கூடிய தலைப்பு;
  ஒவ்வொரு வரியிலும் ஐகான் மற்றும் ஒரு சிறு விளக்கம்; நீங்கள் இருக்கும் திரைக்கு இடதுபுறம்
  வண்ணக் கோடு. கீழே **மொழி** மற்றும் **தோற்றம்** பொத்தான்கள்.
- **தமிழ் எழுத்து அளவு:** தமிழில் ஒவ்வொரு எழுத்துரு நடையும் அதிக வரி உயரத்துடனும் (1.62×),
  கூடுதல் எழுத்து இடைவெளி இல்லாமலும், சற்று பெரிய அளவிலும் அமைகிறது. ஆங்கிலத்தில் மாற்றம் இல்லை.
- **தோற்றங்கள்:** ஏழு நிறங்களும் அதே பெயர்களில்; பட்டித் தலைப்பும் முதல் அட்டையும் அந்த
  நிறத்தின் இரு வண்ணங்களில் அமைவதால் தோற்ற மாற்றம் உடனே தெரியும்.

`CleanSweep-v2.52.apk`-ஐ Android வழியாக நிறுவவும். 2.51-இன் மேல் அதே கையொப்பக் குறியுடன்
நிறுவப்படும்; பேக்கேஜ், தோற்றம், மொழி மற்றும் அமைப்புகள் அப்படியே இருக்கும். புதிய அனுமதி இல்லை.
