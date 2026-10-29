import json
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import verify_store_handoff_policy as policy


class WebsiteUpdatePolicyTest(unittest.TestCase):
    def test_actual_suite_sources(self):
        policy.verify_sources()

    def test_every_catalog_app_has_a_real_website_anchor(self):
        router = (policy.ROOT / 'shared/src/main/java/com/universalrp/updates/StoreUpdateRouter.kt').read_text()
        aliases = dict(re.findall(r'"([a-z]+)" to "([a-z]+)"', router))
        catalog = json.loads((policy.ROOT / 'site/catalog-v2.json').read_text())
        site = (policy.ROOT / 'site/index.html').read_text()
        for app in catalog['apps']:
            anchor = aliases.get(app['id'], app['id'])
            self.assertIn(f'id="st-{anchor}"', site, app['id'])

    def test_update_notice_is_once_daily_foreground_only_and_never_downloads(self):
        notice = (policy.ROOT / 'shared/src/main/java/com/universalrp/updates/DailyWebsiteUpdateNotice.kt').read_text()
        self.assertIn('UpdateReminderPolicy.shouldCheck(enabled = true', notice)
        self.assertIn('Lifecycle.Event.ON_RESUME', notice)
        self.assertIn('catalog-v2.json', notice)
        self.assertIn('StoreUpdateRouter.openWebsite(context, appId)', notice)
        for forbidden in ('WorkManager', 'FileProvider', 'ACTION_INSTALL_PACKAGE', 'launchInstaller'):
            self.assertNotIn(forbidden, notice)

    def test_store_is_not_a_non_store_update_route(self):
        router = (policy.ROOT / 'shared/src/main/java/com/universalrp/updates/StoreUpdateRouter.kt').read_text()
        self.assertIn('fun open(context: Context, appId: String): Boolean = openWebsite(context, appId)', router)
        self.assertNotIn('com.universalrp.tamilanstore', router)
        self.assertNotIn('ComponentName', router)
        sister_apps = (policy.ROOT / 'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        self.assertIn('https://universalrp2003.github.io/RP-Android-Apps/', sister_apps)
        self.assertIn('SisterApps.OFFICIAL_WEBSITE_URL', router)

    def test_cleaner_apk_verifier_tracks_the_current_update_notice(self):
        workflow = (policy.ROOT / '.github/workflows/build-apk.yml').read_text()
        self.assertIn('DailyWebsiteUpdateNotice', workflow)
        self.assertIn('UpdateReminderPolicy', workflow)
        self.assertNotIn('StoreFirstUpdateNotice', workflow)

    def test_pulseeq_allows_metadata_network_but_not_install_capability(self):
        workflow = (policy.ROOT / '.github/workflows/build-apk.yml').read_text()
        section = workflow.split('=== PulseEQ checks ===', 1)[1].split('=== Ramesh Radio checks ===', 1)[0]
        self.assertIn('android\\.permission\\.(REQUEST_INSTALL_PACKAGES|INSTALL_PACKAGES)', section)
        self.assertIn('read-only daily update check', section)
        self.assertNotIn('android\\.permission\\.(INTERNET|', section)

    def test_no_app_installer_permissions_or_update_providers(self):
        for module in policy.MODULES:
            policy.verify_manifest((policy.ROOT / module / 'src/main/AndroidManifest.xml').read_text())
        broken = '<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/><application/></manifest>'
        with self.assertRaises(ValueError):
            policy.verify_manifest(broken)
        policy.verify_manifest('<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="android.permission.INTERNET"/><application/></manifest>')

    def test_tamilan_store_installer_is_outside_this_migration(self):
        ns = '{http://schemas.android.com/apk/res/android}'
        root = ET.parse(policy.ROOT / 'tamilanstore/src/main/AndroidManifest.xml').getroot()
        perms = {node.get(ns + 'name') for node in root.findall('uses-permission')}
        self.assertIn('android.permission.REQUEST_INSTALL_PACKAGES', perms)
        store = (policy.ROOT / 'tamilanstore/src/main/java/com/universalrp/tamilanstore/StoreModel.kt').read_text()
        for gate in ['app.sha256', 'Catalog.signerFor', 'newCode >= oldCode', 'info.packageName == app.packageName']:
            self.assertIn(gate, store)

    def test_shared_update_sources_never_install(self):
        for relative in [
            'shared/src/main/java/com/universalrp/updates/AppUpdates.kt',
            'shared/src/main/java/com/universalrp/updates/DailyWebsiteUpdateNotice.kt',
            'shared/src/main/java/com/universalrp/updates/StoreUpdateRouter.kt',
            'shared/src/main/java/com/universalrp/updates/BrowserReleaseUpdates.kt',
        ]:
            source = (policy.ROOT / relative).read_text()
            for forbidden in ('ACTION_INSTALL_PACKAGE', 'FileProvider', 'launchInstaller', 'canRequestPackageInstalls'):
                self.assertNotIn(forbidden, source)

    def test_secret_message_manual_browser_update_link_maps_to_its_app(self):
        updates = (policy.ROOT / 'shared/src/main/java/com/universalrp/updates/AppUpdates.kt').read_text()
        self.assertIn('"Secret-Message" to "secretmessage"', updates)
        browser_link = (policy.ROOT / 'shared/src/main/java/com/universalrp/updates/BrowserReleaseUpdates.kt').read_text()
        self.assertIn('StoreUpdateRouter.openWebsite(context, appId)', browser_link)

    def test_secret_wifi_published_client_gets_the_shared_notice_source(self):
        build = (policy.ROOT / 'secretmessage/wifi-test/app/build.gradle.kts').read_text()
        self.assertIn('DailyWebsiteUpdateNotice.kt', build)
        self.assertIn('BrowserReleaseUpdates.kt', build)
        self.assertNotIn('StoreFirstUpdateNotice.kt', build)
        app = (policy.ROOT / 'secretmessage/wifi-test/app/src/main/java/com/rpideas/secretmessage/wifitest/WifiTestApp.kt').read_text()
        self.assertIn('AppUpdates.Control("Secret-Message", automatic = true)', app)
        self.assertNotIn('LaunchUpdateCheck.pending', app)


if __name__ == '__main__':
    unittest.main()
