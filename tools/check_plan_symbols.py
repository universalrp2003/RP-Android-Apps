#!/usr/bin/env python3
"""Resolve the cross-object references this change introduced, without a compiler.

Not a Kotlin type checker. It answers one question: does every `Foo.bar` this change calls
actually exist as a declaration in the module? A missing one is exactly the class of mistake
that used to need a CI round-trip to find.
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

for p in problems:
    print("FAIL:", p)
print(f"checked {len(CHANGED)} changed files against "
      f"{', '.join(sorted(cache)) or 'no objects'}: {len(problems)} unresolved")
sys.exit(1 if problems else 0)
