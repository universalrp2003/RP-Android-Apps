# SMS Guard 1.1.17 — steadier online voice recovery

- Gives the preferred online voice more time inside the same bounded deadline: three-voice shares are now 55/30/15 instead of equal thirds.
- Keeps distinct-voice fallback, explicit online consent and clean, complete audio requirements. No same-voice retry or longer total deadline.
- Adds bounded upstream timing/failure diagnostics for synthetic CI probes. Online service availability is not guaranteed.

Normal same-key update from 1.1.16: do not uninstall or clear app data. Existing messages, default-SMS role and preferences should remain. No new permissions. SMS-only default mode remains explicit; MMS is not implemented.

This release is not a physical-phone, carrier/MMS, audible-playback or Google Play Protect certification. Historical legacy-key installations cannot update across signing identities; do not erase data as a workaround.
