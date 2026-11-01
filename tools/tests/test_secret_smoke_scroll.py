"""Exercise the real shell helpers without an emulator; no UI assertion is skipped."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[2]
SOURCE=(ROOT/'secretmessage/wifi-test/run-wifi-test-on-emulator.sh').read_text()

class SecretSmokeScrollTest(unittest.TestCase):
    def probe(self, start, target, alive=True):
        helper='scroll_until() {'+SOURCE.split('scroll_until() {',1)[1].split('\ntap() {',1)[0]
        script='''set -euo pipefail
position=$START
moves=0
app_alive() { "$ALIVE"; }
dump_ui() { return 0; }
find_node() { ((position == TARGET)); }
visible_text() { echo "position=$position moves=$moves"; }
scroll() {
  moves=$((moves+1))
  if [[ "$1" == down ]]; then position=$((position<10 ? position+1 : 10));
  else position=$((position>0 ? position-1 : 0)); fi
}
fail() { echo "$1" >&2; exit 42; }
'''+helper+'\nscroll_until text required\necho "FOUND $position moves=$moves"\n'
        return subprocess.run(['bash','-c',script],env=dict(os.environ,START=str(start),TARGET=str(target),ALIVE=str(alive).lower()),capture_output=True,text=True,timeout=5)

    def test_target_above_or_below_or_in_current_viewport(self):
        for start,target in ((7,2),(0,8),(4,4),(10,0)):
            with self.subTest(start=start,target=target):
                result=self.probe(start,target)
                self.assertEqual(result.returncode,0,result.stderr)
                self.assertIn(f'FOUND {target}',result.stdout)

    def test_missing_control_is_still_a_bounded_failure(self):
        result=self.probe(5,99)
        self.assertEqual(result.returncode,42)
        self.assertIn('moves=24',result.stderr)
        self.assertIn('required',result.stderr)

    def test_dead_app_is_not_recovered_or_ignored(self):
        result=self.probe(0,4,False)
        self.assertEqual(result.returncode,42)
        self.assertIn('process is not running',result.stderr)

    def find(self,nodes):
        helper='find_node() {'+SOURCE.split('find_node() {',1)[1].split('\napp_alive() {',1)[0]
        with tempfile.TemporaryDirectory() as tmp:
            file=Path(tmp)/'ui.xml';file.write_text('<hierarchy>'+nodes+'</hierarchy>')
            return subprocess.run(['bash','-c','set -euo pipefail\n'+helper+'\nfind_node text wanted\n'],env=dict(os.environ,UI_XML=str(file),SCREEN_W='100',SCREEN_H='200'),capture_output=True,text=True,timeout=5)

    def test_offscreen_and_zero_area_semantics_do_not_count_as_visible(self):
        for bounds in ('[0,0][0,0]','[0,210][90,240]','[0,-40][90,-10]'):
            with self.subTest(bounds=bounds):
                self.assertNotEqual(self.find(f'<node text="wanted" bounds="{bounds}"/>').returncode,0)

    def test_visible_node_after_hidden_match_and_clipped_tap_are_supported(self):
        result=self.find('<node text="wanted" bounds="[0,0][0,0]"/><node text="wanted" bounds="[10,180][90,230]"/>')
        self.assertEqual(result.returncode,0,result.stderr)
        self.assertEqual(result.stdout.strip(),'50 190')

    def test_disclosure_body_is_scrolled_into_view_independently_of_heading(self):
        self.assertIn('scroll_until text "About Secret Message"\nscroll_until text "has not been independently security-reviewed" contains',SOURCE)

    def test_zero_exit_dump_failure_cannot_reuse_old_viewport_xml(self):
        helper='dump_ui() {'+SOURCE.split('dump_ui() {',1)[1].split('\n# Print "x y"',1)[0]
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)
            remote=path/'remote.xml';remote.write_text('<hierarchy><node text="stale"/></hierarchy>')
            counter=path/'attempts'
            adb=path/'adb'
            adb.write_text('''#!/usr/bin/env python3
import os,sys
from pathlib import Path
remote=Path(os.environ['REMOTE']);counter=Path(os.environ['COUNTER'])
args=sys.argv[1:]
if args[:3]==['shell','rm','-f']:
 remote.unlink(missing_ok=True)
elif args[:3]==['shell','uiautomator','dump']:
 n=int(counter.read_text())+1 if counter.exists() else 1
 counter.write_text(str(n))
 if n==1:print('ERROR: could not get idle state.')
 else:remote.write_text('<hierarchy><node text="fresh"/></hierarchy>')
elif args[:2]==['shell','cat']:
 if not remote.exists():sys.exit(1)
 print(remote.read_text())
else:sys.exit(91)
''')
            adb.chmod(0o755)
            result=subprocess.run(['bash','-c','set -euo pipefail\nsleep() { :; }\n'+helper+'\ndump_ui\ncat "$UI_XML"\n'],env=dict(os.environ,ADB=str(adb),UI_XML=str(path/'local.xml'),REMOTE=str(remote),COUNTER=str(counter)),capture_output=True,text=True,timeout=5)
            self.assertEqual(result.returncode,0,result.stderr)
            self.assertIn('fresh',result.stdout)
            self.assertNotIn('stale',result.stdout)
            self.assertEqual(counter.read_text(),'2')
