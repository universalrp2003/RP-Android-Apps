# Live Guard 2.53

The seven things the owner listed after using 2.52 — the volume that was hard to reach, the Guide
that opened the wrong page, the flickering white notification square, the update card that ate the
first page, the missing phone age, the oversized assistant card, and the three widgets.

## Highlights

### The volume button opens the volume

> *"the whole application volume slide must be open if i click main page volume button, also in
> settings page."*

Before, the speaker on Home navigated to the **Voice** screen — a page about voices, alarms and
languages — to reach one slider, and the plan card carried its own private copy of that slider. Now
there is one control, `AnnouncementVolumePanel`, used in three places:

- **Home**: tapping the speaker opens the slider in a dialog over the page. *Done* returns to
  exactly where you were, with nothing navigated away.
- **Settings**: the slider sits in its own card, near the top, next to the theme strip.
- **Plan card** (My plan / Mobile): the duplicated block was replaced by the same panel, so all
  three screens can no longer drift apart.

The slider writes the value once, when the finger lifts, and the **Test** button speaks the sample
sentence at the value you just chose. Muting it (0 %) says so in words.

### Guide opens the guide

The **Guide** row in the side menu was wired to `Screen.FEATURE_SETUP` — *Reviewed feature setup*,
the page that says what Live Guard may do. So "how does this work" landed on a permission
checklist. That was the wrong page, exactly as the owner reported.

- **Guide** now has its own `Screen.GUIDE` and shows the real guide — the same expandable Tamil and
  English text the About screen already used, in its own screen with a back arrow.
- **Reviewed feature setup** kept its own row, with its own hint (*"What Live Guard may do"*), so
  the checklist is still one tap away and nobody has to guess which is which.

### One stable notification icon

The data watch notification used `R.mipmap.ic_launcher`. Android draws a status-bar icon from the
**alpha channel only** — a full-colour launcher icon therefore becomes a white square, and the
square changes shape as Android scales it, which is the "Live Guard symbol and white box" the owner
saw changing. It now uses `R.drawable.ic_stat_data`, the same monochrome glyph the other Live Guard
notifications already use. One icon, one silhouette, no white box.

### The update card is one small row

> *"just put one small button if user clicks display info."*

On Home, the update card is now a single slim row: an icon, **App updates**, one status line
(*Update available · 2.53* / *Up to date · tap for details*) and a chevron. Tapping it opens the
dialog that already held everything — installed version, what is downloaded and from where, the
SHA-256 and signing checks, the *check on launch* switch, the release notes and the install steps.
Nothing was deleted; it is simply not read unless asked for. The full card is still what the About
screen shows.

### Phone age, and a small assistant

- **Phone age** now appears on Home, inside the hero card under the storage bar: *Phone age ·
  About 4 yr 2 mo (2021)*. It is the same local estimate as before — model family and factory
  Android generation — and when the ROM hides that evidence the row says **Not enough
  information** rather than guessing. It is never presented as a manufacture or purchase date.
- **The assistant** is a small symbol on Home: a 28 dp gradient tile, the word *Assistant*, the
  engine in one muted line, and a chevron. One tap opens the assistant window. The large card with
  its own 40 dp tile, ONLINE AI badge and question box is gone from Home — those belong on the
  assistant screen, where the answers are actually read.

### Three widgets, three jobs

All three launcher entries are still 2×2 and still have the small **View** button that cycles
between them; what each one *shows* changed.

| Launcher entry | Shows |
|---|---|
| **Battery & plan** | Battery % and charging state, then *Left 3.2 GB · 24 days left*, then free storage and temperature |
| **Plan & days** | The remaining data in large type, *24 days left* beside it, the pack size and today's usage |
| **Security** | The last saved permission review: *2 high*, *Fix these first*, medium count, apps checked and the review date |

The plan lines are read from the plan the user already set (Jio/Airtel/Vi timing rules included),
so the widget and the app agree. The security widget reads the **saved** review only — it never
runs a scan in the background, so refreshing the widget does not cost battery. With no saved review
it says *No saved review* and *Tap Open to run the permission review*.

The class names (`CleanSweepWidget`, `CleanSweepDataWidget`, `CleanSweepPhoneWidget`), the 2×2
preset XML and the manifest receivers are unchanged, so existing widgets update in place.

## Under the hood

- New `ui/AnnouncementVolume.kt` — `AnnouncementVolumePanel` and `AnnouncementVolumeDialog`.
- New `ui/LiveGuardGuideScreen.kt` and `Screen.GUIDE`; `AboutScreen` and the menu both reach the
  same guide content.
