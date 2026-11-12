# Device feedback follow-up — 27 October 2026

> **Update-flow status superseded (29 October 2026):** The earlier browser-only apps described below now also run once-daily foreground release-metadata checks and display in-app notices. All non-Store update actions open the official website for browser download and Android's normal user-confirmed install flow. Tamilan Store behavior is separate.

## நிலை / Status

இந்தக் கிளையில் முகப்புத் திரையின் நிறங்கள், உண்மையான பதிவிறக்கப் பகுதி, பிரிவுவாரியான நிகழ்ச்சிப் பட்டியல் ஆகியவை மாற்றப்பட்டுள்ளன. இவை இன்னும் வெளியிடப்படவில்லை. பாதுகாப்பு எச்சரிக்கையின் காரணம் உறுதியாகவில்லை; எச்சரிக்கையைத் தவிர்த்து நிறுவ வேண்டாம்.

These are unpublished source candidates, not a new stable release or a claim that Play Protect has cleared an APK. Stable version numbers and published download links are deliberately unchanged until publication is justified.

## Launcher contrast

- All four custom filled-button paths now set foreground and disabled foreground explicitly: app rows, large controls, favourites and emergency confirmation.
- The theme defines its foreground roles instead of mixing the default purple onPrimary with custom green backgrounds.
- `LauncherPalette.kt` is the source of truth. `tools/verify_launcher_contrast.py` requires six actual palette pairs to meet 4.5:1 and rejects missing button matches.
- Regression tests remove the new foreground declaration, restore a dark text color, and supply zero matches; all must be rejected.
- This is a source/palette check, not a claim about measured phone rendering or screenshots of the new build.

## Offline library

- Download buttons beside episodes; a visible Downloads entry above the show/episode content.
- Android DownloadManager performs persistent background transfers into app-specific external storage. No shared-storage permission is added.
- Default: new downloads use unmetered Wi-Fi, not roaming. Changing the setting affects new requests; existing requests must be cancelled/deleted and started again.
- Durable download IDs, publisher URL, title and random safe filename survive closing/reopening the app.
- Library shows queued/downloading/system-paused/ready/failed states, transferred and expected sizes, stored bytes and a 100-entry limit. It offers retry and confirmed cancellation/deletion.
- A file is offered offline only after DownloadManager reports success, the app-owned file exists and is nonempty, and a recognised audio-container prefix is present. This is a header check, not full decoding or an assurance that publisher media is trustworthy.
- Play offline passes an explicit offline-only flag. The service resolves the app's own complete file and fails instead of silently streaming when that file is unavailable. Regular Play prefers a complete local download but may otherwise stream.
- Playback still uses the existing media session, audio-focus handling, seek controls and original-URL bookmark identity. No arbitrary file/content URI from an intent is accepted.
- Offline audio and DownloadManager receipt preferences are excluded from standard Android cloud backup and device transfer. Restoring stale system download IDs on another device must not produce false ready entries or affect unrelated transfers.
- No directory/feed refresh is required to open the stored library offline. Removing the app also removes its downloaded audio.
- Download only material the listener is entitled to retain. The app does not bypass paywalls, DRM or host restrictions.

## Catalogue

The candidate has eleven published RSS addresses across six sections: news/world, history, literature, science/space, learning/self-development and children's stories. Show search and category filters supplement the existing episode search. Added feeds remain a separate section. See [feed evidence](podcast-feed-evidence.md). BBC Tamil was researched but excluded after its feed failed the unchanged HTTPS enclosure gate; it is not counted as a working source.

This is a curated starting catalogue, not all Tamil audio on the internet. A directory address is not sufficient to ship: every source must pass the bounded live RSS/first-audio-prefix CI check. No claim is made about every episode's suitability, current publishing activity or availability in every country.

## Play Protect investigation — unresolved

### Evidence and limits

