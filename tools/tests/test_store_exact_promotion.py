"""Negative provenance gates: no GitHub mutation or APK download in these tests."""
import copy
import importlib.util
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
spec = importlib.util.spec_from_file_location('store_exact_promotion', TOOLS / 'promote_verified_store_09.py')
promote = importlib.util.module_from_spec(spec)
spec.loader.exec_module(promote)


class StoreExactPromotionTests(unittest.TestCase):
    def setUp(self):
        self.pin = dict(source='a' * 40, candidate_run=101, native_run=102)
        signed = ['Source and localization gates', 'Test, lint and build release-key candidate',
                  'Pin identity, signer and exact upgrade fixture',
                  'Exact signed Store0.8 upgrade and data preservation on Android15',
                  'Exact artifact receipt after upgrade testing']
        native = ['Test and compile preview', 'Verify real archives and Android installer handoff on Android 15']
        self.responses = {}
        for run_id, workflow, names in [(101, 'tamilanstore-signed-candidate.yml', signed),
                                        (102, 'tamilanstore-preview.yml', native)]:
            self.responses[f'actions/runs/{run_id}'] = dict(head_sha=self.pin['source'],
                head_branch=promote.BRANCH, status='completed', conclusion='success',
                path='.github/workflows/' + workflow)
            self.responses[f'actions/runs/{run_id}/jobs'] = dict(jobs=[dict(conclusion='success',
                steps=[dict(name=name, conclusion='success') for name in names])])

    def verify(self):
        with patch.object(promote, 'api', side_effect=lambda key: copy.deepcopy(self.responses[key])):
            promote.verify_candidate_gates(self.pin)

    def test_both_matching_complete_gates_pass(self):
        self.verify()

    def test_wrong_source_branch_path_and_incomplete_or_failed_runs_stop(self):
        for run_id in (101, 102):
            for key, bad in [('head_sha', 'b' * 40), ('head_branch', 'main'),
                             ('path', '.github/workflows/other.yml'),
                             ('status', 'in_progress'), ('conclusion', 'failure'),
                             ('conclusion', 'cancelled'), ('conclusion', 'skipped')]:
                with self.subTest(run=run_id, field=key, value=bad):
                    info = self.responses[f'actions/runs/{run_id}']
                    old = info[key]
                    info[key] = bad
                    with self.assertRaises(AssertionError):
                        self.verify()
                    info[key] = old

    def test_missing_failed_skipped_required_step_stops_even_if_job_says_success(self):
        for run_id in (101, 102):
            job = self.responses[f'actions/runs/{run_id}/jobs']['jobs'][0]
            for step in job['steps']:
                for bad in ('failure', 'skipped', None):
                    with self.subTest(run=run_id, step=step['name'], bad=bad):
                        step['conclusion'] = bad
                        with self.assertRaises(AssertionError):
                            self.verify()
                step['conclusion'] = 'success'
            removed = job['steps'].pop()
            with self.assertRaises(StopIteration):
                self.verify()
            job['steps'].append(removed)

    def test_empty_extra_or_failed_jobs_stop(self):
        for run_id in (101, 102):
            response = self.responses[f'actions/runs/{run_id}/jobs']
            jobs = response['jobs']
            for bad in ([], jobs * 2, [dict(conclusion='failure', steps=[])]):
                response['jobs'] = bad
                with self.assertRaises(AssertionError):
                    self.verify()
            response['jobs'] = jobs

    def test_signing_guard_does_not_recommend_destructive_reinstall(self):
        import subprocess
        result = subprocess.run([sys.executable, str(TOOLS / 'verify_release_signing_policy.py'), '--fresh-install'],
                                capture_output=True, text=True, check=True)
        self.assertNotIn('require uninstall/reinstall', result.stdout)
        self.assertIn('same-key installs use normal updates', result.stdout)
        self.assertIn('Do not uninstall or clear data', result.stdout)

    def test_publication_script_has_no_build_overwrite_delete_or_bypass(self):
        source = (TOOLS / 'promote_verified_store_09.py').read_text()
        for forbidden in ('--clobber', "'delete'", 'assembleRelease', 'install -t', '--allow-downgrade'):
            self.assertNotIn(forbidden, source)
        self.assertIn("existing[0]['id']==resume", source)
        self.assertIn("existing[0]['draft']", source)
        self.assertIn("draft['assets']", source)
        self.assertIn("--draft=false", source)
