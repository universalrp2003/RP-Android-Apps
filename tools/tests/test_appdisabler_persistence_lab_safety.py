"""The reboot experiment must stay in the debug source set, not target user packages."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
DEBUG = ROOT / 'appdisabler/app/src/debug'


class PersistenceLabSafety(unittest.TestCase):
    def test_lab_is_debug_only_and_refuses_physical_devices_and_other_packages(self):
        activity = (DEBUG / 'java/com/universalrp/appdisabler/lab/PersistenceLabActivity.java').read_text()
        probe = (DEBUG / 'java/com/universalrp/appdisabler/lab/PackageProbe.java').read_text()
        self.assertIn('Build.HARDWARE.equals("ranchu")', activity)
        self.assertIn('Build.HARDWARE.equals("goldfish")', activity)
        self.assertIn('id.trim().equals("2000")', activity)
        self.assertIn('com.universalrp.appdisabler.fixture', activity)
        self.assertIn('com.universalrp.appdisabler.fixture', probe)
        self.assertNotIn('getStringExtra("package")', activity)
        self.assertIn('.lab.PersistenceLabActivity', (DEBUG / 'AndroidManifest.xml').read_text())
        self.assertNotIn('PersistenceLabActivity', (ROOT / 'appdisabler/app/src/main/AndroidManifest.xml').read_text())

    def test_release_gate_examines_apk_contents(self):
        workflow = (ROOT / '.github/workflows/appdisabler-candidate.yml').read_text()
        verifier = (ROOT / 'tools/verify_disabler_lab_is_debug_only.py').read_text()
        self.assertIn('python3 tools/verify_disabler_lab_is_debug_only.py', workflow)
        self.assertIn('MARKER not in release_dex', verifier)
        self.assertIn('release_manifest', verifier)
