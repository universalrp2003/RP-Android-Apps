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

    def test_artist_tab_is_persistent_bilingual_and_outside_station_list(self):
        screen = SCREEN.split("fun RadioScreen(", 1)[1].split("fun ", 1)[0]
        tabs, station_list = screen.split("// Cache rows in composition", 1)
        self.assertIn('TabRow(selectedTabIndex = if (showArtists) 1 else 0', tabs)
        self.assertIn('"நிலையங்கள்" else "Stations"', tabs)
        self.assertIn('"கலைஞர்கள் (${ArtistBrowse.artists.size})"', tabs)
        self.assertIn('"Artists (${ArtistBrowse.artists.size})"', tabs)
        self.assertIn('ArtistScreen(onBack = { showArtists = false }', tabs)
        self.assertIn('LazyColumn(', station_list)
        self.assertNotIn('TabRow(', station_list)

    def test_weather_monitor_is_not_started_by_radio_app(self):
        root = (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/ui/AppRoot.kt").read_text()
        main = (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/MainActivity.kt").read_text()
        service = (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/player/PlaybackService.kt").read_text()
        more = (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/ui/PlayScreens.kt").read_text()
        for source in (root, main, service, more, SCREEN):
            self.assertNotIn("RadioWeatherMonitor.", source)
        self.assertIn("clearWeatherNotification(this)", main)

    def test_light_theme_and_green_header(self):
        theme = (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/ui/Theme.kt").read_text()
        self.assertIn("lightColorScheme(", theme)
        self.assertIn("Color(0xFFF4F6F1)", theme)
        self.assertIn("background(HeaderGreen)", (ROOT / "radio/src/main/java/com/universalrp/tamilnadufm/ui/AppRoot.kt").read_text())

    def test_station_toolbar_uses_compact_accessible_directory_action(self):
        self.assertNotIn('IconChip(Icons.Filled.Language, "Live directory")', SCREEN)
        self.assertIn('contentDescription = AppStrings.translate("Live directory")', SCREEN)
        self.assertNotIn("typography.titleLarge", SCREEN.split("fun RadioScreen(", 1)[1].split("fun ", 1)[0])

    def test_gallery_shows_names_and_opens_confirmation_not_playback(self):
        self.assertIn("state.visible.chunked(gridColumns)", SCREEN)
        self.assertIn("onOpen = { selectedIconStation = station }", SCREEN)
        self.assertIn("title = { Text(station.name, color = TextPrimary) }", SCREEN)
        self.assertIn("vm.play(station)", SCREEN)
        self.assertIn("StationArtwork(station, Modifier.fillMaxWidth().aspectRatio(1.55f), decorative = true, crop = cropArtwork)", SCREEN)
        self.assertIn("Text(label", SCREEN)
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
