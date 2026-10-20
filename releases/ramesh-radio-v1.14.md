# Ramesh Radio 1.14

A calmer first screen, a readable update card, and Tamil that says what it means.

## Highlights

### The update section is a card now, not a grey wall

On the **More** tab the update block used to sit bare between two rounded cards. In the Tamil UI its
text rendered almost black on the dark background, and the block left a visible empty band above the
About card. It is now inside the same card style as every other section:

- a clear **Verified GitHub APK updates** title and one short sentence;
- **How updates are verified** opens only when tapped — the long explanation no longer fills the
  screen;
- every line uses the theme's own text colours, so it is readable in Tamil and English;
- the launch-check switch, Check for updates and the download/install steps are unchanged.

### The first screen is quieter

- The search box is **one line until you touch it**; tapping opens the full field, with the live
  directory button beside it. Once you have typed a search, the field stays open.
- The sorting explanation ("Verified streams stay first…") is folded into **How this list is
  sorted**, so the station list starts higher on the screen.

### Tamil fixes

- The songs tab used to read **உள்ளூர் ஒலி**, which sounds like a local radio station. It now reads
  **பாடல்கள்**, and the screen heading is **என் பாடல்கள்** — the songs on this phone.
- New Tamil text for the search hint and the sorting line.

## Installation and safety

Download `Ramesh-Radio-v1.14.apk` and install it through Android. The update installs over 1.13 with
the same signing key, so favourites, custom stations, the equalizer and the language choice are
kept. No new permission is added.

**Device testing:** this build was **not installed on a phone** at the time of writing. The release
passed the CI build, the unit and regression suites, the APK metadata checks and the published-asset
check. That is not Play Protect approval and not an all-device compatibility claim.

## தமிழ்

# ரமேஷ் ரேடியோ 1.14

- **புதுப்பிப்புப் பகுதி இப்போது ஒரு அட்டை.** முன்பு இது இரண்டு அட்டைகளுக்கு நடுவே வெறும்
  உரையாக இருந்தது; தமிழில் எழுத்துகள் கிட்டத்தட்ட கருப்பாகத் தெரிந்தன, கீழே காலி இடமும் தெரிந்தது.
  இப்போது தலைப்பு, ஒரு சிறு வரி, தேவைப்பட்டால் மட்டும் திறக்கும் **புதுப்பிப்புகள் எப்படிச்
  சரிபார்க்கப்படுகின்றன**, மற்றும் தோற்றத்தின் நிறங்களில் படிக்கக்கூடிய உரை.
- **முதல் திரை அமைதி.** தேடல் பெட்டி தொடும் வரை ஒரே வரியில் இருக்கும்; வரிசைப்படுத்தல் விளக்கம்
  **இந்தப் பட்டியல் எப்படி வரிசைப்படுத்தப்பட்டது** என்பதற்குள் மடங்கியுள்ளது.
- **தமிழ் சொல் திருத்தம்.** பாடல்கள் தாவல் முன்பு **உள்ளூர் ஒலி** என்று இருந்தது — அது உள்ளூர்
  வானொலி நிலையம் போலத் தோன்றியது. இப்போது **பாடல்கள்**; திரையின் தலைப்பு **என் பாடல்கள்**.

`Ramesh-Radio-v1.14.apk`-ஐ Android வழியாக நிறுவவும். 1.13-இன் மேல் அதே கையொப்பக் குறியுடன்
நிறுவப்படும்; பிடித்தவை, சொந்த நிலையங்கள், ஒலிச் சமநிலை மற்றும் மொழி அமைப்பு அப்படியே இருக்கும்.
புதிய அனுமதி இல்லை.
