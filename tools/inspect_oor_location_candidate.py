"""Configured 1.1.20 candidate; preserve the historical inspectors unchanged."""
import json, os
from pathlib import Path
import inspect_battery_voice_candidate as inspector
inspector.APPS['oorarivippu'] = (
    'com.oorarivippu.voiceannouncer', '1.1.20', 25, 'oor-arivippu-v1.1.19',
    'Oor-Arivippu-v1.1.19.apk', 'f00fefa61f8b0c3282746749a32cf9c17d2b9a62b38477a783573b6de9b3bb02',
    'Oor-Arivippu-v1.1.20.apk')
inspector.main('oorarivippu')
p = Path(os.environ['RUNNER_TEMP'])/'health-oorarivippu'/'verification.json'
r = json.loads(p.read_text())
r.update(publication_authorized=False, physical_phone_tested=False)
p.write_text(json.dumps(r, indent=2)+'\n')
