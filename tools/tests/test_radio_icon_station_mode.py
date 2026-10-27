from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCREEN = (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/ui/StationsScreens.kt").read_text(encoding="utf-8")
LANGUAGE = (ROOT / "shared/src/main/java/com/universalrp/updates/AppLanguage.kt").read_text(encoding="utf-8")


class RadioIconStationModeTest(unittest.TestCase):
    def test_toggle_preserves_list_default_and_provides_bilingual_accessibility(self):
        self.assertIn("var iconView by rememberSaveable { mutableStateOf(false) }", SCREEN)
        self.assertIn("if (iconView) Icons.Filled.ViewList else Icons.Filled.GridView", SCREEN)
        self.assertIn('"Switch to icon view" to "சின்னக் காட்சிக்கு மாறு"', LANGUAGE)
        self.assertIn('"Switch to list view" to "பட்டியல் காட்சிக்கு மாறு"', LANGUAGE)

    def test_grid_is_four_columns_and_station_tap_opens_confirmation_not_playback(self):
        self.assertIn("state.visible.chunked(4)", SCREEN)
        self.assertIn(".clickable { selectedIconStation = station }", SCREEN)
        self.assertIn("title = { Text(station.name, color = TextPrimary) }", SCREEN)
        self.assertIn("vm.play(station)", SCREEN)
        self.assertIn("StationLogo(station, 52.dp)", SCREEN)


if __name__ == "__main__":
    unittest.main()
