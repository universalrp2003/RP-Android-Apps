from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]


class FreshInstallMigrationTest(unittest.TestCase):
    def test_site_warns_before_uninstall_in_english_and_tamil(self):
        site = (ROOT / 'site/index.html').read_text()
        self.assertIn('class="migration-notice"', site)
        self.assertIn('Do not uninstall your working apps yet.', site)
        self.assertIn('Uninstalling may erase local data.', site)
        self.assertIn('புதிய பதிப்பு இங்கே வெளியான பிறகு', site)
        self.assertIn('Play Protect எச்சரித்தால் நிறுத்துங்கள்', site)
        self.assertIn('docs/fresh-install-signing-migration.md', site)

    def test_replacement_releases_have_unique_version_codes_and_migration_notes(self):
        modules = {
            'cleaner': ('2.74', '79', 'live-guard-v2.74'),
            'radio': ('1.29', '29', 'ramesh-radio-v1.29'),
            'equalizer': ('1.11', '12', 'pulseeq-v1.11'),
            'vocalremover': ('0.1.16', '17', 'rp-vocal-remover-studio-v0.1.16'),
            'oorarivippu': ('1.1.10', '15', 'oor-arivippu-v1.1.10'),
            'smsguard': ('1.1.11', '17', 'sms-guard-v1.1.11'),
            'voicenotes': ('1.3.3', '7', 'voice-notes-v1.3.3'),
            'expensetracker': ('1.2', '3', 'expense-tracker-v1.2'),
            'vasanthi': ('1.1.5', '8', 'vasanthi-ai-v1.1.5'),
            'podcast': ('1.6', '8', 'tamil-podcast-v1.6'),
            'launcher': ('1.5', '6', 'elderly-launcher-v1.5'),
            'dailyvault': ('0.4', '4', 'daily-vault-v0.4'),
            'tamilanstore': ('0.6', '6', 'tamilan-store-v0.6'),
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
            ('SMS-Guard-v1.1.11.apk', '9fd0aa043eaf261c061b214a27d4427dc73d66eee8a1a23c9dd3697d9f6f0604'),
            ('Vasanthi-AI-v1.1.5.apk', '4b3c98a446f040d5e754eeee22065412ec1e6e756bf427001418e3b100182fa6'),
            ('Oor-Arivippu-v1.1.10.apk', '7a526fc54ac2ed07a5f972cfa030b9eb71ac77c5d1a16151c50803e9b0d7ceaa'),
            ('Tamil-Podcast-Player-v1.6.apk', 'f654c5966d59daf5ff1489e503f97db53e20e202c93eacee91cb5b12597e04ec'),
            ('DailyVault-v0.4.apk', '39ff7b7bcabb9f8a53b329d9f244b0d6e754d9a49159f5f3afac00469d5a0dc1'),
            ('Tamilan-Store-v0.6.apk', '7a87496f70977cbc8b3085f5af442247b52abf59fab2723c0290de9b4659653f'),
            ('Expense-Tracker-v1.2.apk', '286ea8027042b014fb2f8a28198ee01b569eb270577993189c2f5d7be3277108'),
            ('Elderly-Launcher-v1.5.apk', 'c4cb41847576be63e78270672fee80b40b872fa5c9a3984482643233988b5f39'),
            ('Voice-Notes-v1.3.3.apk', '71aba878bc85419933f6d8050626bcf19056f76c4cac7806e8f899efca404d35'),
            ('CleanSweep-v2.74.apk', 'c7b45a11e6a41ca5850e1edf3fa6e37388009e563975fcb30b03f50d74f794fa'),
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
        self.assertIn("echo 'version=0.6'", workflow)
        self.assertIn("echo 'version=0.4'", workflow)
        self.assertIn('app: [tamilanstore, dailyvault]', workflow)
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
