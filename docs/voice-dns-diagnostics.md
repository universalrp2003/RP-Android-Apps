# Voice service / NextDNS diagnostic checklist

Source audit (28 October 2026): both SMS Guard NeuralCloudVoiceEngine and shared SharedNeuralVoiceEngine use `speech.platform.bing.com` via secure WebSocket (WSS/TLS, port 443). This identifies the synthesis dependency, not proof of availability or a NextDNS verdict. DNS resolution alone does not prove TLS/WebSocket/audio success. HTTP 403 is not proof of DNS blocking; both engines already have a bounded server-date clock-skew retry.

This sandbox is restricted to approved development hosts and cannot faithfully test a friend's NextDNS profile or the speech endpoint. Do not attribute the earlier null-audio CI failure to NextDNS without evidence.

Owner/friend checks:
1. Record app version, network (Wi-Fi/mobile), NextDNS enabled state and exact time. Use harmless test text, not an OTP/private note.
2. In NextDNS profile Logs (if enabled), filter `speech.platform.bing.com` at the test timestamp. Check blocked/allowed status and rule/list reason. Do not share profile IDs, keys, private query history or full logs publicly.
3. If that exact domain is blocked, optionally allowlist only that exact hostname in the affected profile and retry the SAME test. Do not wildcard-allow bing.com/Microsoft or turn off protection permanently.
4. If allowed, still check TLS/WebSocket handshake and whether valid audio arrived; potential causes include network filtering, external service rejection, rate limits or timeouts. A successful DNS answer is not an audio success.
5. If comparison is needed, use a trusted alternate network while keeping the rest of the test unchanged. Restore any temporary DNS settings after the test. Cached DNS may delay changed policy effects.

Future diagnostic UI should distinguish DNS lookup failure, timeout, TLS failure, HTTP rejection and no audio, without logging note/SMS text, URLs containing tokens or credentials. UnknownHostException alone does not prove intentional blocking by NextDNS. Do not silently substitute robotic output in apps with an HD-only contract. Never announce OTP codes.

Status: endpoint source verified; actual NextDNS profile blocking and live synthesis not verified.
