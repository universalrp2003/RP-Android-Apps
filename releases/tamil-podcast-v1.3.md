# Kadhai Solli - Tamil Podcast Player 1.3

## Listening controls

- Tamil name: **கதை சொல்லி**, with the English name retained and the same Android package/signing identity for upgrades.
- Player controls remain visible at the bottom while browsing shows: play/pause, a draggable seek bar, elapsed time and duration, and fifteen-second rewind/forward.
- Continue listening restores the last episode and saved position after reopening. Progress saves every five seconds and on pause/stop. A sudden process kill may repeat a few seconds.
- Media-session controls support the lock screen and compatible headsets. Playback pauses on headphone disconnection or audio-focus loss.
- Notification permission is requested on first playback on Android 13 and later. Denial does not block listening; notification settings remain available.

## Shows and navigation

- Episode search and a thirty-at-a-time More episodes button keep long playlists manageable.
- Five separate show playlists with Tamil and English aliases. Up to 300 episodes per feed, still bounded to prevent an unlimited list.
- Removed the two feeds reported unavailable on the owner's phone. Replacements use published RSS addresses, not guessed links. A release-time network check requires episodes and a responding audio enclosure for every built-in feed.
- System Back returns from episodes to shows, closes the add-feed form, or exits a guide/About page.
- Built-in Tamil/English guide, About page, sister-app links and verified self-updater. Android still requires installation confirmation.

## Limits and testing

- This exact build is **not installed on a phone** by us and has **no physical-device test**. CI and source checks cannot verify touch layout, background behaviour or a particular phone/network.
- Publisher content and availability can change. A responding audio URL is not an end-to-end listening test or a review of every story.
- Streaming needs internet; no offline download queue, playback-speed control or sleep timer is promised.

Candidate unit tests and release compilation passed in [CI run 40471829272](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40471829272). This is not physical-device evidence for the rebuilt published APK.

## தமிழ்

- **கதை சொல்லி** என்ற தமிழ்ப் பெயருடன் ஆங்கிலப் பெயரும் உள்ளது. முந்தைய செயலிக்கு மேலேயே புதுப்பிக்கலாம்.
- திரையின் கீழே எப்போதும் இயக்கம், இடைநிறுத்தம், நேரம், நீளம், இழுத்துத் தேடும் பட்டை மற்றும் பதினைந்து நொடிகள் முன்பின் நகர்த்தும் பொத்தான்கள் உள்ளன.
- தொடர்ந்து கேள் பொத்தான் கடைசிப் பகுதியைச் சேமித்த இடத்திலிருந்து இயக்கும். ஐந்து நொடிகளுக்கு ஒருமுறையும் இடைநிறுத்தும்போதும் இடம் சேமிக்கப்படும்.
- காதுக்கருவியை நீக்கினாலோ வேறு ஒலிக்குச் சாதனம் முன்னுரிமை கொடுத்தாலோ இயக்கம் இடைநிறுத்தப்படும்.
- முதல் இயக்கத்தின்போது அறிவிப்பு அனுமதி கேட்கப்படும். மறுத்தாலும் கேட்கலாம்.
- ஐந்து ஊட்டங்களுக்கும் தமிழ் மற்றும் ஆங்கிலப் பெயர்கள் உள்ளன. ஒவ்வொரு ஊட்டத்திலும் அதிகபட்சம் முந்நூறு பகுதிகள் காட்டப்படும்.
- திறக்காத இரண்டு ஊட்டங்கள் மாற்றப்பட்டுள்ளன. புதியவை வெளியிடப்பட்ட உண்மையான இணைப்புகள். உள்ளடக்கமும் இணையக் கிடைப்பும் வெளியீட்டாளரின் கட்டுப்பாட்டில் உள்ளன.
- பின்செல் பொத்தான் பகுதிகளிலிருந்து ஊட்டங்களுக்குச் செல்லும். வழிகாட்டி, செயலி விவரம், பிற செயலிகளின் இணைப்புகள், சரிபார்க்கப்பட்ட புதுப்பிப்பு உள்ளன.
- **இந்தச் சரியான பதிப்பு தொலைபேசியில் நிறுவிச் சோதிக்கப்படவில்லை.** ஒலி முழுவதையும் கேட்பதும் சிறிய திரையில் தொட்டுப் பயன்படுத்துவதும் இன்னும் சோதிக்கப்பட வேண்டும்.
