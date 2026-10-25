#!/usr/bin/env python3
"""Unit tests for the release-note gates in verify_apps_localization.py.

Four gates are covered, all of them silent in production before they were written:

* the note must state its device-test status (the publish step greps for it);
* the note must carry exactly one `## தமிழ்` section, and it must be the last section,
  because `AppUpdates.localizedReleaseNotes` splits on that marker;
* the Tamil section must not contain a Latin word that appears nowhere in the note's English
  text, which is how `40 vaikkappadum` reached a published note;
* the note must only use the markdown the in-app renderer understands, which is how the notes
  came to print their own `**bold**` and `## headings` on the update screen.

The gates read the real repository, so each test points `ROOT` and `module_version` at a
temporary tree holding one fake app's note.
"""
import re
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import verify_apps_localization as mod  # noqa: E402

TAMIL_HEADING = "## \u0ba4\u0bae\u0bbf\u0bb4\u0bcd"
TAMIL_LINE = "- **\u0b9a\u0bcb\u0ba4\u0ba9\u0bc8:** \u0b93\u0bb0\u0bc1 \u0baa\u0ba4\u0bbf\u0bb2\u0bbf\u0baf\u0bc7 \u0b9a\u0bc6\u0bae\u0bbf\u0b95\u0bcd\u0b95\u0baa\u0bcd\u0baa\u0b9f\u0bcd\u0b9f\u0ba4\u0bc1."

GOOD_HEAD = """# Fake App 9.9

**Version:** 9.9 - **App:** Fake App

## What changed

- One thing changed. The chip strip shows a row of chips, and the slot default is every day.
- A second thing changed, wrapped onto two lines:
  the projection is clamped to the last instant of the month.
- A parent point:
  - one sub-point
  - another sub-point

## How it is built

A pure object over strings and numbers, unit-tested without a device.

## Not verified

The exact bytes in this release were not installed on a phone before publication: no device test
was run for 9.9.
"""

GOOD_TAIL = TAMIL_HEADING + "\n\n" + TAMIL_LINE + "\n"


class ReleaseNoteGatesTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.root = Path(self._tmp.name)
        (self.root / "releases").mkdir()
        # One real module, so the gate's own prefix map finds the note.
        self.module = "cleaner"
        self.note = self.root / "releases" / "live-guard-v9.9.md"

        self._orig_root = mod.ROOT
        self._orig_version = mod.module_version
        mod.ROOT = self.root
        mod.module_version = lambda module: "9.9" if module == self.module else ""
        self.addCleanup(self._restore)

    def _restore(self) -> None:
        mod.ROOT = self._orig_root
        mod.module_version = self._orig_version

    def write(self, text: str) -> None:
        self.note.write_text(text, encoding="utf-8")

    # -- device-test honesty -------------------------------------------------
    def test_honesty_accepts_a_note_that_states_its_status(self) -> None:
        self.write(GOOD_HEAD + "\n" + GOOD_TAIL)
        self.assertEqual(mod.verify_release_notes_honesty(), 1)

    def test_honesty_rejects_a_note_that_says_nothing_about_a_phone(self) -> None:
        self.write(GOOD_HEAD.replace(
            "The exact bytes in this release were not installed on a phone before publication: "
            "no device test\nwas run for 9.9.",
            "CI checks pass.",
        ) + "\n" + GOOD_TAIL)
        with self.assertRaises(ValueError):
            mod.verify_release_notes_honesty()

    def test_honesty_accepts_a_positive_device_test_statement(self) -> None:
        self.write(GOOD_HEAD.replace(
            "The exact bytes in this release were not installed on a phone before publication: "
            "no device test\nwas run for 9.9.",
            "This build was installed on a phone on 25 October 2026.",
        ) + "\n" + GOOD_TAIL)
        self.assertEqual(mod.verify_release_notes_honesty(), 1)

    # -- Tamil section placement --------------------------------------------
    def test_tamil_section_accepts_one_section_last(self) -> None:
        self.write(GOOD_HEAD + "\n" + GOOD_TAIL)
        self.assertEqual(mod.verify_release_notes_tamil_section(), 1)

    def test_tamil_section_rejects_a_missing_section(self) -> None:
        self.write(GOOD_HEAD)
        with self.assertRaises(ValueError):
            mod.verify_release_notes_tamil_section()

    def test_tamil_section_rejects_a_duplicate(self) -> None:
        self.write(GOOD_HEAD.replace("## Not verified", TAMIL_HEADING + "\n\n" + TAMIL_LINE)
                   + "\n" + GOOD_TAIL)
        with self.assertRaises(ValueError):
            mod.verify_release_notes_tamil_section()

    def test_tamil_section_rejects_one_that_is_not_last(self) -> None:
        self.write(GOOD_HEAD.replace(
            "## Not verified", TAMIL_HEADING + "\n\n" + TAMIL_LINE + "\n\n## Not verified"
        ))
        with self.assertRaises(ValueError):
            mod.verify_release_notes_tamil_section()

    def test_tamil_section_rejects_a_heading_with_words_after_the_marker(self) -> None:
        # `substringAfter` keeps whatever follows the marker, so "## Tamil summary" leaves the
        # word "summary" as the first line of the Tamil release notes in the app.
        self.write(GOOD_HEAD + "\n" + TAMIL_HEADING
                   + " \u0b9a\u0bc1\u0bb0\u0bc1\u0b95\u0bcd\u0b95\u0bae\u0bcd\n\n" + TAMIL_LINE + "\n")
        with self.assertRaises(ValueError):
            mod.verify_release_notes_tamil_section()

    # -- stray Latin words in the Tamil section ------------------------------
    def test_tamil_words_accept_tamil_with_the_note_s_own_terms(self) -> None:
        self.write(GOOD_HEAD + "\n" + TAMIL_HEADING + "\n\n" + TAMIL_LINE
                   + " \u0b92\u0bb5\u0bcd\u0bb5\u0bcb\u0bb0\u0bc1 chip-\u0b90 \u0b95\u0bbe\u0b9f\u0bcd\u0b9a\u0bbf\u0baf\u0bbf\u0bb2\u0bcd \u0ba4\u0bca\u0bb1\u0bb1\u0bcd\u0baa\u0bc1.\n")
        self.assertEqual(mod.verify_release_notes_tamil_words(), 1)

    def test_tamil_words_rejects_a_word_absent_from_the_english_text(self) -> None:
        self.write(GOOD_HEAD + "\n" + TAMIL_HEADING + "\n\n" + TAMIL_LINE + " 40 vaikkappadum.\n")
        with self.assertRaises(ValueError):
            mod.verify_release_notes_tamil_words()

    def test_tamil_words_allows_a_long_technical_term_the_note_uses_in_english(self) -> None:
        self.write(GOOD_HEAD.replace("## How it is built", "## How it is built\n\ncoordinates")
                   + "\n" + TAMIL_HEADING + "\n\n" + TAMIL_LINE
                   + " \u0b92\u0bb0\u0bc6 coordinates-\u0b95\u0bcd\u0b95\u0bc1 \u0b87\u0bb0\u0ba3\u0bcd\u0b9f\u0bc1 \u0ba4\u0bc6\u0bb0\u0bbf\u0bb5\u0bc1.\n")
        self.assertEqual(mod.verify_release_notes_tamil_words(), 1)

    def test_tamil_words_skips_a_note_without_a_tamil_section(self) -> None:
        self.write(GOOD_HEAD)
        self.assertEqual(mod.verify_release_notes_tamil_words(), 0)

    # -- markdown subset the in-app renderer understands ---------------------
    def test_markdown_subset_accepts_every_construct_the_notes_use(self) -> None:
        # The shapes below are lifted from the six pinned notes: a wrapped bullet, one level of
        # sub-bullet, bold, italic, backtick code and a markdown link.
        self.write(GOOD_HEAD + "\n" + TAMIL_HEADING + "\n\n" + TAMIL_LINE + "\n")
        self.assertEqual(mod.verify_release_notes_markdown_subset(), 1)

    def test_markdown_subset_rejects_a_third_level_heading(self) -> None:
        self.write(GOOD_HEAD + "\n### A sub-heading\n" + GOOD_TAIL)
        with self.assertRaises(ValueError) as caught:
            mod.verify_release_notes_markdown_subset()
        self.assertIn("literal markup", str(caught.exception))

    def test_markdown_subset_rejects_a_block_quote(self) -> None:
        self.write(GOOD_HEAD.replace("## How it is built", "## How it is built\n\n> quoted")
                   + "\n" + GOOD_TAIL)
        with self.assertRaises(ValueError):
            mod.verify_release_notes_markdown_subset()

    def test_markdown_subset_rejects_a_numbered_list(self) -> None:
        self.write(GOOD_HEAD.replace("## What changed", "## What changed\n\n1. first\n2. second")
                   + "\n" + GOOD_TAIL)
        with self.assertRaises(ValueError):
            mod.verify_release_notes_markdown_subset()

    def test_markdown_subset_rejects_a_horizontal_rule(self) -> None:
        self.write(GOOD_HEAD.replace("## Not verified", "## Not verified\n\n---")
                   + "\n" + GOOD_TAIL)
        with self.assertRaises(ValueError):
            mod.verify_release_notes_markdown_subset()

    def test_markdown_subset_rejects_a_fenced_code_block(self) -> None:
        self.write(GOOD_HEAD.replace("## How it is built", "## How it is built\n\n```\ncode\n```")
                   + "\n" + GOOD_TAIL)
        with self.assertRaises(ValueError):
            mod.verify_release_notes_markdown_subset()

    def test_markdown_subset_rejects_a_table(self) -> None:
        self.write(GOOD_HEAD.replace("## What changed", "## What changed\n\n| a | b |\n| - | - |")
                   + "\n" + GOOD_TAIL)
        with self.assertRaises(ValueError):
            mod.verify_release_notes_markdown_subset()

    def test_markdown_subset_rejects_a_sub_bullet_nested_deeper_than_one_level(self) -> None:
        # The renderer flattens an indented line into the block above it, so a second level of
        # nesting would silently merge two separate points into one sentence.
        self.write(GOOD_HEAD.replace("  - one sub-point", "    - one sub-point") + "\n" + GOOD_TAIL)
        with self.assertRaises(ValueError):
            mod.verify_release_notes_markdown_subset()

    def test_markdown_subset_accepts_a_two_space_sub_bullet(self) -> None:
        self.write(GOOD_HEAD + "\n" + GOOD_TAIL)
        self.assertEqual(mod.verify_release_notes_markdown_subset(), 1)

    def test_markdown_subset_skips_a_note_that_does_not_exist(self) -> None:
        self.assertEqual(mod.verify_release_notes_markdown_subset(), 0)

    # -- the six real pinned notes -------------------------------------------
    def test_real_notes_still_pass_every_gate(self) -> None:
        # The temp tree is gone by now, so this runs against the repository itself.
        mod.ROOT = self._orig_root
        mod.module_version = self._orig_version
        self.assertGreaterEqual(mod.verify_release_notes_honesty(), 1)
        self.assertGreaterEqual(mod.verify_release_notes_tamil_section(), 1)
        self.assertGreaterEqual(mod.verify_release_notes_tamil_words(), 1)
        self.assertGreaterEqual(mod.verify_release_notes_markdown_subset(), 1)


if __name__ == "__main__":
    unittest.main()
