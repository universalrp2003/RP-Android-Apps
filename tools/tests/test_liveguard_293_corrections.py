import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'cleaner/src/main/java/com/universalrp/cleansweep'
class LiveGuard293Corrections(unittest.TestCase):
    def test_dbm_with_measurement_kind_and_visible_estimate(self):
        s=(SRC/'ui/HomeScreen.kt').read_text()
        self.assertIn('percent(mobile?.signalDbm, mobile?.signalKind)',s)
        self.assertIn('if (percent != null) Text("≈$percent%"',s)
        self.assertIn('SubscriptionSignalReader.read(context, tm, sims.size, reportedCells)',(SRC/'data/MobileNet.kt').read_text())
        self.assertIn('SimSignalPolicy.percent(it.first, it.second)',(SRC/'data/SubscriptionSignalReader.kt').read_text())
    def test_liveguard_only_about_filter_is_used(self):
        s=(SRC/'ui/AboutScreen.kt').read_text()
        self.assertIn('LiveGuardAboutPolicy.projectLinks',s)
        self.assertIn('LiveGuardAboutPolicy.releaseLinks',s)
        self.assertIn('Legacy Mirror Repository',(ROOT/'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text())
    def test_manual_refresh_is_workmanaged_not_receiver_fetch(self):
        s=(SRC/'widget/CleanSweepWidget.kt').read_text()
        self.assertIn('WeatherWatchWorker.runWidgetCheck(app)',s)
        self.assertNotIn('WeatherWatchWorker.executeCheck',s)
        self.assertNotIn('disaster != null && height >= 180',s)
        s=(SRC/'work/WeatherWatchWorker.kt').read_text().split('val forced =',1)[0]
        self.assertIn('withTimeout(55_000)',s)
        self.assertIn('widgetOnly = true',s)
        self.assertIn('speakIfTriggered = false',s)
        self.assertNotIn('Result.retry()',s)
    def test_manual_reports_bypass_delivery_not_consent_and_have_status(self):
        s=(SRC/'data/NewsDisasterUpdates.kt').read_text()
        self.assertIn('widgetOnly || type == "TC"',s)
        self.assertIn('if (!widgetOnly) runCatching',s)
        self.assertIn('if (widgetOnly) WeatherAlertStore.savedPlace(c)',s)
        s=(SRC/'widget/DisasterWidgetCache.kt').read_text()
        for text in ('not an all-clear','not checked','need refresh','unavailable','cached report'):
            self.assertIn(text,s)
    def test_widget_does_not_guarantee_monitoring(self):
        s=(SRC/'widget/CompactWidgetPolicy.kt').read_text()
        for claim in ('24×7','24/7','1-min radar','Staying quiet · Storm watch'):
            self.assertNotIn(claim,s)
        self.assertIn('Forecast snapshot · not live monitoring',s)
