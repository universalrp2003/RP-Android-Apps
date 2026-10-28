# தமிழன் — Tamilan Android application store (preview)

Owner-approved isolated first-party store for the twelve RP apps. Not Google Play, not an official F-Droid client or repository. No public release, store listing or device compatibility certification is claimed.

## Implemented preview

- Tamil-first with persisted English switch, searchable bundled catalog of twelve public releases (snapshot 28 October 2026), installed-version display, release/help/privacy links.
- Single user-initiated download with progress, cancel and retry. APKs stay in private cache, one at a time. No account, ads, analytics, other-developer uploads or background inventory upload.
- HTTPS-only canonical release URLs; bounded redirect hosts, byte limit, size and SHA-256 checks. Package/version/minimum SDK and pinned single signer checked before handoff; installed signer and downgrade checks fail closed. Android's package installer performs final platform validation and user confirmation.
- Unknown-source settings open only when the user selects Install. The user must return and tap Install again; never disable Play Protect. FileProvider exposes only the verified cache directory, not all storage. Package visibility is scoped to twelve packages; no QUERY_ALL_PACKAGES or storage permission.
- Process termination requires a new download; preview has no background/resumable download or notifications. In-flight work survives configuration changes in a ViewModel, not process death.

## Explicit remaining gates

1. Android CI build, lint and tests; no local SDK/build verification claimed.
2. Exercise redirect, truncated/wrong-size/hash APK, wrong package/version/signer, downgrade, cancellation, low storage, installer rejection, configuration change and process death on devices. Revalidate each public app's signing identity; pinned-signer mismatch blocks installation, it must not be bypassed.
3. Test Android 8 and current Android, large fonts, screen readers and package visibility. A displayed installed version does not prove update compatibility.
4. Current catalog is BUNDLED, not a remotely trusted live update feed. It clearly labels its date; newer releases require a new snapshot/store build. Signed remote catalog, freshness/rollback protection, automatic update discovery, categories, richer descriptions and screenshots remain next-stage work. No private key/token may be bundled for this.
5. Review installer-permission store policies before seeking Play listing. Separate privacy policy, release signing continuity, owner phone sign-off and a standalone publication gate are required before shipping.

The preview is intentionally not in the twelve published-app directory or website release table. Existing apps' update mechanisms and releases are unchanged. `.github/workflows/tamilanstore-preview.yml` builds only a test artifact and has read-only repository permissions.

## Build

`gradle :tamilanstore:testDebugUnitTest :tamilanstore:lintDebug :tamilanstore:assembleDebug`

## Preview 0.2 — installed-state follow-up

Owner screenshots show preview 0.1 running in Tamil/English, download progress and an Android installer prompt. They do not independently establish completed installation of every app. The unknown-source warning is a sensitive-permission disclosure, not an APK malware verdict; do not bypass Play Protect or recommend uninstalling a newer candidate just to test the store.

0.2/code 2 refreshes local installed versions on return to the foreground and package added/replaced/removed broadcasts. Installed apps get Open (or Android app info if no launch activity). Current/newer versions are not offered another Install/Download action. Older numeric versions get Update with installed/offered version details; unrecognized preview labels are not guessed into update prompts. Signer mismatches block store updates and warn against uninstall/data loss. The model also blocks downloads of same/newer/unknown versions, and archive versionCode/signer checks remain mandatory. All/Installed/Updates filters compare only the bundled catalog snapshot, NOT a live Google Play-like remote feed.

The screenshot's Vasanthi 1.1.5 and Podcast 1.6 are newer than the public catalog entries 1.1.4/1.4, so this preview intentionally offers Open, not a downgrade. Existing applications' self-update mechanisms remain unchanged. Android/device validation of this new revision is pending the preview workflow and owner review; cancellation is not treated as an install success.

## Preview 0.3 — compact home and quiet opt-in reminders

Main screen keeps header, search, filters and app actions; long privacy/catalog/verification text is behind Help/About (collapsed by default). A lightweight local vector illustration inspired by Tamil Nadu temple/palm scenery decorates the header, with no image download or tracking. Contact uses ACTION_SENDTO for `universalrp2003@gmail.com`; no email is sent automatically and a mail app is required.

