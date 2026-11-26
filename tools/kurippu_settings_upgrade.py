#!/usr/bin/env python3
"""Upgrade exact 0.3 to exact 0.4, preserving synthetic expenses and note details."""
import traceback
import xml.etree.ElementTree as ET
import kurippu_upgrade_smoke as h


def fields(): return h.find(lambda n: n.get('class') == 'android.widget.EditText')
def enter(index, value):
    nodes = fields(); assert len(nodes) > index, [n.attrib for n in nodes]
    h.tap(nodes[index]); h.adb('shell', 'input', 'text', value); h.adb('shell', 'input', 'keyevent', '4')
def tap_note():
    nodes = h.find(lambda n: n.get('text') == 'குறிப்பு')
    assert len(nodes) >= 2
    h.tap(nodes[-1])
def main():
    assert h.digest(h.OLD) == '66400a37c89db8e78799b9c391accfbe10907d83f6e3b994a2e09707ce514bf3'
    assert h.digest(h.NEW) == '7fc6b3736da8ea0af51f335e87cd02379a19b24166357b1236395572b0021ed8'
    h.adb('install', str(h.OLD)); h.adb('shell','am','start','-n',h.PACKAGE+'/.MainActivity')
    enter(0,'Milk'); enter(1,'50'); enter(2,'Bread'); enter(3,'45')
    h.find(h.text('95'))
    tap_note(); h.find(h.text('Details'))
    enter(0,'Reminder'); enter(1,'Memo')
    h.find(h.text('Reminder')); h.find(h.text('Memo'))
    h.tap_text('+'); enter(0,'Work'); h.tap_text('Add'); h.find(h.text('Work'))
    before = h.uid()
    h.adb('install','-r',str(h.NEW)); assert h.uid() == before
    h.adb('shell','am','force-stop',h.PACKAGE); h.adb('shell','am','start','-n',h.PACKAGE+'/.MainActivity')
    h.find(h.text('Milk')); h.find(h.text('Bread')); h.find(h.text('95'))
    tap_note(); h.find(h.text('Reminder')); h.find(h.text('Memo')); h.find(h.text('Details'))
    h.tap_text('Work'); h.find(h.text('Work'))
    xml = h.tree()
    assert not any(h.text('Details')(n) or h.text('Amount')(n) or h.text('Total')(n) for n in xml.iter('node'))
    h.tap_text('செலவு'); h.find(h.text('95'))
    print('::notice title=Exact signed settings upgrade::0.3 to 0.4 retained UID, expense rows and total, populated note details and custom page; empty note page is full width.')
if __name__ == '__main__':
    try: main()
    except Exception:
        diag=traceback.format_exc()
        try: diag += '\nSCREEN: '+ET.tostring(h.tree(),encoding='unicode')[-7000:]
        except Exception: pass
        print('::error title=Upgrade smoke failure::'+diag[:10000].replace('%','%25').replace('\n','%0A'),flush=True)
        raise
