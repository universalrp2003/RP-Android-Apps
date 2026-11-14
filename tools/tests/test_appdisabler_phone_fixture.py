"""The optional owner-test APK must stay inert and separate from the lab fixture."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID = '{http://schemas.android.com/apk/res/android}'


class PhoneFixtureSafety(unittest.TestCase):
    def test_manifest_is_inert(self):
        root = ET.parse(ROOT / 'tools/appdisabler-phone-fixture/AndroidManifest.xml').getroot()
        self.assertEqual(root.attrib['package'], 'com.universalrp.appdisabler.phonefixture')
        self.assertEqual({child.tag for child in root}, {'uses-sdk', 'application'})
        app = root.find('application')
        self.assertIsNotNone(app)
        self.assertEqual(app.attrib[ANDROID + 'hasCode'], 'false')
        self.assertEqual(len(app), 0)
        self.assertNotIn(ANDROID + 'permission', app.attrib)

    def test_build_uploads_only_an_ephemerally_signed_artifact(self):
        workflow = (ROOT / '.github/workflows/appdisabler-phone-fixture.yml').read_text()
        self.assertIn('keytool -genkeypair', workflow)
        self.assertIn('apksigner" verify', workflow)
        self.assertIn('sha256sum', workflow)
        self.assertIn('actions/upload-artifact@v4', workflow)
        self.assertNotIn('gh release', workflow)
