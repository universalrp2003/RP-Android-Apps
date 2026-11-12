# GitHub rules for every future change

Written after the Live Guard 2.95 round, the two follow-up cleanups and the failures those rounds
actually hit. This is the short list to read before touching GitHub again. It does not replace
[`PUBLISHING.md`](../PUBLISHING.md) (which owns the publication mechanics) or
[`AGENTS.md`](../AGENTS.md) (which owns the history); it is the "what to be careful about" page.

## சுருக்கம் (Tamil summary)

1. வெளியிட்ட APK-ஐ மீண்டும் கட்டவோ, மாற்றவோ, tag-ஐ நகர்த்தவோ கூடாது.
2. ஒரு நேரத்தில் ஒரு செயலி மட்டும்; `main`-க்கு நேரடியாக push செய்யாமல் PR வழியாக மட்டும்.
3. Merge-க்கு முன் அந்த செயலியின் candidate run, website சோதனை, Build APK எல்லாம் பச்சை நிறமாக இருக்க வேண்டும்.
4. வெளியீட்டுக்குப் பிறகு site, catalog, release-dates, README, receipt ஆகியவற்றை அதே APK-இன் அளவு/SHA உடன் மட்டுமே புதுப்பிக்க வேண்டும்.
5. கைப்பேசியில் சோதிக்காத ஒன்றை "சோதித்தது" என்று எழுதக் கூடாது; சொந்தப் படங்களையும் ரகசியத் தகவலையும் பதிவேற்றக் கூடாது.
6. வெளிப்புற tool/MCP சேவைகளுக்கு repo-வில் எழுதும் அனுமதியோ (merge, tag, workflow) ரகசியங்களோ தரக்கூடாது.

## 1. The five things that must never happen

| Never | Why, from this repo's history |
| --- | --- |
| Overwrite, delete or re-upload an existing release asset | Users already downloaded it; the digest in receipts, the site and `catalog.json` would stop matching real bytes. |
| Move, delete or re-create an existing tag | Tags are the permanent identity of a published APK. A moved tag silently changes history. |
| Rebuild "the same version" to fix a problem | Then the tested bytes and published bytes differ. Publish a **new** version instead (2.94 → 2.95 did exactly this). |
| Re-run a publisher workflow for a tag that already exists | The pinned publishers refuse this on purpose. If a publish half-finished, stop and investigate; do not rerun. |
| Claim a phone/OEM/Play Protect result that CI cannot produce | CI proves bytes, permissions, updates and emulator behaviour — never a physical dual-SIM, modem, speaker or Google verdict. |

## 2. Branch and PR hygiene

- Work only on the session branch Arena gives you (`arena/<id>-rp-android-apps`). Do not create,
  rename or push other branches.
- `main` is reached only through a pull request. The `Deploy Official Website to GitHub Pages`
  workflow deploys **only** from `refs/heads/main`, so a merge is also the website release step.
- One app per change. A round that touches `cleaner/**` must not accidentally publish SMS Guard,
  Oor, Launcher or Store; those workflows are candidate-only unless the commit carries their
  `[publish-<app>]` marker.
- Put the publish marker in the commit message only when you actually intend to publish. Markers
  are per app and exactly named: `[publish-liveguard]`, `[publish-smsguard]`,
  `[publish-oorarivippu]`, `[publish-radio]`, `[publish-pulseeq]`, `[publish-voicenotes]`,
  `[publish-podcast]`, `[publish-vasanthi]`, `[publish-vocalremover]`, `[publish-secretmessage]`,
  `[publish-tamilanstore]`, `[publish-dailyvault]`. A wrong or invented marker silently does nothing.
- Mark a single-app round with the `[<app>-only]` suffix (e.g. `[liveguard-295-only]`) so the
  generic sibling matrix skips the other apps, and the intent is readable in `git log`.

## 3. Before merging — the gate list

Everything in this table must be green on the branch. If a row is red, do not merge.

