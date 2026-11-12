"""Configured 1.1.17 candidate; preserve the historical inspectors unchanged."""
import json, os
from pathlib import Path
import inspect_battery_voice_candidate as inspector
inspector.APPS['oorarivippu'] = (
    'com.oorarivippu.voiceannouncer', '1.1.17', 22, 'oor-arivippu-v1.1.16',
    'Oor-Arivippu-v1.1.16.apk', '3d7119ae7d843ee57c4ca2a69df5ef6bfedf4e90210af20ceb4e74a6f5b42fee',
    'Oor-Arivippu-v1.1.17.apk')
inspector.main('oorarivippu')
p = Path(os.environ['RUNNER_TEMP'])/'health-oorarivippu'/'verification.json'
r = json.loads(p.read_text())
r.update(publication_authorized=False, physical_phone_tested=False)
p.write_text(json.dumps(r, indent=2)+'\n')
