"""Guard the radio watch service against the Android 15+ dataSync timeout crash."""
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
A = '{http://schemas.android.com/apk/res/android}'

class RadioTimeoutContract(unittest.TestCase):
    def test_continuous_radio_monitor_is_not_data_sync(self):
        root = ET.parse(ROOT/'cleaner/src/main/AndroidManifest.xml').getroot()
        permissions = {p.get(A+'name') for p in root.findall('uses-permission')}
        self.assertIn('android.permission.FOREGROUND_SERVICE_SPECIAL_USE', permissions)
        service = root.find("application/service[@"+A+"name='.notify.RadioWatchService']")
        self.assertEqual('specialUse', service.get(A+'foregroundServiceType'))
        prop = service.find("property[@"+A+"name='android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE']")
        self.assertIn('radio monitoring', prop.get(A+'value'))
        self.assertEqual('false', service.get(A+'exported'))

    def test_timeout_stops_service_instead_of_killing_process(self):
        source = (ROOT/'cleaner/src/main/java/com/universalrp/cleansweep/notify/RadioWatchService.kt').read_text()
        timeout = source.split('override fun onTimeout(startId: Int, fgsType: Int)',1)[1].split('override fun onDestroy()',1)[0]
        self.assertIn('handler.removeCallbacks(poll)', timeout)
        self.assertIn('stopSelf()', timeout)
        destroy = source.split('override fun onDestroy()',1)[1].split('override fun onBind',1)[0]
        for cleanup in ('handler.removeCallbacks(poll)', 'noSignal.close()', 'unregisterRadioListener()', 'RoutineStatus.release'):
            self.assertIn(cleanup, destroy)
