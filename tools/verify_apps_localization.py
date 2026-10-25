#!/usr/bin/env python3
"""Source-only checks for the shared Tamil-default catalogs in Radio, PulseEQ and Vocal Remover."""
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "shared/src/main/java/com/universalrp/updates/AppLanguage.kt"
STRING = r'"(?:\\.|[^"\\])*"'
EXPR = rf'{STRING}(?:\s*\+\s*{STRING})*'
PROPER = {"Ramesh Radio", "PulseEQ", "RP VOCAL REMOVER", "STUDIO", "›"}
FORMAT_TEMPLATES = {"%.1f dB", "Preamp %.1f dB", "preamp %.1f dB", "Volume boost: %.0f dB"}
# The legacy per-app path is retained only for a future revert: `InternetRadio` was not mirrored for
# the 19 October 2026 releases, so this set names the newest versions that repository actually has.
EXPECTED_RELEASE_URLS = {
    "https://github.com/universalrp2003/InternetRadio/releases/tag/live-guard-v2.46",
    "https://github.com/universalrp2003/InternetRadio/releases/tag/ramesh-radio-v1.11",
    "https://github.com/universalrp2003/InternetRadio/releases/tag/pulseeq-v1.5",
    "https://github.com/universalrp2003/InternetRadio/releases/tag/rp-vocal-remover-studio-v0.1.11",
}
EXPECTED_OOR_RELEASE_URLS = {
    "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/sms-guard-v1.1.6",
    "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/live-guard-v2.72",
    "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/ramesh-radio-v1.25",
    "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/pulseeq-v1.9",
    "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/rp-vocal-remover-studio-v0.1.15",
    "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.1.9",
    "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/secret-message-v1.1.3",
}


def decode_expression(expression: str) -> str:
    return "".join(json.loads(part) for part in re.findall(STRING, expression))


def catalog_keys() -> set[str]:
    source = CATALOG.read_text(encoding="utf-8")
    if "TAMIL(\"ta\")" not in source or "?: TAMIL" not in source or "current: AppLanguage by mutableStateOf(AppLanguage.TAMIL)" not in source:
        raise ValueError("Shared language must default to Tamil when no English choice is saved.")
    body = source.split("private val TAMIL = mapOf(", 1)[1].split("\n    )", 1)[0]
    entries = [
        (json.loads(match.group(1)), json.loads(match.group(2)))
        for match in re.finditer(rf'^\s*({STRING})\s+to\s+({STRING})\s*,?\s*$', body, re.M)
    ]
    counts = Counter(key for key, _ in entries)
    duplicates = sorted(key for key, count in counts.items() if count > 1)
    if duplicates:
        raise ValueError("Duplicate shared Tamil keys: " + ", ".join(repr(key) for key in duplicates))
    for english, tamil in entries:
        if not english.strip() or not tamil.strip():
            raise ValueError(f"Empty Tamil catalog value for {english!r}")
    for dynamic_label in ('Regex("^Preamp ', 'Regex("^preamp ', 'Regex("^Volume boost: '):
        if dynamic_label not in source:
            raise ValueError(f"Missing runtime Tamil formatter for {dynamic_label}")
    return set(counts)


def verify_visible_text(module: str, keys: set[str]) -> int:
    source_root = ROOT / module / "src/main/java"
    missing: dict[str, list[str]] = {}
    checked = 0
    for path in sorted(source_root.rglob("*.kt")):
        source = path.read_text(encoding="utf-8")
        for match in re.finditer(rf'\bText\s*\(\s*(?:text\s*=\s*)?({EXPR})', source):
            key = decode_expression(match.group(1))
            after = source[match.end():].lstrip()
            # A concatenated/dynamic suffix is formatted at runtime; its static prefix is
            # not a full catalog key. The relevant user-facing templates are checked below.
            if after.startswith("+") or after.startswith(".") or "$" in key:
                continue
            if key in keys or key in PROPER or key in FORMAT_TEMPLATES or not re.search(r"[A-Za-z]{3}", key):
                checked += 1
                continue
            missing.setdefault(key, []).append(str(path.relative_to(ROOT)))
        if module == "shared":
            for match in re.finditer(rf'\btext\(\s*({EXPR})', source):
                key = decode_expression(match.group(1))
                after = source[match.end():].lstrip()
                if after.startswith("+") or after.startswith(".") or "$" in key:
                    continue
                if key in keys or key in PROPER or not re.search(r"[A-Za-z]{3}", key):
                    checked += 1
                else:
                    missing.setdefault(key, []).append(str(path.relative_to(ROOT)))
    if missing:
        detail = "\n".join(f"- {key!r}: {', '.join(sorted(set(paths)))}" for key, paths in sorted(missing.items()))
        raise ValueError(f"Untranslated explicit Compose labels in {module}:\n{detail}")
    return checked


