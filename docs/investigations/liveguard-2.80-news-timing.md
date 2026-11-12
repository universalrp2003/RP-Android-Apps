# Live Guard sleeping news regression — 31 October 2026

## Evidence and limits

Owner reports automatic news worked in 2.75 and is unreliable in 2.76–2.79 on three family phones; opening Live Guard and turning the screen off sometimes triggers it. Two screenshots show news enabled/30 minutes and a prepared Science/Nature selection. They do not prove a completed audio read or the scheduler's run times.

Compared `live-guard-v2.75` with baseline main `1d6d5aa2a45ed6de4184881adeed226774cf19ab`. This investigation modifies only Live Guard and its verification/documentation; no new public release is authorized or created here. Other app binaries and public pointers remain unchanged.

## Verified code paths

1. **Cross-trigger cooldown versus an independently queued periodic check.** 2.75 marks the last news time in `onStarted`; 2.79 correctly marks only after the full bulletin finishes. A screen-off/manual read can move that timestamp past a periodic check that was already queued independently: for example, screen-off read starts 8:00 and completes 8:06; an existing 8:30 periodic check sees only 24 minutes elapsed, returns success, and waits another interval. The new absolute deadline is 8:36. **Important limitation:** AndroidX `WorkerWrapper.resetPeriodic()` reanchors a completed periodic worker from its completion time, so a bulletin performed by the periodic worker itself must not be described as necessarily receiving an 8:30 tick after an 8:00 worker start. The collision is between independent triggers; it is a reproducible timing possibility, not independent proof of what happened on the three phones. The pure Kotlin deadline test models an independent next tick, not AndroidX's internal scheduler. The durable clock also removes sole dependence on Doze-deferred periodic work.

   Reference implementation checked: `androidx/androidx`, `work/work-runtime/src/main/java/androidx/work/impl/WorkerWrapper.kt` (`resetPeriodic` calls `setLastEnqueueTime(..., clock.currentTimeMillis())`), with `impl/model/WorkSpec.kt` (`lastEnqueueTime + intervalDuration`).
2. **Activity-owned screen-off coroutine.** `AutomaticAlerts` waits 15 seconds in a coroutine, but cancels it when its last Activity/service owner stops. `MainActivity.onStop` removes its owner. This is not a durable scheduler. Screen-off/background catch-up now uses unique persisted work; the primary clock is a wake alarm, independent of Activity listeners.
3. **False is still success.** A blocked/empty/failed automatic read previously returned false but the worker unconditionally returned SUCCESS. No independent short retry was recorded. New execution arms crash recovery before work and a ten-minute retry after a missed read, versus the selected interval after completion. Periodic work is now a 15-minute recovery check, not the selected interval clock.
4. **Unprotected long preparation.** Multi-part preparation can need minutes. The new execution is a visible bounded foreground task, with a timeout-acquired/released partial wake lock, never a permanent wake lock between checks. One process-wide mutex serializes alarm/worker execution. A retry alarm is armed before fetching/AI/preparation/audio, so process death does not discard the next opportunity.
5. **Network availability is not validation.** `Announcer` previously drained on `onAvailable` only if internet was already validated; it had no capabilities callback. It also immediately ended unprepared requests on a transient `onLost`. The callback is now installed for every accepted request, including initially-online requests. New validated-capability handling drains pending requests and requests due catch-up; a five-second handoff grace rechecks real internet. Fully prepared current-day clips remain playable offline. A sustained loss cancels unprepared work and leaves news unread.
6. **HD warning timer spans unrelated failures.** Previously the first failure timestamp survived until actual playback, so an isolated later failure after a long gap could immediately cross the warning threshold. The timer now resets across long gaps, clock rollback and newly validated transports; connectivity loss is not called an HD-provider outage. Validation is not recorded as successful speech.
7. **Safety after preparation.** Automatic news now rechecks screen/call/quiet/silent-place/media gates after long preparation, and screen/call/quiet/silent-place throughout playback. During playback it deliberately excludes `isMusicActive` (which includes its own MediaPlayer). Manual requests retain their existing rules. Date/timezone expiry, prepare-all, selected voice, Stop and completed-only deduplication remain.

Manual news preparation is also guarded: automatic ticks wait through manual fetching/preparation, not only audible MediaPlayer playback. The guard is released in finally on Stop/cancellation. Persisted in-progress phases are shown as retry after process restart, never as still-playing proof.

## Android constraints / permission decision

`SCHEDULE_EXACT_ALARM` is optional special access, not a runtime-dangerous permission and not `USE_EXACT_ALARM`. Android settings must grant it explicitly. Without it the same persisted deadline uses inexact allow-while-idle alarms and WorkManager recovery. Exact alarms can start the bounded foreground task; background-start rejection falls back to persisted work. WorkManager's service declares the existing `specialUse` foreground type. WAKE_LOCK is declared explicitly (already supplied by WorkManager's merged manifest).

An exact wake-up does **not** promise immediate voice: feeds, validation/editing and prepare-all HD take time and can fail. Doze can restrict networking beyond Android's temporary alarm allowlist; battery/OEM restrictions remain owner-controlled, and force-stop cancels automatic operation until reopening. The UI separates next check / last actual check/result from the prepared category report and links to alarm access and Android battery restrictions without silently granting exemptions.

No eligible fresh stories still means silence. Do not defeat date/source/editor checks, replay consumed stories or fill missing categories with invented news merely to make a scheduled check audible.

## Verification

- Local Python/source gates and EN/TA translation checks: see final receipt / CI.
- Required Kotlin suites: NewsTimingPolicyTest (10) + NewsAutoScheduleTest (12), in both debug/release unit-test runs; Android tests use Robolectric API35, not a real OEM.
- New CI builds/lints a pinned-signer 2.80/code85 candidate and compares its **built** permissions against immutable public 2.79, rejecting missing baseline or new dangerous permissions without evidence.
- CI emulator installs the exact signed release, seeds the existing 30-minute preference with a last-completed timestamp making its next deadline 90 seconds away, backgrounds/kills the app process (not force-stop), turns the screen off and enters forced deep Doze. It requires `last_origin=alarm`, a real persisted check at the deadline, and a future retry/deadline. This is a short-deadline scheduler/process/FGS test, **not** an elapsed 30m/1h/2h/3h, full-audio/provider, physical-phone or OEM guarantee.
- Physical phones: not yet verified. Need the candidate's installed identity and all four real intervals with screen off, followed by Wi-Fi/mobile handoff during preparation and prepared playback, calls/media/quiet hours, Stop, and a reboot/update. Do not call the three owner's screenshots such a test.

No publication, main merge or website pointer change is part of this candidate verification.
