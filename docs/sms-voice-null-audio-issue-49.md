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

## Measured on 4 November 2026 (the first run with the new diagnostics)

Probe run `41801643886` (branch `arena/09d22d90-rp-android-apps`), synthetic text only:

| Profile | Kotlin client | First audio | Total | Independent `edge-tts` reference |
| --- | --- | --- | --- | --- |
| `ta-MY-SuryaNeural` | 26,640 B | **1,590 ms** | **3,133 ms** | 26,640 B, 2,641 ms |
| `ta-IN-PallaviNeural` | 25,920 B | 131 ms | 204 ms | 25,776 B, 298 ms |
| `en-IN-PrabhatNeural` | 30,672 B | 1,552 ms | 2,494 ms | 23,328 B, 1,612 ms |
| `en-IN-NeerjaNeural` | 24,624 B | 314 ms | 498 ms | 24,624 B, 256 ms |

Advertised `ta-IN` catalogue at the same moment: `PallaviNeural` (Female) and `ValluvarNeural`
(Male) — Valluvar is still advertised upstream, which is why the client-side remap matters and why
the probe re-checks this every run.

**The failover lane shows the actual failure mechanism** (`HD voice recovery` run `41801643897`, same
push). Three distinct Tamil voices share one 4,200 ms deadline, 1,400 ms each:

```
HD_VOICE_ATTEMPT=ta-MY-SuryaNeural  | OPEN HTTP=101 | DEADLINE_EXCEEDED | COMPLETE=false BYTES=0 FRAMES=0 ELAPSED_MS=1401
HD_VOICE_ATTEMPT=ta-SG-AnbuNeural   | OPEN HTTP=101 | DEADLINE_EXCEEDED | COMPLETE=false BYTES=0 FRAMES=0 ELAPSED_MS=1392
HD_VOICE_ATTEMPT=ta-IN-PallaviNeural| OPEN HTTP=101 | FIRST_AUDIO_MS=265 | COMPLETE=true BYTES=23328 FRAMES=33 ELAPSED_MS=359
HD_VOICE_SELECTED=ta-IN-PallaviNeural
```

Both first attempts **connected successfully (`HTTP=101`) and then produced zero audio inside their
share**, while the same Surya voice needs ~1.6 s to first audio and ~3.1 s to finish on an idle
probe. So the intermittent null is not "the service is down": the equal-share budget is smaller than
the service's own time-to-first-audio for the slower voices, and success then depends on a later,
faster voice answering inside its share. When none does, the call returns null — with no cause
recorded, which is exactly what the issue described.

This also explains why the same source passed and failed: it is a race between a ~1.4 s per-voice
share and a 0.13–3.1 s time-to-first-audio distribution, not a change in behaviour.

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

## Resolution (chosen: budget shaping)

The owner chose remedy 1. `NeuralBudgetPolicy` now divides one deadline as **55 % / 30 % / 15 %**
(of what is left, so unused time is handed forward) instead of equal thirds, and
`NeuralVoiceFailover.synthesize` uses it. Nothing else changed: same candidate list, same order,
same total deadline, same "≥ 256 bytes and a clean turn.end" success condition, same ban on
same-voice retries, and patient callers that pass `attemptTimeoutMs` (Live Guard news preparation,
Voice Notes) keep their per-attempt cap exactly as before.

With the failover deadline unchanged at 4,200 ms the first voice now gets ~2,310 ms instead of
1,400 ms — comfortably more than the 1,590 ms first-audio time measured for `ta-MY-SuryaNeural` —
while the second and third voices still get 1,260 ms and 630 ms of whatever remains. ### Measured after the change

Live probe `41805403494` (preferred voice still resolves to `ta-MY-SuryaNeural`): 4/4 profiles
`class=OK`, independent `edge-tts==7.3.1` reference 4/4 PASS. HD recovery `41805403597` printed a
different result from the equal-thirds trace in this document:

```
HD_VOICE_ATTEMPT=ta-MY-SuryaNeural budgetMs=2310 | OPEN HTTP=101 | FIRST_AUDIO_MS=1308 |
  DEADLINE_EXCEEDED | COMPLETE=false BYTES=10080 FRAMES=14 ELAPSED_MS=2311 | NO_TURN_END ...
HD_VOICE_ATTEMPT=ta-SG-AnbuNeural  budgetMs=1247 | OPEN HTTP=101 | FIRST_AUDIO_MS=734 |
  COMPLETE=true BYTES=21888 FRAMES=31 ELAPSED_MS=1222 | HD_VOICE_SELECTED=ta-SG-AnbuNeural
```

The preferred voice is now actually attempted with 2,310 ms and produces audio (it is still slower
than the deadline: its own measured total is ~3.1 s, so the remainder is honestly reported as
`DEADLINE_EXCEEDED`), and the *second* voice completes with its 1,247 ms share. Before the change
the call survived only because the third, fastest voice answered in 359 ms; now the call no longer
depends on reaching the last candidate. The shared engine run in the same workflow recovered on the
preferred voice (`SHARED_HD_RECOVERY bytes=24192 selected=ta-MY-SuryaNeural`).

Honest limits: the preferred voice did not itself reach `turn.end` inside 2,310 ms on this run, so
this is a demonstrated improvement in headroom (attempt 1 audio, attempt 2 success), not a claim that
`ta-MY-SuryaNeural` always completes. Speech remains bounded by the same 4,200 ms deadline and the
same "complete MP3, > 1,500 bytes" requirement.

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
- **The measured cause is budget shape, not reachability.** Equal shares (4,200 / 3 = 1,400 ms) are
  shorter than the slower voices' time-to-first-audio (~1.6 s), so the first two attempts are thrown
  away on a healthy connection and success depends on a faster third voice. Two remedies exist and
  both are product decisions:
  1. **Budget shaping** — give the first voice most of the deadline (e.g. 55 % / 30 % / 15 %, total
     still 4,200 ms) instead of equal shares. `NeuralVoiceFailover.synthesize` already supports a
     caller-chosen `attemptTimeoutMs` for patient callers, so this needs no new policy — only a
     decision about SMS speech latency.
  2. **Same-voice retry on 1011** — wire `NeuralRetryPolicy` and update the pinned contract in
     `tools/tests/test_hd_voice_policy.py`. This is the change that contract was written to prevent,
     so it needs an explicit owner decision.
- Either way, this investigation changes nothing about success criteria: a profile still needs more
  than 1,500 real MP3 bytes from the service.