def verify_appstrings_accessibility_and_toasts(keys: set[str]) -> None:
    missing: dict[str, list[str]] = {}
    for module in ("radio", "equalizer", "vocalremover"):
        source_root = ROOT / module / "src/main/java"
        for path in sorted(source_root.rglob("*.kt")):
            for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                # Accessibility labels are not visible Text composables, but still part of the
                # bilingual UI. Dynamic exception fragments are handled by explicit formatters.
                if "AppStrings.translate(" in line:
                    for match in re.finditer(STRING, line):
                        key = json.loads(match.group())
                        if "$" not in key and re.search(r"[A-Za-z]{3}", key) and key not in keys:
                            missing.setdefault(key, []).append(f"{path.relative_to(ROOT)}:{line_no}")
                # Toast copy is user-facing but does not pass through the Compose Text audit.
                for match in re.finditer(rf'showToast\(context,\s*({STRING})', line):
                    key = decode_expression(match.group(1))
                    if "$" not in key and re.search(r"[A-Za-z]{3}", key) and key not in keys:
                        missing.setdefault(key, []).append(f"{path.relative_to(ROOT)}:{line_no}")
    if missing:
        detail = "\n".join(f"- {key!r}: {', '.join(paths)}" for key, paths in sorted(missing.items()))
        raise ValueError("Untranslated accessibility labels or Vocal Remover toast messages:\n" + detail)

    dynamic = CATALOG.read_text(encoding="utf-8")
    for formatter in (
        'Regex("^Download failed:',
        'Regex("^Could not control the background queue:',
        r'Regex("^(\\d+) MP3s?',
        'Regex("^Android could not play this audio',
        'Regex("^Could not play the audio:',
    ):
        if formatter not in dynamic:
            raise ValueError("Missing Tamil runtime formatter for user-facing dynamic text: " + formatter)


def verify_oor_bilingual_catalog() -> int:
    path = ROOT / "oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/BilingualStrings.kt"
    source = path.read_text(encoding="utf-8")
    data_class = source.split("data class UiStrings(", 1)[1].split("\n)", 1)[0]
    expected_fields = set(re.findall(r"\bval\s+(\w+)\s*:\s*String", data_class))
    if not expected_fields:
        raise ValueError("Oor Arivippu UiStrings must declare its bilingual fields.")

    for language in ("TAMIL", "ENGLISH"):
        marker = f"val {language} = UiStrings("
        if marker not in source:
            raise ValueError(f"Oor Arivippu is missing the {language} string catalog.")
        body = source.split(marker, 1)[1].split("\n    )", 1)[0]
        assignments = re.findall(r"^\s*(\w+)\s*=\s*(.+?)\s*,?\s*$", body, re.M)
        counts = Counter(name for name, _ in assignments)
        duplicates = sorted(name for name, count in counts.items() if count > 1)
        missing = expected_fields - set(counts)
        extra = set(counts) - expected_fields
        if duplicates or missing or extra:
            raise ValueError(
                f"Oor Arivippu {language} catalog mismatch: missing={sorted(missing)}, "
                f"extra={sorted(extra)}, duplicates={duplicates}"
            )
        for name, expression in assignments:
            if not expression.strip() or expression.strip() in {'""', "\"\"\"\"\"\""}:
                raise ValueError(f"Oor Arivippu {language}.{name} is empty.")

    main_source = (ROOT / "oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/MainActivity.kt").read_text(encoding="utf-8")
    referenced_fields = set(re.findall(r"(?<![.\w])ui\.(\w+)\b", main_source))
    unknown_fields = referenced_fields - expected_fields
    if unknown_fields:
        raise ValueError(f"Oor Arivippu UI references missing bilingual fields: {sorted(unknown_fields)}")
    return len(expected_fields)


