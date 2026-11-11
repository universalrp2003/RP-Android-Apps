#!/usr/bin/env python3
"""Fail closed unless exactly the requested Android instrumentation tests passed."""
from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path


def verify(text: str, expected_class: str, expected_tests: set[str]) -> None:
    if not expected_tests:
        raise ValueError("No expected instrumentation tests supplied")
    starts: Counter[str] = Counter()
    finishes: Counter[str] = Counter()
    status: dict[str, str] = {}
    final_codes: list[str] = []
    for line in text.splitlines():
        if line.startswith("INSTRUMENTATION_STATUS: "):
            key, sep, value = line.removeprefix("INSTRUMENTATION_STATUS: ").partition("=")
            if sep:
                status[key] = value
        elif line.startswith("INSTRUMENTATION_STATUS_CODE: "):
            code = line.removeprefix("INSTRUMENTATION_STATUS_CODE: ").strip()
            name = status.get("test", "")
            if status.get("class") != expected_class or name not in expected_tests:
                raise ValueError(f"Unexpected instrumentation event: {status!r}, code={code}")
            if code == "1":
                starts[name] += 1
            elif code == "0":
                finishes[name] += 1
            else:
                raise ValueError(f"Instrumentation test {name} failed or was skipped (status {code})")
            status = {}
        elif line.startswith("INSTRUMENTATION_CODE: "):
            final_codes.append(line.removeprefix("INSTRUMENTATION_CODE: ").strip())
        elif line.startswith("INSTRUMENTATION_FAILED:") or line.startswith("INSTRUMENTATION_RESULT: shortMsg="):
            raise ValueError(f"Instrumentation runner failed: {line}")
    required = Counter({name: 1 for name in expected_tests})
    if starts != required or finishes != required:
        raise ValueError(f"Tests did not all start and finish exactly once: starts={starts}, finishes={finishes}")
    # Android's Activity.RESULT_OK is -1; an explicit final code is required.
    if final_codes != ["-1"]:
        raise ValueError(f"Instrumentation did not report a successful final result: {final_codes}")
    if "FAILURES!!!" in text or "Process crashed" in text:
        raise ValueError("Instrumentation reported a failure or a crashed process")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", type=Path)
    parser.add_argument("--class-name", required=True)
    parser.add_argument("--test", action="append", required=True)
    args = parser.parse_args()
    try:
        verify(args.log.read_text(errors="replace"), args.class_name, set(args.test))
    except (OSError, ValueError) as error:
        raise SystemExit(f"Instrumentation verification failed: {error}") from error
    print(f"PASS: {len(args.test)} required {args.class_name} test(s) passed")


if __name__ == "__main__":
    main()
