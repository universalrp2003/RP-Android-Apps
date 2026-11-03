# Oor family-town relay — separate service, candidate only

Owner explicitly selected town/area-only family sharing and encrypted latest town retention (3 November 2026). Reuse the **hosting approach**, not Secret Message's running service, sessions, native library, keys or namespaces. No existing Secret Message files are changed.

## Protocol / consent

- Random128-bit pair ID; independent256-bit host, one-time invitation and newly generated guest capabilities. Authorization header only, no tokens in HTTP query strings. The AES256 sharing key travels only in the private invitation, never to this server.
- Invitation expires after10minutes. Claim is single-phone, idempotent for that same guest. The inviter and invitee separately compare a code binding the pair, encryption key and guest nonce, in person/on a trusted call; both explicitly confirm before the server allows sharing. Neither confirmation enables location publication.
- Each direction requires separate1/4/8hour consent. Generation changes invalidate old in-flight publish requests. Either direction can stop; either party can revoke the entire pair. Local OFF is durable before stop requests. Offline/network failure can delay remote removal until ciphertext expires, at most15minutes after the observation.
- Only latest AES-GCM envelope per direction. AAD binds pair ID, role and sample time. Server clamps expiry to observation+15minutes or consent expiry, rejects stale/future/replayed observations, oversized/extra fields, unknown roles, missing/expired grants. Both clients independently reject expired/tampered/wrong-role envelopes. No route history or latest-town plaintext stored server-side.
- Application alarms delete expired ciphertext. Pair metadata/auth hashes expire after30days; revocation drops credentials and ciphertext, retaining only a random-ID tombstone/expiry against re-creation until cleanup. Cloudflare platform backup/network/account metadata policies are separate; no claim to erase all provider infrastructure traces.
- IP-based create/request limits plus per-capability limits. No application logs/traces, CORS access, analytics or contact discovery. Provider sees IPs, pair IDs and traffic timing/volume. An invitation leak or a compromised authorized phone is a real risk. No independent security audit.

## Verification / deployment boundary

`npm ci && npm test` runs deterministic server-policy tests. `npx wrangler dev --local --port8787` plus `node test/http-smoke.mjs http://127.0.0.1:8787` exercises two synthetic devices through actual HTTP, encryption, approval, consent, stop and revocation. Never use private family data for tests. No actual GPS/provider/OEM guarantee.

Deployment must be separate from Secret Message, only after CI. Existing account credentials remain in GitHub secrets, never chat/source. The worker's public URL is not a credential. Android defaults to **unconfigured/unavailable**, and accepts only a build-time canonical `https://oor-family-relay.<subdomain>.workers.dev` endpoint after live synthetic service verification. No real sharing or location upload occurs with the default empty URL. Cloudflare free-plan availability/quotas are not a promise of unlimited free usage; do not change subscriptions or enable paid resources.
