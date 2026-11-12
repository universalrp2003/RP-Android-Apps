# Website and continuous playback follow-up

## Requested corrections

- Shorten the website front page. Keep detailed device-test history behind a collapsed release-details disclosure and link to the evidence, rather than a large warning narrative above the downloads.
- Correct Secret Message: Internet chat uses the relay; same-network chat needs both phones on the local network. It is not fully offline. Remove the unsupported decoy/steganography copy from the card and structured metadata: the documented product is an MLS chat with timed messages, not ordinary-text steganography. Encrypted Internet traffic is sent to the relay; do not claim nothing is uploaded. The security design remains explicit about metadata and lack of an independent audit.
- The owner selected the exact Tamil display name **என்னைச் சுற்றி** for Vasanthi AI. Change user-visible Tamil names, not the package, English brand, release prefix or update lineage.

## Candidate versions, not stable publications

- Tamil Podcast Player 1.5 / code 7: continuous show queue in publisher feed order, Previous/Next, continuous toggle, durable queue, and offline-only completed-download queue. Default continuous playback stops at the last episode; an error or missing local file stops rather than silently using the network.
- Ramesh Radio 1.27 / code 27: visible Continuous, Random and Repeat off/one/all controls for local files. Continuous off pauses at the end of the current track. Persist settings and restore them in the service for background/widget sessions. Do not carry local modes into live radio.
- PulseEQ 1.10 / code 11: multi-select audio documents; persistent queue and modes; Continuous, Random, Repeat off/one/all and Previous/Next. Advance on the existing decoder thread only after old codec/AudioTrack cleanup. Random visits each selection once per cycle; repeat-all starts another cycle. Continuous off stops after the current track. The existing DSP is retained; no streaming or broad-storage permission is added.
- Vasanthi AI 1.1.5 / code 8: Tamil display-name correction on home screen, launcher label, notifications and shared directory.

The website still offers actually published stable APKs. A separate `publishedVersion` in the shared directory keeps candidate About/sister-app links on existing public tags, rather than inventing a link to an unpublished candidate. Update that field only after an actual publication succeeds. Do not republish unchanged apps or substitute a freshly rebuilt APK for a tested candidate.

## Additional story sources

These are public, free-to-access RSS sources discovered from publisher feeds/directories, not invented URLs. Public listening access does not establish a redistribution licence, perpetual availability or suitability for children. No paywall or DRM bypass is added. The unchanged live feed gate must pass RSS parsing and a sampled HTTPS audio prefix for all fourteen sources before the candidate is recommended.

