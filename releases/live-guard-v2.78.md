# Live Guard 2.78 — balanced, source-grounded news (candidate)

## English

- Up to eight new stories: three Tamil Nadu, one India, one world, one science/invention, one cyber/mobile safety and one environment/nature. Missing categories stay empty: no stale stories or unrelated filler to reach eight.
- Reject hourly headline programmes, pipe-separated topic bundles, live blogs, galleries, obvious shopping promotions, question-only teasers and truncated headlines. These checks also protect the news cards and their individual Read aloud action.
- Keep today's real publication timestamps, reject future/missing dates, recheck after AI editing, and cancel a bulletin if its local day/timezone changes during audio preparation or playback.
- Do not replay already-read news on repeated manual taps. Keep a bounded seven-day history of successfully read event titles to reduce republished/near-duplicate reports. Event matching and topic classification are conservative heuristics, not a guarantee of perfect factual verification.
- Rotate preferred science, cyber-safety and environment subtopics only after a complete successful read. Preserve the working 2.77 prepare-all HD playback and Stop cancellation.
- Safe AI editing uses the owner's configured non-keyless AI provider, when ready, with a 12-second bound and no web search. Only public feed evidence is sent, never phone reports or API keys in the news payload. AI may reject items or choose an exact publisher-provided sentence as an explanation; it cannot invent, translate, reorder, merge or replace the full headline. Failure falls back only to locally validated headlines. A valid AI rejection stays rejected.
- Tamil specialist collection uses Tamil news searches and Tamil technology feeds; English mode additionally tries NASA, The Hindu Science, CISA, BleepingComputer, Mongabay India and BBC Science & Environment. Feed availability and useful same-day coverage are not guaranteed. No English-to-Tamil AI translation in this conservative candidate.
- No new Android permissions, signing migration or data reset.

**Installation:** update the existing 2.77 / 2.76 / 2.75 / 2.74 app in place with the signed release APK. Do not uninstall or clear app data. Builds older than the 2.74 signing transition may require the previously documented migration; back up/export needed local data before uninstalling an old-key build; uninstalling may erase local data.

**Status:** unpublished candidate; not installed on a phone; no physical-device test. Public release remains 2.75. Automated tests do not prove source availability, perfect classification or successful playback/scheduling on a phone.

## தமிழ்

- அதிகபட்சம் எட்டு புதிய செய்திகள்: தமிழ்நாடு 3, இந்தியா 1, உலகம் 1, அறிவியல் 1, இணைய/கைப்பேசி பாதுகாப்பு 1, சுற்றுச்சூழல்/இயற்கை 1. ஒரு பிரிவில் புதிய செய்தி இல்லாவிட்டால் பழைய செய்தியால் நிரப்பப்படாது.
- மணிநேரத் தலைப்புச் செய்தித் தொகுப்புகள், விளம்பரங்கள், முடிவற்ற தலைப்புகள் மற்றும் ஒரே நிகழ்வின் மறுபதிப்புகள் வடிகட்டப்படும். இன்றைய உண்மையான வெளியீட்டு நேரம் மட்டுமே ஏற்கப்படும்.
- முழுத் தொகுப்பும் வெற்றிகரமாகப் படிக்கப்பட்ட பிறகே படித்ததாகக் குறிக்கப்படும்; அறிவியல், பாதுகாப்பு, இயற்கை உட்பிரிவு முன்னுரிமைகள் அப்போது மட்டுமே சுழலும்.
- அமைக்கப்பட்ட AI சேவை இருந்தால் ஆதாரத்தில் உள்ள வாக்கியத்தைத் தேர்ந்தெடுக்கவோ பொருத்தமற்ற செய்தியை நிராகரிக்கவோ மட்டுமே உதவும். புதிய தகவலை உருவாக்காது; செய்தித் தலைப்பைச் சுருக்கி நீக்காது. AI செயலிழந்தாலும் உள்ளூரில் சரிபார்க்கப்பட்ட தலைப்புகள் மட்டுமே படிக்கப்படும்.
- இருக்கும் செயலியைப் புதுப்பிக்கவும். செயலியை நீக்கவோ தரவுகளை அழிக்கவோ வேண்டாம். கைப்பேசியில் இன்னும் சோதிக்கப்படாத வெளியிடப்படாத சோதனைப் பதிப்பு.
