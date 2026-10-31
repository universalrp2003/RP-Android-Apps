# Ramesh Radio station and tab-order investigation — 31 October 2026

## Owner report

Screenshot names **Tamil80’s Radio**, paused at0:02 with Tamilradios branding. Owner reports audio telling them to uninstall this app/use the station's own app, and explicitly has not listened to every channel. Shipped record points to psrlive2.listenon.in/80?station=tamil80shitsradio.13 shipped entries use the psrlive listenon provider family, some with baked directory query parameters. This identifies a reported content issue, not independent listening evidence for all13, not malware and not proof that all Tamilradios-branded streams are restricted.

## Candidate solution, not an access bypass

Temporarily hold psrlive*.listenon.in for content review across manual taps, directory hits, automatic last-station resume, restored queues and the media-service data-source path. Visible review labels explain the hold. Preserve user favourites/history/custom entries and the original station records. Other broadcasters using a Tamilradios homepage/logo are unaffected. Do not fabricate replacement URLs, rename another station as Tamil80’s, strip spoken promotions, renew access tokens or spoof the provider's app. The app does not require uninstalling.

Stream monitoring of504 shipped URLs plus bounded fresh directory discoveries is performed in Actions by tools/audit_radio_catalogue.py. Audio metadata reachability is NOT listening, programme identity, rights clearance, full decoding, availability on every phone/network or absence of promotions. Failed/region-blocked/time-limited entries remain unchecked instead of being deleted. New discoveries are only added if an actual current audio-metadata probe succeeds, without asserting public directory listings establish redistribution rights. Initial hosts are screened; this audit is not a complete redirect/HLS/DNS-rebinding sandbox.

## Whole tab ordering

More → Arrange whole tabs: independent persistent stable-ID orders for bottom navigation and station category tabs,48dp accessible earlier/later buttons, per-list reset. Invalid/duplicate/obsolete IDs are removed and future default tabs appended. The selected tab, active category, playback and existing record keys are unchanged. The first six category chips are chosen from the saved order; expanded chips show the remainder.

## Scope and verification holds

Candidate radio1.30/code30 only. No public release/main merge. SMS1.1.14 and Oor1.1.13 source and exact selected phone-test artifacts remain unchanged. Existing draft PR73 also contains their held work; do not merge it under radio-only authorization. Published LG2.81/site/Store metadata remains untouched. Station audit results, additions and Android unit/lint/APK signing evidence are to be recorded in PR73 after completion. No candidate phone acceptance yet.


## Executed full catalogue probe receipt

Actions41201343767/job124537607608 SUCCESS (after first setup run41201302285 lacked FFprobe; explicit dependency installation corrected that setup failure). Checked all504 shipped URLs plus40 fresh candidates:462 shipped audio-metadata reachable,42 unavailable,39 discovery probes passed.13 reachable psrlive provider entries are still held because a successful connection cannot clear the owner's content report.11 duplicate discovery IDs/names/HLS playlist variants were removed before adding28 additional directory entries,532 total. This is not proof of28 unique programmes or advertising-free content. No original504 records were deleted. Compact data/provenance is in radio-catalogue-audit-2026-10-31.json; original audit report blob447a9fd514a6f34234186e6b89be8321c3be3735, raw proposed catalogue bloba38ef72c1e94b313cf37c53cfaa50ae61f1e33c3. The raw report's39 additions count is pre-deduplication, not the shipped addition count.
