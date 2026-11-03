import unittest
from pathlib import Path
SRC=Path(__file__).resolve().parents[2]/'cleaner/src/main/java/com/universalrp/cleansweep'
class SignalFollowupContracts(unittest.TestCase):
    def test_top_read_and_untruncated_headline(self):
        s=(SRC/'ui/HomeScreen.kt').read_text().split('    Surface(modifier = modifier.clickable',1)[1].split('/**',1)[0]
        self.assertIn('watchWords("Read",',s)
        self.assertLess(s.index('TextButton(onClick = onReadTopNews'),s.index('Text(headline?.title'))
        self.assertNotIn('maxLines = 3',s)
        self.assertNotIn('Read news',s)
    def test_sim_icon_removed_and_percentage_is_relative(self):
        s=(SRC/'ui/HomeScreen.kt').read_text().split('private fun HomeMobileSignal',1)[1].split('@Composable',1)[0]
        self.assertNotIn('SignalCellularAlt',s)
        self.assertIn('SimSignalPolicy.percent',s)
        self.assertIn('SimSignalPolicy.tone',s)
        self.assertNotIn('89%',s)
    def test_tone_does_not_change_volume_or_voice(self):
        s=(SRC/'notify/NoSignalBeep.kt').read_text()
        for banned in ('setStreamVolume','adjustStreamVolume','setRingerMode','requestAudioFocus','Announcer.setEnabled'):
            self.assertNotIn(banned,s)
        for gate in ('CallGuard.inCall','Announcer.inQuietHours','Announcer.inSilentPlace','INTERRUPTION_FILTER_ALL','AIRPLANE_MODE_ON','SIM_STATE_READY','STATE_OUT_OF_SERVICE'):
            self.assertIn(gate,s)
        self.assertIn('TONE_PROP_BEEP, 120',s)
        self.assertIn('STREAM_MUSIC, 10',s)
        self.assertNotIn('isValidSubscriptionId(',s)
        self.assertIn('id !in 0 until Int.MAX_VALUE',s)
        self.assertNotIn('isValidSubscriptionId(',(SRC/'data/MobileNet.kt').read_text())
    def test_widget_source_brand_removed_but_report_provenance_kept(self):
        s=(SRC/'widget/CompactWidgetPolicy.kt').read_text()
        self.assertNotIn('"Tomorrow.io"',s)
        self.assertIn('label("Model outlook"',s)
        c=(SRC/'widget/DisasterWidgetCache.kt').read_text()
        for value in ('NewsDisasterUpdates.enabled','savedPlace','not a local impact prediction','now - at','GDACS/JTWC'):
            self.assertIn(value,c)
        self.assertNotIn('httpGet',c)
