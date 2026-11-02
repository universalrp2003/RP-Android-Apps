# Live Guard 2.86 — private, narrow follow-up

## Owner acceptance and remaining scope

On2November2026 the owner reported the2.85 HD warning gone, no Android unresponsive/force-close dialog, and other features fine. Screenshots show actual GDACS, IMD and Tomorrow pages rendering in compact/expanded windows on that phone. This is limited owner acceptance, not universal ANR freedom, audible-background delivery, precise weather/map accuracy or independently measured installed hash. No raw screenshots, private home coordinates or extracted owner headlines are published here. Do not ask to repeat accepted physical tests without evidence of regression.

Remaining requests: editorial '-' pronounced as minus in news; Tomorrow public website selects a different town from the app label; optionally auto-focus cyclone map only if genuinely supported. The owner privately clarified which same-named town is intended. No authority to change the saved home or publish a new public APK.

## Changes

- `NewsSpeechSanitizer.forSpeech`: a separate speech-only final transform. Tamil numeric suffixes join their number; hyphenated word compounds use spaces; clear editorial separators use pauses. Original display/source/AI evidence/story keys stay unchanged. Negative signs, subtraction, numeric ranges and ISO dates remain untouched rather than guessing semantics. Both local and edited spoken text go through this transform in bulletin text, checkpoint plans, and individual news Read aloud.
- `DisasterWebPolicy.tomorrowForPlace`: a small **explicitly limited verified public-place directory**, not a geocoder. Currently one verified Tomorrow directory entry; town aliases AND public-town geographic area must match. Unknown, missing, invalid and similarly named/distant places return the existing country page, now explicitly labelled as not linked to the app place. No saved-home mutation or coordinates/key appended to website URLs. The shared app directory candidate version advances; publishedVersion stays2.82.
- Every viewer entry point—including switching from GDACS/IMD to Tomorrow—uses the same derived starting page. Header says **App place** rather than implying the provider's currently browsed location matches it. User navigation still remains user-controlled, not force-recentered while inspecting another place.
- GDACS actual event report/map retained. No documented automatic geographic follow/deep-link contract was established; do not inject private map variables, reconstruct tracks, fake a live cyclone position or auto-pan away from a user's inspection. Owner explicitly accepts leaving this manual if unsupported. Existing report URLs omit episode IDs, leaving latest available report selection to the provider, not claiming real-time observation.

## Public references checked

- https://weather.tomorrow.io/IN/TN/ — provider directory lists Ālangudi with public ID134455.
- https://weather.tomorrow.io/IN/TN/%C4%80langudi/134455/ — heading Ālangudi, nearby Pudukkottai/Arimalam/Karambakkudi, not Lalgudi.
- https://weather.tomorrow.io/IN/TN/%C4%80langudi/134455/radar/ — actual town-specific public radar page and provider-owned attribution. Page naming does not verify observed radar coverage or app minute-API entitlement.
- https://www.gdacs.org/report.aspx?eventtype=TC&eventid=1001346 — actual dated report and map/layers. No verified external geographic zoom/follow API found. This example event is research, not hard-coded into the app.

The public town's coarse disambiguation area is not an owner's actual location. Merely renaming a different saved coordinate does not cause a wrong-town website link: the geographic check must also pass. Such a mismatch must be corrected by the owner in the existing place chooser, never by silently replacing stored data.

## Verification status

Prepared66required JVM suites/626test declarations; source checks and actual executed CI evidence are separate. New tests cover Tamil suffixes/English compounds/editorial pauses, real negative values/subtraction/dates/ranges/units, edited and checkpoint paths, idempotence/source preservation; verified town link, same-name disambiguation, invalid/missing place fallback and no home mutation. Prior2.85 thread/cancellation/HD and safety regressions remain in the gate.

No local Android compiler. Exact signed release build, lint, executed regressions, signer/full merged permissions, normal preserved-data update and native UI checks must pass before delivery. Targeted owner checks after delivery: pronunciation of an editorial hyphenated headline and the website's actual town heading. No new publication, reinstall/reset, public location/voice upload or phone/Play Protect clearance claim.
