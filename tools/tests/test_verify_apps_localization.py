#!/usr/bin/env python3
"""Unit tests for the release-note, Kotlin-source and script gates in verify_apps_localization.py.

Six gates are covered, all of them silent in production before they were written:

* the note must state its device-test status (the publish step greps for it);
* the note must carry exactly one `## தமிழ்` section, and it must be the last section,
  because `AppUpdates.localizedReleaseNotes` splits on that marker;
* the Tamil section must not contain a Latin word that appears nowhere in the note's English
  text, which is how `40 vaikkappadum` reached a published note;
* the note must only use the markdown the in-app renderer understands, which is how the notes
  came to print their own `**bold**` and `## headings` on the update screen;
* a `when` expression on an enum must cover every value, which is how adding
  `ReleaseNoteLineKind.SubBullet` reddened two CI runs on a compile error;
* no Indic script other than Tamil may reach the published site, which is how `\\u0b..` escapes
  produced half-Odia Tamil in `site/index.html` twice.

The gates read the real repository, so each test points `ROOT` and `module_version` at a
temporary tree holding one fake app's note.
"""
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

GOOD_KOTLIN = """package fake

enum class Shade { LIGHT, DARK, DIM }

object Pick {
    fun label(shade: Shade): String = when (shade) {
        Shade.LIGHT -> "light"
        Shade.DARK -> "dark"
        Shade.DIM -> "dim"
    }

    // A statement `when` needs no else, and must not be flagged.
    fun apply(shade: Shade) {
        when (shade) {
            Shade.LIGHT -> println("light")
            else -> println("other")
        }
    }
}
"""

GOOD_SITE = (
    '<!doctype html><html><body>\n'
    '<p class="ta">\u0ba4\u0bae\u0bbf\u0bb4\u0bcd \u0bae\u0b9f\u0bcd\u0b9f\u0bc1\u0bae\u0bc7 '
    '\u0b89\u0bb0\u0bc1\u0bb5\u0bbe\u0b95\u0bcd\u0b95\u0bc1\u0bae\u0bcd.</p>\n'
    "</body></html>\n"
)


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

    # -- `when` exhaustiveness over an enum ----------------------------------
    def _write_kotlin(self, source: str) -> None:
        (self.root / "src").mkdir(exist_ok=True)
        (self.root / "src" / "Fake.kt").write_text(source, encoding="utf-8")

    def test_when_gate_accepts_a_complete_expression_when(self) -> None:
        self._write_kotlin(GOOD_KOTLIN)
        self.assertGreaterEqual(mod.verify_kotlin_when_exhaustiveness(), 1)

    def test_when_gate_rejects_a_missing_branch(self) -> None:
        # This is the exact shape that failed to compile: a new enum value added, and the
        # `when` expression that strips a line's prefix left with four of five branches.
        self._write_kotlin(GOOD_KOTLIN.replace('        Shade.DIM -> "dim"\n', ""))
        with self.assertRaises(ValueError) as caught:
            mod.verify_kotlin_when_exhaustiveness()
        self.assertIn("Shade.DIM", str(caught.exception))

    def test_when_gate_allows_an_else_branch(self) -> None:
        self._write_kotlin(GOOD_KOTLIN.replace('        Shade.DIM -> "dim"\n', '        else -> "dim"\n'))
        self.assertGreaterEqual(mod.verify_kotlin_when_exhaustiveness(), 1)

    def test_when_gate_ignores_a_statement_when(self) -> None:
        # A `when` statement needs no else. The assignment is on the line above, and must not
        # make this look like an expression.
        self._write_kotlin(GOOD_KOTLIN.replace(
            "    fun apply(shade: Shade) {\n        when (shade) {\n",
            "    fun apply(shade: Shade) {\n        val picked =\n        when (shade) {\n",
        ).replace(
            '            Shade.LIGHT -> println("light")\n            else -> println("other")\n',
            '            Shade.LIGHT -> println("light")\n            Shade.DARK -> println("dark")\n',
        ))
        self.assertGreaterEqual(mod.verify_kotlin_when_exhaustiveness(), 1)

    def test_when_gate_ignores_a_value_used_inside_another_whens_body(self) -> None:
        # `navigate(Screen.HEALTH)` inside some other `when`'s body is not a branch on Screen.
        self._write_kotlin("""package fake

enum class Screen { HOME, HEALTH }

object Nav {
    fun go(route: Route): Unit = when (route) {
        Route.HEALTH -> pick(Screen.HEALTH)
        Route.HOME -> pick(Screen.HOME)
    }
}

enum class Route { HOME, HEALTH }
""")
        self.assertGreaterEqual(mod.verify_kotlin_when_exhaustiveness(), 2)

    def test_when_gate_runs_over_the_real_repository(self) -> None:
        mod.ROOT = self._orig_root
        self.assertGreaterEqual(mod.verify_kotlin_when_exhaustiveness(), 1)

    # -- Tamil, and only Tamil, in the published files -----------------------
    def _write_site(self, body: str) -> None:
        (self.root / "site").mkdir(exist_ok=True)
        (self.root / "site" / "index.html").write_text(body, encoding="utf-8")

    def test_tamil_block_gate_accepts_a_tamil_site(self) -> None:
        self.write(GOOD_HEAD + "\n" + GOOD_TAIL)
        self._write_site(GOOD_SITE)
        self.assertGreaterEqual(mod.verify_site_tamil_blocks(), 1)

    def test_tamil_block_gate_rejects_odia(self) -> None:
        # `\u0b13` is in the Odia block, not the Tamil one, and this is the exact mistake that
        # put half-Odia text into site/index.html.
        self.write(GOOD_HEAD + "\n" + GOOD_TAIL)
        self._write_site(GOOD_SITE.replace(
            "\u0ba4\u0bae\u0bbf\u0bb4\u0bcd", "\u0b13\u0baa\u0bcd\u0baa\u0bcb"
        ))
        with self.assertRaises(ValueError) as caught:
            mod.verify_site_tamil_blocks()
        self.assertIn("Oriya", str(caught.exception))

    def test_tamil_block_gate_rejects_bengali(self) -> None:
        self.write(GOOD_HEAD + "\n" + GOOD_TAIL)
        self._write_site(GOOD_SITE.replace(
            "\u0ba4\u0bae\u0bbf\u0bb4\u0bcd", "\u09a4\u09ae\u09bf\u09b4\u09cd"
        ))
        with self.assertRaises(ValueError) as caught:
            mod.verify_site_tamil_blocks()
        self.assertIn("Bengali", str(caught.exception))

    def test_tamil_block_gate_covers_the_real_repository(self) -> None:
        mod.ROOT = self._orig_root
        self.assertGreaterEqual(mod.verify_site_tamil_blocks(), 1)


if __name__ == "__main__":
    unittest.main()
