from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class ArtistCollectionContractTest(unittest.TestCase):
    def test_artist_shortcuts_have_no_external_collection(self):
        browse = (ROOT / 'radio/src/main/java/com/universalrp/tamilnadufm/ui/ArtistBrowse.kt').read_text()
        screens = (ROOT / 'radio/src/main/java/com/universalrp/tamilnadufm/ui/StationsScreens.kt').read_text()
        self.assertNotIn('externalCollections', browse + screens)
        self.assertNotIn('spotify.com', browse.lower() + screens.lower())
        self.assertIn('Station-name matches for this artist', screens)
        self.assertIn('A song collection is not available for every artist', screens)
