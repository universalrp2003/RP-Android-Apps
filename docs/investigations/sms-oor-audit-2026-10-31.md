# SMS Guard / Oor audit while owner tests the frozen Live Guard2.81 artifact

Owner authorized a code audit/improvements, not publication. Do not change or replace the owner's exact LG2.81 test artifact (source08c33944/run41181427941/artifact13035561625). New shared directory **candidate-version** entries do not change public pointers or those retained bytes.

- Cache follow-through: ignore legacy-policy or24h-expired/future-dated SAFE entries, and invalidate an existing green entry when a rescan is pending. Otherwise the corrected scanner would still leave old green inbox badges visible. Keep stored threat warnings on failed rescans; no SMS/settings/bill records are deleted.

## Confirmed and corrected

SMS1.1.13/code19 (new candidate, not same-version replacement):
- An ordinary unreachable site could receive SAFE after DNS returned CLEAN; only unresolved *known shorteners* had a no-green guard. All non-success HTTP/incomplete redirects now stay pending.
- A configured Safe Browsing lookup returned Boolean? but only `== true` was checked, treating unavailable as clean. Null now stays pending. An explicit API error is also unavailable.
- `resolveRedirectsHeaderOnly` used Range GET and touched inputStream in finally. It now uses HEAD, never reads the response body and never falls back to GET. HTTP405 remains pending. Each redirect is locally screened before visiting it; unknown app-intent / limit-exhausted targets remain incomplete.
- Threat-DNS used raw-IP HTTPS with comments explicitly avoiding NextDNS filtering, then called unavailable/cooldown CLEAN. Use the provider's normal hostname, respect DNS filtering, and return pending for unavailable/invalid/non-answer checks. This may produce more pending results on filtered networks; never label that a clean check.
- UI/manual/incoming/reconnected speech no longer promises “safe to click”. Keep the existing stored SAFE enum for compatibility, but label completed checks “no threat detected; not a guarantee”. Disclose that URLs themselves may contain personal tokens and HEAD requests reveal URLs/IP/metadata. Optional Google checks send URLs to Google. No complete-SMS-body upload added.

Oor1.1.12/code17:
- DestinationResolver still included subAdminArea in its town fallback, despite fixing the separate journey reverse-geocoder. Remove that fallback; keep the person's query if no place feature/town is available.
- Destination district details were discarded when locality/district shared a name. Preserve explicit district role, merging only the redundant unlabelled locality.
- Reject finite-but-out-of-range coordinates as well as NaN/infinite coordinates before distance/arrival computation.

Regression additions: twelve SMS failure/success/offline/cache cases, three destination policy cases and four Python call-site/network-method/disclosure/cache contracts. Existing bill/route/history controls remain.

Removed blanket “Allow restricted settings” / “Unlock” setup advice. The setup banner and Controls now disclose the incoming-MMS limitation and advise against overriding Android security warnings. The App Info action remains native permission inspection, not a protection bypass.

## Not resolved / do not overclaim

1. **Google Play Protect:** owner screenshot shows “Harmful app blocked” for SMS1.1.12. Later About1.1.12 does not clear it. No exact installed-phone hash, Google diagnostic cause, Google review/appeal or clearance is available. Local reference APK hash remains aa074c13acd403fe747e8dd492db969a9b4f1efb700ab8723edac90ea4c8e5a0. Do not call these changes the cause/fix for Google's verdict; no bypass, key/package churn, permission disguising or publication.
2. **MMS:** MmsDeliverReceiver is an empty role-eligibility placeholder. It is not complete incoming MMS support; becoming the default SMS handler can leave MMS unhandled. This audit does not implement carrier MMS downloading/storage. SMS candidate must not be recommended as a replacement default messaging app pending that work and the security warning.
3. **Receiver time budget:** SMS persistence precedes online link checks, but goAsync processing can still exceed broadcast time limits for multiple slow URLs. A durable work/notification split needs a reviewed follow-up; this patch does not pretend it is solved.
4. **Scan privacy / network scope:** HEAD can trigger poorly implemented state-changing endpoints or expose personal URL tokens. Per-hop heuristics are not complete SSRF/private-network/DNS-rebinding isolation. No server body/JS execution occurs in the redirect probe, but this is not a sandbox or universal protection. No guarantee of malicious/safe classification or external-provider availability.
5. **Oor:** no physical destination/district crossing or route speech test of these new bytes. Existing logs marked SPOKEN are app callback evidence, not independent microphone recordings. Previous legitimate circular returns are preserved.

New candidate versions preserve signing identity, permissions, records/settings, manual reminders and browser-only update handoff. No new app dependencies, APK download/install code or clear-data migration. Public SMS1.1.11/Oor1.1.10 and LG2.79 remain untouched. CI execution results are recorded in PR72, separate from these source findings and owner phone acceptance.
