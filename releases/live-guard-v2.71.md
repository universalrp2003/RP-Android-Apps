## Live Guard (லைவ் கார்டு) v2.71 — HD Voice Only, Silent Place Profile, and a Stop-Voice Button on Every Screen

**Package:** `com.universalrp.cleansweep` · **Version:** `2.71` (`versionCode = 76`) · **Minimum Android:** Android 8.0+ (API 26)
**Published Signed Asset:** `CleanSweep-v2.71.apk` (signed and published by CI).
**Verification Note:** Built, linted, unit-tested, and verified via CI static / manifest / signature checks. Permission delta over `v2.70`: `0` new permissions added. This is a voice-output change only; no new permission is requested.

### Why this release
The owner asked for five things about Live Guard's voice, plus the usual build/publish pass:

1. Use the HD voice **only** — no fallback to the phone's built-in voice.
2. Stop all voice functions when the internet is disconnected.
3. A "silent place" profile for office and working people, with their own start/stop times and an on/off button.
4. The main-page volume slider must control **all** Live Guard voice output.
5. A visible stop button wherever an announcement is playing, so it can be stopped from any window.

### What's New in v2.71

<details>
<summary><b>1. HD voice only — the phone's built-in voice is gone (HD குரல் மட்டும்)</b></summary>

Live Guard now speaks with the Studio Ultra-HD neural voices and **nothing else**. The phone's own built-in text-to-speech voice, the system-voice picker card, the "Text-to-speech settings" shortcut, and the automatic fallback to an offline system voice have all been removed from Live Guard.

- When the HD voice cannot be produced yet (it is still loading, or the network is slow), the announcement is **retried and waits**, then spoken in HD. It is never switched to the phone's voice and never dropped silently.
- The old "Natural voice (online)" switch is replaced by a plain "HD voice only" statement, because there is no longer anything to switch between.
- **Nothing else changed:** Ramesh Radio, Oor Arivippu and the other apps keep their existing voice behaviour, and the shared neural-voice engine and weather runtime were not touched.

</details>

<details>
<summary><b>2. Internet disconnected = voice stops (இணையம் இல்லாதபோது குரல் நிற்கும்)</b></summary>

Because HD is the only voice, losing the internet means there is nothing left to speak with.

- Voice functions stop immediately when the network drops: a playing announcement is cut and the queue is cleared rather than left half-loaded.
- Requests submitted while offline are held and announced when the network returns, instead of being dropped.
- The voice diagnostics line now says *"Waiting: internet disconnected, HD voice will speak when it returns"* / *"காத்திருக்கிறது: இணையம் இல்லை, இணையம் திரும்பும்போது HD குரல் பேசும்"*, so the silence is explained rather than mysterious.

</details>

<details>
<summary><b>3. Silent place / office profile (அமைதி இடம்)</b></summary>

A new card on the Voice screen for office and working people: pick the hours when every voice function must be off, and switch the whole profile on or off with one button.

- **Multiple start/stop slots (ஒன்றுக்கு மேற்பட்ட நேரங்கள்):** add as many windows as you need — for example 09:00–13:00 and 14:00–18:00 — and remove any of them with the ✕ beside it. Up to 6 slots are kept.
- **On/off button (இயக்கு/அணை):** the profile has its own switch. Off means the times are ignored entirely.
- **Silences everything, not just routine alerts:** inside a slot nothing is spoken — not rain warnings, not battery, charging or storage alerts, not assistant answers. This is deliberately stronger than quiet hours: a stray "Test" tap cannot break it either, and switching the profile off is the way out.
- **Live status:** when the profile is on and the current hour falls inside one of your slots, the card says *"Silent place is active right now, so all voice is paused."* / *"அமைதி இடம் இப்போது செயலில் உள்ளது, எனவே எல்லா குரலும் இடைநிறுத்தப்பட்டுள்ளன."*
- The default slot is 09:00–18:00, a plain office day, and it starts switched **off** so nobody is surprised by it.

</details>

<details>
<summary><b>4. The main-page volume slider controls all voice (முதன்மை பக்க ஒலி அளவு)</b></summary>

The speaker button on the main page already opened the announcement-volume slider; that slider is now the single master control for every Live Guard voice function, because the HD playback path is the only path that exists.

- It is the same value everywhere — main page, Settings and the plan card all show the identical control and the identical Test sentence, and it is stored in one place on the phone.
- 0% mutes every spoken alert and answer; the panel says *"Voice alerts are muted"* / *"Voice alerts are muted"* so the state is never a guess.
- Android's own media volume is still separate, and a muted media stream still stays muted — the app never forces audio away from Bluetooth or overrides Do Not Disturb.

</details>

<details>
<summary><b>5. A stop-voice button on every screen (எல்லா திரையிலும் நிறுத்து பொத்தான்)</b></summary>

While an announcement is actually playing, a small **"Stop voice"** / *"குரலை நிறுத்து"* button appears at the top of every screen — main page, scanning, results, settings, weather, guide, anywhere.

- It is driven by the speech engine's own "playback started" and "finished" callbacks, so it is shown only when sound really is playing — never for a request that was merely queued.
- Tapping it stops the HD playback and drops the queue, so nothing more is spoken until the next request.
- The button is small, sits clear of the screen content, and needs no navigation to reach.

</details>

### Notes
- **No new permissions** — `0` added over `v2.70`.
- **Not changed:** Ramesh Radio, Oor Arivippu, SMS Guard, Secret Message, PulseEQ and RP Vocal Remover Studio behave exactly as before. The shared `SharedNeuralVoiceEngine` and `SharedWeatherRuntime` were not modified, so other apps' voice behaviour is untouched.
- **OTP codes are never spoken** by any path in this release.
