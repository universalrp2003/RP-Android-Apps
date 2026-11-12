# Tamil Podcast Player 1.2

## The two things the owner found

**The app published a story that was not for his family.** One of the two built-in feeds carried adult material - love stories, queer fiction, office romance - and he asked for children's stories and the kind a grandfather or grandmother tells instead. That feed is gone. All four built-in shows are now for a family to listen to together:

- **Kadhai Neram** (கதை நேரம்) - Panchatantra, Tenali Raman, Thirukkural and the Hosur Thaatha stories a grandfather tells. Eight hundred episodes.
- **Pesum Kadhai** - the puranas, the Ramayanam and Tamil folk tales.
- **Tamil Stories For Kids** - a hundred moral stories.
- **Kadhai Ketkum Neram** (கதை கேட்கும் நேரம்) - children's stories by the author Ramya Saravanan.

Each of these is named in the Tamil section below in the same words.

**One feed was blocked and the app showed the block page as a podcast.** The owner's screen had a show called "Attention Required - Cloudflare". That was a host answering the request with a challenge page, and the reader was reading the page's HTML title as the name of a show. The fetch now sends a User-Agent, which is the ordinary thing a feed reader does, and refuses any body that is not a feed - so a block page, a login page or a mistyped link says "this link is not a podcast feed" instead of inventing a show name.

## The screen looks like a podcast player now

The previous layout was a column of titles with a play link beside each. It now has:

- **Cover art**, taken from the feed's own published image. A show with no artwork gets its first letter on a tinted tile, so the list never has empty grey boxes. No image library was added - OkHttp was already there and this is one small bitmap.
- **A numbered playlist** with each episode's length and the date it arrived, which is the shape every player of this kind uses.
- **The show's artwork** at the top of its own episode list, next to its name and a back button.

## Honest limits

- **This 1.2 build has not been installed on a phone by anyone**, so it has had no device test.
- None of the four feed URLs could be reached from the machine this was built on - only github, npm and pypi are reachable there. Each address was published as its show's RSS feed by a podcast directory rather than constructed by guesswork, but whether each one loads on a phone is unverified.
- Cover art is fetched from the publisher's server, so a feed with slow or missing artwork shows the letter tile.
- The player is still `android.media.MediaPlayer`: no background download queue, no playback speed, no sleep timer.

## தமிழ்

**தமிழ் போட்காஸ்ட் பிளேயர் 1.2** - உள்ளமைந்த ஊட்டங்கள் குடும்பத்திற்குரியவை, மற்றும் திரை இப்போது ஒரு உண்மைய போட்காஸ்ட் பிளேயரைப் போலவும்.

**முதல் விஷயம்: பொருள்கள்**

1.1-ல் இருந்த இரண்டு ஊட்டங்களில் ஒன்றில் பெரியவர்களுக்குரிய உள்ளடக்கம் இருந்தது - காதல், அலுவலக கதைகள். உரிமையாளர் அதை நிறுத்தி, குழந்தைகளுக்கான கதைகளையும் தாத்தா பாட்டிகள் சொல்வதற்குரிய கதைகளையும் கேட்டார். அந்த ஊட்டம் நீக்கப்பட்டது.

இப்போது உள்ள நான்கு ஊட்டங்களும் ஒரு குடும்பம் ஒன்றாகக் கேட்கவற்றை:

- **கதை நேரம்** - பஞ்சதந்திரம், தெனாலிராமன், திருக்குறள், ஓசூர் தாத்தா கதைகள். எட்டு நூற்றுக்கும் மேற்பட்ட கதைகள்.
- **Pesum Kadhai** - புராணங்கள், ராமாயணம், நாட்டுப்புறக் கதைகள்.
- **Tamil Stories For Kids** - நூற்றுக்குமே அதிகமான நீதிக் கதைகள்.
- **கதை கேட்கும் நேரம்** - எழுத்தாளர் ரம்யா சரவணனின் குழந்தைக் கதைகள்.

**இரண்டாம் விஷயம்: தடுக்கப்பட்ட ஊட்டம்**

உரிமையாளரின் திரையில் "Attention Required - Cloudflare" என்ற பெயரில் ஒரு காட்சி இருந்தது. அது ஒரு ஊட்டம் அல்ல - அந்தச் சேவையகம் கோரிக்கையை நிராகரித்து ஒரு தடைப் பக்கத்தை அனுப்பியது, மற்றும் அந்தப் பக்கத்தின் தலைப்பை செயலி ஒரு காட்சியின் பெயராகக் காட்டியது.

இப்போது கோரிக்கையுடன் ஒரு பயனர்ப் பெயர் அனுப்பப்படுகிறது. மேலும், ஒரு பதில் ஊட்டம் அல்ல என்றால் - மறுக்கும் பக்கம், உள்நுழைவுப் பக்கம், தவறான இணைப்பு - செயலி "இந்த இணைப்பு ஒரு போட்காஸ்ட் ஊட்டம் அல்ல" என்று சொல்லும். ஒரு காட்சியின் பெயரைக் கற்பனை செய்யாது.

**மூன்றாம் விஷயம்: திரை**

முன்பு தலைப்புகளின் ஒரு நீண்ட பட்டியல் மட்டும் இருந்தது, ஒவ்வொன்றுக்கு அருகில் ஒரு இயக்கும் இணைப்பு. இப்போது:

- **ஊட்டம் வெளியிடும் சித்திரம்** - பலகையின் மேல் இடது புறம். சித்திரம் இல்லாத ஊட்டத்திற்கு அதன் முதல் எழுத்து ஒரு வண்ணத் தட்டில்.
- **எண்ணிட்ட பகுதி பட்டியல்** - ஒவ்வொரு பகுதிக்கும் எண், நீளம், வந்த தேதி. இதே வடிவத்தை மற்ற எல்லா போட்காஸ்ட் பிளேயர்களும் பயன்படுத்துகின்றன.
- **காட்சியின் சித்திரம்** அதன் பகுதிப் பட்டியலின் மேலே, பெயருக்குப் பக்கத்தில்.

புதிய சார்பு எதுவும் சேர்க்கப்படவில்லை. சித்திரம் ஏற்றுவதற்கு ஏற்கனவே உள்ள OkHttp பயன்படுத்தப்படுகிறது.

**சாதனப் பரிசோதனை நிலை**

**இந்த 1.2 பதிப்பு யாராலும் எந்தச் சாதனத்திலும் நிறுவப்படவில்லை, எனவே சாதனப் பரிசோதனை எதுவும் நடக்கவில்லை.** நான்கு ஊட்ட இணைப்புகளும் இந்தக் கணினியிலிருந்து அடைய முடியவில்லை - இங்கே github, npm, pypi மட்டுமே அடையக்கூடியவை. ஒவ்வொரு இணைப்பையும் ஒரு போட்காஸ்ட் வழங்குநர் வெளியிட்டிருக்கிறது, ஆனால் ஒரு தொலைபேசியில் திறக்குமா என்பது சோதிக்கப்படவில்லை.
