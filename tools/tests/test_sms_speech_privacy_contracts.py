"""Integration wiring; actual sanitizer and transport behavior have JVM regression tests."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'smsguard/src/main/java/com/universalrp/smsguard'

class SmsSpeechPrivacyContracts(unittest.TestCase):
    def test_consent_is_separate_default_off_and_revocable(self):
        store = (BASE / 'data/GuardSettingsStore.kt').read_text()
        self.assertIn('cloudSpeechConsent: Boolean = false', store)
        self.assertIn('getBoolean("cloud_speech_consent_v1", false)', store)
        self.assertIn('getBoolean("cloud_speech_choice_v1", false)', store)
        self.assertIn('smsguard_neural_tts_v2").deleteRecursively()', store)
        activity = (BASE / 'MainActivity.kt').read_text()
        self.assertIn('!prefs.cloudSpeechChoiceMade || showCloudSpeechDisclosure', activity)
        self.assertIn('CloudSpeechDisclosure', activity)
        controls = (BASE / 'ui/ControlsTab.kt').read_text()
        self.assertIn('checked = prefs.cloudSpeechConsent', controls)
        self.assertIn('vm.setCloudSpeechConsent(false)', controls)
        vm = (BASE / 'ui/MainViewModel.kt').read_text()
        revoke = vm.split('fun setCloudSpeechConsent(', 1)[1].split('fun setVoiceEnabled', 1)[0]
        self.assertIn('SmsVoiceAnnouncer.stop', revoke)
        internet = vm.split('fun setInternetEnabled(', 1)[1].split('fun setCloudSpeechConsent', 1)[0]
        self.assertIn('if (!enabled) SmsVoiceAnnouncer.stop', internet)

    def test_manual_and_transport_final_boundaries_fail_closed(self):
        vm = (BASE / 'ui/MainViewModel.kt').read_text()
        self.assertIn('SpeechPrivacy.prepare(msg.body)?.take(180)', vm)
        repo = (BASE / 'data/SmsRepository.kt').read_text()
        self.assertIn('val otp = OtpDetector.analyze(row.body)', repo)
        self.assertNotIn('if (isSent) OtpResult(false, null)', repo)
        planner = (BASE / 'engine/SmsAnnouncementPlanner.kt').read_text()
        self.assertIn('val safeBody = SpeechPrivacy.prepare(body) ?: return null', planner)
        announcer = (BASE / 'service/SmsVoiceAnnouncer.kt').read_text()
        self.assertIn('SpeechPrivacy.prepare(text) ?: return', announcer)
        self.assertIn('current.internetEnabled, current.cloudSpeechConsent', announcer)
        self.assertIn('(networkAllowed || !v.isNetworkConnectionRequired)', announcer)
        self.assertIn('if (chosen == null)', announcer)
        transport = (BASE / 'service/NeuralCloudVoiceEngine.kt').read_text()
        self.assertIn('if (networkAllowed()) webSocket.send(ssmlMsg)', transport)
        self.assertIn('if (!networkAllowed()) null else streamWebSocketOnce', transport)
        self.assertNotIn('networkAllowed: () -> Boolean = { true }', transport)
        self.assertIn('"v2|', transport)

    def test_permissions_are_requested_at_point_of_use(self):
        activity = (BASE / 'MainActivity.kt').read_text()
        setup = activity.split('fun requestCompanionSmsPermissions()', 1)[1].split('fun openAppInfoForRestrictedSettings()', 1)[0]
        for name in ('SEND_SMS', 'READ_CONTACTS', 'READ_PHONE_STATE'):
            self.assertNotIn('Manifest.permission.' + name, setup)
        self.assertIn('SmsReadDisclosure', setup)
        self.assertIn('onRequestContacts =', activity)
        self.assertIn('onRequestSimInfo =', activity)
        vm = (BASE / 'ui/MainViewModel.kt').read_text()
        self.assertIn('if (!SmsRepository.hasSendSmsPermission(ctx))', vm)
        self.assertIn('onDone(false) // Keep the draft.', vm)
        self.assertIn('Draft kept. Tap Send again', activity)

    def test_public_warning_and_audit_access_are_honest(self):
        note = (ROOT / 'docs/sms-guard-play-protect.md').read_text()
        for text in ('App blocked to protect your device', 'READ_SMS', 'RECEIVE_SMS', 'not a universal secret detector', 'not** an independent audit', 'Keep Play Protect enabled'):
            self.assertIn(text, note)
        self.assertNotIn('are **never** uploaded', (ROOT / 'PRIVACY.md').read_text().split('### SMS Guard', 1)[1].split('## Release checks', 1)[0])
        ui = (BASE / 'ui/SmsPrivacyDialogs.kt').read_text()
        for text in ('speech.platform.bing.com', 'sender names', 'transaction amounts', 'on-device voice only', 'Play Protect', 'மறுத்தால்'):
            self.assertIn(text, ui)
