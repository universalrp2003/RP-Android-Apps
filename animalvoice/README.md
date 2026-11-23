# Animal Sounds — experimental prototype (formerly Reply to animal)

Private-development Android application, not a public release. It uses an on-device YAMNet sound-event classifier to suggest up to three **broad sound categories**, including some dog, cat, cattle, crow, bird, rodent, insect and frog labels. It does not identify every species or determine intention, emotion, words, or what an animal understood. The scores are not calibrated probabilities and are not shown as certainty. Silence, weak results and dominant non-animal sounds produce “unknown”. Results are experimental and require real-world validation.

Version 0.13 removes the animal selector; the basic YAMNet model uses its general animal sound categories. Lizard is explicitly unsupported, and rat ultrasound requires appropriate hardware. The app does not currently produce animal-like replies for any selection; the recording replay is not a reply.

The microphone runs only after explicit permission and a tap, for three approximately 0.975-second windows. The PCM sample stays in memory; it is not written to a file or uploaded, and is cleared when the app stops. The app requests INTERNET and network-state permission for user-confirmed model downloads and the optional reference photo; there is no audio upload, background service, auto-listening, location, analytics, or audio history. The operating system and device can still affect privacy, microphone capture, performance, and playback. A brief user-confirmed replay of the same sample is **not an AI-generated reply**. No sound is played automatically. Avoid playback near animals, nesting sites and protected wildlife; stop and move away if stressed. A recording can contain nearby people's voices, so obtain consent and use headphones for personal listening.

A normal phone microphone/speaker cannot be assumed to handle rats' ultrasonic calls. There is no validated gecko/lizard classifier here and ants use communication channels beyond sound. No real conversation, generated species-specific response, veterinarian advice, public release, phone/device accuracy, or real animal validation is claimed.

## Model and third-party notices

`app/src/main/assets/yamnet.tflite`: YAMNet classification TFLite v1, SHA-256 `10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de`, 4,126,810 bytes. The embedded metadata names Apache License 2.0 and includes the AudioSet label list. Model developed by Google/TensorFlow; source <https://github.com/tensorflow/models/tree/master/research/audioset/yamnet>; original model <https://tfhub.dev/google/lite-model/yamnet/classification/tflite/1?lite-format=tflite>. The file was obtained from the public GitHub mirror <https://github.com/greatbody/CoughLogger/blob/main/app/src/main/assets/yamnet.tflite> and matched the independently documented TensorFlow Hub version hash. AudioSet ontology <https://research.google.com/audioset/>: CC BY-SA 4.0; attribution to Google. Apache 2.0 text is bundled in `assets/notices/APACHE-2.0.txt`. No animal recordings are bundled.

Build locally with Android SDK 36, JDK 17 and `./gradlew :animalvoice:assembleDebug`. Debug signing is for testing only, not an update or a public stable release. For a new app microphone permission requires explicit consent; never use this prototype as an update to another package. See `docs/RESEARCH.md` for evidence, licensing, limitations and open work.

## Retired BirdNET-Lite detector

Version 0.12 removed the old bird-only detector from the UI, and 0.19 removed its inference code too: that V2.4 model was the only reason the app carried the TensorFlow Flex runtime (`tensorflow-lite-select-tf-ops`), which added roughly 400 MB of native libraries for four ABIs. The bundled basic YAMNet fallback and the optional BirdNET+ V3 species model run on plain LiteRT and remain. An already installed old model is not automatically erased: Advanced settings still provides a remove button to reclaim its storage, and the licence texts remain bundled for anyone still holding that file (its terms were non-commercial CC BY-NC-SA 4.0).

## Experimental observation guide

Earlier versions included optional “what you saw” choices; these have been removed from the current interface. These are **manual, transient observations**, not model output, saved training data, a call translation or a reply. A referenced American-crow study is shown only as a research example; it does not identify the meaning of a local crow's call. The zebra-finch archive inventory workflow in `research/` is an investigation tool, not an AI model integrated into the app. No generated replies are provided yet.

### Optional version 3 recognition experiment (0.5)

