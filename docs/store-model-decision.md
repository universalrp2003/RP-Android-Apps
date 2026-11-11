# Store model: unchanged (new Tamilan Store APK per new app)

**Decision, 11 November 2026, from the owner:** keep the existing Tamilan Store model. When a new
application is published, a new Tamilan Store APK is released with it. No web-only store replaces it.

This settles the question raised while building TV Guard 1.5.1. The owner's reasoning is simply that
the current method works and a new store build is not a problem.

## What this means in practice

- **Tamilan Store stays an app.** Its release flow, signing, and website listing are unchanged. A new
  app means a new Tamilan Store version and a new store APK, as before.
- **The official website stays a catalogue.** `https://universalrp2003.github.io/RP-Android-Apps/`
  continues to list every app with verified SHA-256 downloads. It is not an installer and never
  installs anything silently; Android confirms each APK.
- **TV Guard's hub card is only a shortcut.** It opens the website from the phone that is already
  paired to the TV. It does not replace Tamilan Store and does not change how installs happen — the
  APK is still opened from the hub's own file list and confirmed by Android.

## What was explicitly not done

- No hosted-store rewrite, no removal of Tamilan Store from the site, `SisterApps.kt`, or the
  fourteen-app website contract.
- No change to Tamilan Store's version, tag, signing identity, or published bytes.
