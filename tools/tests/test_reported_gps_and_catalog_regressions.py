"""Source regression guards; not a substitute for Kotlin/native/network/device tests."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class ReportedPhoneRegressions(unittest.TestCase):
    def test_receiver_stop_retains_report_but_lifecycle_still_clears_it(self):
        s=(ROOT/'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/LocationDiagnostics.kt').read_text()
        stop=s.split('override fun onStopped()',1)[1].split('override fun onSatelliteStatusChanged',1)[0]
        self.assertNotIn('snapshot = null',stop)
        self.assertIn('val current = snapshot\n',s)
        self.assertIn('seconds old · not live',s)
        self.assertIn('snapshot = null',s.split('fun unregister()',1)[1].split('fun registerIfVisible',1)[0])
        probe=(ROOT/'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer/GpsDiagnosticProbe.kt').read_text()
        self.assertIn('DURATION_MS = 30_000L',probe)
        self.assertIn('setDurationMillis(DURATION_MS)',probe)
    def test_refresh_bypasses_stale_http_cache_without_relaxing_validation(self):
        base=ROOT/'tamilanstore/src/main/java/com/universalrp/tamilanstore'
        s=(base/'LiveCatalog.kt').read_text()
        for text in ('?refresh=', 'connection.useCaches = false', 'no-cache, no-store, max-age=0',
                     'connection.instanceFollowRedirects = false', 'readBytesLimited(256 * 1024)',
                     'app.sha256 == previous.sha256', 'app.packageName == previous.packageName'):
            self.assertIn(text,s)
        ui=(base/'MainActivity.kt').read_text();model=(base/'StoreModel.kt').read_text()
        self.assertIn('Refresh catalog',ui)
        self.assertIn('Online catalog rejected by validation',ui)
        self.assertIn('model.refreshCatalog()',ui)
        self.assertIn('catalogMessage = "rejected"',model)
        self.assertIn('LiveCatalog.validate(text, apps)',model)
    def test_system_download_uses_provider_descriptor_and_keeps_integrity_checks(self):
        base=ROOT/'tamilanstore/src/main/java/com/universalrp/tamilanstore'
        self.assertIn('manager.openDownloadedFile(saved.id)',(base/'ManagedApkDownload.kt').read_text())
        model=(base/'StoreModel.kt').read_text()
        self.assertIn('managed.openCompleted(saved).use',model)
        self.assertIn('verifyGate(total == app.size, TransferFailure.SIZE)',model)
        self.assertIn('verify(app, part)',model)
        self.assertIn('hex(digest.digest()) == app.sha256',model)
    def test_published_store_pins_only_the_final_verified_source(self):
        import json
        from unittest.mock import patch
        import promote_verified_store_011 as publisher
        p=json.loads((ROOT/'docs/approved-store-0.11-candidate.json').read_text())
        r=json.loads((ROOT/'docs/tamilan-store-0.11-publication-receipt.json').read_text())
        self.assertEqual(p['source'],'29083446beb03d6d428c3f681fa3283f75a9394d')
        self.assertEqual(p['background_run'],42182208056)
        self.assertEqual((p['apk_sha256'],p['bytes']),(r['sha256'],r['bytes']))
        self.assertFalse(r['owner_reports_background_download_working'])
        self.assertTrue(r['prior_public_assets_unchanged'])
        # A successful but older candidate cannot be substituted for the tested final source.
        stale=dict(head_sha='09250a8aafdec9d5f4bea6bf47044283bc8fce14',head_branch=publisher.BRANCH,
                   status='completed',conclusion='success',path='.github/workflows/tamilanstore-signed-candidate.yml')
        with patch.object(publisher,'api',return_value=stale),self.assertRaises(AssertionError):
            publisher.verify_candidate_gates(p)
