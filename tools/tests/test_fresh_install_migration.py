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

    def test_store_and_vault_workflow_runs_one_candidate_or_release_at_a_time(self):
        workflow = (ROOT / '.github/workflows/new-apps-public-release.yml').read_text()
        self.assertIn('required: true', workflow)
        self.assertIn('default: false', workflow)
        self.assertIn('- tamilanstore', workflow)
        self.assertIn('- dailyvault', workflow)
        self.assertIn("echo 'version=0.6'", workflow)
        self.assertIn("echo 'version=0.4'", workflow)
        self.assertIn('if: ${{ inputs.publish }}', workflow)
        self.assertIn('published_digest', workflow)
        self.assertIn('RP_RELEASE_KEYSTORE_BASE64', workflow)
        self.assertNotIn('specs=("tamilan-store 0.6', workflow)

    def test_build_apk_workflow_requires_one_app_and_defaults_to_candidate(self):
        workflow = (ROOT / '.github/workflows/build-apk.yml').read_text()
        self.assertIn('Select exactly one app for this candidate or release run', workflow)
        self.assertIn('default: false', workflow)
        self.assertIn('APP_TO_PUBLISH: ${{ inputs.app }}', workflow)
        self.assertIn('Publishing exactly one selected app', workflow)
        self.assertIn('if: ${{ github.event_name == \'workflow_dispatch\' && (inputs.publish || inputs.mirror_existing) }}', workflow)
        self.assertIn('cleaner) SELECTED_TAG=', workflow)
        self.assertIn('radio) SELECTED_TAG=', workflow)
        self.assertIn('equalizer) SELECTED_TAG=', workflow)
        self.assertIn('vocalremover) SELECTED_TAG=', workflow)
        self.assertIn('if [[ "$APP_TO_PUBLISH" == "cleaner" ]] && [', workflow)

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


if __name__ == '__main__':
    unittest.main()
