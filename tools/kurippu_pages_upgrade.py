#!/usr/bin/env python3
"""Use the exact signed 0.2 and 0.3 candidates with synthetic data; do not clear data."""
import traceback
import xml.etree.ElementTree as ET
import kurippu_upgrade_smoke as h


def fields(): return h.find(lambda n: n.get('class') == 'android.widget.EditText')
def enter(index, value):
    nodes = fields(); assert len(nodes) > index, [n.attrib for n in nodes]
    h.tap(nodes[index]); h.adb('shell', 'input', 'text', value)
    h.adb('shell', 'input', 'keyevent', '4')  # close keyboard; refresh bounds before next tap

def main():
    assert h.digest(h.OLD) == 'b30fdeb86b270877b0f4a6ae279546ae1b9a7cce8231d4f9257f2c1348676e6c'
    assert h.digest(h.NEW) == '66400a37c89db8e78799b9c391accfbe10907d83f6e3b994a2e09707ce514bf3'
    h.adb('install', str(h.OLD))
    h.adb('shell', 'am', 'start', '-n', h.PACKAGE + '/.MainActivity')
    enter(0, 'Milk'); enter(1, '50'); enter(2, 'Bread'); enter(3, '45')
    h.find(h.text('Milk')); h.find(h.text('Bread')); h.find(h.text('95'))
    before = h.uid()
    h.adb('install', '-r', str(h.NEW))
    assert h.uid() == before
    h.adb('shell', 'am', 'force-stop', h.PACKAGE)
    h.adb('shell', 'am', 'start', '-n', h.PACKAGE + '/.MainActivity')
    h.find(h.text('Milk')); h.find(h.text('Bread')); h.find(h.text('95'))
    # Both the app title and the note-page tab are named குறிப்பு; the latter is last.
    note_tabs = h.find(lambda n: n.get('text') == 'குறிப்பு')
    assert len(note_tabs) >= 2, [n.attrib for n in note_tabs]
    h.tap(note_tabs[-1])
    h.find(h.text('Details'))
    assert not any(h.text('Total')(n) for n in h.tree().iter('node'))
    enter(0, 'Reminder'); enter(1, '123')
    h.find(h.text('Reminder'))
    assert not any(h.text('Total')(n) for n in h.tree().iter('node'))
    h.tap_text('செலவு')
    h.find(h.text('Milk')); h.find(h.text('Bread')); h.find(h.text('95'))
    print('::notice title=Exact signed upgrade::0.2 to 0.3 retained UID, both synthetic expense entries and total 95; separate note page did not total, and switching back kept expenses.')

if __name__ == '__main__':
    try: main()
    except Exception:
        diag = traceback.format_exc()
        try: diag += '\nSCREEN: ' + ET.tostring(h.tree(), encoding='unicode')[-7000:]
        except Exception: pass
        print('::error title=Upgrade smoke failure::' + diag[:10000].replace('%','%25').replace('\n','%0A'), flush=True)
        raise
