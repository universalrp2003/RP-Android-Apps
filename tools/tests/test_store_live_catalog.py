import copy
import importlib.util
import json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('build_store_catalog',ROOT/'tools/build_store_catalog.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class CatalogGenerationTest(unittest.TestCase):
    def setUp(self):
        self.snapshot=json.loads((ROOT/'tamilanstore/src/main/assets/catalog.json').read_text())
        self.releases=[]
        for app in self.snapshot['apps']:
            self.releases.append(dict(tag_name=app['releaseUrl'].split('/')[-1],draft=False,prerelease=False,assets=[dict(name=app['url'].split('/')[-1],browser_download_url=app['url'],size=app['size'],digest='sha256:'+app['sha256'])]))
    def test_exact_public_metadata(self):
        self.assertEqual(self.snapshot,module.generate(self.snapshot,self.releases))
    def test_draft_missing_digest_foreign_url_fail_closed(self):
        for field,value in [('digest',None),('browser_download_url','https://evil.example/app.apk')]:
            releases=copy.deepcopy(self.releases);releases[0]['assets'][0][field]=value
            with self.assertRaises(ValueError):module.generate(self.snapshot,releases)
        self.releases[0]['draft']=True
        with self.assertRaises(ValueError):module.generate(self.snapshot,self.releases)
    def test_live_test_is_not_deleted_or_silently_ignored(self):
        gradle=(ROOT/'smsguard/build.gradle.kts').read_text()
        self.assertIn('tasks.register<Test>("testLiveSmsVoice")',gradle)
        self.assertNotIn('ignoreFailures = true',gradle)
        flow=(ROOT/'.github/workflows/live-voice-check.yml').read_text()
        self.assertIn(':smsguard:testLiveSmsVoice',flow)
