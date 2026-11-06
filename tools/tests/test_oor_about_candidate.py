from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class OorAboutCandidateContracts(unittest.TestCase):
    def test_unique_version_and_owner_authorized_published_metadata(self):
        gradle=(ROOT/'oorarivippu/build.gradle.kts').read_text()
        self.assertIn('versionCode = 23',gradle);self.assertIn('versionName = "1.1.18"',gradle)
        # Candidate only: public directory remains on the immutable accepted1.1.17.
        sister=(ROOT/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        self.assertIn('version = "1.1.17",\n            publishedVersion = "1.1.17"',sister)
    def test_inspection_pins_accepted_previous_public_bytes(self):
        source=(ROOT/'tools/inspect_oor_family_candidate.py').read_text()
        for value in ("'1.1.16', 21",'oor-arivippu-v1.1.15','ec625cbfb6faa1bab680b71e4184d945913908662e56f898e695dea932c1abe5','Oor-Arivippu-v1.1.16-about-guide-candidate.apk'):
            self.assertIn(value,source)
    def test_required_real_native_about_and_family_gates(self):
        runner=(ROOT/'tools/run_oor_family_instrumentation.py').read_text()
        verifier=(ROOT/'tools/verify_oor_family_native.py').read_text()
        for value in ('OorAboutNativeTest','FamilyNativeTest','bilingualAboutGuideAndSavedFamilyFixture','OK (3 tests)'):
            self.assertIn(value,runner)
        self.assertIn('len(native)==3 and len(jvm)==30 and len(about)==4',verifier)
        self.assertIn("actual==r['sha256']",verifier)
        self.assertIn("['adb','exec-out','cat',installed]",verifier)
        self.assertIn("len(read.stdout)==r['bytes']",verifier)
        self.assertIn('read.returncode==0',verifier)
        self.assertIn("startswith('emulator-')",verifier)
    def test_fixture_is_seeded_on_old_actual_app_before_non_destructive_update(self):
        source=(ROOT/'tools/oor_family_upgrade_smoke.py').read_text()
        self.assertLess(source.index("'upgradePhase','seed'"),source.index("adb('install','-r',str(candidate))"))
        self.assertIn('digest(family_fixture)==family_hash',source)
        native=(ROOT/'oorarivippu/src/androidTest/java/com/oorarivippu/voiceannouncer/OorAboutNativeTest.kt').read_text()
        for value in ('assertEquals("1.1.15", version)','assertEquals("1.1.16", version)','FamilyStore.read(fixture).single()','AppGuides.oorArivippu.topics','listOf(false, true)','automation.waitForIdle(220, 5000)','650'):
            self.assertIn(value,native)
