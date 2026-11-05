from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'oorarivippu/src/main/java/com/oorarivippu/voiceannouncer'
class FamilyPrivacyContracts(unittest.TestCase):
    def test_independent_separate_backend(self):
        config=(ROOT/'oorarivippu/family-relay/wrangler.toml').read_text()
        self.assertIn('name = "oor-family-relay"',config)
        self.assertIn('enabled = false',config)
        self.assertNotIn('secretmessage-relay',config)
    def test_service_configuration_fails_closed(self):
        s=(ROOT/'oorarivippu/build.gradle.kts').read_text()
        self.assertIn('orElse("")',s)
        s=(SRC/'family/FamilyRepository.kt').read_text()
        self.assertIn('check(configured)',s)
        self.assertIn('followRedirects(false)',s)
        self.assertNotIn('Log.',s)
    def test_only_geocoder_town_and_district_in_publish_call(self):
        s=(SRC/'LocationAnnouncementService.kt').read_text()
        self.assertIn('FamilyRepository.publish(this@LocationAnnouncementService, cityOrVillage, resolved.district, location)',s)
        s=(SRC/'family/FamilyProtocol.kt').read_text()
        self.assertIn('put("town",clean(town.town))',s)
        for field in ('latitude','longitude','subArea','speed','address','postcode'):
            self.assertNotIn('put("'+field+'"',s)
    def test_standard_authenticated_encryption_and_at_rest_keys(self):
        s=(SRC/'family/FamilyProtocol.kt').read_text()
        self.assertIn('AES/GCM/NoPadding',s)
        self.assertIn('cipher.updateAAD(aad(id,role',s)
        s=(SRC/'family/FamilyStore.kt').read_text()
        self.assertIn('AndroidKeyStore',s)
        self.assertIn('.commit()',s)
    def test_sharing_opt_in_durable_off_and_trip_stop(self):
        s=(SRC/'family/FamilyRepository.kt').read_text()
        self.assertGreaterEqual(s.count('.put("until",0)'),2)
        self.assertLess(s.index('update(app,id){it.put("until",0)'),s.index('scope.launch { runCatching{lock.withLock { flush(app);cached(app) }} }'))
        self.assertIn('AppPreferences.state.value.isTrackingEnabled',s)
        self.assertIn('FamilyRepository.pauseTrip(this)',(SRC/'LocationAnnouncementService.kt').read_text())
    def test_ui_has_no_automatic_enable_and_explicit_offline_warning(self):
        s=(SRC/'family/FamilyPanel.kt').read_text()
        for text in ('sharing stays OFF','Stop all my family sharing','15-minute expiry','not a live location','No recent town update','FamilyRepository.confirm','FamilyRepository.setSharing'):
            self.assertIn(text,s)
        self.assertNotIn('rememberSaveable',s)
        self.assertIn('Lifecycle.State.RESUMED',s)
    def test_persistent_switch_and_local_voice_privacy(self):
        repo=(SRC/'family/FamilyRepository.kt').read_text()
        self.assertIn('A former timed grant is NOT permission',repo)
        self.assertIn('announcedAt',repo)
        self.assertIn('val state:',repo)
        self.assertNotIn('suspend fun start(',repo)
        panel=(SRC/'family/FamilyPanel.kt').read_text()
        self.assertIn('rows.filter{it.confirmed}',panel)
        self.assertIn('Switch(checked=row.sharing',panel)
        self.assertNotIn('listOf(1,4,8)',panel)
        service=(SRC/'LocationAnnouncementService.kt').read_text()
        self.assertIn('configureTtsLanguage(language,true)',service)
        self.assertIn('logTitle=null,isFamilyUpdate=true',service)
        self.assertIn('familyAudioAllowed()',service)
        self.assertIn('FamilyRepository.homeVisible=false',(SRC/'MainActivity.kt').read_text())
    def test_default_runtime_does_not_depend_on_map_or_contacts(self):
        s=(ROOT/'oorarivippu/src/main/AndroidManifest.xml').read_text()
        for banned in ('READ_CONTACTS','SEND_SMS','RECEIVE_BOOT_COMPLETED'):
            self.assertNotIn(banned,s)
        self.assertNotIn('maps:',(ROOT/'oorarivippu/build.gradle.kts').read_text())
