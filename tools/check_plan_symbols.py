#!/usr/bin/env python3
"""Resolve cross-object references in the Live Guard plan code, without a compiler.

Not a Kotlin type checker. It answers one question: does every `Foo.bar` these files call
actually exist as a declaration in the module? A missing one is exactly the class of mistake
that used to need a CI round-trip to find - it caught `PlanReadingPolicy.allowanceGb` (that
lives on PlanPolicy), a missing import, and `plan.boosterLastDay(plan)` (also PlanPolicy).

Run it after touching any file in CHANGED; tools/tests pins it so it cannot rot.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "cleaner/src/main/java/com/universalrp/cleansweep"

CHANGED = [
    MAIN / "ui/MyPlanScreen.kt",
    MAIN / "notify/ChargeNotifier.kt",
    MAIN / "notify/StatusPill.kt",
    MAIN / "data/PlanReadingPolicy.kt",
    MAIN / "data/DataUsageTracker.kt",
    MAIN / "MainViewModel.kt",
    MAIN / "widget/CleanSweepWidget.kt",
]

# Every Kotlin source in the module, so a declaration can be found wherever it lives.
sources = {p: p.read_text(encoding="utf-8") for p in MAIN.rglob("*.kt")}


def declarations(name: str) -> set[str]:
    """Member names declared in any `object`/`class` called [name] in the module."""
    members: set[str] = set()
    for text in sources.values():
        for block in re.finditer(rf'\b(?:object|class)\s+{re.escape(name)}\b', text):
            start = text.find("{", block.end())
            if start == -1:
                continue
            depth = 0
            for i in range(start, len(text)):
                if text[i] == "{":
                    depth += 1
                elif text[i] == "}":
                    depth -= 1
                    if depth == 0:
                        body = text[start:i]
                        members |= set(re.findall(r'\b(?:fun|val|var)\s+(?:<[^>]+>\s*)?(\w+)', body))
                        members |= set(re.findall(r'\b(?:enum class|data class|class|object|interface)\s+(\w+)', body))
                        break
    return members


# Members that live on the plan objects and are easy to call through the wrong receiver.
PLAN_POLICY_MEMBERS = set()
for text in sources.values():
    if "object PlanPolicy {" in text:
        body = text[text.index("object PlanPolicy {"):]
        PLAN_POLICY_MEMBERS = set(re.findall(r'\bfun\s+(\w+)', body[:body.index("\n}\nobject PlanStore") if "\n}\nobject PlanStore" in body else len(body)]))

cache: dict[str, set[str]] = {}
problems: list[str] = []

for path in CHANGED:
    text = sources[path]
    for obj, member in sorted(set(re.findall(r'\b([A-Z]\w+)\.(\w+)', text))):
        if obj not in ("PlanReadingPolicy", "PlanPolicy", "PlanStore", "DataUsageTracker", "Payload"):
            continue
        if obj not in cache:
            cache[obj] = declarations(obj)
        if not cache[obj]:
            continue  # declared outside this module (Compose, Android SDK…)
        if member not in cache[obj]:
            problems.append(f"{path.name}: {obj}.{member} is not declared")

# A call written as `something.name(...)` where `something` is a value (lower-case) is almost
# always a typo of `Object.name(...)`. That is how `plan.boosterLastDay(plan)` slipped through a
# CI round-trip: the member exists, but on PlanPolicy, not on the PlanSettings instance.
for path in CHANGED:
    text = sources[path]
    for recv, member in sorted(set(re.findall(r'\b([a-z]\w*)\.(\w+)\(', text))):
        if recv in ("it", "this", "context", "plan", "reading", "status") and member in PLAN_POLICY_MEMBERS:
            problems.append(f"{path.name}: `{recv}.{member}(...)` is probably PlanPolicy.{member}(...), not a member of the value")

for p in problems:
    print("FAIL:", p)
print(f"checked {len(CHANGED)} changed files against "
      f"{', '.join(sorted(cache)) or 'no objects'}: {len(problems)} unresolved")
sys.exit(1 if problems else 0)