def verify_app_guides() -> None:
    """Check that all six apps expose a bilingual guide from their primary screen."""
    guides_path = ROOT / "shared/src/main/java/com/universalrp/updates/AppGuides.kt"
    guides_source = guides_path.read_text(encoding="utf-8")
    guide_sections = (
        ("Live Guard", "liveGuard", "// Ramesh Radio"),
        ("Ramesh Radio", "rameshRadio", "// PulseEQ"),
        ("PulseEQ", "pulseEq", "// RP Vocal Remover Studio"),
        ("RP Vocal Remover Studio", "vocalRemover", "// Oor Arivippu"),
        ("Oor Arivippu", "oorArivippu", "// SMS Guard"),
        ("SMS Guard", "smsGuard", None),
    )
    for app, field, end_marker in guide_sections:
        start = guides_source.find(f"val {field} = GuideContent(")
        if start < 0:
            raise ValueError(f"The shared {app} guide is missing.")
        end = guides_source.find(end_marker, start) if end_marker else len(guides_source)
        if end < 0:
            raise ValueError(f"Could not delimit the shared {app} guide.")
        block = guides_source[start:end]
        topic_count = len(re.findall(r"^\s*topic\(", block, re.M))
        if "intro =" not in block or "introTa =" not in block or topic_count < 6:
            raise ValueError(
                f"The shared {app} guide needs English and Tamil introductions and at least six topics; "
                f"found {topic_count}."
            )

    shared_screen = (ROOT / "shared/src/main/java/com/universalrp/updates/GuideScreen.kt").read_text(encoding="utf-8")
    for required in ("topic.titleTa", "topic.title", "topic.bodyTa", "topic.body", "content.topics"):
        if required not in shared_screen:
            raise ValueError(f"The shared expandable guide screen is missing bilingual rendering: {required}")

    direct_entry_points = {
        "Live Guard": (
            "cleaner/src/main/java/com/universalrp/cleansweep/ui/HomeScreen.kt",
            ("vm.navigate(Screen.GUIDE)", "Icons.Outlined.MenuBook", 'tr("Guide")'),
        ),
        "Ramesh Radio": (
            "radio/src/main/java/com/universalrp/tamilnadufm/ui/StationsScreens.kt",
            ("AppGuides.rameshRadio", "showGuide", "Icons.Filled.HelpOutline"),
        ),
        "PulseEQ": (
            "equalizer/src/main/java/com/universalrp/pulseeq/ui/EqualizerScreen.kt",
            ("onGuide", 'AppStrings.translate("Guide")', "Icons.Outlined.MenuBook"),
        ),
        "RP Vocal Remover Studio": (
            "vocalremover/src/main/java/com/universalrp/vocalremover/MainActivity.kt",
            ("AppGuides.vocalRemover", "onGuide", 'AppStrings.translate("Guide")'),
        ),
        "Oor Arivippu": (
            "oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/MainActivity.kt",
            ("AppGuides.oorArivippu", "onClick = { showGuide = true }", "HelpOutline"),
        ),
        "SMS Guard": (
            "smsguard/src/main/java/com/universalrp/smsguard/MainActivity.kt",
            ("AppGuides.smsGuard", "vm.openGuide(true)", "HelpOutline"),
        ),
        "Secret Message": (
            "secretmessage/wifi-test/app/src/main/java/com/rpideas/secretmessage/wifitest/ConnectScreen.kt",
            ("onOpenGuide", "HelpOutline", 'tr("Guide")'),
        ),
    }
    for app, (relative_path, required_tokens) in direct_entry_points.items():
        source = (ROOT / relative_path).read_text(encoding="utf-8")
        missing = [token for token in required_tokens if token not in source]
        if missing:
            raise ValueError(f"{app} main screen does not expose its guide directly: {missing}")

    secret_path = ROOT / "secretmessage/wifi-test/app/src/main/java/com/rpideas/secretmessage/wifitest/GuideScreen.kt"
    secret_source = secret_path.read_text(encoding="utf-8")
    english = secret_source.split("private val ENGLISH_GUIDE = listOf(", 1)[1].split(
        "private val TAMIL_GUIDE = listOf(", 1
    )[0]
    tamil = secret_source.split("private val TAMIL_GUIDE = listOf(", 1)[1]
    title_pattern = r'^\s{4}"[^"\\]*(?:\\.[^"\\]*)*"\s+to\s*$'
    english_topics = len(re.findall(title_pattern, english, re.M))
    tamil_topics = len(re.findall(title_pattern, tamil, re.M))
    if english_topics < 7 or english_topics != tamil_topics:
        raise ValueError(
            f"Secret Message needs matching English and Tamil guide topics; found {english_topics} and {tamil_topics}."
        )
    for required in ("IP addresses", "traffic sizes", "safety-code check", "20-digit safety code"):
        if required not in english:
            raise ValueError(f"Secret Message English guide is missing a key privacy/safety detail: {required}")
    for required in ("IP முகவரி", "தரவு அளவு", "குறியீடு ஒப்பிடுவதற்கு முன்", "20 இலக்கப் பாதுகாப்புக்", "குறியீட்டின் ஒவ்வொரு இலக்கத்தையும்"):
        if required not in tamil:
            raise ValueError(f"Secret Message Tamil guide is missing a key privacy/safety detail: {required}")

    root_source = (ROOT / "secretmessage/wifi-test/app/src/main/java/com/rpideas/secretmessage/wifitest/WifiTestApp.kt").read_text(encoding="utf-8")
    if "GuideScreen(onBack" not in root_source or "onOpenGuide = { showGuide = true }" not in root_source:
        raise ValueError("Secret Message does not route the connection-screen help button to its guide.")


