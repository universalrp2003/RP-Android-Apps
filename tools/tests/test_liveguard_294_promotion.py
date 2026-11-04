import unittest
from pathlib import Path
from promote_verified_liveguard_294 import validate_receipt, SIGNER, OLD_TAG, OLD_SHA, BRANCH, TAG
class Scoped294Promotion(unittest.TestCase):
    def receipt(self):
        p=dict(source='a'*40,apk_sha256='b'*64,bytes=123)
        r=dict(source=p['source'],package='com.universalrp.cleansweep',version='2.94',code=99,sha256=p['apk_sha256'],bytes=123,signer=SIGNER,
               same_uid=True,data_preserved=True,preferences_preserved=True,restart_passed=True,native_283_settings_smoke=True,
               native_288_feature_notice_smoke=True,native_289_named_security_smoke=True,native_292_top_read_and_opt_in_smoke=True,native_294_pill_defaults_overlay=True,
               final_installed_apk_sha256=p["apk_sha256"],required_android35_native_tests=8,
               added_permissions=[],removed_permissions=[],baseline_tag=OLD_TAG,baseline_sha256=OLD_SHA,physical_phone_tested=False,play_protect_tested=False)
        return r,p
    def test_scoped_identity_and_baseline(self):
        self.assertEqual(BRANCH,'arena/282fce0a-rp-android-apps');self.assertEqual(TAG,'live-guard-v2.94');self.assertEqual(OLD_TAG,'live-guard-v2.93');r,p=self.receipt();validate_receipt(r,p)
    def test_wrong_bytes_or_old_private_candidate_rejected(self):
        for field,value in [('version','2.92'),('code',98),('sha256','c'*64),('signer','d'*64),('baseline_sha256','e'*64)]:
            r,p=self.receipt();r[field]=value
            with self.assertRaises(AssertionError):validate_receipt(r,p)
    def test_no_missing_upgrade_or_ui_evidence(self):
        for field in ('same_uid','preferences_preserved','native_292_top_read_and_opt_in_smoke','native_294_pill_defaults_overlay'):
            r,p=self.receipt();r[field]=False
            with self.assertRaises(AssertionError):validate_receipt(r,p)
    def test_publisher_has_immutable_and_gate_boundaries(self):
        s=(Path(__file__).resolve().parents[1]/'promote_verified_liveguard_294.py').read_text()
        for text in ('== 890','== 85','== 8','Never overwrite','Never replace tag',"for phase in ('draft', 'public')",'parse_badging(badging) == parse_badging(previous)'):
            self.assertIn(text,s)
        self.assertNotIn('assembleRelease',s)
        self.assertNotIn('release_all',s)
