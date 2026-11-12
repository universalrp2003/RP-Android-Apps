# Voice Notes improvements

## Owner request

The owner wants future enhancements, specifically retaining the HD neural playback voice and not replacing it with robotic system TTS.

## Changes in this candidate

- Added in-list text search and an edit action; edited notes keep their ID and date, and existing NoteStore edit tests cover trimming and blank edits.
- Added a first-use disclosure and explicit Continue / Not now choice before neural playback. Only the selected note text is submitted when the owner requests HD playback. The HD neural voice remains the only note playback path; if it is unavailable, the app reports that instead of switching to robot TTS.
- Clarified that voice dictation is handled by the device's installed speech-recognition service; the app cannot promise that service processes audio locally.
- Replaced silent direct note-file writes with a synced temporary-file write and atomic replacement. Save failures now show an error rather than silently pretending the note persisted.
- Prevented overlapping neural voice requests and ensured exceptions clear the in-progress indicator.
- Corrected the Voice Notes title language selection.

No app version bump, stable publication, or APK claim is made here. A Kotlin/Android CI build and tests are required before sending this candidate to a device. The current working branch also contains the separately completed SMS Guard inbox fix; the friend's on-device result is still pending.
