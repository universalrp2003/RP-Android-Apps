import importlib.util
import io
import pathlib
import unittest
import zipfile

import numpy as np
from scipy.io import wavfile

path = pathlib.Path(__file__).resolve().parents[1] / 'research' / 'catmeows_baseline.py'
spec = importlib.util.spec_from_file_location('catmeows_baseline', path)
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

class CatBaselineTests(unittest.TestCase):
    def test_input_and_checksum(self):
        audio = io.BytesIO(); wavfile.write(audio, 8000, np.zeros(800, dtype=np.int16))
        zbuf = io.BytesIO()
        with zipfile.ZipFile(zbuf, 'w') as z: z.writestr('dataset/B_ANI01_MC_FN_SIM01_101.wav', audio.getvalue())
        rows = m.read_dataset(zbuf.getvalue(), verify=False)
        self.assertEqual((rows[0][1], rows[0][2], len(rows[0][0])), ('B', 'ANI01', 15))
        self.assertTrue(np.isfinite(rows[0][0]).all())
        with self.assertRaises(ValueError): m.read_dataset(zbuf.getvalue())
    def test_wrong_audio_format(self):
        with self.assertRaises(ValueError): m.features(16000, np.zeros(800, dtype=np.int16))
    def test_evaluation_groups(self):
        rows = [(np.ones(15) * (i+j), 'BFI'[j], 'cat' + str(i)) for i in range(10) for j in range(3)]
        result = m.evaluate(rows)
        self.assertEqual(result['cats'], 10)
        self.assertTrue(all(f['test_cats'] == 2 and f['train_cats'] == 8 for f in result['folds']))
        self.assertEqual(sum(map(sum, result['feature_baseline']['confusion_rows_actual_B_F_I'])), 30)
