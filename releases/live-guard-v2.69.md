# Live Guard (CleanSweep) 2.69

> ⚠️ This exact build was **not installed on a phone** by the developer. It was compiled and
> verified by CI only. Please test on your own device before relying on it.

## 🎙️ Announcement voice — clearer and smarter

- **Random mode now changes the voice every time.** Each announcement uses a *different*
  Ultra-HD voice — this one male, the next female, then the next male, and so on — stepping to
  the next voice in that gender's pool, so you never hear the same voice twice in a row.
- **Fixed "male only" / "female only" keep one steady voice**, so the repeated everyday alerts
  (battery, charging, storage, town) replay **instantly** from the on-device HD cache instead of
  being re-generated each time.
- **HD voice is preferred, always.** Online it synthesises (and waits a little longer for a clean
  start). **Offline it replays an already-cached HD clip** and only falls back to your phone's
  built-in voice when that exact line was never cached — so announcements stay natural even with
  no internet.
- **Removed the manual "Voice A / Voice B" system-voice picker.** The built-in voice isn't as
  good, so answers now use the Ultra-HD voice automatically and fall back to the phone's voice
  only when needed.

## 🔤 Tamil text now renders correctly on every phone

- Live Guard now **ships the Noto Sans Tamil font** (Regular + Bold) and uses it for Tamil.
  Some phones' default fonts mis-shaped Tamil (the AI vowel sign looked like ன and ண/ன collided),
  which made correct text look garbled. Tamil now looks the same and correct on all devices.
  English keeps the normal system font.

## 📶 Wi-Fi announcement fixed

- Reconnecting to Wi-Fi now speaks **"நீங்கள் மீண்டும் வைஃபையில் உள்ளீர்கள்"** properly. Previously
  the Latin "Wi-Fi" word was silently dropped by the Tamil voice, leaving a broken sentence.

## 🔒 Unchanged promises

- 100% open source. **No ads, no tracking, no analytics, no account.**
- Core Live Guard still does **not** read other apps' notifications or messages, and requests no
  notification-listener, SMS, Contacts, call-log, Accessibility or device-admin permission.
- Verified in-app updater checks URL, size, SHA-256 and app signer before installing.
