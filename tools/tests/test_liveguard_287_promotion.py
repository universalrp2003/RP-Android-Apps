import copy
import unittest
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from promote_verified_liveguard_287 import validate_receipt, SIGNER, OLD_TAG, OLD_SHA

class LiveGuard287PromotionTest(unittest.TestCase):
    def setUp(self):
        self.p = dict(source='a'*40, apk_sha256='b'*64, bytes=14_000_000)
        self.r = dict(source='a'*40, package='com.universalrp.cleansweep',version='2.87',code=92,
            sha256='b'*64,bytes=14_000_000,signer=SIGNER,same_uid=True,data_preserved=True,
            preferences_preserved=True,restart_passed=True,native_283_settings_smoke=True,
            added_permissions=[],removed_permissions=[],baseline_tag=OLD_TAG,baseline_sha256=OLD_SHA,
            physical_phone_tested=False,play_protect_tested=False)
    def test_valid_exact_signed_receipt(self): validate_receipt(self.r,self.p)
    def test_wrong_source_package_version_code_digest_size_signer_rejected(self):
        for key in ('source','package','version','code','sha256','bytes','signer'):
            r=copy.deepcopy(self.r);r[key]='wrong'
            with self.subTest(key=key),self.assertRaises(AssertionError):validate_receipt(r,self.p)
    def test_missing_update_or_native_gate_rejected(self):
        for key in ('same_uid','data_preserved','preferences_preserved','restart_passed','native_283_settings_smoke'):
            r=copy.deepcopy(self.r);r[key]=False
            with self.subTest(key=key),self.assertRaises(AssertionError):validate_receipt(r,self.p)
    def test_permission_delta_rejected(self):
        r=copy.deepcopy(self.r);r['added_permissions']=['android.permission.READ_SMS']
        with self.assertRaises(AssertionError):validate_receipt(r,self.p)
    def test_wrong_baseline_rejected(self):
        r=copy.deepcopy(self.r);r['baseline_sha256']='c'*64
        with self.assertRaises(AssertionError):validate_receipt(r,self.p)
    def test_phone_or_clearance_inheritance_rejected(self):
        for key in ('physical_phone_tested','play_protect_tested'):
            r=copy.deepcopy(self.r);r[key]=True
            with self.subTest(key=key),self.assertRaises(AssertionError):validate_receipt(r,self.p)
