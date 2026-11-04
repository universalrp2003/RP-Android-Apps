# Issue #49 — intermittent null audio in the live SMS Guard neural-voice probe

Status: **investigated; evidence improved, root cause is upstream and not fixable client-side
without changing a pinned policy.** Nothing was weakened, skipped or re-pointed.

## What was reported

`SmsGuardEngineTest.liveOnlineNeuralStreamingSynthesizesRealMp3WhenNetworkAvailable` is a live probe:
it asks the Edge neural-voice service for four synthetic profiles and requires real MP3 bytes
(> 1500) from every one. The issue records the **same source** passing in run `40473392111` and
failing in `40474062135` and `40474677451`, with `ta-IN-ValluvarNeural` returning null audio, while
Oor Arivippu's separate live lane passed in the same window.

## What the code actually does today

| Fact | Where |
| --- | --- |
| `ta-IN-ValluvarNeural` is **disabled**: a request for it is remapped to `ta-MY-SuryaNeural`, and no new request is ever sent to Valluvar. | `shared/src/main/java/com/universalrp/voice/NeuralVoiceFailover.kt` |
| The live probe requests Surya, Pallavi, Prabhat and Neerja with `allowVoiceFallback = false`, so it is a **direct** single-voice probe — no remap can hide a failure. | `smsguard/.../SmsGuardEngineTest.kt` |
| A same-voice retry is **deliberately forbidden** by a pinned contract ("one shared policy without recursive same-voice retry"), and `NeuralRetryPolicy.remainingBudget` must not be wired into either engine. | `tools/tests/test_hd_voice_policy.py` |
| Production already fails over to *distinct* voices inside one deadline (max three), and never retries the interrupted request. | `NeuralVoiceFailover.synthesize` |

So the Valluvar null in the issue predates the current code: the voice it names is no longer
requested at all, and the probe that remains is stricter (direct, no fallback).

## Why the failure was intermittent

The Kotlin client is byte-for-byte the same in the passing and failing runs, so the difference was
on the service side. The transport fails in exactly four ways, all of which surfaced as a plain
`null` before this change:

1. **Peer close 1011** (service-side "internal error" under load) — connection opens, audio may be
   partial, `turn.end` never arrives.
2. **HTTP 403** — the throttle answer; the client already reads the `Date` header to correct clock
   skew, so a 403 caused by a skewed token also lands here.
3. **HTTP 5xx / socket timeout** — runner egress or service hiccup.
4. **Close (1000/1005) before `turn.end`** — a truncated turn.

`NeuralRetryPolicy` encodes "retry 1011 once inside the caller's deadline", but it is **unit-tested
and never called**: the pinned HD-voice contract above forbids wiring it. That is a deliberate
product decision, not an oversight — retrying the same voice was rejected in favour of trying a
different voice.

## What this change adds (evidence, not a weaker test)

`NeuralCloudVoiceEngine.streamWebSocketOnce` now reports, without disclosing text, URLs, headers or
exception messages:

- `FIRST_AUDIO_MS=` — how long the service took to produce the first audio frame;
- `COMPLETE=… BYTES=… FRAMES=… ELAPSED_MS=` — whether the turn ended cleanly and how much arrived;
- `NO_TURN_END CLOSE=<code> BYTES=… ELAPSED_MS=…` — an explicit cause when the turn does not end,
  instead of a silent `null`.

`SmsGuardEngineTest` prints a classified line per profile:

```
SMS_LIVE_PROFILE ta-IN-PallaviNeural class=OK bytes=18234 diagnostics=…
SMS_LIVE_PROFILE ta-MY-SuryaNeural class=PEER_1011 bytes=0 diagnostics=… | PEER_CLOSING CODE=1011
SMS_LIVE_PROFILE en-IN-PrabhatNeural class=HTTP_403_THROTTLE bytes=0 diagnostics=… | WEBSOCKET_FAILURE HTTP=403
```

Classes: `OK`, `DNS_FAILURE`, `TLS_FAILURE`, `HTTP_403_THROTTLE`, `HTTP_429_THROTTLE`, `HTTP_5XX`,
`SOCKET_TIMEOUT`, `DEADLINE_EXCEEDED`, `PEER_1011`, `PEER_CLOSED_EARLY`, `NO_TURN_END`,
`WEBSOCKET_FAILURE`, `UNKNOWN`.

The assertion itself is untouched: every required profile still needs more than 1500 real MP3 bytes,
and the probe is still direct (no fallback, no DNS-only success).

## How to investigate the next occurrence

`.github/workflows/sms-live-probe.yml` runs on a branch push whose commit message contains
`[check-live-voice]` (manual dispatch is denied to the current integration). In one runner it:

1. runs the real Kotlin probe;
2. runs the pinned independent reference client (`edge-tts==7.3.1`) against the same four profiles —
   if the reference also fails, the service is at fault; if only our client fails, the bug is ours;
3. records the **advertised** `ta-IN` voices, which is how the disabled-Valluvar decision is re-checked
   against the live catalogue;
4. publishes every line above as a check annotation.

Compare the two clients' classes in the same run: `PEER_1011`/`HTTP_403` on both means throttling or
an outage; a Kotlin-only failure means a protocol/implementation problem on our side.

## Honest conclusion

- The reported null was an upstream transient; the client has no deterministic way to make an
  external service answer, and the repository's pinned policy forbids the one retry that could
  hide it.
- This change makes the next occurrence self-diagnosing in CI. It does not claim the service is
  healthy, that any phone hears audio, or that Valluvar is permanently gone — the probe re-checks
  the advertised catalogue every time it runs.
- If the owner wants the transient absorbed instead of reported, the decision is a one-line policy
  change (`NeuralRetryPolicy` wired in for close code 1011) plus an update to the pinned contract
  in `tools/tests/test_hd_voice_policy.py`. That is a behaviour change for every caller, so it is
  left as an explicit choice rather than made silently.
