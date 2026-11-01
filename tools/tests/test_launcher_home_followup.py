from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'launcher/src/main/java/com/universalrp/launcher'

class LauncherHomeFollowupTests(unittest.TestCase):
    def test_one_home_scroll_and_adjacent_explicit_volume_controls(self):
        text = (SRC / 'MainActivity.kt').read_text()
        self.assertNotIn('LazyVerticalGrid', text)
        self.assertNotIn('BUTTON_GRID_HEIGHT_DP', text)
        self.assertIn('for (up in listOf(false, true))', text)
        self.assertIn('symbol = if (up) "+" else "−"', text)
        self.assertIn('volumeReadout()', text)
        self.assertIn('.heightIn(min =', text)

    def test_smaller_status_and_explicit_clock_policy(self):
        text = (SRC / 'MainActivity.kt').read_text()
        self.assertIn('text = batteryText(),\n                    fontSize = 13.sp', text)
        self.assertIn('LauncherClock.format', text)
        policy = (SRC / 'HomePolicies.kt').read_text()
        self.assertIn('"AM" else "PM"', policy)
        self.assertIn('Locale.ROOT', policy)

    def test_app_favourites_after_contacts_before_emergency(self):
        text = (SRC / 'MainActivity.kt').read_text()
        self.assertLess(text.index('item { FavouritesCard'), text.index('FavouriteAppsCard(packages'))
        self.assertLess(text.index('FavouriteAppsCard(packages'), text.index('label = LocalContext.current.getString(BigButtons.emergency()'))
        store = (SRC / 'FavouriteApps.kt').read_text()
        self.assertIn('launcher_favourite_apps', store)
        self.assertNotIn('launcher_favourites"', store)
        self.assertIn('getLaunchIntentForPackage', store)
        self.assertNotIn('startActivity', store)

    def test_real_levels_only_while_foreground_with_explicit_volume_target(self):
        text = (SRC / 'VolumeReadout.kt').read_text()
        for fragment in ('repeatOnLifecycle(Lifecycle.State.STARTED)', 'getStreamVolume', 'getStreamMaxVolume', 'STREAM_RING', 'STREAM_MUSIC'):
            self.assertIn(fragment, text)
        self.assertNotIn('setStreamVolume', text)
        actions = (SRC/'LauncherActions.kt').read_text()
        self.assertIn('audio.setStreamVolume(stream, target, 0)', actions)
        self.assertNotIn('audio.adjustVolume(', actions)
        manifest = (ROOT/'launcher/src/main/AndroidManifest.xml').read_text()
        self.assertIn('android.permission.MODIFY_AUDIO_SETTINGS', manifest)
        self.assertNotIn('android.permission.ACCESS_NOTIFICATION_POLICY', manifest)
        self.assertIn('RINGER_MODE_NORMAL', actions)
        self.assertIn('getStreamVolume(stream) == target', actions)

    def test_public_links_and_accepted_apps_not_advanced(self):
        sister = (ROOT/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        row = sister.split('id = "launcher",', 1)[1].split('),', 1)[0]
        self.assertIn('version = "1.7"', row)
        self.assertIn('publishedVersion = "1.5"', row)
        catalog = (ROOT/'site/catalog-v2.json').read_text()
        self.assertIn('elderly-launcher-v1.5', catalog)
        self.assertNotIn('elderly-launcher-v1.6', catalog)

    def test_compact_tiles_and_removal_live_only_in_management(self):
        text = (SRC/'MainActivity.kt').read_text()
        card = text.split('private fun FavouriteAppsCard',1)[1].split('/** Shared two-column',1)[0]
        self.assertIn('packages.chunked(2)', card)
        self.assertNotIn('TextButton', card)
        self.assertNotIn('favourite_apps_remove', card)
        self.assertIn('entries.chunked(2)', text)
        self.assertIn('if (selected) FavouriteApps.remove', text)
