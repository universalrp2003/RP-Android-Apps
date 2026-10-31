# SMS Guard 1.1.14 / code 20 — investigation candidate, NOT published

## English

These new candidate bytes are not installed on a phone. Build/test/signing results will be recorded separately; no physical-device acceptance or publication is claimed.

- Correct causal “due to” versus genuine bill due dates; retain prior false positives for review but suppress their automatic/manual reminder delivery. No SMS or manual reminders are deleted.
- Move incoming remote link checks out of broadcast processing into an OS-scheduled WorkManager queue. Persist extracted URLs, not complete SMS bodies; scan at most three remote URLs per pass, rotate incomplete checks fairly and retry with network constraints/backoff. No timing guarantee on every OEM.
- Keep incomplete/failed checks pending, invalidate stale green results, use HEAD-only redirect probes, respect device DNS filtering, and disclose URL/request metadata and optional Google lookups. No threat detected is not a safety guarantee.
- Do not offer new default-SMS role setup while incoming MMS is not implemented. Existing defaults must use the native switch-back action to choose their full messaging app; SMS receipt, package identity, signer and stored records are preserved. This is a companion-mode candidate, not a complete MMS messaging replacement.
- Play Protect “Harmful app blocked” for prior 1.1.12 remains UNRESOLVED. These code fixes do not establish its cause or Google clearance. Do not override protection, disable scanning or change keys/packages to evade a verdict. No appeal/review has been submitted by this agent.
- In-place update only after signing/build verification and owner approval. Do not uninstall or clear app storage: uninstalling may erase local data, reminders and settings. No new signing key or data-reset migration.

## தமிழ்

இது 1.1.14 / code20 சோதனை வேட்பாளர் மட்டுமே; பொதுவெளியில் வெளியிடப்படவில்லை.

- “due to” என்ற காரணச் சொல்லை கட்டணக் கடைசித் தேதியாகக் கருதும் பிழை சரிசெய்யப்பட்டது. பழைய தவறான நினைவூட்டல்கள் பரிசீலனைக்காக இருக்கும்; அவற்றைச் சொல்லாது. SMS அல்லது கையேடு நினைவூட்டல்கள் நீக்கப்படாது.
- இணைய இணைப்புச் சோதனைகள் SMS பெறும் செயலிலிருந்து பின்னணிப் பணிக்கு மாற்றப்பட்டன. ஒவ்வொரு சுற்றிலும் அதிகபட்சம் மூன்று இணைய முகவரிகள்; முடிக்காதவை பின்னர் மீண்டும் சோதிக்கப்படும். முழு SMS உரை பணித் தரவாகச் சேமிக்கப்படாது. இணைப்பில் தனிப்பட்ட குறியீடுகள் இருக்கலாம்; இணையத் தளங்களுக்கும் விருப்ப Google சோதனைக்கும் முகவரி/கோரிக்கைத் தகவல் செல்லலாம்.
- MMS பெறுதல் இன்னும் இல்லை; புதிய இயல்புநிலை SMS அமைப்பு வழங்கப்படாது. முழுமையான செய்திச் செயலியை இயல்புநிலையாக வைத்துத் துணைப் பயன்முறையைப் பயன்படுத்தவும்.
- முந்தைய 1.1.12 Google Play Protect தடை தீர்ந்ததாக உறுதி இல்லை. பாதுகாப்பை முடக்கவோ எச்சரிக்கையை மீறவோ வேண்டாம்.
- அதே கையொப்பத்துடன் புதுப்பிக்க வேண்டும். நிறுவல் நீக்கம் அல்லது தரவு அழித்தல் வேண்டாம்; உள்ளூர் தரவு அழியலாம். பயண/செய்தி/நினைவூட்டல் பதிவுகள் பாதுகாக்கப்படும்.
