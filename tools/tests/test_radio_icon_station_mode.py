from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCREEN = (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/ui/StationsScreens.kt").read_text(encoding="utf-8")
LANGUAGE = (ROOT / "shared/src/main/java/com/universalrp/updates/AppLanguage.kt").read_text(encoding="utf-8")


class RadioIconStationModeTest(unittest.TestCase):
    def test_gallery_default_and_list_toggle_are_bilingual(self):
        self.assertIn("var iconView by rememberSaveable { mutableStateOf(true) }", SCREEN)
        self.assertIn("if (iconView) Icons.Filled.ViewList else Icons.Filled.GridView", SCREEN)
        self.assertIn('"Switch to icon view" to "சின்னக் காட்சிக்கு மாறு"', LANGUAGE)
        self.assertIn('"Switch to list view" to "பட்டியல் காட்சிக்கு மாறு"', LANGUAGE)

    def test_compose_calls_stay_outside_lazy_list_builder(self):
        radio_screen = SCREEN.split("fun RadioScreen(", 1)[1].split("fun ", 1)[0]
        rows = "val iconRows = remember(state.visible, gridColumns) { state.visible.chunked(gridColumns) }"
        self.assertEqual(radio_screen.count(rows), 1)
        self.assertLess(radio_screen.index(rows), radio_screen.index("LazyColumn("))
        self.assertIn("import androidx.compose.material3.TextButton", SCREEN)

    def test_weather_places_are_inside_dialog_not_above_station_browser(self):
        root = (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/ui/AppRoot.kt").read_text()
        main_content, dialog = root.split("if (showWeatherDialog)", 1)
        self.assertNotIn("WeatherMiniAlertBanner(", main_content)
        self.assertNotIn('"Saved places"', main_content)
        self.assertIn("placesContent = {", dialog)
        self.assertIn("selectSavedPlace(rootContext, place)", dialog)
        self.assertIn("removeSavedPlace(", dialog)

    def test_station_toolbar_uses_compact_accessible_directory_action(self):
        self.assertNotIn('IconChip(Icons.Filled.Language, "Live directory")', SCREEN)
        self.assertIn('contentDescription = AppStrings.translate("Live directory")', SCREEN)
        self.assertNotIn("typography.titleLarge", SCREEN.split("fun RadioScreen(", 1)[1].split("fun ", 1)[0])

    def test_gallery_shows_names_and_opens_confirmation_not_playback(self):
        self.assertIn("state.visible.chunked(gridColumns)", SCREEN)
        self.assertIn("onOpen = { selectedIconStation = station }", SCREEN)
        self.assertIn("title = { Text(station.name, color = TextPrimary) }", SCREEN)
        self.assertIn("vm.play(station)", SCREEN)
        self.assertIn("StationArtwork(station, Modifier.fillMaxWidth().aspectRatio(1.55f), decorative = true)", SCREEN)
        self.assertIn("Text(station.name", SCREEN)
        self.assertIn("IconButton(onClick = { vm.toggleFavourite(station) })", SCREEN)
        self.assertIn("IconButton(onClick = { vm.shareStation(station) })", SCREEN)
        self.assertIn("heldForReview(station.url)", SCREEN)

    def test_logo_loading_is_bounded_and_missing_art_has_fallback(self):
        source = (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/ui/Components.kt").read_text()
        for requirement in ("MAX_IMAGE_BYTES = 2 * 1024 * 1024", "TARGET_PIXELS = 512",
                            "Semaphore(4)", "allocationByteCount", "ContentScale.Fit",
                            "Text(station.monogram", "value = null"):
            self.assertIn(requirement, source)



if __name__ == "__main__":
    unittest.main()
