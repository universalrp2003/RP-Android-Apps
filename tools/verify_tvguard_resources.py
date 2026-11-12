#!/usr/bin/env python3
"""Catch Android resource mistakes that only aapt reports, before the build runs."""
import re, sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
problems = []

for strings in sorted(ROOT.glob('tvguard/app/src/main/res/values*/strings.xml')):
    text = strings.read_text()
    try:
        ET.parse(strings)
    except ET.ParseError as e:
        problems.append(f"{strings}: invalid XML: {e}")
        continue
    for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', text, re.S):
        name, body = m.group(1), m.group(2)
        # aapt2 rejects an unescaped apostrophe inside a string.
        if re.search(r"(?<!\\)'", body):
            problems.append(f"{strings}: {name}: unescaped apostrophe (use \\')")
        # A bare % must be escaped unless it is a %n$s style placeholder.
        if re.search(r'(?<!\\)(?<!%)%(?!\d+\$[sd]|%)', body):
            problems.append(f"{strings}: {name}: unescaped % (use %%)")

sys.exit(0) if not any('unescaped' in p or 'invalid XML' in p for p in problems) else None
for p in problems:
    print('ERROR:', p)
sys.exit(1 if problems else 0)
