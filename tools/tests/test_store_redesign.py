from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'tamilanstore/src/main/java/com/universalrp/tamilanstore'

class StoreRedesignContracts(unittest.TestCase):
    def test_foreground_checks_are_metadata_only_bounded_and_not_manual_buttons(self):
        ui = (SRC/'MainActivity.kt').read_text()
        model = (SRC/'StoreModel.kt').read_text()
        self.assertIn('storeModel.refreshOnOpen(storeVersion)', ui)
        self.assertNotIn('"Refresh"', ui)
        self.assertNotIn('"Check store release"', ui)
        self.assertIn('now - lastOpenCheck < 60_000', model)
        self.assertIn('now - lastSelfCheck >= 600_000', model)
        opening = model.split('fun refreshOnOpen', 1)[1].split('fun startBatch', 1)[0]
        self.assertNotIn('downloadInternal', opening)
        self.assertIn('SnackbarDuration.Short', ui)

    def test_verified_queue_cannot_advance_from_activity_result_alone(self):
        model = (SRC/'StoreModel.kt').read_text()
        ui = (SRC/'MainActivity.kt').read_text()
        self.assertIn('BatchInstallEvidence.confirmed(installed(app), app.version, expectedBatchCode)', model)
        self.assertIn('if (updated) startNextBatch()', model)
        self.assertIn('model.verifiedFile(app)', ui)
        self.assertIn('model.markInstallerOpened()', ui)
        self.assertIn('Intent.EXTRA_RETURN_RESULT, true', ui)
        self.assertIn('ActivityResultContracts.StartActivityForResult()', ui)
        self.assertIn('if (!foreground || model.batch.stage != BatchStage.READY)', ui)
        self.assertIn('Source permission', ui)
        self.assertIn('Store self-update is separate', ui)
        self.assertIn('mobile data may be used', ui)
        self.assertNotIn('SharedPreferences', (SRC/'UpdateBatchState.kt').read_text())

    def test_artwork_and_condensed_but_expandable_native_ui(self):
        showcase = (SRC/'StoreShowcase.kt').read_text()
        ui = (SRC/'MainActivity.kt').read_text()
        self.assertIn('R.drawable.tamil_landscape', showcase)
        self.assertIn('contentDescription = null', showcase)
        self.assertIn('if (showScreenshots) AppScreenshots(app.id, tamil)', ui)
        self.assertIn('"திரைப் படங்கள்", "Screenshots"', ui)
        self.assertIn('FlowRow(horizontalArrangement', ui)
        self.assertIn('DropdownMenu(', ui)
        self.assertNotIn('fontScale =', ui)

    def test_stable_version_does_not_reuse_immutable_public_release(self):
        build = (ROOT/'tamilanstore/build.gradle.kts').read_text()
        self.assertIn('versionName = "0.10"', build)
        self.assertIn('versionCode = 12', build)
        site = (ROOT/'site/index.html').read_text()
        receipt = (ROOT/'docs/tamilan-store-0.9-upgrade.md').read_text()
        self.assertIn('0.9-rc1', receipt)  # Retain the earlier candidate's separate provenance.
        workflow = (ROOT/'.github/workflows/new-apps-public-release.yml').read_text()
        self.assertIn('Store 0.8 is immutable', workflow)
        guard = workflow.split('  publish:', 1)[1].split('    strategy:', 1)[0]
        self.assertIn("github.event_name == 'workflow_dispatch'", guard)
        self.assertIn('contains(github.event.head_commit.message', guard)
        self.assertNotIn("echo 'version=0.8'", workflow)
