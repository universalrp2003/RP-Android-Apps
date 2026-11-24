from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]


class FreshInstallMigrationTest(unittest.TestCase):
    def test_site_warns_before_uninstall_in_english_and_tamil(self):
        site = (ROOT / 'site/index.html').read_text()
        self.assertNotIn('class="migration-notice"', site)
        self.assertIn('id="signing-limitation"', site)
        self.assertIn('href="#signing-limitation"', site)
        self.assertIn('Uninstalling may erase local data.', site)
        self.assertIn('Play Protect எச்சரித்தால் நிறுத்துங்கள்', site)
        self.assertIn('docs/fresh-install-signing-migration.md', site)

    def test_website_links_and_mirror_inventory_match_verified_releases(self):
        site = (ROOT / 'site/index.html').read_text()
        pages = (ROOT / '.github/workflows/pages.yml').read_text()
        releases = (
            ('sms-guard-v1.1.18', 'SMS-Guard-v1.1.18.apk', '1.1.18', 12873680, '22c3f7ed4dee0533679022a9099cfe0a55da2a1bc60099ebd3bd5a28f6997222'),
            ('vasanthi-ai-v1.1.6', 'Vasanthi-AI-v1.1.6.apk', '1.1.6', 45794346, '5b59ba8dbe7cf2624ac3e50000547350dd6b6af846f38a39010e2ba5feadc829'),
            ('oor-arivippu-v1.1.26', 'Oor-Arivippu-v1.1.26.apk', '1.1.26', 12941104, '96ff45915749f5f1a851ff246ee9ddcd0b83886b3d1017e6ab5f22661d17d22c'),
            ('tamil-podcast-v1.6', 'Tamil-Podcast-Player-v1.6.apk', '1.6', 11967239, 'f654c5966d59daf5ff1489e503f97db53e20e202c93eacee91cb5b12597e04ec'),
            ('daily-vault-v0.5', 'DailyVault-v0.5.apk', '0.5', 11712552, '8842606659442a1e2b8c7d28fd2affac56bb40c0bc679acf86785f3167903d55'),
            ('tamilan-store-v0.17', 'Tamilan-Store-v0.17.apk', '0.17', 20713347, 'c38bed526fc53e0276f64c853665b60f29ad9cc687b623b3e1c7bf6013be66c7'),
            ('expense-tracker-v1.2', 'Expense-Tracker-v1.2.apk', '1.2', 11564299, '286ea8027042b014fb2f8a28198ee01b569eb270577993189c2f5d7be3277108'),
            ('elderly-launcher-v1.8', 'Elderly-Launcher-v1.8.apk', '1.8', 11538139, '6f633cd543dee244a0a231efe9a8ac71ec6c891ae258867c6307f0e938f3f46c'),
            ('voice-notes-v1.3.4', 'Voice-Notes-v1.3.4.apk', '1.3.4', 13135195, '1aa2ea8eb1037098881ce4a0a4329b6164f77a77e0ab31112610fa2d03f8094a'),
            ('live-guard-v3.01', 'CleanSweep-v3.01.apk', '3.01', 14781231, 'b5850343f4788d9c499024da8ea53b534a20e317bf57b071d855e565a360174f'),
            ('ramesh-radio-v1.31', 'Ramesh-Radio-v1.31.apk', '1.31', 15179972, 'fceac7c50758990eaa33228a3ec375f38fb9a307bca8452566eb84d037a5dc22'),
            ('pulseeq-v1.11', 'PulseEQ-v1.11.apk', '1.11', 12165793, 'bcfb6a606d831ad935efe024bae4960ed33ececcb67157ae408f57c596c43e4a'),
            ('rp-vocal-remover-studio-v0.1.16', 'RP-Vocal-Remover-Studio-v0.1.16.apk', '0.1.16', 65163821, '6d0b04cd63f18bcb82bd2512d71c0985737268752007d96ccc4327558d38489e'),
        )
        for tag, asset, version, size, digest in releases:
            with self.subTest(asset=asset):
                matching_rows = [m.group(0) for m in re.finditer(r'<tr>.*?</tr>', site[site.index('<section class="latest"'):], re.S) if asset in m.group(0)]
                self.assertEqual(len(matching_rows), 1, asset)
                self.assertIn(f'<td class="lver">v{version}</td>', matching_rows[0])
                self.assertIn(f'data-apk="apks/{asset}"', matching_rows[0])
                self.assertIn(f'data-sha="{digest}"', matching_rows[0])
                self.assertIn(f'releases/download/{tag}/{asset}', matching_rows[0])
                self.assertIn(f'("{tag}", "{asset}", {size}, "{digest}")', pages)
                self.assertIn(f'test -s site/apks/{asset}', pages)
                self.assertIn(f'test -s site/apks/{asset[:-4]}.zip', pages)

    def test_owner_requested_localized_display_names(self):
        def string_value(path, name):
            text = (ROOT / path).read_text()
            match = re.search(r'<string name="' + re.escape(name) + r'">([^<]+)</string>', text)
            self.assertIsNotNone(match, path)
            return match.group(1)

        self.assertEqual(string_value('cleaner/src/main/res/values/strings.xml', 'app_name'), 'Live Guard')
        self.assertEqual(string_value('cleaner/src/main/res/values-ta/strings.xml', 'app_name'), 'கைபேசி காவலன்')
        self.assertEqual(string_value('radio/src/main/res/values/strings.xml', 'app_name'), 'Ramesh Radio')
        self.assertEqual(string_value('radio/src/main/res/values-ta/strings.xml', 'app_name'), 'தமிழன் வானொலி')
        self.assertEqual(string_value('vocalremover/src/main/res/values-ta/strings.xml', 'app_name'), 'இசை மட்டும் வேண்டும்')
        self.assertEqual((ROOT / 'cleaner/fastlane/metadata/android/en-US/title.txt').read_text().strip(), 'Live Guard')
        self.assertEqual((ROOT / 'cleaner/fastlane/metadata/android/ta-IN/title.txt').read_text().strip(), 'கைபேசி காவலன்')
        self.assertEqual((ROOT / 'radio/fastlane/metadata/android/ta-IN/title.txt').read_text().strip(), 'தமிழன் வானொலி')
        sister_apps = (ROOT / 'shared/src/main/java/com/universalrp/updates/SisterApps.kt').read_text()
        self.assertIn('tamilName = "தமிழன் வானொலி"', sister_apps)
        self.assertIn('tamilName = "இசை மட்டும் வேண்டும்"', sister_apps)
        catalog = (ROOT / 'tamilanstore/src/main/assets/catalog.json').read_text()
        self.assertIn('"tamilName": "தமிழன் வானொலி"', catalog)
        self.assertIn('"tamilName": "இசை மட்டும் வேண்டும்"', catalog)
        site = (ROOT / 'site/index.html').read_text()
        self.assertIn('(கைபேசி காவலன்)', site)
        self.assertIn('(தமிழன் வானொலி)', site)
        self.assertIn('(இசை மட்டும் வேண்டும்)', site)

    def test_replacement_releases_have_unique_version_codes_and_migration_notes(self):
        modules = {
            # Current source candidate; 3.00 is still the latest published migration note.
            'cleaner': ('3.01', '106', 'live-guard-v3.00'),
            'radio': ('1.31', '31', 'ramesh-radio-v1.31'),
            'equalizer': ('1.11', '12', 'pulseeq-v1.11'),
            'vocalremover': ('0.1.16', '17', 'rp-vocal-remover-studio-v0.1.16'),
            'oorarivippu': ('1.1.26', '31', 'oor-arivippu-v1.1.26'),
            'smsguard': ('1.1.18', '24', 'sms-guard-v1.1.18'),
            'voicenotes': ('1.3.4', '8', 'voice-notes-v1.3.4'),
            'expensetracker': ('1.2', '3', 'expense-tracker-v1.2'),
            'vasanthi': ('1.1.6', '9', 'vasanthi-ai-v1.1.6'),
            'podcast': ('1.6', '8', 'tamil-podcast-v1.6'),
            'launcher': ('1.8', '9', 'elderly-launcher-v1.8'),
            'dailyvault': ('0.5', '5', 'daily-vault-v0.5'),
            # Store0.17 is public (24 Nov 2026): 17-app catalog, same stable key.
            'tamilanstore': ('0.17', '19', 'tamilan-store-v0.17'),
        }
        seen_codes = {}
        for module, (expected_version, expected_code, slug) in modules.items():
            gradle = (ROOT / module / 'build.gradle.kts').read_text()
            version = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
            code = re.search(r'versionCode\s*=\s*(\d+)', gradle)
            self.assertIsNotNone(version, module)
            self.assertIsNotNone(code, module)
            self.assertEqual(version.group(1), expected_version, module)
            self.assertEqual(code.group(1), expected_code, module)
            self.assertNotIn((module, expected_code), seen_codes)
            seen_codes[(module, expected_code)] = slug
            note = (ROOT / 'releases' / f'{slug}.md').read_text()
            self.assertIn('## தமிழ்', note, slug)
            self.assertRegex(note.lower(), r'uninstall|fresh.install|கையொப்ப')
            self.assertRegex(note.lower(), r'local data|uninstalling may erase|தரவு அழிய|தரவு.*அழிய')

    def test_verified_replacement_release_digests_are_recorded(self):
        evidence = (ROOT / 'docs/published-release-digests.md').read_text()
        for release in (
            ('SMS-Guard-v1.1.16.apk', 'db279d30f2ff32606cf808172c91686b3641cdf2f07eb113813a03ddd74ba2df'),
            ('SMS-Guard-v1.1.11.apk', '9fd0aa043eaf261c061b214a27d4427dc73d66eee8a1a23c9dd3697d9f6f0604'),
            ('Vasanthi-AI-v1.1.5.apk', '4b3c98a446f040d5e754eeee22065412ec1e6e756bf427001418e3b100182fa6'),
            ('Vasanthi-AI-v1.1.6.apk', '5b59ba8dbe7cf2624ac3e50000547350dd6b6af846f38a39010e2ba5feadc829'),
            ('Tamilan-Store-v0.16.apk', '7bf87fc4508db6f3e034ddc86fbef2121ce12593259f086efc5e8dadac27c552'),
            ('Tamilan-Store-v0.17.apk', 'c38bed526fc53e0276f64c853665b60f29ad9cc687b623b3e1c7bf6013be66c7'),
            ('Oor-Arivippu-v1.1.10.apk', '7a526fc54ac2ed07a5f972cfa030b9eb71ac77c5d1a16151c50803e9b0d7ceaa'),
            ('Tamil-Podcast-Player-v1.6.apk', 'f654c5966d59daf5ff1489e503f97db53e20e202c93eacee91cb5b12597e04ec'),
            ('DailyVault-v0.5.apk', '8842606659442a1e2b8c7d28fd2affac56bb40c0bc679acf86785f3167903d55'),
            ('Tamilan-Store-v0.7.apk', 'b6618ea5e101c23ce4aada39b28820e9840bd1eede03140ff2582255fe4fd8fe'),
            ('Tamilan-Store-v0.6.apk', '7a87496f70977cbc8b3085f5af442247b52abf59fab2723c0290de9b4659653f'),
            ('Expense-Tracker-v1.2.apk', '286ea8027042b014fb2f8a28198ee01b569eb270577993189c2f5d7be3277108'),
            ('Elderly-Launcher-v1.5.apk', 'c4cb41847576be63e78270672fee80b40b872fa5c9a3984482643233988b5f39'),
            ('Voice-Notes-v1.3.3.apk', '71aba878bc85419933f6d8050626bcf19056f76c4cac7806e8f899efca404d35'),
            ('CleanSweep-v2.75.apk', '2e7ccf2123695e990c74eeda082109bd3d5ce7a916bb3ecf0465f8098137fcbf'),
            ('Ramesh-Radio-v1.29.apk', '11cdb90c4919a13ca23247cf9ba505a13b63b3185f78afa2a5568c79a1af210d'),
            ('PulseEQ-v1.11.apk', 'bcfb6a606d831ad935efe024bae4960ed33ececcb67157ae408f57c596c43e4a'),
            ('RP-Vocal-Remover-Studio-v0.1.16.apk', '6d0b04cd63f18bcb82bd2512d71c0985737268752007d96ccc4327558d38489e'),
        ):
            with self.subTest(asset=release[0]):
                self.assertIn(release[0], evidence)
                self.assertIn(release[1], evidence)

    def test_store_and_vault_workflow_runs_one_candidate_or_release_at_a_time(self):
        workflow = (ROOT / '.github/workflows/new-apps-public-release.yml').read_text()
        self.assertIn('required: true', workflow)
        self.assertIn('default: false', workflow)
        self.assertIn('- tamilanstore', workflow)
        self.assertIn('- dailyvault', workflow)
        self.assertIn("Store 0.8 is immutable", workflow)
        self.assertNotIn("echo 'version=0.8'", workflow)
        self.assertIn("echo 'version=0.5'", workflow)
        self.assertIn('app: ${{ fromJSON(', workflow)
        self.assertIn("format('[\"{0}\"]', inputs.app)", workflow)
        self.assertIn("if: ${{ env.PUBLISH_REQUESTED == 'true' }}", workflow)
        self.assertIn('[publish-tamilanstore]', workflow)
        self.assertIn('[publish-dailyvault]', workflow)
        self.assertIn('published_digest', workflow)
        self.assertIn('RP_RELEASE_KEYSTORE_BASE64', workflow)
        self.assertNotIn('specs=("tamilan-store 0.6', workflow)

    def test_build_apk_workflow_requires_one_app_and_defaults_to_candidate(self):
        workflow = (ROOT / '.github/workflows/build-apk.yml').read_text()
        self.assertIn('Select exactly one app for this candidate or release run', workflow)
        self.assertIn('default: false', workflow)
        self.assertIn('APP_TO_PUBLISH: ${{ inputs.app }}', workflow)
        self.assertIn('Publishing exactly one selected app', workflow)
        self.assertIn("github.event_name == 'push' && (contains", workflow)
        self.assertIn('The commit subject must contain exactly one app-specific publish marker', workflow)
        self.assertIn('cleaner) SELECTED_TAG=', workflow)
        self.assertIn('radio) SELECTED_TAG=', workflow)
        self.assertIn('equalizer) SELECTED_TAG=', workflow)
        self.assertIn('vocalremover) SELECTED_TAG=', workflow)
        self.assertIn('if [[ "$APP_TO_PUBLISH" == "cleaner" ]] && [', workflow)
        for marker in ('[publish-liveguard]', '[publish-radio]', '[publish-pulseeq]', '[publish-vocalremover]'):
            self.assertIn(marker, workflow)

    def test_every_standalone_release_workflow_defaults_to_candidate_only(self):
        names = (
            'expensetracker-release.yml', 'launcher-release.yml',
            'oorarivippu-release.yml', 'podcast-release.yml', 'smsguard-release.yml',
            'vasanthi-release.yml', 'voicenotes-release.yml',
        )
        for name in names:
            workflow = (ROOT / '.github/workflows' / name).read_text()
            self.assertIn('workflow_dispatch:', workflow, name)
            self.assertIn('publish:', workflow, name)
            self.assertIn('default: false', workflow, name)
            self.assertIn("github.event_name == 'push'", workflow, name)
            self.assertIn('PUBLISH_REQUESTED:', workflow, name)
            if name == 'smsguard-release.yml':
                self.assertIn('CANDIDATE VERIFIED ONLY:', workflow)
                self.assertLess(workflow.index('if [[ "$PUBLISH_REQUESTED" != "true" ]]'), workflow.index('gh release create'))
            else:
                self.assertIn("if: ${{ env.PUBLISH_REQUESTED == 'true' }}", workflow, name)


if __name__ == '__main__':
    unittest.main()