- Owner screenshots show **Harmful app blocked**, with the generic text **This app may be harmful**, for முதியவர் திரை and கதை சொல்லி. They do not show a specific malware family, detailed classification, APK version or APK hash.
- The reports followed updater integration. Both published source manifests request `REQUEST_INSTALL_PACKAGES` and declare a non-exported FileProvider for verified update APKs. This correlation does not establish the cause.
- Source inspection of `AppUpdates.kt` found official-repository and HTTPS redirect restrictions, bounded downloads, digest/size checks, package/version checks, signer pinning and Android installer confirmation. These controls protect aspects of the update flow; they are not malware-scanner results or proof that Google's classification is mistaken.
- The current CI signing material is tracked in this public repository. Matching that certificate does not prove exclusive publisher control. A private production-signing migration requires a separately reviewed plan to preserve Android upgrade continuity; no key is rotated here. This is an additional trust concern, not proof of the Play Protect trigger.
- Official launcher 1.3 asset SHA-256: `33070c5dbd50dc3a4be0d3239ac43eff5ab1e0313e52da8f87d5986816e86745`.
- Official podcast 1.3 asset SHA-256: `15770ec6cfcf671b81cb886bceff7d2b9f3ea620d23f795145f7940b5075bb3e`.
- The blocked files' actual bytes have not been collected from the phone or matched against those published hashes. No Play Protect or third-party scanner verdict for the candidate has been obtained.

### Capability reduction in this candidate

- Only launcher and podcast lose `REQUEST_INSTALL_PACKAGES` and their update-APK FileProviders.
- Their About pages route to official release pages in the browser. There is no in-app APK download/install UI for these apps. Manual browser release discovery replaces self-update; there is no automatic version check in that path.
- The shared legacy updater source remains compiled for other suite apps. This change does not pretend to remove every installer-related string from the binary or to hide behavior from a scanner.
- Neither package identity nor signing identity is changed to evade classification. No obfuscation, warning suppression or protection bypass is introduced.
- Source tests deliberately restore the permission/provider and require failure. CI also inspects the **actual merged release APK** permissions and manifest, not only source XML. This is a capability gate, not a Play Protect clearance gate.

### Before calling this resolved

- Identify the exact blocked APK/hash/version and retain the warning evidence privately.
- Build and inspect the candidate, dependencies and merged manifest; compare its signer and capabilities to the published artifact.
- Obtain an actual Play Protect-enabled device assessment. If blocked, stop rather than use Install anyway. CI compilation and a clean hash do not substitute for this step.
- Review Google's PHA/unwanted-software guidance. If review establishes compliance and the developer believes classification is erroneous, prepare the exact artifact/hash and request an official review. Do not assert compliance or submit an appeal on the owner's behalf before that review.
- Official guidance: https://developers.google.com/android/play-protect/warning-dev-guidance
- Official appeal entry: https://support.google.com/googleplay/android-developer/answer/2992033

No appeal has been submitted and no warning-clearance claim is made. No new stable APK is published by this change.

## Storage and Google Drive

At inspection the checkout was approximately 48 MB, uploaded screenshots approximately 4.6 MB, and the workspace filesystem had approximately 20 GB available (5% used). This is not currently a full-disk condition. Arena snapshot/patchset limits are separate from filesystem free space.

Google Drive is enabled for this conversation. A cloud copy does not extend the sandbox filesystem or automatically remove local files. No files were uploaded, made public or deleted as part of this assessment.

Recommended retention:

- Keep source, release notes and small reproducible evidence in Git; push the session branch to GitHub as a remote copy.
- Keep published APKs and official hashes in GitHub Releases rather than repeatedly downloading them into the checkout.
- If a Drive archive is requested, select a destination folder and scope first. Exclude credentials, signing material, `.git`, local SDK/tool caches and reproducible build directories. Preserve private device evidence separately with explicit approval.
- Record archive contents, sizes and SHA-256 hashes, confirm the remote copy, then remove only explicitly approved disposable local copies. Uploading without that cleanup cannot reclaim space.
- Do not remove source, release evidence or the repository root to solve a storage warning. Build artifacts already follow the repository's ignore rules.

## Validation completed

