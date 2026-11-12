# Repository maintenance audit — 28 October 2026

## Completed safe source maintenance

- Corrected the top-level project scope to twelve published apps; distinguished the historical Secret Message preview from the released product without deleting security history.
- Archived the resolved Radio compile failure excerpt at [archive/ci/ci-candidate-radio-failure.md](archive/ci/ci-candidate-radio-failure.md). Original excerpt is unchanged; source `8f52da5`, failed run 40544324236. Fix `7de01fe`; successful follow-up build 40545795673 and publication run 40612310475.
- Added explicit retention where missing: 30 days for batch/candidate APKs and bundles; 14 days for transient build logs/unit-test reports. Existing explicit retention settings are preserved. This applies to future uploads, not retroactive deletion.
- Preserved all signing files, release assets, APK digests, device evidence, active preflight scripts and application folders. No version bumps or publication markers.

## Storage inventory (point-in-time GitHub API snapshot)

- 2250 CI artifacts, 90,834,855,314 bytes (84.60 GiB). Counts may change while CI runs.
- Oldest artifact: 2026-10-19T09:25:27Z. Expired artifacts: 0.
- None was older than 30 days on the audit date. No historical artifacts were deleted: several recent candidates are referenced in device evidence and are not safely classified as disposable by name alone. Release assets are a separate resource and were not touched.
- GitHub denied access to the repository retention-settings API (HTTP 403). Workflow-level retention is therefore used; repository-wide retention is not claimed changed.

## Remote branches

47 branch tips were compared to `main` via GitHub compare API. In the comparison `branch...main`, `ahead` means main contains the branch tip (merged cleanup candidate); `diverged` means the branch has unique work and must be preserved. 33 tips were contained, 13 diverged, and main was identical. Containment alone does not establish that an old Arena session can be removed: confirm session ownership first. No remote branch was deleted; this session may only push its own fixed branch.

| Branch | Tip | Main relative to tip | Action |
| --- | --- | --- | --- |
| `arena/a2dfe5ca-rp-android-apps` | `c7787f876e9f` | diverged | Preserve unique work |
| `arena/a7c60edc-rp-android-apps` | `2142752e67f4` | ahead | Review session ownership before deletion |
| `arena/a37668f4-rp-android-apps` | `200522f8687b` | diverged | Preserve unique work |
| `arena/ad072193-rp-android-apps` | `3b923a0f218b` | diverged | Preserve unique work |
| `arena/b0ad87e2-rp-android-apps` | `8c6830699938` | diverged | Preserve unique work |
| `arena/b748ffb8-rp-android-apps` | `2260ad29ed63` | ahead | Review session ownership before deletion |
| `arena/bb55d717-rp-android-apps` | `ca68a549364e` | ahead | Review session ownership before deletion |
| `arena/bbc17b29-rp-android-apps` | `7202d95e2a1b` | ahead | Review session ownership before deletion |
| `arena/c6aa42a2-rp-android-apps` | `a5dae440f0c2` | diverged | Preserve unique work |
| `arena/cc74b439-rp-android-apps` | `79e9e66103e0` | ahead | Review session ownership before deletion |
| `arena/d4fb2344-internetradio` | `3df75aa7cd49` | diverged | Preserve unique work |
| `arena/e4a30e2b-rp-android-apps` | `bbca906e5a1e` | ahead | Review session ownership before deletion |
| `arena/e6265243-rp-android-apps` | `7c07b8fc5dbc` | ahead | Review session ownership before deletion |
| `arena/0f8babd4-rp-android-apps` | `26589d2cedd9` | ahead | Review session ownership before deletion |
| `arena/1bf15d77-rp-android-apps` | `b0a32f2467eb` | ahead | Review session ownership before deletion |
| `arena/1c17a6e2-rp-android-apps` | `517216d4cd24` | ahead | Review session ownership before deletion |
| `arena/1cd26efe-rp-android-apps` | `d7b89f5268ba` | ahead | Review session ownership before deletion |
| `arena/1d5058a9-rp-android-apps` | `0ea5bc89f6ef` | diverged | Preserve unique work |
| `arena/3a3e0ed0-rp-android-apps` | `847f7821cb0d` | ahead | Review session ownership before deletion |
| `arena/3ed689fe-rp-android-apps` | `f3e90711b102` | diverged | Preserve unique work |
| `arena/05a52d45-rp-android-apps` | `a1bae1cd6ed1` | diverged | Preserve unique work |
| `arena/5df01e11-rp-android-apps` | `9f49b56f7c51` | ahead | Review session ownership before deletion |
| `arena/5e7aa3d4-rp-android-apps` | `96d4fdd40601` | ahead | Review session ownership before deletion |
| `arena/7b7dd467-rp-android-apps` | `8b0afa10eb57` | diverged | Preserve unique work |
| `arena/7d74486d-rp-android-apps` | `3a3f84a81f1e` | ahead | Review session ownership before deletion |
| `arena/8a900841-rp-android-apps` | `78281286cea3` | ahead | Review session ownership before deletion |
| `arena/9d1fb01d-rp-android-apps` | `1d5dc51f73aa` | ahead | Review session ownership before deletion |
| `arena/10d14182-rp-android-apps` | `1bc0f5effd09` | ahead | Review session ownership before deletion |
| `arena/74fc0d89-rp-android-apps` | `642912a255c3` | ahead | Review session ownership before deletion |
| `arena/86d1a321-rp-android-apps` | `3463db5303da` | ahead | Review session ownership before deletion |
| `arena/133b130e-rp-android-apps` | `76568c938bce` | ahead | Review session ownership before deletion |
| `arena/235b3d6f-rp-android-apps` | `b16fb6b60e6f` | ahead | Review session ownership before deletion |
| `arena/239e098d-rp-android-apps` | `1aaee62f1966` | ahead | Review session ownership before deletion |
| `arena/934fb9d0-rp-android-apps` | `5ee9e4effdc9` | ahead | Review session ownership before deletion |
| `arena/5642fac8-rp-android-apps` | `671f14a314b8` | diverged | Preserve unique work |
| `arena/7036baac-rp-android-apps` | `c62a943780bd` | ahead | Review session ownership before deletion |
| `arena/7064a6d8-rp-android-apps` | `0c19c88f42ec` | ahead | Review session ownership before deletion |
| `arena/08451d78-rp-android-apps` | `80459029e4a8` | ahead | Review session ownership before deletion |
| `arena/12471a95-rp-android-apps` | `790025f6de5c` | ahead | Review session ownership before deletion |
| `arena/43450e74-rp-android-apps` | `e828315cb752` | diverged | Preserve unique work |
| `arena/52015e3c-rp-android-apps` | `c006f56f2ac3` | ahead | Review session ownership before deletion |
| `arena/211009c2-rp-android-apps` | `5b98be5c9451` | ahead | Review session ownership before deletion |
| `arena/377705d8-rp-android-apps` | `6f6b8a17663c` | diverged | Preserve unique work |
| `arena/18371591-rp-android-apps` | `770beb8d043c` | ahead | Review session ownership before deletion |
| `arena/68812817-rp-android-apps` | `bd89141d7ede` | ahead | Review session ownership before deletion |
| `arena/93916273-rp-android-apps` | `5eb598c6157e` | ahead | Review session ownership before deletion |
| `main` | `cdddf0cb5130` | identical | Keep default branch |