# The release-tag prefix for each app module. The module's own versionName is what the publish
# step builds, so the note that has to be honest is the one for that exact version.
RELEASE_TAG_PREFIX = {
    "cleaner": "live-guard",
    "radio": "ramesh-radio",
    "equalizer": "pulseeq",
    "vocalremover": "rp-vocal-remover-studio",
    "oorarivippu": "oor-arivippu",
    "smsguard": "sms-guard",
    "secretmessage": "secret-message",
}


def module_version(module: str) -> str:
    path = ROOT / module / "build.gradle.kts"
    if not path.exists():
        return ""
    match = re.search(
        r'versionName\s*=\s*"([^"]+)"', path.read_text(encoding="utf-8")
    )
    return match.group(1) if match else ""


def verify_release_notes_tamil_section() -> int:
    """The note for each app's current version must carry exactly one `## தமிழ்` section, last.

    The in-app update screen calls `localizedReleaseNotes`, which splits the note on that exact
    marker. A note without it makes a Tamil-language owner read the English text instead, because
    `substringBefore` then returns the whole note. That is silent: nothing fails, the screen just
    shows the wrong language.

    Two more failures are equally silent, and both were real:

    * a second `## தமிழ்` heading leaves the Tamil view with the English sections that sat between
      the two copies, because `substringAfter` takes everything to the end of the file;
    * a Tamil section that is not the LAST section makes the *English* view lose everything after
      it, because `substringBefore` stops at the marker.

    So the gate requires exactly one Tamil heading and requires it to be the final section.
    """
    marker = "## தமிழ்"
    checked = 0
    for module, prefix in sorted(RELEASE_TAG_PREFIX.items()):
        version = module_version(module)
        if not version:
            continue
        note = ROOT / "releases" / f"{prefix}-v{version}.md"
        if not note.exists():
            continue
        text = note.read_text(encoding="utf-8")
        rel = note.relative_to(ROOT)
        headings = [line for line in text.split("\n") if line.startswith("## ")]
        tamil_headings = [line for line in headings if line.startswith(marker)]
        if marker not in text:
            raise ValueError(
                f"{rel} has no `{marker}` section, so a Tamil-language owner is shown the English "
                f"text in the in-app update screen. Add the Tamil section."
            )
        if len(tamil_headings) != 1:
            raise ValueError(
                f"{rel} has {len(tamil_headings)} `{marker}` sections. `localizedReleaseNotes` "
                f"takes everything after the first one, so a duplicate drags the English sections "
                f"that sit between the copies into the Tamil view. Keep exactly one."
            )
        if not headings or headings[-1] not in tamil_headings:
            raise ValueError(
                f"{rel} has a `{marker}` section that is not the last section. The English view is "
                f"`substringBefore(marker)`, so every section after the Tamil one is dropped for "
                f"an English reader. Move the Tamil section to the end of the note."
            )
        checked += 1
    return checked


