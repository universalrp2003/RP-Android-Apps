# Tamilan Store 0.9 preview — compact storefront and sequential updates

1 November 2026. **Unpublished candidate source (`0.9-preview`, code 9), not a release.**
The public website/directory and immutable Store0.8 assets are unchanged. The reported
Play Protect harmful-app block remains unresolved; neither this UI nor CI establishes clearance.

> Follow-up: the owner reported that the debug preview requires fresh installation.
> It is not release-key upgrade-compatible. Current source is **0.9-rc1 / code 10** with
> a dedicated release-key candidate workflow; see [upgrade investigation and gates](tamilan-store-0.9-upgrade.md).
> The 0.9-preview/code9 results below are historical evidence for that earlier source, not
> proof that the new signed candidate or Play Protect has passed.

## Design research and decisions

Reviewed the official [F-Droid documentation](https://f-droid.org/en/docs/),
[Droid-ify's public client](https://github.com/Droid-ify/client) and its
[official storefront screenshot](https://github.com/Droid-ify/client/blob/main/metadata/en-US/images/phoneScreenshots/1.png),
and [Material card guidance](https://m3.material.io/components/cards/guidelines).
The F-Droid documentation index is not proof of its exact queue behavior; our sequential flow
is the owner's requested behavior, implemented and tested independently, not copied source.
Droid-ify's screenshot demonstrates scan-friendly app rows, prominent search and
Explore/Installed/Updates navigation. We adopt that hierarchy, not its branding, code, icons
or downloaded art. Material guidance favors clear hierarchy and grouped actions over oversized cards.

- Smaller native titles/body text and card padding; normal scalable `sp`, not forced font scale.
- Open/Download/Update, Screenshots and Details share wrapping action rows instead of a tall
  button column. Screenshot galleries expand on demand; all 13 original galleries remain.
- Teal/navy surfaces, gold secondary accent, the existing local `tamil_landscape` vector
  (temple, palm, sun) visibly restored. No remote decorative image or tracking.
- Compact Search + Update all + More toolbar. Request, Contact, Store releases and Help/About
  remain in More. Tamil/English and motion preferences persist; existing reduced-motion,
  TalkBack and lifecycle guards remain. Decorative ticker is visually bounded when paused;
  its complete content remains available to accessibility semantics.
- Foreground metadata refresh replaces Refresh. Catalog checks coalesce for 60 seconds;
  Store release checks coalesce for ten minutes within the retained process. Neither
  opening nor discovery downloads APKs. A short, non-blinking snackbar announces available
  updates; count/filter and Store-release action remain after it dismisses. Network failures
  explicitly retain the saved catalog, not a false “up to date” claim.

## Explicit Update all contract

1. Only installed, older, pinned-signer-compatible catalog apps qualify. Review names,
   versions, total bytes and mobile-data warning before **Start updates**. This approval
   freezes catalog entries; it does not authorize newly discovered versions or new apps.
2. One APK downloads at a time into private cache. Existing HTTPS/redirect host, size,
   SHA-256, package, versionName, SDK, signer, installed-signer and no-downgrade gates remain.
   Each transfer has a three-minute coroutine timeout in addition to socket timeouts.
3. Only the foreground Store can reverify and hand off a read-only FileProvider URI. Android
   still asks for each installation. No root, privileged/silent install or Play Protect bypass.
4. Activity result codes and package-added broadcasts are **not installation proof**.
   After return, query PackageManager briefly for the expected-or-newer numeric name,
   verified archive versionCode (or higher) and pinned signer before advancing.
5. Cancel/refusal, missing source permission, failure or leaving Store while downloading/ready
   pauses the same item. **Resume** downloads/verifies again; permission settings are opened
   only through an explicit button, and returning from settings never resumes automatically.
   The next prompt is not launched after cancellation. An already-updated queued app is skipped
   only after fresh trusted installed-state inspection.
6. Cancel queue discards remaining work. Rotation retains the in-memory queue; process death
   does not persist consent or replay prompts. No unattended durable/background installer.
   The cache holds only one APK; the next download removes the previous APK only after the
   preceding installation is independently confirmed. The external installer's read grant
   is not revoked while it is still using the file.
7. Store self-update is **separate**: existing discovery supplies a version and official
   release-page URL, not independently pinned APK metadata. A discovered Store update gets
   an automatic notice and explicit browser link; it is not smuggled into the trusted app
   queue. A verified self-update design and process-replacement test are future work.

## Validation / release boundaries

- Local: 271 Python source/policy tests pass; localization verifier and shell syntax pass.
  These are not Android runtime or security-audit evidence.
- Added six native JVM queue/evidence tests: serial order, illegal transitions, cancellation,
  permission/background pauses, bounded immutable plans, process-death default, all 13
  entries, and signer/name/versionCode evidence boundaries. All six passed in Android CI (receipt below).
- Added native Android15 test: compact Tamil/English UI, expandable gallery, actual consent
  cancellation, first Android prompt cancellation → pause, explicit resume, **two sequential
  confirmed same-key public-fixture updates** (Radio1.29→1.31, DailyVault0.4→0.5), and 200%
  font-scale screenshots. Passed in Android CI (receipt below); not a physical-phone result.
- Existing archive tamper/identity/signer and read-only handoff tests stay enabled. All fixture
  installation/removal is restricted to the existing guarded, fresh CI emulator; never owner data.
- Read-only preview workflow only. Legacy Store0.8 publisher now fails closed before signing
  or building, and ordinary pushes cannot rebuild DailyVault or an immutable Store release.
  Historical 0.8 showcase smoke remains historical, not the redesigned candidate's test.
- No permission additions, public APK replacement, release publication, website promotion,
  malware-sample upload or appeal submission. Debug-key preview is not a release-key upgrade;
  do not uninstall a working Store or other data-bearing app to install it.

### Pending owner/device coverage

Physical-phone/OEM installer behavior, Android8, 320dp screens, TalkBack interaction, unknown-source
denial, network interruption, low storage, in-flight rotation, actual process kill and signature/downgrade integration
remain explicit follow-ups. A successful CI run cannot settle the outstanding Play Protect report.

## CI receipt — 1 November 2026, 07:07:40 UTC

[Read-only preview run 41283159703](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/41283159703)
**SUCCESS**, job **124779032975**, tested application source commit
`2feb88c1e7038ff4bdac3023beeb28d43279e394`. Unit tests, debug lint, debug compilation,
instrumentation compilation and guarded Android15 emulator phases all passed. In particular,
the redesign's native test has no skip/assumption path: it reviews/cancels consent, cancels
Android's first prompt, explicitly resumes, installs Radio1.31 followed by DailyVault0.5,
and checks each package's expected versionCode/name and pinned signer before completion.
The six new JVM state/evidence tests run with the existing module suite. The native UI test
also opens the gallery, switches languages, recreates the activity after queue dismissal and captures both
languages at 200% font scaling. This does **not** cover every pending case listed above.

Run artifacts (available through the run page; retention applies):

| Artifact | ID | ZIP bytes | GitHub-reported archive SHA-256 (not an APK hash) |
| --- | --- | ---: | --- |
| `tamilan-store-emulator-evidence` | 13073998526 | 3,241,107 | `a5274b0f1b88c8ac3e1666782839235b10caccfa1831e3f2ee1d23189894282b` |
| `tamilan-store-test-reports` | 13074322679 | 49,629 | `add3a15a5c0d67f7675e3cac958044976287e6b2c713da29e6298fb9f8e777c2` |
| `tamilan-store-preview` (debug-key artifact, not a release upgrade) | 13073784068 | 24,122,717 | `a95aea6b71d727b1c652b1ccd18c469748080d52d3bb2383487e4600bfded21c` |

The agent verified success via Actions job/step status and artifact metadata. Sandbox network
restrictions prevented downloading the signed artifact/log URLs, so the captured screenshots
have **not** been independently visually inspected here; use the evidence artifact to review
the real native rendering. There is no fabricated browser mockup or claimed screenshot review.
The legacy Store/DailyVault publisher run **41283159583** was **skipped**, as intended.
No release or website deployment was requested or performed.
