"""Pre-flight check for a new module. Catches every failure mode that has cost a CI cycle.

Run before pushing a new app module. Each check names the failure it prevents, because a check
whose reason is not recorded gets deleted by somebody who thinks it is noise.
"""

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

MODULE = sys.argv[1] if len(sys.argv) > 1 else "podcast"
PKG_PATH = "com/universalrp/" + MODULE
ROOT = Path(".")
problems = []
notes = []


def bad(msg):
    problems.append(msg)


src = ROOT / MODULE / "src" / "main" / "java" / PKG_PATH
test_src = ROOT / MODULE / "src" / "test" / "java" / PKG_PATH
kt = sorted(src.glob("*.kt")) if src.is_dir() else []
kt_tests = sorted(test_src.glob("*.kt")) if test_src.is_dir() else []
all_kt = kt + kt_tests

if not kt:
    bad("no Kotlin sources found under %s" % src)

# ---------------------------------------------------------------- 1. BuildConfig (cost 1 cycle)
for f in all_kt:
    if "BuildConfig" in f.read_text(encoding="utf-8"):
        bad("%s references BuildConfig, which this project's AGP does not generate" % f.name)

# ---------------------------------------------------------------- 2. '--' inside an XML comment
for values in (ROOT / MODULE / "src" / "main" / "res" / "values").glob("*.xml"):
    text = values.read_text(encoding="utf-8")
    try:
        ET.fromstring(text)
    except ET.ParseError as error:
        bad("%s is not well-formed XML: %s" % (values.name, error))
    for match in re.finditer(r"<!--(.*?)-->", text, re.S):
        if "--" in match.group(1):
            line = text.count("\n", 0, match.start()) + 1
            bad("%s:%d has '--' inside a comment, which AAPT rejects" % (values.name, line))

# ---------------------------------------------------------------- 3. every R.string / R.drawable used exists
strings_xml = (ROOT / MODULE / "src" / "main" / "res" / "values" / "strings.xml")
declared_strings = set()
if strings_xml.exists():
    declared_strings = set(re.findall(r'<string name="([^"]+)"', strings_xml.read_text(encoding="utf-8")))
drawables = {p.stem for p in (ROOT / MODULE / "src" / "main" / "res" / "drawable").glob("*.xml")}
mipmaps = {p.stem for p in (ROOT / MODULE / "src" / "main" / "res" / "mipmap-anydpi-v26").glob("*.xml")}

used_strings, used_drawables = set(), set()
for f in kt:
    text = f.read_text(encoding="utf-8")
    used_strings |= set(re.findall(r"R\.string\.(\w+)", text))
    used_drawables |= set(re.findall(r"R\.drawable\.(\w+)", text))
    used_drawables |= set(re.findall(r"R\.mipmap\.(\w+)", text))
missing_strings = sorted(used_strings - declared_strings)
missing_drawables = sorted(used_drawables - drawables - mipmaps)
if missing_strings:
    bad("strings used in Kotlin but not declared: %s" % ", ".join(missing_strings))
if missing_drawables:
    bad("drawables used in Kotlin but not present: %s" % ", ".join(missing_drawables))

# ---------------------------------------------------------------- 4. cross-object member references
decl = {}
for f in all_kt:
    text = f.read_text(encoding="utf-8")
    for target in ["PodcastState", "RssReader", "PodcastStore", "BuiltInShows",
                   "PodcastPlaybackService", "PodcastShow", "Episode"]:
        m = re.search(r"(?:object|class|data class|enum class)\s+" + target + r"\b", text)
        if not m:
            continue
        i = text.index("{", m.start()) if "{" in text[m.start():] else len(text)
        depth, j = 0, i
        while j < len(text):
            if text[j] == "{":
                depth += 1
            elif text[j] == "}":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        body = text[i:j]
        found = set()
        for pat in [r"\bfun\s+(\w+)", r"\bval\s+(\w+)", r"\bvar\s+(\w+)",
                    r"\bconst val\s+(\w+)", r"\bdata class\s+(\w+)",
                    r"\bclass\s+(\w+)", r"\bobject\s+(\w+)"]:
            found |= set(re.findall(pat, body))
        for em in re.finditer(r"enum class\s+\w+[^{]*\{([^}]*)\}", body):
            found |= {e.strip() for e in em.group(1).split(",") if e.strip()}
        for dm in re.finditer(r"(?:data )?class\s+" + target + r"\s*\(([^)]*)\)", text):
            for pm in re.finditer(r"(?:val|var)\s+(\w+)\s*:", dm.group(1)):
                found.add(pm.group(1))
        decl[target] = decl.get(target, set()) | found

for f in all_kt:
    text = f.read_text(encoding="utf-8")
    for ref in re.finditer(r"\b(PodcastState|RssReader|PodcastStore|BuiltInShows|PodcastPlaybackService)\.(\w+)", text):
        obj, mem = ref.group(1), ref.group(2)
        if obj in decl and mem not in decl[obj]:
            bad("%s: unresolved %s.%s" % (f.name, obj, mem))