def verify_release_notes_tamil_words() -> int:
    """No Tamil release note may hide a transliterated English word inside its Tamil section.

    The Tamil sections are written in Tamil, with the product's own technical words left in
    Latin script on purpose (`chip`, `slot`, `Play Protect`, `HD`). What must never appear is a
    Tamil sentence with a whole English or transliterated word dropped into it - the 1.1.9 note
    really did ship `40 vaikkappadum` where `சேமிக்கப்படும்` belonged, and a Tamil reader sees
    that instantly.

    The rule is deliberately self-maintaining: a Latin word of five letters or more is allowed
    when the same word appears in the *English* half of the same note (so every product and
    technical term is fine), and is refused otherwise. A tiny allow-list covers the two words
    whose English form in the note is inflected (`highlight` / `highlighted`).
    """
    marker = "## தமிழ்"
    allowed = {"highlight", "highlights"}
    checked = 0
    for module, prefix in sorted(RELEASE_TAG_PREFIX.items()):
        version = module_version(module)
        if not version:
            continue
        note = ROOT / "releases" / f"{prefix}-v{version}.md"
        if not note.exists():
            continue
        text = note.read_text(encoding="utf-8")
        if marker not in text:
            continue
        tamil = text.split(marker, 1)[1]
        english = text.split(marker, 1)[0].lower()
        rel = note.relative_to(ROOT)
        for match in re.finditer(r"[A-Za-z][A-Za-z\-']*[A-Za-z]|[A-Za-z]", tamil):
            word = match.group(0).strip("-'")
            if len(word) < 5 or word.lower() in allowed:
                continue
            if word.lower() in english:
                continue
            line = tamil[: match.start()].count("\n") + 1
            raise ValueError(
                f"{rel} Tamil section line {line} contains the Latin word {word!r}, which "
                f"appears nowhere in the note's English text. A Tamil sentence with an "
                f"English or transliterated word dropped into it is not Tamil; write the word "
                f"in Tamil script, or add it to the allow-list if it really is a product term."
            )
        checked += 1
    return checked


def verify_release_notes_honesty() -> int:
    """The note for each app's current version must state its device-test status.

    The publish step greps the note for one of these phrases and fails closed without it, so a note
    that omits the truth about a physical install cannot be published at all. Checking the *current*
    version of every module here turns a publish-time failure into a source-time one, without
    rewriting notes for versions that are already public.
    """
    # Equivalent honest wordings, in both directions. The publish step's own grep uses a subset
    # of these; the rest are phrasings the notes already use to say the same thing, and refusing
    # them would only push someone into rewriting a published note instead of stating the fact.
    # A note that was really installed on a phone is just as required to say so.
    phrases = (
        "not installed on a phone",
        "no physical-device test",
        "no device test",
        "not phone-installed",
        "no phone install",
        "installed on a phone",
        "verified on a phone",
    )
    checked = 0
    for module, prefix in sorted(RELEASE_TAG_PREFIX.items()):
        version = module_version(module)
        if not version:
            continue
        note = ROOT / "releases" / f"{prefix}-v{version}.md"
        if not note.exists():
            continue
        text = note.read_text(encoding="utf-8").lower()
        if not any(phrase in text for phrase in phrases):
            rel = note.relative_to(ROOT)
            raise ValueError(
                f"{rel} does not state whether the published bytes were installed on a phone. "
                f"Every release note must contain one of: {phrases}"
            )
        checked += 1
    return checked