| Check | Proves |
| --- | --- |
| the app's own candidate workflow | release compile, lint, signer, full permissions, required JVM suites, upgrade over the previous public APK, and its Android 35 native tests |
| `Build APK` | the shared source gates (`verify_cleaner_next_update_source.py` pins version/code/test counts) and the full multi-app build |
| `Website usability checks (no deployment)` | the 14 cards, dates, checksums, keyboard/a11y and the `tools/tests/test_website*.py` contracts |
| `Deploy Official Website to GitHub Pages` (build job on the branch) | every mirror downloads and SHA-matches, the mirror budget holds |
| the app's own release workflows (SMS Guard, Oor, Podcast, Vasanthi, …) | they build and verify without publishing |
| local `python3 -m unittest discover -s tools/tests -q` | 430 Python checks today |

Local Gradle/Android builds are not possible in the Arena sandbox — Kotlin correctness is
established by CI only. Do not describe a source check as a build.

## 4. The publishing sequence (copy this order)

1. **Candidate**: source change → candidate workflow green → record the artifact id and archive
   SHA in `docs/<app>-<version>-candidate-verification.json`.
2. **Pins**: write `docs/<app>-<version>-publication-pins.json` with source SHA, run/job/artifact
   ids, exact APK bytes and SHA-256, signer, permission delta and test counts. That file is what
   triggers the publisher — nothing else does.
3. **Publish**: the publisher must copy the artifact without rebuilding
   (`rebuilt: false`) and record `prior_public_assets_unchanged: true`.
4. **Prove**: re-read the release through the API and confirm the asset size and `digest` equal the
   pinned values; write `docs/<app>-<version>-publication-receipt.json`.
5. **Integrate**: `site/index.html` (JSON-LD, latest-table row, card badge, bilingual brief, in-card
   SHA, `.mini` size, ZIP/APK/Notes buttons), `site/catalog.json`, `site/catalog-v2.json`,
   `site/release-dates.json`, `shared/.../SisterApps.kt` `publishedVersion`, READMEs, digests ledger.
6. **Merge**, then confirm the main-only Pages deploy succeeded and its post-deploy verification
   downloaded the live `apks/<asset>` and `.zip` and byte-matched them.

A version bump is a *new* release. Retiring an old website mirror (see §6) is **not** a release.

## 5. Things that quietly break (all of them happened here)

- **Path filters.** Every app workflow has its own `paths:` list; a commit outside them produces no
  run. Documentation-only paths are ignored by `Build APK` and Pages on purpose, so "no run" is
  normal — but if you expect a run and see none, check the filter before assuming success.
- **Duplicate pushes cancel runs.** `concurrency: cancel-in-progress` means two quick pushes cancel
  the first run; only the newest run counts.
- **`gh run rerun` refuses some failures.** An emulator-boot timeout needs a new push (a workflow
  file comment is enough), not a rerun.
- **Downloads are blocked in the sandbox.** Artifact and release blob hosts answer `EOF`/
  `SSL_ERROR_SYSCALL`; rely on GitHub's `digest` metadata plus the publisher's installed-byte
  receipts instead of claiming a local byte inspection.
- **Gradle deletes test APKs after connected tests.** Check installed bytes *before* instrumentation
  and *inside* every required native test.
- **Date and count assertions.** `site/release-dates.json`, the `datetime=` attributes and the
  testing-status sentences are asserted by tests; change them in the same commit as the version.
- **Stale pins in source gates.** `verify_cleaner_next_update_source.py` pins version/code/test
  counts; forgetting it turns unrelated workflows red with a misleading annotation.
- **The test reporter must stay silent when nothing ran.** `tools/report_failing_tests.py` now emits
  nothing when no JUnit XML exists, so a source-gate failure is not reported as a test failure.

## 6. The website mirror is a budget, not an archive

The Pages artifact is capped at 1 GB and every mirror costs twice its size (`.apk` + stored `.zip`).
Only two kinds of release may be mirrored:

1. the current release of each app (its APK fallback, `.ZIP` button and JSON-LD `downloadUrl` read
   those files), and
2. the historical builds `tools/verify_live_three_releases.py` byte-checks after each deploy.

`tools/tests/test_website_mirror_budget.py` enforces both the rule and the `MIRROR_BUDGET` in
`pages.yml`.

