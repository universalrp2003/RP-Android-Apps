# Website usability candidate — not deployed

This change preserves the 14 published apps (12 non-Store + 2 optional: Tamilan Store and DailyVault), public Live Guard 2.75, immutable release filenames, bytes and SHA-256 values. It does not promote any newer app or held Store candidate.

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

Local results at preparation: 210 Python tests passed across `tools/tests`; app localization and public-release parity checks passed; 12 browser checks passed. Browser coverage includes 320/390/768/1280px, 200% root text size, keyboard focus/jumps, no-JavaScript catalogue access, paired language safety guidance, clipboard success/denial, synthetic matching/mismatching downloads, reduced motion and axe WCAG-tagged checks in all three language views, including expanded evidence. These are automated browser checks, not a full accessibility certification or physical Android phone test.

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

## Publication boundary

The current Arena branch contains unpublished Live Guard and held Store work. Opening a pull request from this branch into main would include those changes, violating the owner's website-only requirement. Do not open or merge that mixed PR, reset this branch, change its name, or publish the held code.

The website-only commit is prepared separately so it can be applied in a separate clean main-based session, reviewed through a website-only PR and merged only after CI. Existing Pages configuration is unchanged and deploys only from main. This document does not claim a PR, merge or live deployment has happened.
