"""Configured 1.1.18 candidate; preserve the historical inspectors unchanged."""
import json, os
from pathlib import Path
import inspect_battery_voice_candidate as inspector
inspector.APPS['oorarivippu'] = (
    'com.oorarivippu.voiceannouncer', '1.1.18', 23, 'oor-arivippu-v1.1.17',
    'Oor-Arivippu-v1.1.17.apk', '7b7ad216bc2b5cdbb4c2d1e6323bcd637c49a6365b804e62fa9bd9d1998c708f',
    'Oor-Arivippu-v1.1.18.apk')
inspector.main('oorarivippu')
p = Path(os.environ['RUNNER_TEMP'])/'health-oorarivippu'/'verification.json'
r = json.loads(p.read_text())
r.update(publication_authorized=False, physical_phone_tested=False)
p.write_text(json.dumps(r, indent=2)+'\n')