- Love stories: Wrong Play, `https://anchor.fm/s/5d28c848/podcast/rss`. The feed describes Tamil love stories but also political discussions and mixed topics; it is not a children's feed. Discovery: [1](https://anchor.fm/s/5d28c848/podcast/rss).
- General short stories: Tamil Short Stories - Under the tree, `https://anchor.fm/s/1feab1a8/podcast/rss`. Discovery: [3](https://anchor.fm/s/1feab1a8/podcast/rss). Some episodes are literary/science-fiction stories, but the whole show is classified as short stories rather than science education.
- Science-fiction storytelling: Dhana Siva's தமிழ் அறிவியல் புனைகதை / Tamil Science Fiction, `https://anchor.fm/s/11120d660/podcast/rss`. Directory lists two episodes and explicitly classifies it as fiction/science fiction. Discovery: [8](https://podstatus.com/podcasts/tamil-science-fiction-1340735).

Science fiction is a separate category from science/space education. Existing eleven feeds remain; no claim is made that every episode has been fully listened to or decoded.

## Verification and remaining phone checks

- Python source-wiring regression tests deliberately reintroduce the stop-after-one-story bug, single-file picker, offline-network fallback, cosmetic shuffle/continuous controls and unpublished release links; each must be rejected. Empty sources must fail.
- Android-free JVM queue tests cover sequential end, invalid/empty queues, repeat one/all, single-item queues, finite random traversal, new random cycles and story-category classification.
- Candidate CI compiles/tests all five relevant modules, inspects real APK identity/signatures and writes source/hash receipts. Browser-only capability checks remain mandatory for podcast and launcher. RSS/audio checks are not weakened.
- Mandatory local Cleaner verification is invoked honestly; this sandbox has no real Debug JUnit XML. CI's actual Cleaner/Vocal Remover suites remain the real regression evidence. No reports are fabricated and live neural voice tests are unchanged.
- Before publication, test two or more stories/selected files through a real end-of-track transition with screen off; continuous off; repeat one/all; random order; Previous/Next; pause/resume; service recreation; and offline playback with connectivity disabled. Confirm the selected Tamil name and upgrade behavior. Document observed results, not assumed device success.

No new app release is authorized by this implementation report alone. Original screenshots remain private.


## Verified candidate result

[Candidate run 40495158373](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40495158373) completed successfully at source `4318a7ba23deb0fb3d08820817de6c96ca1daa0c`. All five module test/build/signature/identity/receipt jobs passed. All fourteen RSS and sampled HTTPS audio-prefix checks passed, including the three new sources. This is not full-episode listening, a phone test or a security audit.

The first run exposed a stale expected catalogue size/category count; the exact assertions now require fourteen feeds/nine categories and retain uniqueness, valid-URL and non-fallback category checks. The superseded compile-failure excerpt is removed after the successful replacement run.

Download the owner-test APKs from this exact run, not a later rebuild. Each artifact contains an APK and its source/version/code/size/hash receipt plus real test output:

- [Podcast 1.5 / code 7](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40495158373/artifacts/12729214176)
- [Ramesh Radio 1.27 / code 27](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40495158373/artifacts/12730313180)
- [PulseEQ 1.10 / code 11](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40495158373/artifacts/12729728799)
- [Vasanthi AI / என்னைச் சுற்றி 1.1.5 / code 8](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40495158373/artifacts/12729818124)

Launcher was compiled as a shared-source compatibility check, not a requested new release. Do not ask the owner to reinstall it.

The public-release API audit verified all twelve existing stable tags and the SHA-256 metadata behind twenty-four primary download buttons. Shared directory published-version pins match those same public tags. Pages run 40495158311 passed. New candidate versions are not advertised as public stable releases; promotion remains held for owner phone confirmation.

## Radio icon candidate compile-error follow-up

- Continued prior branch history on the session branch `arena/86d1a321-rp-android-apps` without switching branches.
- Manual `build-apk.yml` dispatch with explicit `publish=false` is still rejected with HTTP 403 (Resource not accessible by integration).
- Run 40544324236 log download still fails, but its repository `ci-build-failure.md` report identifies a composable `remember` inside the non-composable LazyListScope builder and an unresolved Material3 `TextButton`.
- Moved row memoization into RadioScreen composition before LazyColumn and imported Material3 TextButton. Added a source regression check for both errors.
- All 112 Python tests, app localization, continuous-playback and browser-update source checks pass. No local Gradle/Android SDK is available; Android compilation remains unverified until CI succeeds.
- Candidate remains radio 1.28/code 28; no public release authorized or issued. Historical CI failure reports are retained, not represented as a new successful build. SMS Guard OEM behavior still requires device validation.

## Compact radio home after user device review

- User reports candidate 1.28 works on device; screenshots demonstrate four-column icons and the station-name Play/Cancel dialog. This is not validation of all devices or all playback paths.
- Moved saved weather places (including selection/removal) from the permanent app header into the rain control dialog using an optional shared composable slot. The dialog already shows weather alert previews/status; background weather monitoring and voice alerts are unchanged.
- Removed the permanent home weather banner, reduced the radio title/chip spacing, removed the redundant subtitle, and replaced the long directory pill with a labelled accessible globe action. Station count stays a single compact line so it no longer stacks into three lines beside the controls. System font scaling is preserved.
- Candidate remains 1.28/code 28; public release markers are unchanged. This layout revision needs a new CI build and device review before publication.
