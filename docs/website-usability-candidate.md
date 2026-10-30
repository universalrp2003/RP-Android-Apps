# Website usability release preparation

This change preserves the 14 published apps (12 non-Store + 2 optional: Tamilan Store and DailyVault), the verified public Live Guard 2.79 APK, immutable release filenames, bytes and SHA-256 values. Live Guard 2.79 was promoted separately from the immutable approved candidate. Tamilan Store 0.6 was already public; its module has no pending delta against main and no new Store APK is released.

## Changes

- Four browsing categories, counts, app jump selector, static card anchors and table-to-card links. Filtering affects both the table and cards; a jump reveals a hidden card. Without JavaScript, all apps and bilingual safety text remain available.
- Both/Tamil/English preference, default Both; only an explicit preference is remembered locally. Paired summaries and safety guidance follow the preference. App names remain bilingual and technical history/evidence retains its original language; this is not a claim that every technical sentence has a complete translation.
- Current-release summaries and exact-download testing disclosures remain visible. Older feature/device history is labelled as history, rather than implying that historical phone testing covers the current APK. Full release notes remain linked.
- Static descriptive APK download names, accessible copy-SHA buttons for every card, exact clipboard value, honest manual-copy fallback, and live feedback. A copied hash is not file verification. A matching downloaded-file hash is not security approval. The existing streaming/hash-mismatch stop is retained and tested with tiny synthetic bytes only.
- Real GitHub release `published_at` timestamps in UTC, a small local social-preview PNG, readable focus/contrast, wrapped card headings and a keyboard-scrollable table at large text sizes.
- One prominent promise ticker instead of two duplicate tickers, retaining pause, reduced motion and bilingual content. Existing screenshots, lazy loading, expandable sections, metadata and request form remain.
- Conditional signing migration: same-key users update normally and must not uninstall working installations. Only affected old-key installs need backup/export and migration. Security warnings must not be bypassed.

## Date provenance and maintenance

`site/release-dates.json` records the `published_at` values retrieved from the GitHub Releases API for the exact public tags. Dates are not inferred from source commits, tag creation, APK versions, build dates or the computer clock. The table labels these dates UTC.

Check offline:

```sh
python3 tools/update_website_release_dates.py
```

After changing a public release link, refresh from GitHub (requires the configured `gh` connection), review the diff, then rerun the tests:

```sh
python3 tools/update_website_release_dates.py --refresh
```

The helper refuses draft/missing releases, invalid UTC timestamps, mismatched release rows and partial refreshes. It does not change app versions, files, hashes or publication settings.

## Validation

Local results at preparation: 211 Python tests passed across `tools/tests`; app localization and public-release parity checks passed; 12 browser checks passed. Browser coverage includes 320/390/768/1280px, 200% root text size, keyboard focus/jumps, no-JavaScript catalogue access, paired language safety guidance, clipboard success/denial, synthetic matching/mismatching downloads, reduced motion and axe WCAG-tagged checks in all three language views, including expanded evidence. These are automated browser checks, not a full accessibility certification or physical Android phone test.

Reproduce with development-only dependencies (never needed by the deployed site):

```sh
npm ci --ignore-scripts --prefix tools/website-tests
# Install OS browser dependencies if your environment needs them:
(cd tools/website-tests && npx playwright install-deps chromium)
python3 -m http.server 8000 --bind 0.0.0.0 --directory site
# In another terminal:
node tools/website-tests/usability.mjs
```

`RP_SITE_URL` and `RP_TEST_MODULES` can point at a different local server or development dependency directory. Tests prohibit real APK downloads/external requests and use synthetic bytes for checksum branches. The new read-only website CI workflow does not deploy or publish apps.

## Owner-approved publication boundary (30 October 2026)

The owner explicitly selected full current-branch merge and release completion, superseding the earlier website-only PR restriction. Review the complete branch diff, require green CI, then merge the active Arena branch into main. Do not create or switch to another branch, and do not replace public APK files.

Live Guard 2.79 is public from immutable promotion run41052024326/source8bd7cf9 (14,479,951 bytes; SHA-256`95a050510786f1e4a34746cb0b3dcf5d3a3a032ec61052a94745d8f7719941ac`). The website download, JSON-LD, version table, shared public-version metadata and live catalogs now reference these verified bytes. Historic APK/ZIP mirrors are preserved. The Store catalog compatibility test now distinguishes the app's intentionally older bundled fallback from the served feed, checks monotonic versions/identity, and pins live-feed entries to the website/Pages receipts.

The three newly supplied owner screenshots are included in Live Guard's gallery after cropping out phone status/navigation bars. The visible testing disclosure distinguishes owner UI acceptance from installed-hash matching, audible playback and Play Protect clearance.

This is a predeployment preparation record: a successful branch Pages build is not deployment. Verify the merged-main Pages deploy job and live HTML/catalog values before reporting that the website is live.
