#!/usr/bin/env python3
"""Inspect only the configured Oor candidate; never mutate public assets or sibling apps."""
import json
import os
from pathlib import Path
import inspect_battery_voice_candidate as inspector

inspector.APPS['oorarivippu'] = (
    'com.oorarivippu.voiceannouncer', '1.1.15', 20, 'oor-arivippu-v1.1.10',
    'Oor-Arivippu-v1.1.10.apk', '7a526fc54ac2ed07a5f972cfa030b9eb71ac77c5d1a16151c50803e9b0d7ceaa',
    'Oor-Arivippu-v1.1.15-family-candidate.apk')
if __name__ == '__main__':
    inspector.main('oorarivippu')
    root = Path(os.environ['RUNNER_TEMP']) / 'health-oorarivippu'
    p = root / 'verification.json'
    receipt = json.loads(p.read_text())
    pins = json.loads(Path('docs/oor-family-native-verification-pins.json').read_text())
    receipt.update(tested_app_source=pins['source'], service_url=pins['service_url'],
                   service_verification_run=pins['deployment_run'], publication_authorized=False,
                   physical_phone_tested=False, continuous_tracking_proven=False)
    p.write_text(json.dumps(receipt, indent=2)+'\n')
