#!/usr/bin/env python3
"""Check Vasanthi AI's Tamil sound vocabulary against the model it is paired with.

Vasanthi AI shows a deaf Tamil reader what a sound is. The sound names come from YAMNet's 521
AudioSet classes, which are all named in English, so the app carries its own Tamil map.

That map is only trustworthy if it is keyed to the label file that actually ships in the APK. A
key that does not exist in yamnet_labels.txt is a sound the app will never name, and the map
quietly claims coverage it does not have. A key that exists but was written from memory can point
at the wrong sound entirely, which for this user is worse than saying nothing: she is being told
"the doorbell" when it was a car horn.

So this gate checks three things and refuses to pass on any of them:

1. Every key in SoundLabelMap's TAMIL map and in its URGENT set exists in the shipped label file.
2. Every value is Tamil script, not a transliteration and not a leftover English word.
3. The label file is the one the model expects: 521 classes, and the committed model is the
   byte-for-byte file whose digest is recorded here, so a silently swapped model is caught.
"""

import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

SOUND_MAP = ROOT / "vasanthi" / "src" / "main" / "java" / "com" / "universalrp" / "vasanthi" / "SoundLabelMap.kt"
LABELS = ROOT / "vasanthi" / "src" / "main" / "assets" / "yamnet_labels.txt"
MODEL = ROOT / "vasanthi" / "src" / "main" / "assets" / "yamnet.tflite"

# YAMNet's AudioSet class count, and the digest of the Apache-2.0 model this app was built and
# tested against. Both are stated so a swapped file fails loudly instead of quietly.
EXPECTED_CLASS_COUNT = 521
EXPECTED_MODEL_SHA256 = "ee0fd63185c5bca7c721981d52b2826bc294d61e5e907ffcc8146578a8fb0145"

TAMIL_BLOCK = (0x0B80, 0x0BFF)


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_labels() -> list:
    if not LABELS.exists():
        fail(f"{LABELS.relative_to(ROOT)} is missing; the classifier has no class names.")
    labels = [line.strip() for line in LABELS.read_text(encoding="utf-8").split("\n") if line.strip()]
    if len(labels) != EXPECTED_CLASS_COUNT:
        fail(
            f"yamnet_labels.txt holds {len(labels)} classes, but YAMNet predicts {EXPECTED_CLASS_COUNT}. "
            "A label file of the wrong length means every index is off by some amount."
        )
    if len(set(labels)) != len(labels):
        fail("yamnet_labels.txt repeats a class name; the map cannot address them apart.")
    return labels


def check_model() -> None:
    if not MODEL.exists():
        fail(f"{MODEL.relative_to(ROOT)} is missing; the classifier has no model.")
    data = MODEL.read_bytes()
    if data[4:8] != b"TFL3":
        fail(
            "yamnet.tflite is not a TFLite flatbuffer (no TFL3 magic). A file with the right name "
            "and the wrong contents loads as nothing at all on the device."
        )
    digest = hashlib.sha256(data).hexdigest()
    if digest != EXPECTED_MODEL_SHA256:
        fail(
            f"yamnet.tflite has sha256 {digest}, expected {EXPECTED_MODEL_SHA256}. The Tamil map in "
            "SoundLabelMap.kt was written against one specific model; a different file may order its "
            "classes differently, which would attach Tamil names to the wrong sounds."
        )


def read_map_block(name: str) -> str:
    source = SOUND_MAP.read_text(encoding="utf-8")
    marker = f"private val {name}"
    if marker not in source:
        fail(f"SoundLabelMap.kt has no {name}; this gate would silently check nothing.")
    tail = source.split(marker, 1)[1]
    return tail.split("\n    )", 1)[0]


def parse_entries(block: str):
    # Only "key" to "value" pairs, so the surrounding comments and the KDoc cannot contribute.
    return re.findall(r'"((?:[^"\\]|\\.)*)"\s+to\s+"((?:[^"\\]|\\.)*)"', block)


def parse_set(block: str):
    return re.findall(r'"((?:[^"\\]|\\.)*)"', block)


def is_tamil(text: str) -> bool:
    return any(TAMIL_BLOCK[0] <= ord(ch) <= TAMIL_BLOCK[1] for ch in text)


def main() -> int:
    labels = load_labels()
    check_model()
    known = set(labels)

    entries = parse_entries(read_map_block("TAMIL"))
    if not entries:
        fail("SoundLabelMap's TAMIL map parsed as empty; the app would name no sound at all.")

    unknown = sorted(key for key, _ in entries if key not in known)
    if unknown:
        fail(
            "these SoundLabelMap keys are not AudioSet classes in the shipped yamnet_labels.txt, so "
            "the app will never show them: " + ", ".join(repr(k) for k in unknown)
        )

    untranslated = sorted(f"{key} -> {value}" for key, value in entries if not is_tamil(value))
    if untranslated:
        fail(
            "these SoundLabelMap values are not Tamil script, so the user would be shown a "
            "transliteration or an English word: " + ", ".join(untranslated)
        )

    duplicates = sorted({key for key, _ in entries if [k for k, _ in entries].count(key) > 1})
    if duplicates:
        fail("these SoundLabelMap keys appear twice, so the second value is dead code: " + ", ".join(duplicates))

    urgent = parse_set(read_map_block("URGENT"))
    unknown_urgent = sorted(key for key in urgent if key not in known)
    if unknown_urgent:
        fail("URGENT names classes that do not exist: " + ", ".join(repr(k) for k in unknown_urgent))
    translated = {key for key, _ in entries}
    missing_urgent = sorted(key for key in urgent if key not in translated)
    if missing_urgent:
        fail("URGENT names classes with no Tamil translation: " + ", ".join(repr(k) for k in missing_urgent))

    covered = len({key for key, _ in entries if key in known})
    print(
        f"PASS: {covered} of {len(labels)} AudioSet classes carry Tamil names, all keys exist in the "
        f"shipped label file, all values are Tamil script, {len(urgent)} classes are marked urgent, "
        "and the committed model matches its recorded digest."
    )
    print("Sound labels are source-only: Kotlin compilation, the classifier's runtime behaviour and the device still need their own checks.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
