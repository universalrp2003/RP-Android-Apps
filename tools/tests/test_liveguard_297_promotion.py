"""Rain release must not accept older artifacts or omit retained/native rain gates."""
import copy
import unittest
from promote_verified_liveguard_297 import validate_receipt, SIGNER, OLD_TAG, OLD_SHA
class RainReleasePromotion(unittest.TestCase):
    def fixtures(self):
        p=dict(source='a'*40,apk_sha256='b'*64,bytes=123)
        r=dict(source=p['source'],package='com.universalrp.cleansweep',version='2.97',code=102,
               sha256=p['apk_sha256'],bytes=p['bytes'],signer=SIGNER,final_installed_apk_sha256=p['apk_sha256'],
               required_android35_native_tests=14,added_permissions=[],removed_permissions=[],
               baseline_tag=OLD_TAG,baseline_sha256=OLD_SHA,physical_phone_tested=False,play_protect_tested=False)
        for gate in ('same_uid','data_preserved','preferences_preserved','restart_passed','native_283_settings_smoke',
                     'native_288_feature_notice_smoke','native_289_named_security_smoke','native_292_top_read_and_opt_in_smoke',
                     'native_295_compact_pill_and_signal_recency','native_tests_assert_installed_sha256','native_296_bilingual_rain_details','native_297_home_management'):
            r[gate]=True
        return r,p
    def test_complete_new_receipt(self):
        r,p=self.fixtures();validate_receipt(r,p)
    def test_old_or_incomplete_receipt_is_rejected(self):
        for key,value in [('version','2.96'),('code',101),('required_android35_native_tests',9),
                          ('native_297_home_management',False),('native_296_bilingual_rain_details',False),('native_295_compact_pill_and_signal_recency',False),
                          ('same_uid',False),('data_preserved',False),('preferences_preserved',False),
                          ('restart_passed',False),('final_installed_apk_sha256','c'*64),('added_permissions',['extra'])]:
            r,p=self.fixtures();r=copy.deepcopy(r);r[key]=value
            with self.subTest(key=key),self.assertRaises(AssertionError):validate_receipt(r,p)
    def test_current_source_identity_and_required_count_guard_runs(self):
        import verify_cleaner_next_update_source as source_guard
        source_guard.main()
