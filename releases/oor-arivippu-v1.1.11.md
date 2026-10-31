# Oor Arivippu 1.1.11 — district context and current About directory

- Keep town/locality and district separate. A missing locality no longer promotes the district into a town. A city and its district may share a name without losing the district field.
- Known districts are explicitly spoken as “Pudukkottai district” / “புதுக்கோட்டை மாவட்டம்”, including startup and district-only fixes. These are geocoder location reports, not independently verified border crossings; no “entering” claim is added. Unknown districts are never invented.
- History retains combined town, district and landmark labels instead of a later landmark replacing the district title. Headset current-location requests can report district-only fixes without substituting a stale town.
- About uses the shared published-release directory, without a fixed app count, old pinned release links or the legacy InternetRadio repository link. Ramesh Radio remains an active app in the directory; the retired repository is not its current source link.
- Same-town area controls, 500 m/user-selected movement threshold, ten-minute town-repeat protection, call queuing, offline suppression and saved settings/history remain intact. A circular route can legitimately revisit towns; no trip-wide mute is introduced.
- Candidate only; not published. The exact candidate APK is not installed on a phone. CI tests/lint/signing are not physical location or speech proof, nor Play Protect clearance. Intended as an in-place update from owner-key 1.1.10 with no data reset or new permissions; old-key installations remain incompatible across keys.

Uninstalling can erase local data; it is not required to upgrade the current owner-key version. Stop on a signature mismatch rather than resetting data.

## தமிழ்

- ஊரும் மாவட்டமும் தனித்தனியாகக் கையாளப்படும். ஊர் பெயர் கிடைக்காதபோது மாவட்டப் பெயரை ஊராகக் கூறாது. நகரமும் மாவட்டமும் ஒரே பெயரில் இருந்தாலும் மாவட்ட விவரம் மறையாது.
- மாவட்ட விவரம் கிடைத்தால் புதுக்கோட்டை மாவட்டம் என்று தெளிவாகக் கூறப்படும். தொடக்கத்திலும் மாவட்டம் மட்டுமே கிடைக்கும்போதும் இது பொருந்தும். இது இருப்பிடச் சேவை தரும் தகவல்; எல்லையைக் கடந்ததற்கான தனிச் சான்று அல்ல. தெரியாத மாவட்டத்தை உருவாக்கிக் கூறாது.
- ஊர், மாவட்டம் மற்றும் முக்கியச் சாலை ஒரே அறிவிப்பில் வந்தால் அனைத்துப் பெயர்களும் வரலாற்றுத் தலைப்பில் இருக்கும். தலைக்கவசக் கட்டுப்பாட்டில் தற்போதைய இடம் கேட்கும்போது பழைய ஊரைத் தவறாகக் கூறாமல் கிடைத்த மாவட்டத்தைச் சொல்லும்.
- செயலி விவரத்தில் வெளியிடப்பட்ட தற்போதைய பதிப்புகளின் பொதுப் பட்டியல் காட்டப்படும். பழைய நிரந்தர எண்ணிக்கையும் காலாவதியான பதிப்பு இணைப்புகளும் முந்தைய களஞ்சிய இணைப்பும் நீக்கப்பட்டுள்ளன. வானொலி செயலி பட்டியலில் தொடரும்.
- ஒரே ஊரின் அடுத்த பகுதி விருப்பம், பயனர் தேர்ந்தெடுத்த நகர்வு தூரம், பத்து நிமிட ஊர் மறுஅறிவிப்பு தடுப்பு, அழைப்பின்போது காத்திருத்தல், இணையமின்றி ஊர் அறிவிப்பை நிறுத்துதல் மற்றும் சேமித்த அமைப்புகள் தொடரும். வட்டப் பயணத்தில் மீண்டும் வரும் ஊரைப் பயணம் முழுவதும் தடுக்காது.
- இது சோதனைப் பதிப்பு; வெளியிடப்படவில்லை. இந்தப் புதிய கோப்பு கைபேசியில் நிறுவிச் சோதிக்கப்படவில்லை. தானியங்கிச் சோதனை உண்மையான இருப்பிடச் சோதனையோ குரல் சோதனையோ பாதுகாப்பு ஒப்புதலோ அல்ல. தற்போதைய உரிமையாளர் கையொப்பப் பதிப்பிலிருந்து தரவை அழிக்காமல் புதுப்பிக்க வேண்டும். புதிய அனுமதிகள் இல்லை; வேறு பழைய கையொப்ப நிறுவலுக்கு நேரடிப் புதுப்பிப்பு சாத்தியமில்லை.
