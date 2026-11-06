# Oor Arivippu1.1.18/code23 — isolated candidate, NOT published

This candidate is not installed on a phone; no physical-device test of these changes has occurred.

Administrative-area fallback is removed from town resolution. Prefer a finer provider-supplied locality, but do not claim the provider can always distinguish a taluk from a town. GNSS diagnostics are opt-in and foreground-only during a user-started trip, using existing precise-location permission. Show reported/used-in-receiver-fix counts, constellation and bounded signal details; stale/unavailable is not zero. These are not exact satellite attribution for fused location or destination distance.

Compact family summaries retain actual upload/waiting/stop-pending status, and full controls remain under the family button. History avoids duplicate title/message text; redundant navigation inset is removed. Destination milestones require a recent, finite, accurate fix. Speech queue admission is bounded, the worker is shut down with the service, and asynchronous geocoding has a timeout. These are defensive changes, not proof of the cause or cure of a whole-phone reboot. A local Android app-exit summary is available on supported Android versions; nothing is uploaded.

Do not uninstall or clear app data; same-key update/data preservation must pass CI before supplying a signed candidate. Uninstalling may erase local data. No new permissions, no background satellite polling or identifier uploads. The public download stays1.1.17 until explicit authorization after testing. Keep Play Protect enabled and stop if warned; automated/emulator checks are not Google or all-OEM clearance.

## தமிழ்

இது1.1.18/எண்23 சோதனைப் பதிப்பு; பொதுவாக வெளியிடப்படவில்லை. மாவட்டத்தை ஊராகப் பயன்படுத்தும் மாற்றுவழி நீக்கப்படுகிறது; கிடைத்தால் நுணுக்கமான உள்ளூர்ப் பெயருக்கு முன்னுரிமை. வழங்குநர் தரும் பெயர் தாலுகாவா ஊரா என்பதை எப்போதும் உறுதி செய்ய இயலாது.

நீங்கள் திறக்கும் விவரப் பகுதியில் பயணம் இயங்கும்போது மட்டும் செயற்கைக்கோள் எண்ணிக்கை, GNSS பெறுநர் பயன்படுத்தியவை, வகை மற்றும் சமிக்ஞை காட்டப்படும். பழைய / கிடைக்காத எண்ணிக்கை பூஜ்ஜியமாகக் காட்டப்படாது. இது கலப்பு இருப்பிடம் அல்லது தொலைவிற்கான துல்லிய செயற்கைக்கோள் பங்கீடு அல்ல. புதிய அனுமதி அல்லது கூடுதல் இருப்பிடக் கோரிக்கை இல்லை; தகவல் பதிவேற்றப்படாது.

குடும்ப அட்டை, வரலாற்றில் தேவையற்ற இடைவெளியும் ஒரே உரை இருமுறை காட்டுவதும் குறைக்கப்படுகின்றன. இலக்கு அறிவிப்புக்கு புதிய, துல்லிய இருப்பிடம் தேவை. குரல் பணிகள் வரம்பிடப்பட்டு செயலி சேவை முடிவில் விடுவிக்கப்படும். இவை கைப்பேசி மறுதொடக்கத்தின் காரணம் அல்லது தீர்விற்கான உறுதி அல்ல. செயலியை நீக்கவோ தரவை அழிக்கவோ வேண்டாம்; நீக்கினால் தரவு அழியலாம். அதே கையொப்பப் புதுப்பிப்புச் சோதனை தேவை. உரிமையாளர் ஒப்புதல் வரை பொதுப் பதிப்பு1.1.17 மாறாது.
