# Live Guard 2.81 candidate — repeated first story, 31 October 2026

## Owner evidence

The owner reports 2.80 repeatedly reading the same circled Tamil Nadu asset-declaration story, sometimes stopping after it and sometimes continuing. Screenshots 12:33–12:44 show prepared selections changing from four to five items and `screen_on` checks with next checks ten minutes later; the shown times also move after opening Settings. Screen captures alone are not playback traces, but the owner's report is actual repeated-audio evidence, not merely a repeated preview. The battery icon has no charging bolt in these captures; continuous unplugged screen-off/full interval behavior is still not independently verified.

## Confirmed source problems, not an independently captured device trace

1. Announcer checks `isMusicActive` again before **each** prepared HD clip. Android can still report the app's previous player as active between clips. During a clip this flag was already excluded to avoid self-cancellation; the same exclusion was missing between clips. This is a concrete self-audio failure path, not proof it was the only interruption on the owner's phone.
2. The watcher records every headline only after the full bulletin returns true. A later failure discards completion knowledge for an earlier fully played story. A subsequent ten-minute recovery then selects the same story again. No partial-prefix checkpoint existed.
3. `syncDeadline` takes min(saved, due-now) even when a future retry exists and the completed-bulletin clock is overdue. Reopening Settings can therefore rearm almost immediately, record screen-on, then move the retry ten minutes forward again. These are checks, not proof of scheduled successful playback.

## Correction and safeguards

- Keep the media gate before starting. After our own player starts, do not use global `isMusicActive` to block its later clips. Existing call/screen/quiet/silent-place/day/Stop gates remain before and throughout playback. This does not add a new audio-focus implementation or claim universal external-player interruption handling.
- Build a per-story clip plan; clips cannot straddle stories. Prepare **all** HD clips first with unchanged bounded preparation/voice policy. Only a successful final clip of a story synchronously checkpoints its source key and recent-event title. Prepared and interrupted stories remain unread. Manual and automatic paths share this logic. Full completion alone advances the successful-bulletin timestamp and rotation.
- Keep a separate actual-audio-activity timestamp: start or completed clip, never synthesis. A failed audible read respects the selected 30/60/120/180-minute interval before automatic retry. It is **not** called a successful bulletin and does **not** consume incomplete stories. Silent network/preparation failures keep the ten-minute recovery opportunity. A manual read can still be explicitly requested. No legacy read keys or user settings are cleared.
- Preserve a future overdue retry on Settings reopen. Explicit interval changes may reschedule immediately. Add completed-story count / last full completion / last audio activity so a prepared selection is not the only evidence on screen. The Tamil capacity label now correctly says **up to** eight, matching English.
- Keep a bounded 1,024 current-day key set instead of 160 to avoid evicting early completed stories during ordinary full-day 30-minute use. Existing seven-day event/title history remains bounded; this is not unlimited archival deduplication.
- Keep package/signing identity, optional precise-alarm access, all freshness/source/translation checks and saved controls. No invented filler, system TTS fallback, data reset, uninstall or permission bypass.

## Verification boundary

New required tests: eight plan/boundary/media-policy cases, four timing cases and four API35 Robolectric persistence/scheduler cases (57 suites / 548 required declarations total). Source guards additionally require the completion callback after successful clip playback, not on prepare/start. These do not play real provider audio or prove OEM behavior. CI additionally runs both unit-test variants, release lint, signed identity/baseline permission checks and the same short seeded dead-process Doze wake fixture; that fixture is not a full 30/60/120/180-minute or full-audio test. Actual results and exact candidate identity are recorded in the PR receipt when available.

2.81/code86 is a new **unpublished candidate**, not a replacement asset for any previous version. Public2.79, the exact631ecd43 2.80 test APK, and the already supplied SMS/Oor candidates are unchanged. Phone verification of the repeated-prefix scenario and selected intervals is required before publication or main merge.

### First CI attempt: wake check failed, candidate held

Source533b43c9: full Build41180518806 succeeded; dedicated41180518727 passed both JVM variants/548 gate, release lint, signed86/API26 identity and pinned signer, with no added dangerous permissions. Its real Doze check **failed**: at the end of its unchanged130-second observation window the schedule preferences contained only `next_check=1793432184000`, with no observed alarm-origin check. That is not a wake-up pass or a completed bulletin. Other active workflows passed (superseded SMS/Oor duplicates cancelled with successful replacements).

The Azure-hosted artifact/log downloads are inaccessible in this workspace; the GitHub Checks API still exposes annotations. Add bounded failure-only alarm/service/power/logcat diagnostics to that API and emit the exact signed candidate identity at verification time, so an early wake failure cannot hide its digest. Assertions,90-second seed and130-second observation window are unchanged. Recheck with these diagnostics before drawing a cause conclusion or supplying a ready candidate.
