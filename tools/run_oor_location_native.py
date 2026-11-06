"""Strict native runner: never substitute a synthetic JUnit success for a real result."""
import hashlib,json,os,subprocess
from pathlib import Path
root=Path(os.environ['RUNNER_TEMP'])/'health-oorarivippu'
p=root/'verification.json';r=json.loads(p.read_text())
assert subprocess.check_output(['adb','get-serialno'],text=True).strip().startswith('emulator-')
installed=subprocess.check_output(['adb','shell','pm','path',r['package']],text=True).strip().splitlines()[0].removeprefix('package:')
data=subprocess.check_output(['adb','exec-out','cat',installed])
assert len(data)==r['bytes'] and hashlib.sha256(data).hexdigest()==r['sha256']
command=['adb','shell','am','instrument','-w','-r','-e','class',
 'com.oorarivippu.voiceannouncer.OorLocationNativeTest,com.oorarivippu.voiceannouncer.GpsProbeNativeTest',
 '-e','expectedHash',r['sha256'],'com.oorarivippu.voiceannouncer.test/androidx.test.runner.AndroidJUnitRunner']
result=subprocess.run(command,capture_output=True,text=True,timeout=600)
raw=result.stdout+result.stderr;(root/'trip-native.txt').write_text(raw)
if result.returncode or 'OK (4 tests)' not in raw or 'INSTRUMENTATION_STATUS_CODE: 0' not in raw:
 print('::error title=Oor trip native::'+raw[-3500:].replace('%','%25').replace('\n','%0A').replace('\r','%0D'),flush=True)
 raise AssertionError('Actual native UI test failed')
assert 'INSTRUMENTATION_CODE: -1' in raw and 'FAILURES' not in raw
r.update(native_trip_tests=4,gps_probe_background_lifecycle_verified=True,gps_probe_timeout_and_cleanup_verified=True,bilingual_home_history_rendered=True,font100_and200_rendered=True,
 encrypted_family_fixture_decrypted_after_upgrade=True,exact_installed_apk_verified=True)
p.write_text(json.dumps(r,indent=2)+'\n')
print('::notice title=Oor trip native passed::'+json.dumps(r),flush=True)