def verify_about_links() -> None:
    about_files = {
        "cleaner": ROOT / "cleaner/src/main/java/com/universalrp/cleansweep/ui/AboutScreen.kt",
        "radio": ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/ui/PlayScreens.kt",
        "equalizer": ROOT / "equalizer/src/main/java/com/universalrp/pulseeq/ui/AboutScreen.kt",
        "vocalremover": ROOT / "vocalremover/src/main/java/com/universalrp/vocalremover/MainActivity.kt",
    }
    sister_apps_path = ROOT / "shared/src/main/java/com/universalrp/updates/SisterApps.kt"
    sister_apps_source = sister_apps_path.read_text(encoding="utf-8") if sister_apps_path.exists() else ""
    shared_links = "fun releaseLinks()" in sister_apps_source
    if shared_links:
        expected_sister_tags = (
            'tagPrefix = "sms-guard"', 'version = "1.1.6"',
            'tagPrefix = "live-guard"', 'version = "2.72"',
            'tagPrefix = "ramesh-radio"', 'version = "1.25"',
            'tagPrefix = "pulseeq"', 'version = "1.9"',
            'tagPrefix = "rp-vocal-remover-studio"', 'version = "0.1.15"',
            'tagPrefix = "oor-arivippu"', 'version = "1.1.9"',
            'tagPrefix = "secret-message"', 'version = "1.1.3"',
            'OFFICIAL_WEBSITE_URL = "https://universalrp2003.github.io/RP-Android-Apps/"',
        )
        missing_tags = [tag for tag in expected_sister_tags if tag not in sister_apps_source]
        if missing_tags:
            raise ValueError(f"Shared sister-app list is missing current app/version entries: {missing_tags}")

    for app, path in about_files.items():
        source = path.read_text(encoding="utf-8")
        if shared_links:
            if "com.universalrp.updates.SisterApps.releaseLinks()" not in source or "com.universalrp.updates.SisterApps.projectLinks()" not in source:
                raise ValueError(f"{app} About does not render the shared seven-app release list and official project links")
        else:
            urls = set(re.findall(r'https://github\.com/universalrp2003/InternetRadio/releases/tag/[^"\\]+', source))
            missing = EXPECTED_RELEASE_URLS - urls
            if missing:
                raise ValueError(f"{app} About is missing release links: {sorted(missing)}")
        if "universalrp2003@gmail.com" not in source or "Arena.ai Agent Mode" not in source or "Project development" not in source:
            raise ValueError(f"{app} About must include the user contact, project-development credit and Arena.ai Agent Mode credit.")
        if re.search(r'\bAuthor\b', source, re.I):
            raise ValueError(f"{app} About still uses the disallowed 'Author' label.")

    oor_path = ROOT / "oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/MainActivity.kt"
    oor_source = oor_path.read_text(encoding="utf-8")
    oor_strings = (ROOT / "oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/BilingualStrings.kt").read_text(encoding="utf-8")
    oor_about_text = oor_source + "\n" + oor_strings
    oor_urls = set(re.findall(r'https://github\.com/universalrp2003/RP-Android-Apps/releases/tag/[^"\\]+', oor_source))
    missing_oor = EXPECTED_OOR_RELEASE_URLS - oor_urls
    if missing_oor:
        raise ValueError(f"Oor Arivippu About is missing current release links: {sorted(missing_oor)}")
    for required in (
        "https://universalrp2003.github.io/RP-Android-Apps/",
        "https://github.com/universalrp2003/InternetRadio",
        "https://github.com/universalrp2003/RP-Android-Apps",
        "universalrp2003@gmail.com",
        "Arena.ai Agent Mode",
        "Project development",
        'AppUpdates.Control("Oor-Arivippu")',
        'AppUpdates.Control("Oor-Arivippu", automatic = true)',
    ):
        if required not in oor_about_text:
            raise ValueError(f"Oor Arivippu About/updater is missing required content: {required}")
    if re.search(r'\bAuthor\b', oor_about_text, re.I):
        raise ValueError("Oor Arivippu About still uses the disallowed 'Author' label.")

    sms_source = (ROOT / "smsguard/src/main/java/com/universalrp/smsguard/ui/ControlsTab.kt").read_text(encoding="utf-8")
    if '1.0.0' in sms_source or "https://universalrp2003.github.io/RP-Android-Apps/" not in sms_source:
        raise ValueError("SMS Guard About must read installed version dynamically and link to the official website.")

    secret_about = (ROOT / "secretmessage/wifi-test/app/src/main/java/com/rpideas/secretmessage/wifitest/AppSettings.kt").read_text(encoding="utf-8")
    secret_urls = set(re.findall(r'https://github\.com/universalrp2003/RP-Android-Apps/releases/tag/[^"\\]+', secret_about))
    if secret_urls != EXPECTED_OOR_RELEASE_URLS or "https://universalrp2003.github.io/RP-Android-Apps/" not in secret_about:
        raise ValueError(f"Secret Message About is missing current 7-app links or official website URL: {EXPECTED_OOR_RELEASE_URLS - secret_urls}")


