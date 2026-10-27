# Elderly Launcher 1.3

## Home screen fixes

- Set default home uses Android's home-role request on supported versions, with a Home settings fallback and a permanent retry button. It is a system role, not a runtime permission.
- System Back returns from app lists, guides and About to home; it never finishes the home screen.
- Live battery percentage and charging/full state replace the empty battery label. Unavailable readings are explicitly unknown, not a fabricated zero.

## Permissions and help

- Favourite and emergency calls request phone permission when used. If denied, the number opens in the dialler. Contact selection requests contacts access when adding a favourite.
- Torch use requests camera permission, which Android requires on affected devices. This adds CAMERA to the manifest; it is not requested for opening the external camera app. Hardware remains optional.
- Removed unused notification permission: this launcher posts no notifications and must not ask for access it never uses.
- Tamil/English guide, About page, sister-app links and verified self-update control. The updater needs internet and Android's per-app installation confirmation; it never silently installs anything.

## Testing limits

- This exact build is **not installed on a phone** and has **no physical-device test** in this session. In particular, camera permission/torch, the home-role chooser, Back and battery updates need a phone test.
- Never call the emergency number for testing. Default-home selection and permission settings vary by manufacturer.

## தமிழ்

- முகப்புத் திரையாக அமை பொத்தான் ஆண்ட்ராய்டின் உண்மையான தேர்வைத் திறக்கும். ரத்து செய்தாலும் மீண்டும் முயலலாம்.
- பின்செல் செயலிப் பட்டியல், வழிகாட்டி, செயலி விவரப் பக்கங்களிலிருந்து முகப்புத் திரைக்குத் திரும்பும். முகப்புத் திரையை மூடாது.
- மின்கலச் சதவீதமும் மின்னேற்ற நிலையும் காட்டப்படும். தகவல் கிடைக்காவிட்டால் தெளிவாகச் சொல்லப்படும்.
- விருப்ப நபரையும் அவசர எண்ணையும் அழைக்கும்போது அழைப்பு அனுமதி கேட்கப்படும். மறுத்தால் எண் அழைப்புத் திரையில் திறக்கும். தொடர்பைச் சேர்க்கும்போது தொடர்புகள் அனுமதி கேட்கப்படும்.
- விளக்கை இயக்க கேமரா அனுமதி கேட்கப்படும். இது புதிதாகச் சேர்க்கப்பட்ட அனுமதி. அறிவிப்புகள் அனுப்பப்படாததால் அறிவிப்பு அனுமதி நீக்கப்பட்டது.
- தமிழ் மற்றும் ஆங்கில வழிகாட்டி, செயலி விவரம், மற்ற செயலிகளின் இணைப்புகள், சரிபார்க்கப்பட்ட புதுப்பிப்பு உள்ளன. நிறுவ உங்கள் ஒப்புதல் தேவை.
- **இந்தச் சரியான பதிப்பு தொலைபேசியில் நிறுவிச் சோதிக்கப்படவில்லை.** முகப்புத் திரைத் தேர்வு, அனுமதிகள், விளக்கு, பின்செல், மின்கல விவரங்கள் தொலைபேசியில் சோதிக்கப்பட வேண்டும். சோதனைக்காக அவசர எண்ணை அழைக்காதீர்கள்.