Daily reminders are OFF by default. Enabling them requests notification permission where required, schedules a non-exact 24-hour WorkManager check, and compares installed versions only with the bundled snapshot. No live release feed is implied. No notification for zero updates, permission denial, or failures. One aggregate silent low-importance notification at most per rolling 24 hours; no vibration/sound requested. Android/OEM settings and background limits may affect delivery; users can switch it off. WorkManager may add normal wake/boot/network-state permissions through manifest merge; it does not receive permission to read private user data. Old app self-updaters remain unchanged.

The reported Secret Message signer mismatch exposed a real catalog pin assumption: Secret Message uses a separate established release key. Preview 0.3 pins its exact certificate from `secretmessage/signing/certificate-sha256.txt` / its updater (58e370e4…bc04f08) and retains the legacy pin for the other apps. Unknown/mismatched keys still fail closed; this is not trust-on-first-use or bypassing a signature check. Device confirmation is required before claiming the reported mismatch resolved.

## Preview 0.4 — galleries, request link and store release check

- Live Guard's Tamil store name is கைபேசி காவலன்; the shared sister-app directory uses the same owner-approved name.
- Dedicated visible Request an app action opens the website public request box (GitHub sign-in/submission required). Email contact remains unchanged.
- A collapsible Store update panel shows this installed store's actual version and lets the user query public GitHub releases. Only non-draft/non-prerelease `tamilan-store-v<version>` releases with the expected APK filename qualify. A higher numeric stable version links to its canonical official release page in the browser. No public store release is invented; preview CI artifacts are not stable releases. This is discovery + browser handoff, NOT an in-app verified self-installer or signed live catalog. Failed requests are not represented as “up to date.” Existing apps' self-updaters are unchanged.
- Bundled screenshot strips use only existing public website galleries: SMS Guard, Live Guard, Radio and Oor Arivippu have five each; PulseEQ and Vocal Remover have four each. These are earlier app screens, not claims of the current version's UI. No concept cards are labelled screenshots. The remaining six apps explicitly say real screenshots are pending; genuine 4–5 screen sets must be supplied before every card can have a gallery. Thumbnails are about 82×146 dp; tap for a larger image. Files are resized copies, with no remote image tracking. No private screenshots containing API keys, expenses or messages were imported.

Still preview-only; new compilation and device review are required. Self-update discovery queries at most the latest 100 GitHub releases, so older store releases outside that window may be absent; this limitation is not a guarantee of universal latest-version discovery.

### Owner screenshot refresh — 28 October 2026

SMS Guard's five bundled images come from the owner's 09:55 upload batch: voice controls (09-55-51-589), inbox (09-55-19-395), link-safety tests/API settings (09-55-32-070), bill reminders (09-55-46-974), and locked finance screen (09-55-40-675). These are resized JPEG copies stored inside the repository, not references to temporary upload paths. The owner explicitly requested the inbox image and clarified that the API settings screen shows explanatory/saved-key status text, not an API key value. The inbox includes personal message/transaction snippets; its inclusion is owner-authorized, not a claim that it contains no personal information. No screenshot-blocking protections in Secret Message or DailyVault were changed. Other galleries still use the earlier public website images pending the owner's uploads.

Oor Arivippu's five gallery images were also refreshed from the owner's 28 October uploads: Tamil home/destination (10-01-54-146), English destination choices (10-03-58-306), voice/travel settings (10-02-18-763), announcement history (10-02-24-984), and Tamil help (10-02-38-978). Resized copies are bundled in repository assets, independent of temporary upload storage. These owner-supplied screens show location names and recent announcement times.

Live Guard / கைபேசி காவலன்: all 13 owner-supplied screenshots from the 28 October 10:05–10:09 upload batch are now bundled, in chronological order (home first), replacing the five earlier public-gallery images. The horizontal strip supports all 13 with tap-to-enlarge. Screens include weather location, mobile readings, installed-app permission listings, assistant welcome, voice settings, plan configuration and news; they are owner-authorized examples, not independent verification of the displayed capabilities. No actual API-key value is visible in the supplied security status screen.

