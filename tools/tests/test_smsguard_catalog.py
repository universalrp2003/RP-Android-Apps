#!/usr/bin/env python3
"""Tests for verify_smsguard_bilingual_catalog().

Nothing checked SMS Guard's BilingualCatalog before this gate, and the owner's 1.1.7 screenshots
showed what that cost. Each test below reproduces one of the defects that actually reached the
phone, so the gate can never again pass over them:

* the Tamil and English subtitles were swapped, so Tamil mode showed the app name as "SMS Guard"
  and English mode showed it as Tamil;
* "verified credits" and "verified debits" were hardcoded English inside FinanceTab.kt, never
  catalog fields;
* the monthly-spend card's Tamil read "மீதம் நாடகள்" - "remaining dramas" instead of days - and
  three other monthly-spend labels were not Tamil phrases at all.
"""
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import verify_apps_localization as mod  # noqa: E402

CATALOG_REL = "smsguard/src/main/java/com/universalrp/smsguard/ui/BilingualCatalog.kt"

GOOD = '''package fake.ui

data class GuardUiStrings(
    val appTitle: String,
    val appSubtitle: String,
    val verifiedCredits: String,
)

object BilingualCatalog {
    val TAMIL = GuardUiStrings(
        appTitle = "குறுஞ்செய்தி காவலன்",
        appSubtitle = "குறுஞ்செய்தி காவலன் · சைபர் பாதுகாப்பு",
        verifiedCredits = "உறுதிப்படுத்தப்பட்ட வரவுகள்",
    )

    val ENGLISH = GuardUiStrings(
        appTitle = "SMS Guard",
        appSubtitle = "SMS Guard · Cyber Shield & Finance",
        verifiedCredits = "Verified credits",
    )
}
'''

# The two subtitles as they were really shipped: Tamil mode named the app in English, and English
# mode named it in Tamil.
SWAPPED = GOOD.replace(
    '        appSubtitle = "குறுஞ்செய்தி காவலன் · சைபர் பாதுகாப்பு",\n',
    '        appSubtitle = "SMS Guard · சைபர் பாதுகாப்பு",\n',
).replace(
    '        appSubtitle = "SMS Guard · Cyber Shield & Finance",\n',
    '        appSubtitle = "குறுஞ்செய்தி காவலன் · Cyber Shield & Finance",\n',
)

# "verified credits" left as English inside the Tamil block.
LATIN_ONLY_TAMIL = GOOD.replace(
    '        verifiedCredits = "உறுதிப்படுத்தப்பட்ட வரவுகள்",\n',
    '        verifiedCredits = "Verified credits",\n',
)

# A declared field the English block never assigns.
MISSING_ENGLISH = GOOD.replace(
    '        verifiedCredits = "Verified credits",\n', ""
)

# Odia mixed into an otherwise-Tamil value, which is what \\u0b.. escapes produce when they stray
# low: the value still holds Tamil script, so it has to be caught by the script check and not by
# the Latin-only one.
ODIA_IN_TAMIL = GOOD.replace(
    '        verifiedCredits = "உறுதிப்படுத்தப்பட்ட வரவுகள்",\n',
    '        verifiedCredits = "\u0b13\u0b2a\u0b4d\u0b2a\u0b4b \u0bb5\u0bb0\u0bb5\u0bc1\u0b95\u0bb3\u0bcd",\n',
)

# An empty Tamil value.
EMPTY_TAMIL = GOOD.replace(
    '        verifiedCredits = "உறுதிப்படுத்தப்பட்ட வரவுகள்",\n',
    '        verifiedCredits = "   ",\n',
)


class SmsGuardCatalogGateTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.root = Path(self._tmp.name)
        self.catalog = self.root / CATALOG_REL
        self.catalog.parent.mkdir(parents=True, exist_ok=True)

        self._orig_root = mod.ROOT
        mod.ROOT = self.root
        self.addCleanup(self._restore)

    def _restore(self) -> None:
        mod.ROOT = self._orig_root

    def write(self, text: str) -> None:
        self.catalog.write_text(text, encoding="utf-8")

    def test_accepts_a_catalog_where_both_blocks_are_complete(self) -> None:
        self.write(GOOD)
        self.assertEqual(mod.verify_smsguard_bilingual_catalog(), 3)

    def test_rejects_swapped_language_blocks(self) -> None:
        # Tamil mode named the app in English and English mode named it in Tamil, and every
        # screenshot the owner sent showed the app bar reading "SMS Guard" in Tamil mode.
        self.write(SWAPPED)
        with self.assertRaises(ValueError) as caught:
            mod.verify_smsguard_bilingual_catalog()
        self.assertIn("swapped", str(caught.exception))

    def test_rejects_a_latin_only_value_in_the_tamil_block(self) -> None:
        self.write(LATIN_ONLY_TAMIL)
        with self.assertRaises(ValueError) as caught:
            mod.verify_smsguard_bilingual_catalog()
        self.assertIn("verified credits", str(caught.exception))

    def test_rejects_a_field_the_english_block_never_assigns(self) -> None:
        self.write(MISSING_ENGLISH)
        with self.assertRaises(ValueError) as caught:
            mod.verify_smsguard_bilingual_catalog()
        self.assertIn("English block", str(caught.exception))

    def test_rejects_an_undeclared_field(self) -> None:
        self.write(GOOD.replace(
            '        verifiedCredits = "Verified credits",\n',
            '        verifiedCredits = "Verified credits",\n        strayField = "Stray",\n',
        ))
        with self.assertRaises(ValueError) as caught:
            mod.verify_smsguard_bilingual_catalog()
        self.assertIn("undeclared", str(caught.exception))

    def test_rejects_odia_in_the_tamil_block(self) -> None:
        self.write(ODIA_IN_TAMIL)
        with self.assertRaises(ValueError) as caught:
            mod.verify_smsguard_bilingual_catalog()
        self.assertIn("Oriya", str(caught.exception))

    def test_rejects_an_empty_tamil_value(self) -> None:
        self.write(EMPTY_TAMIL)
        with self.assertRaises(ValueError) as caught:
            mod.verify_smsguard_bilingual_catalog()
        self.assertIn("empty Tamil", str(caught.exception))

    def test_rejects_a_catalog_that_declares_nothing(self) -> None:
        self.write("package fake.ui\n\nobject BilingualCatalog\n")
        with self.assertRaises(ValueError) as caught:
            mod.verify_smsguard_bilingual_catalog()
        self.assertIn("no String fields", str(caught.exception))

    def test_runs_over_the_real_repository(self) -> None:
        mod.ROOT = self._orig_root
        self.assertGreaterEqual(mod.verify_smsguard_bilingual_catalog(), 100)

    def test_real_repository_has_no_undeclared_or_unassigned_field(self) -> None:
        # The gate's whole point is that the real catalog is complete; assert the two failure
        # modes it exists for are absent from the shipped file.
        mod.ROOT = REAL_ROOT
        catalog = mod.ROOT / CATALOG_REL
        text = catalog.read_text(encoding="utf-8")
        declared = set(re.findall(r"^    val (\w+): String,", text, re.M))
        tamil_names = {
            name
            for name, value in re.findall(r'^        (\w+) = "([^"]*)",$', text, re.M)
            if any("\u0B80" <= ch <= "\u0BFF" for ch in value)
        }
        english_names = {
            name
            for name, value in re.findall(r'^        (\w+) = "([^"]*)",$', text, re.M)
            if not any("\u0B80" <= ch <= "\u0BFF" for ch in value)
        }
        self.assertEqual(declared, tamil_names)
        self.assertEqual(declared, english_names)
        # The two hardcoded English subtitles FinanceTab.kt used to carry are gone.
        tab = (mod.ROOT / "smsguard/src/main/java/com/universalrp/smsguard/ui/FinanceTab.kt").read_text(
            encoding="utf-8"
        )
        self.assertNotIn('"${summary.creditCount} verified credits"', tab)
        self.assertNotIn('"${summary.debitCount} verified debits"', tab)
        # The trend sentence is no longer pinned to one line, which is what clipped it.
        self.assertNotIn(
            "changeText,\n                        color = changeColor,\n"
            "                        fontWeight = FontWeight.SemiBold,\n"
            "                        fontSize = 12.sp,\n"
            "                        maxLines = 1,",
            tab,
        )


import re  # noqa: E402  (used by the last test)

# The repository root as it was when this module was imported, so a test that swaps mod.ROOT for a
# temporary tree can still assert things about the real files.
REAL_ROOT = mod.ROOT

if __name__ == "__main__":
    unittest.main()
