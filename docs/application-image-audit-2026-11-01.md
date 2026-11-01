# Website and Tamilan Store application-image audit

Date: 1 November 2026. Read-only image audit; no APK, release asset or public listing changed.

## Initial audit (before the prepared SMS image refresh)

- Website source/deployment: main `889ddd094ca7d08c012243b11146ae9e3586dfb1`, successful Pages run41221121385. Parsed all14 application cards. All53 referenced application images exist and pass Pillow image verification. This checks source/assets, not rendering on every browser.
- Store: all64 screenshots referenced by `tamilanstore/src/main/assets/screenshots/index.json` exist and pass image verification. Representative first images were visually reviewed for all12 covered apps; they show application screens, not the website concept illustrations.
- Compared all65 Store screenshot-directory files (index plus64 images) against GitHub's immutable Store0.7 candidate source710cf71: every Git blob hash matches. Public Store0.7 release429650592/APK694029025 retains SHA-256 `b6618ea5e101c23ce4aada39b28820e9840bd1eede03140ff2582255fe4fd8fe`. No claim of a fresh installed-Store runtime test or a new local APK extraction.
- Existing images are not necessarily current-version screenshots. Store deliberately labels its gallery “Earlier app screens.” Website SMS images explicitly say they predate the new consent/warning screens. The website image README contains outdated coverage descriptions; actual HTML and assets were used for this audit.

## Initial coverage

| Application | Website displayed images | Store screenshots |
| --- | --- | ---: |
| SMS Guard |9 real/earlier screenshots |5 |
| Live Guard |18 real screenshots |13 |
| Ramesh Radio |7 real screenshots |8 |
| Oor Arivippu |6 real screenshots |5 |
| PulseEQ |5 real screenshots |4 |
| RP Vocal Remover Studio |5 real screenshots |5 |
| Secret Message |1 labelled concept illustration, not an app screenshot |5 |
| Expense Tracker |1 labelled concept illustration, not an app screenshot |3 |
| Voice Notes |1 labelled concept illustration, not an app screenshot |3 |
| Vasanthi AI |No displayed application image |5 |
| Tamil Podcast Player |No displayed application image |4 |
| Elderly Launcher |No displayed application image |4 |
| DailyVault |No displayed application image |0 |
| Tamilan Store itself |No displayed application image |Not one of its13 ordinary app listings |

## Uploads needed / reuse opportunities

- **DailyVault:** real screenshots needed for both website and Store; Overview, Records and Backup would cover the main screens. Use demonstration records or hide personal financial information.
- **Tamilan Store:** a home/app-list screenshot is needed for its own website card; an app-detail screenshot is optional.
- The six website cards with concept-only/no displayed images (Secret Message, Expense Tracker, Voice Notes, Vasanthi AI, Tamil Podcast Player, Elderly Launcher) already have real images bundled in Store. Those assets can be reused in a later website correction without requesting duplicate uploads; keep older-screen labels where appropriate. This audit did not change the website.
- Optional refresh: SMS Guard1.1.16 Messages and Controls screenshots showing the current layout. Do not publish private sender names, numbers, messages, OTPs, account details or transaction amounts. No private screenshot supplied for troubleshooting is automatically authorized for public listing use.

## Owner SMS feedback

After1.1.16 publication, the owner reports “Its working fine no Google play warning.” This is successful owner-reported operation/no warning on that phone, not universal Google approval or an independently matched installed APK hash. Recorded separately in the1.1.16 verification document. No new SMS build/publication requested.


## SMS upload received — prepared, not published

The owner supplied eight SMS Guard screenshots for this listing refresh. Six contain settings, an empty Bills screen, or an empty Tamil Shield/link-check screen. The two showing the inbox/message previews and financial summary remain private under `/tmp`; neither was copied into the repository or a public gallery. No redacted/fabricated UI was substituted.

