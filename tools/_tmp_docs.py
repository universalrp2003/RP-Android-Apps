#!/usr/bin/env python3
"""Record in CHANGELOG.md that the renderer now compiles green, and why the tests went green."""
from __future__ import annotations

from pathlib import Path

CHANGELOG = Path("/home/user/RP-Android-Apps/CHANGELOG.md")

OLD = (
    "check that turns that into a source-time\n"
    "failure instead of something an owner has to spot on a phone. Because there is no JDK,\n"
    "Gradle or Android SDK here, CI compiles this; no new release was built for it.\n"
)
NEW = (
    "check that turns that into a source-time\n"
    "failure instead of something an owner has to spot on a phone. Because there is no JDK,\n"
    "Gradle or Android SDK here, CI compiles this; no new release was built for it.\n"
    "\n"
    "**CI verification of that compile.** Two runs were needed. [Build APK 40198820056](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40198820056)\n"
    "and the SMS Guard Test APK both failed at `:smsguard:compileReleaseKotlin` on a\n"
    "`'when' expression must be exhaustive` error, because `SubBullet` had been added to the enum\n"
    "without a branch in the `when (kind)` that strips a line's prefix; then the next attempt failed\n"
    "on an `import androidx.compose.foundation.layout.weight` that does not exist, `weight` being a\n"
    "`RowScope` member extension rather than an importable function. Both are fixed, and\n"
    "`verify_kotlin_when_exhaustiveness()` now reads the sources so neither class of mistake can\n"
    "reach CI silently again. [Build APK 40200070221](https://github.com/universalrp2003/RP-Android-Apps/actions/runs/40200070221)\n"
    "then passed, as did the SMS Guard Test APK, the OorArivippu test APK, the Secret Message build\n"
    "(the one that compiles the fork's private parser copy) and the website deploy. The two\n"
    "live-network neural-voice suites that were red on `130ce8c` and `4e2a728` also passed here with\n"
    "no Kotlin change to them, which is further evidence those failures were the Azure speech\n"
    "endpoint and not this branch.\n"
)


def main() -> int:
    text = CHANGELOG.read_text(encoding="utf-8")
    found = text.count(OLD)
    if found != 1:
        print(f"expected exactly one match, found {found}")
        return 1
    CHANGELOG.write_text(text.replace(OLD, NEW), encoding="utf-8")
    print("CI verification recorded")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
