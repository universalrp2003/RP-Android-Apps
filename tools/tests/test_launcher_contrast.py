import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_launcher_contrast import SOURCE, PALETTE, verify


class LauncherContrastTest(unittest.TestCase):
    def setUp(self):
        self.source = SOURCE.read_text()
        self.palette = PALETTE.read_text()

    def test_current_buttons_and_palette(self):
        self.assertGreaterEqual(verify(self.source, self.palette), 4)

    def test_reintroduced_inherited_foreground_is_rejected(self):
        broken = self.source.replace('contentColor = ButtonText,', '', 1)
        self.assertNotEqual(self.source, broken)
        with self.assertRaisesRegex(ValueError, 'inherits'):
            verify(broken, self.palette)

    def test_zero_matches_is_not_a_pass(self):
        with self.assertRaisesRegex(ValueError, 'No filled-button'):
            verify('', self.palette)

    def test_low_contrast_palette_is_rejected(self):
        broken = self.palette.replace('BUTTON_TEXT = 0xFFFFFFFFL', 'BUTTON_TEXT = 0xFF24402CL')
        self.assertNotEqual(self.palette, broken)
        with self.assertRaisesRegex(ValueError, 'below 4.5'):
            verify(self.source, broken)


if __name__ == '__main__':
    unittest.main()