def verify_kotlin_format_strings() -> int:
    """Every Kotlin string literal that holds a positional format specifier must escape its `$`.

    Kotlin reads `"%1$d"` as the literal `%1` followed by a template reference to a variable named
    `d`, which fails to compile only once the module is built. Escaping it as `%1\$d` keeps the
    specifier intact for String.format.
    """
    pattern = re.compile(r'(?<!\\)%\d+\$[A-Za-z]')
    checked = 0
    for path in sorted(ROOT.rglob("*.kt")):
        if "build" in path.parts:
            continue
        text = path.read_text(encoding="utf-8")
        checked += 1
        for lineno, line in enumerate(text.splitlines(), 1):
            if pattern.search(line):
                rel = path.relative_to(ROOT)
                raise ValueError(
                    f"{rel}:{lineno} has an unescaped positional format specifier in a Kotlin "
                    f"string literal - Kotlin reads `$x` as a template reference, so the `$` must "
                    f"be written as `\$`: {line.strip()}"
                )
    return checked


def main() -> int:
    try:
        keys = catalog_keys()
        checked = sum(verify_visible_text(module, keys) for module in ("radio", "equalizer", "vocalremover", "shared"))
        oor_fields = verify_oor_bilingual_catalog()
        verify_app_guides()
        verify_about_links()
        verify_appstrings_accessibility_and_toasts(keys)
        kotlin_files = verify_kotlin_format_strings()
        notes_checked = verify_release_notes_honesty()
        tamil_notes = verify_release_notes_tamil_section()
        tamil_words = verify_release_notes_tamil_words()
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    print(f"PASS: {len(keys)} unique shared Tamil catalog entries; {checked} explicit Compose labels are covered.")
    print(f"PASS: all {oor_fields} Oor Arivippu UI fields have Tamil and English strings and are referenced consistently.")
    print("PASS: all six apps have bilingual, expandable guides and direct main-screen entry points.")
    print(f"PASS: {kotlin_files} Kotlin sources have no unescaped positional format specifiers in string literals.")
    print(f"PASS: {notes_checked} pinned release notes each state their device-test status.")
    print(f"PASS: {tamil_notes} pinned release notes each carry exactly one Tamil section, last, for the in-app update screen.")
    print(f"PASS: {tamil_words} pinned release notes keep their Tamil sections free of stray Latin words.")
    print("PASS: all four legacy About sections and the Oor Arivippu About/update screen have their required release links and contributor credits.")
    print("Source-only check; translated meaning, Kotlin compilation, runtime layout and device behavior still require their own checks.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
