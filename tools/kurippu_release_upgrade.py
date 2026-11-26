#!/usr/bin/env python3
"""In-place update from the owner-tested 0.4 APK to 0.5 candidate; synthetic data only."""
import json, os, traceback
import xml.etree.ElementTree as ET
from pathlib import Path
import kurippu_upgrade_smoke as h

def fields(): return h.find(lambda n: n.get('class') == 'android.widget.EditText')
def enter(index, value):
    nodes = fields(); assert len(nodes) > index, [n.attrib for n in nodes]
    h.tap(nodes[index]); h.adb('shell','input','text',value); h.adb('shell','input','keyevent','4')
def note_tab():
    nodes = h.find(lambda n:n.get('text')=='குறிப்பு'); assert len(nodes) >= 2
    h.tap(nodes[-1])
def main():
    assert h.digest(h.OLD)=='7fc6b3736da8ea0af51f335e87cd02379a19b24166357b1236395572b0021ed8'
    # Disposable CI emulator only; remove instrumentation's synthetic installation before the fixture.
    if h.PACKAGE in h.adb('shell','pm','list','packages',h.PACKAGE):
        h.run('adb','uninstall',h.PACKAGE)
    h.adb('install',str(h.OLD)); h.adb('shell','am','start','-n',h.PACKAGE+'/.MainActivity')
    enter(0,'Milk'); enter(1,'50'); enter(2,'Bread'); enter(3,'45'); h.find(h.text('95'))
    note_tab(); enter(0,'Reminder'); h.find(h.text('Reminder'))
    old_uid=h.uid(); h.adb('install','-r',str(h.NEW)); assert h.uid()==old_uid
    h.adb('shell','am','force-stop',h.PACKAGE); h.adb('shell','am','start','-n',h.PACKAGE+'/.MainActivity')
    h.find(h.text('Milk')); h.find(h.text('Bread')); h.find(h.text('95'))
    note_tab(); h.find(h.text('Reminder'))
    h.tap_text('About'); h.find(h.text('Version 0.5')); h.find(h.text('No backup'))
    receipt={'from_version':'0.4','to_version':'0.5','package':h.PACKAGE,'same_uid':True,'expense_entries_and_total_preserved':True,'note_preserved':True,'about_version_and_warning':True,'physical_phone':False,'apk_sha256':h.digest(h.NEW),'bytes':h.NEW.stat().st_size,'source':os.environ['GITHUB_SHA']}
    Path('/tmp/kurippu/verification.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print('::notice title=Signed in-place notebook upgrade::'+json.dumps(receipt),flush=True)
if __name__=='__main__':
    try: main()
    except Exception:
        diag=traceback.format_exc()
        try: diag+='\nSCREEN: '+ET.tostring(h.tree(),encoding='unicode')[-7000:]
        except Exception: pass
        print('::error title=Notebook upgrade failed::'+diag[:10000].replace('%','%25').replace('\n','%0A'),flush=True)
        raise
