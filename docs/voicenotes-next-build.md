> **Update-flow status superseded (29 October 2026):** Voice Notes now uses the shared once-daily foreground metadata check and official website/browser handoff. The older self-download/update requirements below are historical and no longer apply.

# Voice Notes — owner-requested next-build follow-up

Requested 28 October 2026 alongside the owner screenshot upload:

- Include a compact About page/section.
- Include Tamil/English usage Guide.
- Include links to our other apps and official website.
- Include these together in the next Voice Notes build; do not publish a separate release merely for screenshot collection.
- Preserve existing self-download/update functionality, HD neural voice behavior (no robotic fallback), privacy disclosures and localization/release checks.

Status: implemented in source after screenshot import; Android CI and device verification pending.

## Optional biometric unlock

Owner additionally requested optional biometric unlocking for Voice Notes.

- Provide a clearly labelled Tamil/English opt-in setting, off by default.
- Use Android's system biometric prompt with device screen-lock credential fallback where supported.
- Handle unavailable/not-enrolled biometrics and cancellation without exposing locked notes or trapping the owner out of settings/data.
- When enabled, protect note viewing/editing and playback entry points; review background/foreground and process-restart behavior.
- Do not describe a UI authentication gate as encryption or cryptographic biometric binding unless that is actually implemented and verified.
- Preserve existing notes and existing HD neural voice/self-update behavior.

Status: opt-in Android prompt gate implemented in source; Android CI and device testing pending. Lock defaults off; enabling/disabling requires authentication, leaving the app relocks and interrupts playback. This is not note-file encryption.