**Actions artifacts are a separate pile with the same problem.** Every `Build APK` run uploads five
APK/bundle packages; at 30-day retention that reached 9,299 packages and ~187 GB (free on a public
repository, so it is hygiene rather than a bill, but it buries the runs anyone needs). The five
heavy uploads now keep **7 days**, and `.github/workflows/cleanup-artifacts.yml` prunes the rest
every six hours with `tools/cleanup_actions_artifacts.py`: at most 300 packages per run, never
anything younger than 7 days, never the newest package of a pipeline/branch, never a recent Pages
package, and never an artifact id quoted in `docs/` or `releases/`. A manual dispatch defaults to a
dry run. The Arena integration cannot delete artifacts (403), so this workflow is the supported path
— do not try to bulk-delete from a shell session. When a new version ships, **drop an older mirror in the same change**; the retired
build stays downloadable from its GitHub release page, so no download is lost — only the duplicate
copy on the website disappears.

## 7. Privacy and permissions

- No new permissions, no background location polling, no identifier uploads — ever, without an
  explicit owner request in writing.
- The reference phone screenshots stay private. Never publish originals, SIM labels, cell
  identifiers, phone numbers or any personal detail; only the reviewed masked derivatives already
  recorded in the gallery documents may be public.
- Never ask for or store passwords, keystore material, PATs or 2FA codes in a chat or a file.
- Download APKs only under `/tmp`, never into the repository checkout.
- If Play Protect warns, stop. Do not tell a user to bypass it.

## 8. External tooling and MCP servers

[`github.com/mcp`](https://github.com/mcp) is GitHub's MCP registry: a catalogue of servers that let
an AI assistant call some app's API. An installed MCP server acts with **the authority of the token
you give it**, and being listed in the registry is not a security audit of its code. This project
publishes real APKs and holds keystore secrets, so the default answer is "not needed":

- **Nothing in the release pipeline uses MCP.** Builds, gates, publishing, site deployment and the
  live byte verification all run in `.github/workflows` and are checked with `git`/`gh`.
- **Only one server is acceptable**, and only when you want an assistant in your own editor to read
  this repo: the official [`github/github-mcp-server`](https://github.com/github/github-mcp-server),
  in **read-only** mode, with toolsets limited to `repos,actions,pull_requests`.
- **Forbidden capabilities**, regardless of server: merging PRs, pushing to `main`, creating or
  moving tags, triggering or dispatching workflows, editing release assets, or reading secrets. A
  server that can do any of these can publish a build by accident — the exact failure this repo's
  pinned publishers exist to prevent.
- **Token hygiene**: a fine-grained token scoped to this one repository, read-only permissions, no
  `workflow` scope, stored by the MCP host — never pasted into chat, a commit, an issue or a doc.
- **Third-party servers** (browser, scraping, database, design tools, …) are opt-in per person and
  never part of this repository's process. Read the source before installing one, and never point
  one at this repo with a write-capable token.

If an MCP tool ever proposes a release action, stop: releases go through the documented publisher
sequence (§4), with a human deciding, not an assistant's tool call.

## 9. Honest wording

Say what the evidence says and stop there:

- ✅ "923 required JVM tests / 89 suites, 418→426 Python checks, 9 Android 35 native tests, exact
  installed-byte hash matched, same key, no permission delta."
- ❌ "Works on all phones", "Play Protect clean", "dual-SIM verified", "audible speech verified" —
  unless the owner personally reports that specific result on that specific phone, and then say it
  is an owner report, not an independent check.
- Publication receipts must keep `physical_phone_tested`, `installed_phone_hash_matched` and
  `play_protect_clearance` truthful even when the release is fully published.

## 10. If something goes wrong

1. **A red check that is not yours** — read its annotation before touching anything; confirm whether
   it was already red on `main` (this repo had two such cases: `Build APK` pinning 2.93, and the SMS
   Guard gate dying on that same stale pin).
2. **A half-finished publish** — stop. Never rerun the publisher, never rebuild, never re-upload.
   Inspect draft vs public assets with the API and write down what exists.
3. **A wrong site value after merge** — fix it with a normal PR and let Pages redeploy; never edit
   the release asset to match the site.
4. **A failed deploy** — `Deploy Official Website to GitHub Pages` can be dispatched manually; the
   deploy job runs the live byte verification again, which is the point.
