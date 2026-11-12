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
    MAIN / "ui/SettingsScreen.kt",
    MAIN / "ui/StatusPillHomeControls.kt",
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


# A `Foo.bar` is only resolvable if `Foo` is visible in that file. Kotlin has no wildcard import
# in most of these files, so a newly used object with no matching import is a compile error even
# though the declaration exists somewhere in the module. That is exactly what shipped to CI once:
# SettingsScreen called StatusPill.signalMode() after importing only PillSignalPolicy.
def package_of(text: str) -> str:
    m = re.search(r'^package\s+([\w.]+)', text, re.M)
    return m.group(1) if m else ''


# Where each object/class actually lives, so same-package uses are not called unimported.
decl_package: dict[str, str] = {}
for src_path, text in sources.items():
    pkg = package_of(text)
    for name in re.findall(r'^(?:internal |private )?(?:object|class|enum class|data class)\s+(\w+)', text, re.M):
        decl_package.setdefault(name, pkg)

for path in CHANGED:
    text = sources[path]
    pkg = package_of(text)
    if re.search(r'^import\s+[\w.]+\*\s*$', text, re.M):
        continue  # a wildcard import makes a whole package visible
    imported_simple = {i.rsplit('.', 1)[-1] for i in re.findall(r'^import\s+([\w.]+)', text, re.M)}
    # A fully-qualified use (com.x.y.Foo.bar) needs no import, so drop the package prefix
    # before looking for the receiver name. Comments are stripped: a name mentioned in prose
    # is not a reference.
    code = re.sub(r'//[^\n]*', '', text)
    qualified = re.sub(r'\b(?:[a-z]\w*\.)+([A-Z]\w+)\.', r'\1.', code)
    for obj in sorted({o for o, _ in re.findall(r'\b([A-Z]\w+)\.(\w+)', code)}):
        if obj not in decl_package:
            continue  # declared outside this module: Compose, Android SDK, Java
        if decl_package[obj] == pkg:
            continue  # same package: no import needed in Kotlin
        if obj in imported_simple:
            continue
        if re.search(rf'\b(?:[a-z]\w*\.)+{obj}\b', code):
            continue  # used fully-qualified somewhere, which satisfies the compiler
        if re.search(rf'^(?:internal |private )?(?:object|class|fun|val)\s+{obj}\b', text, re.M):
            continue  # declared in this very file
        problems.append(f"{path.name}: `{obj}` lives in {decl_package[obj]} but is never imported")

for p in problems:
    print("FAIL:", p)
print(f"checked {len(CHANGED)} changed files against "
      f"{', '.join(sorted(cache)) or 'no objects'}: {len(problems)} unresolved")
sys.exit(1 if problems else 0)
