import importlib.util
import io
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('geo_labels', Path(__file__).parents[1] / 'research/check_geo_labels.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class GeoLabelTests(unittest.TestCase):
    def test_exact_matching_missing_and_duplicates(self):
        acoustic = io.StringIO('scientific_name,common_name\nCorvus splendens,Crow\nFelis catus,Cat\n,Noise\n')
        geo = io.StringIO('x\tCorvus splendens\nx2\tCorvus splendens\ny\tOther\n')
        out = module.compare(acoustic, geo)
        self.assertEqual((out['acoustic_matched_rows'], out['acoustic_unmatched_rows']), (1, 2))
        self.assertEqual(out['geo_duplicate_scientific_names'], 1)
        self.assertEqual(out['house_crow_geo'], 2)
        self.assertEqual(out['domestic_cat_geo'], 0)

    def test_malformed_labels_fail_closed(self):
        with self.assertRaises(ValueError):
            module.compare(io.StringIO('scientific_name\nCat\n'), io.StringIO('broken\n'))