The retired BirdNET-Lite file may be removed in settings. A separate experimental card accepts **both** the published BirdNET+ 3.0 `BirdNET+_V3.0_Global_12K_FP16_pruned.tflite` and `BirdNET+_V3.0_Global_12K_Labels.csv` from [Zenodo 24162518](https://zenodo.org/records/24162518). It checks the exact SHA-256 for each before replacing existing files; in version 0.8, a user-confirmed one-button download can fetch missing files from the publisher. The new option processes one three-second clip at 32 kHz offline and shows unverified top-scoring possible sources; it does not use the generic animal selection, location or dates. The cutoff is an uncalibrated gate, not an accuracy guarantee. The version 3 model and code are subject to their separate Apache 2.0 licence and acceptable-use guidance, included in the app assets; the legacy model remains CC BY-NC-SA. See `docs/RESEARCH.md` for positive-only testing and important caveats. Neither model predicts meaning or produces replies.

In 0.6 the v3 listen button and its results also appear near the top when both v3 files are installed. For a few explicitly mapped choices (crow, cat, cattle, Asian house gecko) the UI can mark a matching result, but **does not filter or reorder** the model's output; other animal choices are not mapped to v3 groups. Selecting an animal cannot confirm it. V3 House Crow has a Tamil display name. The exact `.tflite` download link is offered separately from `.onnx`; import status appears only in its own section and success is green. Installed files and the legacy model are retained on a compatible app update.

In 0.7, the optional v3 model can listen for 3 or 9 seconds. The 9-second mode checks three separate 3-second windows and shows the single window with the highest qualifying score, not an average, translation or calibrated probability. It may capture a call that starts later, but repeated searching can also increase false positives and cannot repair a signal buried in noise. Silence is skipped, no amplification or denoising is claimed, the longer clip remains in memory only, and Stop cancels recording/remaining windows. Real quiet calls, noise, target phones and accuracy still need testing. The app still has no online backend or upload.

### Version 0.8 network and identification details

With explicit confirmation, **Download model** fetches missing BirdNET+ 3.0 files from Zenodo over HTTPS (up to approximately 79 MB total) and verifies pinned SHA-256 hashes before installation. The button leaves already present files alone. It may use mobile data. If the device is offline, it waits only while the app remains open; Stop or leaving the app cancels, and a later retry starts the missing file again. Model files and an optional reference photo are downloaded only with confirmation; no recordings, account credentials or observations are uploaded. There is no hosted identification or AI reply yet. The app now requests internet and network-state permissions for this download, while local listening remains offline. Per-species notes for a few supported identities are general curated information, not descriptions of the specific call or inferred intentions.

### Version 0.9
The main screen presents BirdNET+ species listening and its download first, with the bundled basic sound classifier as an explicit fallback. Older BirdNET-Lite and manual file controls are under Other model settings; existing model files are not removed. The models are not merged and no automatic switching or reply generation is claimed.

Version 0.10 corrects download-status language switching and avoids showing a listening message while the model is downloading. No new animal reply model is included.

Version 0.11 stores both language versions of remaining runtime and legacy-model notices, so existing notices can switch with the language. This does not translate every species name; unknown Tamil names still fall back to the supplied English name. Geographic filtering is researched but not enabled.

Version 0.12 renames the app Animal Sounds, adds a custom icon and a concise model coverage explanation, and removes the obsolete bird-only controls. Counts are publisher class-list coverage, not tested identification accuracy, and include human and non-animal classes as explained. No semantic translation or generated reply is available. The application ID is unchanged.

### Version 0.13 reference photo
The selector and optional observation guide were removed to focus on automatic sound identification. As a first limited example, House Crow results offer one user-requested reference photograph from [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Corvus_splendens.jpg), photographer Pkspks, [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). It is not a photograph of the detected animal and does not verify the guess. A confirmation precedes each fetch: Wikimedia can see the request and IP address, but no recorded audio is uploaded. The photo is held in memory for the current result, not saved; other species do not yet have photos. The app checks HTTPS hosts, byte size and image dimensions; offline or failed requests show an error.

### Version 0.14 reference images
The large reply-warning card was removed; replay still asks for confirmation and warns about privacy and wildlife. For species results with a scientific name (except humans), a user can request a small reference image. The app sends that name to English Wikipedia, uses its freely licensed page image only if Commons supplies a supported CC BY or CC BY-SA licence and credit, then fetches the thumbnail from Wikimedia. Credits and licence links are shown. Images are not proof of the detection or photos of the recorded animal, and might be missing, inaccurate, outdated or unavailable; no audio is sent. The user's IP address is visible to Wikimedia. Images are kept only in memory. Unsupported or incomplete licences, missing attribution and failed lookups show an unavailable message.
A separate, consent-based button opens Google Images in the device browser with the candidate's scientific name. Google receives the name and IP address, but not the recording. Search results may be incorrect or copyrighted and their display depends on the browser and Google.

### Version 0.15
Reference photos are larger and each of up to three qualifying candidates can have its own image. Automatic loading is off by default and can be enabled after an explicit privacy notice, or turned off again using the switch. When enabled, species names are sent to Wikipedia and photos fetched from Wikimedia for future identifications; no audio is sent. Not every candidate has an available, suitable image. Three candidates are not guaranteed: in 0.15, only scores above the existing uncalibrated threshold in the strongest window were shown (see 0.16 below).

### Version 0.16
Nine-second captures now show up to three distinct qualifying labels ranked by their best score across all three non-overlapping windows; each label lists the windows where it appeared among that window's top three above the existing threshold. More windows can increase false positives, and scores are not calibrated probabilities. Switching automatic photos off cancels downloads and clears currently displayed reference photos; manual requests remain available after confirmation.

### Version 0.17
The first-run species-model setup is a clear card: it explains the one-time ~79 MB download, that the basic offline model is available in ⚙ Settings, and that audio never leaves the phone. Download progress is real byte-level progress (MB downloaded/total and percent; indeterminate when the server does not provide a length), with model and label files shown separately and a Stop control. The layout uses smaller card and spacing gaps, full-row radio targets and 48 dp+ controls so Tamil and English remain readable at large text sizes without unnecessary scrolling. CI now launches the first screen on the emulator at 2× font scale and checks that the download control is still reachable by scrolling, visible, tappable and at least 48 dp tall in both languages.

From 0.20 everything optional lives on a separate Settings screen, reached with the ⚙ button in the header. The main screen keeps only the identify controls, results and the “What can it identify?” explanation.

Reference photos now accept the eligible CC BY 2.0 licence with the same attribution and licence-link checks as CC BY/BY-SA 3.0 and 4.0. This fixes the Wood Frog (*Lithobates sylvaticus*) lookup: Wikipedia redirects the scientific name to Wood frog, and the selected Commons file is Brian Gratwicke's CC BY 2.0 photograph. The app also uses Wikipedia's `page_image_free` when it is a free image that differs from the lead image, and falls back to Commons `Credit` when `Artist` is blank. Non-commercial or no-derivatives licences, missing credit, non-HTTPS URLs and unexpected hosts are still rejected.

A Wood Frog guess in Tamil Nadu is treated as a possible geographic false positive, not as proof: the result shows a per-species caveat that the species is native to North America and that a similar local frog or a model error is possible. No location permission is requested, no region model is used, and no species is silently suppressed. The geomodel label-comparison research above is not a validated region filter.

The app continues to state that guesses and reference images do not confirm a species, scores are uncalibrated, and multiple windows can increase false positives. Audio remains on-device; only species names are sent when a reference photo is requested.

#### Signing and updates
From 0.17 the experimental APK is signed with the owner-backed RP release key already used by the repository's other Android apps. The private key lives only in GitHub Actions secrets (`RP_RELEASE_KEYSTORE_BASE64`, `RP_RELEASE_KEYSTORE_PASSWORD`); it is never committed. The release artifact is verified against certificate SHA-256 `7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d` and is not debuggable.

0.16 and earlier were signed with a per-run debug key, so a 0.17 APK signed with the persistent key **cannot update that installation**; Android will report a signature conflict. Uninstall 0.16 once first, which deletes the downloaded BirdNET model and the app settings. There is no model export and `allowBackup` is disabled, so re-download the ~79 MB model after installing 0.17. From 0.17 onward, same-key updates preserve app data, including the downloaded model and settings, as long as the application ID and signing certificate remain the same.

### Version 0.18
Fixes a Tamil layout bug reported from a phone test of 0.17: in Optional settings the side-by-side "Hide advanced settings" button squeezed the Tamil heading "விருப்ப அமைப்புகள்" into one-character lines. The heading and the Advanced settings toggle are now stacked, each full width, so no language can be squeezed mid-word at any font size. The large-text CI check also asserts that the heading keeps its width and that the toggle still expands the advanced section.

The success confirmations "File verified and imported." and "Model downloaded and verified." now fade after six seconds and are cleared when a new recording starts. Errors, waiting and stop messages stay until the next action; the Ready ✓ state stays in the card.

Each guess can show a small "Group:" badge (Bird, Insect, Amphibian, Mammal, Fish, Reptile, Arachnid, or Human-made sound / Natural sound, not an animal / Music, with Tamil labels) taken from the publisher's `taxon_group` column in the BirdNET+ V3 label table. It is display only: nothing is hidden, filtered or reordered, an unknown or missing group shows no badge, and the badge does not confirm the guess. Regional filtering stays disabled (see the geomodel research); the Wood Frog caveat still appears in Tamil Nadu.

0.18 is signed with the same key as 0.17, so it installs as an in-place update over 0.17 and keeps the downloaded model and settings.

### Version 0.20
Moves everything optional off the main screen into a Settings screen, opened by the ⚙ button in the header (TalkBack reads it as "Settings" / "அமைப்புகள்"). The main screen keeps only the header with the language switch, the species-model card (3 s / 9 s and the identify button), the download progress card while a download runs, and the results. The Settings screen holds: the auto reference-photo toggle and its privacy note, "Basic offline listening" (YAMNet) with the note that its result appears on the main screen, "Species model files" with the advanced import/download/remove controls, the "What can it identify?" coverage note, and an About card with the experimental notice and the BirdNET+/YAMNet licences. Both screens are Compose states of the single activity, so state (language, model status, results) survives the switch; the system Back button returns from Settings to the main screen instead of leaving the app.

The large-text CI test now also verifies that the main screen shows none of the moved items, that the gear opens Settings in both languages, that the advanced toggle still works from there, that the Settings headings keep their full width at 2× font scale, and that "← Back" returns to the main screen. `run-emulator-checks.sh` expects 12 tests.

0.20 is signed with the same key as 0.17–0.19, so it installs as an in-place update and keeps the downloaded model and settings.

### Version 0.19
A phone test of 0.18 showed a 461 MB installer. The cause was not an asset (bundled assets are 4.3 MB) but the TensorFlow Flex runtime `tensorflow-lite-select-tf-ops`, whose native libraries are about 100 MB per ABI and were packaged for all four ABIs. Only the retired BirdNET-Lite V2.4 model needed those ops (see the "Retired BirdNET-Lite detector" section); the bundled YAMNet model and the BirdNET+ V3 model run on plain LiteRT — the publisher's own minimal example and our host probes use LiteRT without a Flex delegate. 0.19 therefore removes the Flex dependency and the dead legacy inference code (file detection and removal stay), and the emulator test now also proves YAMNet runs without the Flex runtime. The workflow prints the release APK size, its largest entries and per-ABI native totals, and fails if the release APK exceeds 150 MB.

Human speech is still shown as a guess, now with a note that people talking nearby may be masking quieter animal calls in the same recording; it is not hidden or demoted, because the ranking is the model's output and the app does not silently filter results.

0.19 is signed with the same key as 0.17 and 0.18, so it installs as an in-place update and keeps the downloaded model and settings.

### Version 0.21

- **Built-in Guide and About, reachable from ⚙ Settings.** Settings now ends with a **Guide & about** card: the shared bilingual guide (12 topics: what this app does, the ~79 MB first download, 3 vs 9 seconds, reading results, reference photos, the models, privacy, settings) and **About this app** — team details (Ramesh prathap .R, RP Ideas; software assistance Arena.ai Agent Mode), links to the official website and the GitHub repository, the update policy, model licences, and the shared directory of every RP Ideas app (18, including this one and the optional Tamilan Store).
- **Daily update notice.** Once a day the app reads only public release metadata from the official website and shows an "update available" card when a newer release exists; tapping it opens the official website in a browser. Nothing is downloaded or installed by the app. AUDIO NEVER LEAVES THE PHONE.
- The interface language (English / தமிழ்) is now kept in the shared RP Ideas preference, so the app, its Guide and its About always agree.
- Version code 21, version name 0.21. Signed with the same key as 0.17–0.20, so it installs over 0.20 in place and keeps the downloaded model and settings.
- Model licences and credits moved from the Settings card to About; the experimental-prototype notice stays on the main screen and in Settings.

### Version 0.22

Fixes from the owner's phone screenshots of 0.21.

- **Tamil text at the same visual size as English.** Tamil glyphs are drawn larger than Latin letters at the same sp, so the Tamil title and heading broke into two lines on a phone. Tamil text is now drawn at 85% of the sp size through a density override; spacing and 48 dp touch targets are unchanged and the system font scale is still honoured.
- **Settings shows every detail directly.** The "Advanced settings" and "What can it identify?" dropdowns are gone; the species model file controls and the full coverage text are always visible.
- **About page.** Links carry only the official website and the GitHub repository. **Our apps** is a plain list of all 18 app names (Tamilan Store and DailyVault first) plus one button to the official website; per-app download links and the legacy mirror link are removed from this app only.
- **Reference photos on by default** (owner choice): after each identification only species names go to Wikipedia/Wikimedia Commons and a credited, free-licence photo is shown; no audio is sent. The Settings switch turns it off. This also applies to existing installations that never touched the switch.
- Version code 22, version name 0.22. Signed with the same key as 0.17–0.21, so it installs over 0.21 in place and keeps the downloaded model and settings.