# ---------------------------------------------------------------- 5. Service actions must match
service_text = (src / "PodcastPlaybackService.kt").read_text(encoding="utf-8") if (src / "PodcastPlaybackService.kt").exists() else ""
declared_actions = re.findall(r'const val (ACTION_\w+)\s*=\s*"([^"]+)"', service_text)
for action, value in declared_actions:
    # The service handles an action by the constant's NAME inside a `when`, so the string literal
    # itself legitimately appears only once. Counting identifier uses is what catches a declared
    # action that nothing responds to.
    if len(re.findall(r"\b" + action + r"\b", service_text)) < 2:
        bad("PodcastPlaybackService declares %s (%s) but never handles it" % (action, value))

# ---------------------------------------------------------------- 6. format strings must use positional args
for f in all_kt:
    text = f.read_text(encoding="utf-8")
    # Same rule as tools/verify_apps_localization.py: only POSITIONAL specifiers are a compile
    # error, because Kotlin reads the `$` as a template reference. A plain `%d` is fine.
    for lineno, line in enumerate(text.splitlines(), 1):
        if re.search(r'(?<!\\)%\d+\$[A-Za-z]', line):
            bad("%s:%d has an unescaped positional format specifier" % (f.name, lineno))

# ---------------------------------------------------------------- 7. module wired into the build
settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
if 'include(":%s")' % MODULE not in settings:
    bad("settings.gradle.kts does not include :%s" % MODULE)
build_apk = (ROOT / ".github" / "workflows" / "build-apk.yml").read_text(encoding="utf-8")
for task in [":%s:testDebugUnitTest" % MODULE, ":%s:assembleRelease" % MODULE]:
    if task not in build_apk:
        bad("build-apk.yml does not run %s" % task)

# ---------------------------------------------------------------- 8. Release note honesty gate
RELEASE_PHRASES = [
    "not installed on a phone", "no physical-device test", "no device test",
    "not phone-installed", "no phone install", "installed on a phone", "verified on a phone",
]
gradle = (ROOT / MODULE / "build.gradle.kts").read_text(encoding="utf-8")
version = re.search(r'versionName\s*=\s*"([^"]+)"', gradle).group(1)
note = ROOT / "releases" / ("tamil-podcast-v%s.md" % version) if MODULE == "podcast" else None
if note is not None:
    if not note.exists():
        bad("release note missing: %s" % note.name)
    else:
        low = note.read_text(encoding="utf-8").lower()
        if not any(p in low for p in RELEASE_PHRASES):
            bad("%s does not state its device-test status" % note.name)
        if low.count("## தமிழ்") != 1:
            bad("%s must carry exactly one '## தமிழ்' heading" % note.name)

# ---------------------------------------------------------------- 9. known-bad Android patterns
# Each of these cost a CI cycle, so they are checked here instead of on the runner.
for f in all_kt:
    raw = f.read_text(encoding="utf-8")
    # Comments stripped before matching, so an explanatory comment that names the bad constant
    # does not trip the check that exists because of it.
    text = re.sub(r"//[^\n]*", "", raw)
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    if "Intent.CATEGORY_APP_CAMERA" in text:
        bad("%s uses Intent.CATEGORY_APP_CAMERA, which does not exist. Use "
            "MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA." % f.name)
    if "onNewIntent(intent: Intent?)" in text:
        bad("%s declares onNewIntent with a nullable parameter; this androidx version wants Intent."
            % f.name)
    if re.search(r'onClick\s*=\s*\{[^}]*LocalContext\.current', text, re.S):
        bad("%s reads LocalContext.current inside an onClick lambda, which is not a composable "
            "context." % f.name)
    # LazyColumn and LazyVerticalGrid need DIFFERENT items overloads. The grid overload is correct
    # inside a grid, so its presence alone is not a defect; using it where only a LazyColumn exists
    # is, and so is a grid that never imported it.
    if "lazy.grid.items" in text and "LazyVerticalGrid" not in text:
        bad("%s imports the grid items overload but has no LazyVerticalGrid." % f.name)

# ---------------------------------------------------------------- 10. stray '#' comment markers
# A '#' opening a trimmed line is never valid Kotlin. It is what a scripted edit leaves behind when
# a replacement string is written in Python and the '//' is lost, and the resulting error message
# points at the line below the mistake, which sends you looking in the wrong place.
for f in all_kt:
    for lineno, line in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
        if line.lstrip().startswith("# ") and not line.lstrip().startswith("#!"):
            bad("%s:%d starts a line with '#', which is not a Kotlin comment" % (f.name, lineno))

print("module: %s  version: %s" % (MODULE, version))
print("kotlin sources: %d main, %d test" % (len(kt), len(kt_tests)))
print("strings declared: %d  drawables: %d" % (len(declared_strings), len(drawables)))
if problems:
    print("\nPROBLEMS (%d):" % len(problems))
    for p in problems:
        print("  -", p)
    sys.exit(1)
print("\nPRE-FLIGHT CLEAN: no known failure mode present.")
