#!/usr/bin/env python3
"""Print the failing unit tests recorded in Gradle's JUnit XML.

Gradle's own failure message for a broken test is one line - "There were failing tests" - and it
names none of them. That turns a wrong assertion into guesswork, which is exactly what happened
while Expense Tracker was being brought up: three CI rounds were spent reasoning about which
assertion was wrong when the answer was sitting in build/test-results.

The XML is right there, so read it. Exit status is always 0: this is a reporter, not a gate, and it
runs on a build that has already failed.

    python3 tools/report_failing_tests.py
"""

from __future__ import annotations

import glob
import sys
import xml.etree.ElementTree as ET

# Each app's test results live under its own module directory; Secret Message keeps its Gradle
# project one level deeper, so it needs its own pattern.
PATTERNS = (
    "*/build/test-results/**/*.xml",
    "secretmessage/product/build/test-results/**/*.xml",
)

MAX_MESSAGE_LINES = 8


def result_files() -> list[str]:
    paths: list[str] = []
    for pattern in PATTERNS:
        paths.extend(sorted(glob.glob(pattern, recursive=True)))
    return paths


def main() -> int:
    found = 0
    seen = 0
    for path in result_files():
        seen += 1
        try:
            root = ET.parse(path).getroot()
        except Exception as exc:  # unreadable or half-written XML is not worth failing over
            print("(could not read %s: %s)" % (path, exc))
            continue
        for case in root.iter("testcase"):
            for bad in list(case.iter("failure")) + list(case.iter("error")):
                found += 1
                print("%s :: %s" % (case.get("classname"), case.get("name")))
                text = (bad.get("message") or bad.text or "").strip()
                for line in text.splitlines()[:MAX_MESSAGE_LINES]:
                    print("    " + line)
                print()

    if found:
        print("(%d failing test(s))" % found)
    elif seen:
        # Silence is the honest answer when the build never reached a test task: the caller's
        # failure was a source gate, a lint error or a compile error, and a "no failing tests"
        # line would be turned into a bogus ::error:: annotation by the workflows below.
        print("(no failing tests in %d result file(s))" % seen)
    # No XML at all means the failure happened before any test task ran, so there is nothing to
    # report here; the caller's own log tail names that failure. Printing anything would become a
    # misleading ::error:: annotation, which is how a stale source gate once looked like a test bug.
    return 0


if __name__ == "__main__":
    sys.exit(main())