- Source commit: `5eeafc96e26f6c493dcf0f789da0b079820967a4`.
- [Candidate run 40480048906](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40480048906): launcher, podcast and Vasanthi all passed their debug JVM test tasks and signed release assembly. Both affected actual APKs passed the no-installer-permission/provider inspection.
- The podcast's live gate passed all eleven RSS feeds and sampled first-audio responses. These are endpoint smoke checks, not whole-episode listening or offline phone tests.
- Local checks: all eighteen localization gates, sound labels, all three module preflights, contrast/browser capability guards, eighty-eight Python tests and `git diff --check` passed.
- CleanSweep's local verifier was run before each commit and correctly refused to pass because local real JUnit XML is absent; no local Android SDK/JDK is available. The separate [broad CI run 40479496557](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40479496557) at preceding source `0294dd091e8c9483d12622fc1faf35affda475b2` passed its real CleanSweep and Vocal Remover regression gates. It is not described as a local pass or a phone test.
- Root README and website carry visible warnings for the existing affected stable downloads; they do not advertise candidate features as released.

## Validation still required
- Phone tests: all launcher button/screens at different font sizes; download completion, queued Wi-Fi behavior, retry, cancellation, app relaunch, offline-only playback in airplane mode, resume/seek, missing-file handling, deletion/storage recovery and uninstall cleanup.
- Play Protect classification remains a release-blocking unresolved report, independent of functional tests.
- Separate existing SMS Guard live neural-voice issue: https://github.com/universalrp2003/RP-Android-Apps/issues/49 . Its tests are not weakened by this work.


## Subsequent owner phone tests and versioned rebuilds

On 27 October 2026 the owner supplied Play Protect screens saying **This app looks safe** for both candidate APKs, launcher screenshots with readable controls and the default-home chooser, and reported **No warning everything working fine**. This is evidence for those tested candidate files, not a blanket security audit or a determination of the earlier classification's cause.

The owner then confirmed **Its playing offline**. Screenshots show a completed 4.94 MB episode download, airplane mode enabled, Wi-Fi off/mobile data unavailable, and playback at 2:58 of 5:23. This establishes one downloaded episode playing offline on the owner's phone. Relaunch, deletion/storage recovery, every episode, every permission and home-role completion are not separately established by these screenshots.

Owner-supplied SHA-256 strings transcribed from the file-manager screenshots:

- Podcast: `a64aa4281a463f901c14c4156051b5d5538aa8a369216865fa23780730a9849e`.
- Launcher: `f6ccbeaa442257e9828c7b8dfa97a9ca9b65e56379b366838c57f1cc940b9a1a`.

These are screenshot transcriptions, not independently computed hashes of files received from the phone, and have not been matched to a particular CI artifact. Originals remain private user attachments; no screenshot is copied into the public repository.

With the owner's approval, prepare version 1.4: podcast version code 6 and launcher version code 5. These are unpublished rebuilds, with package/signing identity unchanged. Existing published download links, site versions and official published hashes remain at 1.3 until actual publication. The exact rebuilt 1.4 APKs still need their own final phone checks; do not transfer the candidate's Play Protect verdict to new bytes.


## Final 1.4 phone confirmation and publication plan

The owner supplied version metadata showing launcher 1.4/code 5 and podcast 1.4/code 6, Play Protect screens saying This app looks safe for both, and readable launcher screenshots, then reported **All good** after being asked to perform the final checks. The earlier offline demonstration remains the direct offline screenshot evidence; the final report is an overall owner attestation, not additional screenshots of every scenario.

Publication uses immutable artifacts from successful run 40485186570, source 2dacaf567d0d884e3ad839ecad608c3f456067b3, recorded in releases/phone-tested-candidates.json. The release gates verify the pinned run and artifact IDs, unchanged compiled inputs, receipt source/package/version/size/hash, and the promoted APK's package, signer and capabilities. They fail rather than silently publish a fresh rebuild. Tests and release compilation still run, but their freshly built APK is replaced with the exact tested artifact before final verification/publication. Release tags explicitly target the publication commit. The earlier classification cause and public signing-trust concern are not declared resolved by the phone's scan result.


## Published result

Both version 1.4 promotion workflows passed and both releases are public. The detailed publication/digest record is in [published release digests](published-release-digests.md). Site, module READMEs and root download links now point to the actual published 1.4 assets. Historical unreleased/pending statements above describe the investigation timeline, not the final release status. No original phone screenshots were uploaded to the public repository.
