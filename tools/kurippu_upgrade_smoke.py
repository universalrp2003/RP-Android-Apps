#!/usr/bin/env python3
"""Exercise a real same-key 0.1 -> 0.2 upgrade using synthetic UI-entered data."""
import hashlib
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = 'com.universalrp.kurippu'
OLD = Path(os.environ['OLD_APK'])
NEW = Path(os.environ['NEW_APK'])

def run(*args): return subprocess.check_output(args, text=True).strip()
def adb(*args): return run('adb', *args)
def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def tree():
    adb('shell','uiautomator','dump','/sdcard/kurippu-window.xml')
    return ET.fromstring(adb('exec-out','cat','/sdcard/kurippu-window.xml'))
def find(predicate, timeout=25):
    deadline = time.monotonic()+timeout
    while time.monotonic()<deadline:
        root=tree()
        nodes=[n for n in root.iter('node') if predicate(n)]
        if nodes: return nodes
        time.sleep(1)
    raise AssertionError('UI element not found. Screen: '+ET.tostring(root,encoding='unicode')[-10000:])
def text(value): return lambda n: value in (n.get('text','') + ' ' + n.get('content-desc',''))
def tap(node):
    nums=list(map(int,re.findall(r'\d+',node.get('bounds','')))); assert len(nums)==4, node.attrib
    adb('shell','input','tap',str((nums[0]+nums[2])//2),str((nums[1]+nums[3])//2))
def tap_text(value): tap(find(text(value))[0])
def uid():
    out=adb('shell','cmd','package','list','packages','-U',PACKAGE)
    match=re.search(r'uid:(\d+)',out); assert match, out
    return match.group(1)
def main():
    assert digest(OLD)=='6eb3c2fde4ded6111d149c721ea84baa999f98e850d076265e7ee628d1c4886e'
    assert digest(NEW)=='b30fdeb86b270877b0f4a6ae279546ae1b9a7cce8231d4f9257f2c1348676e6c'
    adb('install',str(OLD))
    adb('shell','am','start','-n',PACKAGE+'/.MainActivity')
    tap_text('New section')
    fields=find(lambda n:n.get('class')=='android.widget.EditText'); tap(fields[0]); adb('shell','input','text','Expense')
    adb('shell','input','keyevent','4')
    tap_text('Save')
    tap_text('Add entry')
    fields=find(lambda n:n.get('class')=='android.widget.EditText' and len(n.get('bounds',''))>0)
    assert len(fields)==2, [n.attrib for n in fields]
    tap(fields[0]); adb('shell','input','text','Milk')
    adb('shell','input','keyevent','4')
    fields=find(lambda n:n.get('class')=='android.widget.EditText')
    assert len(fields)==2, [n.attrib for n in fields]
    tap(fields[1]); adb('shell','input','text','30')
    adb('shell','input','keyevent','4')
    tap_text('Save')
    find(text('Add entry')); find(text('Milk')); find(text('30'))
    before=uid()
    adb('install','-r',str(NEW))
    assert uid()==before
    adb('shell','am','force-stop',PACKAGE)
    adb('shell','am','start','-n',PACKAGE+'/.MainActivity')
    find(text('Expense')); find(text('Milk')); find(text('30'))
    print('::notice title=In-place upgrade::Exact signed 0.1 to 0.2 kept UID and synthetic section title, entry and amount; no uninstall or data clear.')
if __name__=='__main__':
    try: main()
    except Exception:
        import traceback
        diagnostic = traceback.format_exc()
        try: diagnostic += "\nSCREEN: " + ET.tostring(tree(), encoding="unicode")[-7000:]
        except Exception: pass
        print('::error title=Upgrade smoke failure::' + diagnostic[:10000].replace('%','%25').replace('\n','%0A'), flush=True)
        raise

