# Live Guard 2.91 / code96 — private compact-home candidate

## Scope and consent (3 November 2026)

Owner accepts the concise cyclone announcement in private2.90; its source is preserved unchanged. New work: daily radio-counter rollover, compact home, headline Read news text button, collapsed data details, real radio dBm versus ping, honest junk/cache measurements and a small enabled-feature permission summary. User explicitly chose silent notifications for genuinely new verified disaster advisories. No public release authorization.

## Delivery and data boundaries

- Counter reads reset stale local-day preferences even without a running sampler. The sampler refreshes its shared card on zero-traffic/baseline paths. Resetting does not attribute pre-baseline whole-day bytes to a radio.
- Cache is a filtered/capped observation of readable app caches (existing repository: excludes self, optional system apps, under32KiB entries and unreadable apps; largest300). Empty/unchecked/inaccessible means unknown, never whole-phone zero. Home refreshes cache on entry when its observation is over five minutes old, only with usage access. Mobile binder reads move off the UI thread; home signal uses existing foreground dashboard cadence.
- Daily counters, app labels and grant checks remain local. No new Android permission, dependency, network host or background worker. No raw screenshots are committed.
- Optional notification feature uses stable ask-first discovery ID `disaster_silent_notifications`, defaults OFF, provides Enable / Not now and a Rain & safety switch. Existing news-disaster OFF also blocks/cancels these notifications. Android notification denial/channel blocking does not consume the independent dedup ledger. No voice master, quiet/call/HD policy, spoken reservation/count or cyclone high-water change.
- Notifications originate only from successfully validated candidates in existing news checks, using their existing locality selection and 2000km bound. TC requires a verified observation timestamp; FL/EQ dedup by stable event identity. Separate local bounded256-entry high-water ledger fails closed on corruption/full memory; no tombstone eviction. The silent low-importance channel has no sound/vibration; immutable report PendingIntents allow only known official/GDACS source URLs. Lock-screen contents are private by default. OS/user channel settings remain authoritative.
- Notification posting and its durable receipt cannot be one atomic Android transaction: process death between those operations can repeat a delivery. Failed local receipt commits stop the batch. Reports are regional source reports, not emergency/local-impact warnings or a real-time monitoring guarantee.

## Verification — SUCCESS

Source `c3925de271b0e93581eefea93cac0ea1e93c6b83`, CI41561059637/job125671501023 **SUCCESS**, completed `2026-11-03T01:51:29Z`. All823 required JVM tests actually executed /77 suites, including8 daily reset tests,4 compact-measurement tests,14 notification tests and2 ask-first tests.339Python checks,1427 bilingual keys, release build/lint/signer/full permission continuity passed. Exact public2.89 same-key update preserved UID, preferences/private data and restart. Expanded native settings script passed the new Not now introduction, compact home (observed cache/latency) and Details disclosure. All4 required Android35 tests (PDF isolation/invalid rejection/shared foreground ownership/six-segment rendering) passed. No physical-device, audible-speech, live-provider accuracy or Google clearance claim.

Private artifact [13196999874](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41561059637/artifacts/13196999874): extract `CleanSweep-v2.91.apk`; normal same-key update over the installed app, no uninstall/data clear. APK14734343 bytes / SHA256`90438971b49967a3890132245cb0ab7bee0097fca4e8b411071fbe9eaa317b6c`. Archive SHA256`27d6b22ad697a0d43602979165307b98bf39a0b648c38a7a4e1c7963024461ef`. Hash evidence is CI annotations/GitHub metadata, not an independent Arena download. Full machine-readable evidence: `docs/live-guard-2.91-candidate-verification.json`.

Public2.89 APK, site/catalog, accepted sibling apps and publishers unchanged. Same-key update only, never uninstall/data-clear. Prior2.90 verification receipt stays historical and does not certify2.91.
