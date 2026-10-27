import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('continuous', Path(__file__).parents[1] / 'verify_continuous_playback.py')
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)

class ContinuousPlaybackGateTest(unittest.TestCase):
    def test_current_source_passes(self):
        gate.verify(gate.sources())

    def test_zero_sources_fail(self):
        with self.assertRaises(ValueError): gate.verify({})

    def test_reintroduce_stop_after_one_story_bug(self):
        data = gate.sources()
        data['podcast'] = data['podcast'].replace('!stepQueue(1)', 'true')
        with self.assertRaises(ValueError): gate.verify(data)

    def test_reintroduce_single_file_picker_bug(self):
        data = gate.sources()
        data['pulse_ui'] = data['pulse_ui'].replace('OpenMultipleDocuments()', 'OpenDocument()')
        with self.assertRaises(ValueError): gate.verify(data)

    def test_offline_queue_must_not_fall_back_to_streaming(self):
        data = gate.sources()
        data['podcast'] = data['podcast'].replace('0, store.queueOfflineOnly)', '0, false)')
        with self.assertRaises(ValueError): gate.verify(data)

    def test_random_setting_cannot_be_cosmetic(self):
        data = gate.sources()
        data['radio'] = data['radio'].replace('c.shuffleModeEnabled = _state.value.localShuffle', 'c.shuffleModeEnabled = false')
        with self.assertRaises(ValueError): gate.verify(data)

    def test_candidate_directory_must_not_link_to_unpublished_tags(self):
        data = gate.sources()
        data['links'] = data['links'].replace('${app.publishedVersion}', '${app.version}')
        with self.assertRaises(ValueError): gate.verify(data)
