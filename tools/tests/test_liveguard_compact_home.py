import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'cleaner/src/main/java/com/universalrp/cleansweep'


class CompactHomeSourceContracts(unittest.TestCase):
    def test_split_reads_reset_before_returning_stored_bytes(self):
        source = (SRC / 'data/NetworkSplit.kt').read_text()
        for name in ('fourGBytes', 'fiveGBytes', 'lastCumulative'):
            body = source.split('fun ' + name + '(', 1)[1].split('\n    }', 1)[0]
            self.assertLess(body.index('resetDayIfNeeded(context)'), body.index('return prefs(context)'))

    def test_empty_sampler_paths_refresh_the_card(self):
        source = (SRC / 'notify/RadioWatchService.kt').read_text().split('private fun sample()', 1)[1]
        for branch in ('if (cumulative <= 0)', 'if (last < 0)'):
            self.assertIn('refreshNotification()', source.split(branch, 1)[1].split('return', 1)[0])

    def test_notification_ledger_never_touches_spoken_delivery(self):
        source = (SRC / 'notify/DisasterUpdateNotifier.kt').read_text()
        self.assertIn('"disaster_notifications_291"', source)
        self.assertIn('getBoolean("enabled", false)', source)
        self.assertIn('setSilent(true)', source)
        for forbidden in ('Announcer.', 'NewsDisasterUpdates.reserve', 'NewsDisasterUpdates.finish', 'WorkManager', 'OkHttpClient'):
            self.assertNotIn(forbidden, source)

    def test_notifier_only_consumes_validated_existing_check(self):
        source = (SRC / 'data/NewsDisasterUpdates.kt').read_text()
        end = source[source.index('val enriched ='):]
        self.assertLess(end.index('fingerprint != fingerprint(c)'), end.index('DisasterUpdateNotifier.postVerified'))
        self.assertLess(end.index('it.positionAt != null'), end.index('DisasterUpdateNotifier.postVerified'))

    def test_home_has_folded_copy_real_signal_and_honest_cache(self):
        source = (SRC / 'ui/HomeScreen.kt').read_text()
        card = source.split('private fun HomeMobileDataCard', 1)[1]
        self.assertLess(card.index('if (detailsOpen)'), card.index('Android usage is an estimate'))
        self.assertIn('watchWords("Read"', source)
        self.assertIn('mobile?.signalDbm', source)
        self.assertIn('watchWords("Latency"', source)
        self.assertIn('HomeCompactPolicy.observedCacheBytes(state.usageAccess', source)
        self.assertIn('if (missing.isEmpty()) return', source)

    def test_new_feature_is_ask_first_not_default_on(self):
        source = (SRC / 'data/FeatureDiscovery.kt').read_text()
        self.assertIn('DISASTER_NOTIFICATIONS("disaster_silent_notifications", Mode.ASK_FIRST)', source)
        ui = (SRC / 'ui/FeatureUpdatesNotice.kt').read_text()
        self.assertIn('FeatureDiscovery.Feature.DISASTER_NOTIFICATIONS ->', ui)
        self.assertIn('No extra speech, sound, vibration or separate background polling', ui)
