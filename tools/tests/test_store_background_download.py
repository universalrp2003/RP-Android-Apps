"""Contracts for transport ownership, not evidence of Android execution."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT/'tamilanstore/src/main/java/com/universalrp/tamilanstore'
class StoreBackgroundDownloadContracts(unittest.TestCase):
    def test_transport_survives_ui_hiding_without_background_installation(self):
        model=(SRC/'StoreModel.kt').read_text()
        hidden=model.split('fun hidden() {',1)[1].split('fun refreshCatalog()',1)[0]
        self.assertNotIn('pauseBatch()',hidden)
        self.assertNotIn('job?.cancel()',hidden)
        self.assertIn('managed.restore()',model)
        self.assertNotIn('init { directory.listFiles()',model)
        self.assertNotIn('withTimeout(180_000)',model)
        self.assertIn('previous?.cancelAndJoin()',model)
        ui=(SRC/'MainActivity.kt').read_text()
        self.assertIn('if (!foreground || model.batch.stage != BatchStage.READY)',ui)
        self.assertIn('model.verifiedFile(app)',ui)
    def test_persistent_user_selected_system_request_has_no_installer_or_credentials(self):
        source=(SRC/'ManagedApkDownload.kt').read_text()
        for token in ('DownloadManager.Request','setDestinationInExternalFilesDir','putLong("id", id).commit()',
                      'Catalog.parse(snapshot)','application/octet-stream','manager.remove(saved.id)'):
            self.assertIn(token,source)
        for token in ('ACTION_INSTALL_PACKAGE','REQUEST_IGNORE_BATTERY_OPTIMIZATIONS','addRequestHeader','startForeground'):
            self.assertNotIn(token,source)
        manifest=(ROOT/'tamilanstore/src/main/AndroidManifest.xml').read_text()
        self.assertNotIn('WAKE_LOCK',manifest)
        self.assertNotIn('FOREGROUND_SERVICE',manifest)