Ramesh Radio: all eight owner-supplied screenshots from the 28 October 10:12–10:13 upload batch replace the earlier public-gallery images. Chronological order starts with the station grid, followed by news radio, local songs, equalizer/balance, EQ bands, clear-sound settings, More/sleep timer and rain-alert dialog. Resized JPEGs are bundled in repository assets, not temporary upload references.

PulseEQ: four owner-supplied 28 October 10:15 screenshots replace the earlier gallery: bands/preamp, presets, local player and background/coverage settings, in that order. Resized JPEGs are stored in repository assets. The screenshots show a device UI with some low-contrast headings; they are retained as supplied, not edited to imply a UI fix. They do not establish system-wide effect availability or correspond necessarily to the catalog stable version.

RP Vocal Remover Studio: five owner-supplied 28 October 10:17–10:18 screenshots replace the earlier gallery: English home/system watch, English processing/model controls, Tamil processing controls, About/privacy and Guide, in chronological order. Resized JPEGs are stored in repository assets independently of temporary uploads. Device readings and model-ready status shown are examples from the owner device, not general compatibility or output-quality verification.

Kathai Solli / Tamil Podcast Player: four owner-supplied 28 October 10:22 screenshots added as the podcast gallery. Main/continue-listening screen first (10-22-19-827), then science fiction/news/history (10-22-34-065), science/space/learning (10-22-29-448), and children's story catalogue (10-22-27-057). Resized JPEG copies are stored in repository assets. Displayed catalogue entries and listening position are owner-device examples, not playback or content-availability verification.

Expense Tracker / செலவு கண்காணிப்பு: three owner-supplied 28 October 10:25 screenshots added: monthly dashboard (10-25-50-256), English SMS permission explanation (10-25-09-072), and Tamil SMS permission explanation (10-25-06-634). Resized copies are bundled in repository assets. The dashboard includes transaction amounts and counterpart names from the supplied screen; owner provided it for the gallery. Only three real images have been supplied, not a claimed four-to-five-image set.

Owner-requested privacy edit: Expense Tracker dashboard counterpart names are partially masked as CH***** R*** and HE******* P. using opaque replacement (not reversible blur). Amounts and the existing masked bank-account suffix remain unchanged. Only the gallery copy was edited; the original temporary upload and app data were not modified.

Elder Launcher: four owner-supplied 28 October 10:29–10:30 screenshots added: main large-button home, additional controls, emergency confirmation (108), and lower home/actions. Personal contact names are partially masked in all four gallery copies, including behind the confirmation dialog; relationship labels remain unchanged. Originals and app data are not modified. Resized JPEG copies are stored in repository assets.

Vasanthi / என்னை சுற்றி: five owner-supplied 28 October 10:32–10:34 screenshots added: English permission setup, Tamil stopped/history controls, Tamil Guide, floating speech panel over the app, and floating panel over a gallery photo of electronics. The last upload has the gallery package filename because it demonstrates the overlay on another app; it is indexed under Vasanthi, not as a separate app. Resized copies are bundled in repository assets. Visible recognition text is an owner-device example, not independent accuracy verification.

Voice Notes: three owner-supplied 28 October 10:37 screenshots added: English notes list, Tamil notes list and Tamil edit dialog. Resized copies are bundled in repository assets. The displayed listening error is retained as supplied; screenshots do not establish working dictation or audible HD playback. Owner requested that the next Voice Notes build include compact About, Guide and links to other apps/website; this follow-up is recorded in docs/voicenotes-next-build.md, not implemented by this gallery-only change.

Compact search follow-up: the permanent full-width search field is replaced by a Search action in the horizontal toolbar. It opens a single-line search dialog; Show results closes it while keeping the filter, Clear removes it, and dismissal retains the query. Existing name/Tamil-name matching is unchanged. Android/device verification pending.

Secret Message: five owner-supplied camera photos (WA0019–WA0023) added: home, invite waiting, timer/tones, About/app lock and device/guide. Invite URL/token and device identity are covered with opaque redaction in gallery copies before bundling. Originals remain only in temporary uploads; no screenshot protection changed. Photos are owner-device examples, not independent security verification.
