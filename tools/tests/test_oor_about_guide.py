"""Prevent another website-only completion while Oor's built-in help stays stale."""
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
import verify_apps_localization as localization
MAIN=ROOT/'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/MainActivity.kt'
GUIDE=ROOT/'shared/src/main/java/com/universalrp/updates/AppGuides.kt'

class OorAboutGuideContracts(unittest.TestCase):
    def test_current_shared_release_links_web_guide_and_credits(self):
        localization.verify_about_links()
        text=MAIN.read_text()
        for required in ('SisterApps.releaseLinks(excludeId = "oorarivippu")',
                         'SisterApps.OFFICIAL_WEBSITE_URL + "#app-oor-arivippu"',
                         'oor-arivippu-guide.html','${ui.aboutVersionLabel}: $installedVersion'):
            self.assertIn(required,text)
        self.assertNotIn('/releases/tag/',text)
        self.assertIn('https://github.com/universalrp2003/InternetRadio',text)
    def rejected_change(self,path,transform):
        read=Path.read_text
        def substitute(p,*args,**kwargs):
            text=read(p,*args,**kwargs)
            return transform(text) if p==path else text
        with patch.object(Path,'read_text',substitute),self.assertRaises(ValueError):
            localization.verify_about_links()
    def test_stale_hardcoded_tag_is_rejected(self):
        self.rejected_change(MAIN,lambda s:s+'\nhttps://github.com/universalrp2003/RP-Android-Apps/releases/tag/oor-arivippu-v1.1.9')
    def test_omitted_web_guide_or_current_family_topic_is_rejected(self):
        self.rejected_change(MAIN,lambda s:s.replace('oor-arivippu-guide.html','index.html'))
        self.rejected_change(GUIDE,lambda s:s.replace('Connecting family — up to six connections','Old topic'))
    def test_source_only_workflow_never_assembles_or_promotes_apk(self):
        text=(ROOT/'.github/workflows/oor-about-guide-check.yml').read_text()
        self.assertIn(':oorarivippu:compileReleaseKotlin',text)
        self.assertIn(':oorarivippu:testReleaseUnitTest',text)
        self.assertIn(':oorarivippu:lintRelease',text)
        self.assertNotIn('assembleRelease',text)
        self.assertNotIn('gh release',text)
        self.assertNotIn('RP_RELEASE_KEYSTORE_PASSWORD',text)
        self.assertIn('livePlaceAnnouncementSynthesisWorksForTamilAndEnglishStudioVoices',text)
        self.assertIn('Unexpected skipped test',text)
        self.assertIn('A required new guide test was skipped',text)
