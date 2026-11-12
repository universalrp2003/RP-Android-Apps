"""Only the hosted system launcher ANR may be recovered; app failures still fail."""
from pathlib import Path
import os
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]

class EmulatorLauncherRecoveryTest(unittest.TestCase):
    def run_probe(self, text, *, ci='true', actions='true', serial='emulator-5554', stage='start screen in Tamil, then English'):
        source = (ROOT/'secretmessage/wifi-test/run-wifi-test-on-emulator.sh').read_text()
        function = 'LAUNCHER_RECOVERY_USED=false\n' + source.split('LAUNCHER_RECOVERY_USED=false\n', 1)[1].split('\n# Scroll the connect screen:', 1)[0]
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            adb = path/'adb'
            adb.write_text('''#!/bin/bash
case "$*" in
  get-serialno) echo "$SERIAL";;
  'shell am force-stop com.google.android.apps.nexuslauncher') echo "$*" >> "$RECOVERY_LOG";;
  *) exit 91;;
esac
''')
            adb.chmod(0o755)
            xml = path/'ui.xml'; xml.write_text(text)
            log = path/'recoveries'
            script = '''set -euo pipefail
app_alive() { return 0; }
dump_ui() { return 0; }
find_node() { "$LAUNCHER_RECOVERY_USED"; }
visible_text() { cat "$UI_XML"; }
sleep() { :; }
fail() { echo "$1" >&2; exit 42; }
''' + function + '\nwait_for text expected equals 3\n'
            env = dict(os.environ, CI=ci, GITHUB_ACTIONS=actions, SERIAL=serial, STEP=stage,
                       ADB=str(adb), UI_XML=str(xml), RECOVERY_LOG=str(log))
            result = subprocess.run(['bash', '-c', script], env=env, capture_output=True, text=True, timeout=5)
            calls = log.read_text().splitlines() if log.exists() else []
            return result, calls

    def test_only_hosted_emulator_startup_launcher_dialog_is_recovered_once(self):
        result, calls = self.run_probe("Pixel Launcher isn't responding")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(calls, ['shell am force-stop com.google.android.apps.nexuslauncher'])
        self.assertIn('target app remains running', result.stdout)

    def test_target_app_anr_non_ci_phone_and_later_stage_still_fail(self):
        cases = [dict(text="Secret Message isn't responding"),
                 dict(text="Pixel Launcher isn't responding", ci='false'),
                 dict(text="Pixel Launcher isn't responding", actions='false'),
                 dict(text="Pixel Launcher isn't responding", serial='physical-phone'),
                 dict(text="Pixel Launcher isn't responding", stage='settings')]
        for case in cases:
            with self.subTest(case=case):
                result, calls = self.run_probe(**case)
                self.assertEqual(result.returncode, 42, result.stderr)
                self.assertEqual(calls, [])
                self.assertIn('Timed out', result.stderr)