## Notification / email hygiene

Repository subscription API access was denied (HTTP 403), so no account watch setting, email preference or inbox message was changed. Do not ask the owner for credentials or disable security alerts.

Owner steps, if GitHub mail is too noisy:
1. Repository → Watch → Custom: select the categories actually wanted (for example Issues and Releases). Keep replies/mentions so visitor requests are not missed.
2. GitHub Settings → Notifications: review email delivery and Actions notifications; choose failed workflows only where that option is available. Keep security alerts enabled.
3. Process old mail separately after checking it. Do not bulk-delete inbox messages just because they came from GitHub.

No automatic request-reply email service was added: visitors submit public GitHub Issues and follow the same thread. The owner replies on GitHub.

## Still open — not maintenance fixes

- Issue #49: intermittent null audio from live neural voice CI. Recent passing tests do not establish the cause or permanent recovery; do not weaken tests or add robotic fallback.
- Friend's Redmi/MIUI inbox refresh needs validation on that device, including a large history and visible progress. Owner success on another phone is not proof.
- Pending candidate product updates (e.g. PulseEQ) remain outside this maintenance scope and must not be published incidentally.

## Delivery

All maintenance is on `arena/6d87e1ec-rp-android-apps`. Main-only website deployment rules are unchanged. This document records an audit, not a promise of future service availability.

## Validation and permission limitations

119 Python tests passed. App localization, Cleaner localization, Cleaner core manifest and Cleaner next-update source checks passed. These are source checks, not a new Android compilation. The archived failure report was compared byte-for-byte against its original tracked file. Workflow YAML was syntax-parsed separately. No application source/version or APK release asset was changed.

An attempt to add a factual maintenance update to issue #49 was denied with “Resource not accessible by integration”; the issue remains open and no successful comment is claimed. Account notification/subscription and repository-wide retention API access were likewise denied. Remote branch deletion is not permitted by this session's branch constraints; the inventory is a review list, not an executed deletion list.
