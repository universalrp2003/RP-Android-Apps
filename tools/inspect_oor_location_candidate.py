"""Configured 1.1.18 candidate; preserve the historical inspectors unchanged."""
import json, os
from pathlib import Path
import inspect_battery_voice_candidate as inspector
inspector.APPS['oorarivippu'] = (
    'com.oorarivippu.voiceannouncer', '1.1.19', 24, 'oor-arivippu-v1.1.18',
    'Oor-Arivippu-v1.1.17.apk', 'e224ed2a9db1494c480c14e616821b24fbc73bd2ec48fea05390f401281ef0da',
    'Oor-Arivippu-v1.1.18.apk')
inspector.main('oorarivippu')
p = Path(os.environ['RUNNER_TEMP'])/'health-oorarivippu'/'verification.json'
r = json.loads(p.read_text())
r.update(publication_authorized=False, physical_phone_tested=False)
p.write_text(json.dumps(r, indent=2)+'\n')
