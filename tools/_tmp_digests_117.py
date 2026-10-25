#!/usr/bin/env python3
"""Fill in the real SMS Guard 1.1.7 digest, size and run id across the prose docs.

Real values, read out of the release the publish run created:
`SMS-Guard-v1.1.7.apk`, 12,428,897 bytes, SHA-256
`b1f01bf25127f1e0e4bd69cea3fc2fda1f9f2f9a59b0ac429c9387e0bf4c1371`,
run 40202187916, published 2026-10-25T14:28:45Z.
"""
from __future__ import annotations

from pathlib import Path

ROOT = Path("/home/user/RP-Android-Apps")

SHA = "b1f01bf25127f1e0e4bd69cea3fc2fda1f9f2f9a59b0ac429c9387e0bf4c1371"
SIZE = "12,428,897"
RUN = "40202187916"
RUN_URL = f"https://github.com/universalrp2003/RP-Android-Apps/actions/runs/{RUN}"

TARGETS = [
    # --- CHANGELOG ---------------------------------------------------------
    (
        ROOT / "CHANGELOG.md",
        "## Published — SMS Guard 1.1.7 / code 13 (25 October 2026)\n\nA one-line layout fix",
        "## Published — SMS Guard 1.1.7 / code 13 (25 October 2026)\n\n"
        f"Published by [SMS Guard run {RUN}]({RUN_URL}): `SMS-Guard-v1.1.7.apk`, `{SIZE}` bytes, "
        f"SHA-256 `{SHA}`. A one-line layout fix",
        "changelog heading",
    ),
    # --- PUBLISHING --------------------------------------------------------
    (
        ROOT / "PUBLISHING.md",
        "Published by the standalone SMS Guard workflow. Two changes, both prompted by the owner's second",
        f"Published by [SMS Guard run {RUN}]({RUN_URL}): `SMS-Guard-v1.1.7.apk`, `{SIZE}` bytes, "
        f"SHA-256 `{SHA}`. Two changes, both prompted by the owner's second",
        "publishing entry",
    ),
    # --- digests -----------------------------------------------------------
    (
        ROOT / "docs/published-release-digests.md",
        "standalone SMS Guard workflow, with `SMS-Guard-v1.1.7.apk` carrying the layout fix",
        f"standalone SMS Guard workflow ([SMS Guard run {RUN}]({RUN_URL})), with "
        f"`SMS-Guard-v1.1.7.apk` ({SIZE} bytes, SHA-256 `{SHA}`) carrying the layout fix",
        "digests entry",
    ),
    # --- README latest bullet ---------------------------------------------
    (
        ROOT / "README.md",
        "- **SMS Guard 1.1.6 / code 12** ([`releases/sms-guard-v1.1.6.md`](releases/sms-guard-v1.1.6.md), `SMS-Guard-v1.1.6.apk`, `12,428,897` bytes, SHA-256 `3e129c31dd1556fc4cedfdfcd36f50147fda2edb3963ee10575bb244b064b8b8`, [SMS Guard run 40183935196](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40183935196)): a monthly-spend card on the Finance tab — this month against last month, the change in rupees and percent (suppressed when last month had no spending), the running daily average, the projected month end, the days remaining and a plain-language trend. The month ends at the last instant of the calendar month, not midnight on the last day. The card is deliberately the whole calendar month across every account, so it can read larger than the filtered period summary beneath it (on the owner's test phone, ₹10,427 against ₹10,302); the phone test also caught the projected figure wrapping mid-number, so every amount in the card is now a single non-wrapping line. OTP silence unchanged. `0` new permissions over `v1.1.5`.",
        "- **SMS Guard 1.1.7 / code 13** ([`releases/sms-guard-v1.1.7.md`](releases/sms-guard-v1.1.7.md), `SMS-Guard-v1.1.7.apk`, `12,428,897` bytes, SHA-256 `b1f01bf25127f1e0e4bd69cea3fc2fda1f9f2f9a59b0ac429c9387e0bf4c1371`, [SMS Guard run 40202187916](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40202187916)): the monthly-spend card's rupee amounts no longer wrap inside their own digits (the 1.1.6 card read \"₹12,92\" then \"9.48\" on two lines), and the note's claim that the card and the ledger can never disagree is withdrawn — the card is always the whole month across every account while the summary honours the selected account, so the two legitimately differ. `0` new permissions over `1.1.6`. The exact bytes were **not installed on a phone** before publication; the 1.1.6 build before them was, and its screenshots are what found the wrap.",
        "README latest bullet",
    ),
    # --- app README --------------------------------------------------------
    (
        ROOT / "smsguard/README.md",
        "**Current Stable Release:** [`v1.1.7` (`versionCode = 13`)](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/sms-guard-v1.1.7) — a one-line layout fix to the monthly-spend card and an honest correction to what the note claimed. The published 1.1.6 bytes are unchanged and still available.",
        f"**Current Stable Release:** [`v1.1.7` (`versionCode = 13`)](https://github.com/universalrp2003/RP-Android-Apps/releases/tag/sms-guard-v1.1.7) (`{SIZE}` bytes · SHA-256 `{SHA}`, [run {RUN}]({RUN_URL})) — a one-line layout fix to the monthly-spend card and an honest correction to what the note claimed. The published 1.1.6 bytes are unchanged and still available.",
        "app README release line",
    ),
]


def main() -> int:
    for path, old, new, label in TARGETS:
        text = path.read_text(encoding="utf-8")
        found = text.count(old)
        if found != 1:
            print(f"{label}: expected exactly one match, found {found}")
            return 1
        path.write_text(text.replace(old, new), encoding="utf-8")
        print(f"{label}: replaced")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
