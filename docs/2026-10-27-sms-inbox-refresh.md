# SMS Guard disappearing inbox investigation

## Device report

The owner reports that a friend's stored messages appear briefly (about three seconds) and disappear, while new-message voice notifications work. Screenshots show both a populated list and an empty state on Redmi 10 Prime, Android 13 / MIUI 14.0.8. The installed SMS Guard version is not shown. Do not publish the screenshots, contact names, message bodies or the device's personal name.

The owner also confirms the latest Podcast/PulseEQ candidates work. Supplied dialogs identify Podcast 1.6/code 8 and PulseEQ 1.11/code 12; both scan dialogs say This app looks safe, and a PulseEQ playback screen is supplied. This is device feedback, not universal security clearance or proof of which change affected the earlier classification. Podcast playback/genres are owner-confirmed, not independently demonstrated by its install screenshots. No new publication is authorised by this report.

## Confirmed source defect

SMS Guard paints a quick 250-message batch, then hydrates a larger batch. The repository used to turn any enrichment exception into an empty list; the view model unconditionally replaced the quick paint with that list. Thus a failed larger load could erase a previously visible inbox. This is a confirmed loss-of-display path, but the precise provider/message exception on the friend's device has not been captured.

The fix distinguishes successful reads (including a genuinely empty database), failed reads and missing permission. Failed hydration retains the last successful in-memory snapshot and shows a bilingual retry message. Real successful empty reads clear deleted messages. Permission loss clears private UI data. Null provider cursors, missing columns and cursor iteration failures no longer impersonate successful empty inboxes. Null Bundle queries try the existing legacy query fallback, with consistent date/id ordering. No SMS deletion, database mutation, new permission, voice/OTP behavior, networking or sensitive-content logging is added.

Android-free JVM regression tests cover quick-paint retention, repeated failures, successful empty replacement, successful full replacement and permission revocation. Compiler/device validation remains required. Test on the affected phone with stored messages visible for at least thirty seconds, returning from background, loading older messages, and receiving a new SMS; never use a real OTP as a speech test. Do not clear application storage or delete messages as a workaround.

## Workspace constraint

At the start of this turn, the checkout was at the earlier PR50 merge with the prior playback/catalogue changes present as a large uncommitted working tree. Those pre-existing changes were left intact. This SMS-only fix is not represented as a built APK or a passing Android CI run; do not substitute a previous candidate link for it.

## Local validation

All eighteen localization checks, AudioSet labels, the three standing preflights, 101 existing Python tests and SMS-change whitespace checks passed. These are not execution of the five new JVM tests. No local Kotlin/Gradle compiler was found. The Cleaner verifier was invoked and reported missing Debug JUnit XML; no report was fabricated. No commit, push, release or new APK was made in this turn.
