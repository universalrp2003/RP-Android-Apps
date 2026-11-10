"""Source wiring guard; not proof of audible voice/provider delivery."""
from pathlib import Path
import unittest

class NewsVoiceRotationTest(unittest.TestCase):
    def test_rotation_survives_process_restart_and_profile_stays_per_request(self):
        source = (Path(__file__).resolve().parents[2] / 'cleaner/src/main/java/com/universalrp/cleansweep/voice/Announcer.kt').read_text()
        self.assertIn('p.getInt(KEY_STUDIO_ROTATION, 0)', source)
        self.assertIn('putInt(KEY_STUDIO_ROTATION', source)
        self.assertIn('turn % 2 == 1', source)
        self.assertIn('pool[(turn / 2) % pool.size]', source)
        self.assertIn('VoiceGenderMode.RANDOM -> nextRandomStudioVoice(context)', source)
        self.assertIn('studioProfile ?: resolveStudioProfile(context, this)', source)
        self.assertNotIn('randomGenderFlip', source)