Prepared website hero: Controls with “Default SMS active — MMS is not supported”. Five additional website pictures: cloud-consent/permissions, speech choices, privacy/text size/quiet hours, empty Bills, and Tamil Shield. Store source gallery now contains the same six images in the same order, replacing the five older SMS images. JPEG metadata is stripped; original screen geometry is preserved. The screenshot shows the owner's enabled settings, not application defaults or proof of all security promises displayed in the UI.

Website source caption and scoped owner feedback updated; old SMS gallery references/files6–8 removed. All other application images are untouched. Updated source totals:50 website application-image references and65 Store screenshots. A regression verifies the website/Store SMS image bytes match, the gallery count is correct and the privacy/defaults caption remains.

**Not live yet:** no push, website deployment, new Store APK, version increment, release-asset modification or publication was performed. Existing Store0.7 embeds the old gallery; the prepared assets require a future, normally versioned Store update to reach installed users. The initial immutable Store0.7 source comparison above describes the original audited files, not these new images. Remaining uploads requested: DailyVault and Tamilan Store home/list screens.

Prepared-source checks:254 Python tests PASS; bilingual/source checks PASS; all50 website images and65 Store images decode; six new SMS images preserve original1080×2460 dimensions and contain no EXIF; whitespace check PASS. No native build or deployed-gallery runtime test was run for this image-only preparation.


## Tamilan Store upload received — prepared, not published

Seven owner-supplied Store screenshots are now prepared for its own website card: home/inventory hero, Store0.7 self-update check, and five additional catalog views covering Radio/PulseEQ, Vocal Remover/Oor, Secret Message/Expense Tracker, Voice Notes/Vasanthi, Podcast/Launcher. Six extra images are in the expandable gallery. No self-listing was added to Store's13 ordinary app records.

Privacy review: the two home screenshots include an embedded SMS inbox thumbnail, and the Secret/Expense view includes a financial-summary thumbnail. These three thumbnail rectangles are covered by solid opaque panels labelled “Private preview hidden”; no raw versions were copied to the repository. The originals remain under `/tmp`. All seven prepared JPEGs have EXIF removed and retain original1080×2460 geometry. The website explicitly discloses masking. No message text, account fragment or financial total from these thumbnails is published in the prepared versions.

The expanded self-update screenshot visibly reports installed Store0.7. Other screenshots show catalog/installed version labels, including the already-held Oor1.1.13 installation; these are not independent APK hash checks, trip tests, authorization to publish Oor, or evidence every listed app was installed/updated. Caption distinguishes capture-time versions from new releases. Existing accepted apps and public APK bytes remain untouched.

Updated prepared-source image totals:57 website application images and65 Store gallery images. **Not live:** collection/preparation only, no push/deployment/build/release. Only **DailyVault** screenshots remain requested; the Tamilan Store website-image gap now has prepared assets.


## Authorized publication phase — DailyVault complete and Store0.8 public

Owner subsequently authorized Drive retrieval, Store design/animation, build/publication and website verification. Seven empty/blank DailyVault screens fetched and reviewed; prepared in both galleries. All six remaining website illustration/missing-image gaps now use real existing Store screens, labelled older where applicable; Expense uses its permission screen, not financial summary. All14 website cards now have actual screenshots:70 image references. Store0.8 contains72 screenshots across all13 ordinary app galleries.

Signed Store0.8 was published without rebuild by41256209522; release430268186/APK696852937/checksum696852940,19,276,334 bytes/SHA3fae24373a6ead0bab0c0b2ad56a80a3b191de7a44ca69b50561deac3eea8474. Exact APK ZIP inspected locally only under `/tmp`; all72 asset bytes match reviewed source. Native update/language/motion/gallery and separate archive/installer gates passed. Public0.7/other APKs unchanged; no DailyVault rebuild published. Earlier “prepared/not live” entries describe their collection stage, not final release status. Website integration/Pages is the remaining gate; see [Store0.8 receipt](tamilan-store-0.8-verification.md).
