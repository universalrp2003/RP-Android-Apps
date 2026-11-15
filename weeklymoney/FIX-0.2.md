# 0.2 diagnostic — book Save feedback and screenshot control

**Synthetic records only; incomplete development preview, not a stable release.**

## Owner defect report

Owner installed 0.1 and reported screenshot blocking and an apparently unresponsive
new-book Save. Code inspection found that the old form required numeric weekday and
strict ISO date strings, caught validation errors generically and rendered the message
behind the modal. This is an identified defect, not proof that every failure on the
owner's device had that cause. No owner data/screenshots were uploaded or published.

## Changes

- Book form uses seven bilingual named weekday choices, defaulting to today's weekday.
- Starting date is selected through Android's calendar, defaulting to today; no manual
  ISO date typing is required to create a book.
- Blank book number shows a specific bilingual error **inside** the modal. Dialog scrolls
  to that error. Number accepts Tamil text/digits. Heading stays optional.
- Saving shows busy feedback and disables repeated submission. Dialog closes only after
  the encrypted atomic write completes. Success is shown in the main window. Other
  failures keep inputs and show a visible error inside book/loan/payment forms.
- Screenshots ON/OFF switch at top of screen, including unlock screen, with privacy warning.
  Screenshots default OFF; choice is saved in app-local privacy preferences (not backups).
  ON clears FLAG_SECURE; OFF sets it. Compose dialogs and calendar follow the choice.
- Safe-drawing and keyboard insets prevent system bars/keyboard from overlapping main UI.

## Verified build

- Source `8ec2b25d27d6cfe9bf1ee73230d7cd6ff3afbf93`.
- [GitHub Actions run 43714507107](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43714507107): **SUCCESS**, 15 November 2026 12:37 UTC.
- Passed source checks, JVM tests, Android lint, debug APK assembly, native Android 35
  emulator regression through `:weeklymoney:connectedDebugAndroidTest`.
- Native test uses real setup/new-book Save controls: toggles screenshot window flag,
  sets up synthetic owner, checks empty-number inline error, enters a Tamil-digit book
  number, chooses Monday, opens/dismisses calendar, saves, recreates Activity, unlocks,
  checks book persisted, turns screenshots OFF and checks preference after recreation.
- This tests Activity recreation + re-read from encrypted disk, **not process kill,
  uninstall/reinstall, full backup recovery, pixel-level screen capture or every OEM**.
- No physical-phone verification or owner acceptance of 0.2 yet. Previous 0.1 safety
  and feature limitations still apply.

## Safe installation — side by side, not an in-place update

Initial 0.1 used an ephemeral debug signer, which was not retained. No same-key update
is promised. 0.2 is deliberately `com.universalrp.weeklymoney.preview.fix2`, labelled
**வார வசூல் · TEST 0.2**. It installs beside 0.1 without replacing its files. Do **not**
uninstall 0.1 or clear its storage. Export anything important from 0.1 before testing.
Existing data is not copied automatically. Full encrypted backup restore is available
only on an empty diagnostic installation and requires the original backup passphrase.
That restore workflow has not been end-to-end device-tested.

Download [artifact 14162205398](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43714507107/artifacts/14162205398)
while signed into GitHub, extract the ZIP and install `app-debug.apk`. Artifact name
`WeeklyMoney-0.2-diagnostic-synthetic-records-only`, 14-day retention; archive size
9,754,149 bytes. GitHub archive digest:
`277cda10ce74e8a2ce3f6a6fe99c2efb886068c4f75e1208a007033b2756eecc`.
This is the **ZIP digest, not an independently checked APK digest**. No artifact APK
has been downloaded into Arena; GitHub artifact storage is outside sandbox network access.

Open TEST 0.2, turn Screenshots ON, then create a test book: number is required, day/date
have defaults, heading is optional. Screenshot only synthetic details or redact private
names/amounts before sharing. Turn screenshots OFF when debugging is finished.
