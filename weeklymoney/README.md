# Current public release: Weekly Money TEST 0.10

Owner phone-tested and approved this exact APK for publication on 16 November 2026.

- [Direct APK download](https://github.com/universalrp2003/RP-Android-Apps/releases/download/weekly-money-v0.10/Weekly-Money-v0.10.apk) · [Release notes](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/weekly-money-v0.10) · [Optional online guide](https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html)
- Source/CI and hashes: [verification](../docs/weekly-money-0.10-verification.md), [public release receipt](../docs/weekly-money-0.10-publication-receipt.json).
- **Update in place:** permanent package `com.universalrp.weeklymoney.preview.notebook`; same persistent signing key, code10. Future updates must keep package/key and increase versionCode. [Signing policy](../docs/weekly-money-signing-policy.md). Do not uninstall/clear data for normal updates.
- Weekly pre-change and post-collection Downloads backups; separate original loan dates, optional bounded Tamil date OFF by default; settled-debt dashes/entry block; full receipt history; previous date links and same-weekday fresh-volume archive continuation. About names all17 applications.
- Schema5 reads1–4. Keep external encrypted backups and master passphrase. Do not downgrade after saving. Collector exchange/independent recovery-secret features remain unfinished. Physical biometric and old Android SAF acceptance are not universally verified. Archive remains in same encrypted snapshot; no separate lazy-storage claim.
- [Tamilan Store0.14](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/tamilan-store-v0.14) lists this public TEST explicitly. Store0.13→0.14 same-key update preserved UID/preferences/private marker in the emulator; physical/Play Protect acceptance is not claimed.

## Historical development receipts (superseded by the release above)

# Latest: verified TEST0.7 active-debt labels and imported colour groups

[Download TEST0.7 + separate DEMO0.7](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43803717584/artifacts/14197905836).
See [TEST0.7 full receipt, biometric setup and data safety](FIX-0.7.md).

- I/II only for simultaneous unpaid debts. Fully settled renewals are unmarked; immutable lifetime sequences remain intact.
- Settings Old entry ON/OFF; no per-loan/bulk-ten-week checkbox. Tap a week box for old receipt entry.
- Old entry ON exposes BLUE/RED opening-group selection with audited corrections.
- Within each actual cycle page: blue first, red below; same-place/family customers adjacent in each colour group.
- Summary collections green, new loans purple with a divider.
- Android 9+ authenticated Keystore/BiometricPrompt wrapping, configured in Settings after one master unlock.
  Routine biometric mode hides the password field. Master fallback remains for lockout, enrolment changes, new installs and restores.
- Schema 4 reads old schemas 1–3. Export an external encrypted backup before updating; no uninstall/data clear. Do not downgrade after saving schema 4.

CI 43803717584 passed unit suites, lint, builds, Android 35 regression (1 test, 0 failures/errors) and both APK signature/identity checks.
**Successful hardware biometric enrolment/authentication on the owner's phone is not yet verified.**
Earlier receipts below are historical and superseded where these rules differ. Collector exchange and independent recovery-secret workflows remain unfinished.

---

# Latest: verified TEST0.6 receipt-row identification

[Download TEST0.6 + separate DEMO0.6](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43797310251/artifacts/14194734059).
See [TEST0.6 receipt, changes and upgrade safety](FIX-0.6.md).
Faint customer identity behind the ten receipt columns; no outside book-number/date header in entry;
no bottom aggregate loan amount; weekly collection totals distinguished by a pale-green row, green text and outline.
Individual loan amounts, collection-cycle dates, weekly totals and records are preserved.
CI 43797310251 passed unit suites, lint, builds, Android 35 regression (1 test, zero failures/errors) and both APK signature/identity checks.
Export an external encrypted backup before updating; do not uninstall or clear data.

Earlier receipts below are historical; TEST0.6 supersedes their relevant interface descriptions.

---

# Latest: verified TEST0.5 focused notebook preview

See [TEST0.5 changes, lock semantics and verified receipt](FIX-0.5.md).
[Download TEST0.5 + separate DEMO0.5](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43792552224/artifacts/14193158500).
CI 43792552224 passed compilation, tests, lint, Android 35 native regression and both APK identity/signature checks.
Export an external encrypted backup before updating. No uninstall/data clear is required.
Default auto-lock is five minutes **in the background**, not foreground inactivity; process death/restart still requires a passphrase.
Pinch defaults OFF; fixed +/− and 100% reset are available. File-manager MIME support/performance on the owner's phone still needs confirmation.

The earlier TEST0.4 receipt below is retained as history; TEST0.5 supersedes its UI/locking behaviour.

---

# வார வசூல் — Weekly Money · RP Ideas application 17

Status: **Owner-only Android TEST0.4 collection-calendar preview; not production-ready for real data**.

The normal notebook build retains `com.universalrp.weeklymoney.preview.notebook`
with version code 4 and the pinned TEST0.3 signer. A separate **DEMO0.4** build uses
`.preview.sample` to try invented restored records without replacing existing books.
See [verified TEST0.4 receipt and install guidance](FIX-0.4.md),
[sample restore instructions](samples/README.md) and
[தமிழ் மாதிரி சரிபார்ப்பு](samples/மாதிரி-சரிபார்ப்பு.md).

- Roman I/II appears **only** when the same customer ID has multiple loans.
- History/bulk ten-week entry is a persisted Settings switch, OFF by default.
- A book's first weekly collection is the collection weekday **strictly after** loan
  disbursement. Thursday 05-11-2026 → 12-11, 19-11, etc.
- Manually marked book holidays do not consume a week. Sunday 08-11-2026 skipped →
  that week's slot moves to 15-11-2026, and subsequent dates follow active Sundays.
  This is distinct from a customer's missed-payment dash. Calendar changes/reversals
  retain reasons/actor/time and never automatically rewrite actual receipt records.
- The normal customer dialog shows ten dates with amount boxes, then today's additional
  receipt and a new-balance preview. Save is allowed only for an active duty slot.
  Additional receipts add to earlier amounts through retained before/after revisions.
- Collection finished only marks blank cells of an active duty day, with confirmation,
  no duplicate closure records, no overwrite of existing amounts and no skipped-day entry.
- Native ruled paper, centre crease, directional page-turn shading, separate horizontal/
  vertical panning, two-finger 20–220% layout zoom, 100% reset, calendar date inputs.
  This is not a photographic texture or a full physically simulated paper curl.

Schema 3 stores book holiday history. Schema 1/2 records still load with their original
amounts/actual dates/audits unchanged. Corrected planned dates can differ; the UI labels
actual receipt dates separately. Do not downgrade after saving schema-3 data. Export an
external encrypted backup before a normal app update. Never uninstall/clear an existing
notebook to try the sample: restore into the **fresh DEMO app** before creating an owner.

**Not finished for production:** authenticated collector operation-file exchange/conflicts,
biometrics, independent recovery-secret flow, English-only UI selection, verified Tamil
solar-calendar dates, complete phone-loss/reinstall/upgrade and large-book acceptance.
All previews are fully offline and owner-only. Existing stable catalogs/apps are unchanged.
Private reference photographs must never be committed or packaged. Samples are invented.

## Portfolio identity — owner designation

The owner designated this as **RP Ideas' 17th application** on 15 November 2026.
Machine-readable identity lives in `app-identity.json`. It is a separate Android app,
not a screen inside Expense Tracker or DailyVault. Development builds keep the isolated
`.preview` base package. Diagnostic 0.2 adds `.fix2` for safe side-by-side installation
because the initial ephemeral debug signer was not retained; do not uninstall 0.1 to
try it. The planned production package is reserved, not released. Portfolio
number 17 does not mean 17 published releases: existing stable catalogs/download links
remain unchanged until this application's actual publication and verification.

## Confirmed product contract (15 November 2026)

- Tamil-first UI; Tamil/English customer input, with an English interface option.
- Initially one empty notebook; owner enters number, collection weekday, start date
  and optional heading. No fixed seven-book limit. Today’s existing book opens first.
- Paper ledger spread: place, family head, customer, manually entered final total,
  ten weekly receipt columns, remaining balance. Blue rules/red dividers; real-feeling
  right-to-left forward / left-to-right backward page gesture with accessible buttons.
- New borrowing starts a new blue entry/page even if another loan remains active.
  Stable customer identity links loans; immutable loan identity separates debt. Display
  I/II/III etc. on concurrent loans; do not use names alone as identity.
- Completion is derived from zero remaining balance, including early week-4 closure.
  Unpaid balance rolls over after ten weeks in red, below new blue borrowers.
  Old and new start dates and verified Tamil calendar dates belong in page headers.
  Calendar conversion still needs implementation and reference-date verification.
- Explicit no-payment visit is a distinct-colour dash. Future/unentered cell is blank.
  A book-level skipped duty day is not a missed visit: it has no weekly slot, and later
  planned collection dates move to the next active book weekday. Week 1 is strictly after
  disbursement, never the disbursement date itself.
- Prefix search by place, family head or customer navigates to the original loan/page.
- Long press shows every entry/correction, collector, recorded time and edit time.
  Retain original values; corrections recalculate subsequent balance projections.

## Owner / collector authority

Owner controls all books. A collector may add today's receipts and new borrowing;
closure follows repayment, never an unchecked “close” command that erases debt.
Collectors cannot correct historical entries without owner approval. Approval can be
exchanged through files or unlocked locally by owner authentication. Never send/store an
owner password in a handoff file. Verified, scoped, expiring approval grants permit
specific additional operations but **never whole-book deletion**. Typed collector
names are audit labels, not authentication. Real authenticated device identities,
owner unlock with rate limiting, collector-to-book assignment and grant revocation
remain required before any real-data use. Core actor/grant instances are trusted inputs.

## Fully offline file exchange — not yet implemented

Offline: collector exports a day's append-only operations; owner previews/imports,
corrects/approves, then exports a fresh authoritative snapshot for the collector.
Use unique operation IDs, book/loan/payment IDs, schema versions and base revisions.
Import must be authenticated and bounded, transactional and replay-idempotent. Reject
unknown references, wrong owner/book, stale grants, duplicate IDs with changed bodies,
unsupported schemas and conflicting revisions. Never replace a ledger with an imported
file blindly. Show import summary, unresolved conflicts and acknowledgement state;
retain pending operations until authoritative acknowledgement. Device timestamps are
claimed times, not proof: owner import time and offline clock warnings are needed.

The latest owner decision supersedes the earlier online plan: **fully offline**.
No live sync, background upload, Google sign-in, Drive integration or relay deployment.
Use Android Sharesheet to send a file through any chosen app (WhatsApp, Gmail, Drive,
Bluetooth, etc.); those apps transport files independently, not through our service.
Secret Message remains untouched.

### File association and safe opening

Proposed extension `.vvasool` (not yet registered), with a versioned binary container
and explicit file kind: COLLECTION / OWNER_UPDATE / BACKUP. Extension is a convenience,
not encryption or authenticity. Android content-URI grants, MIME handling and document
picker fallback must be tested: receiving apps sometimes rename files or use generic
MIME types, so tapping may show an app chooser rather than launch automatically.
Open → authenticate → validate/decrypt → preview → explicit import confirmation.
Never import automatically merely because a file was tapped. Parse bounded streams,
reject corrupt/oversized/unsupported data, authenticate sender and intended recipient,
check revisions and import atomically. Keep a safety snapshot before any merge.
No imported file may grant owner identity or delete records. Never export private
identity keys or unlock credentials in collection/update packages.

### No destructive book or history deletion

Permanent book deletion is denied for **owner and collector**, including elevated
collector grants. Archive/unarchive should be owner-authenticated, audited and reversible;
archived books remain searchable and included in backups. Wrong entries are corrected
or explicitly voided with a reason via append-only owner-authorized events; original
amounts/names remain in audit history. A void reverses its financial effect, not its
historical existence. Archive/void persistence is still to be implemented.

Backups: the primary recovery backup MUST be outside app-private/app-specific storage,
including Android/data/<package>, cache and internal databases. These locations can be
removed on uninstall and must never be presented as uninstall-safe backups. Private
transaction/safety snapshots may supplement, never replace, external recovery backups.
At setup, ask the owner to choose a separate Documents/WeeklyMoney-Backups folder (or
another supported shared-storage/document-provider folder) via Android SAF, retaining
its URI permission. Modern Android restricts selecting Downloads itself as a folder;
offer CreateDocument for individual exports to Downloads rather than bypassing this.
Do not request broad all-files access. Scheduled external backups depend on provider
availability, retained access and Android scheduling; show failures and last verified
successful write, never promise exact-time background execution. Write a new dated
file, close and verify it before reporting success; never overwrite the only good copy.
Reinstallation requires selecting the saved backup again because URI grants are lost.
User can share/copy a backup to Drive, USB or HDD without giving this app cloud access.
Shared-storage backups normally survive app uninstall but not phone loss/reset, manual
file deletion or storage/provider failure. Encourage a second copy on another device.
Rotation must never prune the authoritative ledger or its historical records. Keep
several recent backups and longer-term dated copies; show last successful external
export and errors. Backup restores merge verified history rather than replacing newer
records with an old snapshot. Restoring to a fresh installation requires verified owner
recovery. Test recovery on a second device before relying on it.

### Unlock and recovery design — implementation/security review pending

- Owner password/passphrase with a salted, deliberately expensive password KDF;
  no plaintext password storage, logs, exports or collector knowledge of it.
- Optional Android BiometricPrompt with Keystore-backed key protection and an explicit
  password fallback. A biometric enrolled on a shared phone is not proof of ownership;
  sensitive owner approvals must still require the owner's secret on collector devices.
- Separate offline recovery key (“master recovery key”), generated randomly and shown
  for the owner to save outside the phone. **No developer/default master password or
  universal bypass.** Recovery must unwrap encrypted backup keys, not simply skip checks.
- Password, recovery-key and biometric changes need explicit flows, reauthentication,
  audit records and tested handling of Keystore invalidation/device loss. Use platform
  and vetted cryptography, not custom crypto. Rate-limit local unlock attempts without
  auto-wiping financial records. Auto-lock on timeout/backgrounding.
- Local DB, backups and exchange files need authenticated encryption and a reviewed
  key-wrapping/pairing/recovery design before any real customer data is entered.

No app can prevent loss after uninstall, phone failure, storage clearing or destruction
of all backups. Clear warnings and independently saved, tested encrypted backups are
essential; a “cannot delete” UI is not a guarantee of permanent storage.

## Accepted recovery and continuation flow (owner confirmation)

On a new/reinstalled phone, choose Restore existing books, select an externally saved
full BACKUP file and supply the owner's master recovery secret. A collection handoff
file is not a full backup and must not be offered as full recovery. Portable backup
key wrapping must work independently of the lost phone's Android Keystore. Wrong
secret/corrupt file fails without changing local records. Validate/decrypt in staging,
preview books, record counts and backup creation time, then restore transactionally.
Preserve book/customer/loan/event IDs and audit history; re-enrol this phone's biometrics
and set a new local unlock password only after successful recovery. Show the backup's
last included entry time separately from its export time: a recent export can still
contain old records.

After restore, show a reconciliation checklist for the missing period. Owner can enter
missing payments, missed visits and loans from physical notes. Retain both the actual
collection date and today's recording timestamp, actor, source='physical-note recovery'
and optional note reference. Never fabricate the original collector/timestamp. Use
normal append-only correction/balance validation and duplicate detection; flag uncertain
matches for owner review. Recalculate later balances and completed/carried loans from
these entries. No silent opening-balance overwrite or loss of earlier audit history.

If only a remaining balance is known, offer an explicitly labelled, owner-authorized
reconciliation adjustment with reason/source and before/after values, not invented
weekly payments. This needs its own audited event type (not yet implemented). Backup
and recovery retain all such adjustments. Review totals, export a fresh full backup,
then continue the same books. Mark the reconstructed period visibly in the audit.
Old collector files/devices require explicit review/re-pairing after recovery so stale
files cannot override reconstructed records or silently restore old permissions.

Required recovery tests before real use: fresh-install restore without original
Keystore; wrong secret; corrupt/truncated/unsupported backup; interrupted restore;
preserved IDs/history; physical-note gap entry; duplicate later collector import;
recomputed rollover; and fresh backup/restore of reconstructed records. Recovery is
limited to the saved backup plus information the owner can reconstruct; a master
secret alone cannot retrieve records from a lost phone.

## Android preview implementation — incomplete

Implemented source: Tamil/English-labelled owner setup/password unlock; atomic local
AES-GCM encrypted snapshots using the preview passphrase; background auto-lock and
a remembered screenshot ON/OFF switch (protection ON by default); book/loan creation (book weekday choices, calendar picker and inline Save errors); today's books first; prefix search;
integer-paise receipts, explicit missed-visit dashes, owner correction audit history;
completion balances; encrypted full-backup export through CreateDocument with readback
verification; full-backup restore on an empty installation with password validation and
record-count preview before explicit confirmation; reversible owner archive/unarchive
with retained history and archived-book search toggle. No INTERNET/storage permissions and no delete UI. Export is an external
user-selected document, not the internal database. This preview uses the same passphrase
for local encryption and exported backups, **not the final master-key/recovery design**.
Preview password must be retained; there is no password reset/recovery bypass.

NOT implemented: merge/import into existing books, independent master recovery key, password change,
biometric unlock, login rate limiting, automatic external backup scheduling/rotation,
collector pairing/permissions/handoffs, file association/import, void events,
loan/customer matching and Roman suffixes, name/loan/date editing, physical-note recovery,
real ledger-spread/page curl layout, Tamil calendar conversion, expandable guide/About,
accessibility review and device testing. There is deliberately no misleading Online switch.
Do not use real customer data; use synthetic test names and values only.

Verification: sandbox build is unavailable (no Java), but GitHub Actions run
[43712248695](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/43712248695)
**passed** `:weeklymoney:testDebugUnitTest :weeklymoney:lintDebug :weeklymoney:assembleDebug`
at source `3129f4756f704d544d73ff057b564c63698a9099`. The first two attempts failed
SDK setup and Compose scope compilation respectively, then were fixed. Artifact is a
temporary debug/test build only; no stable signing/release/publisher was used. The
four Python source checks also passed. See build receipt for evidence and limits.

## Current source scope and checks

`core/.../Ledger.kt`: integer-paise arithmetic, explicit missed visits, append-only
payment corrections, optimistic revision checks, per-loan balance/cycle projections,
prefix search and role/grant policy. Cycle projection is relative to a loan's ten-week
schedule; the mixed-new-blue/old-red physical page assembler is **not implemented**.
No domain object should be deserialized from untrusted exchange files as authority.

`LedgerChecks.kt`: deterministic executable checks for payments, correction rejection,
owner corrections, audit retention, rollover recalculation, missed visits, overpayment,
old-date collector rejection, deletion prohibition, expired grants, completion and replay.

With a Kotlin JVM compiler/JDK installed:

```sh
kotlinc core/src/main/kotlin/com/universalrp/weeklymoney/Ledger.kt \
  core/src/test/kotlin/com/universalrp/weeklymoney/LedgerChecks.kt \
  -include-runtime -d /tmp/weeklymoney-checks.jar
java -jar /tmp/weeklymoney-checks.jar
```

The domain checks were executed by the successful CI JVM test suite, not in the Arena
sandbox (which has neither Java nor Kotlin). Device/UI/recovery/two-phone tests remain open.
Do not publish or use this foundation to collect real financial records yet.