- `LiveGuardUpdateCard(compact = true)` on Home; the About screen keeps the full card.
- Widget strings and labels updated in English and Tamil (`Battery & plan`, `Plan & days`,
  `Security`).
- 12 new Tamil strings for the new controls and widget states.
- Version 2.53 / code 58. **No new permission.**

## Verification

**Static checks:** `verify_cleaner_next_update_source.py` and the regression suites pass, including
the widget contract (three receivers, `exported=false`, three 2×2 presets) and the language pins.

**CI:** the Build APK workflow compiles both variants, runs every required unit and regression
suite, verifies the APK metadata and the permission delta, and uploads the APK. The published
digest is recorded in `docs/published-release-digests.md`.

**Device testing:** this build was **not installed on a phone** at the time of writing. CI
compilation and the static checks are not a device test, not Play Protect approval and not an
all-device compatibility claim.

## தமிழ்

# லைவ் கார்டு 2.53

2.52 பயன்படுத்திய பிறகு சொன்ன ஏழு விஷயங்கள்: ஒலி அளவு, வழிகாட்டி, அறிவிப்பு ஐகான், புதுப்பிப்பு
அட்டை, போனின் வயது, உதவியாளர் அளவு, மூன்று விட்ஜெட்டுகள்.

- **ஒலி அளவு:** முதல் பக்கத்தில் உள்ள ஸ்பீக்கர் பொத்தானைத் தட்டினால் இப்போது **ஒலி அளவு சறுக்கி**
  அதே பக்கத்தின் மேல் திறக்கிறது; *முடிந்தது* தட்டினால் அதே இடத்திற்குத் திரும்பும். **அமைப்புகள்**
  பக்கத்திலும் அதே சறுக்கி தனி அட்டையில் உள்ளது. திட்ட அட்டையில் இருந்த தனி நகல் நீக்கப்பட்டு
  இதே கட்டுப்பாடு பயன்படுத்தப்படுகிறது. **சோதனை** பொத்தான் தேர்ந்தெடுத்த அளவில் ஒரு வாக்கியத்தைப்
  பேசிக் காட்டும்.
- **வழிகாட்டி:** பக்கப் பட்டியில் *வழிகாட்டி* தவறான பக்கம் (அனுமதி பட்டியல்) திறந்தது. இப்போது
  அது உண்மையான வழிகாட்டியைத் திறக்கிறது; அனுமதி பட்டியலுக்கு *சரிபார்த்த வசதி அமைப்பு* என்ற
  தனி வரி உள்ளது.
- **அறிவிப்பு ஐகான்:** நிலைப்பட்டையில் ஆப் ஐகான் வெள்ளைச் சதுரமாகத் தெரிந்தது; இப்போது மற்ற
  லைவ் கார்டு அறிவிப்புகளைப் போல ஒரே வண்ண ஐகான். மாறும் வெள்ளைப் பெட்டி இல்லை.
- **புதுப்பிப்பு:** முதல் பக்கத்தில் இப்போது ஒரு சிறிய வரி மட்டும் — *ஆப் புதுப்பிப்புகள்* மற்றும்
  நிலை. தட்டினால் முழு விவரமும் (பதிப்பு, SHA-256, கையொப்பச் சரிபார்ப்பு, வெளியீட்டுக் குறிப்புகள்,
  நிறுவும் படிகள்) அதே உரையாடலில் திறக்கும். எதுவும் நீக்கப்படவில்லை.
- **போனின் வயது:** முதல் பக்கத்தின் முதல் அட்டையில் *போனின் வயது · சுமார் 4 வருடம் 2 மாதம் (2021)*
  என்று தெரிகிறது. இது மதிப்பீடு மட்டுமே; தகவல் இல்லாவிட்டால் *மதிப்பிடத் தகவல் இல்லை* என்று
  சொல்லும், ஊகிக்காது.
- **உதவியாளர்:** இப்போது சிறிய ஐகான் — ஒரு தட்டில் உதவியாளர் சாளரம் திறக்கும்.
- **விட்ஜெட்டுகள்:** **பேட்டரி & திட்டம்** (பேட்டரி, மீதமுள்ள தரவு, நாட்கள், சேமிப்பு),
  **திட்டம் & நாட்கள்** (மீதமுள்ள தரவு பெரிதாக, நாட்கள், இன்றைய பயன்பாடு),
  **பாதுகாப்பு** (கடைசியாகச் சேமித்த அனுமதி விவரம்: அதிக/நடுத்தர ஆபத்துகள், ஆப்கள், தேதி).
  பாதுகாப்பு விட்ஜெட் பின்னணியில் ஸ்கேன் செய்யாது.
- **புதிய அனுமதி இல்லை.** பதிப்பு 2.53 / குறியீடு 58.
