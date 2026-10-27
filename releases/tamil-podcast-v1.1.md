# Tamil Podcast Player 1.1 - the feeds actually load, and a dead one says so

The owner installed 1.0 and reported that it was stuck: nothing loaded, no permission was asked for, and neither the back button nor the cancel button worked. That report was correct, and the cause was mine.

## What was broken

The four built-in feed URLs in 1.0 **were invented**. They looked plausible, they followed the shape of real podcast feeds, and none of them existed. Every fetch therefore failed, and the screen was built so that a failed fetch left it exactly where it started: on "loading", forever.

Two things made that worse than it needed to be:

- **The load state was a single `busy` flag for the whole screen**, set when a show was opened and cleared only on success. One dead feed froze every show, and the flag had no failure path at all.
- **The fetch had no error handling.** The network call threw, the coroutine that was going to clear the flag died with it, and the flag stayed set. The back button set a different piece of state, so the screen stayed where it was.

The owner's description - "displayed One Time news and not loading cannot cancel or back button not working" - is an accurate account of what a hung screen with no error message looks like.

## What 1.1 does differently

- **Every fetch is wrapped.** A network failure now produces a visible result rather than an exception, so no code path can leave the screen stuck.
- **Load state is per show**, in a map, so one dead feed cannot freeze the others.
- **A feed that cannot be reached says so**, in Tamil, with a retry button and a hint about the connection.
- **Back works from every state**, including while loading and after a failure.
- **Adding a feed reports why it was refused** - not an http(s) URL, already added, or the cap.

## The built-in shows

Two, not four, and both are public feeds I have a real source for rather than ones I made up:

- **கதை கேட்கும் நேரம்** - `https://feeds.buzzsprout.com/1143884`
- **Tamil Podcast · Millennial Thoughts** - `https://anchor.fm/s/2c4ecfc4/podcast/rss`

Fewer shows that exist beats more that do not. Adding a feed by URL is how this app is meant to grow, and these two are a starting point.

## Also

The episode rows now show a spinner while a feed loads instead of only the word "loading", so it is clear the app is doing something.

## Device test status

**This 1.1 build has not been installed on a phone by anyone, so it has had no device test.** The RSS reader, the duration and date parsers, the episode cap and the store's rules are covered by unit tests. What is not covered, and what 1.0 proved matters: whether a real feed parses on a real phone, and whether the failure path reads clearly when a feed is genuinely unreachable. Those need a device.

## தமிழ்

**தமிழ் போட்காஸ்ட் பிளேயர் 1.1** - நாட்காட்டிகள் உண்மையில் ஏற்றுவதே அல்லவென்று உரிமையாளர் 1.0-ஐ நிறுவி தெரிவித்தார்கள். அந்தத் தெரிவிப்பு சரி, காரணம் என்னிடமே இருந்தது.

**என்ன உடைந்திருந்தது**

1.0-ல் உள்ள நான்கு உள்ளமைந்த நாட்காட்டி இணைப்புகள் **நான் கற்பனையாக எழுதியவை**. அவை உண்மையான போட்காஸ்ட் இணைப்புகளின் வடிவத்தில் இருந்தன, ஒன்று கிடைக்கவில்லை. எனவே ஒவ்வொரு பதிவிறக்கும் தோல்வியடைந்தது, திரை அப்படியே நின்றது: "ஏற்றுகிறது", என்று நிரந்தரம்.

இரண்டு விஷயங்கள் இதை இன்னும் மோசமாக்கின:

- **ஏற்றும் நிலை முழுத் திரைக்கும் ஒரே `busy` கொடி**. ஒரு நாட்காட்டி திறக்கப்படும்போது அது அமைக்கப்பட்டது, வெற்றியில் மட்டுமே அழிக்கப்பட்டது. ஒரு இறந்த நாட்காட்டி எல்லா நாட்காட்டிகளையும் உறைத்தது, அந்தக் கொடிக்கு தோல்விப் பாதை எதுவும் இல்லை.
- **பதிவிறக்கத்தில் பிழை கையாளுதல் இல்லை.** வலைய அழைப்பு வீசியது, கொடியை அழைக்கப் போன coroutine அதனுடன் இறந்தது, கொடி அமைத்தபடியே இருந்தது. பின்செல் பொத்தான் வேறு நிலையை மாற்றியது, எனவே திரை அங்கேயே இருந்தது.

**1.1 என்ன வேறாக செய்கிறது**

- **ஒவ்வொரு பதிவிறக்கமும் போர்த்தப்பட்டுள்ளது.** வலைய தோல்வி இப்போது ஒரு விதிவிலக்கல்ல, தெரியும் முடிவு, எனவே எந்தக் கோட்ப் பாதையும் திரையை உறைக்க முடியாது.
- **ஏற்றும் நிலை ஒவ்வொரு நாட்காட்டிக்கும் தனித்தனி**, ஒரு வரைபடத்தில், எனவே ஒரு இறந்த நாட்காட்டி மற்றவற்றை உறைக்காது.
- **அடைய முடியாத நாட்காட்டி அதைச் சொல்லும்**, தமிழில், மீண்டும் முயற்சிப் பொத்தானுடன், இணைப்புப் பற்றி ஒரு குறிப்புடன்.
- **எல்லா நிலையிலிருந்தும் பின்செல் வேலை செய்யும்**, ஏற்றும் போதும், தோல்விக்குப் பிறகும்.
- **நாட்காட்டி சேர்ப்பது ஏன் மறுக்கப்பட்டது என்று சொல்லும்** - http(s) இணைப்பு அல்ல, ஏற்கனவே உள்ளது, அல்லது வரம்பு நிறைந்தது.

**உள்ளமைந்த நாட்காட்டிகள்**

நான்கு அல்ல, இரண்டு, இரண்டும் நான் உண்மையான ஆதாரம் வைத்த பொது நாட்காட்டிகள்:

- **கதை கேட்கும் நேரம்** - `https://feeds.buzzsprout.com/1143884`
- **Tamil Podcast · Millennial Thoughts** - `https://anchor.fm/s/2c4ecfc4/podcast/rss`

கிடைக்காத நான்கை விட கிடைக்கும் இரண்டு நல்லது. இணைப்பை ஒட்டி சேர்ப்பதே இந்தச் செயலி வளரும் வழி, இவை இரண்டு ஒரு தொடக்கம் மட்டுமே.

**மேலும்**

ஏற்றும் போது பகுதி வரிசைகள் இப்போது "ஏற்றுகிறது" என்ற வார்த்தை மட்டுமல்ல, ஒரு சுழற்சிக் காட்டியைக் காட்டுகின்றன, எனவே செயலி ஏதாவது செய்கிறது என்று தெரியும்.

**சாதனப் பரிசோதனை நிலை**

**இந்த 1.1 பதிப்பு யாராலும் எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** RSS படிப்பி, காலம் மற்றும் தேதி பகுப்பாய்வு, பகுதி வரம்பு, கிடங்கின் விதிகள் ஆகியவை அலகு சோதனையில் உள்ளன. இல்லாதவை, 1.0 முக்கியத்துவத்தை நிரூபித்தவை: ஒரு உண்மையான நாட்காட்டி உண்மையான தொலைபேசியில் பகுப்பாய்வு செய்யப்படுகிறதா, நாட்காட்டி உண்மையில் அணுக முடியாத போது தோல்விப் பாதை தெளிவாகத் தெரிகிறதா. இவை ஒரு சாதனம் தேவை.
