#!/usr/bin/env python3
"""Inspect only the configured Oor candidate; never mutate public assets or sibling apps."""
import json
import os
from pathlib import Path
import inspect_battery_voice_candidate as inspector

inspector.APPS['oorarivippu'] = (
    'com.oorarivippu.voiceannouncer', '1.1.16', 21, 'oor-arivippu-v1.1.15',
    'Oor-Arivippu-v1.1.15.apk', 'ec625cbfb6faa1bab680b71e4184d945913908662e56f898e695dea932c1abe5',
    'Oor-Arivippu-v1.1.16-about-guide-candidate.apk')
if __name__ == '__main__':
    inspector.main('oorarivippu')
    root = Path(os.environ['RUNNER_TEMP']) / 'health-oorarivippu'
    p = root / 'verification.json'
    receipt = json.loads(p.read_text())
    pins = json.loads(Path('docs/oor-family-native-verification-pins.json').read_text())
    receipt.update(baseline_version='1.1.15', baseline_code=20, tested_app_source=pins['source'], service_url=pins['service_url'],
                   service_verification_run=pins['deployment_run'], publication_authorized=False,
                   physical_phone_tested=False, continuous_tracking_proven=False)
    p.write_text(json.dumps(receipt, indent=2)+'\n')
