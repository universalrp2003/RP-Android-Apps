#!/usr/bin/env python3
"""In-place update from the owner-tested 1.2 APK to 1.3 candidate; synthetic data only."""
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
    assert h.digest(h.OLD)=='081ca0827160d4bedde2e01613b15462ee792a65513a48f0609d5fcfa8656791'
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
    h.tap_text('About'); h.find(h.text('Version 1.3')); h.find(h.text('No backup'))
    h.adb('shell','input','keyevent','4')
    # The third tab can be off-screen on the emulator. Scroll the tab strip, not the notebook.
    h.adb('shell','input','swipe','980','150','200','150','400')
    h.tap_text('Calculator')
    for label in ('5', '+', '4', '='):
        h.tap(h.find(lambda n, label=label: n.get('text') == label)[-1])
    h.adb('shell','input','swipe','500','1700','500','700','400')
    h.find(h.text('5 + 4 = 9'))
    h.adb('shell','am','force-stop',h.PACKAGE)
    h.adb('shell','am','start','-n',h.PACKAGE+'/.MainActivity')
    h.adb('shell','input','swipe','980','150','200','150','400')
    h.tap_text('Calculator')
    h.adb('shell','input','swipe','500','1700','500','700','400')
    h.find(h.text('5 + 4 = 9'))
    h.adb('shell','input','swipe','500','700','500','1700','400')
    h.tap(h.find(lambda n:n.get('text') == 'C')[0])
    h.adb('shell','input','swipe','500','1700','500','700','400')
    h.find(h.text('5 + 4 = 9'))
    receipt={'from_version':'1.2','to_version':'1.3','package':h.PACKAGE,'same_uid':True,'expense_entries_and_total_preserved':True,'note_preserved':True,'about_version_and_warning':True,'history_survives_force_stop_and_clear':True,'physical_phone':False,'apk_sha256':h.digest(h.NEW),'bytes':h.NEW.stat().st_size,'source':os.environ['GITHUB_SHA']}
    Path('/tmp/kurippu/verification.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print('::notice title=Signed in-place notebook upgrade::'+json.dumps(receipt),flush=True)
if __name__=='__main__':
    try: main()
    except Exception:
        diag=traceback.format_exc()
        try: diag+='\nVISIBLE TEXT: '+repr([n.get('text') for n in h.tree().iter('node') if n.get('text')])
        except Exception: pass
        print('::error title=Notebook upgrade failed::'+diag[:10000].replace('%','%25').replace('\n','%0A'),flush=True)
        raise
